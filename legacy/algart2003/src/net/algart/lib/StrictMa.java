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

package net.algart.lib;

/*Repeat(INCLUDE_FROM_FILE, Ma.java, title-doc)
    \bMa ==> StrictMa
    !! Auto-generated: NOT EDIT !! */
/**
 * <p>An analog of standard <code>java.lang.StrictMath</code> class.
 * It contains all static methods and constants declared in
 * <code>java.lang.StrictMath</code> class and a convenient set of
 * the following additional methods:<ul>
 *
 * <li>{@link #iround(double)}
 * <li>{@link #minu(byte, byte)}
 * <li>{@link #maxu(byte, byte)}
 * <li>{@link #minu(short, short)}
 * <li>{@link #maxu(short, short)}
 * <li>{@link #minu(int, int)}
 * <li>{@link #maxu(int, int)}
 * <li>{@link #minu(long, long)}
 * <li>{@link #maxu(long, long)}
 * <li>{@link #min(byte, byte, byte)}
 * <li>{@link #max(byte, byte, byte)}
 * <li>{@link #median(byte, byte, byte)}
 * <li>{@link #min(short, short, short)}
 * <li>{@link #max(short, short, short)}
 * <li>{@link #median(short, short, short)}
 * <li>{@link #min(int, int, int)}
 * <li>{@link #max(int, int, int)}
 * <li>{@link #median(int, int, int)}
 * <li>{@link #min(long, long, long)}
 * <li>{@link #max(long, long, long)}
 * <li>{@link #median(long, long, long)}
 * <li>{@link #min(float, float, float)}
 * <li>{@link #max(float, float, float)}
 * <li>{@link #median(float, float, float)}
 * <li>{@link #min(double, double, double)}
 * <li>{@link #max(double, double, double)}
 * <li>{@link #median(double, double, double)}
 * <li>{@link #between(char, char, char)}
 * <li>{@link #between(byte, byte, byte)}
 * <li>{@link #between(short, short, short)}
 * <li>{@link #between(int, int, int)}
 * <li>{@link #between(long, long, long)}
 * <li>{@link #between(float, float, float)}
 * <li>{@link #between(float, float, float, float)}
 * <li>{@link #between(double, double, double)}
 * <li>{@link #between(double, double, double, double)}
 * <li>{@link #sqr(int)}}
 * <li>{@link #sqr(long)}
 * <li>{@link #sqr(float)}
 * <li>{@link #sqr(double)}
 * </ul>
 *
 * <p>Be careful: <code>min</code> and <code>max</code> methods for float and
 * double values are <b>not identical</b> to the same methods from standard
 * <code>java.lang.StrictMath</code> class.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */
/*Repeat.IncludeEnd*/

public strictfp class StrictMa implements TrueStatic {
// TrueStatic is necessary because the random generator should be global

  /*Repeat(INCLUDE_FROM_FILE, Ma.java, main)
      \bMa ==> StrictMa
      !! Auto-generated: NOT EDIT !! */
  // Don't let anyone instantiate or extend this class (excepting extending by JavaCompiler.StrictMathFunctionsForFormulaInternal)
  StrictMa() {}

  /**
   * An equivalent of <code>java.lang.StrictMath.E</code>
   */
  public static final double E = java.lang.StrictMath.E;
  /**
   * An equivalent of <code>java.lang.StrictMath.PI</code>
   */
  public static final double PI = java.lang.StrictMath.PI;

  /**
   * An equivalent of <code>(int)java.lang.StrictMath.round(a)</code> call.
   * @param a   the argument
   * @return    the result of <code>(int)java.lang.StrictMath.round(a)</code> call
   */
  public static int iround(double a) {
    return (int)java.lang.StrictMath.round(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.round(a)</code> call.
   * @param a   the argument
   * @return    the result of standard <code>java.lang.StrictMath.round(a)</code> call
   */
  public static int round(float a) {
    return java.lang.StrictMath.round(a);
  }/**
   * An equivalent of <code>java.lang.StrictMath.round(a)</code> call.
   * @param a   the argument
   * @return    the result of standard <code>java.lang.StrictMath.round(a)</code> call
   */
  public static long round(double a) {
    return java.lang.StrictMath.round(a);
  }/**
   * An equivalent of <code>java.lang.StrictMath.abs</code> method.
   * @param a   the argument
   * @return    the absolute value of the argument
   */
  public static int abs(int a) {
    return a >=0? a: 0-a;
  }/**
   * An equivalent of <code>java.lang.StrictMath.abs</code> method.
   * @param a   the argument
   * @return    the absolute value of the argument
   */
  public static long abs(long a) {
    return a >=0? a: 0-a;
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.abs</code> method.
   * @param a   the argument
   * @return    the absolute value of the argument
   */
  public static double abs(double a) {
    return a >=0.0D? a: 0.0D-a;
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.abs</code> method.
   * @param a   the argument
   * @return    the absolute value of the argument
   */
  public static float abs(float a) {
    return a >=0.0F? a: 0.0F-a;
  }/**
   * Returns minimum of two <b>unsigned</b> 8-bit integer values.
   * An equivalent of <code>(a&amp;0xFF)&nbsp;&lt;=&nbsp;(b&amp;0xFF)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned minimum of two 8-bit integer numbers
   */
  public static byte minu(byte a, byte b) {
    return (a&0xFF) <= (b&0xFF)? a: b;
  }
  /**
   * Returns maximum of two <b>unsigned</b> 8-bit integer values.
   * An equivalent of <code>(a&amp;0xFF)&nbsp;&gt;=&nbsp;(b&amp;0xFF)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned maximum of two 8-bit integer numbers
   */
  public static byte maxu(byte a, byte b) {
    return (a&0xFF) >= (b&0xFF)? a: b;
  }
  /**
   * Returns minimum of two <b>unsigned</b> 16-bit integer values.
   * An equivalent of <code>((char)a)&nbsp;&lt;=&nbsp;((char)b)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned minimum of two 16-bit integer numbers
   */
  public static short minu(short a, short b) {
    return ((char)a) <= ((char)b)? a: b;
  }
  /**
   * Returns maximum of two <b>unsigned</b> 16-bit integer values.
   * An equivalent of <code>((char)a)&nbsp;&gt;=&nbsp;((char)b)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned maximum of two 16-bit integer numbers
   */
  public static short maxu(short a, short b) {
    return ((char)a) >= ((char)b)? a: b;
  }

  /**
   * Returns minimum of two <b>unsigned</b> 32-bit integer values.
   * An equivalent of <code>(a^0x80000000)&nbsp;&lt;=&nbsp;(b^0x80000000)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned minimum of two 32-bit integer numbers
   */
  public static int minu(int a, int b) {
    return (a^0x80000000) <= (b^0x80000000)? a: b;
  }
  /**
   * Returns maximum of two <b>unsigned</b> 32-bit integer values.
   * An equivalent of <code>(a^0x80000000)&nbsp;&gt;=&nbsp;(b^0x80000000)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned maximum of two 32-bit integer numbers
   */
  public static int maxu(int a, int b) {
    return (a^0x80000000) >= (b^0x80000000)? a: b;
  }

  /**
   * Returns minimum of two <b>unsigned</b> 64-bit integer values.
   * An equivalent of <code>(a^0x8000000000000000L)&nbsp;&lt;=&nbsp;(b^0x8000000000000000L)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned minimum of two 64-bit integer numbers
   */
  public static long minu(long a, long b) {
    return (a^0x8000000000000000L) <= (b^0x8000000000000000L)? a: b;
  }
  /**
   * Returns maximum of two <b>unsigned</b> 64-bit integer values.
   * An equivalent of <code>(a^0x8000000000000000L)&nbsp;&gt;=&nbsp;(b^0x8000000000000000L)?&nbsp;a:&nbsp;b</code>
   * @param a   1st argument
   * @param b   2nd argument
   * @return    unsigned maximum of two 64-bit integer numbers
   */
  public static long maxu(long a, long b) {
    return (a^0x8000000000000000L) >= (b^0x8000000000000000L)? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&lt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    minimum of two numbers ("byte" type)
   */
  public static byte min(byte a, byte b) {
    return a <= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&gt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    maximum of two numbers ("byte" type)
   */
  public static byte max(byte a, byte b) {
    return a >= b? a: b;
  }/**
   * An equivalent of <code>a&nbsp;&lt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    minimum of two numbers ("short" type)
   */
  public static short min(short a, short b) {
    return a <= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&gt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    maximum of two numbers ("short" type)
   */
  public static short max(short a, short b) {
    return a >= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&lt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    minimum of two numbers ("int" type)
   */
  public static int min(int a, int b) {
    return a <= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&gt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    maximum of two numbers ("int" type)
   */
  public static int max(int a, int b) {
    return a >= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&lt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    minimum of two numbers ("long" type)
   */
  public static long min(long a, long b) {
    return a <= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&gt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    maximum of two numbers ("long" type)
   */
  public static long max(long a, long b) {
    return a >= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&lt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    minimum of two numbers ("float" type)
   */
  public static float min(float a, float b) {
    return a <= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&gt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    maximum of two numbers ("float" type)
   */
  public static float max(float a, float b) {
    return a >= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&lt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    minimum of two numbers ("double" type)
   */
  public static double min(double a, double b) {
    return a <= b? a: b;
  }
  /**
   * An equivalent of <code>a&nbsp;&gt;=&nbsp;b&#63;&nbsp;a:&nbsp;b</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @return    maximum of two numbers ("double" type)
   */
  public static double max(double a, double b) {
    return a >= b? a: b;
  }/**
   * Quicker equivalent of <code>min(a,min(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    minimum of 3 numbers ("byte" type)
   */
  public static byte min(byte a, byte b, byte c) {
    return b < c? (a < b? a: b): (a < c? a: c);
  }
  /**
   * Quicker equivalent of <code>max(a,max(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    maximum of 3 numbers ("byte" type)
   */
  public static byte max(byte a, byte b, byte c) {
    return b > c? (a > b? a: b): (a > c? a: c);
  }
  /**
   * Returns the median of 3 numbers: the 2nd number in the increasing order.
   * Can be interpreted as the first number <code>a</code> "cut" by the range
   * <code>min(b,c)...max(b,c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    median of 3 numbers ("byte" type)
   */
  public static byte median(byte a, byte b, byte c) {
    return b < c? (a < b? b: a > c? c: a): (a < c? c: a > b? b: a);
  }/**
   * Quicker equivalent of <code>min(a,min(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    minimum of 3 numbers ("short" type)
   */
  public static short min(short a, short b, short c) {
    return b < c? (a < b? a: b): (a < c? a: c);
  }
  /**
   * Quicker equivalent of <code>max(a,max(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    maximum of 3 numbers ("short" type)
   */
  public static short max(short a, short b, short c) {
    return b > c? (a > b? a: b): (a > c? a: c);
  }
  /**
   * Returns the median of 3 numbers: the 2nd number in the increasing order.
   * Can be interpreted as the first number <code>a</code> "cut" by the range
   * <code>min(b,c)...max(b,c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    median of 3 numbers ("short" type)
   */
  public static short median(short a, short b, short c) {
    return b < c? (a < b? b: a > c? c: a): (a < c? c: a > b? b: a);
  }
  /**
   * Quicker equivalent of <code>min(a,min(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    minimum of 3 numbers ("int" type)
   */
  public static int min(int a, int b, int c) {
    return b < c? (a < b? a: b): (a < c? a: c);
  }
  /**
   * Quicker equivalent of <code>max(a,max(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    maximum of 3 numbers ("int" type)
   */
  public static int max(int a, int b, int c) {
    return b > c? (a > b? a: b): (a > c? a: c);
  }
  /**
   * Returns the median of 3 numbers: the 2nd number in the increasing order.
   * Can be interpreted as the first number <code>a</code> "cut" by the range
   * <code>min(b,c)...max(b,c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    median of 3 numbers ("int" type)
   */
  public static int median(int a, int b, int c) {
    return b < c? (a < b? b: a > c? c: a): (a < c? c: a > b? b: a);
  }
  /**
   * Quicker equivalent of <code>min(a,min(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    minimum of 3 numbers ("long" type)
   */
  public static long min(long a, long b, long c) {
    return b < c? (a < b? a: b): (a < c? a: c);
  }
  /**
   * Quicker equivalent of <code>max(a,max(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    maximum of 3 numbers ("long" type)
   */
  public static long max(long a, long b, long c) {
    return b > c? (a > b? a: b): (a > c? a: c);
  }
  /**
   * Returns the median of 3 numbers: the 2nd number in the increasing order.
   * Can be interpreted as the first number <code>a</code> "cut" by the range
   * <code>min(b,c)...max(b,c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    median of 3 numbers ("long" type)
   */
  public static long median(long a, long b, long c) {
    return b < c? (a < b? b: a > c? c: a): (a < c? c: a > b? b: a);
  }
  /**
   * Quicker equivalent of <code>min(a,min(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    minimum of 3 numbers ("float" type)
   */
  public static float min(float a, float b, float c) {
    return b < c? (a < b? a: b): (a < c? a: c);
  }
  /**
   * Quicker equivalent of <code>max(a,max(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    maximum of 3 numbers ("float" type)
   */
  public static float max(float a, float b, float c) {
    return b > c? (a > b? a: b): (a > c? a: c);
  }
  /**
   * Returns the median of 3 numbers: the 2nd number in the increasing order.
   * Can be interpreted as the first number <code>a</code> "cut" by the range
   * <code>min(b,c)...max(b,c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    median of 3 numbers ("float" type)
   */
  public static float median(float a, float b, float c) {
    return b < c? (a < b? b: a > c? c: a): (a < c? c: a > b? b: a);
  }
  /**
   * Quicker equivalent of <code>min(a,min(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    minimum of 3 numbers ("double" type)
   */
  public static double min(double a, double b, double c) {
    return b < c? (a < b? a: b): (a < c? a: c);
  }
  /**
   * Quicker equivalent of <code>max(a,max(b,c))</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    maximum of 3 numbers ("double" type)
   */
  public static double max(double a, double b, double c) {
    return b > c? (a > b? a: b): (a > c? a: c);
  }
  /**
   * Returns the median of 3 numbers: the 2nd number in the increasing order.
   * Can be interpreted as the first number <code>a</code> "cut" by the range
   * <code>min(b,c)...max(b,c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    median of 3 numbers ("double" type)
   */
  public static double median(double a, double b, double c) {
    return b < c? (a < b? b: a > c? c: a): (a < c? c: a > b? b: a);
  }/**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("char" type)
   */
  public static boolean between(char a, char b, char c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }/**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("byte" type)
   */
  public static boolean between(byte a, byte b, byte c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }
  /**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("short" type)
   */
  public static boolean between(short a, short b, short c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }
  /**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("int" type)
   */
  public static boolean between(int a, int b, int c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }
  /**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("long" type)
   */
  public static boolean between(long a, long b, long c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }
  /**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("float" type)
   */
  public static boolean between(float a, float b, float c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }
  /**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)</code>.
   * @param a   1st argument
   * @param b   2nd argument
   * @param c   3rd argument
   * @return    <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values ("double" type)
   */
  public static boolean between(double a, double b, double c) {
    return b <= c? a >= b && a <= c: a >= c && a <= b;
  }/**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;+&nbsp;epsilon&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)&nbsp;-&nbsp;epsilon</code>.
   * @param a         1st argument
   * @param b         2nd argument
   * @param c         3rd argument
   * @param epsilon   permitted error of the check
   * @return          <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values with <code>epsilon</code> error ("float" type)
   */
  public static boolean between(float a, float b, float c, float epsilon) {
    return b <= c? a >= b+epsilon && a <= c-epsilon: a >= c+epsilon && a <= b-epsilon;
  }/**
   * An equivalent of
   * <code>a&nbsp;&gt;=&nbsp;min(b,&nbsp;c)&nbsp;+&nbsp;epsilon&nbsp;&amp;&amp;&nbsp;a&nbsp;&lt;=&nbsp;max(b,&nbsp;c)&nbsp;-&nbsp;epsilon</code>.
   * @param a         1st argument
   * @param b         2nd argument
   * @param c         3rd argument
   * @param epsilon   permitted error of the check
   * @return          <code>true</code> if the <code>a</code> value is between <code>b</code>
   *    and <code>c</code> values with <code>epsilon</code> error ("double" type)
   */
  public static boolean between(double a, double b, double c, double epsilon) {
    return b <= c? a >= b+epsilon && a <= c-epsilon: a >= c+epsilon && a <= b-epsilon;
  }/**
   * An equivalent of <code>a&nbsp;*&nbsp;a</code>
   * @param a the argument
   * @return  the square of the argument
   */
  public static int sqr(int a) {
    return a*a;
  }/**
   * An equivalent of <code>a&nbsp;*&nbsp;a</code>
   * @param a the argument
   * @return  the square of the argument
   */
  public static long sqr(long a) {
    return a*a;
  }
  /**
   * An equivalent of <code>a&nbsp;*&nbsp;a</code>
   * @param a the argument
   * @return  the square of the argument
   */
  public static float sqr(float a) {
    return a*a;
  }
  /**
   * An equivalent of <code>a&nbsp;*&nbsp;a</code>
   * @param a the argument
   * @return  the square of the argument
   */
  public static double sqr(double a) {
    return a*a;
  }/**
   * An equivalent of <code>java.lang.StrictMath.sin(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.sin(a)</code> call
   */
  public static double sin(double a) {
    return java.lang.StrictMath.sin(a);
  }/**
   * An equivalent of <code>java.lang.StrictMath.cos(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.cos(a)</code> call
   */
  public static double cos(double a) {
    return java.lang.StrictMath.cos(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.tan(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.tan(a)</code> call
   */
  public static double tan(double a) {
    return java.lang.StrictMath.tan(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.asin(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.asin(a)</code> call
   */
  public static double asin(double a) {
    return java.lang.StrictMath.asin(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.acos(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.acos(a)</code> call
   */
  public static double acos(double a) {
    return java.lang.StrictMath.acos(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.atan(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.atan(a)</code> call
   */
  public static double atan(double a) {
    return java.lang.StrictMath.atan(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.toRadians(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.toRadians(a)</code> call
   */
  public static double toRadians(double a) {
    return java.lang.StrictMath.toRadians(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.toDegrees(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.toDegrees(a)</code> call
   */
  public static double toDegrees(double a) {
    return java.lang.StrictMath.toDegrees(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.exp(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.exp(a)</code> call
   */
  public static double exp(double a) {
    return java.lang.StrictMath.exp(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.log(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.log(a)</code> call
   */
  public static double log(double a) {
    return java.lang.StrictMath.log(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.sqrt(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.sqrt(a)</code> call
   */
  public static double sqrt(double a) {
    return java.lang.StrictMath.sqrt(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.ceil(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.ceil(a)</code> call
   */
  public static double ceil(double a) {
    return java.lang.StrictMath.ceil(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.floor(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.floor(a)</code> call
   */
  public static double floor(double a) {
    return java.lang.StrictMath.floor(a);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.rint(a)</code>
   * @param a the argument
   * @return  the result of standard <code>java.lang.StrictMath.rint(a)</code> call
   */
  public static double rint(double a) {
    return java.lang.StrictMath.rint(a);
  }/**
   * An equivalent of <code>java.lang.StrictMath.atan2(a,b)</code>
   * @param a 1st argument
   * @param b 2nd argument
   * @return  the result of standard <code>java.lang.StrictMath.atan2(a,b)</code> call
   */
  public static double atan2(double a, double b) {
    return java.lang.StrictMath.atan2(a, b);
  }/**
   * An equivalent of <code>java.lang.StrictMath.IEEEremainder(a,b)</code>
   * @param a 1st argument
   * @param b 2nd argument
   * @return  the result of standard <code>java.lang.StrictMath.IEEEremainder(a,b)</code> call
   */
  public static double IEEEremainder(double a, double b) {
    return java.lang.StrictMath.IEEEremainder(a, b);
  }
  /**
   * An equivalent of <code>java.lang.StrictMath.pow(a,b)</code>
   * @param a 1st argument
   * @param b 2nd argument
   * @return  the result of standard <code>java.lang.StrictMath.pow(a,b)</code> call
   */
  public static double pow(double a, double b) {
    return java.lang.StrictMath.pow(a, b);
  }/*Repeat.IncludeEnd*/
}