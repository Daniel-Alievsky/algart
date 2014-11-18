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
import net.algart.array.Arrays;

/**
 * <p>Allows to use external Java compilers (as <code>com.sun.tools.javac</code>)
 * to compile and load new Java classes and Java formulas
 *
 * <p><b>Synchronization note</b>: different threads should use different
 * instances of this class because the arguments of the compiler are
 * set through the fields of <code>JavaCompiler</code> instances.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public final class JavaCompiler implements Cloneable, TrueStatic {
    public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(JavaCompiler.class);
    /**
     * The system property returned by <code>{@link GlobalProperties#getClassBooleanProperty(Class, String, boolean)
     * GlobalProperties.getClassBooleanProperty}(JavaCompiler.class, "USE_DCO", true)</code> call:
     * it indicates whether this class will use {@link Reflection.DynamicClassOverloader Dynamic Class Overloaders}.
     * This property should be set to <code>false</code> <b>only</b> if the {@link
     * Reflection#getTempClassesDirectory() TEMP_CLASSES_DIRECTORY} property is also set
     * and included in the standard system CLASSPATH.
     */
    public static final boolean USE_DCO = GlobalProperties.getClassBooleanProperty(JavaCompiler.class,"USE_DCO",true);
    private static final String useDcoPropertyNamePrivate = GlobalProperties.getClassPropertyName(JavaCompiler.class,"USE_DCO");
    static {
        if (!USE_DCO && !Reflection.isTempClassesDirectoryPropertySet())
            throw new AssertionError("Internal problem while initializing " + JavaCompiler.class
                + ": \"" + useDcoPropertyNamePrivate + "\" system property is set to false, "
                +"but \"" + GlobalProperties.getClassPropertyName(Reflection.class,"TEMP_CLASSES_DIRECTORY")
                +"\" system property is not specified");
    }

    public static final String EVAL_DEFAULT_IMPORTS =
        "import java.io.*;\n"
        + "import java.util.ArrayList;\n"
        // skipping java.util.Arrays to avoid a conflict with net.algart.array.Arrays
        + "import java.util.BitSet;\n"
        + "import java.util.Calendar;\n"
        + "import java.util.Collections;\n"
        + "import java.util.Currency;\n"
        + "import java.util.Date;\n"
        // skipping deprecated java.util.Dictionary
        + "import java.util.EventListenerProxy;\n"
        + "import java.util.EventObject;\n"
        + "import java.util.GregorianCalendar;\n"
        + "import java.util.HashMap;\n"
        + "import java.util.HashSet;\n"
        // skipping deprecated java.util.Hashtable
        + "import java.util.IdentityHashMap;\n"
        + "import java.util.LinkedHashMap;\n"
        + "import java.util.LinkedHashSet;\n"
        + "import java.util.LinkedList;\n"
        + "import java.util.ListResourceBundle;\n"
        + "import java.util.Locale;\n"
        + "import java.util.Observable;\n"
        + "import java.util.Properties;\n"
        + "import java.util.PropertyPermission;\n"
        + "import java.util.PropertyResourceBundle;\n"
        + "import java.util.Random;\n"
        + "import java.util.ResourceBundle;\n"
        + "import java.util.SimpleTimeZone;\n"
        // skipping deprecated java.util.Stack
        + "import java.util.StringTokenizer;\n"
        + "import java.util.Timer;\n"
        + "import java.util.TimerTask;\n"
        + "import java.util.TimeZone;\n"
        + "import java.util.TreeMap;\n"
        + "import java.util.TreeSet;\n"
        // skipping deprecated java.util.Vector
        + "import java.util.WeakHashMap;\n"
        + "import java.util.ConcurrentModificationException;\n"
        + "import java.util.EmptyStackException;\n"
        + "import java.util.MissingResourceException;\n"
        + "import java.util.NoSuchElementException;\n"
        + "import java.util.TooManyListenersException;\n"
        + "import net.algart.lib.*;\n"
        + "import net.algart.array.*;\n";

    public static final String EVAL_PACKAGE = "eval"; // null to block

    public abstract static class LocalStaticException extends AbstractException {
        LocalStaticException() {}
        LocalStaticException(String s) {super(s);}
    }
    public abstract class LocalException extends LocalStaticException {
        String infoUsedTool = usedTool + "";
        String infoCompileClassPath = Out.join(compileClassPath,File.pathSeparator);
        String infoCompileDestinationDirectory = compileDestinationDirectory;
        String infoCompileAdditionalOptions = Out.join(compileAdditionalOptions," ");
        public void clear() {
            infoUsedTool = null;
            infoCompileClassPath = null;
            infoCompileDestinationDirectory = null;
            infoCompileAdditionalOptions = null;
        }
    }
    public static class ToolNotFoundException extends LocalStaticException {
        public ToolNotFoundException(String toolName, String s) {
            super(toolName+" not found; "
                +"maybe, Java compiler is not installed on this computer.\nSystem message: " + s);
        }
    }
    public static class ToolInvalidCallException extends LocalStaticException {
        public ToolInvalidCallException(String toolName, String s) {
            super("Incompatible version of "+toolName+" - cannot use it's methods.\n"
                +"Maybe, incompatible Java machine is installed on this computer.\nSystem message: " + s);
        }
    }
    public interface CompilationException {
        public String getFullCompilerMessage();
        public String getErrorMessage();
        public Integer getLineIndex();   // line numbers start from 1, not 0
        public void setLineIndex(Integer v);
        public Integer getCharIndex();
        public void setCharIndex(Integer v);
        public String getProblemLine();
        public void setProblemLine(String v);
    }
    public abstract class ToolException extends LocalException {
        String infoCompilerArguments;
        String infoJavaFile;
        String infoClassName;
        public ToolException(String[] argv, File javaFile, String className) {
            this.infoCompilerArguments = Out.join(argv," ");
            this.infoJavaFile = javaFile.getPath();
            this.infoClassName = className;
            this.infoCompileClassPath= null;
            // - infoCompilerArguments already contains this information
        }
    }
    public class ToolInternalException extends ToolException {
        String infoInternalExceptionMessage;
        public ToolInternalException(String[] argv, File javaFile, String className, Throwable e) {
            super(argv,javaFile,className);
            setMessage("Exception occurred while calling " + usedTool + "\n"
                +(this.infoInternalExceptionMessage = e.toString()));
        }
    }
    public class CompilerErrorException extends ToolException implements CompilationException {
        String infoFullCompilerMessage;
        String infoErrorMessage;
        Integer infoLineIndex;
        Integer infoCharIndex;
        String infoProblemLine;
        int infoCompilerExitStatus;
        CompilerErrorException(String[] argv, File javaFile, CompilationResults results) {
            super(argv,javaFile,results.className());
            this.infoFullCompilerMessage = results.compilerOutput();
            this.infoCompilerExitStatus = results.exitStatus();
            int p = 0;
            for (; ; p++) {
                int q = p;
                p = infoFullCompilerMessage.indexOf("\n",p+1);
                if (p == -1) break;
                while (q < p && infoFullCompilerMessage.charAt(q) <= ' ') q++;
                String line = infoFullCompilerMessage.substring(q,p);
                // we found a line without lead spaces from q to p
                int p1 = line.indexOf(".java:");
                if (p1 == -1) continue;
                p1 += ".java:".length();
                int p2 = line.indexOf(":",p1);
                if (p2 == -1) continue;
                try {
                    infoLineIndex = Integer.valueOf(line.substring(p1,p2));
                    infoErrorMessage = line.substring(p2+1).trim();
                    break;
                } catch (NumberFormatException e) {}
            }
            String previousLine = null;
            for (; ; p++) {
                int q = p;
                p = infoFullCompilerMessage.indexOf("\n",p+1);
                if (p == -1) break;
                char c;
                while (q < p && (c=infoFullCompilerMessage.charAt(q)) < ' ' && c != '\t') q++;
                int p1 = p;
                while (p1 > q && (c=infoFullCompilerMessage.charAt(p1-1)) < ' ' && c != '\t') p1--;
                String line = infoFullCompilerMessage.substring(q,p1);
                // we found a line without lead/trail control characters (\r is implied)
                p1 = 0;
                int lineLen = line.length();
                while (p1 < lineLen && line.charAt(p1) <= ' ') p1++;
                int p2 = lineLen;
                while (p2 > 0 && line.charAt(p2-1) <= ' ') p2--;
                if (p2 == p1+1 && line.charAt(p1) == '^') {
                    if (previousLine != null) {
                        infoCharIndex = new Integer(p1);
                        infoProblemLine = previousLine;
                    }
                    break;
                }
                previousLine = line;
            }
        }
        public String getFullCompilerMessage()  {return infoFullCompilerMessage;}
        public String getErrorMessage()         {return infoErrorMessage;}
        public Integer getLineIndex()           {return infoLineIndex;}
        public void setLineIndex(Integer v)     {infoLineIndex = v;}
        public Integer getCharIndex()           {return infoCharIndex;}
        public void setCharIndex(Integer v)     {infoCharIndex = v;}
        public String getProblemLine()          {return infoProblemLine;}
        public void setProblemLine(String v)    {infoProblemLine = v;}
    }
    public class CannotFindCompiledClassException extends LocalException {
        String infoClassFile;
        public CannotFindCompiledClassException(File classFile) {
            this.infoClassFile = classFile.getPath();
        }
    }
    public static class CannotCorrectClassException extends LocalStaticException {
        {
            setMessage("Internal error in JavaCompiler.eval(): cannot"
                + " recognize and correct Java class file. Maybe,"
                + " current version of Java compiler is incompatible with "
                + JavaCompiler.class);
        }
    }
    public static class CannotChangeSourceOfLoadedClassException extends IllegalArgumentException {
        public CannotChangeSourceOfLoadedClassException(String mainClassName) {
            super("Some class with the same name \"" + mainClassName
                + "\" BUT A DIFFERENT SOURCE CODE has been already compiled and loaded by " + JavaCompiler.class.getName()
                + ".compileAndLoad method. It is prohibited usually (excepting a special case when \""
                + useDcoPropertyNamePrivate + "\" system property "
                + "is true and " + JavaCompiler.class.getName() + ".invalidateCache method has been called)");
        }
    }


    public abstract static class UsedTool {
        private UsedTool() {}
        public static class CompilationResults {
            private final boolean successful;
            private final int exitStatus;         // exit status of "javac" utility class
            private final String compilerOutput;  // never null, maybe ""
             // full text output of compiler;
             // in a case of exception will be returned as infoCompilerMessage
            public CompilationResults(boolean successful, int exitStatus, String compilerOutput) {
                if (compilerOutput == null) throw new NullPointerException("Null compilerOutput argument in CompilationResults constructor");
                this.successful = successful;
                this.exitStatus = exitStatus;
                this.compilerOutput = compilerOutput;
            }
            public boolean successful()     {return this.successful;}
            public int exitStatus()         {return this.exitStatus;}
            public String compilerOutput()  {return this.compilerOutput;}
        }

        public abstract CompilationResults compile(String[] argv)
        throws ToolNotFoundException, ToolInvalidCallException, InvocationTargetException;

        Class findToolClass(String className) throws ToolNotFoundException {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new ToolNotFoundException(this.toString(),e.toString());
            } catch (NoClassDefFoundError e) {
                throw new ToolNotFoundException(this.toString(),e.toString());
            }
        }

        static final class SunToolsJavac extends UsedTool {
            public static final String CLASS_NAME = "sun.tools.javac.Main";
            private SunToolsJavac() {}
            public String toString() {
                return "class " + CLASS_NAME;
            }

            private boolean javacOk = false;
            private Object javacInstance;
            private Method compileMethod;
            private Method getExitStatusMethod;
            private ByteArrayOutputStream outputStream;

            public UsedTool.CompilationResults compile(String[] argv)
            throws ToolNotFoundException, ToolInvalidCallException, InvocationTargetException
            {
                Boolean successful;
                String compilerOutput;
                Integer exitStatus;
                synchronized(this) {
                    // this initialization secion should be synchronized to get correct internal fields simultaneously
                    if (!this.javacOk) {
                        this.outputStream = new ByteArrayOutputStream();
                        Class main = findToolClass(CLASS_NAME);
                        try {
                            Constructor constructor = main.getDeclaredConstructor(new Class[] {OutputStream.class,String.class});
                            this.javacInstance = constructor.newInstance(new Object[] {outputStream,"javac"});
                            this.compileMethod = main.getDeclaredMethod("compile", new Class[] {String[].class});
                            this.getExitStatusMethod= main.getDeclaredMethod("getExitStatus", new Class[0]);
                        } catch (InstantiationException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (IllegalAccessException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (InvocationTargetException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (ExceptionInInitializerError e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (NoSuchMethodException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        }
                        this.javacOk = true;
                    } else {
                        this.outputStream.reset();
                    }
                }
                try {
                    successful = (Boolean)this.compileMethod.invoke(this.javacInstance, new Object[] {argv});
                    exitStatus = (Integer)this.getExitStatusMethod.invoke(this.javacInstance, new Object[0]);
                    compilerOutput = outputStream.toString();
                } catch (IllegalAccessException e) {
                    throw new ToolInvalidCallException(this.toString(),e.toString());
                }
                return new UsedTool.CompilationResults(
                    successful.booleanValue(),exitStatus.intValue(),compilerOutput);
            }
        }

        static final class ComSunToolsJavac extends UsedTool {
            public static final String CLASS_NAME = "com.sun.tools.javac.Main";
            private ComSunToolsJavac() {}
            public String toString() {
                return "class " + CLASS_NAME;
            }

            private boolean javacOk= false;
            private Object javacInstance;
            private Method compileMethod;
            private CharArrayWriter writer;

            public UsedTool.CompilationResults compile(String[] argv)
            throws ToolNotFoundException, ToolInvalidCallException, InvocationTargetException
            {
                boolean successful;
                String compilerOutput;
                int exitStatus;
                synchronized(this) {
                    // this initialization secion should be synchronized to get correct internal fields simultaneously
                    if (!this.javacOk) {
                        Class main = findToolClass(CLASS_NAME);
                        try {
                            this.javacInstance = main.newInstance();
                            this.compileMethod = main.getDeclaredMethod("compile", new Class[] {
                                String[].class,
                                PrintWriter.class,
                            });
                            this.writer = new CharArrayWriter();
                            this.javacOk = true;
                        } catch (InstantiationException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (IllegalAccessException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (ExceptionInInitializerError e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        } catch (NoSuchMethodException e) {
                            throw new ToolInvalidCallException(this.toString(),e.toString());
                        }
                    } else {
                        this.writer.reset();
                    }
                }
                try {
                    exitStatus = ((Integer)this.compileMethod.invoke(this.javacInstance,
                        new Object[] {argv, new PrintWriter(writer,true)})).intValue();
                    successful = exitStatus == 0;
                    compilerOutput = writer.toString();
                } catch (IllegalAccessException e) {
                    throw new ToolInvalidCallException(this.toString(),e.toString());
                }
                return new UsedTool.CompilationResults(successful,exitStatus,compilerOutput);
            }
        }

        public static final UsedTool SUN_TOOLS_JAVAC = new SunToolsJavac();
        public static final UsedTool COM_SUN_TOOLS_JAVAC = new ComSunToolsJavac();
        public static final UsedTool DEFAULT = COM_SUN_TOOLS_JAVAC;
    }


    JavaCompiler(UsedTool usedTool) {
        if (usedTool == null) usedTool = UsedTool.DEFAULT;
        this.usedTool = usedTool;
    }

    public static JavaCompiler getInstance() {
        return new JavaCompiler(null);
    }
    public static JavaCompiler getInstance(UsedTool usedTool) {
        return new JavaCompiler(usedTool);
    }

    private final UsedTool usedTool;
    public UsedTool getUsedTool() {
        return usedTool;
    }
    private Set compileClassPath = Reflection.JAVA_CLASS_PATH;
    // Value of -classpath option
    private String compileDestinationDirectory = null;
    // Value of -d option. When "", the directory containing source Java-file
    // will be passed to compiler. When null, -d option will not be used
    private String[] compileAdditionalOptions = new String[0];
    // Can contain other flags, for example: {"-verbose","-encoding",someEncoding}.
    // May include -classpath arguments pair; this classpath is added to
    // this.compileClassPath
    // May include -d arguments pair; this destination directory overrides
    // this.compileDestinationDirectory

    private String evalImports = EVAL_DEFAULT_IMPORTS;
    // Used by eval() methods

    public void setCompileClassPath(Set value) {
        if (value == null) throw new NullPointerException("Null argument in " + JavaCompiler.class.getName() + ".setCompileAdditionalOptions. Please use \"Collections.EMPTY_SET\" instead null");
        this.compileClassPath = new LinkedHashSet();
        for (Iterator i = value.iterator(); i.hasNext(); ) {
            Object item = i.next();
            if (!(item instanceof String)) throw new IllegalArgumentException("Illegal argument in " + JavaCompiler.class.getName() + ".setCompileAdditionalOptions: it must contain String objects only");
            this.compileClassPath.add(item);
        }
    }
    public Set getCompileClassPath() {
        return Collections.unmodifiableSet(this.compileClassPath);
    }
    public void setCompileDestinationDirectory(String value) {
        this.compileDestinationDirectory = value;
    }
    public String getCompileDestinationDirectory() {
        return this.compileDestinationDirectory;
    }
    public void setCompileAdditionalOptions(String[] value) {
        if (value == null) throw new NullPointerException("Null argument in setCompileAdditionalOptions. Please use \"new String[0]\" instead null");
        this.compileAdditionalOptions = Arrays.copy(value);
    }
    public String[] getCompileAdditionalOptions() {
        return Arrays.copy(this.compileAdditionalOptions);
    }
    public void setEvalImports(String value) {
        this.evalImports = value;
    }
    public String getEvalImports() {
        return this.evalImports;
    }

    public static class CompilationResults extends UsedTool.CompilationResults {
        private final String className;       // always =classFile.getName() without ".class"
        public CompilationResults(UsedTool.CompilationResults toolCompilationResults, String className) {
            super(
                toolCompilationResults.successful(),
                toolCompilationResults.exitStatus(),
                toolCompilationResults.compilerOutput());
            this.className = className;
        }
        public String className() {
            return className;
        }
    }
    public CompilationResults compileJavaFile (File javaFile)
    throws
        ToolNotFoundException,
        ToolInvalidCallException,
        ToolInternalException,
        CompilerErrorException
    {
    // Compile javaFile. If some error occurs, corresponding exception raises
        String className = javaFile.getName();
        int p = className.lastIndexOf(".");
        if (p != -1) className = className.substring(0,p);
        String classDir = this.compileDestinationDirectory;
        if ("".equals(classDir)) classDir = javaFile.getParent();
        String classPath = Out.join(this.compileClassPath, File.pathSeparator);
        List argList = new ArrayList();
        if (this.compileAdditionalOptions != null) {
            for (int k = 0; k < this.compileAdditionalOptions.length; k++) {
                String arg = this.compileAdditionalOptions[k];
                if (arg.equals("-classpath")) {
                    if (k+1 < this.compileAdditionalOptions.length) {
                        k++;
                        if (classPath.length() > 0) classPath+= File.pathSeparator;
                        classPath+= this.compileAdditionalOptions[k];
                    }
                } else if (arg.equals("-d")) {
                    if (k+1 < this.compileAdditionalOptions.length) {
                        k++;
                        classDir = this.compileAdditionalOptions[k];
                    }
                } else {
                    argList.add(arg);
                }
            }
        }
        if (classPath.length() > 0) {
            argList.add("-classpath");
            argList.add(classPath);
        }
        if (classDir != null && classDir.length() > 0) {
            argList.add("-d");
            argList.add(classDir);
        }
        argList.add(javaFile.getPath());
        String[] argv = (String[])argList.toArray(new String[0]);

        CompilationResults results;
        try {
            UsedTool.CompilationResults toolCompilationResults = usedTool.compile(argv);
            results = new CompilationResults(toolCompilationResults,className);
        } catch (InvocationTargetException e) {
            throw new ToolInternalException(argv,javaFile,className,e.getTargetException());
        }
        if (!results.successful())
            throw new CompilerErrorException(argv,javaFile,results);
        return results;
    }


    interface ClassCorrector {
        byte[] correct(byte[] contentOfClassFile, File classFile) throws CannotCorrectClassException;
    }

    public Class compileAndLoad(String source,
        String mainClassName,
        ClassLoader parentClassLoader // parent class loader; can be null
    ) throws
        IOException,
        ToolNotFoundException,
        ToolInvalidCallException,
        ToolInternalException,
        CompilerErrorException,
        CannotFindCompiledClassException,
        CannotCorrectClassException
    {
        return compileAndLoad(source,mainClassName,parentClassLoader,null);
    }

    /**
     * Note! <code>classCorrector</code> and <code>parentClassLoader</code> arguments
     * are used only when the class is compiled. It means that they will be be ignored
     * if you call this method second time (after application start or last
     * {@link #invalidateCache()} call) with the same <code>mainClassName</code>
     * and <code>source</code>.
     *
     * @throws CannotChangeSourceOfLoadedClassException  Thrown if this method has been
     *    already called with the same <code>mainClassName</code> but another <code>source</code>
     */
    public Class compileAndLoad(String source,
        String mainClassName,
        ClassLoader parentClassLoader, // parent class loader; can be null, ignored if !USE_DCO
        ClassCorrector classCorrector // can be null
    ) throws
        IOException,
        ToolNotFoundException,
        ToolInvalidCallException,
        ToolInternalException,
        CompilerErrorException,
        CannotFindCompiledClassException,
        CannotCorrectClassException
    {
        if (source == null) throw new NullPointerException("Null source argument in " + JavaCompiler.class.getName() + ".compileAndLoad method");
        if (mainClassName == null) throw new NullPointerException("Null mainClassName argument in " + JavaCompiler.class.getName() + ".compileAndLoad method");
        synchronized (lock) {
            // global static class data can be changed in this synchronized section
            CacheCompileAndLoad.ClassInfo classInfo = CacheCompileAndLoad.getClassInfo(mainClassName);
            if (classInfo != null) {
                if (!source.equals(classInfo.sourceMustNotVary))
                    throw new CannotChangeSourceOfLoadedClassException(mainClassName);
                return classInfo.clazz;
            }

            double t1 = Timing.timemsDouble();
            if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** Compiling new class " + mainClassName + " [" + sourceToReportPrivate(source) + "]");
            File tempJavaDir = Reflection.getTempClassesDirectory();
            File javaFile = tempJavaDir;
            javaFile.mkdir();
            StringTokenizer stringTokenizer = new StringTokenizer(mainClassName,".");
            String packageSubdir = null;
            for (boolean first = true; stringTokenizer.hasMoreTokens(); first = false) {
                packageSubdir = stringTokenizer.nextToken();
                javaFile = new File(javaFile,packageSubdir);
                javaFile.mkdir();
                if (first && Reflection.isTempClassesDirectoryPropertySet())
                    FileVisitor.removeOnShutdown(javaFile);
            }
            String mainContainerClassNameWithoutPackage = packageSubdir;
            int p = mainContainerClassNameWithoutPackage.indexOf("$");
            if (p != -1) mainContainerClassNameWithoutPackage = mainContainerClassNameWithoutPackage.substring(0,p);
            javaFile = new File(javaFile,mainContainerClassNameWithoutPackage + ".java");
            TextIO.ASCII.write(javaFile,Strings.encodeUnicodeEscapesSingle(source));
            double t2 = Timing.timemsDouble();

            JavaCompiler compiler = (JavaCompiler)this.clone();
            Set additionalClassPath = new LinkedHashSet();
            additionalClassPath.add(tempJavaDir.getPath());
            if (USE_DCO && parentClassLoader instanceof Reflection.WithClassPath)
                additionalClassPath.addAll(((Reflection.WithClassPath)parentClassLoader).classPath());
            Set newClassPath = new LinkedHashSet(compiler.compileClassPath);
            newClassPath.addAll(additionalClassPath);
            compiler.compileClassPath = newClassPath;
            compiler.compileDestinationDirectory = tempJavaDir.getPath();

            if (DEBUG_LEVEL >= 1) {
                Out.println(" **JavaCompiler** Saving new Java source code of " + mainClassName
                    + " (" + source.length() + " characters) in " + javaFile + ": " + Out.dec(t2-t1,2) + " ms");
                if (USE_DCO) Out.println(" **JavaCompiler** Parent class loader: " + parentClassLoader);
                Out.println(" **JavaCompiler** Additional class paths:\n    " + Out.join(additionalClassPath,File.pathSeparator+"\n    "));
                Out.print(" **JavaCompiler** Compiling new " + javaFile.getPath().substring(0,javaFile.getPath().length()-".java".length()) + ".class... ");
                Out.flush();
            }

            boolean compilationSuccessful = false;
            try {
                compiler.compileJavaFile(javaFile);
                compilationSuccessful = true;
            } finally {
                if (!compilationSuccessful && DEBUG_LEVEL >= 1)
                    Out.printlnFlush("\n **JavaCompiler** COMPILATION FAILED: an exception occurred");
            }

            double t3 = Timing.timemsDouble();
            if (DEBUG_LEVEL >= 1)
                Out.printlnFlush("done: " + Out.dec(t3-t2,2) + " ms");

            File classFile = new File(compiler.compileDestinationDirectory
                + File.separatorChar
                + mainClassName.replace('.',File.separatorChar)
                + ".class");
            if (!classFile.exists()) throw compiler.new CannotFindCompiledClassException(classFile);

            if (classCorrector != null) {
                byte[] content = BinaryIO.read(classFile);
                content = classCorrector.correct(content,classFile);
                BinaryIO.write(classFile,content);
            }

            double t4 = t3;
            ClassLoader classLoader = JavaCompiler.class.getClassLoader();
            if (USE_DCO) {
                classLoader = CacheCompileAndLoad.getClassLoader(parentClassLoader);
                if (classLoader == null) {
                    if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** Creating new DCO \"JavaCompiler.jcClassesLoader\": no custom CLASSPATH, parent \"" + parentClassLoader + "\"");
                    classLoader = new Reflection.DynamicClassOverloader(
                        Collections.singleton(tempJavaDir.getPath()),
                        Collections.EMPTY_SET,
                        "JavaCompiler.jcClassesLoader", parentClassLoader, false);
                }
                t4 = Timing.timemsDouble();
                if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** Preparing for loading class " + mainClassName + " by class loader \"" + classLoader + "\": " + Out.dec(t4-t1,2) + " ms");
            }
            Class clazz;
            try {
                clazz = Class.forName(mainClassName,true,classLoader);
            } catch (ClassNotFoundException e) {
                throw new InternalError("Internal error in " + JavaCompiler.class.getName() + ".compileAndLoad: cannot load class " + mainClassName + " that has been just compiled");
            }

            CacheCompileAndLoad.putClassInfo(mainClassName,source,clazz);
            if (USE_DCO) CacheCompileAndLoad.putClassLoader(parentClassLoader,classLoader);
            double t5 = Timing.timemsDouble();
            if (DEBUG_LEVEL >= 1) {
                Out.println(" **JavaCompiler** Loading class " + mainClassName + ": "+ Out.dec(t5-t4,2) + " ms");
                Out.println(" **JavaCompiler** Class and source are stored in cache. "
                    + "There are " + CacheCompileAndLoad.mainClassNameToClassInfo.size() + " stored classes and "
                    + CacheCompileAndLoad.parentClassLoaderToClassLoader.size() + " stored class loaders now"
                    + (USE_DCO? "": " (DCO IS NOT USED!)"));
            }
            if (DEBUG_LEVEL >= 2)
                Out.println(" **JavaCompiler** Stored classes:\n    " + Out.join(CacheCompileAndLoad.mainClassNameToClassInfo.keySet(),"\n    ")
                    +"\n **JavaCompiler** Stored class loaders:\n    " + Out.join(CacheCompileAndLoad.parentClassLoaderToClassLoader.keySet(),"\n    "));
            return clazz;
        }
    }

    public ClassLoader getClassLoaderAfterCompileAndLoad(ClassLoader parentClassLoader) {
        if (!USE_DCO) return JavaCompiler.class.getClassLoader();
        ClassLoader result = CacheCompileAndLoad.getClassLoader(parentClassLoader);
        if (result == null) throw new IllegalArgumentException(JavaCompiler.class.getName() + ".getClassLoaderAfterCompileAndLoad:"
            + " no class loaders found for the given parent class loader \"" + parentClassLoader
            + "\". Maybe, no classes were compiled and loaded yet by compileAndLoad method with this parent class loader");
        return result;
    }

    public static class EvalReturn {
        private static List values = new ArrayList();

        private String returnedClassJavaName;
        private EvalReturn(Class returnedClass) {
            this.returnedClassJavaName = JVM.toJavaName(returnedClass,true);
            values.add(this);
        }
        public String returnedClassJavaName() {
            return returnedClassJavaName;
        }
        public String toString() {
            return "eval returns " + this.returnedClassJavaName();
        }
        public Object eval(JavaCompiler javaCompiler, String javaExpression) throws Exception {
            return javaCompiler.evalPrivate(this,javaExpression,null,null);
        }
        public Object eval(JavaCompiler javaCompiler, String javaExpression, Object context) throws Exception {
            return javaCompiler.evalPrivate(this,javaExpression,context,null);
        }
        public Object eval(JavaCompiler javaCompiler, String javaExpression, Object context, String contextParameterName) throws Exception {
            return javaCompiler.evalPrivate(this,javaExpression,context,contextParameterName);
        }

        /* wrapJavaExpression must NOT add '\n' characters */
        String wrapJavaExpression(String javaExpression) {
            return returnedClassJavaName() + " returnValue= " + javaExpression
                + "; return returnValue";
        }

        public static final EvalReturn VOID    = new EvalReturn(void.class) {
            String wrapJavaExpression(String javaExpression) {
                return javaExpression;
            }
        };
        public static final EvalReturn BOOLEAN = new EvalReturn(boolean.class);
        public static final EvalReturn CHAR    = new EvalReturn(char.class);
        public static final EvalReturn BYTE    = new EvalReturn(byte.class);
        public static final EvalReturn SHORT   = new EvalReturn(short.class);
        public static final EvalReturn INT     = new EvalReturn(int.class);
        public static final EvalReturn LONG    = new EvalReturn(long.class);
        public static final EvalReturn FLOAT   = new EvalReturn(float.class);
        public static final EvalReturn DOUBLE  = new EvalReturn(double.class);
        public static final EvalReturn STRING  = new EvalReturn(String.class) {
            String wrapJavaExpression(String javaExpression) {
                return super.wrapJavaExpression("String.valueOf(" + javaExpression + ")");
            }
        };
        public static final EvalReturn OBJECT  = new EvalReturn(Object.class);

        public static final List VALUES = Collections.unmodifiableList(values);
    }

    public void evalVoid(String javaExpression) throws Exception {
        evalVoid(javaExpression,null);
    }
    public void evalVoid(String javaExpression, Object context) throws Exception {
        evalVoid(javaExpression,context,null);
    }
    public void evalVoid(String javaExpression, Object context, String contextParameterName) throws Exception {
        EvalReturn.VOID.eval(this,javaExpression,context,contextParameterName);
    }
    public int evalInt(String javaExpression) throws Exception {
        return evalInt(javaExpression,null);
    }
    public int evalInt(String javaExpression, Object context) throws Exception {
        return evalInt(javaExpression,context,null);
    }
    public int evalInt(String javaExpression, Object context, String contextParameterName) throws Exception {
        return ((Integer)EvalReturn.INT.eval(this,javaExpression,context,contextParameterName)).intValue();
    }
    public long evalLong(String javaExpression) throws Exception {
        return evalLong(javaExpression,null);
    }
    public long evalLong(String javaExpression, Object context) throws Exception {
        return evalLong(javaExpression,context,null);
    }
    public long evalLong(String javaExpression, Object context, String contextParameterName) throws Exception {
        return ((Long)EvalReturn.LONG.eval(this,javaExpression,context,contextParameterName)).longValue();
    }
    public float evalFloat(String javaExpression) throws Exception {
        return evalFloat(javaExpression,null);
    }
    public float evalFloat(String javaExpression, Object context) throws Exception {
        return evalFloat(javaExpression,context,null);
    }
    public float evalFloat(String javaExpression, Object context, String contextParameterName) throws Exception {
        return ((Float)EvalReturn.FLOAT.eval(this,javaExpression,context,contextParameterName)).floatValue();
    }
    public double evalDouble(String javaExpression) throws Exception {
        return evalDouble(javaExpression,null);
    }
    public double evalDouble(String javaExpression, Object context) throws Exception {
        return evalDouble(javaExpression,context,null);
    }
    public double evalDouble(String javaExpression, Object context, String contextParameterName) throws Exception {
        return ((Double)EvalReturn.DOUBLE.eval(this,javaExpression,context,contextParameterName)).doubleValue();
    }
    public String evalString(String javaExpression) throws Exception {
        return evalString(javaExpression,null);
    }
    public String evalString(String javaExpression, Object context) throws Exception {
        return evalString(javaExpression,context,null);
    }
    public String evalString(String javaExpression, Object context, String contextParameterName) throws Exception {
        return String.valueOf(EvalReturn.STRING.eval(this,javaExpression,context,contextParameterName));
    }
    public Object evalObject(String javaExpression) throws Exception {
        return evalObject(javaExpression,null);
    }
    public Object evalObject(String javaExpression, Object context) throws Exception {
        return evalObject(javaExpression,context,null);
    }
    public Object evalObject(String javaExpression, Object context, String contextParameterName) throws Exception {
        return EvalReturn.OBJECT.eval(this,javaExpression,context,contextParameterName);
    }

    private Object evalPrivate(EvalReturn evalReturn,
        String javaExpression,
        Object context,
        String contextParameterName)
    throws Exception
    // if contextParameterName==null, context.getClass() should be static class
    // that can be instantiated by context.getClass().newInstance()
    {
        if (javaExpression == null) throw new NullPointerException("Null Java expression in one of " + JavaCompiler.class.getName() + ".eval... methods");
        CacheEval.EvalPerformer performer;
        Object instanceLast;
        Object[] argumentsLast;
        boolean buildingNewClassesRequired = false;
        synchronized (lock) {
            performer =  CacheEval.performerLast;
            if (!((javaExpression == CacheEval.javaExpressionLast
                    || javaExpression.equals(CacheEval.javaExpressionLast))
                && (context == null? null: context.getClass()) == CacheEval.contextClassLast
                && (contextParameterName == CacheEval.contextParameterNameLast
                    || (contextParameterName != null && contextParameterName.equals(CacheEval.contextParameterNameLast)))
                && evalReturn == CacheEval.evalReturnLast))
            {
                Class contextClass = context == null? null: context.getClass();
                if (DEBUG_LEVEL >= 3) Out.print(" **JavaCompiler** eval... cannot be performed with maximal speed "
                    + "because arguments have been changed since last call:\n"
                    + (javaExpression.equals(CacheEval.javaExpressionLast)? "":
                        "    expression [" + sourceToReportPrivate(javaExpression) + "] instead [" + CacheEval.javaExpressionLast + "]\n")
                    + (contextClass == CacheEval.contextClassLast? "":
                        "    context class " + contextClass + " instead " + CacheEval.contextClassLast + "\n")
                    + (evalReturn == CacheEval.evalReturnLast? "":
                        "    " + evalReturn + " instead " + CacheEval.evalReturnLast + "\n"));
                String key = CacheEval.evalPerformerKey(
                    javaExpression,contextClass,contextParameterName,evalReturn);
                performer = CacheEval.getEvalPerformer(key);
                buildingNewClassesRequired = performer == null;
                if (buildingNewClassesRequired) {
                    if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** No performer found for new expression "
                        + sourceToReportPrivate(javaExpression) + " (with context class @" + Out.hex(System.identityHashCode(contextClass))
                        + ", parameter name " + contextParameterName + ", " + evalReturn + ")");
                    performer = getEvalPerformer(evalReturn,javaExpression,contextClass,contextParameterName);
                    CacheEval.putEvalPerformer(key,performer);
                    CacheEval.expressionCount++;
                }
                CacheEval.contextClassLast = contextClass;
                CacheEval.evalReturnLast = evalReturn;
            }
            CacheEval.javaExpressionLast = javaExpression;
            CacheEval.contextParameterNameLast = contextParameterName;
            // Last 2 assignments should be HERE: they should be performed even if
            // javaExpression.equals(CacheEval.javaExpressionLast)
            // && contextParameterName.equals(CacheEval.contextParameterNameLast)
            // (because in this case the INSTANCES of javaExpression and
            // contextParameterNameLast can be different)
            if (context != CacheEval.contextLast || performer != CacheEval.performerLast) {
                CacheEval.argumentsLast = context == null ? new Object[0] : new Object[]{context};
                CacheEval.instanceLast = performer.nestedClassConstructor.newInstance(new Object[]{
                    contextParameterName == null ? context : performer.containerClass.newInstance()});
                CacheEval.contextLast = context;
                CacheEval.performerLast = performer;
            }
            instanceLast = CacheEval.instanceLast;
            argumentsLast = CacheEval.argumentsLast;
        }
        Object result;
        try {
            result = performer.performMethod.invoke(instanceLast, argumentsLast);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof Exception) throw (Exception)target;
            if (target instanceof Error) throw (Error)target;
            throw e;
        }
        if (buildingNewClassesRequired && DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** " + CacheEval.instanceLast.getClass() + " performed successfully\n");
        return result;
    }

    private CacheEval.EvalPerformer getEvalPerformer(EvalReturn evalReturn,
        String javaExpression,
        Class contextClass,
        String contextParameterName)
    throws
        IOException,
        ToolNotFoundException,
        ToolInvalidCallException,
        ToolInternalException,
        CompilerErrorException,
        CannotFindCompiledClassException,
        CannotCorrectClassException
    {
        boolean cheatCompiler = contextClass != null && contextParameterName == null;
        ClassLoader contextClassLoader = contextClass == null? null: contextClass.getClassLoader();
        if (cheatCompiler) {
            try {
                contextClass.newInstance();
            } catch (InstantiationException e) {
                throw (IllegalArgumentException)new IllegalArgumentException("Illegal class \"" + contextClass
                    + "\" of the \"context\" argument in one of " + JavaCompiler.class.getName()
                    + ".eval... methods: this class cannot be instantiated."
                    + " To access such a context, non-null contextParameterName argument must be used").initCause(e);
            } catch (Throwable e) {
                throw (IllegalArgumentException)new IllegalArgumentException("An exception occurred while instantiating class \"" + contextClass
                    + "\" of the \"context\" argument in one of " + JavaCompiler.class.getName()
                    + ".eval... methods. Maybe, this class is a non-static local class."
                    + " To access such a context, non-null contextParameterName argument must be used").initCause(e);
            }
        }
        String packageNameWithDot = EVAL_PACKAGE == null? "": EVAL_PACKAGE + ".";
        String containerClassNameWithoutPackage = "___JCEExpression" + CacheEval.expressionCount;
        String containerClassName = packageNameWithDot + containerClassNameWithoutPackage;
        String nestedClassName = containerClassName + "$___Nested";
        String contextClassJavaName = contextClass == null? null: JVM.toJavaName(contextClass,true);

        StringBuffer sb = new StringBuffer(2048);
        if (EVAL_PACKAGE != null)
            sb.append("package ").append(EVAL_PACKAGE).append(";\n");
        if (contextClass != null)
            sb.append("import ").append(contextClassJavaName).append(";\n");
        sb.append(evalImports)
            .append("public class ").append(containerClassNameWithoutPackage);
        if (cheatCompiler)
            sb.append(" extends ").append(contextClassJavaName);
        sb.append(" {")
            .append("\n  public class ___Nested extends ").append(JavaCompiler.class.getName()).append(".MathFunctionsForFormulaInternal {")
            .append("\n    public ").append(evalReturn.returnedClassJavaName()).append(" ___performEval(");
        if (contextClass != null)
            sb.append(contextClassJavaName + " "
                + (contextParameterName == null? "_context": contextParameterName));
        sb.append(") {\n")
            .append("      ");
        int sbIndex = 0;
        int crBefore = 0;
        for (int len = sb.length(); sbIndex < len; sbIndex++)
            if (sb.charAt(sbIndex) == '\n') crBefore++;
        sb.append(evalReturn.wrapJavaExpression(javaExpression + "\n"));
            // "\n" is added to allow "//" comments in an expression
        int crUntilEnd = crBefore;
        for (int len = sb.length(); sbIndex < len; sbIndex++)
            if (sb.charAt(sbIndex) == '\n') crUntilEnd++;
        crUntilEnd--; // because "\n" has been added
        sb.append(";\n")
            .append("    }\n")
            .append("  }\n")
            .append("}\n");
        String source = sb.toString();
        if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** New Java source created for " + containerClassName + "; compiling and loading");
        ClassCorrector classCorrector = null;
        if (cheatCompiler)
            classCorrector = new CheatCompilerCorrector(containerClassName,contextClass.getName());

        Class nestedClass;
        try {
            nestedClass = compileAndLoad(source,nestedClassName,contextClassLoader,classCorrector);
        } catch (CompilerErrorException e) {
            Integer line = e.getLineIndex();
            if (line != null) {
                if (line.intValue()-1 >= crBefore && line.intValue()-1 <= crUntilEnd)
                    line = new Integer(line.intValue() - crBefore);
                else
                    line = null;
                e.setLineIndex(line);
            }
            throw e;
        }

        if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** Preparing for calling new class " + nestedClassName);
        ClassLoader classLoader = getClassLoaderAfterCompileAndLoad(contextClassLoader);
        Class containerClass;
        try {
            containerClass = Class.forName(containerClassName,true,classLoader);
        } catch (ClassNotFoundException e) {
            throw new InternalError("Internal error in " + JavaCompiler.class.getName() + ".eval...: cannot load class " + containerClassName + " that has been just compiled");
        }
        Constructor[] constructors = nestedClass.getConstructors();
        Constructor nestedClassConstructor = null;
        for (int k = 0; k < constructors.length; k++) {
            if (constructors[k].getParameterTypes().length==1) {
                nestedClassConstructor = constructors[k]; break;
            }
        }
        if (nestedClassConstructor == null)
            throw new InternalError("Internal error in " + JavaCompiler.class.getName() + ".eval...: cannot find constructor for nested class " + nestedClassName);
        Method performMethod;
        try {
            performMethod = nestedClass.getMethod("___performEval",
                contextClass == null? new Class[0]: new Class[] {contextClass});
        } catch (NoSuchMethodException e) {
            throw new InternalError("Internal error in " + JavaCompiler.class.getName() + ".eval...: cannot find ___performEval method in nested class " + nestedClassName);
        }
        return new CacheEval.EvalPerformer(containerClass,nestedClassConstructor,nestedClass,performMethod);
    }

    /**
     * Enforces <code>JavaCompiler</code> class to "forget" all classes and class loaders
     * stored in the internal cache. May be called when some of your classes
     * or class loaders that probably were passed as arguments to <code>JavaCompiler</code>
     * methods, or the loaders of some that classes, become obsolete and can be
     * released to free memory.
     *
     * <p>This method must not be called if {@link #USE_DCO} propetry is <code>false</code>.
     *
     * <p>This method is never called internally.
     *
     * @throws IllegalStateException  Thrown if {@link #USE_DCO} contains <code>false</code>
     */
    public static void invalidateCache() {
        if (!USE_DCO) throw new IllegalStateException(JavaCompiler.class.getName() + " internal caches must be never invalidated if DCO is not used");
        if (DEBUG_LEVEL >= 1) Out.println(" **JavaCompiler** INVALIDATING CACHE");
        CacheCompileAndLoad.invalidate();
        CacheEval.invalidate();
        invalidationCount++;
    }
    public static long invalidationCount() {
        return invalidationCount;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw (InternalError)new InternalError(e.toString()).initCause(e);
        }
    }

    private static final Object lock = new Object();
    private static long invalidationCount = 0;
    private static class CacheCompileAndLoad {
    // Will be new for each new class loader that loads THIS class
        static class ClassInfo {
            final String sourceMustNotVary;
            final Class clazz;
            ClassInfo(String source, Class clazz) {
                this.sourceMustNotVary = source;
                this.clazz = clazz;
            }
        }

        private static Map mainClassNameToClassInfo;
        private static Map parentClassLoaderToClassLoader;
        static void invalidate() {
            mainClassNameToClassInfo = new HashMap();
            parentClassLoaderToClassLoader = new HashMap();
        }
        static {
            invalidate();
        }

        static ClassInfo getClassInfo(String mainClassName) {
            return (ClassInfo)mainClassNameToClassInfo.get(mainClassName);
        }
        static void putClassInfo(String mainClassName, String source, Class clazz) {
            mainClassNameToClassInfo.put(mainClassName,new ClassInfo(source,clazz));
        }

        static ClassLoader getClassLoader(ClassLoader parentClassLoader) {
            return (ClassLoader)parentClassLoaderToClassLoader.get(System.identityHashCode(parentClassLoader)+" "+parentClassLoader);
        }
        static void putClassLoader(ClassLoader parentClassLoader, ClassLoader classLoader) {
            parentClassLoaderToClassLoader.put(System.identityHashCode(parentClassLoader)+" "+parentClassLoader,classLoader);
        }
    }

    private static class CacheEval {
        static class EvalPerformer {
            final Class containerClass;
            final Constructor nestedClassConstructor;
            final Class nestedClass;
            final Method performMethod;
            EvalPerformer(Class containerClass,
                Constructor nestedClassConstructor,
                Class nestedClass,
                Method performMethod)
            {
                this.containerClass = containerClass;
                this.nestedClassConstructor = nestedClassConstructor;
                this.nestedClass = nestedClass;
                this.performMethod = performMethod;
            }
        }

        static long expressionCount;
        private static Map javaExpressionToEvalPerformer;

        static String javaExpressionLast;
        static Class contextClassLast;
        static String contextParameterNameLast;
        static EvalReturn evalReturnLast;

        static Object[] argumentsLast;
        static Object instanceLast;
        static Object contextLast;
        static EvalPerformer performerLast;
        static void invalidate() {
            expressionCount = 1;
            javaExpressionToEvalPerformer = new HashMap();
            javaExpressionLast = null;
            contextClassLast = null;
            contextParameterNameLast = null;
            evalReturnLast = null;
            performerLast = null;
            contextLast = null;
            instanceLast = null;
            argumentsLast = new Object[0];
        }
        static {
            invalidate();
        }
        static String evalPerformerKey(
            String javaExpression,
            Class contextClass,
            String contextParameterName,
            JavaCompiler.EvalReturn evalReturn)
        {
            return javaExpression + "\n"
                + Integer.toHexString(System.identityHashCode(contextClass))
                    // identityHashCode: contextClass versions loaded by different class loaders may have identical toString() representation
                + contextParameterName + "\n"
                + evalReturn.returnedClassJavaName;
        }

        static EvalPerformer getEvalPerformer(String key) {
            return (EvalPerformer)javaExpressionToEvalPerformer.get(key);
        }
        static void putEvalPerformer(String key, EvalPerformer value) {
         javaExpressionToEvalPerformer.put(key,value);
        }
    }

    private static class CheatCompilerCorrector implements ClassCorrector {
        private String containerClassName, contextClassName;
        CheatCompilerCorrector(String containerClassName, String contextClassName) {
            this.containerClassName = containerClassName;
            this.contextClassName = contextClassName;
        }
        private byte[] toJvmConst(String s) {
            try {
                byte[] sUtf8 = s.getBytes("UTF-8");
                return Arrays.append(new byte[] {1,0,(byte)sUtf8.length}, sUtf8);
            } catch (UnsupportedEncodingException e) {
                throw (InternalError)new InternalError(e.toString()).initCause(e);
            }
        }
        public byte[] correct(byte[] contentOfClassFile, File classFile) throws CannotCorrectClassException {
            String containerClassNameJVM = containerClassName.replace('.','/');
            String contextClassNameJVM = contextClassName.replace('.','/');
            String containerJavaTypeName = "L" + containerClassNameJVM + ";";
            String contextJavaTypeName = "L" + contextClassNameJVM + ";";
            String containerMethodSignature = "(" + containerJavaTypeName +")V";
            String contextMethodSignature = "(" + contextJavaTypeName +")V";
            byte[] content = contentOfClassFile;
            Arrays.Replacer r = new Arrays.Replacer(content);
            r.replace(
                toJvmConst(containerClassNameJVM),
                toJvmConst(contextClassNameJVM));
            int classNameCount = r.count();
            r.replace(
                toJvmConst(containerJavaTypeName),
                toJvmConst(contextJavaTypeName));
            int javaTypeCount = r.count();
            r.replace(
                toJvmConst(containerMethodSignature),
                toJvmConst(contextMethodSignature));
            int signatureCount = r.count();
            if (DEBUG_LEVEL >= 1) {
                try {
                    String s = classFile.getPath();
                    s = s.substring(0,s.length()-".class".length());
                    BinaryIO.write(new File(s + ".original.class"),(byte[])content);
                    TextIO.SYSTEM_DEFAULT.write(new File(s + ".report.txt"),
                        Out.replaceLFToLineSeparator(
                            "Class-file " + classFile + "\nhas been corrected by the following replacements:\n"
                            + "  " + classNameCount + " change" + (classNameCount > 1? "s": "") + " \"" + containerClassNameJVM    + "\" --> \"" + contextClassNameJVM    + "\"\n"
                            + "  " + javaTypeCount  + " change" + (javaTypeCount  > 1? "s": "") + " \"" + containerJavaTypeName    + "\" --> \"" + contextJavaTypeName    + "\"\n"
                            + "  " + signatureCount + " change" + (signatureCount > 1? "s": "") + " \"" + containerMethodSignature + "\" --> \"" + contextMethodSignature + "\"\n"
                        )
                    );
                } catch (IOException e) {
                    Out.println("Warning: " + JavaCompiler.class.getName() + " cannot save debug info.\nSystem message: " + e);
                }
            }
            if (javaTypeCount < 1 || signatureCount < 1)
                throw new CannotCorrectClassException();
            return (byte[])r.result();
        }
    }

    /**
     * This class is designed for internal use by <code>evalXxx</code> methods only.
     * Please don't use it.
     */
    public static class MathFunctionsForFormulaInternal extends Ma {
        // Let temporaty classes for evaluting expressions to extend this class
        // to get direct access for all Math static methods (this pattern
        // is usually bad, but suitable for formulas)
        protected MathFunctionsForFormulaInternal() {}
    }

    private static String sourceToReportPrivate(String s) {
        s = s.replaceAll("[\\n\\r\\t]"," ");
        int len = s.length();
        if (len > 50)
            s = s.substring(0,20) + " ... " + s.substring(len-25);
        return "\"" + s +"\" (" + len + " chars)";
    }

    public static void main(String[] argv) throws JavaCompiler.LocalStaticException {
        if (argv.length == 0) {
            Out.println("Usage: JavaCompiler [-0|-1] [options] sourcefile1.java sourcefile2.java ...\n"
                + "(almost the same syntax as for \"javac\")\n"
                + "-0 or -1 chooses used compiler:\n"
                + "  -0 (default) for " + JavaCompiler.UsedTool.COM_SUN_TOOLS_JAVAC + ",\n"
                + "  -1 for " + JavaCompiler.UsedTool.SUN_TOOLS_JAVAC + ".\n"
                + "Important: sourcefileN must be the last arguments.");
            return;
        }
        JavaCompiler.UsedTool usedTool = null;
        if (argv[0].equals("-0")) usedTool = JavaCompiler.UsedTool.COM_SUN_TOOLS_JAVAC;
        else if (argv[0].equals("-1")) usedTool = JavaCompiler.UsedTool.SUN_TOOLS_JAVAC;
        if (usedTool != null)
            argv= (String[])Arrays.deleteOne(argv,0);

        int n = argv.length;
        while (n > 0 && argv[n-1].toLowerCase().endsWith(".java")) n--;
        if (n == argv.length) argv = (String[])Arrays.append(argv,new String[]{""});

        JavaCompiler compiler = new JavaCompiler(usedTool);
        String[] additional = (String[])Arrays.copy(argv,0,n);
        compiler.setCompileAdditionalOptions(additional);

        for (; n < argv.length; n++) {
            Out.print("Compiling: [" + compiler.getUsedTool() + "] "
                + Out.join(additional," ") + " " + argv[n] + " ...");
            long t1 = Timing.timemcs();
            try {
                JavaCompiler.CompilationResults results = compiler.compileJavaFile(new File(argv[n]));
                Out.print(
                    (results.compilerOutput().length() == 0? " ":
                        "\nCompiler report:\n<<\n" + results.compilerOutput() + ">>\n")
                    +(results.exitStatus() == 0? "":
                        "Compiler exit status: " + results.exitStatus() + "\n"));
            } catch (Exception e) {
                Out.println();
                e.printStackTrace();
            }
            long t2 = Timing.timemcs();
            Out.println(Out.dec((t2-t1)/1000.0,2) + " ms\n");
        }
    }
}