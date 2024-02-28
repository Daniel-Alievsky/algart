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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * <p>Title:     <b>Algart RTTI</b><br>
 * Description:  Dynamic Class Overloading system and miscellaneous RTTI tools<br>
 * @author       Daniel Alievsky
 * @version      1.1
 * @deprecated
 */

public class ARTTI {
    // for compatibility
    private static class Logger {
        public void debug(String s) {
            System.out.println(s);
        }
        public void debug(String s, Object o) {
            System.out.println(s + ": " + o);
        }
    }
  private static final Logger LOG = new Logger(); //Logger.getLogger(ARTTI.class);

  public static int debugLevel= 0;

  /* ********** */
  /* DCO system */


  private static boolean invalidateWasCalled= false;
  private static final HashMap hash= new HashMap(); //loaderId -> ClassLoader
  private static final HashMap hashClassesId= new HashMap(); //loaderId -> ClassesId

  private static String tempClassesDirectory= null;
  public static final String CLASSES_TEMPORARY_SUBDIRECTORY_PREFIX= "tempjavaclasses";

  /**
   * All DCO class loaders automatically adds special "temp classes directory"
   * to their CLASSPATH.
   * setTempClassesDirectory() sets this path. It can be null (default value);
   * then the result of
   *   AFiles.createTempDirectory(CLASSES_TEMPORARY_SUBDIRECTORY_PREFIX,"",AFiles.tempDirectory()).getPath()
   * is assumed
   * This directory is automatically deleted on the program shutdown
   * So, it should be unique for the current Virtual Machine, to
   * allow user to call several instances of the application
   */
  public static synchronized void setTempClassesDirectory(String tempClassesDirectory) {
    ARTTI.tempClassesDirectory= tempClassesDirectory;
    getTempClassesDirectory();
  }
  /**
   * getTempClassesDirectory() returns the value that has been set by
   * setTempClassesDirectory() or the default assumed value
   * if the argument of setTempClassesDirectory() was null
   */
  public static synchronized String getTempClassesDirectory() {
    if (tempClassesDirectory==null)
      tempClassesDirectory= AFiles.createTempDirectory(
        CLASSES_TEMPORARY_SUBDIRECTORY_PREFIX,"",AFiles.tempDirectory()).getPath();
    AFiles.deleteOnShutdown(tempClassesDirectory);
    return tempClassesDirectory;
  }


  /**
   * getSummaryClassesId() allows to check whether class loading system has been invalidated
   * It returns 0 after first loading this class; it's result increments after
   * each call <code>invalidateClasses()</code> or <code>invalidateClasses(loaderId)</code>
   */
  public static synchronized long getSummaryClassesId() {
    long sum= 0;
    for (Iterator iterator= hashClassesId.values().iterator(); iterator.hasNext(); )
      sum+= ((ClassesId)iterator.next()).value;
    return sum;
  }
  /**
   * getClassesId() is equivalent to getClassesId(null) (default
   * class loader)
   */
  public static synchronized long getClassesId() {
    return getClassesId(null);
  }
  /**
   * getClassesId(loaderId) allows to check whether DCO class loader with given
   * identifier has been invalidated.
   * It returns 0 if this class loader has never used
   */
  public static synchronized long getClassesId(Object loaderId) {
    ClassesId classesId= (ClassesId)hashClassesId.get(loaderId);
    if (classesId==null) return invalidateWasCalled?1:0;
    return classesId.value;
  }

  /**
   * getAllClassesId() returns an unmodifiable map containing
   * <code>getClassesId(...)</code> values for all class loaders
   * known to DCO system. This map contains instances of ClassesId
   * class, that contains only 1 field: "long value"
   */
  public static synchronized Map getAllClassesId() {
    return Collections.unmodifiableMap(hashClassesId);
  }
  public static class ClassesId {
    long value;
    public ClassesId() {this.value= 100;}
    public ClassesId(long value) {this.value= value;}
    public String toString() {return String.valueOf(value);}
  }

  /**
   * Invalidates all classes loaded by DCO system
   * All class files will be reloaded by the next <code>loadClass()</code>
   */
  public static synchronized void invalidateClasses() {
    if (!hash.isEmpty()) {
      for (Iterator iterator= hash.keySet().iterator(); iterator.hasNext(); )
        ((ClassesId)hashClassesId.get(iterator.next())).value++;
      hash.clear();
      LOG.debug("All dynamically loaded classes have been invalidated");
    } else if (!invalidateWasCalled) {
      LOG.debug("Dynamic Overloading Classes system initialized: dynamic classes will be reloaded");
    }
    invalidateWasCalled= true;
  }
  /**
   * Invalidates all classes loaded by the given DCO class loader
   * All these class files will be reloaded by the next <code>loadClass()</code>
   * @param loaderId  Identifier of class loader that should be invalidated.
   *    Can be null; it is <em>not</em> equivalent to <code>invalidateClasses()</code>
   */
  public static synchronized void invalidateClasses(Object loaderId) {
    if (hash.containsKey(loaderId)) {
      LOG.debug("["+ATools.toS(loaderId)+"] Dynamically loaded classes have been invalidated");
      ((ClassesId)hashClassesId.get(loaderId)).value++;
      hash.remove(loaderId);
    }
  }

  /**
   * If true, then <code>loadClass(name)</code> (without <code>loaderId</code>)
   * will always use system class loader (<code>Class.forName()</code>)
   * until first call of <code>invalidateClasses()</code>
   */
  public static boolean useSystemLoaderAtFirst= true;

  /**
   * Equivalent to loadClass(name,null)
   * @throws ClassNotFoundException
   */
  public static synchronized Class loadClass(String name) throws ClassNotFoundException {
    return loadClass(name,null);
  }
  /**
   * Loads a .class file by DCO system
   * @param name      Full class name (with package)
   * @param loaderId  Identifier of class loader that should be used
   * @throws ClassNotFoundException
   */
  public static synchronized Class loadClass(String name, Object loaderId)
  throws ClassNotFoundException
  {
    return loadClass(name,false,loaderId);
  }
  /**
   * Loads a .class file by DCO system; allows to specify whether static
   * initializing is required.
   * @param name      Full class name (with package)
   * @param initialize whether the class must be initialized
   * @param loaderId  Identifier of class loader that should be used
   * @throws ClassNotFoundException
   */
  public static synchronized Class loadClass(String name, boolean initialize, Object loaderId)
  throws ClassNotFoundException
  {
    ClassLoader classLoader= getClassLoader(loaderId);
    if (classLoader==null) {
      if (debugLevel>=3) LOG.debug("["+ATools.toS(loaderId)+"] Standard loading (Class.forName()) class "+name);
      if (initialize) return Class.forName(name);
      return Class.forName(name,false,ARTTI.class.getClassLoader());
    } else {
      return Class.forName(name,initialize,classLoader);
    }
  }

  /**
   * Equivalent to getClassLoader(loaderId)
   */
  public static synchronized ClassLoader getClassLoader() {
    return getClassLoader(null);
  }

  /**
   * Returns DCO class loader used by loadClass(...,loaderId)
   */
  public static synchronized ClassLoader getClassLoader(Object loaderId) {
    ClassLoader result= null;
    if (!(useSystemLoaderAtFirst && !invalidateWasCalled && loaderId==null)) {
      result= (ClassLoader)hash.get(loaderId);
      if (result==null) {
        result= createClassLoader(loaderId);
        if (result!=null) {
          if (debugLevel>=1)
            LOG.debug("["+ATools.toS(loaderId)
              +"] Creating new class loader with for "+loaderId+" loaderId: "
              +(result instanceof ARTTI.DynamicClassOverloader?
                "it is DCO, classPath="+ATools.toS(((ARTTI.DynamicClassOverloader)result).classPath,";")
                +" ignored="+ATools.toS(((ARTTI.DynamicClassOverloader)result).ignored,";"):
                result.getClass().getName()));
          hash.put(loaderId,result);
          if (!hashClassesId.containsKey(loaderId))
            hashClassesId.put(loaderId,new ClassesId());
        }
      }
    }
    return result;
  }

  private static String[] defaultDcoClassPath;
  private static String[] defaultDcoIgnored;
  /**
   * The following settings will be used for creating Dynamic Class Overloader
   * with no class loader producer is specified
   */
  public static void setDefaultDcoSettings(String[] classPath, String[] ignored) {
    defaultDcoClassPath= classPath;
    defaultDcoIgnored= ignored;
  }
  /**
   * @return Default DCO CLASSPATH assumed by <code>setDefaultDcoSettings()</code>
   */
  public static String[] getDefaultDcoClassPath() {
    return defaultDcoClassPath;
  }
  /**
   * @return Default DCO ignored assumed by <code>setDefaultDcoSettings()</code>
   */
  public static String[] getDefaultDcoIgnored() {
    return defaultDcoIgnored;
  }
  /**
   * @param loaderId Identifier of Dynamic Class Overloader
   * @return Class loader, produced by current class loader producer
   * assigned by <code>setClassLoaderProducer()</code>, or Dynamic
   * Class Overloader created with default settings in a case
   * when not class loader procuder has been assigned
   */
  public static ClassLoader createClassLoader(Object loaderId) {
    if (classLoaderProducer==null) {
      return new ARTTI.DynamicClassOverloader(
        defaultDcoClassPath,
        defaultDcoIgnored);
    } else {
      try {
        return classLoaderProducer.createClassLoader(loaderId);
      } catch (Exception e) {
        return null;
      }
    }
  }

  /**
   * isDCO() returns true if the given class loader is DCO
   */
  public static boolean isDCO(ClassLoader classLoader) {
    return classLoader instanceof DynamicClassOverloader;
  }
  /**
   * getCurrentDCO() returns the nearest DCO class loader
   * in the current stack trace, or null if this class
   * and all classes used in the stack trace are not loaded
   * by DCO system
   */
  public static DynamicClassOverloader getCurrentDCO() {
    Class[] stackTraceClasses= ATools.getCurrentStackTraceClasses();
    for (int k=0; k<stackTraceClasses.length; k++) {
      ClassLoader classLoader= stackTraceClasses[k].getClassLoader();
      if (isDCO(classLoader)) return (DynamicClassOverloader)classLoader;
    }
    return null;
  }

  /**
   * getAdditionalClassPath() returns CLASSPATH of the given class loader,
   * if it is DCO, or "new String[0]" in other cases
   */
  public static String[] getAdditionalClassPath(ClassLoader classLoader) {
    if (!isDCO(classLoader)) return new String[0];
    return ((DynamicClassOverloader)classLoader).classPath;
  }

  /**
   * getFileSystemJavaClassPathAsStrings() returns a part of the default
   * Java CLASSPATH that corresponds to usual subdirectories, not ZIP or JAR
   */
  private static String[] fileSystemJavaClassPathAsStrings;
  public static String[] getFileSystemJavaClassPathAsStrings() {
    if (fileSystemJavaClassPathAsStrings==null) {
      fileSystemJavaClassPathAsStrings= (String[])AArrays.choose(
        ATools.getJavaClassPathAsStrings(),
        new AArrays.Matcher() {
          public boolean matches(Object o) {
            return new File((String)o).isDirectory();
          }
        },new String[0]);
    }
    return fileSystemJavaClassPathAsStrings;
  }
  /**
   * getFileSystemJavaClassPathAsFiles() is analog of
   * getFileSystemJavaClassPathAsStrings()
   */
  public static File[] getFileSystemJavaClassPathAsFiles() {
    String[] classPath= getFileSystemJavaClassPathAsStrings();
    File[] result= new File[classPath.length];
    for (int k=0; k<result.length; k++) result[k]= new File(classPath[k]);
    return result;
  }

  /**
   * trueStaticData can be used for saving some true static data,
   * that can be shared between several classes loaded by Dynamic
   * Class Overloader. (Usual static fieds of overladed classes
   * are not shared between instances of these classes created
   * after overloading the class.)
   */
  public static Map trueStaticData= new HashMap();

  /**
   * ClassLoaderProcuder interface should produce some ClassLoader for
   * given ClassLoader identifier.
   *
   * <p>Title: Algart Java</p>
   * <p>Description: </p>
   * <p>Copyright: Copyright (c) 2001</p>
   * <p>Company: SIAMS</p>
   * @author Daniel Alievsky
   * @version 1.0
   */
  public interface ClassLoaderProducer {
    /**
     * @param loaderId  Identifier of some Class Loader; may be null
     * @return          Some ClassLoader; will be used by ARTTI.loadClass
     *                  for this identifier
     */
    public ClassLoader createClassLoader(Object loaderId) throws Exception;
  }

  /**
   * Current class loader producer used by <code>loadClass()</code>
   */
  public static ARTTI.ClassLoaderProducer classLoaderProducer;

  public static class DynamicClassOverloader extends ClassLoader implements net.algart.lib.Reflection.WithClassPath {
    public Set classPath() {
      return Collections.unmodifiableSet(new LinkedHashSet(Arrays.asList(classPath)));
    }
    private Map classesHash= new HashMap();
    public static final String THIS_CLASS_IS_ALWAYS_IGNORED= ARTTI.class.getName();
    // We should never dynamically load this class ARTTI:
    // in other case, invalidateClasses() will not work from dynamic classes
    public static final String ALWAYS_IGNORED= "truestatic";
    // If the full name of a class, converted to lower case, contains
    // ALWAYS_IGNORED substring, this class is never dynamically loaded

    public final String[] classPath;
    /**
     * Example of <code>ignored</code>: {
     * "algart.lib.**" - means algart.lib package and all its subpackages
     * "algart.lib.*" - means algart.lib package only
     * "algart.lib.A*" - means all classes in algart.lib package started with A
     * "algart.lib.legacy.ATools" - means algart.lib.legacy.ATools class only
     */

    public final String[] ignored;
    public final Object loaderId; // can be used for debugging
    public final ClassLoader parentClassLoader;

    public DynamicClassOverloader(String[] classPath) {
      this(classPath,null,null,null);
    }
    public DynamicClassOverloader(String[] classPath, String[] ignored) {
      this(classPath,ignored,null,null);
    }
    public DynamicClassOverloader(String[] classPath, Object loaderId) {
      this(classPath,null,loaderId,null);
    }
    public DynamicClassOverloader(String[] classPath, String[] ignored, Object loaderId) {
      this(classPath,ignored,loaderId,null);
    }
    public DynamicClassOverloader(String[] classPath, ClassLoader parentClassLoader) {
      this(classPath,null,null,parentClassLoader);
    }
    public DynamicClassOverloader(String[] classPath, String[] ignored, ClassLoader parentClassLoader) {
      this(classPath,ignored,null,parentClassLoader);
    }
    public DynamicClassOverloader(String[] classPath, Object loaderId, ClassLoader parentClassLoader) {
      this(classPath,null,loaderId,parentClassLoader);
    }
    public DynamicClassOverloader(String[] classPath, String[] ignored, Object loaderId, ClassLoader parentClassLoader) {
      this.classPath= (String[])AArrays.append(classPath,new String[] {getTempClassesDirectory()});
      this.ignored= (String[])AArrays.copy(ignored);
      this.loaderId= loaderId;
      this.parentClassLoader= parentClassLoader;
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class result= findClass(name);
      if (resolve) resolveClass(result);
      return result;
    }
    protected Class findClass(String name) throws ClassNotFoundException {
      Class result= (Class)classesHash.get(name);
      if (result!=null) {
        if (debugLevel>=3) LOG.debug("["+ ATools.toS(loaderId)+"] Class "+name+" found in cache");
        return result;
      }
      try {
        boolean useFileLoader= !shouldBeIgnored(name);
        File f= null;
        if (useFileLoader) {
          f= findFile(name.replace('.','/'),".class");
          if (f==null) useFileLoader= false;
        }
        if (useFileLoader) {
          try {
            result= loadClassByParentClassLoader(name);
          } catch (ClassNotFoundException e) {
            result= null;
          }
          boolean isTrueStatic= false;
          if (result!=null) {
            if (ARTTI.hasDeclaredField(result,"isTrueStaticFlag")) {
              isTrueStatic= true;
            } else {
              for (Class clazz= result; clazz!=null; clazz=clazz.getDeclaringClass()) {
                if (net.algart.lib.Reflection.isTrueStaticImplemented(clazz)) {
                  isTrueStatic= true; break;
                }
              }
            }
          }
          if (!isTrueStatic) {
            if (debugLevel>=1) LOG.debug("["+ATools.toS(loaderId)+"] Loading class "+name+(debugLevel>=2?" from "+f.getPath():""));
            byte[] classBytes= AFiles.loadFileAsBytes(f);
            result= defineClass(name,classBytes,0,classBytes.length);
          } else {
            if (debugLevel>=1) LOG.debug("["+ATools.toS(loaderId)+"] System loading true-static class "+name);
          }
        } else {
          if (debugLevel>=3) LOG.debug("["+ATools.toS(loaderId)+"] Parent ("+(parentClassLoader==null?"system":parentClassLoader+"")+") loading class "+name);
          result= loadClassByParentClassLoader(name);
        }
      } catch (IOException e) {
        throw new ClassNotFoundException("Cannot load class ["+(loaderId==null?"":loaderId)+"]: "+e);
      } catch (ClassFormatError e) {
        throw new ClassNotFoundException("Format of class file incorrect for class "+name+" ["+(loaderId==null?"":loaderId)+"]: "+e);
      }
      classesHash.put(name,result);
      return result;
    }

    private Class loadClassByParentClassLoader(String name) throws ClassNotFoundException {
      if (parentClassLoader==null) return findSystemClass(name);
      return parentClassLoader.loadClass(name);
    }

    public java.net.URL getResource(String name) {
      if (debugLevel>=1) LOG.debug("["+ATools.toS(loaderId)+"] Getting resource "+name);
      java.net.URL url= super.getResource(name);
      if (debugLevel>=1) LOG.debug(url==null?"":": found at "+url);
      return url;
    }
    protected java.net.URL findResource(String name) {
      File f= findFile(name);
      if (debugLevel>=1) LOG.debug("(DCO findResource())"+(debugLevel>=2?" in "+ATools.toS(classPath,File.pathSeparator):""));
      if (f==null) {
        if (debugLevel>=1) LOG.debug(": NOT FOUND");
        return null;
      }
      try {
        return f.toURI().toURL();
      } catch(java.net.MalformedURLException e) {
        if (debugLevel>=1) LOG.debug(": CANNOT BE FOUND", e);
        return null;
      }
    }

    private boolean shouldBeIgnored(String name) {
      if (classPath==null
        || name.startsWith(THIS_CLASS_IS_ALWAYS_IGNORED)
        || name.toLowerCase().indexOf(ALWAYS_IGNORED)!=-1)
        return true;
      String packageName= name.substring(0,name.lastIndexOf(".")+1);
      if (ignored!=null) {
        for (int k=0; k<ignored.length; k++) {
          String s= ignored[k];
          if (s.endsWith(".**")) {
            if (name.startsWith(s.substring(0,s.length()-2))) return true;
          } else if (s.endsWith(".*")) {
            if (s.substring(0,s.length()-1).equals(packageName)) return true;
          } else if (s.endsWith("*")) {
            if (name.startsWith(s.substring(0,s.length()-1))) return true;
          } else {
            if (s.equals(name)) return true;
          }
        }
      }
      return false;
    }

    private File findFile(String name) {
      return findFile(name,"");
    }
    private File findFile(String name, String extension) {
      for (int k=0; k<classPath.length; k++) {
        File f= new File((new File(classPath[k])).getPath()
          +File.separatorChar
          +name.replace('/',File.separatorChar)+extension);
        if (f.exists()) return f;
      }
      return null;
    }

    public static boolean toStringDetailedDefault= false;
    public int toStringDetailed= -1;
    public String toString() {
      boolean detailed= toStringDetailed<0? toStringDetailedDefault: toStringDetailed>0;
      return "DCO ["+ATools.toS(this.loaderId)+"]"
        +(detailed? " ("
          +classesHash.size()+" classes in cache)\n"
          +"CLASSPATH="+ATools.toS(this.classPath,File.pathSeparator)
          +(this.ignored==null || this.ignored.length==0? "":
            "\nIGNORED="+ATools.toS(this.ignored,File.pathSeparator))
          :"");
    }
  }

  /* ***************** */
  /* Service functions */

  public static boolean enableAccess(AccessibleObject member) {
    try {
      member.setAccessible(true);
      return true;
    } catch (SecurityException e) {
      return false;
    }
  }
  public static boolean hasDeclaredField(Class clazz, String fieldName) {
    try {
      clazz.getDeclaredField(fieldName);
      return true;
    } catch (NoSuchFieldException e) {
      return false;
    }
  }
  public static Field getDeclaredOrPublicField(Class clazz, String fieldName)
  throws NoSuchFieldException
  {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      return clazz.getField(fieldName);
    }
  }
  public static Field getDeclaredFieldInHierarchy(Class clazz, String fieldName)
  throws NoSuchFieldException
  {
    for (; clazz!=null; clazz=clazz.getSuperclass()) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  public static Field[] getDeclaredAndPublicFields(Class clazz) {
    List f= Arrays.asList(clazz.getFields());
    Field[] df= clazz.getDeclaredFields();
    List r= new ArrayList(f.size()+df.length);
    r.addAll(f);
    Set fSet= new HashSet(f);
    for (int k=0; k<df.length; k++)
      if (!fSet.contains(df[k])) r.add(df[k]);
    return (Field[])r.toArray(new Field[0]);
  }
  public static Method[] getDeclaredAndPublicMethods(Class clazz) {
    List m= Arrays.asList(clazz.getMethods());
    Method[] dm= clazz.getDeclaredMethods();
    List r= new ArrayList(m.size()+dm.length);
    r.addAll(m);
    Set mSet= new HashSet(m);
    for (int k=0; k<dm.length; k++)
      if (!mSet.contains(dm[k])) r.add(dm[k]);
    return (Method[])r.toArray(new Method[0]);
  }

  public static Object newInstance(Class clazz, boolean enableIllegalAccess)
  throws InstantiationException,IllegalAccessException,NoSuchMethodException,InvocationTargetException
  {
    if (!enableIllegalAccess) return clazz.newInstance();
    try {
      return clazz.newInstance();
    } catch (IllegalAccessException e) {
      Constructor constructor= clazz.getDeclaredConstructor(new Class[0]);
      constructor.setAccessible(true);
      return constructor.newInstance(new Object[0]);
    }
  }

  public static Object getDeclaredFieldValue(Object object, String fieldName, boolean enableIllegalAccess)
  throws NoSuchFieldException,IllegalAccessException
  {
    Field field= getDeclaredFieldInHierarchy(object.getClass(),fieldName);
    boolean saveAccessible= field.isAccessible();
    if (enableIllegalAccess && !saveAccessible) field.setAccessible(true);
    try {
      return field.get(object);
    } finally {
      if (enableIllegalAccess && !saveAccessible) field.setAccessible(false);
    }
  }
  public static Object getStaticDeclaredFieldValue(Class clazz, String fieldName, boolean enableIllegalAccess)
  throws NoSuchFieldException,IllegalAccessException
  {
    Field field= getDeclaredFieldInHierarchy(clazz,fieldName);
    boolean saveAccessible= field.isAccessible();
    if (enableIllegalAccess && !saveAccessible) field.setAccessible(true);
    try {
      return field.get(null);
    } finally {
      if (enableIllegalAccess && !saveAccessible) field.setAccessible(false);
    }
  }
  public static Object callDeclaredMethod(Object object, String methodName, boolean enableIllegalAccess)
  throws Exception
  {
    return callDeclaredMethod(object,methodName,new Class[0],new Object[0],enableIllegalAccess);
  }
  public static Object callDeclaredMethod(Object object, String methodName,
    Class[] parameterTypes, Object[] parameters, boolean enableIllegalAccess)
  throws Exception
  {
    Method method= object.getClass().getDeclaredMethod(methodName,parameterTypes);
    boolean saveAccessible= method.isAccessible();
    if (enableIllegalAccess && !saveAccessible) method.setAccessible(true);
    try {
      return method.invoke(object,parameters);
    } catch (InvocationTargetException e) {
      Throwable target= e.getTargetException();
      if (target instanceof Exception) throw (Exception)target;
      if (target instanceof Error) throw (Error)target;
      throw e;
    } finally {
      if (enableIllegalAccess && !saveAccessible) method.setAccessible(false);
    }
  }
  public static Object callStaticDeclaredMethod(Class clazz, String methodName, boolean enableIllegalAccess)
  throws Exception
  {
    return callStaticDeclaredMethod(clazz,methodName,new Class[0],new Object[0],enableIllegalAccess);
  }
  public static Object callStaticDeclaredMethod(Class clazz, String methodName,
    Class[] parameterTypes, Object[] parameters, boolean enableIllegalAccess)
  throws Exception
  {
    Method method= clazz.getDeclaredMethod(methodName,parameterTypes);
    boolean saveAccessible= method.isAccessible();
    if (enableIllegalAccess && !saveAccessible) method.setAccessible(true);
    try {
      return method.invoke(null,parameters);
    } catch (InvocationTargetException e) {
      Throwable target= e.getTargetException();
      if (target instanceof Exception) throw (Exception)target;
      if (target instanceof Error) throw (Error)target;
      throw e;
    } finally {
      if (enableIllegalAccess && !saveAccessible) method.setAccessible(false);
    }
  }

  public static class FieldComparator implements Comparator {
    private Field field;
    public FieldComparator(Class clazz, String fieldName) {
      this(getFieldNoException(clazz,fieldName));
    }
    public FieldComparator(Field field) {
      this.field= field;
    }
    private static Field getFieldNoException(Class clazz, String fieldName) {
      try {
        return clazz.getField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new InternalError(e.toString());
      }
    }

    public int compare(Object o1, Object o2) {
      Object f1,f2;
      try {
        f1= (Comparable)field.get(o1);
      } catch (Exception e) {
        return 1;
      }
      try {
        f2= (Comparable)field.get(o2);
      } catch (Exception e) {
        return -1;
      }
      return ((Comparable)f1).compareTo((Comparable)f2);
    }
  }
}