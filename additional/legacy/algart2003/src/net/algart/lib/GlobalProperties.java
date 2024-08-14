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

import java.io.*;
import java.util.*;

/**
 * <p>This class offers a global store for different string properties.
 * If some class <code>MyClass</code> needs a global flag or control parameter that
 * acts on the class behavior, we offer to use the following idiom:<pre>
 *    public static final MY_CONTROL_PARAMETER = GlobalProperties.getClassProperty(
 *      MyClass.class,
 *      "MY_CONTROL_PARAMETER",
 *      (default value));
 * </pre>(or analogous). Then the behavior of your class, controlled by
 * this parameter, will be guaranteed unchanged since the moment of the class
 * initialization. Simultaneously, you are able to initialize this
 * control parameter in your application or applet <i>before</i> the first access
 * to that class by {@link #setProperty(String, String) GlobalProperties.setProperty(String, String)}
 * method.
 *
 * <p><code>GlobalProperties</code> class <i>does not depend</i>, directly or
 * indirectly, on any other classes excepting standard Java API.
 *
 * <p>Unlike <code>System.getProperty</code> call, the methods of this class
 * never throw <code>SecurityException</code>. It makes <code>GlobalProperties</code>
 * class more suitable for creating service Java libraries.
 *
 * <p>By default, the global store supported by <code>GlobalProperties</code>
 * contains the following 3 properties:<ul>
 * <li><code>"line.separator"</code> (also stored in {@link #LINE_SEPARATOR} class constant),
 * <li><code>"file.separator"</code> (also stored in {@link #FILE_SEPARATOR} class constant),
 * <li><code>"path.separator"</code> (also stored in {@link #PATH_SEPARATOR} class constant).
 * </ul>These values are calculated while initializing <code>GlobalProperties</code>
 * on the base of standard Java Runtime libraries, without any risk to
 * meet with <code>SecurityException</code>.
 *
 * <p>For Java applications, initializing <code>GlobalProperties</code>
 * by standard system properties can be a good idea:<pre>
 *    GlobalProperties.{@link #setProperties(Properties) setProperties}(System.getProperties());
 * </pre>(In applets, <code>System.getProperties()</code> throws
 * <code>SecurityException</code> usually.)
 * <p>&nbsp;
 *
 * @author  Daniel Alievsky
 * @version 1.0
 * @since   JDK1.0
 */

public final class GlobalProperties {

  // Don't let anyone instantiate this class
  private GlobalProperties() {}

  /**
   * The standard sequence of characters that separates lines in text files and
   * console output. Depends on operating system: <code>"\n"</code> on Unix-like systems,
   * <code>"\r\n"</code> on DOS/Windows family, <code>"\r"</code> on Macintosh.
   *
   * <p>Standard <code>System.out.println()</code> call is equivalent to
   * <code>System.out.print(LINE_SEPARATOR)</code>.
   *
   * <p>If you want to print several lines on the console by a single call
   * of <code>System.out.println</code>, you <b>must always use
   * <code>LINE_SEPARATOR</code></b> to separate lines instead more simple
   * <code>"\n"</code>. For example, the call<pre>
   *
   *    System.out.println("First line\nSecond line");
   * </pre>
   * <b>is incorrect</b> (unlike an analogous call in C/C++): it will not properly
   * work under Macintosh. The following call should be called instead:<pre>
   *
   *    System.out.println("First line" + GlobalProperties.LINE_SEPARATOR + "Second line");
   * </pre>
   * You can also use the following more simple call that is also correct:<pre>
   *
   *    {@link Out#println(String) Out.println}("First line\nSecond line")
   * </pre>
   * <p><code>LINE_SEPARATOR</code> is also recommended to be used while
   * creating text files that should be then used by other software installed
   * on the same operating system.
   *
   * @see Out#print(String)
   * @see Out#println(String)
   */
  public static final String LINE_SEPARATOR = getLineSeparator();

  /**
   * Equivalent of the standard {@link java.io.File#separatorChar} constant:
   * the system-dependent default name-separator character. On UNIX systems
   * the value of this constant is <code>'/'</code>; on Microsoft Windows systems
   * it is <code>'\'</code>.
   */
  public static final char FILE_SEPARATOR = java.io.File.separatorChar;

  /**
   * Equivalent of the standard {@link java.io.File#pathSeparatorChar} constant:
   * the system-dependent path-separator character. This character is used to
   * separate filenames in a sequence of files given as a <em>path list</em>.
   * On UNIX systems, this character is <code>':'</code>; on Microsoft Windows systems
   * it is <code>';'</code>.
   */
  public static final char PATH_SEPARATOR = java.io.File.pathSeparatorChar;

  /**
   * The name of the global boolean property that is recommended to be used for
   * &quot;disabling native code&quot;, i.e. for informing classes, which may
   * need native libraries, that they should not try to call <code>loadLibrary</code>
   * or <code>load</code> methods of <code>System</code> and <code>Runtime</code>
   * standard classes.
   *
   * <p>More precisely, all classes in <code>net.algart.*</code> package, which use
   * native-code for optimization or other goals, never try to load external native
   * libraries if<pre>
   *    GlobalProperties.getBooleanProperty(
   *      GlobalProperties.JAVA_NATIVE_ENABLED_PROPERTY_NAME,
   *      true)
   * </pre>returns false. If other case, and if some exceptions occur
   * while attempt to call <code>System.load(...)</code>, these classes
   * don't use native methods and work using only methods implemented in Java.
   * If you implements  your own class, which need native code but can be
   * implemented without native also, we recommend you to follow the same logic.
   *
   * <p>In Java applications, you usually don't need to set this property.
   *
   * <p>In Java applets, we recommend to disable native libraries
   * by the call<pre>
   *    GlobalProperties.setBooleanProperty(JAVA_NATIVE_ENABLED_PROPERTY_NAME,false)
   * </pre><i>before</i> the first access to any other classes excepting
   * standard Java API classes. Without such a call, Microsoft Internet Explorer
   * with Microsoft JVM will print an error message while initializing
   * classes that try to load native library, despite catching
   * and ignoring all possible exceptions while such an attempt.
   */
  public static final String JAVA_NATIVE_ENABLED_PROPERTY_NAME = "java.native.enabled";

  /**
   * Returns all properties stored in the global store represented by
   * <code>GlobalProperties</code> class. The returned object is a
   * clone of the internal store. All keys and values in the returned
   * <code>java.util.Properties</code> object are instances of <code>String</code>
   * class.
   *
   * @return  all properties stored in the global store
   * @see     #getProperty(String)
   * @see     #getProperty(String, String)
   */
  public static Properties getProperties() {
    return (Properties)globalPropertiesPrivate.clone();
  }

  /**
   * Returns the global property with the given name <code>key</code>.
   *
   * <p>Returns <code>null</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the property with the given name
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   * @see     #getBooleanProperty(String)
   * @see     #getIntProperty(String)
   * @see     #getLongProperty(String)
   * @see     #getClassProperty(Class, String)
   * @see     #setProperty(String, String)
   */
  public static String getProperty(String key) {
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getProperty method");
    return (String)globalPropertiesPrivate.get(key);
  }

  /**
   * Returns the global property with the given name <code>key</code>,
   * or <code>defaultValue</code> if there is no such a property.
   *
   * @param key           the property name
   * @param defaultValue  the default value for this property
   * @return  the property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String)
   * @see     #getBooleanProperty(String, boolean)
   * @see     #getIntProperty(String, int)
   * @see     #getLongProperty(String, long)
   * @see     #getClassProperty(Class, String, String)
   * @see     #setProperty(String, String)
   */
  public static String getProperty(String key, String defaultValue) {
    String result = getProperty(key);
    if (result == null) return defaultValue;
    return result;
  }

  /*Repeat()
    boolean property ==> integer property,, long integer property;;
    getBoolean ==> getInt,, getLong;;
    boolean ==> int,, long;;
    Boolean ==> Integer,, Long
  */
  /**
   * Returns the global boolean property with the given name <code>key</code>.
   * It is calculated as a result of <code>Boolean.valueOf(value)</code> call,
   * where <code>value</code> is a result of {@link #getProperty(String)
   * getProperty(key)} call.
   *
   * <p>Returns <code>null</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the <code>Boolean</code> wrapper for an existing boolean property,
   *          <code>null</code> for non-existing property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   */
  public static Boolean getBooleanProperty(String key) {
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getBooleanProperty method");
    String result = getProperty(key);
    if (result == null) return null;
    try {
      return Boolean.valueOf(result);
    } catch (NumberFormatException e) { // for int and long
      return null;
    }
  }

  /**
   * Returns the global boolean property with the given name <code>key</code>.
   * It is calculated as a result of <code>Boolean.valueOf(value).booleanValue()</code> call,
   * where <code>value</code> is a result of {@link #getProperty(String)
   * getProperty(key)} call.
   *
   * <p>Returns <code>defaultValue</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the boolean property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   */
  public static boolean getBooleanProperty(String key, boolean defaultValue) {
    Boolean result = getBooleanProperty(key);
    if (result == null) return defaultValue;
    return result.booleanValue();
  }/*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
  /**
   * Returns the global integer property with the given name <code>key</code>.
   * It is calculated as a result of <code>Integer.valueOf(value)</code> call,
   * where <code>value</code> is a result of {@link #getProperty(String)
   * getProperty(key)} call.
   *
   * <p>Returns <code>null</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the <code>Integer</code> wrapper for an existing integer property,
   *          <code>null</code> for non-existing property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   */
  public static Integer getIntProperty(String key) {
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getIntProperty method");
    String result = getProperty(key);
    if (result == null) return null;
    try {
      return Integer.valueOf(result);
    } catch (NumberFormatException e) { // for int and long
      return null;
    }
  }

  /**
   * Returns the global integer property with the given name <code>key</code>.
   * It is calculated as a result of <code>Integer.valueOf(value).intValue()</code> call,
   * where <code>value</code> is a result of {@link #getProperty(String)
   * getProperty(key)} call.
   *
   * <p>Returns <code>defaultValue</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the integer property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   */
  public static int getIntProperty(String key, int defaultValue) {
    Integer result = getIntProperty(key);
    if (result == null) return defaultValue;
    return result.intValue();
  }
  /**
   * Returns the global long integer property with the given name <code>key</code>.
   * It is calculated as a result of <code>Long.valueOf(value)</code> call,
   * where <code>value</code> is a result of {@link #getProperty(String)
   * getProperty(key)} call.
   *
   * <p>Returns <code>null</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the <code>Long</code> wrapper for an existing long integer property,
   *          <code>null</code> for non-existing property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   */
  public static Long getLongProperty(String key) {
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getLongProperty method");
    String result = getProperty(key);
    if (result == null) return null;
    try {
      return Long.valueOf(result);
    } catch (NumberFormatException e) { // for int and long
      return null;
    }
  }

  /**
   * Returns the global long integer property with the given name <code>key</code>.
   * It is calculated as a result of <code>Long.valueOf(value).longValue()</code> call,
   * where <code>value</code> is a result of {@link #getProperty(String)
   * getProperty(key)} call.
   *
   * <p>Returns <code>defaultValue</code> if there is no property with the given name.
   *
   * @param key the property name
   * @return  the long integer property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   */
  public static long getLongProperty(String key, long defaultValue) {
    Long result = getLongProperty(key);
    if (result == null) return defaultValue;
    return result.longValue();
  }/*Repeat.AutoGeneratedEnd*/

  /**
   * Returns the name for a property oriented to the given class. Such property
   * names are used by the methods <code>GlobalProperties.getClassProperty</code>
   * and <code>GlobalProperties.getClassXxxProperty</code>.
   *
   * @param clazz the given class
   * @param key   the property name inside the class
   * @return <code>clazz.getName() + "." + key</code>
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   */
  public static String getClassPropertyName(Class clazz, String key) {
    if (clazz == null) throw new NullPointerException("Null clazz argument in " + GlobalProperties.class.getName() + ".getClassPropertyName method");
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getClassPropertyName method");
    return clazz.getName() + "." + key;
  }

  /**
   * Equivalent to <code>{@link #getProperty(String) getProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key))</code> call.
   *
   * @param clazz the class the property corresponds to which
   * @param key   the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @return  the property with the given name, or <code>null</code> for it doesn't exist
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String)
   * @see     #getClassBooleanProperty(Class, String)
   * @see     #getClassIntProperty(Class, String)
   * @see     #getClassLongProperty(Class, String)
   * @see     #getClassProperty(Class, String, String)
   */
  public static String getClassProperty(Class clazz, String key) {
    if (clazz == null) throw new NullPointerException("Null clazz argument in " + GlobalProperties.class.getName() + ".getClassProperty method");
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getClassProperty method");
    return getProperty(getClassPropertyName(clazz,key));
  }

  /**
   * Equivalent to <code>{@link #getProperty(String, String) getProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key),defaultValue)</code> call.
   *
   * @param clazz         the class the property corresponds to which
   * @param key           the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @param defaultValue  the default value for this property
   * @return  the property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getProperty(String, String)
   * @see     #getClassBooleanProperty(Class, String, boolean)
   * @see     #getClassIntProperty(Class, String, int)
   * @see     #getClassLongProperty(Class, String, long)
   * @see     #getClassProperty(Class, String)
   */
  public static String getClassProperty(Class clazz, String key, String defaultValue) {
    if (clazz == null) throw new NullPointerException("Null clazz argument in " + GlobalProperties.class.getName() + ".getClassProperty method");
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".getClassProperty method");
    return getProperty(getClassPropertyName(clazz,key),defaultValue);
  }

  /*Repeat()
    boolean property ==> integer property,, long integer property;;
    get(Class)?Boolean ==> get$1Int,, get$1Long;;
    boolean ==> int,, long;;
    Boolean ==> Integer,, Long
  */
  /**
   * Equivalent to <code>{@link #getBooleanProperty(String) getBooleanProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key))</code> call.
   *
   * @param clazz the class the property corresponds to which
   * @param key   the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @return  the <code>Boolean</code> wrapper for an existing boolean property,
   *          <code>null</code> for non-existing property
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getBooleanProperty(String)
   * @see     #getClassBooleanProperty(Class, String, boolean)
   */
  public static Boolean getClassBooleanProperty(Class clazz, String key) {
    return getBooleanProperty(getClassPropertyName(clazz,key));
  }

  /**
   * Equivalent to <code>{@link #getBooleanProperty(String, boolean) getBooleanProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key),defaultValue)</code> call.
   *
   * @param clazz         the class the property corresponds to which
   * @param key           the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @param defaultValue  the default value for this property
   * @return  the property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getBooleanProperty(String, boolean)
   * @see     #getClassBooleanProperty(Class, String)
   */
  public static boolean getClassBooleanProperty(Class clazz, String key, boolean defaultValue) {
    return getBooleanProperty(getClassPropertyName(clazz,key),defaultValue);
  }/*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
  /**
   * Equivalent to <code>{@link #getIntProperty(String) getIntProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key))</code> call.
   *
   * @param clazz the class the property corresponds to which
   * @param key   the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @return  the <code>Integer</code> wrapper for an existing integer property,
   *          <code>null</code> for non-existing property
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getIntProperty(String)
   * @see     #getClassIntProperty(Class, String, int)
   */
  public static Integer getClassIntProperty(Class clazz, String key) {
    return getIntProperty(getClassPropertyName(clazz,key));
  }

  /**
   * Equivalent to <code>{@link #getIntProperty(String, int) getIntProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key),defaultValue)</code> call.
   *
   * @param clazz         the class the property corresponds to which
   * @param key           the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @param defaultValue  the default value for this property
   * @return  the property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getIntProperty(String, int)
   * @see     #getClassIntProperty(Class, String)
   */
  public static int getClassIntProperty(Class clazz, String key, int defaultValue) {
    return getIntProperty(getClassPropertyName(clazz,key),defaultValue);
  }
  /**
   * Equivalent to <code>{@link #getLongProperty(String) getLongProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key))</code> call.
   *
   * @param clazz the class the property corresponds to which
   * @param key   the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @return  the <code>Long</code> wrapper for an existing long integer property,
   *          <code>null</code> for non-existing property
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getLongProperty(String)
   * @see     #getClassLongProperty(Class, String, long)
   */
  public static Long getClassLongProperty(Class clazz, String key) {
    return getLongProperty(getClassPropertyName(clazz,key));
  }

  /**
   * Equivalent to <code>{@link #getLongProperty(String, long) getLongProperty}({@link
   * #getClassPropertyName(Class, String) getClassPropertyName}(clazz,key),defaultValue)</code> call.
   *
   * @param clazz         the class the property corresponds to which
   * @param key           the property name inside the class (full property name will be <code>clazz.getName() + "." + key</code>)
   * @param defaultValue  the default value for this property
   * @return  the property with the given name, or <code>defaultValue</code> for it doesn't exist
   * @throws  NullPointerException if <code>clazz</code> or <code>key</code> argument is <code>null</code>
   * @see     #getLongProperty(String, long)
   * @see     #getClassLongProperty(Class, String)
   */
  public static long getClassLongProperty(Class clazz, String key, long defaultValue) {
    return getLongProperty(getClassPropertyName(clazz,key),defaultValue);
  }/*Repeat.AutoGeneratedEnd*/

  /**
   * Equivalent to <code>{@link #getClassIntProperty(Class, String, int)
   * getClassIntProperty}(clazz, "DEBUG_LEVEL", 0)</code>.
   *
   * <p>We recommend to use such properties to control whether
   * the class should print additional debug information or no.
   * For example:<pre>
   *    public class MyClass {
   *        public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(MyClass.class);
   *        . . .
   *    }</pre>
   *
   * @param clazz the class the property corresponds to which
   * @return      the integer property with the name <code>clazz.getName() + ".DEBUG_LEVEL"</code>
   *              or 0 if there is no such a property
   * @throws  NullPointerException if <code>clazz</code> argument is <code>null</code>
   * @see     #getClassIntProperty(Class, String, int)
   */
  public static int getClassDebugLevel(Class clazz) {
    return getClassIntProperty(clazz, "DEBUG_LEVEL", 0);
  }


  /**
   * Sets the global string property with the given name <code>key</code>.
   *
   * @param key   the property name
   * @param value the new value for this property, must not be <code>null</code>
   * @return      the previous value of the specified property
   *              or <code>null</code> if there was no such a property
   * @throws  NullPointerException if <code>key</code> or <code>value</code> argument is <code>null</code>
   * @throws  IllegalAccessError if {@link #fix()} method has been called before
   * @see     #getProperty(String)
   * @see     #setBooleanProperty(String, boolean)
   * @see     #setIntProperty(String, int)
   * @see     #setLongProperty(String, long)
   * @see     #setProperties(Properties)
   * @see     #removeProperty(String)
   * @see     #fix()
   */
  public static String setProperty(String key, String value) {
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".setProperty method");
    if (value == null) throw new NullPointerException("Null value argument in " + GlobalProperties.class.getName() + ".setProperty method");
    checkFix();
    return (String)globalPropertiesPrivate.put(key,value);
  }

  /*Repeat()
    boolean ==> int,, long;;
    setBoolean ==> setInt,, setLong
  */
  /**
   * Equivalent to {@link #setProperty(String, String)
   * setProperty(key, String.valueOf(value))} call.
   *
   * @param key   the property name
   * @param value the new value for this property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #setProperty(String, String)
   */
  public static void setBooleanProperty(String key, boolean value) {
    setProperty(key,String.valueOf(value));
  }/*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
  /**
   * Equivalent to {@link #setProperty(String, String)
   * setProperty(key, String.valueOf(value))} call.
   *
   * @param key   the property name
   * @param value the new value for this property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #setProperty(String, String)
   */
  public static void setIntProperty(String key, int value) {
    setProperty(key,String.valueOf(value));
  }
  /**
   * Equivalent to {@link #setProperty(String, String)
   * setProperty(key, String.valueOf(value))} call.
   *
   * @param key   the property name
   * @param value the new value for this property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @see     #setProperty(String, String)
   */
  public static void setLongProperty(String key, long value) {
    setProperty(key,String.valueOf(value));
  }/*Repeat.AutoGeneratedEnd*/


  /**
   * Removes the global property with the given name <code>key</code>.
   *
   * @param key   the property name
   * @return      the previous value of the specified property
   *              or <code>null</code> if there was no such a property
   * @throws  NullPointerException if <code>key</code> argument is <code>null</code>
   * @throws  IllegalAccessError if {@link #fix()} method has been called before
   * @see     #getProperty(String)
   * @see     #setProperty(String, String)
   * @see     #setProperties(Properties)
   * @see     #removeProperty(String)
   * @see     #fix()
   */
  public static String removeProperty(String key) {
    if (key == null) throw new NullPointerException("Null key argument in " + GlobalProperties.class.getName() + ".removeProperty method");
    checkFix();
    return (String)globalPropertiesPrivate.remove(key);
  }

  /**
   * Adds a collection of string properties to the global store. Equivalent to a sequence of
   * {@link #setProperty(String, String) setProperty(key,value}} calls for
   * all (<code>key</code>, <code>value</code>) pairs contained in
   * <code>properties</code> argument, excepting all pairs where
   * <code>key</code> or <code>value</code> is not a non-null instance of the
   * <code>String</code> class.
   *
   * <p>A good usage example for Java applications:<pre>
   *    GlobalProperties.setProperties(System.getProperties());
   * </pre>Please check that such a call is placed in the very beginning
   * of your application, before any possible usage of these properties.
   * In particular, place this call before any access to classes
   * that probably use these global properties for initializing
   * there statis class variables, such as DEBUG_LEVEL (see
   * {@link #getClassDebugLevel(Class)}).
   *
   * @param properties  the collection of string properties that will be added to the current global store
   * @throws  NullPointerException if <code>properties</code> argument is <code>null</code>
   * @throws  IllegalAccessError if {@link #fix()} method has been called before
   * @see     #getProperty(String)
   * @see     #setProperty(String, String)
   * @see     #removeProperty(String)
   * @see     #fix()
   */
  public static void setProperties(Properties properties) {
    if (properties == null) throw new NullPointerException("Null properties argument in " + GlobalProperties.class.getName() + ".setProperties method");
    checkFix();
    for (Enumeration en = properties.keys(); en.hasMoreElements(); ) {
      Object key = en.nextElement();
      if (!(key instanceof String)) continue;
      Object value = properties.get(key);
      if (!(value instanceof String)) continue;
      globalPropertiesPrivate.put(key,value);
    }
  }

  /**
   * Fixes the global store: any modifications become impossible after the first call of this method.
   * An attemt to call {@link #setProperty(String, String)}, {@link #removeProperty(String)},
   * {@link #setProperties(Properties)} will lead to <code>IllegalAccessError</code>.
   *
   * <p>The call of <code>fix()</code> method never throws exceptions. The second and further
   * its calls do nothing.
   */
  public static void fix() {
    fixed = true;
  }

  private static void checkFix() {
    // IllegalAccessError is chosen for compatibility with Java 1.0
    if (fixed) throw new IllegalAccessError("Cannot change global properties after calling fix() method");
  }
  private static String getLineSeparator() {
    // Don't use BufferWriter to provide compatibility with Java 1.0
    // All known OS use characters <256 for line separators
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8);
    PrintStream printStream = new PrintStream(byteArrayOutputStream);
    printStream.println();
    printStream.flush();
    printStream.close();
    byte[] bytes = byteArrayOutputStream.toByteArray();
    char[] chars = new char[bytes.length];
    for (int k = 0; k < bytes.length; k++) chars[k] = (char)(bytes[k]&0xFF);
    // such a code provides compatibility with Java 1.0 and avoids deprecation warnings
    return String.valueOf(chars);
  }

  private static boolean isTrueStaticFlag;
  // TrueStatic is necessary because these properties should be really global.
  // Implementing TrueStatic is impossible due to the contract that this class doesn't depend on other ones.

  private static boolean fixed = false;
  private static final Properties globalPropertiesPrivate = new Properties();
  static {
    globalPropertiesPrivate.put("line.separator", LINE_SEPARATOR);
    globalPropertiesPrivate.put("file.separator", java.io.File.separator);
    globalPropertiesPrivate.put("path.separator", java.io.File.pathSeparator);
  }
}