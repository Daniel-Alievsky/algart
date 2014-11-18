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
 * <p>Dynamic Class Overloading system and miscellaneous reflection tools.
 *
 * @version 1.1
 * @author  Daniel Alievsky
 * @since   JDK1.4
 */

public final class Reflection implements TrueStatic {
// TrueStatic is necessary because the control over DCO instances should be global

    // for compatibility
    private static class Logger {
        public void debug(Object s) {
        }
        public void debug(Object s, Object o) {
        }
    }

    private static final Logger LOG = new Logger(); //Logger.getLogger(Reflection.class);
    /**
     * Don't let anyone instantiate this class.
     */
    private Reflection() {}

    public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(Reflection.class);
    /**
     * If true, then <tt>loadClass(String)</tt> call (without <code>loaderId</code>)
     * will always use the system class loader (<code>Class.forName(className)</code> call)
     * until first call of {@link #invalidateClasses()}
     */
    public static final boolean USE_SYSTEM_CLASS_LOADER_AT_FIRST = GlobalProperties.getClassBooleanProperty(Reflection.class,"USE_SYSTEM_CLASS_LOADER_AT_FIRST",true);


    private static String tempClassesDirectoryName = GlobalProperties.getClassPropertyName(Reflection.class,"TEMP_CLASSES_DIRECTORY");
    private static String tempClassesDirectory = GlobalProperties.getProperty(tempClassesDirectoryName);
    private static final boolean isTempClassesDirectoryPropertySet = tempClassesDirectory != null;

    /**
     * The result of <code>System.getProperty("java.class.path","")</code>
     * call splitted into a set of String objects (one path in one String).
     * If Java CLASSPATH cannot be got due to security reasons, this constant
     * contains an empty set.
     */
    public static final Set JAVA_CLASS_PATH;
    static {
        String s = "";
        try {
            s = System.getProperty("java.class.path","");
        } catch (SecurityException e) {
        }
        JAVA_CLASS_PATH = Collections.unmodifiableSet(splitClassPath(s));
        if (isTempClassesDirectoryPropertySet && !JAVA_CLASS_PATH.contains(tempClassesDirectory))
            throw new AssertionError("Internal problem while initializing " + Reflection.class
                + ": \"" + tempClassesDirectoryName + "\" system property is set, "
                + "but this directory is not listed in standard CLASSPATH");
    }
    public static Set splitClassPath(String classPath) {
        LinkedHashSet result = new LinkedHashSet();
        StringTokenizer stringTokenizer = new StringTokenizer(classPath,File.pathSeparator);
        for (; stringTokenizer.hasMoreTokens(); ) {
            result.add(stringTokenizer.nextToken());
        }
        return result;
    }

    /* ********** */
    /* DCO system */


    private static boolean invalidateWasCalled = false;
    private static final HashMap loaderIdToClassLoader = new HashMap(); //loaderId -> ClassLoader
    private static final HashMap loaderIdToClassesId = new HashMap(); //loaderId -> ClassesId

    public static final String CLASSES_TEMPORARY_SUBDIRECTORY_PREFIX = "tempjavaclasses";

    /**
     * Returns the special "temp classes directory" that is added to CLASSPATH
     * by all DCO class loaders. This directory is:<ul>
     *
     * <li>the result of <code>{@link GlobalProperties#getClassProperty(Class, String)
     * GlobalProperties.getClassProperty}(Reflection.class,"TEMP_CLASSES_DIRECTORY")</code>
     * call if it is not null;
     *
     * <li>the result of <code>{@link Directory#createTempDirectory(String, String, File)
     * Directory.createTempDirectory}({@link #CLASSES_TEMPORARY_SUBDIRECTORY_PREFIX},
     * "").getPath()}</code> call in other case.
     *
     * </ul>
     *
     * <p>In the first case this directory <b>must</b> be included in
     * the standard Java CLASSPATH. In other case, <code>Reflection</code>
     * class will produce an assertion exception while static initialization.
     *
     * <p>In the second case this directory is automatically created,
     * as a secondary effect of calling this method, and will be
     * automatically deleted on the program shutdown. I/O exception
     * can occur while creating temp directory. If it is occurred
     * while creating an instance of {@link DynamicClassOverloader},
     * then the exception is ignored, but the created class loader
     * will not include "temp classes directory" into it's CLASSPATH.
     */
    public static synchronized File getTempClassesDirectory() throws IOException {
        File result = tempClassesDirectory == null? null: new File(tempClassesDirectory);
        if (result == null) {
            result = Directory.createTempDirectory(CLASSES_TEMPORARY_SUBDIRECTORY_PREFIX,"");
            tempClassesDirectory = result.getPath();
            FileVisitor.removeOnShutdown(result);
        }
        return result;
    }

    /**
     * Returns true if "net.algart.lib.Reflection.TEMP_CLASSES_DIRECTORY" system property
     * (more precisely, the result of <code>{@link GlobalProperties#getClassProperty(Class, String)
     * GlobalProperties.getClassProperty}(Reflection.class,"TEMP_CLASSES_DIRECTORY")</code> call)
     * is not null. In this case, you can get it's value by {@link
     * #getTempClassesDirectory()} method.
     *
     * @return  true if "net.algart.lib.Reflection.TEMP_CLASSES_DIRECTORY" system property is set
     * @see #getTempClassesDirectory()
     */
    public static boolean isTempClassesDirectoryPropertySet() {
        return isTempClassesDirectoryPropertySet;
    }

    /**
     * Allows to check whether class loading system has been invalidated
     * It returns 0 after first loading <code>Reflection</code> class;
     * it's result increments after each call of <tt>invalidateClasses()</tt>
     * or {@link #invalidateClasses(Object)}.
     *
     * @return  an integer value that is incremented every time when DCO is invalidated
     */
    public static synchronized long getSummaryClassesId() {
        long sum = 0;
        for (Iterator iterator = loaderIdToClassesId.values().iterator(); iterator.hasNext(); )
            sum += ((ClassesId)iterator.next()).value;
        return sum;
    }
    /**
     * getClassesId(loaderId) allows to check whether DCO class loader with given
     * identifier has been invalidated.
     * It returns 0 if this class loader has never used
     */
    public static synchronized long getClassesId(Object loaderId) {
        ClassesId classesId = (ClassesId)loaderIdToClassesId.get(loaderId);
        if (classesId == null) return invalidateWasCalled? 1: 0;
        return classesId.value;
    }

    /**
     * getAllClassesId() returns an unmodifiable map containing
     * <code>getClassesId(...)</code> values for all class loaders
     * known to DCO system. This map contains instances of ClassesId
     * class, that contains only 1 field: "long value"
     */
    public static synchronized Map getAllClassesId() {
        return Collections.unmodifiableMap(loaderIdToClassesId);
    }
    private static final class ClassesId {
        long value;
        ClassesId() {
            this.value = 100;
        }
        ClassesId(long value) {
            this.value = value;
        }
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Invalidates all classes loaded by DCO system
     * All class files will be reloaded by the next <code>loadClass()</code>
     */
    public static synchronized void invalidateClasses() {
        if (!loaderIdToClassLoader.isEmpty()) {
            for (Iterator iterator = loaderIdToClassLoader.keySet().iterator(); iterator.hasNext(); )
                ((ClassesId)loaderIdToClassesId.get(iterator.next())).value++;
            loaderIdToClassLoader.clear();
            LOG.debug("All dynamically loaded classes have been invalidated");
        } else if (!invalidateWasCalled) {
            LOG.debug("Dynamic Overloading Classes system initialized: dynamic classes will be reloaded");
        }
        invalidateWasCalled = true;
    }
    /**
     * Invalidates all classes loaded by the given DCO class loader
     * All these class files will be reloaded by the next <code>loadClass()</code>
     * @param loaderId  Identifier of class loader that should be invalidated.
     *    Can be null; it is <em>not</em> equivalent to <code>invalidateClasses()</code>
     */
    public static synchronized void invalidateClasses(Object loaderId) {
        if (loaderIdToClassLoader.containsKey(loaderId)) {
            LOG.debug(LazyString.format("Dynamically loaded classes have been invalidated [%s]",
                    loaderId == null ? "" : loaderId.toString()));
            ((ClassesId)loaderIdToClassesId.get(loaderId)).value++;
            loaderIdToClassLoader.remove(loaderId);
        }
    }

    /**
     * Loads a .class file by DCO system; allows to specify whether static
     * initializing is required.
     *
     * <p><b>Do not use this method</b> for true-static classes!
     *
     * @param name      Full class name (with package)
     * @param initialize whether the class must be initialized
     * @param loaderId  Identifier of class loader that should be used
     * @throws ClassNotFoundException
     */
    public static synchronized Class loadClass(String name, boolean initialize, Object loaderId)
    throws ClassNotFoundException
    {
        ClassLoader classLoader = getClassLoader(loaderId);
        if (classLoader == null) {
            if (DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString()) + "]   Standard loading (Class.forName()) class " + name);
            if (initialize) return Class.forName(name);
            return Class.forName(name,false,Reflection.class.getClassLoader());
        } else {
            return Class.forName(name,initialize,classLoader);
        }
    }

    /**
     * Returns DCO class loader used by loadClass(...,loaderId)
     */
    public static synchronized ClassLoader getClassLoader(Object loaderId) {
        ClassLoader result = null;
        if (!(USE_SYSTEM_CLASS_LOADER_AT_FIRST && !invalidateWasCalled && loaderId == null)) {
            result = (ClassLoader)loaderIdToClassLoader.get(loaderId);
            if (result == null) {
                result = createClassLoader(loaderId);
                if (result != null) {
                    if (DEBUG_LEVEL >= 2) LOG.debug("New DCO created with for " +loaderId + " loaderId: " + result);
                    loaderIdToClassLoader.put(loaderId,result);
                    if (!loaderIdToClassesId.containsKey(loaderId))
                        loaderIdToClassesId.put(loaderId,new ClassesId());
                }
            }
        }
        return result;
    }

    /**
     * @param loaderId Identifier of Dynamic Class Overloader
     * @return Class loader, created by current class loader factory
     * assigned by <tt>setClassLoaderFactory()</tt>, or Dynamic
     * Class Overloader created with default settings in a case
     * when not class loader producer has been assigned
     */
    private static ClassLoader createClassLoader(Object loaderId) {
        if (classLoaderFactory == null) return null;
        return classLoaderFactory.createClassLoader(loaderId);
    }

    private static Set fileSystemJavaClassPath = null;
    /**
     * Returns a part of the defaultJava CLASSPATH that corresponds
     * to usual subdirectories, not ZIP or JAR. Result is a set
     * of String objects.
     */
    public static synchronized Set getFileSystemJavaClassPath() {
        if (fileSystemJavaClassPath == null) {
            Set set = new LinkedHashSet();
            for (Iterator i = JAVA_CLASS_PATH.iterator(); i.hasNext(); ) {
                String path = (String)i.next();
                if (new File(path).isDirectory())
                    set.add(path);
            }
            fileSystemJavaClassPath = Collections.unmodifiableSet(set);
        }
        return fileSystemJavaClassPath;
    }


    /**
     * Returns true if the given class (or some ot it's ancestors)
     * implements an interface called "<code>&lt;some_package&gt;.TrueStatic</code>".
     * See {@link TrueStatic} interface.
     *
     * @param clazz Some checked class is true-static: DCO system
     *    will not overload it.
     * @return      true, if the given class implements some interface named "<code>TrueStatic</code>"
     */
    public static boolean isTrueStaticImplemented(Class clazz) {
        for (; clazz != null; clazz = clazz.getSuperclass()) {
            Class[] interfaces = clazz.getInterfaces();
            for (int k = 0; k < interfaces.length; k++) {
                String s = interfaces[k].getName();
                s = s.substring(s.lastIndexOf(".") + 1);
                if (s.equals(TRUE_STATIC_INTERFACE_NAME_PRIVATE)) return true;
                if (isTrueStaticImplemented(interfaces[k])) return true;
                    // - recursion through all interfaces extended by this interface
            }
        }
        return false;
    }

    public static boolean isTrueStatic(Class clazz) {
        for (Class c = clazz; c != null; c = c.getDeclaringClass()) {
            boolean isTrueStatic = isTrueStaticImplemented(c);
            if (DEBUG_LEVEL >= 3) LOG.debug("Checking interfaces of " + c + ": "
                + (isTrueStatic? "TRUE STATIC": "TrueStatic not found..."));
            if (isTrueStatic) return true;
        }
        try {
            if (DEBUG_LEVEL >= 3) LOG.debug("Checking \"isTrueStaticFlag\" field of " + clazz + ": ");
            clazz.getDeclaredField("isTrueStaticFlag");
            if (DEBUG_LEVEL >= 3) LOG.debug("TRUE STATIC");
            return true;
        } catch (NoSuchFieldException e) {
            if (DEBUG_LEVEL >= 3) LOG.debug("not found; will be overloaded");
        }
        return false;
    }

    /**
     * ClassLoaderFactory interface should produce some ClassLoader for
     * given ClassLoader identifier.
     */
    public interface ClassLoaderFactory {
        /**
         * @param loaderId  Identifier of some Class Loader; may be null
         * @return          Some ClassLoader; will be used by Reflection.loadClass
         *                  for this identifier
         */
        public ClassLoader createClassLoader(Object loaderId);
    }

    /**
     * This interface should be implemented by a ClassLoader ineritor
     * that allows to get list of all class paths where it searches the classes.
     */
    public interface WithClassPath {
        /**
         * Returns CLASSPATH as a set (should contain String elements only)
         * @return  list of all class paths
         */
        public Set classPath();
    }

    /**
     * Current class loader factory used by <code>loadClass()</code>
     */
    private static Reflection.ClassLoaderFactory classLoaderFactory;
    public static void setClassLoaderFactory(ClassLoaderFactory classLoaderFactory) {
        Reflection.classLoaderFactory = classLoaderFactory;
    }
    public static ClassLoaderFactory getClassLoaderFactory() {
        return Reflection.classLoaderFactory;
    }

    private static final String TRUE_STATIC_INTERFACE_NAME_PRIVATE;
    static {
        String s = TrueStatic.class.getName();
        TRUE_STATIC_INTERFACE_NAME_PRIVATE = s.substring(s.lastIndexOf(".") + 1);
    }

    public static class DynamicClassOverloader extends ClassLoader implements WithClassPath {
        private Map classesHash = new HashMap();
        protected static final String[] ALWAYS_IGNORED_PREFIXES = {
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "net.algart.arrays.",
        };
        // We should never dynamically load this class Reflection:
        // in other case, invalidateClasses() will not work from dynamic classes
        private static final String ALWAYS_IGNORED_SUBSTRING = "truestatic";
        // If the full name of a class, converted to lower case, contains
        // ALWAYS_IGNORED substring, this class is never dynamically loaded

        private final String[] classPath;
        /**
         * Example of <code>ignored</code>: {
         * "net.algart.lib.**" - means net.algart.lib package and all its subpackages
         * "net.algart.lib.*" - means net.algart.lib package only
         * "net.algart.lib.Out" - means net.algart.lib.Out class only
         */
        private final String[] ignored;
        private final Object loaderId; // can be used for debugging
        private final ClassLoader parentClassLoader;
        private final boolean overloadClassesFoundByParent;

        public final Set classPath() {
            return Collections.unmodifiableSet(new LinkedHashSet(java.util.Arrays.asList(classPath)));
        }
        public final Set ignored() {
            return Collections.unmodifiableSet(new LinkedHashSet(java.util.Arrays.asList(ignored)));
        }

        public DynamicClassOverloader(
            Set classPath,
            Set ignored,
            Object loaderId,
            ClassLoader parentClassLoader,
            boolean overloadClassesFoundByParent)
        {
            if (classPath == null) throw new NullPointerException("Null classPath argument (\"Collections.EMPTY_SET\" should be used)" + msgEnd(null));
            if (ignored == null) throw new NullPointerException("Null ignored argument (\"Collections.EMPTY_SET\" should be used)" + msgEnd(null));
            this.classPath = (String[])classPath.toArray(new String[0]);
            this.ignored = (String[])ignored.toArray(new String[0]);
            this.loaderId = loaderId;
            this.parentClassLoader = parentClassLoader;
            this.overloadClassesFoundByParent = overloadClassesFoundByParent;
        }

        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString()) + "]   Request for class " + name);
            Class result = findClass(name);
            if (resolve) resolveClass(result);
            return result;
        }
        protected Class findClass(String name) throws ClassNotFoundException {
            Class result = (Class)classesHash.get(name);
            if (result != null) {
                if (DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString()) + "]   Class " + name + " found in cache");
                return result;
            }
            Throwable occurredException = null;
            try {
                boolean useFileLoader = true;
                if (!overloadClassesFoundByParent) {
                    result = tryToLoadClassByParentClassLoader(name,true,true);
                    useFileLoader = result == null;
                }
                if (useFileLoader) {
                    File f = null;
                    useFileLoader = !shouldNotBeOverloaded(name);
                    if (useFileLoader) {
                        f = findFile(name.replace('.','/'),".class");
                        if (f == null) useFileLoader = false;
                        if (useFileLoader) {
                            if (result != null) throw new AssertionError("Internal problem (useFileLoader && result != null)" + msgEnd("findClass"));
                            result = tryToLoadClassByParentClassLoader(name,false,overloadClassesFoundByParent);
                            if (result != null && shouldNotBeOverloaded(result)) useFileLoader = false;
                            if (useFileLoader) {
                                if (DEBUG_LEVEL >= 1) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                                    + "] Loading class " + name + (DEBUG_LEVEL >= 4? " from " + f.getPath(): ""));
                                byte[] classBytes = BinaryIO.read(f);
                                result = defineClass(name,classBytes,0,classBytes.length);
                                if (DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                                    + "]   Class " + name + " is successfully loaded from file");
                            } else { // class file was found, but test preloading by parent revealed that this class should not be overloaded
                                if (DEBUG_LEVEL >= 1) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                                    + "] Class " + name + " is non-overloadable: use "
                                    + (parentClassLoader == null? "system": "the parent (" + parentClassLoader + ")"));
                            }
                        } else { // class file was not found in this.classPath
                            if (DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                                + "]   Class file for " + name + " is not found: try to use "
                                + (parentClassLoader == null? "system": "the parent (" + parentClassLoader + ")"));
                            result = loadClassByParentClassLoader(name,true);
                        }
                    } else { // class NAME is disabled for overloading
                        if (DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                            + "]   Class name " + name + " is disabled to be overloaded: try to use "
                            + (parentClassLoader == null? "system": "the parent (" + parentClassLoader + ")"));
                        result = loadClassByParentClassLoader(name,true);
                    }
                } // else: !overloadClassesFoundByParent and class was successully loaded by parent
            } catch (IOException e) {
                throw new ClassNotFoundException("Cannot load class [" + (loaderId == null? "": loaderId) + "]: " + e);
            } catch (ClassFormatError e) {
                throw new ClassNotFoundException("Format of class file incorrect for class " + name + " [" + (loaderId == null? "": loaderId.toString()) + "]: " + e);
            } catch (ClassNotFoundException e) {
                occurredException = e; throw e;
            } catch (RuntimeException e) {
                occurredException = e; throw e;
            } catch (Error e) {
                occurredException = e; throw e;
            } finally {
                if (occurredException != null && DEBUG_LEVEL >= 1) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                    + "] ERROR WHILE LOADING " + name + ": " + occurredException);
            }
            classesHash.put(name,result);
            return result;
        }

        private Class loadClassByParentClassLoader(String name, boolean debugMessage)
        throws ClassNotFoundException
        {
            Class result =  parentClassLoader == null? findSystemClass(name): parentClassLoader.loadClass(name);
            if (debugMessage && DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                + "]   " + result + " is successfully loaded by " + (parentClassLoader == null? "system": "the parent (" + parentClassLoader + ")"));
            return result;
        }
        private Class tryToLoadClassByParentClassLoader(String name,
                                                        boolean successfulDebugMessage,
                                                        boolean classNotFoundDebugMessage)
        {
            try {
                return loadClassByParentClassLoader(name,successfulDebugMessage);
            } catch (ClassNotFoundException e) {
                if (classNotFoundDebugMessage && DEBUG_LEVEL >= 2) LOG.debug("[" + (loaderId == null? "": loaderId.toString())
                    + "]   Class " + name + " cannot be found by " + (parentClassLoader == null? "system": "the parent (" + parentClassLoader + ")"));
                return null;
            }
        }

        public java.net.URL getResource(String name) {
            if (DEBUG_LEVEL >= 1) LOG.debug("[" + (loaderId == null? "": loaderId.toString()) + "] Getting resource " + name);
            java.net.URL url = super.getResource(name);
            if (DEBUG_LEVEL >= 1) LOG.debug(url == null? "": ": found at " + url);
            return url;
        }
        protected java.net.URL findResource(String name) {
            File f = findFile(name);
            if (DEBUG_LEVEL >= 1) LOG.debug("(DCO findResource())" + (DEBUG_LEVEL >= 4? " in " + Out.join(classPath,File.pathSeparator): ""));
            if (f == null) {
                if (DEBUG_LEVEL >= 1) LOG.debug(": NOT FOUND");
                return null;
            }
            try {
                return f.toURI().toURL();
            } catch (java.net.MalformedURLException e) {
                if (DEBUG_LEVEL >= 1) LOG.debug(": CANNOT BE FOUND - " + e);
                return null;
            }
        }

        protected boolean shouldNotBeOverloaded(String name) {
            if (classPath.length == 0 || name.toLowerCase().indexOf(ALWAYS_IGNORED_SUBSTRING) != -1)
                return true;
            for (int k = 0, n = ALWAYS_IGNORED_PREFIXES.length; k < n; k++)
                if (name.startsWith(ALWAYS_IGNORED_PREFIXES[k])) return true;
            String packageName = name.substring(0,name.lastIndexOf(".")+1);
            for (int k = 0; k < ignored.length; k++) {
                String s = ignored[k];
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
            return false;
        }

        protected boolean shouldNotBeOverloaded(Class clazz) {
            return isTrueStatic(clazz);
        }

        private File findFile(String name) {
            return findFile(name,"");
        }
        private File findFile(String name, String extension) {
            for (int k = 0; k < classPath.length; k++) {
                File f = new File((new File(classPath[k])).getPath()
                    +File.separatorChar
                    +name.replace('/',File.separatorChar)+extension);
                if (f.exists()) return f;
            }
            return null;
        }

        public String toString() {
            return "DynamicClassOverloader " + Out.hex(System.identityHashCode(this))
                + " [" + (loaderId == null? "": loaderId.toString()) + "] ("
                + classesHash.size() + " cached classes; "
                + (DEBUG_LEVEL >= 4?
                    "classpath=" + Out.join(this.classPath,File.pathSeparator)
                    + (this.ignored.length == 0? "": "; ignored=" + Out.join(this.ignored,File.pathSeparator)):
                    this.classPath.length + " classpaths"
                    + (this.ignored.length == 0? "": ", " + this.ignored.length + " ignored"))
                + ") "
                + (overloadClassesFoundByParent? "overloads ": "joined to ")
                + (parentClassLoader == null? "system": "the parent (" + parentClassLoader + ")");
        }

        private static String msgEnd(String methodName) {
            return " in " + DynamicClassOverloader.class.getName() + (methodName == null? " constructor": "." + methodName);
        }
    }

    /* ***************** */
    /* Service functions */

    public static java.net.URL getClassURL(Class clazz) {
        String s = clazz.getName();
        s = s.substring(s.lastIndexOf(".")+1);
        return clazz.getResource(s + ".class");
    }
    public static File getClassFile(Class clazz) {
    // The file will exist only if it is usual class-file,
    // not a part of JAR or Web resource
        String s = clazz.getName();
        s = s.substring(s.lastIndexOf(".")+1);
        s = clazz.getResource(s + ".class").getFile();
        try {
            s = java.net.URLDecoder.decode(s,TextIO.UTF8.charsetName());
        } catch(java.io.UnsupportedEncodingException e) {
        }
        return new File(s);
    }
    public static String getClassName(Class clazz, char nestedClassSeparator) {
        StringBuffer name = new StringBuffer(clazz.getName());
        for (Class c = clazz.getDeclaringClass(); c != null; c = c.getDeclaringClass())
            name.setCharAt(c.getName().length(),nestedClassSeparator);
        return name.toString();
    }

    public static StackTraceElement[] getCurrentStackTrace() {
        return getCurrentStackTrace(1,Integer.MAX_VALUE);
    }
    public static StackTraceElement[] getCurrentStackTrace(int beginIndex, int len) {
        if (len <= 0) return new StackTraceElement[0];
        try {
            throw new Exception();
        } catch (Exception e) {
            StackTraceElement[] stackTrace= e.getStackTrace();
            if (stackTrace.length <= 1) return stackTrace;
            if (beginIndex < 0) beginIndex = 0;
            if (beginIndex >= stackTrace.length-1) return new StackTraceElement[0];
            beginIndex++; // now we are sure that beginIndex<=Integer.MAX_VALUE-1
            if (len >= stackTrace.length-beginIndex) len = stackTrace.length-beginIndex;
            StackTraceElement[] result = new StackTraceElement[len];
            for (int k = 0; k < len; k++)
                result[k]= stackTrace[k+beginIndex];
            return result;
        }
    }
}
