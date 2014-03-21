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

/**
 * <p>An interface to native class allowing to measure the time
 * with high resolution; used in {@link Tools}.
 *
 * @author  Daniel Alievsky
 * @version 1.1
 */

public final class Timing implements TrueStatic {

  /**
   * Don't let anyone instantiate this class.
   */
  private Timing() {}

  public static long timeStart= timems();
  public static final long timeStartFrom1970 = System.currentTimeMillis();
  public static final long timensCorrection;
  public static final long timecpuCorrection;
  public static void decreaseTimeStart(long decrement) {
    // Useful in a case when Timer.class is firstly loaded
    // since some time after starting application
    timeStart-= decrement;
  }

  public static long timems() {
    if (isNative()) return TimingNative.timems();
    return System.currentTimeMillis();
  }
  public static long timemcs() {
    return timens() / 1000;
  }
  public static long timens() {
    if (isNative()) return TimingNative.timens();
    return System.currentTimeMillis() * 1000000;
  }

  public static boolean isTimecpuSupported() {
    return TimingNative.timecpuSupported != 0;
  }
  public static long timecpu() {
    if (TimingNative.timecpuSupported == 0) return timens();
    return TimingNative.timecpuInternal();
  }
  public static double timesecDouble() {
    return timens() * 1.0E-9;
  }
  public static double timemsDouble() {
    return timens() * 1.0E-6;
  }
  public static double timemcsDouble() {
    return timens() * 1.0E-3;
  }

  public static long diffns(long t1, long t2) {
    return Ma.max(t2-t1-timensCorrection,0);
  }
  public static long diffns(long t1) {
    return diffns(t1,timens());
  }
  public static double diffns(long t1, long t2, double divider) {
    return diffns(t1,t2)/divider;
  }
  public static double diffns(long t1, double divider) {
    return diffns(t1) / divider;
  }
  public static long diffcpu(long t1, long t2) {
    return Ma.max(t2-t1-timecpuCorrection,0);
  }
  public static long diffcpu(long t1) {
    return diffcpu(t1,timecpu());
  }
  public static double diffcpu(long t1, long t2, double divider) {
    return diffcpu(t1,t2) / divider;
  }
  public static double diffcpu(long t1, double divider) {
    return diffcpu(t1) / divider;
  }

  /**
   * Returns a string that is unique for every call of this method
   * with very high probability (almost 100%). This string is short
   * enough (10-20 characters) and is based on the string representation
   * of random numbers.
   *
   * @return New unique random string.
   */
  /*Repeat.IncludeEnd*/
  public static String uniqueString() {
    return Long.toHexString(timeStartCodePrivate) + "_" + Integer.toHexString(getRndForUniqueStringPrivate().nextInt());
  }


  private static final long timeStartCodePrivate = System.currentTimeMillis() ^ timens();
  private static java.util.Random rndForUniqueStringPrivate = null;
  private static synchronized java.util.Random getRndForUniqueStringPrivate() {
    // it's important to create rndForUniqueString at a random moment
    // of the first uniqueString() call, not when the application starts!
    if (rndForUniqueStringPrivate == null) rndForUniqueStringPrivate = new java.util.Random(timens());
    return rndForUniqueStringPrivate;
  }


  public static boolean isNative() {
    return TimingNative.loaded;
  }
  public static String initializationExceptionMessage() {
    return TimingNative.initializationExceptionMessage;
  }

  static {
    long tmin = Long.MAX_VALUE;
    for (int k = 0; k < 1000; k++) {
      long t1 = timens();
      long t2 = timens();
      if (t2-t1 < tmin) tmin = t2-t1;
    }
    timensCorrection = tmin;
    tmin = Long.MAX_VALUE;
    for (int k = 0; k < 1000; k++) {
      long t1 = timecpu();
      long t2 = timecpu();
      if (t2-t1 < tmin) tmin = t2-t1;
    }
    timecpuCorrection= tmin;
  }
}

class TimingNative implements TrueStatic {
  static native long timens();
  static long timems() {return timens() / 1000000;}

  static int timecpuSupported = 0;
  static native int getTimecpuSupportedInternal();
  static native long timecpuInternal();
  static boolean loaded = false;
  static final String initializationExceptionMessage;
  static {
    String message = null;
    if (GlobalProperties.getBooleanProperty(GlobalProperties.JAVA_NATIVE_ENABLED_PROPERTY_NAME,true))
      try {
        System.loadLibrary(TimingNative.class.getName().replace('.','_'));
        loaded = true;
        timecpuSupported= getTimecpuSupportedInternal();
      } catch (UnsatisfiedLinkError e) {
        message = e.toString();
      } catch (SecurityException e) {
        message = e.toString();
      }
    initializationExceptionMessage = message;
  }
}
