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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * <p>Title:     <b>Algart Tools</b><br>
 * Description:  Miscellaneous basic tools<br>
 * @author       Daniel Alievsky
 * @version      1.0
 * @deprecated
 */

public class ATools {

  /* Constants */
  public static final long classLoaderId= TrueStatics.getClassLoaderId();
  public static long timeStart= TrueStatics.timeStart;
  public static long timeStartFrom1970= TrueStatics.timeStartFrom1970;
  public static void decreaseTimeStart(long decrement) {
    // Useful in a case when ATools.class is first loaded
    // since some time after starting application
    TrueStatics.timeStart-= decrement;
    timeStart-= decrement;
  }
  public static final long timeStartForThisClassLoader= classLoaderId==0? timeStart: timems();

  public static final double E= Math.E;
  public static final double PI= Math.PI;
  public static final double LOG2= Math.log(2.0);
  public static final double LOG10= Math.log(10.0);
  public static final double SQRT2= Math.sqrt(2.0);
  public static final int MIN_INT= Integer.MIN_VALUE;
  public static final int MAX_INT= Integer.MAX_VALUE;
  public static final long MIN_LONG= Long.MIN_VALUE;
  public static final long MAX_LONG= Long.MAX_VALUE;

  public static final int SIZE_OF_CHAR= 2;
  public static final int SIZE_OF_BYTE= 1;
  public static final int SIZE_OF_SHORT= 2;
  public static final int SIZE_OF_INT= 4;
  public static final int SIZE_OF_LONG= 8;
  public static final int SIZE_OF_FLOAT= 4;
  public static final int SIZE_OF_DOUBLE= 8;
  // No constant for boolean: Java doesn't specify sizeof(boolean)

  public static final String lineSeparator= ATools.getNotEmptyProperty("line.separator","\n");

  /* Wrappers around primitive types */
  /* These wrappers allow functions to return several results,
  for example:
    public static int someFunction(
      double inputArgument,
      VarBoolean additionalResult)
    {
      ...
      additionalResult.v= true;
      return 1;
    }
  */
  public static class VarBoolean  {public volatile boolean v;   public VarBoolean(){} public VarBoolean(boolean v) {this.v= v;}}
  public static class VarChar     {public volatile char v;      public VarChar(){}    public VarChar(char v)       {this.v= v;}}
  public static class VarByte     {public volatile byte v;      public VarByte(){}    public VarByte(byte v)       {this.v= v;}   public VarByte(int v)     {this.v= (byte)v;}}
  public static class VarShort    {public volatile short v;     public VarShort(){}   public VarShort(short v)     {this.v= v;}   public VarShort(int v)    {this.v= (short)v;}}
  public static class VarInteger  {public volatile int v;       public VarInteger(){} public VarInteger(int v)     {this.v= v;}}
  public static class VarLong     {public volatile long v;      public VarLong(){}    public VarLong(long v)       {this.v= v;}}
  public static class VarFloat    {public volatile float v;     public VarFloat(){}   public VarFloat(float v)     {this.v= v;}   public VarFloat(double v) {this.v= (float)v;}}
  public static class VarDouble   {public volatile double v;    public VarDouble(){}  public VarDouble(double v)   {this.v= v;}}
  public static class VarString   {public volatile String v;    public VarString(){}  public VarString(String v)   {this.v= v;}}

  /*
  Sizes is an immutable equivalent of the standard java.awt.Dimension class
  */
  public static final class Sizes {
    private final int width;
    private final int height;
    public Sizes(int width, int height) {
      this.width= width;
      this.height= height;
    }
    public int width()      {return width;}
    public int height()     {return height;}
    public int sx()         {return width;}
    public int sy()         {return height;}
    public long size()      {return (long)width*(long)height;}
    public String toString() {
      return width+"x"+height;
    }
    public boolean equals(Object obj) {
      if (!(obj instanceof Sizes)) return false;
      return ((Sizes)obj).width==width && ((Sizes)obj).height==height;
    }
    public int hashCode() {
      return width*height;
    }
  }

  /*
  Point is an immutable equivalent of the standard java.awt.Point class
  */
  public static final class Point {
    public final int x,y;
    private Point(int x, int y) {
      this.x= x;
      this.y= y;
    }
    public static Point valueOf(int x, int y) {
      return new Point(x,y);
    }
    public String toString() {
      return x+","+y;
    }
    public boolean equals(Object obj) {
      if (!(obj instanceof Point)) return false;
      return ((Point)obj).x==x && ((Point)obj).y==y;
    }
    public int hashCode() {
      return x*y;
    }
  }

  /*
  Rectangle is an immutable equivalent of the standard java.awt.Rectangle class
  */
  public static final class Rectangle {
    public final int x1,y1,x2,y2;
    public final int sx,sy,width,height;
    private Rectangle(int x1, int y1, int x2, int y2) {
      this.x1= x1; this.y1= y1;
      this.x2= x2; this.y2= y2;
      this.sx= this.width= x2-x1;
      this.sy= this.height= y2-y1;
    }
    public static Rectangle valueOf(int x1, int y1, int x2, int y2) {
      return new Rectangle(x1,y1,x2,y2);
    }
    public static Rectangle valueOfWH(int x1, int y1, int width, int height) {
      return new Rectangle(x1,y1,x1+width,y1+height);
    }
    public String toString() {
      return x1+","+y1+","+x2+","+y2;
    }
    public boolean equals(Object obj) {
      if (!(obj instanceof Rectangle)) return false;
      Rectangle r= (Rectangle)obj;
      return r.x1==x1 && r.y1==y1 && r.x2==x2 && r.y2==y2;
    }
    public int hashCode() {
      return (((x1*37+157)*37+y1)*37+x2)*37+y2;
    }
  }

  /* All math functions */
  public static int toI(byte a)                 {return ((int)a)&0xFF;}
  public static int toI(short a)                {return ((int)a)&0xFFFF;}

  public static int  iround(double a)           {return (int)Math.round(a);}
  public static byte bround(double a)           {return (byte)Math.round(a);}
  public static int  round(float a)             {return Math.round(a);}
  public static long round(double a)            {return Math.round(a);}

  public static double ceil(double a)           {return Math.ceil(a);}
  public static double floor(double a)          {return Math.floor(a);}
  public static double rint(double a)           {return Math.rint(a);}

  public static int    abs(int a)               {return a >=0? a: -a;}
  public static long   abs(long a)              {return a >=0? a: -a;}
  public static double abs(double a)            {return a <= 0.0D? 0.0D - a : a;}
  public static float  abs(float a)             {return a <= 0.0F? 0.0F - a : a;}

  public static byte   minu(byte a, byte b)     {return (a&0xFF)<(b&0xFF)? a: b;}
  public static byte   maxu(byte a, byte b)     {return (a&0xFF)>(b&0xFF)? a: b;}
  public static short  minu(short a, short b)   {return (char)a<(char)b? a: b;}
  public static short  maxu(short a, short b)   {return (char)a>(char)b? a: b;}
  public static int    minu(int a, int b)       {return (a^0x80000000)<(b^0x80000000)? a: b;}
  public static int    maxu(int a, int b)       {return (a^0x80000000)>(b^0x80000000)? a: b;}
  public static long   minu(long a, long b)     {return (a^0x8000000000000000L)<(b^0x8000000000000000L)? a: b;}
  public static long   maxu(long a, long b)     {return (a^0x8000000000000000L)>(b^0x8000000000000000L)? a: b;}

  public static byte   min(byte a, byte b)      {return a<=b? a: b;}
  public static byte   max(byte a, byte b)      {return a>=b? a: b;}
  public static short  min(short a, short b)    {return a<=b? a: b;}
  public static short  max(short a, short b)    {return a>=b? a: b;}
  public static int    min(int a, int b)        {return a<=b? a: b;}
  public static int    max(int a, int b)        {return a>=b? a: b;}
  public static long   min(long a, long b)      {return a<=b? a: b;}
  public static long   max(long a, long b)      {return a>=b? a: b;}
  public static float  min(float a, float b)    {return Math.min(a,b);}
  public static float  max(float a, float b)    {return Math.max(a,b);}
  public static double min(double a, double b)  {return Math.min(a,b);}
  public static double max(double a, double b)  {return Math.max(a,b);}

  public static byte   min(byte a, byte b, byte c)        {return b<c?(a<b?a:b):(a<c?a:c);}
  public static byte   max(byte a, byte b, byte c)        {return b>c?(a>b?a:b):(a>c?a:c);}
  public static short  min(short a, short b, short c)     {return b<c?(a<b?a:b):(a<c?a:c);}
  public static short  max(short a, short b, short c)     {return b>c?(a>b?a:b):(a>c?a:c);}
  public static int    min(int a, int b, int c)           {return b<c?(a<b?a:b):(a<c?a:c);}
  public static int    max(int a, int b, int c)           {return b>c?(a>b?a:b):(a>c?a:c);}
  public static long   min(long a, long b, long c)        {return b<c?(a<b?a:b):(a<c?a:c);}
  public static long   max(long a, long b, long c)        {return b>c?(a>b?a:b):(a>c?a:c);}
  public static float  min(float a, float b, float c)     {return b<c?(a<b?a:b):(a<c?a:c);}
  public static float  max(float a, float b, float c)     {return b>c?(a>b?a:b):(a>c?a:c);}
  public static double min(double a, double b, double c)  {return b<c?(a<b?a:b):(a<c?a:c);}
  public static double max(double a, double b, double c)  {return b>c?(a>b?a:b):(a>c?a:c);}

  public static byte   median(byte a, byte b, byte c)       {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}
  public static short  median(short a, short b, short c)    {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}
  public static int    median(int a, int b, int c)          {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}
  public static long   median(long a, long b, long c)       {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}
  public static float  median(float a, float b, float c)    {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}
  public static double median(double a, double b, double c) {return b<c?(a<b?b:a>c?c:a):(a<c?c:a>b?b:a);}

  public static boolean between(char a, char b, char c)       {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(byte a, byte b, byte c)       {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(short a, short b, short c)    {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(int a, int b, int c)          {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(long a, long b, long c)       {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(float a, float b, float c)    {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(double a, double b, double c) {return b<=c? a>=b && a<=c: a>=c && a<=b;}
  public static boolean between(float a, float b, float c, float epsilon)     {return b<=c? a>=b+epsilon && a<=c-epsilon: a>=c+epsilon && a<=b-epsilon;}
  public static boolean between(double a, double b, double c, double epsilon) {return b<=c? a>=b+epsilon && a<=c-epsilon: a>=c+epsilon && a<=b-epsilon;}

  public static boolean between(int a, int[] bounds) {
  /* Check whether
      min(bounds[0],bounds[1]) <= a <= max(bounds[0],bounds[1]) ||
      min(bounds[2],bounds[3]) <= a <= max(bounds[2],bounds[3]) ||
        . . .
      min(bounds[2*k],bounds[2*k+1]) <= a <= max(bounds[2*k],bounds[2*k+1]) ||
        . . .
  */
    for (int k=0; k<=bounds.length-2; k+=2) {
      if (between(a,bounds[k],bounds[k+1])) return true;
    }
    return false;
  }


  public static int    sqr(int a)               {return a*a;}
  public static long   sqr(long a)              {return a*a;}
  public static float  sqr(float a)             {return a*a;}
  public static double sqr(double a)            {return a*a;}

  public static int    pow3(int a)              {return a*a*a;}
  public static long   pow3(long a)             {return a*a*a;}
  public static float  pow3(float a)            {return a*a*a;}
  public static double pow3(double a)           {return a*a*a;}

  public static int    pow4(int a)              {return sqr(sqr(a));}
  public static long   pow4(long a)             {return sqr(sqr(a));}
  public static float  pow4(float a)            {return sqr(sqr(a));}
  public static double pow4(double a)           {return sqr(sqr(a));}

  public static double sqrt(double a)           {return Math.sqrt(a);}
  public static double pow(double a, double b)  {return Math.pow(a,b);}
  public static double exp(double a)            {return Math.exp(a);}
  public static double log(double a)            {return Math.log(a);}

  public static double sin(double a)            {return Math.sin(a);}
  public static double cos(double a)            {return Math.cos(a);}
  public static double tan(double a)            {return Math.tan(a);}
  public static double tg(double a)             {return Math.tan(a);}
  public static double ctg(double a)            {return 1.0/Math.tan(a);}
  public static double asin(double a)           {return Math.asin(a);}
  public static double acos(double a)           {return Math.acos(a);}
  public static double atan(double a)           {return Math.atan(a);}
  public static double atan(double y,double x)  {return Math.atan2(y,x);}
  public static double arcsin(double a)         {return Math.asin(a);}
  public static double arccos(double a)         {return Math.acos(a);}
  public static double arctg(double a)          {return Math.atan(a);}
  public static double arctg(double y,double x) {return Math.atan2(y,x);}

  public static double toRadians(double a)      {return Math.toRadians(a);}
  public static double toDegrees(double a)      {return Math.toDegrees(a);}

  public static Random rnd= new Random(timeStartForThisClassLoader);
  public static double random()                 {return rnd.nextDouble();}
  public static int random(int n)               {return rnd.nextInt(n);}

  public static int    add(int a, int b)        {return a+b;}
  public static long   add(long a, long b)      {return a+b;}
  public static double add(double a, double b)  {return a+b;}
  public static float  add(float a, float b)    {return a+b;}
  public static int    sub(int a, int b)        {return a-b;}
  public static long   sub(long a, long b)      {return a-b;}
  public static double sub(double a, double b)  {return a-b;}
  public static float  sub(float a, float b)    {return a-b;}
  public static int    mul(int a, int b)        {return a*b;}
  public static long   mul(long a, long b)      {return a*b;}
  public static double mul(double a, double b)  {return a*b;}
  public static float  mul(float a, float b)    {return a*b;}
  public static int    div(int a, int b)        {return a/b;}
  public static long   div(long a, long b)      {return a/b;}
  public static double div(double a, double b)  {return a/b;}
  public static float  div(float a, float b)    {return a/b;}
  public static Object iif(boolean condition, Object left, Object right) {
    return condition ? left : right ;
  }

  /* Some string functions (duplicated in AStrings) */
  public static String dup(char c, int len) {
    char[] ca= new char[len];
    for (int k=0; k<len; k++) ca[k]= c;
    return new String(ca);
  }
  public static String leftPad(String s, int len) {
    return leftPad(s,len,' ');
  }
  public static String rightPad(String s, int len) {
    return rightPad(s,len,' ');
  }
  public static String leftPad(String s, int len, char pad) {
    return s.length()>len? s: dup(pad,len-s.length())+s;
  }
  public static String rightPad(String s, int len, char pad) {
    return s.length()>len? s: s+dup(pad,len-s.length());
  }
  public static String replace(String s, String oldSubstring, String newSubstring) {
  // Returns s if any string is null, or s is "", or oldSubstring is ""
    try {
      int len= s.length();
      int oldLen= oldSubstring.length();
      if (oldLen==0 || newSubstring==null) return s;
      StringBuffer sb= new StringBuffer(len);
      int p=0;
      for (;p<len;) {
        int q= s.indexOf(oldSubstring,p);
        if (q==-1) break;
        sb.append(s.substring(p,q)).append(newSubstring);
        p= q+oldLen;
      }
      sb.append(s.substring(p));
      return sb.toString();
    } catch (NullPointerException e) {
      return s;
    }
  }
  public static String[] split(String s, String separators) {
    if (s==null) return new String[0];
    StringTokenizer stok= new StringTokenizer(s,separators);
    String[] result= new String[stok.countTokens()];
    for (int k=0; k<result.length; k++) {
      result[k]= stok.nextToken();
    }
    return result;
  }


  /* print functions */
  public static String avoidJavaBuilderClankForSystemOut(String s) {
    return replace(s,"Exception","[[E]]xception");
  }
  public static String replaceLFToLineSeparator(String s) {
    return replace(s,"\n",lineSeparator);
  }
  public static void print(boolean a)    {print(String.valueOf(a));}
  public static void print(char a)       {print(String.valueOf(a));}
  public static void print(int a)        {print(String.valueOf(a));}
  public static void print(long a)       {print(String.valueOf(a));}
  public static void print(float a)      {print(String.valueOf(a));}
  public static void print(double a)     {print(String.valueOf(a));}
  public static void print(char a[])     {print(String.valueOf(a));}
  public static void print(String a)     {printNoCorrection(avoidJavaBuilderClankForSystemOut(replaceLFToLineSeparator(a)));}
  public static void print(Object a)     {print(String.valueOf(a));}
  public static void println()           {print("\n");}
  public static void println(boolean a)  {println(String.valueOf(a));}
  public static void println(char a)     {println(String.valueOf(a));}
  public static void println(int a)      {println(String.valueOf(a));}
  public static void println(long a)     {println(String.valueOf(a));}
  public static void println(float a)    {println(String.valueOf(a));}
  public static void println(double a)   {println(String.valueOf(a));}
  public static void println(char a[])   {println(String.valueOf(a));}
  public static void println(String a)   {print(String.valueOf(a+"\n"));}
  public static void println(Object a)   {println(String.valueOf(a));}

  public static void printNoCorrection(String a) {
    TrueStatics.printNoCorrection(a);
  }
  public static void printlnNoCorrection(String a) {
    printNoCorrection(a+lineSeparator);
  }
  public static void flushPrinting() {
    TrueStatics.flushPrinting();
  }
  public static String readln() throws IOException {
    flushPrinting();
    return new BufferedReader(new InputStreamReader(System.in)).readLine();
  }
  public static long getPrintDelay() {
    return TrueStatics.getPrintDelay();
  }
  public static void setPrintDelay(long ms) {
    TrueStatics.setPrintDelay(ms);
  }

  /* Conversion to array */
  public static Object[] toA(Object v) {
    return toA(v,null);
  }
  public static Object[] toA(Object v, Object[] results) {
    if (v==null) {
      if (results==null) return new Object[0];
      return results;
    }
    if (v.getClass().isArray()) {
      if (results==null)
        return (Object[])v;
      int len= Array.getLength(v);
      if (results.length<len)
        results= (Object[])Array.newInstance(
          results.getClass().getComponentType(),len);
      System.arraycopy(v,0,results,0,len);
      return results;
    }
    if (v instanceof Collection) {
      if (results==null) return ((Collection)v).toArray();
      return ((Collection)v).toArray(results);
    }
    if (v instanceof Iterator) {
      Iterator it= (Iterator)v;
      List list= new ArrayList();
      for (; it.hasNext(); ) list.add(it.next());
      return toA(list,results);
    }
    if (v instanceof Map) {
      return toA(((Map)v).entrySet(),results);
    }
    Object oneObject= Array.newInstance(v.getClass(),1);
    Array.set(oneObject,0,v);
    return toA(oneObject,results);
  }

  /* Conversion to String */
  public static String toS(Object v) {
  // More convenient analog for "String.valueOf(v)"
    return v instanceof char[]?
      new String((char[])v):
      toS(v,",");
  }
  public static String toSOrNull(Object v) {
    return v==null? null: v.toString();
  }
  public static String extS(Object v, String format) {
  // Example: extS("1,2,3","Results: \000") returns "Results: 1,2,3",
  // but extS("","Results: \000") returns "".
    String s= toS(v);
    if (s.length()==0) return "";
    return replace(format,"\000",s);
  }
  public static String extS(Object v, String separator, String format) {
    String s= toS(v,separator);
    if (s.length()==0) return "";
    return replace(format,"\000",s);
  }

  public static String toS(double v, int d) {
    return toS(v,d,false);
  }
  public static String toS(double v, int d, boolean exponentForm) {
    StringBuffer ptn= new StringBuffer("0");
    if (d>0) {
      ptn.append('.');
      for (; d>0; d--) ptn.append('0');
    }
    if (exponentForm) ptn.append("E0");
    DecimalFormat f= new DecimalFormat(ptn.toString());
    return f.format(v).replace(',','.');
  }
  public static String toS(double v, int d, int totalLen) {
    return leftPad(toS(v,d),totalLen);
  }
  public static String toS(double v, int d, boolean exponentForm, int totalLen) {
    return leftPad(toS(v,d,exponentForm),totalLen);
  }
  public static String toS(long v, int totalLen) {
    return leftPad(v+"",totalLen);
  }
  public static String toS(int v, int totalLen) {
    return toS((long)v,totalLen);
  }
  public static String toS(String v, int totalLen) {
    return leftPad(v,totalLen);
  }
  public static String toSR(double v, int d, int totalLen) {
    return rightPad(toS(v,d),totalLen);
  }
  public static String toSR(double v, int d, boolean exponentForm, int totalLen) {
    return rightPad(toS(v,d,exponentForm),totalLen);
  }
  public static String toSR(long v, int totalLen) {
    return rightPad(v+"",totalLen);
  }
  public static String toSR(int v, int totalLen) {
    return toSR((long)v,totalLen);
  }
  public static String toSR(String v, int totalLen) {
    return rightPad(v,totalLen);
  }
  public static String toSSpaced(long v) {
    return toSSpaced(v,' ');
  }
  public static String toSSpaced(long v, char space) {
    DecimalFormat f= new DecimalFormat("#,###");
    return f.format(v).replace(',',space);
  }
  public static String toSHex(byte v) {
    return leftPad(Integer.toHexString(v&0xFF).toUpperCase(),2,'0');
  }
  public static String toSHex(short v) {
    return leftPad(Integer.toHexString(v&0xFFFF).toUpperCase(),4,'0');
  }
  public static String toSHex(int v) {
    return leftPad(Integer.toHexString(v).toUpperCase(),8,'0');
  }
  public static String toSHex(long v) {
    return leftPad(Long.toHexString(v).toUpperCase(),16,'0');
  }
  public static String toSAbc(long v) {
  // "A","B",...,"Z","AA","AB",...,"AZ","BA",...,"ZZ","AAA",...,"ZZZ","AAAA",...
    StringBuffer sb= new StringBuffer(8);
    int offset= 0;
    if (v<0) {
      sb.append('-'); v= -v; offset= 1;
    }
    for (; v>=0; v= v/26-1) {
      sb.insert(offset,(char)('A'+(char)(v%26)));
    }
    return sb.toString();
  }

  public static long parseLongAbc(String s) throws NumberFormatException {
    return parseLongAbc(s, false);
  }
  public static long parseLongAbc(String s, boolean upperCaseOnly) throws NumberFormatException {
    int offset= 0;
    if (s.startsWith("-")) offset= 1;
    long result= -1, mult= 1;
    for (int p= s.length()-1; p>=offset; p--) {
      char c= s.charAt(p);
      int dig;
      if (c>='A' && c<='Z') dig= c-'A';
      else if (!upperCaseOnly && c>='a' && c<='z') dig= c-'a';
      else throw new NumberFormatException("Illegal character: "+c);
      result+= (dig+1)*mult;
      mult*= 26;
    }
    if (mult==1) throw new NumberFormatException("Empty string");
    if (offset==1) result= -result;
    return result;
  }

  public static String toS(Object v, String separator, boolean addSeparatorAfterLastElement) {
  // v should be an array of any objects or primirive types,
  // or should implement AbstractCollection
  // By default, addSeparatorAfterLastElement is supposed false
  // If v.length==0 (or v.isEmpty() for AbstractCollection), returns ""
    if (v==null) return "";
    if (!addSeparatorAfterLastElement)
      return toS(v,separator);
    if (v instanceof Collection)
      return !((Collection)v).isEmpty()?
        toS(v,separator)+separator:
        toS(v,separator);
    if (v instanceof Iterator)
      return ((Iterator)v).hasNext()?
        toS(v,separator)+separator:
        toS(v,separator);
    return toS(v,-1,false,separator,addSeparatorAfterLastElement);
  }
  public static String toS(Object v, String separator) {
    if (v==null) return "";
    if (v instanceof Iterator) {
      Iterator it= (Iterator)v;
      if (!it.hasNext()) return "";
      StringBuffer result= new StringBuffer(String.valueOf(it.next()));
      for (; it.hasNext(); )
        result.append(separator).append(String.valueOf(it.next()));
      return result.toString();
    }
    if (v instanceof Collection) {
      return toS(((Collection)v).iterator(),separator);
    }
    if (v instanceof Map) {
      return toS(((Map)v).entrySet(),separator);
    }
    return toS(v,-1,false,separator);
  }
  public static String toS(Object v, int d, String separator, boolean addSeparatorAfterLastElement) {
    return toS(v,d,false,separator,addSeparatorAfterLastElement);
  }
  public static String toS(Object v, int d, boolean exponentForm, String separator, boolean addSeparatorAfterLastElement) {
  // A version of the same method for array of floats or doubles; d is the mantissa length
    return addSeparatorAfterLastElement && v!=null && Array.getLength(v)>0?
      toS(v,d,exponentForm,separator)+separator:
      toS(v,d,exponentForm,separator);
  }
  public static String toS(Object v, int d, String separator) {
    return toS(v,d,false,separator);
  }
  public static String toS(Object v, int d, boolean exponentForm, String separator) {
    if (v==null) return "";
    if (v.getClass().isArray()) {
      int len;
      if ((len=Array.getLength(v))==0) return "";
      StringBuffer result= new StringBuffer();
      for (int k=0; k<len; k++) {
        if (k>0) result.append(separator);
        result.append(d<0?String.valueOf(Array.get(v,k)):toS(Array.getDouble(v,k),d,exponentForm));
      }
      return result.toString();
    }
    return String.valueOf(v);
  }
  public static String toSHex(Object v) {
    return toSHex(v,",");
  }
  public static String toSHex(Object v, String separator) {
    if (v==null) return "";
    Class componentClass= v.getClass().getComponentType();
    int digits= componentClass==byte.class? 2:
      componentClass==short.class || componentClass==char.class? 4:
      componentClass==int.class? 8:
      componentClass==long.class? 16:
      0;
    if (digits>0) {
      int len;
      if ((len=Array.getLength(v))==0) return "";
      StringBuffer result= new StringBuffer();
      for (int k=0; k<len; k++) {
        if (k>0) result.append(separator);
        result.append(digits==16?
          toSHex(Array.getLong(v,k)):
          leftPad(Integer.toHexString(Array.getInt(v,k)
            &((16<<((digits-1)*4))-1)).toUpperCase(),digits,'0'));
      }
      return result.toString();
    }
    return toS(v,separator);
  }

  /* Timing */
  public static final long timensCorrection= TrueStatics.timensCorrection;
  public static final long timecpuCorrection= TrueStatics.timecpuCorrection;
  public static long timems()   {return TrueStatics.timerReader.readTimeMs();}
  public static long timens()   {return TrueStatics.timerReader.readTimeNs();}
  public static long timemcs()  {return timens()/1000;}
  public static double timesecDouble()  {return timens()*1.0E-9;}
  public static double timemsDouble()   {return timens()*1.0E-6;}
  public static double timemcsDouble()  {return timens()*1.0E-3;}
  public static long timecpu()  {return TrueStatics.timerReader.readTimeCpu();}

  public static long diffns(long t1, long t2) {return max(t2-t1-timensCorrection,0);}
  public static long diffns(long t1)          {return diffns(t1,timens());}
  public static double diffns(long t1, long t2, double divider) {return diffns(t1,t2)/divider;}
  public static double diffns(long t1, double divider)          {return diffns(t1)/divider;}
  public static long diffcpu(long t1, long t2) {return max(t2-t1-timecpuCorrection,0);}
  public static long diffcpu(long t1)          {return diffcpu(t1,timecpu());}
  public static double diffcpu(long t1, long t2, double divider) {return diffcpu(t1,t2)/divider;}
  public static double diffcpu(long t1, double divider)          {return diffcpu(t1)/divider;}

  /* Memory control */
  public static String memoryInfo() {
    return memoryInfo(false);
  }
  public static String memoryInfo(boolean detailed) {
    Runtime rt= Runtime.getRuntime();
    return memoryInfo(rt.freeMemory(),rt.totalMemory(),detailed);
  }
  public static String memoryInfo(long free, long total, boolean detailed) {
    return toS((total-free)/1024.0/1024.0,2)+"M used +"
      +toS(free/1024.0/1024.0,2)+"M free ="
      +toS(total/1024.0/1024.0,2)+"M"
      +(detailed? " ["+(total-free)+"/"+total+"]": "");
  }
  public static long runFinalization() {
    return gc(false,false);
  }
  public static long gc() {
    return gc(true,false);
  }
  public static long gc(boolean aggressive) {
    return gc(true,aggressive);
  }
  public static long gc(boolean doGc, boolean aggressive) {
    Runtime rt= Runtime.getRuntime();
    long free= rt.freeMemory(), total= rt.totalMemory();
    for (int count=1; count<=100; count++) {
      long t1= timems();
      long freeLast= free, totalLast= total;
      rt.runFinalization();
      long t2= timems();
      if (doGc) rt.gc();
      free= rt.freeMemory(); total= rt.totalMemory();
      long t3= timems();
      if (TrueStatics.debugLevel>=1) println(" **ATools** GC: "
        +(free>freeLast? memoryInfo(freeLast,totalLast,false)+" => ": "")
        +memoryInfo(free,total,free<=freeLast)
        +(free<=freeLast? ", no effect - ": " - ")
        +(t2-t1)+(doGc?"+"+(t3-t2):"")+" ms"
        +(aggressive? " ("+count+")": ""));
      if (!aggressive || free<=freeLast) return free;
    }
    return free;
  }

  private static Timer timerGc= null;
  public static void sheduleGc(long delayms) {
    sheduleGc(true,false,delayms);
  }
  public static void sheduleGc(boolean aggressive, long delayms) {
    sheduleGc(true,aggressive,delayms);
  }
  public static void sheduleGc(final boolean doGc, final boolean aggressive, long delayms) {
    if (timerGc!=null) timerGc.cancel();
    timerGc= new Timer(true);
    timerGc.schedule(new TimerTask() {
      public void run() {
        ATools.gc(doGc,aggressive);
      }
    },delayms);
  }

  private static long gcLastT= 0;
  private static long[] gcLastTimes= new long[10];
  public static synchronized void gc(double maximumCPUTimePercent) {
    maximumCPUTimePercent= median(maximumCPUTimePercent,0.0,1.0);
    long minLastTime= Long.MAX_VALUE;
    for (int k=0; k<gcLastTimes.length; k++) {
      if (gcLastTimes[k]!=0 && gcLastTimes[k]<minLastTime)
        minLastTime= gcLastTimes[k];
    }
    long t= timemcs();
    if (minLastTime!=Long.MAX_VALUE &&
      (t-gcLastT+minLastTime)*maximumCPUTimePercent<minLastTime) return;
    gc();
    gcLastT= timemcs();
    System.arraycopy(gcLastTimes,0,gcLastTimes,1,gcLastTimes.length-1);
    gcLastTimes[0]= gcLastT-t;
  }

  /* Status interface */
  public static String getStatus() { // Never returns null
    return getStatus(null);
  }
  public static String setStatus(String status) {
    return setStatus(null,status);
  }
  public static String getSystemStatus() { // Never returns null
    return getSystemStatus(null);
  }
  public static String setSystemStatus(String status) {
    return setSystemStatus(null,status);
  }
  public static String getStatus(String contextId) { // Never returns null
    if (TrueStatics.statusProducer!=null)
      return toS(TrueStatics.statusProducer.getStatus(contextId));
    return "";
  }
  public static String setStatus(String contextId, String status) {
    if (TrueStatics.statusProducer!=null)
      TrueStatics.statusProducer.setStatus(contextId,status);
    return status;
  }
  public static String getSystemStatus(String contextId) { // Never returns null
    if (TrueStatics.statusProducer instanceof TrueStatics.SystemStatusProducer)
      return toS(((TrueStatics.SystemStatusProducer)TrueStatics.statusProducer).getSystemStatus(contextId));
    return "";
  }
  public static String setSystemStatus(String contextId, String systemStatus) {
    if (TrueStatics.statusProducer instanceof TrueStatics.SystemStatusProducer)
      ((TrueStatics.SystemStatusProducer)TrueStatics.statusProducer).setSystemStatus(contextId,systemStatus);
    return systemStatus;
  }

  /* Classes and reflection */
  public static int length(Object someVector) {
    try {
      if (someVector instanceof String) return ((String)someVector).length();
      if (someVector instanceof StringBuffer) return ((StringBuffer)someVector).length();
      if (someVector instanceof List) return ((List)someVector).size();
      return Array.getLength(someVector);
    } catch (Exception e) {
      return 0;
    }
  }
  public static Object getElement(Object someVector, int k) {
    try {
      if (someVector instanceof String) return new Character(((String)someVector).charAt(k));
      if (someVector instanceof StringBuffer) return new Character(((StringBuffer)someVector).charAt(k));
      if (someVector instanceof List) return ((List)someVector).get(k);
      return Array.get(someVector,k);
    } catch (Exception e) {
      return null;
    }
  }
  public static int sizeOf(Class clazz) {
  // 0 means void; -1 means unknown or null class
    if (clazz==null) return -1;
    if (clazz==boolean.class) return -1;
    if (clazz==char.class)    return SIZE_OF_CHAR;
    if (clazz==byte.class)    return SIZE_OF_BYTE;
    if (clazz==short.class)   return SIZE_OF_SHORT;
    if (clazz==int.class)     return SIZE_OF_INT;
    if (clazz==long.class)    return SIZE_OF_LONG;
    if (clazz==float.class)   return SIZE_OF_FLOAT;
    if (clazz==double.class)  return SIZE_OF_DOUBLE;
    if (clazz==void.class)    return 0;
    return -1;
  }
  public static int sizeOfElement(Object someArray) {
  // -1 means unknown or not an array
    if (someArray==null) return -1;
    return sizeOf(someArray.getClass().getComponentType());
  }

  // Unlike System.getProperty("java.class.path"), the result of
  // following funtion cannot be changed during application work
  public static String getJavaClassPath() {
    return TrueStatics.javaClassPath;
  }
  public static String[] getJavaClassPathAsStrings() {
    return split(TrueStatics.javaClassPath,File.pathSeparator);
  }
  public static Class forName(String className) {
    return forName(className,null);
  }
  public static Class forName(String className, ClassLoader classLoader) {
  // Important: classLoader==null corresponds to "Class.forName(className)",
  // not to "Class.forName(className,true,null)"
    try {
      if (classLoader==null)
        return Class.forName(className);
      return Class.forName(className,true,classLoader);
    } catch(ClassNotFoundException e) {
      return null;
    } catch(NoClassDefFoundError e) {
      return null;
    } catch(NullPointerException e) {
      return null;
    }
  }

  public static void printCurrentStackTrace() {
    println(currentStackTraceInfoPrivate(4096));
  }
  public static void printCurrentStackTrace(int len) {
    println(currentStackTraceInfoPrivate(len));
  }
  public static String currentStackTraceInfo() {
    return currentStackTraceInfoPrivate(4096);
  }
  public static String currentStackTraceInfo(int len) {
    return currentStackTraceInfoPrivate(len);
    // All call depths should be equal
  }
  private static String currentStackTraceInfoPrivate(int len) {
    return " **ATools** STACK TRACE:\n\t"+toS(getCurrentStackTrace(2,len),"\n\t");
  }

  public static StackTraceElement[] getCurrentStackTrace() {
    return getCurrentStackTrace(1,Integer.MAX_VALUE);
  }
  public static StackTraceElement[] getCurrentStackTrace(int beginIndex, int len) {
    if (len<=0) return new StackTraceElement[0];
    try {
      throw new Exception();
    } catch (Exception e) {
      StackTraceElement[] stackTrace= e.getStackTrace();
      if (stackTrace.length<=1) return stackTrace;
      if (beginIndex<0) beginIndex= 0;
      if (beginIndex>=stackTrace.length-1) return new StackTraceElement[0];
      beginIndex++; // now we are sure that beginIndex<=Integer.MAX_VALUE-1
      if (len>=stackTrace.length-beginIndex) len= stackTrace.length-beginIndex;
      StackTraceElement[] result= new StackTraceElement[len];
      for (int k=0; k<len; k++)
        result[k]= stackTrace[k+beginIndex];
      return result;
    }
  }

  // If several class loaders are used, then it's possible
  // that some classes, named in the stack trace elements
  // returned by getCurrentStackTrace(), cannot be reached
  // by usual forName technique
  public static Class[] getCurrentStackTraceClasses() {
    List result= new ArrayList();
    for (int count=1; count<10000 /* to be on the safe side */; count++) {
      Class clazz= sun.reflect.Reflection.getCallerClass(count);
      if (clazz==null) break;
      result.add(clazz);
    }
    return (Class[])result.toArray(new Class[0]);
  }

  public static String toJavaName(Class clazz) {
    return toJavaName(clazz,true);
  }
  public static String toJavaName(Class clazz, boolean toDots) {
  // When toDots==true, it replaces ALL '$' characters in the class name with '.',
  // even if these characters don't separate local classes names, but are
  // the usual chararacters in Java identifier. For example,
  // the name of following class:
  //    public class MyClass$$ {...}
  // will be returned as "MyClass.."
    if (clazz==null) return "";
    if (clazz==boolean.class) return "boolean";
    if (clazz==char.class)    return "char";
    if (clazz==byte.class)    return "byte";
    if (clazz==short.class)   return "short";
    if (clazz==int.class)     return "int";
    if (clazz==long.class)    return "long";
    if (clazz==float.class)   return "float";
    if (clazz==double.class)  return "double";
    if (clazz==void.class)    return "void";
    Class componentClass= clazz.getComponentType();
    if (componentClass!=null) {
      return toJavaName(componentClass,toDots)+"[]";
    }
    return toDots? clazz.getName().replace('$','.'): clazz.getName();
  }
  public static String toJavaClassName(Object o) {
    if (o==null) return "null";
    return toJavaName(o.getClass());
  }
  public static Class getClass(Object object) {
    if (object==null) return null;
    return object.getClass();
  }
  public static String getClassName(Object object) {
    if (object==null) return null+"";
    return object.getClass().getName();
  }
  public static java.net.URL getClassURL(Class clazz) {
    String s= clazz.getName();
    s= s.substring(s.lastIndexOf(".")+1);
    return clazz.getResource(s+".class");
  }
  public static File getClassFile(Class clazz) {
  // The file will exist only if it is usual class-file,
  // not a part of JAR or Web resource
    String s= clazz.getName();
    s= s.substring(s.lastIndexOf(".")+1);
    s= clazz.getResource(s+".class").getFile();
    try {
      s= java.net.URLDecoder.decode(s,"UTF-8");
    } catch(java.io.UnsupportedEncodingException e) {
    }
    return new File(s);
  }

  /* Miscellaneous */
  public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
    try {
      return ResourceBundle.getBundle(baseName,locale,loader);
    } catch (MissingResourceException e) {
      return null;
    }
  }
  public static ResourceBundle getBundle(String baseName, Locale locale) {
    try {
      return ResourceBundle.getBundle(baseName,locale);
    } catch (MissingResourceException e) {
      return null;
    }
  }
  public static ResourceBundle getBundle(String baseName) {
    try {
      return ResourceBundle.getBundle(baseName);
    } catch (MissingResourceException e) {
      return null;
    }
  }
  public static String bundleGet(ResourceBundle b, String name) {
    return bundleGet(b,name,null);
  }
  public static String bundleGet(ResourceBundle b, String name, String defaultValue) {
    if (name==null) throw new NullPointerException("ATools.bundleGet: name argument cannot be null");
    if (defaultValue==null) defaultValue= name.replace('_',' ');
    if (b==null) return defaultValue;
    try {
      return b.getString(name);
    } catch (MissingResourceException e) {
      return defaultValue;
    }
  }
  public static String uniqueString() {
    return Long.toHexString(timeStartFrom1970)+"_"+Integer.toHexString(TrueStatics.getRndForUniqueString().nextInt());
  }
  public static String getNotEmptyProperty(String key, String defaultValue) {
    String result= System.getProperty(key,defaultValue);
    if (result==null || result.length()==0) result= defaultValue;
    return result;
  }

  /* Internal part */
  public static class TrueStatics {
    public static int debugLevel= 0;

    private static long printDelay= 0;
    private static StringBuffer printBuffer= null;
    private static Timer timerPrint= null;
    public static synchronized void printNoCorrection(String a) {
      if (printDelay<=0) {
        System.out.print(a); return;
      }
      if (printBuffer==null) printBuffer= new StringBuffer(a);
      else                   printBuffer.append(a);
      if (timerPrint!=null) timerPrint.cancel();
      timerPrint= new Timer(false);
      timerPrint.schedule(new TimerTask() {
        public void run() {
          TrueStatics.flushPrinting();
        }
      },printDelay);
    }
    public synchronized static void flushPrinting() {
      if (printDelay<=0) return;
      if (printBuffer!=null) System.out.print(printBuffer);
      printBuffer= null;
    }
    public static synchronized long getPrintDelay() {
      return printDelay;
    }
    public static synchronized void setPrintDelay(long ms) {
      flushPrinting();
      printDelay= ms;
    }

    public interface TimerReader {
      public long readTimeMs();
      public long readTimeNs();
      public long readTimeCpu();
    }
    public static class TimerReaderDefault implements TimerReader {
      public long readTimeMs()  {return System.currentTimeMillis();}
      public long readTimeNs()  {return System.currentTimeMillis()*1000000;}
      public long readTimeCpu() {return readTimeNs();}
    }
    public static TimerReader timerReader;
    // Allows "ATools.java" to be delivered without ATimer class
    static {
      try {
        timerReader= (TimerReader)(Class.forName("algart.lib.legacy.ATimer").newInstance());
      } catch (Exception e) {
        timerReader= new TimerReaderDefault();
      }
    }
    public static long timeStart= timems();
    public static long timeStartFrom1970= System.currentTimeMillis();
    // - Must be public to enable ATools to be DCO-loaded
    public static final long timensCorrection;
    public static final long timecpuCorrection;
    static {
      long tmin= Long.MAX_VALUE;
      for (int k=0; k<1000; k++) {
        long t1= timens();
        long t2= timens();
        if (t2-t1<tmin) tmin= t2-t1;
      }
      timensCorrection= tmin;
      tmin= Long.MAX_VALUE;
      for (int k=0; k<1000; k++) {
        long t1= timecpu();
        long t2= timecpu();
        if (t2-t1<tmin) tmin= t2-t1;
      }
      timecpuCorrection= tmin;
    }

    private static long classLoaderId= 0;
    public static long getClassLoaderId() {
      return classLoaderId++;
    }

    public interface StatusProducer {
      public String getStatus(Object contextId);
      public void setStatus(Object contextId, String status);
    }
    public interface SystemStatusProducer {
      public String getSystemStatus(Object contextId);
      public void setSystemStatus(Object contextId, String systemStatus);
    }
    public interface StatusesProducer extends StatusProducer,SystemStatusProducer {
    }
    public static StatusProducer statusProducer;

    public static final String javaClassPath= System.getProperty("java.class.path");

    private static Random rndForUniqueString= null;
    public static synchronized Random getRndForUniqueString() {
      if (rndForUniqueString==null) rndForUniqueString= new Random(timecpu());
      return rndForUniqueString;
    }

    public static class InternalFlusher implements net.algart.lib.Out.Flusher { // compatibility issues
      public void flush() {
        flushPrinting();
      }
    }
  }
}
