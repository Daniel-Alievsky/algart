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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

/**
 * <p>A collection of static methods for getting Java Virtual Machine
 * information, in particular, for JVM version control.
 *
 * <p>This class can be executed since JVM 1.0 and can be compiled since JDK 1.1
 *
 * @version 1.0
 * @author  Daniel Alievsky
 * @since   JVM1.0, JDK1.1
 */

public final class JVM {

  /**
   * Don't let anyone instantiate this class.
   */
  private JVM() {}

  /**
   * True if Integer class implements an interface named "java.io.Serializable".
   * <p>Can be used to detect Java 1.1 compatibility.
   */
  public static final boolean JAVA_11_SERIALIZATION_SUPPORTED;

  /**
   * True if {@link #JAVA_11_SERIALIZATION_SUPPORTED} is true and
   * the call <code>Class.forName("java.lang.reflect.Method")</code> works
   * without exception.
   * <p>Can be used to detect Java 1.1 compatibility with more assurance than
   * {@link #JAVA_11_SERIALIZATION_SUPPORTED}.
   */
  public static final boolean JAVA_11_REFLECTION_SUPPORTED;

  /**
   * True if both {@link #JAVA_11_SERIALIZATION_SUPPORTED} and
   * {@link #JAVA_11_REFLECTION_SUPPORTED} constants are true and if
   * Integer class implements an interface named "java.lang.Comparable".
   * <p>Can be used to detect Java 1.2 compatibility.
   */
  public static final boolean JAVA_12_COMPARABLE_SUPPORTED;

  /**
   * True if all {@link #JAVA_11_SERIALIZATION_SUPPORTED},
   * {@link #JAVA_11_REFLECTION_SUPPORTED} and {@link #JAVA_12_COMPARABLE_SUPPORTED}
   * constants are true and if Java 2 collections (<code>java.util.Map</code>,
   * <code>java.util.HashMap</code>, etc.) are found.
   * <p>Can be used to detect Java 1.2 compatibility with more assurance than
   * {@link #JAVA_12_COMPARABLE_SUPPORTED}.
   */
  public static final boolean JAVA_12_COLLECTIONS_SUPPORTED;

  public static String toJavaName(Class clazz, boolean dollarsToDotsInNestedClasses) {
    if (clazz == null)          return "";
    if (clazz == boolean.class) return "boolean";
    if (clazz == char.class)    return "char";
    if (clazz == byte.class)    return "byte";
    if (clazz == short.class)   return "short";
    if (clazz == int.class)     return "int";
    if (clazz == long.class)    return "long";
    if (clazz == float.class)   return "float";
    if (clazz == double.class)  return "double";
    if (clazz == void.class)    return "void";
    Class componentClass = clazz.getComponentType();
    if (componentClass != null) {
      return toJavaName(componentClass,dollarsToDotsInNestedClasses) + "[]";
    }
    if (!dollarsToDotsInNestedClasses) return clazz.getName();
    StringBuffer sb = new StringBuffer(clazz.getName());
    for (;;) {
      Class declaringClass = clazz.getDeclaringClass();
      if (declaringClass == null) break;
      int index = declaringClass.getName().length();
      if (sb.charAt(index) != '$') throw new InternalError("Illegal name of the class that declares " + clazz + " in " + JVM.class.getName() + ".toJavaName");
      sb.setCharAt(index,'.');
      clazz = declaringClass;
    }
    return sb.toString();
  }

  public static String toJavaClassName(Object o) {
    if (o == null) return "null";
    return toJavaName(o.getClass(),true);
  }


  public static class CannotCheckStackTrace extends Exception {
    public CannotCheckStackTrace() {
      super();
    }
    public CannotCheckStackTrace(String message) {
      super(message);
    }
  }

  public static boolean isInInstanceInitialization(Class clazz) throws CannotCheckStackTrace {
    return isInMethod(clazz,"<init>");
  }
  public static boolean isInStaticInitialization(Class clazz) throws CannotCheckStackTrace {
    return isInMethod(clazz,"<clinit>");
  }

  public static boolean isInMethod(Class clazz, String methodName) throws CannotCheckStackTrace {
    try {
      try {
        throw new Throwable();
      } catch (Throwable dummy) {
        return INTERNAL_COMPATIBILITY_TOOL.isInMethod(dummy,clazz,methodName);
      }
    } catch (SecurityException e) {
      throw new CannotCheckStackTrace(e.toString());
    }
  }


  static class InternalCompatibilityTool {
  // This implementation is compatible with JDK 1.0
    boolean isInMethod(Throwable dummy, Class clazz, String methodName) throws CannotCheckStackTrace {
      String stackTrace = getStackTraceText(dummy);
      boolean thisMethodFound = false;
      String className1 = clazz.getName();              // used by most JVM
      String className2 = className1.replace('.','/');  // used by Microsoft Internet Explorer
      String thisClassName1 = JVM.class.getName();
      String thisClassName2 = thisClassName1.replace('.','/');
      StringTokenizer stringTokenizer = new StringTokenizer(stackTrace);
      for (; stringTokenizer.hasMoreTokens(); ) {
        String stackTraceLine = stringTokenizer.nextToken();
        if (stackTraceLine.indexOf(className1 + "." + methodName) != -1
          || stackTraceLine.indexOf(className2 + "." + methodName) != -1)
          return true;
        if (stackTraceLine.indexOf(thisClassName1 + ".isInMethod") != -1
          || stackTraceLine.indexOf(thisClassName2 + ".isInMethod") != -1)
          thisMethodFound = true;
      }
      if (!thisMethodFound)
        throw new CannotCheckStackTrace("Unknown stack trace format: cannot find " + thisClassName1 + ".isInMethod method");
      return false;
    }

    String getStackTraceText(Throwable dummy) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(byteArrayOutputStream);
      dummy.printStackTrace(printStream);
      printStream.flush();
      printStream.close();
      byte[] bytes = byteArrayOutputStream.toByteArray();
      char[] chars = new char[bytes.length];
      for (int k = 0; k < bytes.length; k++) chars[k] = (char)(bytes[k]&0xFF);
      // such a code provides compatibility with Java 1.0 and avoids deprecation warnings
      return String.valueOf(chars);
    }

    String decOrExpPrivate(double v, int d, boolean exponentForm) {
      if (exponentForm)
        return String.valueOf(v);
        if (d <= 0)
        return String.valueOf(Ma.round(v));
        for (int k = 1; k <= d; k++) v *= 10;
        String s = ""+Ma.round(Ma.abs(v));
        int len = s.length();
      while (len < d+1) { // it is needed when |v|<1.0
        s = "0" + s; len++;
      }
      return ((v < 0.0)? "-": "") + s.substring(0,len-d) + "." + s.substring(len-d);
    }

    String spaced(long v, char space) {
      return String.valueOf(v);
    }

    // Returns null if the object is unknown (unlike Out.join that throws an exception)
    String join(Object o, String separator) {
      if (o == null) return "";
      if (o instanceof Enumeration) {
        Enumeration en = (Enumeration)o;
        if (!en.hasMoreElements()) return "";
        StringBuffer sb = new StringBuffer(String.valueOf(en.nextElement()));
        for (; en.hasMoreElements(); ) {
          sb.append(separator).append(en.nextElement());
        }
        return sb.toString();
      }
      if (o instanceof Dictionary) {
        Enumeration en = ((Dictionary)o).keys();
        if (!en.hasMoreElements()) return "";
        StringBuffer sb = new StringBuffer();
        Object key = en.nextElement();
        Object value = ((Dictionary)o).get(key);
        sb.append(key).append("=").append(value);
        for (; en.hasMoreElements(); ) {
          key = en.nextElement();
          value = ((Hashtable)o).get(key);
          sb.append(separator).append(key).append("=").append(value);
        }
        return sb.toString();
      }
      if (o instanceof Vector) {
        Vector v = (Vector)o;
        if (v.isEmpty()) return "";
        StringBuffer sb = new StringBuffer(String.valueOf(v.elementAt(0)));
        for (int k = 1, n = v.size(); k < n; )
          sb.append(separator).append(v.elementAt(k));
        return sb.toString();
      }
      return null;
    }
  }

  static class InternalCompatibilityToolJava11 extends InternalCompatibilityTool {
    boolean isInMethod(Throwable dummy, Class clazz, String methodName) throws CannotCheckStackTrace {
      try {
        // use reflection to allow compilation by javac 1.1+ and normally work under Java 1.4+
        Method getStackTraceMethod = dummy.getClass().getMethod("getStackTrace",new Class[0]);
        // check the method before the class: Microsoft Internet Explorer always prints an error message while checking non-existing class
        Class stackTraceElementClass = Class.forName("java.lang.StackTraceElement");
        Method getClassNameMethod = stackTraceElementClass.getMethod("getClassName",new Class[0]);
        Method getMethodNameMethod = stackTraceElementClass.getMethod("getMethodName",new Class[0]);
        Object stackTraceElements = getStackTraceMethod.invoke(dummy,new Object[0]);
        for (int k = 0, n = Array.getLength(stackTraceElements); k < n; k++) {
          Object element = Array.get(stackTraceElements,k);
          String elementClassName = (String)getClassNameMethod.invoke(element,new Object[0]);
          String elementMethodName = (String)getMethodNameMethod.invoke(element,new Object[0]);
          if (elementClassName.equals(clazz.getName()) && elementMethodName.equals(methodName)) {
            return true;
          }
        }
        return false;
      } catch (NoSuchMethodException e) {
        // JDK 1.3 or earlier: getStackTrace, getClassName or getMethodName not found
        return super.isInMethod(dummy,clazz,methodName);
      } catch (ClassNotFoundException e) {
        // JDK 1.3 or earlier: java.lang.StackTraceElement not found
        return super.isInMethod(dummy,clazz,methodName);
      } catch(NoClassDefFoundError e) {
        // JDK 1.3 or earlier: some required class not found
        return super.isInMethod(dummy,clazz,methodName);
      } catch (InvocationTargetException e) {
        e.printStackTrace();
        throw new InternalError("Unexpected InvocationTargetException exception in " + JVM.class.getName() + ".isInMethod(): " + e.getTargetException());
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        throw new InternalError("Unexpected IllegalAccessException exception in " + JVM.class.getName() + ".isInMethod(): " + e);
      }
    }

    String getStackTraceText(Throwable dummy) {
      StringWriter writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      dummy.printStackTrace(printWriter);
      printWriter.close(); // flush is performed also, according Writer.close specification
      return writer.toString();
    }

    String decOrExpPrivate(double v, int d, boolean exponentForm) {
      StringBuffer ptn = new StringBuffer("0");
      if (d > 0) {
        ptn.append('.');
        for (; d > 0; d--) ptn.append('0');
      }
      if (exponentForm) ptn.append("E0");
      java.text.DecimalFormat f = new java.text.DecimalFormat(ptn.toString(),DECIMAL_FORMAT_SYMBOLS_US);
      return f.format(v);
    }

    String spaced(long v, char space) {
      return DECIMAL_FORMAT_SPACED_US.format(v).replace(',',space);
    }

    String join(Object o, String separator) {
      if (o == null) return "";
      if (o.getClass().isArray()) {
        int len = Array.getLength(o);
        if (len == 0) return "";
        StringBuffer sb = new StringBuffer(String.valueOf(Array.get(o,0)));
        for (int k = 1; k < len; k++) {
          sb.append(separator).append(String.valueOf(Array.get(o,k)));
        }
        return sb.toString();
      }
      return super.join(o,separator);
    }

    private static final java.text.DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS_US = new java.text.DecimalFormatSymbols(java.util.Locale.US);
    private static final java.text.DecimalFormat DECIMAL_FORMAT_SPACED_US = new java.text.DecimalFormat("#,###",DECIMAL_FORMAT_SYMBOLS_US);
  }

  static class InternalCompatibilityToolJava12 extends InternalCompatibilityToolJava11 {
    String join(Object o, String separator) {
      if (o == null) return "";
      Class c = o.getClass();
      try {
        // use reflection to allow compilation by javac 1.1+ and normally work under Java 1.4+
        if (iteratorClass.isAssignableFrom(c)) {
          Method hasNextMethod = iteratorClass.getMethod("hasNext",new Class[0]);
          Method nextMethod = iteratorClass.getMethod("next",new Class[0]);
          if (!((Boolean)hasNextMethod.invoke(o,new Object[0])).booleanValue()) return "";
          StringBuffer sb = new StringBuffer(String.valueOf(nextMethod.invoke(o,new Object[0])));
          for (; ((Boolean)hasNextMethod.invoke(o,new Object[0])).booleanValue(); )
            sb.append(separator).append(nextMethod.invoke(o,new Object[0]));
          return sb.toString();
        }
        if (collectionClass.isAssignableFrom(c)) {
          Method iteratorMethod = collectionClass.getMethod("iterator",new Class[0]);
          return join(iteratorMethod.invoke(o,new Object[0]),separator);
        }
        if (mapClass.isAssignableFrom(c)) {
          Method entrySetMethod = mapClass.getMethod("entrySet",new Class[0]);
          return join(entrySetMethod.invoke(o,new Object[0]),separator);
        }
      } catch (NoSuchMethodException e) {
        // JDK 1.1 or earlier: Iterator.next/hasNext method not found
        return super.join(o,separator);
      } catch (InvocationTargetException e) {
        e.printStackTrace();
        throw new InternalError("Unexpected InvocationTargetException exception in Out.join: " + e.getTargetException());
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        throw new InternalError("Unexpected IllegalAccessException exception in Out.join: " + e);
      }
      return super.join(o,separator);
    }
  }

  private static InternalCompatibilityTool newInternalCompatibilityTool() {
    if (!JAVA_11_REFLECTION_SUPPORTED)
      return new InternalCompatibilityTool();
    try {
      Class.forName("java.io.StringWriter");
      Class.forName("java.io.PrintWriter");
    } catch (Throwable e) {
      return new InternalCompatibilityTool();
    }
    try {
      if (!JAVA_12_COLLECTIONS_SUPPORTED)
        return (InternalCompatibilityTool)Class.forName(InternalCompatibilityTool.class.getName()+"Java11").newInstance();
      else
        return (InternalCompatibilityTool)Class.forName(InternalCompatibilityTool.class.getName()+"Java12").newInstance();
    } catch (Throwable e) { // should not occur
      e.printStackTrace();
      throw new InternalError("Unexpected exception in " + JVM.class + " static initialization: " + e);
    }
  }

  private static Class iteratorClass = null;
  private static Class mapClass = null;
  private static Class collectionClass = null;
  static {
    boolean java11Serialization = false;
    boolean java11Reflection = false;
    boolean java12Comparable = false;
    boolean java12Collections = false;
    for (Class c = Integer.class; c != null; c = c.getSuperclass()) {
      Class[] interfaces = c.getInterfaces();
      for (int k = 0; k < interfaces.length; k++) {
        String name = interfaces[k].getName();
        if (name.equals("java.io.Serializable")) java11Serialization = true;
        else if (name.equals("java.lang.Comparable")) java12Comparable = true;
      }
    }
    for (Class c = Hashtable.class; c != null; c = c.getSuperclass()) {
      Class[] interfaces = c.getInterfaces();
      for (int k = 0; k < interfaces.length; k++) {
        String name = interfaces[k].getName();
        if (name.equals("java.util.Map")) {
          java12Collections = true;
          mapClass = interfaces[k];
          break;
        }
      }
    }
    if (java11Serialization) {
      try {
        Class.forName("java.lang.reflect.Method");
        java11Reflection = true;
      } catch (Throwable e) {
      }
    }
    if (java12Collections) {
      try {
        iteratorClass = Class.forName("java.util.Iterator");
        collectionClass = Class.forName("java.util.Collection");
        Class.forName("java.util.ArrayList");
        Class.forName("java.util.HashMap");
        Class.forName("java.util.TreeMap");
        Class.forName("java.util.HashSet");
        Class.forName("java.util.TreeSet");
        Class.forName("java.util.Collections");
      } catch (Throwable e) {
        java12Collections = false;
      }
    }
    JAVA_11_SERIALIZATION_SUPPORTED = java11Serialization;
    JAVA_11_REFLECTION_SUPPORTED = JAVA_11_SERIALIZATION_SUPPORTED && java11Reflection;
    JAVA_12_COMPARABLE_SUPPORTED = JAVA_11_REFLECTION_SUPPORTED && java12Comparable;
    JAVA_12_COLLECTIONS_SUPPORTED = JAVA_12_COMPARABLE_SUPPORTED && java12Collections;
  }

  static InternalCompatibilityTool INTERNAL_COMPATIBILITY_TOOL = newInternalCompatibilityTool();
    // must be initialized AFTER JAVA_1X_... constants

}