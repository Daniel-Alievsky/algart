/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * <p>A tool helping to call external programs (OS processes) for processing AlgART arrays and matrices.</p>
 *
 * <p>This class helps to solve two following main tasks.</p>
 *
 * <ol>
 * <li>Creation of some disk directory (<i>work directory</i>) for exchanging data with the called
 * external program. This class provides automatic creation of the work directory
 * (in {@link #getInstance(ArrayContext, String, String)} / {@link #getInstance(ArrayContext)} methods),
 * automatic deletion of it after finishing
 * usage of an external program (in {@link #close()} method and in the {@link #finalize() finalizer})
 * and the special {@link #cleanup(String)} method for removing all work directories,
 * which were not successfully deleted (usually due to file mapping or abnormal JVM termination)
 * while previous usages of this class or while previous calls of your application.
 * <br>&nbsp;</li>
 *
 * <li>Support of the standard AlgART logic of interrupting the algorithm via
 * {@link ArrayContext#checkInterruption()} method while execution of an external program:
 * the basic {@link #execute(ProcessBuilder)} method periodically calls {@link ArrayContext#checkInterruption()
 * checkInterruption()}, and if it throws an exception (i.e. the user wants to stop calculations),
 * automatically terminates the external process via <tt>java.lang.Process.destroy()</tt> call.</li>
 * </ol>
 *
 * <p>This class also contains a collection of static method, useful while implementing inter-application
 * communications.</p>
 *
 * <p>The instances of this class are <b>thread-safe</b>: all non-static methods are internally synchronized
 * (besides {@link #context()} and {@link #getWorkDirectory()}, that always return the same values and don't
 * require synchronization, and the service methods {@link #getWorkFile(String)},
 * {@link #writeWorkUTF8(String, String)}, {@link #readWorkUTF8(String)}).
 * So, you may use the same instance of this class in several threads; but usually there is not sense to do it.</p>
 *
 * <p>Unlike this, the static methods of this class (like {@link #cleanup(String)}) are not thread-safe,
 * but <b>are thread-compatible</b> and can be synchronized manually.</p>
 *
 * @author Daniel Alievsky
 */
public class ExternalProcessor implements ArrayProcessor, Closeable {
    /**
     * The name of default temporary directory for creating work directories by instances of this class.
     * Such subdirectory is created by {@link #getInstance(ArrayContext)} method in
     * the standard system temporary directory <tt>System.getProperty("java.io.tmpdir")</tt>.
     *
     * <p>The value of this constant is {@value}.
     *
     * @see #getDefaultTempDirectory()
     */
    public static final String TEMP_SUBDIRECTORY_DEFAULT_NAME = "algart__ep";
    /**
     * The prefix of unique <i>work directories</i>, created while instantiation of this class.
     * See {@link #getInstance(ArrayContext, String, String)} method about "work directory" concept.
     *
     * <p>The value of this constant is {@value}.
     *
     * <p>Note: if you are planning to use {@link #cleanup(String)} method, <b>you must be sure</b>
     * that your temporary directory, passed to that method, never contains files or directories with names,
     * started with this prefix, besides work directories created by this class.
     * The reason is that {@link #cleanup(String)} method automatically tries to recursively
     * remove all such subdirectories.
     * The simplest way to provide this condition is usage of the default temporary directory
     * {@link #getDefaultTempDirectory()}, for example, via {@link #cleanup()} method without arguments:
     * this directory is created by this class itself in the system directory
     * <tt>System.getProperty("java.io.tmpdir")</tt> and, so, never contain extra files/subdirectories.
     *
     * @see #getInstance(ArrayContext, String, String)
     */
    public static final String WORK_DIRECTORY_PREFIX = "algart__ep_";
    /**
     * The name of a temporary file-marker, created inside <i>work directories</i> while instantiation of this class
     * for internal use (see {@link #cleanup(String)} method for more details).
     * See {@link #getInstance(ArrayContext, String, String)} method about "work directory" concept.
     *
     * <p>The value of this constant is {@value}.
     *
     * <p>Note: <b>you must guarantee</b> that you will not try to create or write into a file
     * with this name in the work directory.
     *
     * @see #cleanup()
     */
    public static final String USAGE_MARKER_FILE_NAME = ".algart__ep_used";

    /**
     * The name of system property (<tt>{@value}</tt>),
     * used for finding some custom JRE by {@link #getCustomJREHome()} method.
     */
    public static final String JRE_PATH_PROPERTY_NAME = "net.algart.arrays.jre.path";

    /**
     * The name of environment variable (<tt>{@value}</tt>),
     * used for finding some custom JRE by {@link #getCustomJREHome()} method.
     */
    public static final String JRE_PATH_ENV_NAME = "NET_ALGART_ARRAYS_JRE_PATH";

    /**
     * The name of system property (<tt>{@value}</tt>),
     * used to get a list of custom JVM options by {@link #getCustomJVMOptions()} method.
     */
    public static final String JVM_OPTIONS_PROPERTY_NAME = "net.algart.arrays.jvm.options";

    /**
     * The name of environment variable (<tt>{@value}</tt>),
     * used to get a list of custom JVM options by {@link #getCustomJVMOptions()} method.
     */
    public static final String JVM_OPTIONS_ENV_NAME = "NET_ALGART_ARRAYS_JVM_OPTIONS";

    private static enum State {
        ACTIVE,
        CLOSED,
        CLOSED_SUCCESSFULLY
    }

    private static final Logger LOGGER = Logger.getLogger(ExternalProcessor.class.getName());
    private static final int TEMP_DIR_NUMBER_OF_ATTEMPTS = 1000;
    private static final Object TEMP_DIR_LOCK = new Object();
    private static long tempDirCounter = -1; // synchronized by TMP_DIR_LOCK
    private static final int CLEANUP_GC_TIMEOUT = 1000; // 1 second
    private static final int CLEANUP_MAX_NUMBER_OF_GC = 10;
    private static final int ADDITIONAL_WAIT_FOR_TERMINATION_TIMEOUT = 1000; // 1 second

    private final ArrayContext context;

    private final File workDirectory;
    private final File marker;
    private final RandomAccessFile markerFile;
    private final FileChannel markerChannel;
    private final FileLock markerLock;

    private OutputStream outputStream = null;
    private OutputStream errorStream = null;

    private final Object lock = new Object();
    private volatile State state = null;
    private volatile boolean removingCancelled = false;

    private ExternalProcessor(ArrayContext context, String tempDirectory, String additionalPrefix) {
        this.context = context;
        if (tempDirectory == null)
            throw new NullPointerException("Null tempDirectory argument");
        if (additionalPrefix == null)
            throw new NullPointerException("Null additionalPrefix argument");
        File tempDir = new File(tempDirectory);
        if (tempDirectory.equals(getDefaultTempDirectory())) {
            if (!tempDir.mkdir()) {
                if (!tempDir.isDirectory()) // i.e. if doesn't really exist
                    throw IOErrorJ5.getInstance(new IOException("Cannot create temporary directory " + tempDir));
                // Important note: we must attempt to create tempDir BEFORE checking its existence;
                // in other case, using this class from parallel threads can lead to attempt to create
                // this directory twice and, so, to illegal messages about "errors" while creation
            }
        } else if (!tempDir.exists())
            throw IOErrorJ5.getInstance(new IOException("The specified temporary directory "
                + tempDir + " does not exist"));
        try {
            this.workDirectory = createTempDirectory(
                tempDir, WORK_DIRECTORY_PREFIX + additionalPrefix).getAbsoluteFile();
        } catch (IOException e) {
            throw IOErrorJ5.getInstance(e);
        }
        try {
            this.marker = new File(workDirectory, USAGE_MARKER_FILE_NAME);
            this.markerFile = new RandomAccessFile(marker, "rw");
            this.markerChannel = markerFile.getChannel();
            this.markerLock = this.markerChannel.lock();
        } catch (Throwable e) {
            close(); // remove all previously created temporary directories
            if (e instanceof IOException) {
                throw IOErrorJ5.getInstance(e);
            }
            // Arrays.throwUncheckedException(e) is bad here: the compiler requires initialization of all final fields
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new AssertionError("Impossible exception: " + e);
        }
        this.state = State.ACTIVE;
    }

    /**
     * Creates new instance of this class.
     *
     * <p>The newly created instance allows to execute, one or several times,
     * some external OS command, usually another command-line application, via the call of
     * {@link #execute(ProcessBuilder)} method.
     * Before finishing usage of this instance, you must call {@link #close()} method;
     * after this, you cannot use this object.
     *
     * <p>This method automatically creates an unique temporary disk subdirectory,
     * called <i>the work directory</i>, inside <tt>tempDirectory</tt>,
     * which is supposed to be some global disk directory for storing temporary data.
     * Usually you may pass the result of {@link #getDefaultTempDirectory()} method here
     * (or use the alternative method {@link #getInstance(ArrayContext)}).
     *
     * <p>The created work directory can be used for exchanging data with the external application,
     * called by {@link #execute(ProcessBuilder)} method.
     * The method {@link #close()}, as well as {@link #finalize()}, tries to completely remove this directory
     * with all its files and subdirectories (you may {@link #cancelRemovingWorkDirectory() cancel}
     * this action). You should understand, that it is possible
     * that neither {@link #close()}, nor even {@link #finalize()} method will not be able to remove it,
     * because some temporary files can be locked by OS &mdash; for example, if you map them via
     * {@link LargeMemoryModel}. You can use {@link #cleanup(String)} method at any time
     * to remove all "garbage", i.e. work directories, created by this class (maybe while previous calls
     * of your application), but not successfully deleted by {@link #close()} or {@link #finalize()} methods.
     *
     * <p>The created unique work directory always has a name, started with the character sequence<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<tt>{@link #WORK_DIRECTORY_PREFIX} + additionalPrefix</tt>.<br>
     * Here {@link #WORK_DIRECTORY_PREFIX} allows to detect subdirectories, created by this class,
     * and <tt>additionalPrefix</tt> allows you (if you want) to distinguish subdirectories, created by
     * different instance of this class for different goals.
     * You must take this into consideration, when you specify some custom <tt>tempDirectory</tt>
     * (different from {@link #getDefaultTempDirectory()}). In this case, you should either be sure
     * that {@link #WORK_DIRECTORY_PREFIX} prefix cannot appear in other file/subdirectory names
     * in this directory, or never use {@link #cleanup(String)} method &mdash; because it will try to remove
     * all subdirectories with names, started with {@link #WORK_DIRECTORY_PREFIX}.
     *
     * <p>Important note: this method tries to create the parent directory <tt>tempDirectory</tt>
     * <b>if and only if</b> it is identical (in terms of <tt>String.equals()</tt>)
     * to the result of {@link #getDefaultTempDirectory()} method.
     * In other case, the passed <tt>tempDirectory</tt> must already exist:
     * <tt><nobr>new File(tempDirectory).exists()</nobr></tt> must return <tt>true</tt>.
     *
     * <p>Note: you can use these features, even if you are not going to call external commands
     * via {@link #execute(ProcessBuilder)} method.
     *
     * @param context          the context of execution; may be <tt>null</tt>, then will be ignored.
     *                         In the current implementations, it is used only to allow the user to stop
     *                         the running external application: see {@link #context()} method.
     * @param tempDirectory    the path to the global disk directory for allocating temporary data;
     *                         in most cases you may pass here the result of {@link #getDefaultTempDirectory()}.
     * @param additionalPrefix some string, added to {@link #WORK_DIRECTORY_PREFIX} to provide unique prefix
     *                         for the the name of the work directory, created by this class;
     *                         you may pass <tt>""</tt>, if you are not interested in it.
     * @return new instance of this class.
     * @throws NullPointerException if <tt>tempDirectory</tt> or <tt>additionalPrefix</tt> argument is <tt>null</tt>.
     * @throws SecurityException    if <tt>System.getProperty("java.io.tmpdir")</tt> call throws this exception.
     * @throws java.io.IOError      if this method cannot create an unique subdirectory due to some reasons,
     *                              in particular, if <tt>!tempDirectory.equals({@link
     *                              #getDefaultTempDirectory()})</tt> and the corresponding path does not exist.
     * @see #getInstance(ArrayContext)
     * @see #getWorkDirectory()
     */
    public static ExternalProcessor getInstance(
        ArrayContext context,
        String tempDirectory,
        String additionalPrefix)
    {
        return new ExternalProcessor(context, tempDirectory, additionalPrefix);
    }

    /**
     * Equivalent to <tt>{@link #getInstance(ArrayContext, String, String)
     * getInstance}(context, {@link #getDefaultTempDirectory()}, "")</tt>.
     *
     * @param context the context of execution; may be <tt>null</tt>, then will be ignored.
     *                In the current implementations, it is used only to allow the user to stop
     *                the running external application: see {@link #context()} method.
     * @return new instance of this class.
     * @throws SecurityException if <tt>System.getProperty("java.io.tmpdir")</tt> call throws this exception.
     * @throws java.io.IOError   if this method cannot create an unique subdirectory due to some reasons,
     *                           in particular, if <tt>!tempDirectory.equals({@link
     *                           #getDefaultTempDirectory()})</tt> and the corresponding path does not exist.
     * @see #getWorkDirectory()
     */
    public static ExternalProcessor getInstance(ArrayContext context) {
        return new ExternalProcessor(context, getDefaultTempDirectory(), "");
    }

    /**
     * Tries to remove all work directories (recursively, with all their content),
     * which were created by some instances of this class, instantiated by
     * {@link #getInstance(ArrayContext, String, String)}
     * method with the same <tt>tempDirectory</tt> argument, and which were not successfully deleted yet.
     *
     * <p>Such situation is possible, for example, when some temporary files, used for exchanging data
     * with an external program, are locked by OS, and we cannot unlock them immediately after termination
     * of the external program. It can be connected with file mapping (in particular, mapping
     * via {@link LargeMemoryModel}, which is unmapped in Java while garbage collection only).
     * Another typical reasons of appearing non-deleted work directories is canceling automatic deletion
     * of work data for some instance of this class by {@link #cancelRemovingWorkDirectory()} method
     * or abnormal termination of your application,
     * when neither {@link #close()}, nor {@link #finalize()} were not called for some instance of this class.
     *
     * <p>The cleanup technique, implemented in this method, is based on <tt>java.nio.channels.FileLock</tt> class.
     * When an instance of this class is created, it places into its {@link #getWorkDirectory() work directory}
     * an empty file with some reserved name {@link #USAGE_MARKER_FILE_NAME} and locks it via <tt>FileLock</tt>.
     * When an instance finishes working, i.e. when its {@link #close()} method or the {@link #finalize() finalizer}
     * is called, it unlocks this file and removes it.
     * It is obvious that all locked files are unlocked when the application is terminated, normally or abnormally.
     * So, if <tt>tempDirectory</tt> contains some subdirectory, the name of which is started
     * with {@link #WORK_DIRECTORY_PREFIX} and which does not contain a file with the name {@link #USAGE_MARKER_FILE_NAME}
     * or contains such a file, but it is not locked, it means that this subdirectory is a "garbage" and
     * can be removed. This method tries to remove all such subdirectories and returns <tt>true</tt>,
     * if it has successfully removed all content of <tt>tempDirectory</tt>.
     *
     * <p>Note: if this method was not able to remove some work directory (a subdirectory with name,
     * started with {@link #WORK_DIRECTORY_PREFIX}), and this directory is not locked
     * via {@link #USAGE_MARKER_FILE_NAME} file (i.e. it is not in use now), this method performs
     * several attempts to remove the work directory, calling <tt>System.gc()</tt> between attempts.
     * Usually it helps to release resources (like file mappings), which prevents from removing
     * such unused subdirectories.
     *
     * <p>Note: this method does not try to remove the directory <tt>tempDirectory</tt> itself.
     *
     * <p>It is a good idea to call this method while starting your application and, maybe, after
     * calling long-running external applications via {@link #execute(ProcessBuilder)} method,
     * to remove all "garbage" that was not deleted by normal calls
     * of {@link #close()} method in this or previous calls of your application.
     *
     * @param tempDirectory the path to the global disk directory for allocating temporary data;
     *                      should be identical to the same argument of
     *                      {@link #getInstance(ArrayContext, String, String)}
     *                      method, which you use for instantiation this object
     *                      (or you may use {@link #cleanup()} instead of this method, if you use
     *                      {@link #getInstance(ArrayContext)} method without the second argument).
     * @return <tt>true</tt> if and only this method has successfully removed all subdirectories
     *         of the specified directory <tt>tempDirectory</tt> with names,
     *         started with {@link #WORK_DIRECTORY_PREFIX}.
     * @throws NullPointerException if <tt>tempDirectory</tt> argument is <tt>null</tt>.
     * @throws SecurityException    if <tt>System.getProperty("java.io.tmpdir")</tt> call throws this exception.
     * @see #cleanup()
     */
    public static boolean cleanup(String tempDirectory) {
        if (tempDirectory == null)
            throw new NullPointerException("Null tempDirectory argument");
        final File tempDir = new File(tempDirectory);
        if (!tempDir.exists()) {
            return true;
        }
        File[] workDirs = tempDir.listFiles();
        // boolean allTempContentDeleted = true; - deprecated
        boolean allOk = true;
        if (workDirs != null) {
            for (File workDir : workDirs) {
                if (workDir.isFile()) {
                    // Note: we must not check "!isDirectory()", because it could be removed
                    // as a result of some finalization methods or due to other parallel threads
                    // allTempContentDeleted = false; - deprecated
                    continue;
                }
                if (!workDir.getName().startsWith(WORK_DIRECTORY_PREFIX)) {
                    // allTempContentDeleted = false; - deprecated
                    continue;
                }
                File marker = new File(workDir, USAGE_MARKER_FILE_NAME);
                boolean inUse = marker.exists();
                if (inUse) {
                    RandomAccessFile markerFile = null;
                    FileChannel markerChannel = null;
                    FileLock markerLock = null;
                    try {
                        try {
                            markerFile = new RandomAccessFile(marker, "rw");
                            markerChannel = markerFile.getChannel();
                            markerLock = markerChannel.tryLock();
                            inUse = markerLock == null;
                            if (inUse) {
                                LOGGER.config("EP cleanup: the marker " + marker + " is locked in another JVM");
                            } else {
                                LOGGER.config("EP cleanup: " + workDir + " was probably not deleted due to "
                                    + "previous system crash! It is deleted now");
                            }
                        } finally {
                            if (markerLock != null) {
                                markerLock.release();
                            }
                            if (markerChannel != null) {
                                markerChannel.close();
                            }
                            if (markerFile != null) {
                                markerFile.close();
                            }
                        }
                    } catch (OverlappingFileLockException e) {
                        LOGGER.config("EP cleanup: the marker " + marker + " is locked in this JVM");
                    } catch (IOException e) {
                        LOGGER.warning("EP cleanup: cannot check the lock of the marker " + marker + ": " + e);
                    } catch (RuntimeException e) {
                        LOGGER.warning("EP cleanup: exception while checking the lock of the " + marker + ": " + e);
                    }
                }
                if (!inUse) {
                    if (deleteRecursive(workDir, null)) {
                        LOGGER.config("EP cleanup: " + workDir + " deleted");
                    } else {
                        // something already is not in use, but cannot be deleted:
                        // probably it is connected with non-utilized garbage (like file mappings)
                        boolean deletedAfterGc = false;
                        long time = System.currentTimeMillis();
                        int numberOfGc = 1;
                        for (; numberOfGc <= CLEANUP_MAX_NUMBER_OF_GC; numberOfGc++) {
                            System.gc();
                            try {
                                Thread.sleep(150); // to be on the safe side, sleeping as in java.nio.Bits
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // restore interrupt status
                            }
                            deletedAfterGc = deleteRecursive(workDir, null);
                            if (deletedAfterGc) {
                                break;
                            }
                            if (System.currentTimeMillis() - time >= CLEANUP_GC_TIMEOUT) {
                                break;
                            }
                        }
                        if (deletedAfterGc) {
                            LOGGER.config("EP cleanup: " + workDir + " deleted after " + numberOfGc + " gc() calls");
                        } else {
                            allOk = false;
                            // allTempContentDeleted = false; - deprecated
                            LOGGER.warning("EP cleanup: cannot delete " + workDir
                                + " after " + numberOfGc + " gc() calls");
                            if (!workDir.exists()) {
                                LOGGER.warning("...however " + workDir + " is already deleted!");
                            }
                        }
                    }
                    // } else {
                    // allTempContentDeleted = false; - deprecated
                }
            }
        }
        /*
        // Removing global tempDirectory below is very bad idea:
        // see the analogous comment in close() method.
        if (allTempContentDeleted && tempDirectory.equals(getDefaultTempDirectory())) {
            if (!deleteIfExists(tempDir)) {
                allOk = false;
                LOGGER.warning("EP cleanup: cannot delete the global temporary directory " + tempDir);
            } else {
                LOGGER.config("EP cleanup: the global temporary directory " + tempDir + " deleted");
            }
        }
        */
        return allOk;
    }

    /**
     * Equivalent to <tt>{@link #cleanup(String) cleanup}({@link #getDefaultTempDirectory()})</tt>.
     * If you use {@link #getInstance(ArrayContext)} method only and don't use
     * {@link #getInstance(ArrayContext, String, String)} with a custom second argument,
     * you can use this method instead of {@link #cleanup(String)}.
     *
     * @return <tt>true</tt> if and only this method has successfully removed all subdirectories
     *         of {@link #getDefaultTempDirectory()} with names, started with {@link #WORK_DIRECTORY_PREFIX}.
     * @throws SecurityException if <tt>System.getProperty("java.io.tmpdir")</tt> call throws this exception.
     */
    public static boolean cleanup() {
        return cleanup(getDefaultTempDirectory());
    }

    /**
     * Returns the path to the default disk directory for storing temporary data: some "standard" subdirectory
     * of the system temporary directory will be used.
     * Namely, this method returns
     * <pre>
     * new File(System.getProperty("java.io.tmpdir"), {@link #TEMP_SUBDIRECTORY_DEFAULT_NAME}).getPath()
     * </pre>
     *
     * <p>Note: this subdirectory is automatically created in {@link #getInstance(ArrayContext)} method
     * or in {@link #getInstance(ArrayContext, String, String)} method, when its second argument is identical to
     * the result of this method. However, <b>this class never tries to remove this subdirectory</b>:
     * such an attempt could lead to errors if another application simultaneously tries to create this
     * subdirectory. You may remove this subdirectory manually, for example, in the uninstaller of your software.
     *
     * @return the default disk directory for storing temporary data.
     * @throws SecurityException if <tt>System.getProperty("java.io.tmpdir")</tt> call throws this exception.
     * @see #getInstance(ArrayContext, String, String)
     * @see #cleanup(String)
     */
    public static String getDefaultTempDirectory() {
        String systemTempDirectory = System.getProperty("java.io.tmpdir");
        if (systemTempDirectory == null)
            throw new AssertionError("Strange JVM problem: java.io.tmpdir system property is not set");
        return new File(systemTempDirectory, TEMP_SUBDIRECTORY_DEFAULT_NAME).getPath();
    }

    /**
     * Returns the system property indicated by the specified key <tt>propertyKey</tt>, if it exists,
     * or, if it is <tt>null</tt>, the environment variable indicated by the specified name <tt>envVarName</tt>.
     * Equivalent to the following operator:
     * <pre>
     *     System.getProperty(propertyKey) != null ? System.getProperty(propertyKey) : System.getenv(envVarName)
     * </pre>
     *
     * <p>This method is often used to specify parameters of the called external programs,
     * such as a path to the called application,
     * to provide user both mechanisms of customization (system properties and system environment).
     *
     * @param propertyKey the key, indicating the system property.
     * @param envVarName  the name of the environment variable.
     * @return the system property / environment variable with the given key / name;
     *         <tt>null</tt>, if there is no system property with the given key and
     *         also there is no environment variable with the given name.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>propertyKey</tt> is empty.
     * @throws SecurityException        when this exception was thrown by <tt>System.getProperty</tt> or
     *                                  <tt>System.getenv</tt> method.
     * @see #getExistingPathFromPropertyOrEnv(String, String, File)
     */
    public static String getPropertyOrEnv(String propertyKey, String envVarName) {
        if (propertyKey == null)
            throw new NullPointerException("Null propertyKey");
        if (envVarName == null)
            throw new NullPointerException("Null envVarName");
        String result = System.getProperty(propertyKey);
        if (result == null) {
            result = System.getenv(envVarName);
        }
        return result;
    }

    /**
     * Returns <tt>new File({@link #getPropertyOrEnv(String, String)
     * getPropertyOrEnv}(propertyKey,envVarName))</tt>, if such file or directory really exists,
     * throws <tt>FileNotFoundException</tt>, if it does not exist, or returns
     * <tt>defaultPath</tt> if it is not specified.
     * More precisely, equivalent to the following:
     *
     * <pre>
     *     String s = {@link #getPropertyOrEnv(String, String) getPropertyOrEnv}(propertyKey, envVarName);
     *     File result = s != null ? new File(s) : defaultPath;
     *     if (result != null && !result.exists())
     *         throw new FileNotFoundException(<i>some message</i>);
     *     return result;
     * </pre>
     *
     * <p>Note: this method returns <tt>null</tt> in the only case, when {@link #getPropertyOrEnv(String, String)
     * getPropertyOrEnv} method returns <tt>null</tt> (there is no system property with the given key
     * and there is no environment variable with the given name) and, at the same time,
     * <tt>defaultPath==null</tt>. If this method returns some non-null result, then
     * there is a guarantee that it is an existing path, in terms of <tt>java.io.File.exists()</tt> method.
     *
     * <p>This method is used for retrieving path to some disk file or directory, which must exist,
     * for example, the path to an external executable program, which you want to call.
     *
     * @param propertyKey the key, indicating the system property.
     * @param envVarName  the name of the environment variable.
     * @param defaultPath the path, returned when both the system property and the environment variable
     *                    are not specified; may be <tt>null</tt>.
     * @return the existing disk path, containing in the system property / environment variable
     *         with the given key / name, or <tt>null</tt> of there is no system property
     *         with the given key, there is no environment variable with the given name and
     *         <tt>defaultPath==null</tt>.
     * @throws NullPointerException     if <tt>propertyKey</tt> or <tt>envVarName</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>propertyKey</tt> is empty.
     * @throws SecurityException        if this exception was thrown by <tt>System.getProperty</tt> or
     *                                  <tt>System.getenv</tt> method.
     * @throws FileNotFoundException    if 1) the path, specified by the system property / environment variable,
     *                                  or 2) <tt>defaultPath</tt> if there is no such system property and
     *                                  environment variable, &mdash; does not correspond to an existing disk file
     *                                  or directory; note that in the 2nd case the exception is not thrown
     *                                  if <tt>defaultPath==null</tt>.
     * @see #getCustomJREHome()
     * @see #getExistingPathFromPropertyOrEnv(String, String)
     */
    public static File getExistingPathFromPropertyOrEnv(String propertyKey, String envVarName, File defaultPath)
        throws FileNotFoundException
    {
        // We do not actually use getPropertyOrEnv here for the only goal:
        // to form better message in a case of FileNotFoundException
        if (propertyKey == null)
            throw new NullPointerException("Null propertyKey");
        if (envVarName == null)
            throw new NullPointerException("Null envVarName");
        String s = System.getProperty(propertyKey);
        boolean propertyExist = s != null;
        if (s == null) {
            s = System.getenv(envVarName);
        }
        File result = s != null ? new File(s) : defaultPath;
        if (result != null && !result.exists())
            throw new FileNotFoundException((s == null ?
                "Default file / directory " + defaultPath :
                "File or directory, specified by "
                    + (propertyExist ? "system property " + propertyKey : "environment variable " + envVarName)
                    + ",")
                + " does not exists (" + result + ")");
        return result;
    }

    /**
     * Calls <tt>{@link #getExistingPathFromPropertyOrEnv(String, String, File)
     * getExistingPathFromPropertyOrEnv}(propertyKey,envVarName,null)</tt> and
     * throws <tt>FileNotFoundException</tt> in a case of <tt>null</tt> result.
     *
     * <p>This method may be used instead of {@link #getExistingPathFromPropertyOrEnv(String, String, File)},
     * if you require that either the specified system property, or the environment variable must be specified
     * and contain a correct existing disk path.</p>
     *
     * @param propertyKey the key, indicating the system property.
     * @param envVarName  the name of the environment variable.
     * @return the existing disk path, containing in the system property / environment variable
     *         with the given key / name;
     *         cannot be <tt>null</tt>.
     * @throws NullPointerException     if <tt>propertyKey</tt> or <tt>envVarName</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>propertyKey</tt> is empty.
     * @throws SecurityException        if this exception was thrown by <tt>System.getProperty</tt> or
     *                                  <tt>System.getenv</tt> method.
     * @throws FileNotFoundException    if some path is really specified by the system property / environment variable,
     *                                  but this path does not correspond to an existing disk file or directory,
     *                                  or if both <tt>System.getProperty</tt>
     *                                  and <tt>System.getenv</tt> method have returned <tt>null</tt>.
     * @see #getCustomJREHome()
     */
    public static File getExistingPathFromPropertyOrEnv(String propertyKey, String envVarName)
        throws FileNotFoundException
    {
        File result = getExistingPathFromPropertyOrEnv(propertyKey, envVarName, null);
        if (result == null)
            throw new FileNotFoundException("Some existing file or directory must be specified in "
                + "system property " + propertyKey + " or environment variable " + envVarName);
        return result;
    }

    /**
     * Returns path to the home directory of the currently executed JRE.
     * Equivalent to the following:
     * <pre>
     *     new File(System.getProperty("java.home"));
     * </pre>
     *
     * <p>Note: unlike {@link #getCustomJREHome()}, here is no a strict guarantee that this method
     * always returns an existing path. If the path, got from the system property "<tt>java.home</tt>",
     * does not exists (very improbable situation), this method still returns it and does not throw
     * <tt>FileNotFoundException</tt>.
     *
     * <p>Note: if there is no system property "<tt>java.home</tt>" (also very improbable situation),
     * i.e. if <tt>System.getProperty("java.home")</tt> returns <tt>null</tt>, this method throws
     * <tt>InternalError</tt>.
     *
     * @return the home directory of the currently executed JRE,
     * @throws SecurityException if this exception was thrown by <tt>System.getProperty</tt>.
     * @see #getCustomJREHome()
     * @see #getJavaExecutable(java.io.File)
     * @see #getCustomJVMOptions()
     */
    public static File getCurrentJREHome() {
        String s = System.getProperty("java.home");
        if (s == null)
            throw new InternalError("Null java.home system property");
        return new File(s);
    }

    /**
     * Returns path to the home directory of some custom JRE, specified in
     * {@link #JRE_PATH_PROPERTY_NAME} system property or in {@link #JRE_PATH_ENV_NAME} environment variable,
     * or the home directory of the currently executed JVM if there is no such property / environment variable.
     * Equivalent to the following:
     * <pre>
     *     result = {@link #getExistingPathFromPropertyOrEnv(String, String, File)
     *     getExistingPathFromPropertyOrEnv}(
     *         {@link #JRE_PATH_PROPERTY_NAME},
     *         {@link #JRE_PATH_ENV_NAME},
     *         {@link #getCurrentJREHome()});
     * </pre>
     *
     * <p>Note: there is a guarantee that this method always returns an existing path, in terms of
     * <tt>java.io.File.exists()</tt> method. If the path, got from system properties / environment,
     * does not exists, this method throws <tt>FileNotFoundException</tt>.
     *
     * @return the home directory of the JRE, specified by
     *         {@link #JRE_PATH_PROPERTY_NAME} / {@link #JRE_PATH_ENV_NAME},
     *         or the result of {@link #getCurrentJREHome()} if it is not specified.
     * @throws SecurityException     if this exception was thrown by <tt>System.getProperty</tt> or
     *                               <tt>System.getenv</tt> method.
     * @throws FileNotFoundException if {@link #getExistingPathFromPropertyOrEnv} method has thrown this exception.
     * @see #getJavaExecutable(java.io.File)
     * @see #getCustomJREHome(String)
     * @see #getCustomJVMOptions()
     */
    public static File getCustomJREHome() throws FileNotFoundException {
        return getExistingPathFromPropertyOrEnv(
            JRE_PATH_PROPERTY_NAME,
            JRE_PATH_ENV_NAME,
            getCurrentJREHome());
    }

    /**
     * Extended analog of {@link #getCustomJREHome()} method, allowing to specify some
     * "name" of JRE, which is added as a suffix to the name of the corresponding system property or
     * environment variable. It allows to use several different JREs, which you probably
     * need in the same application. The typical recommended example of such usage is specifying
     * the names "32" and "64": <tt>jreName="32"</tt> means that you need to work with 32-bit JRE,
     * installed on the computer, <tt>jreName="64"</tt> means that you need to work with 64-bit JRE,
     * installed on the computer, <tt>jreName=null</tt> means that the difference between JREs is not important
     * and you need to work with some external JRE, globally customized for your application.
     *
     * <p>More precisely, if <tt>jreName==null</tt>, this method is strictly equivalent to
     * {@link #getCustomJREHome()} method, and if <tt>jreName!=null</tt>, it is
     * equivalent to the following:
     * <pre>
     *     File namedHome = {@link #getExistingPathFromPropertyOrEnv(String, String, File)
     *     getExistingPathFromPropertyOrEnv}(
     *         {@link #JRE_PATH_PROPERTY_NAME} + "." + jreName,
     *         {@link #JRE_PATH_ENV_NAME} + "_" + jreName,
     *         null);
     *     result = namedHome == null ? {@link #getCustomJREHome()} : namedHome;
     * </pre>
     * <p>For example, if <tt>jreName</tt> is "64", then this method will check existence of
     * system property "<tt>net.algart.arrays.jre.path.64</tt>" and environment variable
     * "<tt>NET_ALGART_ARRAYS_JVM_OPTIONS_64</tt>". If one of them exists,
     * this method will check existence of the disk path (probably folder), specified by this
     * system property / environment variable, and either will return this path (if it exists),
     * or will throw <tt>FileNotFoundException</tt> (if there is no such disk folder or file).
     * In other case, i.e. if both system property "<tt>net.algart.arrays.jre.path.64</tt>"
     * and environment variable "<tt>NET_ALGART_ARRAYS_JRE_PATH_64</tt>" are not found,
     * this method will call {@link #getCustomJREHome()} method, which will try to find JRE home directory,
     * specified in the system property "<tt>net.algart.arrays.jre.path</tt>" and environment variable
     * "<tt>NET_ALGART_ARRAYS_JRE_PATH</tt>". If one of them exists,
     * the method, analogously, will check existence of the disk path (probably folder), specified by this
     * system property / environment variable, and either will return this path (if it exists),
     * or will throw <tt>FileNotFoundException</tt> (if not).
     * At last, if both system property "<tt>net.algart.arrays.jre.path</tt>"
     * and environment variable "<tt>NET_ALGART_ARRAYS_JRE_PATH</tt>" are also not found,
     * the current JVM home directory will be returned: <tt>System.getProperty("java.home")</tt>
     * (also with the check of existence: if this property contains non-existing disk path,
     * <tt>FileNotFoundException</tt> will be thrown).
     *
     * <p>Note: there is a guarantee that this method always returns an existing path, in terms of
     * <tt>java.io.File.exists()</tt> method. If the path, got from system properties / environment,
     * does not exists, this method throws <tt>FileNotFoundException</tt>.
     *
     * @param jreName some internal "name" of the required JRE; typical values are "32" or "64";
     *                may be null, then this method is equivalent to {@link #getCustomJREHome()}.
     * @return the home directory of the JRE, specified by
     *         the corresponding system property or environment variable,
     *         or the result of {@link #getCustomJREHome()} if it is not specified.
     * @throws SecurityException     if this exception was thrown by <tt>System.getProperty</tt> or
     *                               <tt>System.getenv</tt> method.
     * @throws FileNotFoundException if {@link #getExistingPathFromPropertyOrEnv} or {@link #getCustomJREHome()}
     *                               method has thrown this exception.
     */
    public static File getCustomJREHome(String jreName) throws FileNotFoundException {
        if (jreName == null) {
            return getCustomJREHome();
        }
        File namedHome = getExistingPathFromPropertyOrEnv(
            JRE_PATH_PROPERTY_NAME + "." + jreName,
            JRE_PATH_ENV_NAME + "_" + jreName,
            null);
        return namedHome == null ? getCustomJREHome() : namedHome;
    }

    /**
     * Returns the path to the "<i>java</i>" executable utility ("<i>java.exe</i>" on Windows platform),
     * located inside the specified JRE home directory.
     * If this method fails to find <i>java</i> utility, it throws <tt>FileNotFoundException</tt>.
     *
     * <p>Note: there is a guarantee that this method always returns an existing path, in terms of
     * <tt>java.io.File.exists()</tt> method (when it does not throw <tt>FileNotFoundException</tt>).
     *
     * <p>This method is useful when you want to execute an external program, written in Java:
     * the result of this method should be passed as the first argument of the <tt>ProcessBuilder</tt> constructor.
     *
     * @param jreHome home directory of some JRE (for example, the result of {@link #getCurrentJREHome()},
     *                {@link #getCustomJREHome()} or {@link #getCustomJREHome(String)} method).
     * @return the path to "<i>java</i>" executable program, starting JVM of the specified JRE.
     * @throws NullPointerException  if the argument is <tt>null</tt>.
     * @throws FileNotFoundException if this method cannot find "<i>java</i>" utility.
     * @see #getCustomJVMOptions()
     */
    public static File getJavaExecutable(File jreHome) throws FileNotFoundException {
        // Finding according http://docs.oracle.com/javase/1.5.0/docs/tooldocs/solaris/jdkfiles.html
        if (jreHome == null)
            throw new NullPointerException("Null jreHome argument");
        if (!jreHome.exists())
            throw new FileNotFoundException("JRE home directory " + jreHome + " does not exist");
        File javaBin = new File(jreHome, "bin");
        File javaFile = new File(javaBin, "java"); // Unix
        if (!javaFile.exists()) {
            javaFile = new File(javaBin, "java.exe"); // Windows
        }
        if (!javaFile.exists())
            throw new FileNotFoundException("Cannot find java utility at " + javaFile);
        return javaFile;
    }

    /**
     * Returns the list of JVM options (arguments of "<i>java</i>" executable utility),
     * listed in {@link #JVM_OPTIONS_PROPERTY_NAME} system property
     * or in {@link #JVM_OPTIONS_ENV_NAME} environment variable, or <tt>null</tt>
     * if there is no such property / environment variable.
     * JVM options (command line arguments) should be separated by spaces
     * in this system property / environment variable,
     * and there should be no spaces inside each option (argument).
     * This method is typically used together with {@link #getCustomJREHome()}.
     *
     * <p>Equivalent to the following:
     * <pre>
     *     String jvmOptions = {@link #getPropertyOrEnv(String, String)
     *     getPropertyOrEnv}(
     *         {@link #JVM_OPTIONS_PROPERTY_NAME},
     *         {@link #JVM_OPTIONS_ENV_NAME});
     *     result = jvmOptions == null ? null : java.util.Arrays.asList(jvmOptions.split("\\s+"));
     * </pre>
     *
     * <p>We recommend you to include this list into the arguments of the <tt>ProcessBuilder</tt> constructor,
     * when you are going to execute an external Java program via Java machine, returned
     * by {@link #getJavaExecutable(File)} method.
     *
     * @return the list of JVM options, specified by
     *         {@link #JVM_OPTIONS_PROPERTY_NAME} / {@link #JVM_OPTIONS_ENV_NAME},
     *         or <tt>null</tt> if it is not specified.
     * @throws SecurityException if this exception was thrown by <tt>System.getProperty</tt> or
     *                           <tt>System.getenv</tt> method.
     * @see #getCustomJVMOptions(String)
     * @see #getCustomJREHome()
     */
    public static List<String> getCustomJVMOptions() {
        String jvmOptions = getPropertyOrEnv(JVM_OPTIONS_PROPERTY_NAME, JVM_OPTIONS_ENV_NAME);
        return jvmOptions == null ? null : java.util.Arrays.asList(jvmOptions.split("\\s+"));
    }

    /**
     * Extended analog of {@link #getCustomJVMOptions()} method, allowing to specify some
     * "name" of JRE, which is added as a suffix to the name of the corresponding system property or
     * environment variable. It allows to use several settings for several JREs, which you probably
     * need in the application. This method is typically used together with {@link #getCustomJREHome(String)}.
     *
     * <p>More precisely, if <tt>jreName==null</tt>, this method is strictly equivalent to
     * {@link #getCustomJVMOptions()} method, and if <tt>jreName!=null</tt>, it is
     * equivalent to the following:
     * <pre>
     *     String jvmOptions = {@link #getPropertyOrEnv(String, String)
     *     getPropertyOrEnv}(
     *         {@link #JVM_OPTIONS_PROPERTY_NAME} + "." + jreName,
     *         {@link #JVM_OPTIONS_ENV_NAME} + "_" + jreName);
     *     result = jvmOptions == null ? {@link
     *     #getCustomJVMOptions()} : java.util.Arrays.asList(jvmOptions.split("\\s+"));
     * </pre>
     * <p>For example, if <tt>jreName</tt> is "64", then this method will check existence of
     * system property "<tt>net.algart.arrays.jvm.options.64</tt>" and environment variable
     * "<tt>NET_ALGART_ARRAYS_JVM_OPTIONS_64</tt>". If one of them exists, it will be parsed and returned;
     * in other case, this method will call {@link #getCustomJVMOptions()}.
     *
     * @param jreName some internal "name" of the required JRE; typical values are "32" or "64";
     *                may be null, then this method is equivalent to {@link #getCustomJVMOptions()}.
     * @return the list of JVM options, specified by
     *         the corresponding system property or environment variable,
     *         or the result of {@link #getCustomJVMOptions()} if it is not specified.
     * @throws SecurityException if this exception was thrown by <tt>System.getProperty</tt> or
     *                           <tt>System.getenv</tt> method.
     */
    public static List<String> getCustomJVMOptions(String jreName) {
        if (jreName == null) {
            return getCustomJVMOptions();
        }
        String jvmOptions = getPropertyOrEnv(
            JVM_OPTIONS_PROPERTY_NAME + "." + jreName,
            JVM_OPTIONS_ENV_NAME + "_" + jreName);
        return jvmOptions == null ? getCustomJVMOptions() : java.util.Arrays.asList(jvmOptions.split("\\s+"));
    }

    /**
     * Writes the given text into the file in UTF-8 encoding.
     * The file is fully rewritten and will contain this text only.
     *
     * @param file the file which should be written.
     * @param text some text data, which will be saved into this file.
     * @throws IOException if an I/O error occurs.
     * @see #writeWorkUTF8(String, String)
     * @see #readUTF8(java.io.File)
     */
    public static void writeUTF8(File file, String text) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
        try {
            writer.write(text);
        } catch (IOException e) {
            try {
                writer.close();
            } catch (IOException ex) {
                // preserve the original exception
            }
            throw e;
        }
        writer.close();
    }

    /**
     * Reads the full content of the given text file
     * and returns it as a <tt>String</tt>. This file is supposed to be written in UTF-8 encoding.
     *
     * @param file the file which should be read.
     * @return the full text content of this file.
     * @throws IOException if an I/O error occurs.
     * @see #readWorkUTF8(String)
     * @see #writeUTF8(java.io.File, String)
     */
    public static String readUTF8(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        StringBuilder sb = new StringBuilder();
        try {
            char[] buf = new char[65536];
            int len;
            while ((len = reader.read(buf)) >= 0) {
                sb.append(buf, 0, len);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * <p>Creates a new empty unique subdirectory within the specified parent directory, using the
     * given prefix to generate its name. It is an analog of <tt>java.io.File.createTempFile</tt> method,
     * but creates a subdirectory, not a file.
     *
     * <p>The created subdirectory will be the following:
     * <pre>
     *     new java.io.File(parentDirectory, prefix + String.valueOf(n))
     * </pre>
     * where <tt>n</tt> is some positive long integer value, initialized to some random value while the first call
     * of this method and corrected while each new call.
     * This subdirectory is created by <tt>java.io.File.mkdir()</tt> method; in a case of failure,
     * several attemps are performed.
     *
     * @param parentDirectory some parent directory; unlike <tt>java.io.File.createTempFile</tt> method,
     *                        <tt>null</tt> is not allowed here.
     * @param prefix          the prefix string; unlike <tt>java.io.File.createTempFile</tt> method, here is no
     *                        requirements for its length
     * @return new unique subdirectory of <tt>parentDirectory</tt>, created by this method.
     * @throws NullPointerException if <tt>prefix</tt> or <tt>parentDirectory</tt> argument is <tt>null</tt>.
     * @throws IOException          if a new subdirectory cannot be created.
     */
    public static File createTempDirectory(File parentDirectory, String prefix) throws IOException {
        if (prefix == null)
            throw new NullPointerException("Null prefix argument");
        if (parentDirectory == null)
            throw new NullPointerException("Null parentDirectory argument");
        synchronized (TEMP_DIR_LOCK) {
            for (int k = 0; k < TEMP_DIR_NUMBER_OF_ATTEMPTS; k++) {
                File f = generateDir(parentDirectory, prefix);
                if (f.mkdir()) {
                    return f;
                }
            }
            throw new IOException("Cannot create an unique directory inside "
                + parentDirectory + " in " + TEMP_DIR_NUMBER_OF_ATTEMPTS + " attempts");
        }
    }


    /**
     * Returns the current context used by this instance for some operations.
     * This context is specified while creating an instance of this class.
     *
     * <p>The context is used in {@link #execute(ProcessBuilder)} method to allow the user to interrupt it.
     * Namely, if the context is not <tt>null</tt>, {@link #execute(ProcessBuilder)}
     * method periodically calls <tt>context.{@link ArrayContext#checkInterruption()
     * checkInterruption()}</tt>, and if some exception is thrown, terminates the external process
     * by <tt>java.lang.Process.destroy()</tt>.
     *
     * @return the current context used by this instance; may be <tt>null</tt>.
     */
    public ArrayContext context() {
        return context;
    }

    /**
     * Returns the work directory of this processor, created by {@link #getInstance(ArrayContext, String, String)}
     * or {@link #getInstance(ArrayContext)} method.
     * See {@link #getInstance(ArrayContext, String, String)} method about "work directory" concept.
     *
     * <p>The returned object contains an absolute path: <tt>File.getAbsoluteFile()</tt>.</p>
     *
     * @return the work directory of this processor.
     * @see #getWorkFile(String)
     */
    public File getWorkDirectory() {
        return workDirectory;
    }

    /**
     * Equivalent to <tt>new File({@link #getWorkDirectory()}, childFileName)</tt>.
     *
     * @param childFileName the child file name.
     * @return new <tt>File</tt> instance, describing the given file
     *         inside the work directory of this object.
     * @see #writeWorkUTF8(String, String)
     * @see #readWorkUTF8(String)
     */
    public File getWorkFile(String childFileName) {
        return new File(workDirectory, childFileName);
    }

    /**
     * Equivalent to <tt>{@link #writeUTF8(java.io.File, String)
     * writeUTF8}({@link #getWorkFile(String) getWorkFile}(childFileName),text)</tt>.
     *
     * <p>It is a useful method for inter-application communication with the called external program.
     *
     * @param childFileName the name of the file inside the work directory of this object.
     * @param text          some text data, which will be saved into this file.
     * @throws IOException if an I/O error occurs.
     */
    public void writeWorkUTF8(String childFileName, String text) throws IOException {
        writeUTF8(getWorkFile(childFileName), text);
    }

    /**
     * Equivalent to <tt>{@link #readUTF8(java.io.File)
     * readUTF8}({@link #getWorkFile(String) getWorkFile}(childFileName))</tt>.
     *
     * <p>It is a useful method for inter-application communication with the called external program.
     *
     * @param childFileName the name of the file inside the work directory of this object.
     * @return the full text content of this file.
     * @throws IOException if an I/O error occurs.
     */
    public String readWorkUTF8(String childFileName) throws IOException {
        return readUTF8(getWorkFile(childFileName));
    }

    /**
     * Returns the output stream, set by {@link #setOutputStream(java.io.OutputStream)} method
     * and used by {@link #execute(ProcessBuilder) execute} method
     * for duplication of the output stream of an external program.
     * The default value is <tt>null</tt> &mdash; it is returned
     * if {@link #setOutputStream(java.io.OutputStream)} was never called.
     *
     * @return the current output stream, used by {@link #execute(ProcessBuilder) execute} method
     *         for duplication of the output stream of an external program.
     */
    public OutputStream getOutputStream() {
        synchronized (lock) {
            return outputStream;
        }
    }

    /**
     * Sets the stream for duplication of the output stream of an external program.
     * If you set some non-null stream by this method, then the {@link #execute(ProcessBuilder)} method
     * will duplicate each line, written to the standard OS output stream by the called program,
     * into this stream.
     *
     * @param outputStream the output stream for duplication of the standard OS output stream of external programs;
     *                     may be <tt>null</tt>, then it will be ignored.
     * @see #getOutputStream()
     * @see #setErrorStream(java.io.OutputStream)
     */
    public void setOutputStream(OutputStream outputStream) {
        synchronized (lock) {
            this.outputStream = outputStream;
        }
    }

    /**
     * Returns the error stream, set by {@link #setErrorStream(java.io.OutputStream)} method
     * and used by {@link #execute(ProcessBuilder) execute} method
     * for duplication of the error stream of an external program.
     * The default value is <tt>null</tt> &mdash; it is returned
     * if {@link #setErrorStream(java.io.OutputStream)} was never called.
     *
     * @return the current error stream, used by {@link #execute(ProcessBuilder) execute} method
     *         for duplication of the error stream of an external program.
     */
    public OutputStream getErrorStream() {
        synchronized (lock) {
            return errorStream;
        }
    }

    /**
     * Sets the stream for duplication of the error stream of an external program.
     * If you set some non-null stream by this method, then the {@link #execute(ProcessBuilder)} method
     * will duplicate each line, written to the standard OS error stream by the called program,
     * into this stream.
     *
     * <p>Note: even when this stream is <tt>null</tt> (default value), if the called program was finished
     * unsuccessfully, i.e. with non-zero OS exit code, you can get the full content of the error stream
     * of the called program by {@link ExternalProcessException#getExternalProcessErrorMessage()
     * getExternalProcessErrorMessage()} method of the {@link ExternalProcessException},
     * thrown by {@link #execute(ProcessBuilder)} method.
     *
     * @param errorStream the output stream for duplication of the standard OS error stream of external programs;
     *                    may be <tt>null</tt>, then it will be ignored.
     * @see #getErrorStream()
     * @see #setOutputStream(java.io.OutputStream)
     */
    public void setErrorStream(OutputStream errorStream) {
        synchronized (lock) {
            this.errorStream = errorStream;
        }
    }

    /**
     * Equivalent to two calls: <tt>{@link #setOutputStream(java.io.OutputStream) setOutputStream}(System.out)</tt>
     * and <tt>{@link #setErrorStream(java.io.OutputStream) setErrorStream}(System.err)</tt>.
     */
    public void setSystemStreams() {
        setOutputStream(System.out);
        setErrorStream(System.err);
    }

    /**
     * Executes the external program by <tt>process=processBuilder.start()</tt> call
     * and waits until it will terminate by <tt>process.waitFor()</tt> call.
     * In addition to these main actions, this method:
     * <br>&nbsp;
     * <ul>
     * <li>periodically calls {@link ArrayContext#checkInterruption() checkInterruption()}
     * of the current {@link #context() context} of this object (if it is not <tt>null</tt>),
     * and if that method throws an exception (that means: the user wants to stop calculations),
     * automatically terminates the external process via <tt>process.destroy()</tt> call
     * and re-throws that exception again (as if {@link ArrayContext#checkInterruption() checkInterruption()}
     * was called in the current thread);
     * <br>&nbsp;</li>
     *
     * <li>catches the system output and error streams of the called external program
     * (via <tt>process.getInputStream()</tt> and <tt>process.getErrorStream()</tt>)
     * and, if {@link #setOutputStream(java.io.OutputStream)} and/or {@link #setErrorStream(java.io.OutputStream)}
     * methods were called with non-null argument, duplicates the text, written into
     * output/error stream of the external program, to the specified streams;
     * <br>&nbsp;</li>
     *
     * <li>checks the result of <tt>process.waitFor()</tt> (an exit code of the external application),
     * and, if it is non-zero, throws {@link ExternalProcessException};
     * in this case, you can retrieve the returned exit code and the full content of the OS error stream
     * of the external application via
     * {@link ExternalProcessException#getExternalProcessExitCode()} and
     * {@link ExternalProcessException#getExternalProcessErrorMessage()} methods.
     * </li>
     * </ul>
     *
     * <p>Note: this method cannot be used after closing this object by {@link #close()} method.
     *
     * <p>Note: if writing into the streams, set by {@link #setOutputStream(java.io.OutputStream)} or
     * {@link #setErrorStream(java.io.OutputStream)} methods, lead to <tt>IOException</tt>,
     * this exception is ignored.
     *
     * @param processBuilder the process builder, which will used for calling external application
     *                       (by <tt>processBuilder.start()</tt> call).
     * @throws IOException           if <tt>processBuilder.start()</tt> has thrown this exception,
     *                               or if <tt>process.waitFor()</tt> has returned non-zero result
     *                               (it will be {@link ExternalProcessException} in the last case).
     * @throws IllegalStateException if {@link #close()} method of this instance was already called.
     * @throws RuntimeException      this method also can throw all runtime exceptions, which are thrown
     *                               by <tt>processBuilder.start()</tt> call.
     */
    public void execute(final ProcessBuilder processBuilder) throws IOException {
        if (processBuilder == null)
            throw new NullPointerException("Null processBuilder argument");
        synchronized (lock) {
            if (isClosed())
                throw new IllegalStateException("This object is already closed");
            LOGGER.config("EP starting: " + processBuilder.command());
            final Process process = processBuilder.start();
            final BufferedWriter outputWriter = outputStream == null ? null :
                new BufferedWriter(new OutputStreamWriter(outputStream));
            final BufferedWriter errorWriter = errorStream == null ? null :
                new BufferedWriter(new OutputStreamWriter(errorStream));
            LOGGER.config("EP execution (" + workDirectory + ", the current directory "
                + new File("").getAbsolutePath() + "): " + processBuilder.command());
            StringBuilder sb = new StringBuilder();
            for (String s : processBuilder.command()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(s.contains(" ") ? "\"" + s + "\"" : s);
            }
            final String processBuilderCommand = sb.toString();
            writeStringOfPossible(outputWriter, processBuilderCommand);
            final AtomicReference<Object> interruptionReason = new AtomicReference<Object>(null);
            final AtomicBoolean outputReaderFinished = new AtomicBoolean(false);
            final Thread outputReaderThread = new Thread() {
                @Override
                public void run() {
                    InputStreamReader outputReader = new InputStreamReader(process.getInputStream());
                    try {
                        char[] data = new char[65536];
                        while (interruptionReason.get() == null) {
                            if (!outputReader.ready()) {
                                // allows to terminate this thread in a case of unexpected blocking
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException terminationSignal) {
                                    LOGGER.fine("outputReaderThread is normally terminated (" + workDirectory + ")");
                                    writeStringOfPossible(outputWriter,
                                        "*** Application terminated: skipping the rest of its output stream ***");
                                    return;
                                }
                            }
                            int n = outputReader.read(data);
                            if (n == -1) {
                                break;
                            }
                            if (n > 0 && outputWriter != null) {
                                try {
                                    outputWriter.write(data, 0, n);
                                    outputWriter.flush();
                                } catch (IOException ignored) {
                                    // minor problem
                                }
                            }
                        }
                        outputReader.close();
                    } catch (IOException e) {
                        LOGGER.warning("Unexpected error while reading the output of the process: " + e);
                        // just finish the thread
                    }
                    LOGGER.fine("outputReaderThread is finished (" + workDirectory + ")");
                    outputReaderFinished.set(true);
                }
            };

            final StringBuilder errorMessage = new StringBuilder();
            final AtomicBoolean errorReaderFinished = new AtomicBoolean(false);
            final Thread errorReaderThread = new Thread() {
                @Override
                public void run() {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    try {
                        char[] data = new char[65536];
                        while (interruptionReason.get() == null) {
                            if (!errorReader.ready()) {
                                // allows to terminate this thread in a case of unexpected blocking
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException terminationSignal) {
                                    LOGGER.fine("errorReaderThread is normally terminated (" + workDirectory + ")");
                                    writeStringOfPossible(errorWriter,
                                        "!!! Application terminated: skipping the rest of its error stream !!!");
                                    return;
                                }
                            }
                            int n = errorReader.read(data);
                            if (n == -1) {
                                break;
                            }
                            errorMessage.append(data, 0, n);
                            if (errorWriter != null) {
                                try {
                                    errorWriter.write(data, 0, n);
                                    errorWriter.flush();
                                } catch (IOException ignored) {
                                    // minor problem
                                }
                            }
                        }
                        errorReader.close();
                    } catch (IOException e) {
                        LOGGER.warning("Unexpected error while reading the output of the process: " + e);
                        // just finish the thread
                    }
                    LOGGER.fine("errorReaderThread is finished (" + workDirectory + ")");
                    errorReaderFinished.set(true);
                }
            };
            Thread interruptingThread = context == null ? null : new Thread() {
                @Override
                public void run() {
                    for (; ; ) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException terminationSignal) {
                            LOGGER.fine("interruptingThread is normally terminated (" + workDirectory + ")");
                            return;
                        }
                        try {
                            context.checkInterruption();
                        } catch (RuntimeException interruptionSignal) {
                            if (outputWriter != null) {
                                try {
                                    outputWriter.write("Stopping " + processBuilderCommand);
                                    outputWriter.newLine();
                                    outputWriter.flush();
                                } catch (IOException ignored) {
                                    // minor problem
                                }
                            }
                            process.destroy();
                            interruptionReason.set(interruptionSignal);
                            return;
                        }
                    }
                }
            };
            if (interruptingThread != null) {
                interruptingThread.start();
            }
            outputReaderThread.start();
            errorReaderThread.start();
            int exitCode;
            try {
                exitCode = process.waitFor();
                for (int count = 0; count < ADDITIONAL_WAIT_FOR_TERMINATION_TIMEOUT / 10; count++) {
                    if ((outputWriter == null || outputReaderFinished.get())
                        && (((errorWriter == null && exitCode == 0) || errorReaderFinished.get())))
                    {   // the readers are either normally finished, or not necessary
                        break;
                    }
                    Thread.sleep(10); // give a chance to read all output/error streams
                }
            } catch (InterruptedException e) {
                throw IOErrorJ5.getInstance(new IOException(
                    "Unexpected InterruptedException while waiting finishing the process: " + e));
            }
            outputReaderThread.interrupt();
            errorReaderThread.interrupt();
            String readersMessage = "the thread, reading the program's output stream, "
                + (outputReaderFinished.get() ?
                "was finished due to reaching the end of the stream" :
                "will be TERMINATED, because the program is already finished")
                + "; the thread, reading the program's error stream, "
                + (errorReaderFinished.get() ?
                "was finished due to reaching the end of the stream" :
                "will be TERMINATED,  because the program is already finished");
            if (interruptingThread != null) {
                interruptingThread.interrupt();
                if (interruptionReason.get() != null) {
                    LOGGER.config("EP execution stopped by user (" + workDirectory + "); " + readersMessage);
                    throw (RuntimeException) interruptionReason.get();
                }
            }
            LOGGER.config("EP execution finished (" + workDirectory + "); " + readersMessage);
            if (exitCode != 0)
                throw new ExternalProcessException(exitCode, errorMessage.toString(),
                    "Some problem occurred while calling external process: exit code " + exitCode
                        + String.format("%n") + errorMessage);
        }
    }

    /**
     * Cancels automatic removing of the work directory, created while instantiation of this object,
     * in {@link #close()} and {@link #finalize()} methods. If you call this method at least once,
     * then the further call of {@link #close()} will just set the state
     * of this instance to "{@link #isClosedSuccessfully() successfully closed}" and will not try to
     * remove the temporary disk data, which you possibly used for exchanging information with an external
     * program. It can be useful for debugging needs.
     *
     * <p>Note that this method <i>does not</i> cancel removing data, performed by {@link #cleanup()}
     * and {@link #cleanup(String)} methods.
     */
    public void cancelRemovingWorkDirectory() {
        synchronized (lock) {
            this.removingCancelled = true;
        }
    }

    /**
     * Returns <tt>true</tt> if and only if the method {@link #cancelRemovingWorkDirectory()}
     * was called at least once.
     *
     * @return whether automatic removing of the work directory is cancelled by
     *         {@link #cancelRemovingWorkDirectory()} method.
     */
    public boolean isRemovingWorkDirectoryCancelled() {
        return this.removingCancelled;
    }

    /**
     * Tries to fully (recursively) remove the work directory, created while instantiation of this object
     * (if this action was not cancelled by {@link #cancelRemovingWorkDirectory()} call),
     * and sets the state of this instance to "{@link #isClosed() closed}".
     * See {@link #getInstance(ArrayContext, String, String)} method about "work directory" concept.
     *
     * <p>You must call this method after finishing usage of this object. We strongly recommend you to
     * call it in the <tt>finally</tt> section of your code. After this call, you cannot
     * use {@link #execute(ProcessBuilder)} method, though can use other methods &mdash; in particular,
     * you can call this method again.
     *
     * <p>There is no guarantee that this method successfully removes the work directory.
     * This method can fail, for example, when some temporary files, used for exchanging data
     * with an external program, are locked by OS, and we cannot unlock them immediately after termination
     * of the external program &mdash; it can be connected with file mapping (in particular, mapping
     * via {@link LargeMemoryModel}, which is unmapped in Java while garbage collection only).
     *
     * <p>If this method has successfully removed the work directory
     * or if the removing was cancelled by {@link #cancelRemovingWorkDirectory()} call,
     * then {@link #isClosedSuccessfully()} method
     * will return <tt>true</tt>, and further calls of this method do nothing.
     * If this method was not able to remove all content of the work directory,
     * {@link #isClosedSuccessfully()} method will return <tt>false</tt>.
     * In this case, you may try to call this method again after some time or after a call of <tt>System.gc()</tt>.
     * In any case, after the first call of this method, {@link #isClosed()} method returns <tt>true</tt>
     * and {@link #execute(ProcessBuilder)} method throws <tt>IllegalStateException</tt>.
     *
     * @see #finalize()
     * @see #cleanup(String)
     */
    public void close() {
        synchronized (lock) {
            if (state == State.CLOSED_SUCCESSFULLY) {
                return;
            }
            boolean allOk;
            if (removingCancelled) {
                allOk = true;
            } else {
                allOk = deleteRecursive(workDirectory, state == State.ACTIVE ? USAGE_MARKER_FILE_NAME : null);
            }
            if (state == State.ACTIVE) {
                try {
                    markerLock.release();
                } catch (IOException e) {
                    LOGGER.warning("Cannot release the lock of the marker " + marker + ": " + e);
                }
                try {
                    markerChannel.close();
                    markerFile.close();
                } catch (IOException e) {
                    LOGGER.warning("Cannot close the marker file " + marker + ": " + e);
                }
                if (!marker.delete()) {
                    allOk = false;
                    LOGGER.warning("Cannot delete the marker file " + marker);
                } else {
                    allOk &= workDirectory.delete();
                }
            }
            state = allOk ? State.CLOSED_SUCCESSFULLY : State.CLOSED;
            /*
            // Removing global tempDirectory below is very bad idea: it is incompatible
            // with parallel execution of several applications, because it can occur
            // at any moment while another application tries to create this directory.
            // A correct solution is complicated enough and should be based on the file-lock technique.
            if (allOk && !removingCancelled) {
                String tempDirectory = workDirectory.getParent();
                if (tempDirectory != null && tempDirectory.equals(getDefaultTempDirectory())) {
                    // attempt to delete the global temporary directory, when not empty; does not affect allOk flag
                    new File(tempDirectory).delete();
                }
            }
            */
        }
    }

    /**
     * Returns <tt>true</tt> if this instance is "closed", i&#46;e&#46;
     * if {@link #close()} method was called at least once.
     * You cannot call {@link #execute(ProcessBuilder)} method for closed objects &mdash;
     * it will throw <tt>IllegalStateException</tt>.
     *
     * @return whether this object is already closed and, so, cannot be used for calling external programs.
     * @see #isClosedSuccessfully()
     */
    public boolean isClosed() {
        synchronized (lock) {
            return state != State.ACTIVE;
        }
    }

    /**
     * Returns <tt>true</tt> if this instance is "closed" and all its temporary data were successfully removed,
     * i&#46;e&#46; if {@link #close()} method was called and it has successfully removed
     * the {@link #getWorkDirectory() work directory} of this object.
     * If automatic removing of the work directory was cancelled by {@link #cancelRemovingWorkDirectory()} method,
     * then call of this method usually returns <tt>true</tt> after {@link #close()} (though the temporary data
     * were not removed).
     * If this method returns <tt>true</tt>, then {@link #isClosed()} method also returns <tt>true</tt>.
     *
     * @return whether this object is already closed and its work directory is successfully completely removed.
     */
    public boolean isClosedSuccessfully() {
        synchronized (lock) {
            return state == State.CLOSED_SUCCESSFULLY;
        }
    }

    /**
     * Returns a brief string description of this factory.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        synchronized (lock) {
            return "external array processor (" + workDirectory
                + (!isClosed() ? ")" : !isClosedSuccessfully() ?
                ", CLOSED (but some files were not deleted)" : ", CLOSED)");
        }
    }

    /**
     * Calls {@link #close()} method.
     */
    protected void finalize() {
        close();
//        System.out.println("Finalized " + this);
        // No reasons to call super.finalize(): it is the method of Object class, which does nothing.
    }

    private static void writeStringOfPossible(BufferedWriter writer, String s) {
        if (writer != null) {
            try {
                writer.write(s);
                writer.newLine();
                writer.flush();
            } catch (IOException ignored) {
                // minor problem
            }
        }
    }

    private static boolean deleteRecursive(File fileOrDir, String skippedFileName) {
        String[] fileNames = fileOrDir.list();
        boolean allOk = true;
        if (fileNames != null) {
            // A feature: always removes subdirectories before files
            for (int k = 0; k < fileNames.length; k++) {
                File f = new File(fileOrDir, fileNames[k]);
                if (f.isDirectory()) {
                    allOk &= deleteRecursive(f, null);
                    fileNames[k] = null;
                }
            }
            for (String fileName : fileNames) {
                if (fileName != null && !fileName.equals(skippedFileName)) {
                    allOk &= deleteIfExists(new File(fileOrDir, fileName));
                }
            }
        }
//        System.out.println("Deleting " + fileOrDir);
        return skippedFileName == null ? deleteIfExists(fileOrDir) : allOk;
    }

    private static boolean deleteIfExists(File fileOrDir) {
        return fileOrDir.delete() || !fileOrDir.exists();
        // Important note: fileOrDir, which even existed before, could be removed
        // as a result of some finalization methods or due to other parallel threads;
        // so, we must check its existence AFTER an attempt to call delete() method -
        // in other case we shall get sometimes illegal messages about "errors" while deletion
    }

    private static File generateDir(File dir, String prefix) {
        if (tempDirCounter == -1) {
            tempDirCounter = new Random().nextLong() & 0xFFFFFFFFL;
        }
        tempDirCounter++;
        return new File(dir, prefix + tempDirCounter);
    }

}
