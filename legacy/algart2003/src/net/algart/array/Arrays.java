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

package net.algart.array;

import java.util.*;
import java.lang.reflect.*;
import java.nio.*;
import java.lang.Math;
import net.algart.lib.*;

/**
 * <p>Wide library of tools for working with arrays (native-optimized).
 *
 * @author  Daniel Alievsky
 * @version 1.1
 */

public final class Arrays implements TrueStatic {

    /**
     * Don't let anyone instantiate this class.
     */
    private Arrays() {}

    /* Arrays service */

    private static int length(Object someVector) {
        try {
            return Array.getLength(someVector);
        } catch (RuntimeException e) {
            return 0;
        }
    }
    private static int sizeOfElement(Object someArray) {
    // -1 means unknown or not an array
        if (someArray==null) return -1;
        return Memory.memoryOfPrimitive(someArray.getClass().getComponentType());
    }

    public static Object newCompatible(Object a, int len) {
    // Here and below "Object" means "some Array"
        if (a==null) return null;
        return Array.newInstance(a.getClass().getComponentType(),len);
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
    public static Object appendEnsured(Object a, int alen, Object b, int bofs, int len) {
        return copyEnsured(b,bofs,a,alen,len);
    }
    public static Object appendEnsured(Object a, int alen, Object b) {
        return copyEnsured(b,0,a,alen,Array.getLength(b));
    }
    public static Object truncate(Object a, int alen) {
        return copy(a,0,alen);
    }
    public static Object[] join2d(Object[] a) {
        if (a == null) return null;
        int size = 0;
        for (int k = 0; k < a.length; k++) size += Array.getLength(a[k]);
        Object[] result = (Object[])Array.newInstance(a.getClass().getComponentType().getComponentType(),size);
        for (int k = 0, disp = 0; k < a.length; k++) {
            int len = Array.getLength(a[k]);
            copy(a[k],0,result,disp,len);
            disp += len;
        }
        return result;
    }
    public static Object[] vectorToArray(Vector vector) {
        return vectorToArray(vector,new Object[0]);
    }
    public static Object[] vectorToArray(Vector vector, Object[] results) {
        if (results.length < vector.size()) results = (Object[])newCompatible(results,vector.size());
        vector.copyInto(results);
        return results;
    }


    /* Copying arrays with same-type components  */

    public static boolean[] copy(boolean a[]) {return copy(a,0,a.length);}
    public static char[] copy(char a[])       {return copy(a,0,a.length);}
    public static byte[] copy(byte a[])       {return copy(a,0,a.length);}
    public static short[] copy(short a[])     {return copy(a,0,a.length);}
    public static int[] copy(int a[])         {return copy(a,0,a.length);}
    public static long[] copy(long a[])       {return copy(a,0,a.length);}
    public static float[] copy(float a[])     {return copy(a,0,a.length);}
    public static double[] copy(double a[])   {return copy(a,0,a.length);}
    public static String[] copy(String a[])   {return copy(a,0,a.length);}
    public static Object[] copy(Object a[])   {return copy(a,0,a.length);}
    public static Object copy(Object a)       {return a==null? null: copy(a,0,Array.getLength(a));}
    // In the last function, "Object" means "some Array"

    public static boolean[] copy(boolean a[], int aofs, int len) {
        boolean[] r= new boolean[len]; copy(a,aofs,r,0,len); return r;
    }
    public static char[] copy(char a[], int aofs, int len) {
        char[] r= new char[len]; copy(a,aofs,r,0,len); return r;
    }
    public static byte[] copy(byte a[], int aofs, int len) {
        byte[] r= new byte[len]; copy(a,aofs,r,0,len); return r;
    }
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
    public static String[] copy(String a[], int aofs, int len) {
        String[] r= new String[len]; copy(a,aofs,r,0,len); return r;
    }
    public static Object[] copy(Object a[], int aofs, int len) {
        Object[] r= (Object[])newCompatible(a,len);
        copy(a,aofs,r,0,len); return r;
    }
    public static Object copy(Object a, int aofs, int len) {
    // Here "Object" means "some Array"
        if (a==null) return null;
        Object r= newCompatible(a,len);
        copy(a,aofs,r,0,len);
        return r;
    }

    // Skipping boolean type: Java doesn't specify sizeof(boolean)
    // copy(Object,Object) will be used for boolean[]
    public static void copy(char[] a, char[] b)     {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(byte[] a, byte[] b)     {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(short[] a, short[] b)   {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(int[] a, int[] b)       {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(long[] a, long[] b)     {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(float[] a, float[] b)   {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(double[] a, double[] b) {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(String[] a, String[] b) {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(Object[] a, Object[] b) {copy(a,0,b,0,min(a.length,b.length));}
    public static void copy(Object a, Object b)     {copy(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));}

    public static void copy(char[] a, int aofs, char[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<1,b,bofs<<1,(len-1)<<1);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(byte[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len-1);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(short[] a, int aofs, short[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<1,b,bofs<<1,(len-1)<<1);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(int[] a, int aofs, int[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<2,b,bofs<<2,(len-1)<<2);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(long[] a, int aofs, long[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<3,b,bofs<<3,(len-1)<<3);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(float[] a, int aofs, float[] b, int bofs, int len) {
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<2,b,bofs<<2,(len-1)<<2);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(double[] a, int aofs, double[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            b[bofs]= a[aofs]; b[bofs+len-1]= a[aofs+len-1];
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<3,b,bofs<<3,(len-1)<<3);
            return;
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(String[] a, int aofs, String[] b, int bofs, int len) {
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(Object[] a, int aofs, Object[] b, int bofs, int len) {
        System.arraycopy(a,aofs,b,bofs,len);
    }
    public static void copy(Object a, int aofs, Object b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && (ArraysNative.cpuInfo&CPU_SSE)!=0 && len>nativeMinLenFill) {
            Class elemClass= a.getClass().getComponentType();
            int sizeLog= elemClass==byte.class? 0:
                elemClass==short.class || elemClass==char.class? 1:
                elemClass==int.class || elemClass==float.class? 2:
                elemClass==long.class || elemClass==double.class? 3:
                -1;
            if (sizeLog!=-1) {
                System.arraycopy(a,aofs,b,bofs,1);
                System.arraycopy(a,aofs+len-1,b,bofs+len-1,1);
                ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs<<sizeLog,b,bofs<<sizeLog,
                    (len-1)<<sizeLog);
                return;
            }
        }
        System.arraycopy(a,aofs,b,bofs,len);
    }

    public static boolean[] append(boolean[] a, boolean[] b) {
        if (a==null) return b; if (b==null) return a;
        boolean[] r= new boolean[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static byte[] append(byte[] a, byte[] b) {
        if (a==null) return b; if (b==null) return a;
        byte[] r= new byte[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static short[] append(short[] a, short[] b) {
        if (a==null) return b; if (b==null) return a;
        short[] r= new short[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static int[] append(int[] a, int[] b) {
        if (a==null) return b; if (b==null) return a;
        int[] r= new int[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static long[] append(long[] a, long[] b) {
        if (a==null) return b; if (b==null) return a;
        long[] r= new long[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static float[] append(float[] a, float[] b) {
        if (a==null) return b; if (b==null) return a;
        float[] r= new float[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static double[] append(double[] a, double[] b) {
        if (a==null) return b; if (b==null) return a;
        double[] r= new double[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static String[] append(String[] a, String[] b) {
        if (a==null) return b; if (b==null) return a;
        String[] r= new String[a.length+b.length];
        copy(a,0,r,0,a.length);
        copy(b,0,r,a.length,b.length);
        return r;
    }
    public static Object append(Object a, Object b) {
    // Here "Object" means "some Array"
        if (a==null) return b; if (b==null) return a;
        int len1= Array.getLength(a);
        int len2= Array.getLength(b);
        Object r= newCompatible(a,len1+len2);
        copy(a,0,r,0,len1);
        copy(b,0,r,len1,len2);
        return r;
    }


    /* Coercion arrays with different-type components */

    // All offsets and lengths are measured in bytes
    public static byte[] coerciveCopyBytes(char[] a)    {return coerciveCopyBytes(a,0,2*a.length);}
    public static char[] coerciveCopyChars(byte[] a)    {return coerciveCopyChars(a,0,a.length);}
    public static byte[] coerciveCopyBytes(short[] a)   {return coerciveCopyBytes(a,0,2*a.length);}
    public static short[] coerciveCopyShorts(byte[] a)  {return coerciveCopyShorts(a,0,a.length);}
    public static byte[] coerciveCopyBytes(int[] a)     {return coerciveCopyBytes(a,0,4*a.length);}
    public static int[] coerciveCopyInts(byte[] a)      {return coerciveCopyInts(a,0,a.length);}
    public static byte[] coerciveCopyBytes(long[] a)    {return coerciveCopyBytes(a,0,8*a.length);}
    public static long[] coerciveCopyLongs(byte[] a)    {return coerciveCopyLongs(a,0,a.length);}
    public static byte[] coerciveCopyBytes(float[] a)   {return coerciveCopyBytes(a,0,4*a.length);}
    public static float[] coerciveCopyFloats(byte[] a)  {return coerciveCopyFloats(a,0,a.length);}
    public static byte[] coerciveCopyBytes(double[] a)  {return coerciveCopyBytes(a,0,8*a.length);}
    public static double[] coerciveCopyDoubles(byte[] a){return coerciveCopyDoubles(a,0,a.length);}
    public static byte[] coerciveCopyBytes(Object a)    {return coerciveCopyBytes(a,0,sizeOfElement(a)*length(a));}

    public static byte[] coerciveCopyBytes(char[] a, int aofs, int len)     {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}
    public static char[] coerciveCopyChars(byte[] a, int aofs, int len)     {char[] r= new char[(len+1)/2];     coerciveCopy(a,aofs,r,0,len); return r;}
    public static byte[] coerciveCopyBytes(short[] a, int aofs, int len)    {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}
    public static short[] coerciveCopyShorts(byte[] a, int aofs, int len)   {short[] r= new short[(len+1)/2];   coerciveCopy(a,aofs,r,0,len); return r;}
    public static byte[] coerciveCopyBytes(int[] a, int aofs, int len)      {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}
    public static int[] coerciveCopyInts(byte[] a, int aofs, int len)       {int[] r= new int[(len+3)/4];       coerciveCopy(a,aofs,r,0,len); return r;}
    public static byte[] coerciveCopyBytes(long[] a, int aofs, int len)     {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}
    public static long[] coerciveCopyLongs(byte[] a, int aofs, int len)     {long[] r= new long[(len+7)/8];     coerciveCopy(a,aofs,r,0,len); return r;}
    public static byte[] coerciveCopyBytes(float[] a, int aofs, int len)    {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}
    public static float[] coerciveCopyFloats(byte[] a, int aofs, int len)   {float[] r= new float[(len+3)/4];   coerciveCopy(a,aofs,r,0,len); return r;}
    public static byte[] coerciveCopyBytes(double[] a, int aofs, int len)   {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}
    public static double[] coerciveCopyDoubles(byte[] a, int aofs, int len) {double[] r= new double[(len+7)/8]; coerciveCopy(a,aofs,r,0,len); return r;}
    public static byte[] coerciveCopyBytes(Object a, int aofs, int len)     {byte[] r= new byte[len];           coerciveCopy(a,aofs,r,0,len); return r;}

    public static void coerciveCopy(char[] a, byte[] b)   {coerciveCopy(a,0,b,0,min(a.length*2,b.length));}
    public static void coerciveCopy(byte[] a, char[] b)   {coerciveCopy(a,0,b,0,min(a.length,b.length*2));}
    public static void coerciveCopy(short[] a, byte[] b)  {coerciveCopy(a,0,b,0,min(a.length*2,b.length));}
    public static void coerciveCopy(byte[] a, short[] b)  {coerciveCopy(a,0,b,0,min(a.length,b.length*2));}
    public static void coerciveCopy(int[] a, byte[] b)    {coerciveCopy(a,0,b,0,min(a.length*4,b.length));}
    public static void coerciveCopy(byte[] a, int[] b)    {coerciveCopy(a,0,b,0,min(a.length,b.length*4));}
    public static void coerciveCopy(long[] a, byte[] b)   {coerciveCopy(a,0,b,0,min(a.length*8,b.length));}
    public static void coerciveCopy(byte[] a, long[] b)   {coerciveCopy(a,0,b,0,min(a.length,b.length*8));}
    public static void coerciveCopy(float[] a, byte[] b)  {coerciveCopy(a,0,b,0,min(a.length*4,b.length));}
    public static void coerciveCopy(byte[] a, float[] b)  {coerciveCopy(a,0,b,0,min(a.length,b.length*4));}
    public static void coerciveCopy(double[] a, byte[] b) {coerciveCopy(a,0,b,0,min(a.length*8,b.length));}
    public static void coerciveCopy(byte[] a, double[] b) {coerciveCopy(a,0,b,0,min(a.length,b.length*8));}
    public static void coerciveCopy(Object a, Object b)   {
        int sizeOfElementA= sizeOfElement(a);
        if (sizeOfElementA<0) throw new IllegalArgumentException("Unsupported a argument type in " + Arrays.class.getName() + ".coerciveCopy(): "+JVM.toJavaClassName(a));
        int sizeOfElementB= sizeOfElement(b);
        if (sizeOfElementB<0) throw new IllegalArgumentException("Unsupported b argument type in " + Arrays.class.getName() + ".coerciveCopy(): "+JVM.toJavaClassName(b));
        int size= 0;
        try {
            size= min(Array.getLength(a)*sizeOfElementA,
                Array.getLength(b)*sizeOfElementB);
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Arguments are not arrays in " + Arrays.class.getName() + ".coerciveCopy(a,b): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
        }
        coerciveCopy(a,0,b,0,size);
    }

    private static void checkByteCC(char[] a, int ofs)  {char v= a[ofs/2];}
    private static void checkByteCC(byte[] a, int ofs)  {byte v= a[ofs];}
    private static void checkByteCC(short[] a, int ofs) {short v= a[ofs/2];}
    private static void checkByteCC(int[] a, int ofs)   {int v= a[ofs/4];}
    private static void checkByteCC(long[] a, int ofs)  {long v= a[ofs/8];}
    private static void checkByteCC(float[] a, int ofs) {float v= a[ofs/4];}
    private static void checkByteCC(double[] a, int ofs){double v= a[ofs/8];}
    private static boolean checkByteCC(Object a, int ofs) {
        if (a instanceof char[])  {char v= ((char[])a)[ofs/2]; return true;}
        if (a instanceof byte[])  {byte v= ((byte[])a)[ofs]; return true;}
        if (a instanceof short[]) {short v= ((short[])a)[ofs/2]; return true;}
        if (a instanceof int[])   {int v= ((int[])a)[ofs/4]; return true;}
        if (a instanceof long[])  {long v= ((long[])a)[ofs/8]; return true;}
        if (a instanceof float[]) {float v= ((float[])a)[ofs/4]; return true;}
        if (a instanceof double[]){double v= ((double[])a)[ofs/8]; return true;}
        return false;
    }
    public static void coerciveCopy(char[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (int k=0; k<len; k++,aofs++,bofs++) b[bofs]= (byte)((aofs&1)==0?a[aofs>>>1]:(a[aofs>>>1]>>>8));
    }
    public static void coerciveCopy(byte[] a, int aofs, char[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (int k=0; k<len; k++,aofs++,bofs++) b[bofs>>>1]= (char)((bofs&1)==0?(b[bofs>>>1]&0xFF00)|(a[aofs]&0xFF):(b[bofs>>>1]&0x00FF)|(a[aofs]&0xFF)<<8);
    }
    public static void coerciveCopy(short[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (int k=0; k<len; k++,aofs++,bofs++) b[bofs]= (byte)((aofs&1)==0?a[aofs>>>1]:(a[aofs>>>1]>>>8));
    }
    public static void coerciveCopy(byte[] a, int aofs, short[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (int k=0; k<len; k++,aofs++,bofs++) b[bofs>>>1]= (short)((bofs&1)==0?(b[bofs>>>1]&0xFF00)|(a[aofs]&0xFF):(b[bofs>>>1]&0x00FF)|(a[aofs]&0xFF)<<8);
    }
    public static void coerciveCopy(int[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
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
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
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
    public static void coerciveCopy(long[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (; len>0 && (aofs&7)!=0; len--,aofs++,bofs++) b[bofs]= (byte)(a[aofs>>>3]>>>((aofs&7)<<3));
        int k=aofs>>3, l=len>>3, n=k+l;
        for (; k<n; k++,bofs+=8) {
            long v= a[k];
            b[bofs]= (byte)v;          b[bofs+1]= (byte)(v>>>8);
            b[bofs+2]= (byte)(v>>>16); b[bofs+3]= (byte)(v>>>24);
            b[bofs+4]= (byte)(v>>>32); b[bofs+5]= (byte)(v>>>40);
            b[bofs+6]= (byte)(v>>>48); b[bofs+7]= (byte)(v>>>56);
        }
        len-=(l<<3); aofs+=(l<<3);
        for (; len>0; len--,aofs++,bofs++) b[bofs]= (byte)(a[aofs>>>3]>>>((aofs&7)<<3));
    }
    public static void coerciveCopy(byte[] a, int aofs, long[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (; len>0 && (bofs&7)!=0; len--,aofs++,bofs++) {int sh= (bofs&7)<<3; b[bofs>>>3]= (b[bofs>>>3]&~(0xFFL<<sh))|((a[aofs]&0xFFL)<<sh);}
        int k=bofs>>3, l=len>>3, n=k+l;
        for (; k<n; k++,aofs+=8) {
            b[k]= (a[aofs]&0xFFL) | (a[aofs+1]&0xFFL)<<8
                | (a[aofs+2]&0xFFL)<<16 | (a[aofs+3]&0xFFL)<<24
                | (a[aofs+4]&0xFFL)<<32 | (a[aofs+5]&0xFFL)<<40
                | (a[aofs+6]&0xFFL)<<48 | (a[aofs+7]&0xFFL)<<56;
        }
        len-=(l<<3); bofs+=(l<<3);
        for (; len>0; len--,aofs++,bofs++) {int sh= (bofs&7)<<3; b[bofs>>>3]= (b[bofs>>>3]&~(0xFFL<<sh))|((a[aofs]&0xFFL)<<sh);}
    }
    public static void coerciveCopy(float[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (; len>0 && (aofs&3)!=0; len--,aofs++,bofs++) b[bofs]= (byte)(Float.floatToIntBits(a[aofs>>>2])>>>((aofs&3)<<3));
        int k=aofs>>2, l=len>>2, n=k+l;
        for (; k<n; k++,bofs+=4) {
            int v= Float.floatToIntBits(a[k]);
            b[bofs]= (byte)v;          b[bofs+1]= (byte)(v>>>8);
            b[bofs+2]= (byte)(v>>>16); b[bofs+3]= (byte)(v>>>24);
        }
        len-=(l<<2); aofs+=(l<<2);
        for (; len>0; len--,aofs++,bofs++) b[bofs]= (byte)(Float.floatToIntBits(a[aofs>>>2])>>>((aofs&3)<<3));
    }
    public static void coerciveCopy(byte[] a, int aofs, float[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (; len>0 && (bofs&3)!=0; len--,aofs++,bofs++) {int sh= (bofs&3)<<3; b[bofs>>>2]= Float.intBitsToFloat((Float.floatToIntBits(b[bofs>>>2])&~(0xFF<<sh))|((a[aofs]&0xFF)<<sh));}
        int k=bofs>>2, l=len>>2, n=k+l;
        for (; k<n; k++,aofs+=4) {
            b[k]= Float.intBitsToFloat((a[aofs]&0xFF) | (a[aofs+1]&0xFF)<<8
                | (a[aofs+2]&0xFF)<<16 | (a[aofs+3]&0xFF)<<24);
        }
        len-=(l<<2); bofs+=(l<<2);
        for (; len>0; len--,aofs++,bofs++) {int sh= (bofs&3)<<3; b[bofs>>>2]= Float.intBitsToFloat((Float.floatToIntBits(b[bofs>>>2])&~(0xFF<<sh))|((a[aofs]&0xFF)<<sh));}
    }
    public static void coerciveCopy(double[] a, int aofs, byte[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (; len>0 && (aofs&7)!=0; len--,aofs++,bofs++) b[bofs]= (byte)(Double.doubleToLongBits(a[aofs>>>3])>>>((aofs&7)<<3));
        int k=aofs>>3, l=len>>3, n=k+l;
        for (; k<n; k++,bofs+=8) {
            long v= Double.doubleToLongBits(a[k]);
            b[bofs]= (byte)v;          b[bofs+1]= (byte)(v>>>8);
            b[bofs+2]= (byte)(v>>>16); b[bofs+3]= (byte)(v>>>24);
            b[bofs+4]= (byte)(v>>>32); b[bofs+5]= (byte)(v>>>40);
            b[bofs+6]= (byte)(v>>>48); b[bofs+7]= (byte)(v>>>56);
        }
        len-=(l<<3); aofs+=(l<<3);
        for (; len>0; len--,aofs++,bofs++) b[bofs]= (byte)(Double.doubleToLongBits(a[aofs>>>3])>>>((aofs&7)<<3));
    }
    public static void coerciveCopy(byte[] a, int aofs, double[] b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            checkByteCC(a,aofs); checkByteCC(b,bofs); checkByteCC(a,aofs+len-1); checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        for (; len>0 && (bofs&7)!=0; len--,aofs++,bofs++) {int sh= (bofs&7)<<3; b[bofs>>>3]= Double.longBitsToDouble((Double.doubleToLongBits(b[bofs>>>3])&~(0xFFL<<sh))|((a[aofs]&0xFFL)<<sh));}
        int k=bofs>>3, l=len>>3, n=k+l;
        for (; k<n; k++,aofs+=8) {
            b[k]= Double.longBitsToDouble((a[aofs]&0xFFL) | (a[aofs+1]&0xFFL)<<8
                | (a[aofs+2]&0xFFL)<<16 | (a[aofs+3]&0xFFL)<<24
                | (a[aofs+4]&0xFFL)<<32 | (a[aofs+5]&0xFFL)<<40
                | (a[aofs+6]&0xFFL)<<48 | (a[aofs+7]&0xFFL)<<56);
        }
        len-=(l<<3); bofs+=(l<<3);
        for (; len>0; len--,aofs++,bofs++) {int sh= (bofs&7)<<3; b[bofs>>>3]= Double.longBitsToDouble((Double.doubleToLongBits(b[bofs>>>3])&~(0xFFL<<sh))|((a[aofs]&0xFFL)<<sh));}
    }
    public static void coerciveCopy(Object a, int aofs, Object b, int bofs, int len) {
        if (len<=0) return;
        if (isNative && ArraysNative.copyBytesImplemented && len>nativeMinLenFill) {
            if (!checkByteCC(a,aofs)) throw new IllegalArgumentException("Unsupported a argument type in " + Arrays.class.getName() + ".coerciveCopy(): "+JVM.toJavaClassName(a));
            if (!checkByteCC(b,bofs)) throw new IllegalArgumentException("Unsupported b argument type in " + Arrays.class.getName() + ".coerciveCopy(): "+JVM.toJavaClassName(b));
            checkByteCC(a,aofs+len-1);
            checkByteCC(b,bofs+len-1);
            ArraysNative.copyBytes(ArraysNative.cpuInfo,a,aofs,b,bofs,len);
            return;
        }
        if (a instanceof char[]) {
            if (b instanceof byte[]) coerciveCopy((char[])a,aofs,(byte[])b,bofs,len);
            else coerciveCopy(coerciveCopyBytes((char[])a,aofs,len),0,b,bofs,len);
        } else if (a instanceof byte[]) {
            if (b instanceof char[]) coerciveCopy((byte[])a,aofs,(char[])b,bofs,len);
            else if (b instanceof byte[]) copy((byte[])a,aofs,(byte[])b,bofs,len);
            else if (b instanceof short[]) coerciveCopy((byte[])a,aofs,(short[])b,bofs,len);
            else if (b instanceof int[]) coerciveCopy((byte[])a,aofs,(int[])b,bofs,len);
            else if (b instanceof long[]) coerciveCopy((byte[])a,aofs,(long[])b,bofs,len);
            else if (b instanceof float[]) coerciveCopy((byte[])a,aofs,(float[])b,bofs,len);
            else if (b instanceof double[]) coerciveCopy((byte[])a,aofs,(double[])b,bofs,len);
            else throw new IllegalArgumentException("Unsupported b argument type in " + Arrays.class.getName() + ".coerciveCopy(): "+JVM.toJavaClassName(b));
        } else if (a instanceof short[]) {
            if (b instanceof byte[]) coerciveCopy((short[])a,aofs,(byte[])b,bofs,len);
            else coerciveCopy(coerciveCopyBytes((short[])a,aofs,len),0,b,bofs,len);
        } else if (a instanceof int[]) {
            if (b instanceof byte[]) coerciveCopy((int[])a,aofs,(byte[])b,bofs,len);
            else coerciveCopy(coerciveCopyBytes((int[])a,aofs,len),0,b,bofs,len);
        } else if (a instanceof long[]) {
            if (b instanceof byte[]) coerciveCopy((long[])a,aofs,(byte[])b,bofs,len);
            else coerciveCopy(coerciveCopyBytes((long[])a,aofs,len),0,b,bofs,len);
        } else if (a instanceof float[]) {
            if (b instanceof byte[]) coerciveCopy((float[])a,aofs,(byte[])b,bofs,len);
            else coerciveCopy(coerciveCopyBytes((float[])a,aofs,len),0,b,bofs,len);
        } else if (a instanceof double[]) {
            if (b instanceof byte[]) coerciveCopy((double[])a,aofs,(byte[])b,bofs,len);
            else coerciveCopy(coerciveCopyBytes((double[])a,aofs,len),0,b,bofs,len);
        } else throw new IllegalArgumentException("Unsupported a argument type in " + Arrays.class.getName() + ".coerciveCopy(): "+JVM.toJavaClassName(a));
    }


    /* Converting arrays: int[] to byte[], char[] to long[], etc...  */

    public static boolean[] toBooleans(boolean[] a)     {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(char[] a)        {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(byte[] a)        {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(short[] a)       {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(int[] a)         {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(long[] a)        {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(float[] a)       {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(double[] a)      {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(String[] a)      {return toBooleans(a,0,a.length);}
    public static boolean[] toBooleans(Object a)        {return toBooleans(a,0,length(a));}

    public static byte[] toBytes(boolean[] a)     {return toBytes(a,0,a.length);}
    public static byte[] toBytes(char[] a)        {return toBytes(a,0,a.length);}
    public static byte[] toBytes(byte[] a)        {return toBytes(a,0,a.length);}
    public static byte[] toBytes(short[] a)       {return toBytes(a,0,a.length);}
    public static byte[] toBytes(int[] a)         {return toBytes(a,0,a.length);}
    public static byte[] toBytes(long[] a)        {return toBytes(a,0,a.length);}
    public static byte[] toBytes(float[] a)       {return toBytes(a,0,a.length);}
    public static byte[] toBytes(double[] a)      {return toBytes(a,0,a.length);}
    public static byte[] toBytes(String[] a)      {return toBytes(a,0,a.length);}
    public static byte[] toBytes(Object a)        {return toBytes(a,0,length(a));}

    public static char[] toChars(boolean[] a)     {return toChars(a,0,a.length);}
    public static char[] toChars(char[] a)        {return toChars(a,0,a.length);}
    public static char[] toChars(byte[] a)        {return toChars(a,0,a.length);}
    public static char[] toChars(short[] a)       {return toChars(a,0,a.length);}
    public static char[] toChars(int[] a)         {return toChars(a,0,a.length);}
    public static char[] toChars(long[] a)        {return toChars(a,0,a.length);}
    public static char[] toChars(float[] a)       {return toChars(a,0,a.length);}
    public static char[] toChars(double[] a)      {return toChars(a,0,a.length);}
    public static char[] toChars(String[] a)      {return toChars(a,0,a.length);}
    public static char[] toChars(Object a)        {return toChars(a,0,length(a));}

    public static short[] toShorts(boolean[] a)   {return toShorts(a,0,a.length);}
    public static short[] toShorts(char[] a)      {return toShorts(a,0,a.length);}
    public static short[] toShorts(byte[] a)      {return toShorts(a,0,a.length);}
    public static short[] toShortsU(byte[] a)     {return toShortsU(a,0,a.length);}
    public static short[] toShorts(short[] a)     {return toShorts(a,0,a.length);}
    public static short[] toShorts(int[] a)       {return toShorts(a,0,a.length);}
    public static short[] toShorts(long[] a)      {return toShorts(a,0,a.length);}
    public static short[] toShorts(float[] a)     {return toShorts(a,0,a.length);}
    public static short[] toShorts(double[] a)    {return toShorts(a,0,a.length);}
    public static short[] toShorts(String[] a)    {return toShorts(a,0,a.length);}
    public static short[] toShorts(Object a)      {return toShorts(a,0,length(a));}

    public static int[] toInts(boolean[] a)       {return toInts(a,0,a.length);}
    public static int[] toInts(char[] a)          {return toInts(a,0,a.length);}
    public static int[] toInts(byte[] a)          {return toInts(a,0,a.length);}
    public static int[] toIntsU(byte[] a)         {return toIntsU(a,0,a.length);}
    public static int[] toInts(short[] a)         {return toInts(a,0,a.length);}
    public static int[] toIntsU(short[] a)        {return toIntsU(a,0,a.length);}
    public static int[] toInts(int[] a)           {return toInts(a,0,a.length);}
    public static int[] toInts(long[] a)          {return toInts(a,0,a.length);}
    public static int[] toInts(float[] a)         {return toInts(a,0,a.length);}
    public static int[] toInts(double[] a)        {return toInts(a,0,a.length);}
    public static int[] toInts(String[] a)        {return toInts(a,0,a.length);}
    public static int[] toInts(Object a)          {return toInts(a,0,length(a));}

    public static long[] toLongs(boolean[] a)     {return toLongs(a,0,a.length);}
    public static long[] toLongs(char[] a)        {return toLongs(a,0,a.length);}
    public static long[] toLongs(byte[] a)        {return toLongs(a,0,a.length);}
    public static long[] toLongsU(byte[] a)       {return toLongsU(a,0,a.length);}
    public static long[] toLongs(short[] a)       {return toLongs(a,0,a.length);}
    public static long[] toLongsU(short[] a)      {return toLongsU(a,0,a.length);}
    public static long[] toLongs(int[] a)         {return toLongs(a,0,a.length);}
    public static long[] toLongs(long[] a)        {return toLongs(a,0,a.length);}
    public static long[] toLongs(float[] a)       {return toLongs(a,0,a.length);}
    public static long[] toLongs(double[] a)      {return toLongs(a,0,a.length);}
    public static long[] toLongs(String[] a)      {return toLongs(a,0,a.length);}
    public static long[] toLongs(Object a)        {return toLongs(a,0,length(a));}

    public static float[] toFloats(boolean[] a)   {return toFloats(a,0,a.length);}
    public static float[] toFloats(char[] a)      {return toFloats(a,0,a.length);}
    public static float[] toFloats(byte[] a)      {return toFloats(a,0,a.length);}
    public static float[] toFloatsU(byte[] a)     {return toFloatsU(a,0,a.length);}
    public static float[] toFloats(short[] a)     {return toFloats(a,0,a.length);}
    public static float[] toFloatsU(short[] a)    {return toFloatsU(a,0,a.length);}
    public static float[] toFloats(int[] a)       {return toFloats(a,0,a.length);}
    public static float[] toFloats(long[] a)      {return toFloats(a,0,a.length);}
    public static float[] toFloats(float[] a)     {return toFloats(a,0,a.length);}
    public static float[] toFloats(double[] a)    {return toFloats(a,0,a.length);}
    public static float[] toFloats(String[] a)    {return toFloats(a,0,a.length);}
    public static float[] toFloats(Object a)      {return toFloats(a,0,length(a));}

    public static double[] toDoubles(boolean[] a) {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(char[] a)    {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(byte[] a)    {return toDoubles(a,0,a.length);}
    public static double[] toDoublesU(byte[] a)   {return toDoublesU(a,0,a.length);}
    public static double[] toDoubles(short[] a)   {return toDoubles(a,0,a.length);}
    public static double[] toDoublesU(short[] a)  {return toDoublesU(a,0,a.length);}
    public static double[] toDoubles(int[] a)     {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(long[] a)    {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(float[] a)   {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(double[] a)  {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(String[] a)  {return toDoubles(a,0,a.length);}
    public static double[] toDoubles(Object a)    {return toDoubles(a,0,length(a));}

    public static String[] toStrings(boolean[] a) {return toStrings(a,0,a.length);}
    public static String[] toStrings(char[] a)    {return toStrings(a,0,a.length);}
    public static String[] toStrings(byte[] a)    {return toStrings(a,0,a.length);}
    public static String[] toStringsU(byte[] a)   {return toStringsU(a,0,a.length);}
    public static String[] toStrings(short[] a)   {return toStrings(a,0,a.length);}
    public static String[] toStringsU(short[] a)  {return toStringsU(a,0,a.length);}
    public static String[] toStrings(int[] a)     {return toStrings(a,0,a.length);}
    public static String[] toStrings(long[] a)    {return toStrings(a,0,a.length);}
    public static String[] toStrings(float[] a)   {return toStrings(a,0,a.length);}
    public static String[] toStrings(double[] a)  {return toStrings(a,0,a.length);}
    public static String[] toStrings(String[] a)  {return toStrings(a,0,a.length);}
    public static String[] toStrings(Object a)    {return toStrings(a,0,length(a));}

    public static Object[] toObjects(boolean[] a) {return toObjects(a,0,a.length);}
    public static Object[] toObjects(char[] a)    {return toObjects(a,0,a.length);}
    public static Object[] toObjects(byte[] a)    {return toObjects(a,0,a.length);}
    public static Object[] toObjectsU(byte[] a)   {return toObjectsU(a,0,a.length);}
    public static Object[] toObjects(short[] a)   {return toObjects(a,0,a.length);}
    public static Object[] toObjectsU(short[] a)  {return toObjectsU(a,0,a.length);}
    public static Object[] toObjects(int[] a)     {return toObjects(a,0,a.length);}
    public static Object[] toObjects(long[] a)    {return toObjects(a,0,a.length);}
    public static Object[] toObjects(float[] a)   {return toObjects(a,0,a.length);}
    public static Object[] toObjects(double[] a)  {return toObjects(a,0,a.length);}
    public static Object[] toObjects(String[] a)  {return toObjects(a,0,a.length);}
    public static Object[] toObjects(Object a)    {return toObjects(a,0,length(a));}


    public static boolean[] toBooleans(boolean[] a, int aofs, int len)     {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(char[] a, int aofs, int len)        {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(byte[] a, int aofs, int len)        {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(short[] a, int aofs, int len)       {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(int[] a, int aofs, int len)         {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(long[] a, int aofs, int len)        {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(float[] a, int aofs, int len)       {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(double[] a, int aofs, int len)      {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(String[] a, int aofs, int len)      {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}
    public static boolean[] toBooleans(Object a, int aofs, int len)        {boolean[] r= new boolean[len]; toBooleans(a,aofs,r,0,len); return r;}

    public static byte[] toBytes(boolean[] a, int aofs, int len)     {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(char[] a, int aofs, int len)        {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(byte[] a, int aofs, int len)        {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(short[] a, int aofs, int len)       {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(int[] a, int aofs, int len)         {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(long[] a, int aofs, int len)        {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(float[] a, int aofs, int len)       {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(double[] a, int aofs, int len)      {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(String[] a, int aofs, int len)      {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}
    public static byte[] toBytes(Object a, int aofs, int len)        {byte[] r= new byte[len]; toBytes(a,aofs,r,0,len); return r;}

    public static char[] toChars(boolean[] a, int aofs, int len)     {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(char[] a, int aofs, int len)        {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(byte[] a, int aofs, int len)        {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(short[] a, int aofs, int len)       {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(int[] a, int aofs, int len)         {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(long[] a, int aofs, int len)        {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(float[] a, int aofs, int len)       {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(double[] a, int aofs, int len)      {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(String[] a, int aofs, int len)      {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}
    public static char[] toChars(Object a, int aofs, int len)        {char[] r= new char[len]; toChars(a,aofs,r,0,len); return r;}

    public static short[] toShorts(boolean[] a, int aofs, int len)   {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(char[] a, int aofs, int len)      {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(byte[] a, int aofs, int len)      {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShortsU(byte[] a, int aofs, int len)     {short[] r= new short[len]; toShortsU(a,aofs,r,0,len); return r;}
    public static short[] toShorts(short[] a, int aofs, int len)     {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(int[] a, int aofs, int len)       {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(long[] a, int aofs, int len)      {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(float[] a, int aofs, int len)     {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(double[] a, int aofs, int len)    {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(String[] a, int aofs, int len)    {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}
    public static short[] toShorts(Object a, int aofs, int len)      {short[] r= new short[len]; toShorts(a,aofs,r,0,len); return r;}

    public static int[] toInts(boolean[] a, int aofs, int len)       {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(char[] a, int aofs, int len)          {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(byte[] a, int aofs, int len)          {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toIntsU(byte[] a, int aofs, int len)         {int[] r= new int[len]; toIntsU(a,aofs,r,0,len); return r;}
    public static int[] toInts(short[] a, int aofs, int len)         {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toIntsU(short[] a, int aofs, int len)        {int[] r= new int[len]; toIntsU(a,aofs,r,0,len); return r;}
    public static int[] toInts(int[] a, int aofs, int len)           {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(long[] a, int aofs, int len)          {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(float[] a, int aofs, int len)         {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(double[] a, int aofs, int len)        {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(String[] a, int aofs, int len)        {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}
    public static int[] toInts(Object a, int aofs, int len)          {int[] r= new int[len]; toInts(a,aofs,r,0,len); return r;}

    public static long[] toLongs(boolean[] a, int aofs, int len)     {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(char[] a, int aofs, int len)        {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(byte[] a, int aofs, int len)        {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongsU(byte[] a, int aofs, int len)       {long[] r= new long[len]; toLongsU(a,aofs,r,0,len); return r;}
    public static long[] toLongs(short[] a, int aofs, int len)       {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongsU(short[] a, int aofs, int len)      {long[] r= new long[len]; toLongsU(a,aofs,r,0,len); return r;}
    public static long[] toLongs(int[] a, int aofs, int len)         {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(long[] a, int aofs, int len)        {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(float[] a, int aofs, int len)       {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(double[] a, int aofs, int len)      {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(String[] a, int aofs, int len)      {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}
    public static long[] toLongs(Object a, int aofs, int len)        {long[] r= new long[len]; toLongs(a,aofs,r,0,len); return r;}

    public static float[] toFloats(boolean[] a, int aofs, int len)   {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(char[] a, int aofs, int len)      {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(byte[] a, int aofs, int len)      {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloatsU(byte[] a, int aofs, int len)     {float[] r= new float[len]; toFloatsU(a,aofs,r,0,len); return r;}
    public static float[] toFloats(short[] a, int aofs, int len)     {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloatsU(short[] a, int aofs, int len)    {float[] r= new float[len]; toFloatsU(a,aofs,r,0,len); return r;}
    public static float[] toFloats(int[] a, int aofs, int len)       {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(long[] a, int aofs, int len)      {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(float[] a, int aofs, int len)     {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(double[] a, int aofs, int len)    {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(String[] a, int aofs, int len)    {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}
    public static float[] toFloats(Object a, int aofs, int len)      {float[] r= new float[len]; toFloats(a,aofs,r,0,len); return r;}

    public static double[] toDoubles(boolean[] a, int aofs, int len) {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(char[] a, int aofs, int len)    {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(byte[] a, int aofs, int len)    {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoublesU(byte[] a, int aofs, int len)   {double[] r= new double[len]; toDoublesU(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(short[] a, int aofs, int len)   {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoublesU(short[] a, int aofs, int len)  {double[] r= new double[len]; toDoublesU(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(int[] a, int aofs, int len)     {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(long[] a, int aofs, int len)    {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(float[] a, int aofs, int len)   {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(double[] a, int aofs, int len)  {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(String[] a, int aofs, int len)  {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}
    public static double[] toDoubles(Object a, int aofs, int len)    {double[] r= new double[len]; toDoubles(a,aofs,r,0,len); return r;}

    public static String[] toStrings(boolean[] a, int aofs, int len) {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(char[] a, int aofs, int len)    {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(byte[] a, int aofs, int len)    {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStringsU(byte[] a, int aofs, int len)   {String[] r= new String[len]; toStringsU(a,aofs,r,0,len); return r;}
    public static String[] toStrings(short[] a, int aofs, int len)   {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStringsU(short[] a, int aofs, int len)  {String[] r= new String[len]; toStringsU(a,aofs,r,0,len); return r;}
    public static String[] toStrings(int[] a, int aofs, int len)     {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(long[] a, int aofs, int len)    {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(float[] a, int aofs, int len)   {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(double[] a, int aofs, int len)  {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(String[] a, int aofs, int len)  {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}
    public static String[] toStrings(Object a, int aofs, int len)    {String[] r= new String[len]; toStrings(a,aofs,r,0,len); return r;}

    public static Object[] toObjects(boolean[] a, int aofs, int len) {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(char[] a, int aofs, int len)    {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(byte[] a, int aofs, int len)    {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjectsU(byte[] a, int aofs, int len)   {Object[] r= new Object[len]; toObjectsU(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(short[] a, int aofs, int len)   {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjectsU(short[] a, int aofs, int len)  {Object[] r= new Object[len]; toObjectsU(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(int[] a, int aofs, int len)     {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(long[] a, int aofs, int len)    {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(float[] a, int aofs, int len)   {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(double[] a, int aofs, int len)  {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(String[] a, int aofs, int len)  {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}
    public static Object[] toObjects(Object a, int aofs, int len)    {Object[] r= new Object[len]; toObjects(a,aofs,r,0,len); return r;}


    public static void toBooleans(boolean[] a, boolean[] b)     {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(char[] a, boolean[] b)        {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(byte[] a, boolean[] b)        {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(short[] a, boolean[] b)       {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(int[] a, boolean[] b)         {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(long[] a, boolean[] b)        {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(float[] a, boolean[] b)       {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(double[] a, boolean[] b)      {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(String[] a, boolean[] b)      {toBooleans(a,0,b,0,min(a.length,b.length));}
    public static void toBooleans(Object a, boolean[] b)        {toBooleans(a,0,b,0,min(length(a),b.length));}

    public static void toBytes(boolean[] a, byte[] b)     {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(char[] a, byte[] b)        {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(byte[] a, byte[] b)        {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(short[] a, byte[] b)       {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(int[] a, byte[] b)         {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(long[] a, byte[] b)        {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(float[] a, byte[] b)       {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(double[] a, byte[] b)      {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(String[] a, byte[] b)      {toBytes(a,0,b,0,min(a.length,b.length));}
    public static void toBytes(Object a, byte[] b)        {toBytes(a,0,b,0,min(length(a),b.length));}

    public static void toChars(boolean[] a, char[] b)     {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(char[] a, char[] b)        {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(byte[] a, char[] b)        {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(short[] a, char[] b)       {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(int[] a, char[] b)         {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(long[] a, char[] b)        {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(float[] a, char[] b)       {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(double[] a, char[] b)      {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(String[] a, char[] b)      {toChars(a,0,b,0,min(a.length,b.length));}
    public static void toChars(Object a, char[] b)        {toChars(a,0,b,0,min(length(a),b.length));}

    public static void toShorts(boolean[] a, short[] b)   {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(char[] a, short[] b)      {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(byte[] a, short[] b)      {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShortsU(byte[] a, short[] b)     {toShortsU(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(short[] a, short[] b)     {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(int[] a, short[] b)       {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(long[] a, short[] b)      {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(float[] a, short[] b)     {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(double[] a, short[] b)    {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(String[] a, short[] b)    {toShorts(a,0,b,0,min(a.length,b.length));}
    public static void toShorts(Object a, short[] b)      {toShorts(a,0,b,0,min(length(a),b.length));}

    public static void toInts(boolean[] a, int[] b)       {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(char[] a, int[] b)          {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(byte[] a, int[] b)          {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toIntsU(byte[] a, int[] b)         {toIntsU(a,0,b,0,min(a.length,b.length));}
    public static void toInts(short[] a, int[] b)         {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toIntsU(short[] a, int[] b)        {toIntsU(a,0,b,0,min(a.length,b.length));}
    public static void toInts(int[] a, int[] b)           {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(long[] a, int[] b)          {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(float[] a, int[] b)         {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(double[] a, int[] b)        {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(String[] a, int[] b)        {toInts(a,0,b,0,min(a.length,b.length));}
    public static void toInts(Object a, int[] b)          {toInts(a,0,b,0,min(length(a),b.length));}

    public static void toLongs(boolean[] a, long[] b)     {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(char[] a, long[] b)        {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(byte[] a, long[] b)        {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongsU(byte[] a, long[] b)       {toLongsU(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(short[] a, long[] b)       {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongsU(short[] a, long[] b)      {toLongsU(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(int[] a, long[] b)         {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(long[] a, long[] b)        {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(float[] a, long[] b)       {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(double[] a, long[] b)      {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(String[] a, long[] b)      {toLongs(a,0,b,0,min(a.length,b.length));}
    public static void toLongs(Object a, long[] b)        {toLongs(a,0,b,0,min(length(a),b.length));}

    public static void toFloats(boolean[] a, float[] b)   {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(char[] a, float[] b)      {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(byte[] a, float[] b)      {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloatsU(byte[] a, float[] b)     {toFloatsU(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(short[] a, float[] b)     {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloatsU(short[] a, float[] b)    {toFloatsU(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(int[] a, float[] b)       {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(long[] a, float[] b)      {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(float[] a, float[] b)     {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(double[] a, float[] b)    {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(String[] a, float[] b)    {toFloats(a,0,b,0,min(a.length,b.length));}
    public static void toFloats(Object a, float[] b)      {toFloats(a,0,b,0,min(length(a),b.length));}

    public static void toDoubles(boolean[] a, double[] b) {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(char[] a, double[] b)    {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(byte[] a, double[] b)    {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoublesU(byte[] a, double[] b)   {toDoublesU(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(short[] a, double[] b)   {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoublesU(short[] a, double[] b)  {toDoublesU(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(int[] a, double[] b)     {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(long[] a, double[] b)    {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(float[] a, double[] b)   {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(double[] a, double[] b)  {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(String[] a, double[] b)  {toDoubles(a,0,b,0,min(a.length,b.length));}
    public static void toDoubles(Object a, double[] b)    {toDoubles(a,0,b,0,min(length(a),b.length));}

    public static void toStrings(boolean[] a, String[] b) {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(char[] a, String[] b)    {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(byte[] a, String[] b)    {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStringsU(byte[] a, String[] b)   {toStringsU(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(short[] a, String[] b)   {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStringsU(short[] a, String[] b)  {toStringsU(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(int[] a, String[] b)     {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(long[] a, String[] b)    {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(float[] a, String[] b)   {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(double[] a, String[] b)  {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(String[] a, String[] b)  {toStrings(a,0,b,0,min(a.length,b.length));}
    public static void toStrings(Object a, String[] b)    {toStrings(a,0,b,0,min(length(a),b.length));}

    public static void toObjects(boolean[] a, Object[] b) {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(char[] a, Object[] b)    {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(byte[] a, Object[] b)    {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjectsU(byte[] a, Object[] b)   {toObjectsU(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(short[] a, Object[] b)   {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjectsU(short[] a, Object[] b)  {toObjectsU(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(int[] a, Object[] b)     {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(long[] a, Object[] b)    {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(float[] a, Object[] b)   {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(double[] a, Object[] b)  {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(String[] a, Object[] b)  {toObjects(a,0,b,0,min(a.length,b.length));}
    public static void toObjects(Object a, Object[] b)    {toObjects(a,0,b,0,min(length(a),b.length));}


    public static void toBooleans(boolean[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toBooleans(char[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(byte[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(short[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(int[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(long[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(float[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(double[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++]!=0;}
    public static void toBooleans(String[] a, int aofs, boolean[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) b[bofs++]= a[aofs++]!=null;}
    public static void toBooleans(Object a, int aofs, boolean[] b, int bofs, int len) {
        if (a instanceof boolean[]) toBooleans((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toBooleans((char[])a,aofs,b,bofs,len);
        else if (a instanceof boolean[]) toBooleans((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toBooleans((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toBooleans((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toBooleans((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toBooleans((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toBooleans((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toBooleans((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) b[bofs]= Array.get(a,aofs)!=null;
    }

    public static void toBytes(boolean[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)(a[aofs++]?1:0);}
    public static void toBytes(char[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)a[aofs++];}
    public static void toBytes(byte[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)a[aofs++];}
    public static void toBytes(short[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)a[aofs++];}
    public static void toBytes(int[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)a[aofs++];}
    public static void toBytes(long[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)a[aofs++];}
    public static void toBytes(float[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)Math.round(a[aofs++]);}
    public static void toBytes(double[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (byte)Math.round(a[aofs++]);}
    public static void toBytes(String[] a, int aofs, byte[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (byte)Integer.parseInt(a[aofs]);} catch (NumberFormatException e){}}
    public static void toBytes(Object a, int aofs, byte[] b, int bofs, int len) {
        if (a instanceof boolean[]) toBytes((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toBytes((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toBytes((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toBytes((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toBytes((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toBytes((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toBytes((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toBytes((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toBytes((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (byte)Integer.parseInt(String.valueOf(Array.get(a,aofs)));} catch (NumberFormatException e){}
    }

    public static void toChars(boolean[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)(a[aofs++]?1:0);}
    public static void toChars(char[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toChars(byte[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)(a[aofs++]&0xFF);}
    public static void toChars(short[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toChars(int[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toChars(long[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toChars(float[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)Math.round(a[aofs++]);}
    public static void toChars(double[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)Math.round(a[aofs++]);}
    public static void toChars(String[] a, int aofs, char[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (char)Integer.parseInt(a[aofs]);} catch (NumberFormatException e){}}
    public static void toChars(Object a, int aofs, char[] b, int bofs, int len) {
        if (a instanceof boolean[]) toChars((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toChars((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toChars((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toChars((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toChars((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toChars((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toChars((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toChars((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toChars((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (char)Integer.parseInt(String.valueOf(Array.get(a,aofs)));} catch (NumberFormatException e){}
    }

    public static void toShorts(boolean[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)(a[aofs++]?1:0);}
    public static void toShorts(char[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)a[aofs++];}
    public static void toShorts(byte[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)a[aofs++];}
    public static void toShortsU(byte[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)(a[aofs++]&0xFF);}
    public static void toShorts(short[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)a[aofs++];}
    public static void toShorts(int[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)a[aofs++];}
    public static void toShorts(long[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)a[aofs++];}
    public static void toShorts(float[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)Math.round(a[aofs++]);}
    public static void toShorts(double[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (short)Math.round(a[aofs++]);}
    public static void toShorts(String[] a, int aofs, short[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (short)Integer.parseInt(a[aofs]);} catch (NumberFormatException e){}}
    public static void toShorts(Object a, int aofs, short[] b, int bofs, int len) {
        if (a instanceof boolean[]) toShorts((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toShorts((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toShorts((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toShorts((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toShorts((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toShorts((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toShorts((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toShorts((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toShorts((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (short)Integer.parseInt(String.valueOf(Array.get(a,aofs)));} catch (NumberFormatException e){}
    }

    public static void toInts(boolean[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)(a[aofs++]?1:0);}
    public static void toInts(char[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)a[aofs++];}
    public static void toInts(byte[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)a[aofs++];}
    public static void toIntsU(byte[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)(a[aofs++]&0xFF);}
    public static void toInts(short[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)a[aofs++];}
    public static void toIntsU(short[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toInts(int[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)a[aofs++];}
    public static void toInts(long[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)a[aofs++];}
    public static void toInts(float[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)Math.round(a[aofs++]);}
    public static void toInts(double[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (int)Math.round(a[aofs++]);}
    public static void toInts(String[] a, int aofs, int[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (int)Integer.parseInt(a[aofs]);} catch (NumberFormatException e){}}
    public static void toInts(Object a, int aofs, int[] b, int bofs, int len) {
        if (a instanceof boolean[]) toInts((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toInts((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toInts((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toInts((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toInts((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toInts((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toInts((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toInts((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toInts((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= (int)Integer.parseInt(String.valueOf(Array.get(a,aofs)));} catch (NumberFormatException e){}
    }

    public static void toLongs(boolean[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]?1:0);}
    public static void toLongs(char[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toLongs(byte[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toLongsU(byte[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]&0xFF);}
    public static void toLongs(short[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toLongsU(short[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toLongs(int[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toLongs(long[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toLongs(float[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (long)Math.round((double)a[aofs++]);}
    public static void toLongs(double[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (long)Math.round(a[aofs++]);}
    public static void toLongs(String[] a, int aofs, long[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Long.parseLong(a[aofs]);} catch (NumberFormatException e){}}
    public static void toLongs(Object a, int aofs, long[] b, int bofs, int len) {
        if (a instanceof boolean[]) toLongs((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toLongs((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toLongs((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toLongs((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toLongs((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toLongs((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toLongs((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toLongs((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toLongs((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Long.parseLong(String.valueOf(Array.get(a,aofs)));} catch (NumberFormatException e){}
    }

    public static void toFloats(boolean[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]?1:0);}
    public static void toFloats(char[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toFloats(byte[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toFloatsU(byte[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]&0xFF);}
    public static void toFloats(short[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toFloatsU(short[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toFloats(int[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toFloats(long[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toFloats(float[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toFloats(double[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (float)a[aofs++];}
    public static void toFloats(String[] a, int aofs, float[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Float.valueOf(a[aofs]).floatValue();} catch (NumberFormatException e){}}
    public static void toFloats(Object a, int aofs, float[] b, int bofs, int len) {
        if (a instanceof boolean[]) toFloats((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toFloats((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toFloats((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toFloats((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toFloats((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toFloats((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toFloats((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toFloats((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toFloats((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Float.valueOf(String.valueOf(Array.get(a,aofs))).floatValue();} catch (NumberFormatException e){}
    }

    public static void toDoubles(boolean[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]?1:0);}
    public static void toDoubles(char[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoubles(byte[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoublesU(byte[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]&0xFF);}
    public static void toDoubles(short[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoublesU(short[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (char)a[aofs++];}
    public static void toDoubles(int[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoubles(long[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoubles(float[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoubles(double[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= a[aofs++];}
    public static void toDoubles(String[] a, int aofs, double[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Double.valueOf(a[aofs]).doubleValue();} catch (NumberFormatException e){}}
    public static void toDoubles(Object a, int aofs, double[] b, int bofs, int len) {
        if (a instanceof boolean[]) toDoubles((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toDoubles((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toDoubles((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toDoubles((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toDoubles((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toDoubles((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toDoubles((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toDoubles((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toDoubles((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Double.valueOf(String.valueOf(Array.get(a,aofs))).doubleValue();} catch (NumberFormatException e){}
    }

    public static void toStrings(boolean[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= (a[aofs++]?"1":"0");}
    public static void toStrings(char[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf((int)a[aofs++]);}
    public static void toStrings(byte[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]);}
    public static void toStringsU(byte[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]&0xFF);}
    public static void toStrings(short[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]);}
    public static void toStringsU(short[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf((char)a[aofs++]);}
    public static void toStrings(int[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]);}
    public static void toStrings(long[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]);}
    public static void toStrings(float[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]);}
    public static void toStrings(double[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= String.valueOf(a[aofs++]);}
    public static void toStrings(String[] a, int aofs, String[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) b[bofs]= a[aofs];}
    public static void toStrings(Object a, int aofs, String[] b, int bofs, int len) {
        if (a instanceof boolean[]) toStrings((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toStrings((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toStrings((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toStrings((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toStrings((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toStrings((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toStrings((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toStrings((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toStrings((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= String.valueOf(Array.get(a,aofs));} catch (NumberFormatException e){}
    }

    public static void toObjects(boolean[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Boolean(a[aofs++]);}
    public static void toObjects(char[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Character(a[aofs++]);}
    public static void toObjects(byte[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Byte(a[aofs++]);}
    public static void toObjectsU(byte[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Integer(a[aofs++]&0xFF);}
    public static void toObjects(short[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Short(a[aofs++]);}
    public static void toObjectsU(short[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Integer((char)a[aofs++]);}
    public static void toObjects(int[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Integer(a[aofs++]);}
    public static void toObjects(long[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Long(a[aofs++]);}
    public static void toObjects(float[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Float(a[aofs++]);}
    public static void toObjects(double[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++) b[bofs++]= new Double(a[aofs++]);}
    public static void toObjects(String[] a, int aofs, Object[] b, int bofs, int len) {for (int k=0; k<len; k++,aofs++,bofs++) b[bofs]= a[aofs];}
    public static void toObjects(Object a, int aofs, Object[] b, int bofs, int len) {
        if (a instanceof boolean[]) toObjects((boolean[])a,aofs,b,bofs,len);
        else if (a instanceof char[]) toObjects((char[])a,aofs,b,bofs,len);
        else if (a instanceof byte[]) toObjects((byte[])a,aofs,b,bofs,len);
        else if (a instanceof short[]) toObjects((short[])a,aofs,b,bofs,len);
        else if (a instanceof int[]) toObjects((int[])a,aofs,b,bofs,len);
        else if (a instanceof long[]) toObjects((long[])a,aofs,b,bofs,len);
        else if (a instanceof float[]) toObjects((float[])a,aofs,b,bofs,len);
        else if (a instanceof double[]) toObjects((double[])a,aofs,b,bofs,len);
        else if (a instanceof String[]) toObjects((String[])a,aofs,b,bofs,len);
        else for (int k=0; k<len; k++,aofs++,bofs++) try {b[bofs]= Array.get(a,aofs);} catch (NumberFormatException e){}
    }


    /* Filling arrays  */

    public static boolean[] constantBooleans(int len, boolean v) {
        boolean[] a= new boolean[len]; fill(a,v); return a;
    }
    public static char[] constantChars(int len, char v) {
        char[] a= new char[len]; fill(a,v); return a;
    }
    public static byte[] constantBytes(int len, byte v) {
        byte[] a= new byte[len]; fill(a,v); return a;
    }
    public static short[] constantShorts(int len, short v) {
        short[] a= new short[len]; fill(a,v); return a;
    }
    public static int[] constantInts(int len, int v) {
        int[] a= new int[len]; fill(a,v); return a;
    }
    public static long[] constantLongs(int len, long v) {
        long[] a= new long[len]; fill(a,v); return a;
    }
    public static float[] constantFloats(int len, float v) {
        float[] a= new float[len]; fill(a,v); return a;
    }
    public static double[] constantDoubles(int len, double v) {
        double[] a= new double[len]; fill(a,v); return a;
    }
    public static String[] constantStrings(int len, String v) {
        String[] a= new String[len]; fill(a,v); return a;
    }
    public static Object[] constantObjects(int len, Object v) {
        Object[] a = (Object[])Array.newInstance(v == null? Object.class: v.getClass(),len);
        fill(a,v); return a;
    }

    public static void fill(boolean[] a, boolean v) {fill(a, 0, a.length, v);}
    public static void fill(char[] a, char v)       {fill(a, 0, a.length, v);}
    public static void fill(byte[] a, byte v)       {fill(a, 0, a.length, v);}
    public static void fill(short[] a, short v)     {fill(a, 0, a.length, v);}
    public static void fill(int[] a, int v)         {fill(a, 0, a.length, v);}
    public static void fill(long[] a, long v)       {fill(a, 0, a.length, v);}
    public static void fill(float[] a, float v)     {fill(a, 0, a.length, v);}
    public static void fill(double[] a, double v)   {fill(a, 0, a.length, v);}
    public static void fill(Object[] a, Object v)   {fill(a, 0, a.length, v);}

    public static void fill(boolean[] a, int beginIndex, int endIndex, boolean v) {
        // We are not using native here: Java doesn't specify sizeof(boolean)
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(char[] a, int beginIndex, int endIndex, char v) {
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(byte[] a, int beginIndex, int endIndex, byte v) {
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(short[] a, int beginIndex, int endIndex, short v) {
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(int[] a, int beginIndex, int endIndex, int v) {
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }

    public static void fill(long[] a, int beginIndex, int endIndex, long v) {
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(float[] a, int beginIndex, int endIndex, float v) {
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(double[] a, int beginIndex, int endIndex,double v){
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }
    public static void fill(Object[] a, int beginIndex, int endIndex, Object v){
        if (isNative && ArraysNative.fillImplemented && endIndex-beginIndex>nativeMinLenFill) {
            a[beginIndex]= a[endIndex-1]= v;
            ArraysNative.fill(ArraysNative.cpuInfo,a,beginIndex,endIndex,v);
            return;
        }
        for (int k=beginIndex; k<endIndex; k++) a[k] = v;
    }


    public static boolean[] randomBooleans(int len) {
        return randomBooleans(len,rnd);
    }
    public static boolean[] randomBooleans(int len, Random rnd) {
        boolean a[]= new boolean[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static char[] randomChars(int len) {
        return randomChars(len,rnd);
    }
    public static char[] randomChars(int len, Random rnd) {
        char a[]= new char[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static byte[] randomBytes(int len) {
        return randomBytes(len,rnd);
    }
    public static byte[] randomBytes(int len, Random rnd) {
        byte a[]= new byte[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static short[] randomShorts(int len) {
        return randomShorts(len,rnd);
    }
    public static short[] randomShorts(int len, Random rnd) {
        short a[]= new short[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static int[] randomInts(int len) {
        return randomInts(len,rnd);
    }
    public static int[] randomInts(int len, Random rnd) {
        int a[]= new int[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static long[] randomLongs(int len) {
        return randomLongs(len,rnd);
    }
    public static long[] randomLongs(int len, Random rnd) {
        long a[]= new long[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static float[] randomFloats(int len) {
        return randomFloats(len,rnd);
    }
    public static float[] randomFloats(int len, Random rnd) {
        float a[]= new float[len]; fillRandom(a,0,len,rnd); return a;
    }
    public static double[] randomDoubles(int len) {
        return randomDoubles(len,rnd);
    }
    public static double[] randomDoubles(int len, Random rnd) {
        double a[]= new double[len]; fillRandom(a,0,len,rnd); return a;
    }

    public static void fillRandom(Object a) {
        fillRandom(a,0,Array.getLength(a),rnd);
    }
    public static void fillRandom(Object a, long seed) {
        fillRandom(a,0,Array.getLength(a),new Random(seed));
    }
    public static void fillRandom(Object a, Random rnd) {
        fillRandom(a,0,Array.getLength(a),rnd);
    }
    public static void fillRandom(Object a, int beginIndex, int endIndex) {
        fillRandom(a,beginIndex,endIndex,rnd);
    }
    public static void fillRandom(Object a, int beginIndex, int endIndex, long seed) {
        fillRandom(a,beginIndex,endIndex,new Random(seed));
    }
    public static void fillRandom(Object a, int beginIndex, int endIndex, Random rnd) {
        if (a instanceof boolean[]) fillRandom((boolean[])a,beginIndex,endIndex,rnd);
        else if (a instanceof char[]) fillRandom((char[])a,beginIndex,endIndex,rnd);
        else if (a instanceof byte[]) fillRandom((byte[])a,beginIndex,endIndex,rnd);
        else if (a instanceof short[]) fillRandom((short[])a,beginIndex,endIndex,rnd);
        else if (a instanceof int[]) fillRandom((int[])a,beginIndex,endIndex,rnd);
        else if (a instanceof long[]) fillRandom((long[])a,beginIndex,endIndex,rnd);
        else if (a instanceof float[]) fillRandom((float[])a,beginIndex,endIndex,rnd);
        else if (a instanceof double[]) fillRandom((double[])a,beginIndex,endIndex,rnd);
        else throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".fillRandom(): "+JVM.toJavaClassName(a));
    }
    public static void fillRandom(boolean[] a, int beginIndex, int endIndex, Random rnd) {
        for (int k=beginIndex; k<endIndex; k++) a[k]= rnd.nextInt()>=0;
    }
    public static void fillRandom(char[] a, int beginIndex, int endIndex, Random rnd) {
        int k;
        for (k=beginIndex; k<endIndex-1; k+=2) {
            int v= rnd.nextInt();
            a[k]= (char)v;
            a[k+1]= (char)(v>>>16);
        }
        if (k<endIndex) a[k]= (char)rnd.nextInt();
    }
    public static void fillRandom(byte[] a, int beginIndex, int endIndex, Random rnd) {
        int k;
        for (k=beginIndex; k<endIndex-3; k+=4) {
            int v= rnd.nextInt();
            a[k]= (byte)v;
            a[k+1]= (byte)(v>>>8);
            a[k+2]= (byte)(v>>>16);
            a[k+3]= (byte)(v>>>24);
        }
        if (k<endIndex) {
            int v= rnd.nextInt();
            a[k++]= (byte)v;
            if (k<endIndex) {
                a[k++]= (byte)(v>>>8);
                if (k<endIndex) {
                    a[k++]= (byte)(v>>>16);
                }
            }
        }
    }
    public static void fillRandom(short[] a, int beginIndex, int endIndex, Random rnd) {
        int k;
        for (k=beginIndex; k<endIndex-1; k+=2) {
            int v= rnd.nextInt();
            a[k]= (short)v;
            a[k+1]= (short)(v>>>16);
        }
        if (k<endIndex) a[k]= (short)rnd.nextInt();
    }
    public static void fillRandom(int[] a, int beginIndex, int endIndex, Random rnd) {
        for (int k=beginIndex; k<endIndex; k++) a[k]= rnd.nextInt();
    }
    public static void fillRandom(long[] a, int beginIndex, int endIndex, Random rnd) {
        for (int k=beginIndex; k<endIndex; k++) a[k]= rnd.nextLong();
    }
    public static void fillRandom(float[] a, int beginIndex, int endIndex, Random rnd) {
        for (int k=beginIndex; k<endIndex; k++) a[k]= rnd.nextFloat();
    }
    public static void fillRandom(double[] a, int beginIndex, int endIndex, Random rnd) {
        for (int k=beginIndex; k<endIndex; k++) a[k]= rnd.nextDouble();
    }


    /* Comparing arrays, searcing in arrays, delete/insert/replace  */

    public static boolean equals(Object a, Object b) {
        return equals(a,0,b,0,max(Array.getLength(a),Array.getLength(b)));
    }
    public static boolean equals(boolean[] a, int aofs, boolean[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(char[] a, int aofs, char[] b, int bofs, int length) {
    //!! TODO: quick native
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(byte[] a, int aofs, byte[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(short[] a, int aofs, short[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(int[] a, int aofs, int[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(long[] a, int aofs, long[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(float[] a, int aofs, float[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(double[] a, int aofs, double[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]!=b[bofs]) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(Object[] a, int aofs, Object[] b, int bofs, int length) {
        try {
            for (; length>0; aofs++,bofs++,length--) if (a[aofs]==null? b[bofs]!=null: !a[aofs].equals(b[bofs])) return false;
            return true;
        } catch (Exception e) {return false;
        }
    }
    public static boolean equals(Object a, int aofs, Object b, int bofs, int len) {
        if (a instanceof boolean[]) return equals((boolean[])a,aofs,(boolean[])b,bofs,len);
        else if (a instanceof char[]) return equals((char[])a,aofs,(char[])b,bofs,len);
        else if (a instanceof byte[]) return equals((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[]) return equals((short[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof int[]) return equals((int[])a,aofs,(int[])b,bofs,len);
        else if (a instanceof long[]) return equals((long[])a,aofs,(long[])b,bofs,len);
        else if (a instanceof float[]) return equals((float[])a,aofs,(float[])b,bofs,len);
        else if (a instanceof double[]) return equals((double[])a,aofs,(double[])b,bofs,len);
        else if (a instanceof Object[]) return equals((Object[])a,aofs,(Object[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".equals(): "+JVM.toJavaClassName(a));
    }

    public static int indexOf(boolean[] a, boolean sub[]) {return indexOf(a,sub,0);}
    public static int indexOf(char[] a, char sub[])       {return indexOf(a,sub,0);}
    public static int indexOf(byte[] a, byte sub[])       {return indexOf(a,sub,0);}
    public static int indexOf(short[] a, short sub[])     {return indexOf(a,sub,0);}
    public static int indexOf(int[] a, int sub[])         {return indexOf(a,sub,0);}
    public static int indexOf(long[] a, long sub[])       {return indexOf(a,sub,0);}
    public static int indexOf(float[] a, float sub[])     {return indexOf(a,sub,0);}
    public static int indexOf(double[] a, double sub[])   {return indexOf(a,sub,0);}
    public static int indexOf(Object[] a, Object sub[])   {return indexOf(a,sub,0);}
    public static int indexOf(Object a, Object sub)       {return indexOf(a,sub,0);}

    public static int indexOf(boolean[] a, boolean sub[], int fromIndex) {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(char[] a, char sub[], int fromIndex)       {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(byte[] a, byte sub[], int fromIndex)       {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(short[] a, short sub[], int fromIndex)     {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(int[] a, int sub[], int fromIndex)         {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(long[] a, long sub[], int fromIndex)       {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(float[] a, float sub[], int fromIndex)     {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(double[] a, double sub[], int fromIndex)   {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(Object[] a, Object sub[], int fromIndex)   {for (int k=fromIndex,len=a.length-sub.length; k<=len; k++) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int indexOf(Object a, Object sub, int fromIndex) {
        if (a instanceof boolean[]) return indexOf((boolean[])a,(boolean[])sub,fromIndex);
        else if (a instanceof char[]) return indexOf((char[])a,(char[])sub,fromIndex);
        else if (a instanceof byte[]) return indexOf((byte[])a,(byte[])sub,fromIndex);
        else if (a instanceof short[]) return indexOf((short[])a,(short[])sub,fromIndex);
        else if (a instanceof int[]) return indexOf((int[])a,(int[])sub,fromIndex);
        else if (a instanceof long[]) return indexOf((long[])a,(long[])sub,fromIndex);
        else if (a instanceof float[]) return indexOf((float[])a,(float[])sub,fromIndex);
        else if (a instanceof double[]) return indexOf((double[])a,(double[])sub,fromIndex);
        else if (a instanceof Object[]) return indexOf((Object[])a,(Object[])sub,fromIndex);
        else throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".indexOf(): "+JVM.toJavaClassName(a));
    }

    public static int lastIndexOf(boolean[] a, boolean sub[]) {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(char[] a, char sub[])       {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(byte[] a, byte sub[])       {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(short[] a, short sub[])     {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(int[] a, int sub[])         {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(long[] a, long sub[])       {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(Object[] a, Object sub[])   {return lastIndexOf(a,sub,a.length-sub.length);}
    public static int lastIndexOf(Object a, Object sub)       {return lastIndexOf(a,sub,Array.getLength(a)-Array.getLength(sub));}

    public static int lastIndexOf(boolean[] a, boolean sub[], int fromIndex) {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(char[] a, char sub[], int fromIndex)       {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(byte[] a, byte sub[], int fromIndex)       {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(short[] a, short sub[], int fromIndex)     {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(int[] a, int sub[], int fromIndex)         {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(long[] a, long sub[], int fromIndex)       {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(Object[] a, Object sub[], int fromIndex)   {for (int k=fromIndex; k>=0; k--) if (equals(a,k,sub,0,sub.length)) return k; return -1;}
    public static int lastIndexOf(Object a, Object sub, int fromIndex) {
        if (a instanceof boolean[]) return lastIndexOf((boolean[])a,(boolean[])sub,fromIndex);
        else if (a instanceof char[]) return lastIndexOf((char[])a,(char[])sub,fromIndex);
        else if (a instanceof byte[]) return lastIndexOf((byte[])a,(byte[])sub,fromIndex);
        else if (a instanceof short[]) return lastIndexOf((short[])a,(short[])sub,fromIndex);
        else if (a instanceof int[]) return lastIndexOf((int[])a,(int[])sub,fromIndex);
        else if (a instanceof long[]) return lastIndexOf((long[])a,(long[])sub,fromIndex);
        else if (a instanceof float[]) return lastIndexOf((float[])a,(float[])sub,fromIndex);
        else if (a instanceof double[]) return lastIndexOf((double[])a,(double[])sub,fromIndex);
        else if (a instanceof Object[]) return lastIndexOf((Object[])a,(Object[])sub,fromIndex);
        else throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".lastIndexOf(): "+JVM.toJavaClassName(a));
    }

    public static int indexOf(boolean[] a, boolean b)     {return indexOf(a,b,0);}
    public static int indexOf(char[] a, char b)           {return indexOf(a,b,0);}
    public static int indexOf(byte[] a, byte b)           {return indexOf(a,b,0);}
    public static int indexOf(short[] a, short b)         {return indexOf(a,b,0);}
    public static int indexOf(int[] a, int b)             {return indexOf(a,b,0);}
    public static int indexOf(float[] a, float b)         {return indexOf(a,b,0);}
    public static int indexOf(double[] a, double b)       {return indexOf(a,b,0);}
    public static int indexOf(Object[] a, Object b)       {return indexOf(a,b,0);}
    public static int indexOfEqual(Object[] a, Object b)  {return indexOfEqual(a,b,0);}

    public static int indexOf(boolean[] a, boolean b, int fromIndex)     {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(char[] a, char b, int fromIndex)           {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(byte[] a, byte b, int fromIndex)           {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(short[] a, short b, int fromIndex)         {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(int[] a, int b, int fromIndex)             {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(float[] a, float b, int fromIndex)         {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(double[] a, double b, int fromIndex)       {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOf(Object[] a, Object b, int fromIndex)       {for (int k=fromIndex; k<a.length; k++) if (a[k]==b) return k; return -1;}
    public static int indexOfEqual(Object[] a, Object b, int fromIndex)  {
        if (b==null) return indexOf(a,b,fromIndex);
        for (int k=fromIndex; k<a.length; k++) if (b.equals(a[k])) return k; return -1;
    }

    public static int lastIndexOf(boolean[] a, boolean b)   {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(char[] a, char b)         {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(byte[] a, byte b)         {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(short[] a, short b)       {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(int[] a, int b)           {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(long[] a, long b)         {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(float[] a, float b)       {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(double[] a, double b)     {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOf(Object[] a, Object b)     {return lastIndexOf(a,b,a.length-1);}
    public static int lastIndexOfEqual(Object[] a, Object b){return lastIndexOfEqual(a,b,a.length-1);}

    public static int lastIndexOf(boolean[] a, boolean b, int fromIndex)   {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(char[] a, char b, int fromIndex)         {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(byte[] a, byte b, int fromIndex)         {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(short[] a, short b, int fromIndex)       {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(int[] a, int b, int fromIndex)           {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(long[] a, long b, int fromIndex)         {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(float[] a, float b, int fromIndex)       {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(double[] a, double b, int fromIndex)     {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOf(Object[] a, Object b, int fromIndex)     {for (int k=fromIndex; k>=0; k--) if (a[k]==b) return k; return -1;}
    public static int lastIndexOfEqual(Object[] a, Object b, int fromIndex){
        if (b==null) return lastIndexOf(a,b,fromIndex);
        for (int k=fromIndex; k>=0; k--) if (b.equals(a[k])) return k; return -1;
    }


    public static Object insert(Object a, int aofs, Object b) {
        return replace(a,aofs,0,b,0,length(b));
    }
    public static Object insert(Object a, int aofs, Object b, int bofs, int len) {
        return replace(a,aofs,0,b,bofs,len);
    }
    public static Object delete(Object a, int aofs) {
        return deleteOne(a,aofs);
    }
    public static Object deleteOne(Object a, int aofs) {
        return replace(a,aofs,1,null,0,0);
    }
    public static Object deleteToEnd(Object a, int aofs) {
        return replace(a,aofs,length(a)-aofs,null,0,0);
    }
    public static Object delete(Object a, int aofs, int len) {
        return replace(a,aofs,len,null,0,0);
    }
    public static Object replace(Object a, int aofs, int oldlen, Object b) {
        return replace(a,aofs,oldlen,b,0,length(b));
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

    public static class Replacer {
        private int indexOfFirst = -1;
        private int count = 0;
        private int totalCount = 0; // not cleared in every "replace()"
        private Object a;
        public int indexOfFirst() {return indexOfFirst;}
        public int count()        {return count;}
        public int totalCount()   {return totalCount;}
        public Object result()    {return a;}
        public Replacer(Object a) {
            this.a = a;
        }
        public Replacer replace(Object oldsub, Object newsub) {
            try {
                this.indexOfFirst = -1;
                this.count = 0;
                int len = Array.getLength(a);
                int oldLen = Array.getLength(oldsub);
                int newLen = Array.getLength(newsub);
                if (oldLen == 0) {
                    a = copy(a); return this;
                }
                Object r = newCompatible(a,len);
                int rlen = 0, p =0;
                for (; p<len; ) {
                    int q = indexOf(a,oldsub,p); if (q == -1) break;
                    if (p == 0) this.indexOfFirst = q;
                    r = appendEnsured(r,rlen,a,p,q-p); rlen += q-p;
                    r = appendEnsured(r,rlen,newsub);  rlen += newLen;
                    p = q + oldLen;
                    this.count++;
                    this.totalCount++;
                }
                r = appendEnsured(r,rlen,a,p,len-p); rlen += len-p;
                r = truncate(r,rlen);
                a = r;
            } catch (NullPointerException e) {
                a = copy(a);
            }
            return this;
        }
    }
    public static Object replace(Object a, Object oldsub, Object newsub) {
        Replacer r= new Replacer(a);
        r.replace(oldsub,newsub);
        return r.result();
    }

    public static interface Matcher {
        public boolean matches(Object o);
    }
    public static class ClassMatcher implements Matcher {
        private final Class predecessor;
        public ClassMatcher(Class predecessor) {
            this.predecessor= predecessor;
        }
        public boolean matches(Object o) {
            return predecessor.isAssignableFrom(o.getClass());
        }
    }

    public static Object[] choose(Object[] a, Matcher matcher) {
        return choose(a,matcher,null);
    }
    public static Object[] choose(Object[] a, Matcher matcher, Object[] results) {
        Vector vector= new Vector();
        for (int k=0; k<a.length; k++) {
            if (matcher.matches(a[k])) vector.addElement(a[k]);
        }
        if (results == null)
            results = (Object[])newCompatible(a,vector.size());
        else if (results.length < vector.size())
            results = (Object[])newCompatible(results,vector.size());
        vector.copyInto(results);
        return results;
    }
    public static Object[] delete(Object[] a, Matcher matcher) {
        return delete(a,matcher,null);
    }
    public static Object[] delete(Object[] a, Matcher matcher, Object[] results) {
        Vector vector= new Vector();
        for (int k=0; k<a.length; k++) {
            if (!matcher.matches(a[k])) vector.addElement(a[k]);
        }
        if (results == null)
            results = (Object[])newCompatible(a,vector.size());
        else if (results.length < vector.size())
            results = (Object[])newCompatible(results,vector.size());
        vector.copyInto(results);
        return results;
    }
    public static int count(Object[] a, Matcher matcher) {
        int result= 0;
        for (int k=0; k<a.length; k++) {
            if (matcher.matches(a[k])) result++;
        }
        return result;
    }
    public static boolean contains(Object[] a, Matcher matcher) {
        for (int k=0; k<a.length; k++) {
            if (matcher.matches(a[k])) return true;
        }
        return false;
    }


    public static interface MainValue {
        public Object getMainValue();
    }
    public static Object[] getMainValues(MainValue[] a) {
        return getMainValues(a,null);
    }
    public static Object[] getMainValues(MainValue[] a, Object[] results) {
        Vector vector = new Vector(a.length);
        for (int k=0; k<a.length; k++) vector.addElement(a[k].getMainValue());
        if (results == null)
            results = new Object[vector.size()];
        else if (results.length < vector.size())
            results = (Object[])Array.newInstance(results.getClass().getComponentType(),vector.size());
        vector.copyInto(results);
        return results;
    }

    public static interface ValueFrom {
        public Object getValueFrom(Object o);
    }
    public static Object[] getValuesFrom(Object[] a, ValueFrom valueFrom) {
        return getValuesFrom(a,valueFrom,null);
    }
    public static Object[] getValuesFrom(Object[] a, ValueFrom valueFrom, Object[] results) {
        Vector vector = new Vector(a.length);
        for (int k=0; k<a.length; k++) vector.addElement(valueFrom.getValueFrom(a[k]));
        if (results == null)
            results = new Object[vector.size()];
        else if (results.length < vector.size())
            results = (Object[])Array.newInstance(results.getClass().getComponentType(),vector.size());
        vector.copyInto(results);
        return results;
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
        else throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".min(): "+JVM.toJavaClassName(a));
    }
    public static void max(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[]) max((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[]) max((short[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof int[]) max((int[])a,aofs,(int[])b,bofs,len);
        else if (a instanceof long[]) max((long[])a,aofs,(long[])b,bofs,len);
        else if (a instanceof float[]) max((float[])a,aofs,(float[])b,bofs,len);
        else if (a instanceof double[]) max((double[])a,aofs,(double[])b,bofs,len);
        else if (a instanceof ByteBuffer) max((ByteBuffer)a,aofs,(ByteBuffer)b,bofs,len);
        else throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".max(): "+JVM.toJavaClassName(a));
    }

    public static void min(byte[] a, int aofs, byte[] b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxImplemented && len>=nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>=nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp && a.isDirect() && b.isDirect()) {
            boolean check= a.get(aofs)<b.get(bofs) && a.get(aofs+len-1)<b.get(bofs+len-1); //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp && a.isDirect() && b.isDirect()) {
            boolean check= a.get(aofs)<b.get(bofs) && a.get(aofs+len-1)<b.get(bofs+len-1); //avoiding GPF
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.min(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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
        if (isNative && ArraysNative.minmaxImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.max(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
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

    public static void minu(Object a, Object b) throws Exception {
        minu(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void maxu(Object a, Object b) throws Exception {
        maxu(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void minu(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[]) minu((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[]) minu((short[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof ByteBuffer) minu((ByteBuffer)a,aofs,(ByteBuffer)b,bofs,len);
        else min(a,aofs,b,bofs,len);
    }
    public static void maxu(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[]) maxu((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[]) maxu((short[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof ByteBuffer) maxu((ByteBuffer)a,aofs,(ByteBuffer)b,bofs,len);
        else max(a,aofs,b,bofs,len);
    }

    public static void minu(byte[] a, int aofs, byte[] b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxuImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.minu(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
        }
        int aofsmax=aofs+len-3;
        for (; aofs<aofsmax; aofs+=4,bofs+=4) {
            if ((a[aofs]&0xFF)>(b[bofs]&0xFF)) a[aofs]=b[bofs];
            if ((a[aofs+1]&0xFF)>(b[bofs+1]&0xFF)) a[aofs+1]=b[bofs+1];
            if ((a[aofs+2]&0xFF)>(b[bofs+2]&0xFF)) a[aofs+2]=b[bofs+2];
            if ((a[aofs+3]&0xFF)>(b[bofs+3]&0xFF)) a[aofs+3]=b[bofs+3];
        }
        for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if ((a[aofs]&0xFF)>(b[bofs]&0xFF)) a[aofs]=b[bofs];
    }
    public static void maxu(byte[] a, int aofs, byte[] b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxuImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.maxu(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
        }
/*
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++)
            if ((a[aofs]&0xFF)<(b[bofs]&0xFF)) a[aofs]=b[bofs];
        // The full version is quicker by 20-50% on Java 1.4.2_07, PIV-1800
*/
        int aofsmax=aofs+len-3;
        for (; aofs<aofsmax; aofs+=4,bofs+=4) {
            if ((a[aofs]&0xFF)<(b[bofs]&0xFF)) a[aofs]=b[bofs];
            if ((a[aofs+1]&0xFF)<(b[bofs+1]&0xFF)) a[aofs+1]=b[bofs+1];
            if ((a[aofs+2]&0xFF)<(b[bofs+2]&0xFF)) a[aofs+2]=b[bofs+2];
            if ((a[aofs+3]&0xFF)<(b[bofs+3]&0xFF)) a[aofs+3]=b[bofs+3];
        }
        for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if ((a[aofs]&0xFF)<(b[bofs]&0xFF)) a[aofs]=b[bofs];
    }

    public static void minu(ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxuImplemented && len>nativeMinLenPairOp && a.isDirect() && b.isDirect()) {
            boolean check= a.get(aofs)<b.get(bofs) && a.get(aofs+len-1)<b.get(bofs+len-1); //avoiding GPF
            ArraysNative.minu(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
        }
        int aofsmax=aofs+len-3;
        for (; aofs<aofsmax; aofs+=4,bofs+=4) {
            if ((a.get(aofs)&0xFF)>(b.get(bofs)&0xFF)) a.put(aofs,b.get(bofs));
            if ((a.get(aofs+1)&0xFF)>(b.get(bofs+1)&0xFF)) a.put(aofs+1,b.get(bofs+1));
            if ((a.get(aofs+2)&0xFF)>(b.get(bofs+2)&0xFF)) a.put(aofs+2,b.get(bofs+2));
            if ((a.get(aofs+3)&0xFF)>(b.get(bofs+3)&0xFF)) a.put(aofs+3,b.get(bofs+3));
        }
        for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if ((a.get(aofs)&0xFF)>(b.get(bofs)&0xFF)) a.put(aofs,b.get(bofs));
    }
    public static void maxu(ByteBuffer a, int aofs, ByteBuffer b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxuImplemented && len>nativeMinLenPairOp && a.isDirect() && b.isDirect()) {
            boolean check= a.get(aofs)<b.get(bofs) && a.get(aofs+len-1)<b.get(bofs+len-1); //avoiding GPF
            ArraysNative.maxu(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
        }
        int aofsmax=aofs+len-3;
        for (; aofs<aofsmax; aofs+=4,bofs+=4) {
            if ((a.get(aofs)&0xFF)<(b.get(bofs)&0xFF)) a.put(aofs,b.get(bofs));
            if ((a.get(aofs+1)&0xFF)<(b.get(bofs+1)&0xFF)) a.put(aofs+1,b.get(bofs+1));
            if ((a.get(aofs+2)&0xFF)<(b.get(bofs+2)&0xFF)) a.put(aofs+2,b.get(bofs+2));
            if ((a.get(aofs+3)&0xFF)<(b.get(bofs+3)&0xFF)) a.put(aofs+3,b.get(bofs+3));
        }
        for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if ((a.get(aofs)&0xFF)<(b.get(bofs)&0xFF)) a.put(aofs,b.get(bofs));
    }

    public static void minu(short[] a, int aofs, short[] b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxuImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1]; //avoiding GPF
            ArraysNative.minu(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
        }
        int aofsmax=aofs+len-3;
        for (; aofs<aofsmax; aofs+=4,bofs+=4) {
            if ((char)a[aofs]>(char)b[bofs]) a[aofs]=b[bofs];
            if ((char)a[aofs+1]>(char)b[bofs+1]) a[aofs+1]=b[bofs+1];
            if ((char)a[aofs+2]>(char)b[bofs+2]) a[aofs+2]=b[bofs+2];
            if ((char)a[aofs+3]>(char)b[bofs+3]) a[aofs+3]=b[bofs+3];
        }
        for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if ((char)a[aofs]>(char)b[bofs]) a[aofs]=b[bofs];
    }
    public static void maxu(short[] a, int aofs, short[] b, int bofs, int len) {
        if (isNative && ArraysNative.minmaxuImplemented && len>nativeMinLenPairOp) {
            boolean check= a[aofs]<b[bofs] && a[aofs+len-1]<b[bofs+len-1];
            ArraysNative.maxu(ArraysNative.cpuInfo,a,aofs,b,bofs,len); return;
        }
        int aofsmax=aofs+len-3;
        for (; aofs<aofsmax; aofs+=4,bofs+=4) {
            if ((char)a[aofs]<(char)b[bofs]) a[aofs]=b[bofs];
            if ((char)a[aofs+1]<(char)b[bofs+1]) a[aofs+1]=b[bofs+1];
            if ((char)a[aofs+2]<(char)b[bofs+2]) a[aofs+2]=b[bofs+2];
            if ((char)a[aofs+3]<(char)b[bofs+3]) a[aofs+3]=b[bofs+3];
        }
        for (aofsmax+=3; aofs<aofsmax; aofs++,bofs++) if ((char)a[aofs]<(char)b[bofs]) a[aofs]=b[bofs];
    }

    /* Adding and subtracting */

    public static void add(Object a, Object b) throws Exception {
        add(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void sub(Object a, Object b) throws Exception {
        sub(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void addu(Object a, Object b) throws Exception {
        addu(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void subu(Object a, Object b) throws Exception {
        subu(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void adds(Object a, Object b) throws Exception {
        adds(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void subs(Object a, Object b) throws Exception {
        subs(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void addus(Object a, Object b) throws Exception {
        addus(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void subus(Object a, Object b) throws Exception {
        subus(a,0,b,0,min(Array.getLength(a),Array.getLength(b)));
    }
    public static void add(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[] && b instanceof byte[]) add((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof byte[]) add((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof short[]) add((short[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof int[] && b instanceof byte[]) add((int[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof int[] && b instanceof short[]) add((int[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof int[] && b instanceof int[]) add((int[])a,aofs,(int[])b,bofs,len);
        else if (a instanceof long[] && b instanceof long[]) add((long[])a,aofs,(long[])b,bofs,len);
        else if (a instanceof float[] && b instanceof float[]) add((float[])a,aofs,(float[])b,bofs,len);
        else if (a instanceof float[] && b instanceof double[]) add((float[])a,aofs,(double[])b,bofs,len);
        else if (a instanceof double[] && b instanceof float[]) add((double[])a,aofs,(float[])b,bofs,len);
        else if (a instanceof double[] && b instanceof double[]) add((double[])a,aofs,(double[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".add(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void sub(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[] && b instanceof byte[]) sub((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof byte[]) sub((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof short[]) sub((short[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof int[] && b instanceof byte[]) sub((int[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof int[] && b instanceof short[]) sub((int[])a,aofs,(short[])b,bofs,len);
        else if (a instanceof int[] && b instanceof int[]) sub((int[])a,aofs,(int[])b,bofs,len);
        else if (a instanceof long[] && b instanceof long[]) sub((long[])a,aofs,(long[])b,bofs,len);
        else if (a instanceof float[] && b instanceof float[]) sub((float[])a,aofs,(float[])b,bofs,len);
        else if (a instanceof float[] && b instanceof double[]) sub((float[])a,aofs,(double[])b,bofs,len);
        else if (a instanceof double[] && b instanceof float[]) sub((double[])a,aofs,(float[])b,bofs,len);
        else if (a instanceof double[] && b instanceof double[]) sub((double[])a,aofs,(double[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".sub(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void addu(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof short[] && b instanceof byte[]) addu((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof int[] && b instanceof byte[]) addu((int[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof int[] && b instanceof short[]) addu((int[])a,aofs,(short[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".addu(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void subu(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof short[] && b instanceof byte[]) subu((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof int[] && b instanceof byte[]) subu((int[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof int[] && b instanceof short[]) subu((int[])a,aofs,(short[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".subu(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void adds(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[] && b instanceof byte[]) adds((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof byte[]) adds((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof short[]) adds((short[])a,aofs,(short[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".adds(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void subs(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[] && b instanceof byte[]) subs((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof byte[]) subs((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof short[]) subs((short[])a,aofs,(short[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".subs(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void addus(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[] && b instanceof byte[]) addus((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof byte[]) addus((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof short[]) addus((short[])a,aofs,(short[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".addus(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
    }
    public static void subus(Object a, int aofs, Object b, int bofs, int len) throws Exception {
        if (a instanceof byte[] && b instanceof byte[]) subus((byte[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof byte[]) subus((short[])a,aofs,(byte[])b,bofs,len);
        else if (a instanceof short[] && b instanceof short[]) subus((short[])a,aofs,(short[])b,bofs,len);
        else throw new IllegalArgumentException("Unsupported arguments types in " + Arrays.class.getName() + ".subus(): "+JVM.toJavaClassName(a)+","+JVM.toJavaClassName(b));
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

    public static void addu(short[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=(short)(b[bofs]&0xFF);
    }
    public static void subu(short[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=(short)(b[bofs]&0xFF);
    }
    public static void addu(int[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=(b[bofs]&0xFF);
    }
    public static void subu(int[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=(b[bofs]&0xFF);
    }
    public static void addu(int[] a, int aofs, short[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]+=(char)b[bofs];
    }
    public static void subu(int[] a, int aofs, short[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) a[aofs]-=(char)b[bofs];
    }

    public static void adds(byte[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(byte)median((int)a[aofs]+(int)b[bofs],Byte.MIN_VALUE,Byte.MAX_VALUE);}
    }
    public static void subs(byte[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(byte)median((int)a[aofs]-(int)b[bofs],Byte.MIN_VALUE,Byte.MAX_VALUE);}
    }
    public static void adds(short[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((int)a[aofs]+(int)b[bofs],Short.MIN_VALUE,Short.MAX_VALUE);}
    }
    public static void subs(short[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((int)a[aofs]-(int)b[bofs],Short.MIN_VALUE,Short.MAX_VALUE);}
    }
    public static void adds(short[] a, int aofs, short[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((int)a[aofs]+(int)b[bofs],Short.MIN_VALUE,Short.MAX_VALUE);}
    }
    public static void subs(short[] a, int aofs, short[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((int)a[aofs]-(int)b[bofs],Short.MIN_VALUE,Short.MAX_VALUE);}
    }

    public static void addus(byte[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(byte)median((a[aofs]&0xFF)+(b[bofs]&0xFF),0,255);}
    }
    public static void subus(byte[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(byte)median((a[aofs]&0xFF)-(b[bofs]&0xFF),0,255);}
    }
    public static void addus(short[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((char)a[aofs]+(b[bofs]&0xFF),0,65535);}
    }
    public static void subus(short[] a, int aofs, byte[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((char)a[aofs]-(b[bofs]&0xFF),0,65535);}
    }
    public static void addus(short[] a, int aofs, short[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((char)a[aofs]+(char)b[bofs],0,65535);}
    }
    public static void subus(short[] a, int aofs, short[] b, int bofs, int len) {
        for (int aofsmax=aofs+len; aofs<aofsmax; aofs++,bofs++) {a[aofs]=(short)median((char)a[aofs]-(char)b[bofs],0,65535);}
    }


    public static Object newmin(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        min(r,0,b,0,blen);
        return r;
    }
    public static Object newmax(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        max(r,0,b,0,blen);
        return r;
    }
    public static Object newminu(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        minu(r,0,b,0,blen);
        return r;
    }
    public static Object newmaxu(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        maxu(r,0,b,0,blen);
        return r;
    }
    public static Object newadd(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        add(r,0,b,0,blen);
        return r;
    }
    public static Object newsub(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        sub(r,0,b,0,blen);
        return r;
    }
    public static Object newaddu(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        addu(r,0,b,0,blen);
        return r;
    }
    public static Object newsubu(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        subu(r,0,b,0,blen);
        return r;
    }
    public static Object newadds(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        adds(r,0,b,0,blen);
        return r;
    }
    public static Object newsubs(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        subs(r,0,b,0,blen);
        return r;
    }
    public static Object newaddus(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        addus(r,0,b,0,blen);
        return r;
    }
    public static Object newsubus(Object a, Object b) throws Exception {
        if (b==null) return a;
        int alen= length(a);
        int blen= length(b);
        Object r= newCompatible(a==null?b:a,max(alen,blen));
        copy(a,0,r,0,alen);
        subus(r,0,b,0,blen);
        return r;
    }

    private static final Random rnd = new Random();
    // for maximal speed:
    private static int min(int a, int b)            {return a<=b? a: b;}
    private static int max(int a, int b)            {return a>=b? a: b;}
    private static int median(int a, int b, int c)  {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}


    /* Constants, CPU service functions */

    public static final long CPU_TSC= 1<<4;
    public static final long CPU_MMX= 1<<23;   // always set if CPU_SSE is set
    public static final long CPU_CMOV= 1<<15;  // always cleared if no FPU exist;
        // so, it means that all commands CMOVxx, FCMOVxx, FCOMI, FUCOMI are avaiable
    public static final long CPU_SSE= 1<<25;   // always set if CPU_SSE2 is set
    public static final long CPU_SSE2= 1<<26;
    public static final long CPU_AMD= 1L<<59;
    public static final long CPU_MMXEX= 1L<<60; // always set if CPU_SSE is set
    public static final long CPU_3DNOWEX= 1L<<62;
    public static final long CPU_3DNOW= 1L<<63;

    public static final int CPU_L2SIZE_SHIFT= 32;
    public static final long CPU_L2SIZE_UNIT= 32*1024;
    public static final long CPU_L2SIZE= 1023L;
        // mask for L2 cache size in 32KB blocks, maximum 32MB
    public static final int CPU_L1DATASIZE_SHIFT= 42;
    public static final int CPU_L1DATASIZE_UNIT= 8*1024;
    public static final long CPU_L1DATASIZE= 255L;
        // mask for L1 cache size in 8KB blocks, maximum 2MB
    public static final int CPU_FAMILY_SHIFT= 50;
    public static final long CPU_FAMILY= 15L;

    public static long getCpuInfo() {
        return ArraysNative.cpuInfo;
    }
    public static int getCpuFamily() {
        return (int)((getCpuInfo()>>>CPU_FAMILY_SHIFT)&CPU_FAMILY);
    }
    public static long getCpuL1CacheSize() {  // in bytes
        return ((getCpuInfo()>>>CPU_L1DATASIZE_SHIFT)&CPU_L1DATASIZE)*CPU_L1DATASIZE_UNIT;
    }
    public static long getCpuL2CacheSize() {  // in bytes
        return ((getCpuInfo()>>>CPU_L2SIZE_SHIFT)&CPU_L2SIZE)*CPU_L2SIZE_UNIT;
    }
    public static void setCpuInfo(long v) {
        if ((v&CPU_MMX)==0) v&= ~(CPU_MMXEX|CPU_SSE|CPU_SSE2);
        if ((v&CPU_MMXEX)==0) v&= ~(CPU_SSE|CPU_SSE2);
        if ((v&CPU_SSE)==0) v&= ~CPU_SSE2;
        if ((v&CPU_3DNOW)==0) v&= ~CPU_3DNOWEX;
        ArraysNative.cpuInfo= v;
    }
    public static void resetCpuInfo() {
        ArraysNative.cpuInfo=
            ArraysNative.loaded?
                ArraysNative.getCpuInfoInternal():
                0;
    }
    static boolean isNative = ArraysNative.loaded;  // for maximal speed
    public static boolean isNative() {
        return isNative;
    }
    public static void setNative(boolean v) {
        isNative = v;
    }
    public static String initializationExceptionMessage() {
        return ArraysNative.initializationExceptionMessage;
    }

    private static int nativeMinLenFill= 100;
    private static int nativeMinLenPairOp= 100;
    public static int getNativeMinLenFill() {return nativeMinLenFill;}
    public static void setNativeMinLenFill(int v) {nativeMinLenFill= max(v,0);}
    public static int getNativeMinLenPairOp() {return nativeMinLenPairOp;}
    public static void setNativeMinLenPairOp(int v) {nativeMinLenPairOp= max(v,0);}

    public static int ptrOfs(Object a) {
        if (!a.getClass().isArray()) throw new IllegalArgumentException("Unsupported argument type in " + Arrays.class.getName() + ".ptrOfs(): it should be an array");
        if (!isNative) return 0;
        return ArraysNative.ptrOfs(a);
    }
}

class ArraysNative implements TrueStatic {

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
    static boolean loaded = false;
    static final String initializationExceptionMessage;
    static {
    String message = null;
    if (GlobalProperties.getBooleanProperty(GlobalProperties.JAVA_NATIVE_ENABLED_PROPERTY_NAME,true))
      try {
        System.loadLibrary(ArraysNative.class.getName().replace('.','_'));
        loaded = true;
        detectImplementedFlags();
        cpuInfo = getCpuInfoInternal();
      } catch (UnsatisfiedLinkError e) {
        message = e.toString();
      } catch (SecurityException e) {
        message = e.toString();
      }
    initializationExceptionMessage = message;
//    System.out.println("!!!!Arrays: " + initializationExceptionMessage);
    }
}