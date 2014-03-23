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

/**
 * <p>Title:     <b>Algart Timer</b><br>
 * Description:  An interface to native class allowing to measure the time with high resolution; used in ATools<br>
 * @author       Daniel Alievsky
 * @version      1.0
 * @deprecated
 */

public class ATimer implements TrueStatic, ATools.TrueStatics.TimerReader {

  public static boolean isNative() {
    return ATimerNative.loaded;
  }
  public static long timems() {
    if (isNative()) return ATimerNative.timems();
    return System.currentTimeMillis();
  }
  public static long timens() {
    if (isNative()) return ATimerNative.timens();
    return System.currentTimeMillis()*1000000;
  }

  public static boolean isTimecpuSupported() {
    return ATimerNative.timecpuSupported!=0;
  }
  public static long timecpu() {
    if (ATimerNative.timecpuSupported==0) return timens();
    return ATimerNative.timecpuInternal();
  }
  public long readTimeMs()  {return timems();}
  public long readTimeNs()  {return timens();}
  public long readTimeCpu() {return timecpu();}
}

class ATimerNative implements TrueStatic {
  static native long timens();
  static long timems() {return timens()/1000000;}

  static int timecpuSupported= 0;
  static native int getTimecpuSupportedInternal();
  static native long timecpuInternal();
  static boolean loaded= false;
  /* // Native support is very deprecated
  static {
    try {
      System.loadLibrary("ATimerNative");
      loaded= true;
      timecpuSupported= getTimecpuSupportedInternal();
    } catch (UnsatisfiedLinkError e) {
    } catch (SecurityException e) {
    }
  }
  */
}
