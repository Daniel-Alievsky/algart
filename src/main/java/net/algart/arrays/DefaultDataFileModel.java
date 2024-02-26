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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.logging.Level;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Default implementation of {@link DataFileModel} that creates usual Java files,
 * which are mapped via standard Java technique (<tt>FileChannel.map</tt> method).</p>
 *
 * <p>The {@link DataFile data files}, returned by this class, creates
 * {@link DataFile.BufferHolder buffer holders} with the
 * {@link DataFile.BufferHolder#unmap(boolean) unmap(boolean)} method which does not perform
 * actual unmapping: Java NIO package does not support unmapping.
 * File mapping will be released automatically by the built-in finalizers.</p>
 *
 * <p>The {@link DataFile#close() close()} method in data files, returned by this class,
 * perform closing via <tt>RandomAccessFile.close()</tt> method,
 * but it <i>may not completely close the disk file</i>!
 * The disk file will be completely closed and all connected system resources will be freed
 * only while the following garbage collection at the unspecified moment,
 * or while exiting JVM.
 * So, if you need to process a very large number of AlgART arrays (tens of thousands or millions),
 * we recommend to use the {@link StandardIODataFileModel} and
 * call {@link Array#freeResources(ArrayContext)} method in time.</p>
 *
 * <p>See comments to {@link #createTemporary(boolean)} method for information about temporary
 * files created by this class.</p>
 *
 * <p>Warning: under Sun Java versions before 1.7 (i.e. 1.5 and 1.6),
 * this data file model <b>is not stable</b>,
 * due to the Sun's bug
 * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521677"
 * >(fc)&nbsp;"Cleaner terminated abnormally" error in simple mapping test</a>".
 * More precisely, for Java prior to 1.7:</p>
 *
 * <ul>
 * <li>In {@link #isLazyWriting() lazy-writing mode}, this model is <b>unstable at all</b>:
 * processing large arrays can lead to internal error while garbage collection,
 * that will lead to immediate abnormal JVM termination.
 * Due to this reason, the {@link #defaultLazyWriting() default lazy-writing mode} is <tt>false</tt>
 * in Java versions prior to 1.7, but <tt>true</tt> in Java 1.7+.</li>
 *
 * <li>In usual mode, this model can occasionally lead to unexpected <tt>IOError</tt>
 * while processing large arrays. Unlike an internal error in the lazy-writing mode,
 * this exception can be normally caught and shown to the user in GUI applications.
 * It can occur with large {@link #recommendedBankSize(boolean) bank size}
 * (32 MB or more), if an array occupies several banks.
 * This probability of this situation is not too high for unresizable arrays
 * when {@link #recommendedSingleMappingLimit() single mapping} is used.</li>
 * </ul>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see StandardIODataFileModel
 */
public class DefaultDataFileModel extends AbstractDataFileModel implements DataFileModel<File> {

    private static final int DEFAULT_NUMBER_OF_BANKS =
        MappedDataStorages.MappingSettings.nearestCorrectNumberOfBanks(
            Math.max(0, (int) Math.min((long) Integer.MAX_VALUE,
                (long) InternalUtils.getIntProperty("net.algart.arrays.DefaultDataFileModel.numberOfBanksPerCPU", 3)
                    * (long) InternalUtils.availableProcessors())));

    private static final int DEFAULT_BANK_SIZE =
        InternalUtils.JAVA_32 ?
            MappedDataStorages.MappingSettings.nearestCorrectBankSize(InternalUtils.getIntProperty(
                "net.algart.arrays.DefaultDataFileModel.bankSize32", 4194304)) : // 4M: for 4 kernels, up to 48M/array
            MappedDataStorages.MappingSettings.nearestCorrectBankSize(InternalUtils.getIntProperty(
                "net.algart.arrays.DefaultDataFileModel.bankSize", 16777216)); // 16M: for 12 kernels, up to 576M/array

    private static final int DEFAULT_RESIZABLE_BANK_SIZE =
        InternalUtils.JAVA_32 ?
            MappedDataStorages.MappingSettings.nearestCorrectBankSize(InternalUtils.getIntProperty(
                "net.algart.arrays.DefaultDataFileModel.resizableBankSize32", 2097152)) : // 2M
            MappedDataStorages.MappingSettings.nearestCorrectBankSize(InternalUtils.getIntProperty(
                "net.algart.arrays.DefaultDataFileModel.resizableBankSize", 4194304)); // 4M
    // These values must be not too high: for resizable arrays, it is a granularity of growing large files

    private static final int DEFAULT_SINGLE_MAPPING_LIMIT =
        InternalUtils.JAVA_32 ?
            Math.max(0, InternalUtils.getIntProperty(
                "net.algart.arrays.DefaultDataFileModel.singleMappingLimit32", 4194304)) : // 4M: up to 4M/array
            Math.max(0, InternalUtils.getIntProperty(
                "net.algart.arrays.DefaultDataFileModel.singleMappingLimit", 268435456)); // 256M: up to 256M/array


    private static final boolean DEFAULT_AUTO_RESIZING_ON_MAPPING = InternalUtils.getBooleanProperty(
        "net.algart.arrays.DefaultDataFileModel.autoResizingOnMapping", false);

    private static final boolean DEFAULT_LAZY_WRITING = InternalUtils.getBooleanProperty(
        "net.algart.arrays.DefaultDataFileModel.lazyWriting", InternalUtils.JAVA_7);

    private static final String DEFAULT_FILE_WRITE_MODE = InternalUtils.getStringProperty(
        "net.algart.arrays.DefaultDataFileModel.fileWriteMode", "rwd"); // used also in StandardIODataFileModel

    private static final long DEFAULT_PREFIX_SIZE = Math.max(0L,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.prefixSize", 0)); // for debugging only

    private static final int OPEN_SLEEP_DELAY = 200; // ms
    private static final int OPEN_TIMEOUT = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.openTimeout", 5000));  // 5 sec sleeping time

    private static final int MAP_SLEEP_DELAY = 200; // ms
    private static final int MAP_TIMEOUT = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.mapTimeout", 600));  // 0.6 sec sleeping time
    private static final int MAP_TIMEOUT_WITH_GC = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.mapTimeoutWithGc", 400));  // and 0.4 sec sleeping time with gc

    private static final int FORCE_SLEEP_DELAY = 250; // ms
    private static final int FORCE_TIMEOUT = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.forceTimeout", 15000));  // 15 sec sleeping time (40 attempts)
    private static final int MEMORY_UTILIZATION_FORCE_TIMEOUT = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.memoryUtilizationForceTimeout", 1000)); // 1 sec sleeping time
    private static final int WRITE_THROUGH_FORCE_TIMEOUT = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.writeThroughForceTimeout", 500));  // 0.5 sec sleeping time

    private static final int NEXT_RELOAD_MIN_DELAY = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.DefaultDataFileModel.nextReloadMinDelay", 2000));  // 2 sec

    private static final long MAX_MAPPED_MEMORY = Math.min(1L << 56, Math.max(0L,
        InternalUtils.getLongPropertyWithImportant(
            "net.algart.arrays.maxMappedMemory", 536870912L))); // 512 MB

    static final boolean UNSAFE_UNMAP_ON_EXIT = InternalUtils.getBooleanProperty(
        "net.algart.arrays.DefaultDataFileModel.unsafeUnmapOnExit", false); // false by default

    static final boolean UNSAFE_UNMAP_ON_DISPOSE = InternalUtils.getBooleanProperty(
        "net.algart.arrays.DefaultDataFileModel.unsafeUnmapOnDispose", false); // false by default; not used now

    static final boolean UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY = InternalUtils.getBooleanProperty(
        "net.algart.arrays.DefaultDataFileModel.unsafeUnmapOnExceedingMaxMappedMemory", false); // false by default

    static final boolean GC_ON_EXCEEDING_MAX_MAPPED_MEMORY = InternalUtils.getBooleanProperty(
        "net.algart.arrays.DefaultDataFileModel.gcOnExceedingMaxMappedMemory",
        !UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY); // !UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY by default

    private static final boolean CACHE_MAPPINGS = true;
    // This flag may be set to false for debug goals only!
    // It must be true to provide correct full DataFile.force() method.

    private final boolean lazyWriting;

    /**
     * Default {@link #isLazyWriting() lazy-writing mode}, used when this this class
     * is instantiated by a constructor without <tt>lazyWriting</tt> argument.
     * More precisely, if there is the system property
     * "<tt>net.algart.arrays.DefaultDataFileModel.lazyWriting</tt>",
     * containing "<tt>true</tt>" or "<tt>false</tt>" string (in lower case),
     * this method returns <tt>true</tt> if this property contains "<tt>true</tt>"
     * and <tt>false</tt> if this property contains "<tt>false</tt>".
     * If there is no such property, or if it contains illegal string (not "<tt>true</tt>" or "<tt>false</tt>"),
     * or if some exception occurred while calling <tt>System.getProperty</tt>,
     * this method returns <tt>true</tt> in Java 1.7 or higher Java version
     * and <tt>false</tt> in Java 1.5 and Java 1.6.
     * The value of this system property is loaded and checked only once
     * while initializing {@link DefaultDataFileModel} class.
     *
     * @return default {@link #isLazyWriting() lazy-writing mode}
     */
    public static boolean defaultLazyWriting() {
        return DEFAULT_LAZY_WRITING;
    }

    /**
     * The maximal amount of RAM (in bytes), allowed for simultaneous mapping by <tt>FileChannel.map</tt> method
     * without flushing data to the disk by <tt>MappedByteBuffer.force()</tt> method.
     *
     * <p>This value is important while using {@link #isLazyWriting() lazy-writing mode}.
     * In this case, a lot of mapping requests (calls of <tt>FileChannel.map</tt>),
     * with modifications of the mapped data and without further <tt>MappedByteBuffer.force()</tt>,
     * will use RAM for storing the changed data in the system cache.
     * When all (or almost all) available RAM will be spent, it may lead to intensive disk swapping.
     * The reason is that the mapped memory is not controlled by Java garbage collector:
     * it is possible to map much more disk memory than <tt>Runtime.maxMemory()</tt>.
     * The result may be extremely slowing down of all other applications, working on the computer,
     * and even practical impossibility of any work: all RAM will be occupied by your application.
     *
     * <p>To avoid this behavior, this class controls the total amount of mapped memory
     * (summary size of all mapped buffers in all files),
     * and when it exceeds the limit, returned by this method,
     * calls <tt>MappedByteBuffer.force()</tt> for all currently mapped buffers
     * and, so, flushs the data to the disk and frees the system memory.
     *
     * <p>This value, returned by this method, is retrieved from the system property
     * "<tt>net.algart.arrays.maxMappedMemory</tt>",
     * if it exists and contains a valid integer number.
     * If this property contains zero or negative integer, this method returns 0, and
     * it means that the amount of RAM for simultaneous mapping is not limited at all:
     * the application will try to use all available system memory.
     * If this property contains an integer greater than the limit 2<sup>56</sup>~7.2*10<sup>16</sup>,
     * this limit is used instead: it guarantees that using this value will not lead to integer overflow.
     * If there is no such property, or if it contains not a number,
     * or if some exception occurred while calling <tt>Long.getLong</tt>,
     * this method returns the default value <tt>536870912</tt> (512 MB).
     * The value of this system property is loaded and checked only once
     * while initializing {@link DefaultDataFileModel} class.
     *
     * <p>We recommend to always set this system property in serious applications,
     * working with large AlgART arrays.
     * The suitable value is about 50-100% of the current RAM installed on the computer.
     * The default value 512 MB works well if you don't need to process larger amounts of data frequently.
     *
     * @return the maximal amount of RAM (in bytes), allowed for simultaneous mapping by this class.
     */
    public static long maxMappedMemory() {
        return MAX_MAPPED_MEMORY;
    }

    /**
     * Equivalent to <tt>new {@link #DefaultDataFileModel(File, long, boolean)
     * DefaultDataFileModel}(null, 0, {@link #defaultLazyWriting()})</tt>.
     */
    public DefaultDataFileModel() {
        this(null, DEFAULT_PREFIX_SIZE, defaultLazyWriting());
    }

    /**
     * Equivalent to <tt>new {@link #DefaultDataFileModel(File, long, boolean)
     * DefaultDataFileModel}(null, 0, lazyWriting)</tt>.
     *
     * @param lazyWriting it <tt>true</tt>, lazy-writing mode will be used.
     */
    public DefaultDataFileModel(boolean lazyWriting) {
        this(null, DEFAULT_PREFIX_SIZE, lazyWriting);
    }

    /**
     * Equivalent to <tt>new {@link #DefaultDataFileModel(File, long, boolean)
     * DefaultDataFileModel}(tempPath, 0, {@link #defaultLazyWriting()})</tt>.
     *
     * @param tempPath    the path where new temporary files will be created
     *                    by {@link #createTemporaryFile(boolean)} method
     *                    or <tt>null</tt> if the default temporary-file directory is to be used.
     */
    public DefaultDataFileModel(File tempPath) {
        this(tempPath, DEFAULT_PREFIX_SIZE, defaultLazyWriting());
    }

    /**
     * Equivalent to <tt>new {@link #DefaultDataFileModel(File, long, boolean)
     * DefaultDataFileModel}(tempPath, 0, lazyWriting)</tt>.
     *
     * @param tempPath    the path where new temporary files will be created
     *                    by {@link #createTemporaryFile(boolean)} method
     *                    or <tt>null</tt> if the default temporary-file directory is to be used.
     * @param lazyWriting it <tt>true</tt>, lazy-writing mode will be used.
     */
    public DefaultDataFileModel(File tempPath, boolean lazyWriting) {
        this(tempPath, DEFAULT_PREFIX_SIZE, lazyWriting);
    }

    /**
     * Creates new instance with specified lazy-writing mode.
     *
     * <p>Please see {@link AbstractDataFileModel#AbstractDataFileModel(File, long)} about
     * <tt>tempPath</tt> and <tt>prefixSize</tt> arguments.
     *
     * <p>The <tt>lazyWriting</tt> argument specifies whether the data files
     * will use lazy writing mode. Namely, if this flag is set, flushing or unmapping
     * the mapped regions via {@link DataFile.BufferHolder#flush(boolean) DataFile.BufferHolder.flush(false)},
     * {@link DataFile.BufferHolder#unmap(boolean) DataFile.BufferHolder.unmap(false)} calls
     * will not lead to immediate writing data
     * to the disk file: this method will not do anything.
     * Instead, the data will be really written by garbage collector.
     * If this flag is not set, any call of {@link DataFile.BufferHolder#flush(boolean)}
     * or {@link DataFile.BufferHolder#unmap(boolean)} method
     * forces writing data to the disk by
     * <tt>force()</tt> method of <tt>MappedByteBuffer</tt> class.
     *
     * <p>By default, if you use constructors without <tt>lazyWriting</tt> argument,
     * this flag is retrieved from the system property
     * "<tt>net.algart.arrays.DefaultDataFileModel.lazyWriting</tt>"
     * or, if there is no such property,
     * is set to <tt>true</tt> in Java 1.7+ or <tt>false</tt> in Java 1.5 and 1.6.
     * Please see {@link #defaultLazyWriting()}.
     *
     * <p>Usually, you should set <tt>lazyWriting</tt> flag to <tt>true</tt>.
     * It can essentially increase the performance, if you create and modify many large AlgART arrays,
     * because OS will store the new data in the cache and will not physically write data to the disk.
     * Even in this case, this class periodically flushs the unsaved data, when the summary
     * amount of mapped buffers exceeds the limit returned by {@link #maxMappedMemory()} method.
     *
     * <p>The <tt>false</tt> value of this flag may be recommended if the stable behavior of your application
     * is more important than the speed. If lazy writing is disabled,
     * the application will use less RAM and the risk of swapping will be much less,
     * because each new data will be immediately saved to the disk and will not be cached in RAM.
     *
     * <p>Unfortunately, <b>lazy-writing mode leads to internal Sun's bug in Java 1.5 and 1.6</b>:
     * we recommend never set it to <tt>true</tt> in these Java versions.
     * The detailed description of this bug is here:
     * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521677"
     * >(fc)&nbsp;"Cleaner terminated abnormally" error in simple mapping test</a>".
     *
     * @param tempPath    the path where new temporary files will be created
     *                    by {@link #createTemporaryFile(boolean)} method
     *                    or <tt>null</tt> if the default temporary-file directory is to be used.
     * @param prefixSize  the value returned by {@link #recommendedPrefixSize()} implementation in this class.
     * @param lazyWriting it <tt>true</tt>, lazy-writing mode will be used.
     * @see #maxMappedMemory()
     */
    public DefaultDataFileModel(File tempPath, long prefixSize, boolean lazyWriting) {
        super(tempPath, prefixSize);
        this.lazyWriting = lazyWriting;
    }

    /**
     * Returns the <tt>lazyWriting</tt> argument, passed to
     * {@link #DefaultDataFileModel(boolean) the constructor} while creating this instance.
     *
     * @return the <tt>lazyWriting</tt> flag, passed to the constructor.
     */
    public final boolean isLazyWriting() {
        return this.lazyWriting;
    }

    /**
     * This implementation returns the data file corresponding to usual Java file <tt>new java.io.File(path)</tt>
     * with {@link DataFile#map(net.algart.arrays.DataFile.Range, boolean) DataFile.map}
     * method based on standard Java mapping.
     *
     * <p>This method never throws <tt>java.io.IOError</tt>.
     *
     * @param path      the path to disk file (as the argument of <tt>new java.io.File(path)</tt>).
     * @param byteOrder the byte order that will be always used for mapping this file.
     * @return          new instance of {@link DataFile} object.
     * @throws NullPointerException if one of the passed arguments is <tt>null</tt>.
     */
    public DataFile getDataFile(File path, ByteOrder byteOrder) {
        if (path == null)
            throw new NullPointerException("Null path argument");
        if (byteOrder == null)
            throw new NullPointerException("Null byteOrder argument");
        return new MappableFile(path, byteOrder, lazyWriting);
    }

    /**
     * Returns the absolute path to the disk file (<tt>java.io.File.getAbsoluteFile()</tt>).
     * The argument may be created by this data file model or by {@link StandardIODataFileModel}.
     *
     * <p>This method never throws <tt>java.io.IOError</tt>.
     *
     * @param dataFile the data file.
     * @return         the absolute path to the disk file.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     * @throws ClassCastException   if the data file was created by data file model, other than
     *                              {@link DefaultDataFileModel} or {@link StandardIODataFileModel}.
     */
    public File getPath(DataFile dataFile) {
        return ((MappableFile)dataFile).file;
    }

    /**
     * <p>This implementation returns <tt>true</tt>.
     *
     * @return <tt>true</tt>.
     */
    @Override
    public boolean isAutoDeletionRequested() {
        return true;
    }

    /**
     * <p>This implementation returns the value
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.numberOfBanksPerCPU",&nbsp;3)
     * * {@link Arrays.SystemSettings#availableProcessors()}</tt>,
     * stored while initializing this {@link DefaultDataFileModel} class,
     * or default value <tt>3&nbsp;*&nbsp;{@link Arrays.SystemSettings#availableProcessors()}</tt>,
     * if some exception occurred while calling <tt>Integer.getInteger</tt>.
     * If this value is less than 2, returns 2.
     * If "net.algart.arrays.DefaultDataFileModel.numberOfBanksPerCPU" property contains negative or zero integer,
     * returns 2.
     *
     <!--Repeat(INCLUDE_FROM_FILE, DataFileModel.java, recommendedNumberOfBanks_multiprocessor)   !! Auto-generated: NOT EDIT !! -->
     * <p>Please note that many algorithms, on multiprocessor or multi-core systems,
     * use several parallel threads for processing arrays: see {@link Arrays.ParallelExecutor}.
     * So, the number of banks should be enough for parallel using by all CPU units,
     * to avoid frequently bank swapping.
     * There should be at least 2 banks per each CPU unit,
     * better 3-4 banks (for complex random-access algorithms).
     <!--Repeat.IncludeEnd-->
     * @return the recommended number of memory banks.
     */
    @Override
    public int recommendedNumberOfBanks() {
        return DEFAULT_NUMBER_OF_BANKS;
    }

    /**
     * <p>This implementation returns the value
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.bankSize",16777216)</tt> (16 MB)
     * when the argument is <tt>true</tt> and
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.resizableBankSize",4194304)</tt> (4 MB)
     * when the argument is <tt>false</tt> on 64-bit Java machines.
     * On 32-bit JVM, this method returns
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.bankSize32",4194304)</tt> (4 MB)
     * when the argument is <tt>true</tt> and
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.resizableBankSize32",2097152)</tt> (2 MB)
     * when the argument is <tt>false</tt>.
     * These values are stored while initializing {@link DefaultDataFileModel} class.
     * If some exceptions occur while calling <tt>Integer.getInteger</tt>,
     * the default values 16777216 / 4194304 (for 64-bit Java) or
     * 4194304 / 2097152 (for 32-bit Java) are returned.
     * If this property contains invalid value (for example, not a power of two),
     * this value is automatically corrected to the nearest valid one.
     *
     * <p>This method distinguishes between 32-bit and 64-bit Java via {@link Arrays.SystemSettings#isJava32()} method.
     * Please remember that the result of that method is not 100% robust;
     * so, please not specify too high values if you are not quite sure that your JVM is not 32-bit
     * and has no 32-bit limitations for the address space.
     *
     * @param unresizable <tt>true</tt> if this bank size will be used for unresizable arrays only.
     * @return            the recommended size of every memory bank in bytes.
     */
    @Override
    public int recommendedBankSize(boolean unresizable) {
        return unresizable ? DEFAULT_BANK_SIZE : DEFAULT_RESIZABLE_BANK_SIZE;
    }

    /**
     * <p>This implementation returns the value
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.singleMappingLimit",268435456)</tt> (256 MB)
     * on 64-bit Java machines. On 32-bit JVM, this method returns
     * <tt>Integer.getInteger("net.algart.arrays.DefaultDataFileModel.singleMappingLimit32",4194304)</tt> (4 MB).
     * This value is stored while initializing {@link DefaultDataFileModel} class.
     * If some exceptions occur while calling <tt>Integer.getInteger</tt>,
     * the default value 268435456 (or 4194304 for 32-bit Java) is returned.
     *
     * <p>This method distinguishes between 32-bit and 64-bit Java via {@link Arrays.SystemSettings#isJava32()} method.
     * Please remember that the result of that method is not 100% robust;
     * so, please not specify too high values if you are not quite sure that your JVM is not 32-bit
     * and has no 32-bit limitations for the address space.
     *
     * @return the recommended limit for file size, in bytes, so that less files, if they are unresizable,
     *         should be mapped only once by single call of {@link DataFile#map} method.
     */
    @Override
    public int recommendedSingleMappingLimit() {
        return DEFAULT_SINGLE_MAPPING_LIMIT;
    }

    /**
     * <p>This implementation returns the value
     * <tt>Boolean.getBoolean("net.algart.arrays.DefaultDataFileModel.autoResizingOnMapping")</tt>,
     * stored while initializing {@link DefaultDataFileModel} class,
     * or <tt>false</tt> if there is no such system property or some exception occurred while
     * calling <tt>Boolean.getBoolean</tt>.
     *
     * @return <tt>true</tt> if mapping outside the file length automatically increase the length.
     */
    @Override
    public boolean autoResizingOnMapping() {
        return DEFAULT_AUTO_RESIZING_ON_MAPPING;
    }

    /**
     * This implementation returns <tt>"mapmm"</tt>;
     *
     * @return <tt>"mapmm"</tt>.
     */
    @Override
    public String temporaryFilePrefix() {
        return "mapmm";
    }

    /**
     * Returns a brief string description of this class.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "default data file model: " + recommendedNumberOfBanks()
            + " banks per " + recommendedBankSize(true) + "/" + recommendedBankSize(false) + " bytes, "
            + (recommendedSingleMappingLimit() > 0 ?
            "single mapping until " + recommendedSingleMappingLimit() + " bytes, " : "")
            + (lazyWriting ? "lazy-writing" : "write-through");
    }

    // The reasons of this method are analogous to mapWithSeveralAttempts and forceWithSeveralAttempts:
    // Windows NTFS sometimes cannot open file, because it is still "used" by abother process,
    // alike flushing mapped blocks from the cache
    private static RandomAccessFile openWithSeveralAttempts(File file, boolean readOnly) throws FileNotFoundException {
        long t1 = System.nanoTime();
        int numberOfAttempts = 0;
        FileNotFoundException exception = null;
        RandomAccessFile result = null;
        for (int timeoutInMillis = OPEN_TIMEOUT; ; timeoutInMillis -= OPEN_SLEEP_DELAY) {
            try {
                result = new RandomAccessFile(file, readOnly ? "r" : DEFAULT_FILE_WRITE_MODE);
                break;
            } catch (FileNotFoundException e) {
                if (readOnly && !file.exists()) {
                    throw e;
                }
                // strange situation: maybe, the Windows error
                // "The process cannot access the file because it is being used by another process"
                exception = e;
            }
            numberOfAttempts++;
            if (timeoutInMillis <= 0) {
                break;
            }
            try {
                Thread.sleep(OPEN_SLEEP_DELAY);
//                System.out.println("Sleeping for " + file);
            } catch (InterruptedException ex) {
                break;  // return the last exception if interrupted
            }
        }
        long t2 = System.nanoTime();
        if (result == null) {
            assert exception != null;
            LargeMemoryModel.LOGGER.warning(String.format(Locale.US,
                "MMMM open: cannot open file in %.2f sec, "
                + numberOfAttempts + " attempts (%s; %s)",
                (t2 - t1) * 1e-9, file, exception));
            throw exception;
        }
        return result;
    }

    // The following method is useful to avoid a bug in 32-bit Java:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6776490
    private static MappedByteBuffer mapWithSeveralAttempts(String fileName,
        final FileChannel fileChannel, final FileChannel.MapMode mode,
        final long position, final long size) throws IOException
    {
        // We should not use System.currentTimeMillis() below, but need to count calls of Thread.sleep:
        // the system time, theoretically, can be changed during this loop
//        System.out.print("Mapping " + Long.toHexString(position) + "...");
        long t1 = System.nanoTime();
        int numberOfAttempts = 0, numberOfGc = 0;
        IOException exception = null;
        MappedByteBuffer result = null;
        for (int timeoutInMilliseconds = MAP_TIMEOUT + MAP_TIMEOUT_WITH_GC; ;
             timeoutInMilliseconds -= MAP_SLEEP_DELAY)
        {
            try {
                result = Arrays.SystemSettings.globalDiskSynchronizer().doSynchronously(fileName,
                    new Callable<MappedByteBuffer>() {
                        public MappedByteBuffer call() throws IOException {
                            return fileChannel.map(mode, position, size);
                        }
                    });
                break;
            } catch (Exception ex) {
                if (!(ex instanceof IOException))
                    throw new AssertionError("Unexpected exception type: " + ex);
                exception = (IOException)ex;
            }
            numberOfAttempts++;
            if (timeoutInMilliseconds <= 0) {
                break;
            }
            boolean doGc = timeoutInMilliseconds <= MAP_TIMEOUT_WITH_GC;
            LargeMemoryModel.LOGGER.config("MMMM map: problem with mapping data, new attempt #"
                + numberOfAttempts + (doGc ? " with gc" : ""));
            if (doGc) {
                System.gc();
                numberOfGc++;
            }
            try {
                Thread.sleep(MAP_SLEEP_DELAY);
            } catch (InterruptedException ex) {
                break;  // return the last exception if interrupted
            }
        }
        long t2 = System.nanoTime();
        if (result == null) {
            assert exception != null;
            LargeMemoryModel.LOGGER.warning(String.format(Locale.US,
                "MMMM map: cannot map data in %.2f sec, "
                + numberOfAttempts + " attempts" + (numberOfGc > 0 ? ", " + numberOfGc + " with gc" : "")
                + " (%s; %s)",
                (t2 - t1) * 1e-9, fileName, exception));
            throw exception;
        }
//        System.out.println(" done: " + size + " bytes, "
//            + size / 1048576.0 / ((t2 - t1) * 1e-9) + " MB/sec (" + fileName
//            + ") in " + Thread.currentThread());
        return result;
    }

    // The following method is useful to avoid a bug in Java 1.6:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6539707
    private static Error forceWithSeveralAttempts(String fileName,
        final MappedByteBuffer mbb,
        DataFile.Range range, // for logging only
        boolean memoryUtilization,
        int timeoutInMillis)
    {
        Error resultError; // will be not null in a case of some exception inside mbb.force() method
        // We should not use System.currentTimeMillis() below, but need to count calls of Thread.sleep:
        // the system time, theoretically, can be changed during this loop
        int attemptCount = 0;
        long t1 = System.currentTimeMillis();
        for (; ; timeoutInMillis -= FORCE_SLEEP_DELAY) {
            resultError = null;
            try {
                attemptCount++;
                Arrays.SystemSettings.globalDiskSynchronizer().doSynchronously(fileName, new Callable<Object>() {
                    public Object call() {
                        mbb.force();
                        return null;
                    }
                });
            } catch (Throwable ex) {
                if (ex instanceof Error)
                    resultError = (Error)ex;
                else if (ex instanceof Exception)
                    resultError = IOErrorJ5.getInstance(ex);
                else
                    throw IOErrorJ5.getInstance(
                        new AssertionError("Invalid class of caught exception: " + ex.getClass()));
            }
            if (resultError == null || timeoutInMillis <= 0) {
                break;
            }
            if (MappedDataStorages.shutdownInProgress) {
                break; // not "think" too long in this situation and return the last exception "result"
            }
            try {
                Thread.sleep(Math.min(FORCE_SLEEP_DELAY, timeoutInMillis));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt(); // restore interrupt status: "result" exception can be ignored
                break; // return the last exception "result"
            }
        }
        if (resultError != null) {
            long t2 = System.currentTimeMillis();
            LargeMemoryModel.LOGGER.warning(String.format(Locale.US,
                "MMMM " + (memoryUtilization ? "memory utilization" : "flush")
                    + ": cannot force data at %s in %.2f sec, %d attempts (%s; %s)",
                range, (t2 - t1) * 1e-3, attemptCount, fileName, resultError));
        }
        return resultError;
    }

    private static void unsafeUnmap(final MappedByteBuffer mbb) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
            public Object run() throws Exception {
                Method getCleanerMethod = mbb.getClass().getMethod("cleaner");
                getCleanerMethod.setAccessible(true);
                Object cleaner = getCleanerMethod.invoke(mbb); // sun.misc.Cleaner instance
                Method cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.invoke(cleaner);
                return null;
            }
        });
    }

    static class RangeWeakReference<T> extends WeakReference<T> implements Comparable<RangeWeakReference<T>> {
        final Long fileIndex;
        // in "compareTo", we check fileIndex.longValue();
        // in "equals", we check the reference (it is unique for every file)
        final String fileName;
        final DataFile.Range key;
        volatile boolean unused = false;
        volatile boolean errorWhileForcing = false; // if true, will not try force again without necessity
        RangeWeakReference(T referent, Long fileIndex, String fileName, DataFile.Range key, ReferenceQueue<T> q) {
            super(referent, q);
            assert fileIndex != null;
            assert key != null;
            this.fileIndex = fileIndex;
            this.fileName = fileName;
            this.key = key;
        }

        public int compareTo(RangeWeakReference<T> o) {
            return this.fileIndex < o.fileIndex ? -1 :
                this.fileIndex > o.fileIndex ? 1 :
                    key.compareTo(o.key);
        }

        @Override
        public String toString() {
            return (unused ? "unused ": "") + "weak mapping " + key + " (file #" + fileIndex + ")";
        }

        @Override
        public int hashCode() {
            return fileIndex.hashCode() * 37 + key.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RangeWeakReference<?>)) {
                return false;
            }
            RangeWeakReference<?> rwr = (RangeWeakReference<?>)o;
            return fileIndex == rwr.fileIndex && key.equals(rwr.key);
            // "==" here (instead of "equals") is not a bug:
            // different files always have different ranges, even for the same numeric value of fileIndex
        }
    }

    static class UnmappableRangeWeakReference extends RangeWeakReference<ByteBuffer> {
        private boolean unmapped = false;
        UnmappableRangeWeakReference(MappedByteBuffer referent, Long fileIndex, String fileName,
            DataFile.Range key, ReferenceQueue<ByteBuffer> q)
        {
            super(referent, fileIndex, fileName, key, q);
        }

        public synchronized void unsafeUnmap() {
            if (!unused) {
                return;
            }
            if (unmapped) {
                return;
            }
            MappedByteBuffer mbb = (MappedByteBuffer)super.get();
            unmapped = true;
            try {
                DefaultDataFileModel.unsafeUnmap(mbb);
            } catch (PrivilegedActionException e) {
                LargeMemoryModel.LOGGER.log(Level.WARNING, "MMMM unsafe unmapping: " + e, e);
            }
        }

        @Override
        public synchronized ByteBuffer get() {
            return unmapped ? null : super.get();
        }

        @Override
        public String toString() {
            return (unmapped ? "UNMAPPED " : "") + super.toString();
        }
    }

    static class MappableFile implements DataFile {
        private static final AtomicLong CURRENT_FILE_INDEX = new AtomicLong(0);
        private static final Set<RangeWeakReference<ByteBuffer>> ALL_WRITABLE_MAPPINGS = Collections.synchronizedSet(
            new HashSet<RangeWeakReference<ByteBuffer>>());
        private static volatile long unforcedMappingMemory = 0; // atomic to be on the safe side

        final File file;
        final Long fileIndex; // not "long": we must be sure that this key is unique (for "==") for this instance
        private final ByteOrder byteOrder;
        private final boolean lazyWriting;
        private boolean readOnly = false;
        private RandomAccessFile raf;
        FileChannel fc;

        final ReferenceQueue<ByteBuffer> reaped = new ReferenceQueue<ByteBuffer>();
        final Map<Range, RangeWeakReference<ByteBuffer>> mappingCache = Collections.synchronizedMap(
            new HashMap<Range, RangeWeakReference<ByteBuffer>>());
        // - synchronization necessary due to using it in unsafeUnmap

        MappableFile(File file, ByteOrder byteOrder, boolean lazyWriting) {
            if (file == null)
                throw new NullPointerException("Null file argument");
            if (byteOrder == null)
                throw new NullPointerException("Null byteOrder argument");
            this.file = file.getAbsoluteFile();
            this.fileIndex = CURRENT_FILE_INDEX.getAndIncrement();
            this.byteOrder = byteOrder;
            this.lazyWriting = lazyWriting;
        }

        public final ByteOrder byteOrder() {
            return this.byteOrder;
        }

        public final OpenResult open(boolean readOnly) {
            OpenResult result = OpenResult.OPENED;
            if (raf == null) {
                this.readOnly = readOnly;
                try {
                    if (!readOnly && !file.exists()) {
                        result = OpenResult.CREATED;
                    }
                    raf = openWithSeveralAttempts(file, readOnly);
                } catch (IOException ex) {
                    throw IOErrorJ5.getInstance(ex);
                }
                fc = raf.getChannel();
            }
            reap();
            return result;
        }

        public void close() {
            if (raf != null) {
                try {
                    fc.close(); // to be on the safe side: can help under Java 1.7.0-ea-b10
                    raf.close(); // the channel is closed inside this call
                } catch (IOException ex) {
                    throw IOErrorJ5.getInstance(ex);
                } finally {
                    raf = null;
                    fc = null;
                }
            }
            reap();
        }

        public void force() {
            if (!lazyWriting) {
                return;
            }
            List<RangeWeakReference<ByteBuffer>> ranges;
            synchronized (mappingCache) {
                ranges = new ArrayList<RangeWeakReference<ByteBuffer>>(mappingCache.values());
            }
            Collections.sort(ranges);
            // sorting allows better force() method: buffers will be written in the position increasing order
            LargeMemoryModel.LOGGER.fine("MMMM flush: forcing all cache (" + ranges.size() + " blocks) for " + this);
            int count = 0;
            long totalBytes = 0;
            long t1 = System.nanoTime();
            for (RangeWeakReference<ByteBuffer> br : ranges) {
                if (br == null) {
                    continue;
                }
                MappedByteBuffer mbb = (MappedByteBuffer)br.get();
                if (mbb == null) {
                    continue;
                }
                LargeMemoryModel.LOGGER.finer("MMMM flush: forcing " + br + " of " + this);
                Error e = forceWithSeveralAttempts(file.getPath(), mbb, br.key, false, FORCE_TIMEOUT);
                if (e != null) {
                    br.errorWhileForcing = true;
                    throw e;
                }
                count++;
                totalBytes += br.key.length();
            }
            long t2 = System.nanoTime();
            LargeMemoryModel.LOGGER.fine(String.format(Locale.US,
                "MMMM flush: %d blocks (%.2f MB, %.3f ms, %.3f MB/sec) are forced for %s",
                count, totalBytes / 1048576.0, (t2 - t1) * 1e-6,
                totalBytes / 1048576.0 / ((t2 - t1) * 1e-9), this));
            if (raf != null) {
                try {
                    t1 = System.nanoTime();
                    fc.force(false);
                    // "false" argument prevents from extra I/O operations for changing file modification/access time
                    // It is not important because this call may have no effect at all if true mapping is used.
                    // (Usually this method is useful only in StandardIODataFileModel.)
                    t2 = System.nanoTime();
                    LargeMemoryModel.LOGGER.fine(String.format(Locale.US,
                        "MMMM flush: forcing FileChannel (%.3f ms) for %s", (t2 - t1) * 1e-6, this));
                } catch (IOException ex) {
                    throw IOErrorJ5.getInstance(ex);
                }
            }
        }


        public final boolean isReadOnly() {
            return this.readOnly;
        }

        public BufferHolder map(final Range range, final boolean notLoadDataFromFile) {
            try {
                FileChannel.MapMode mode = readOnly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE;
                MappedByteBuffer mbb;
                RangeWeakReference<ByteBuffer> br;
                if (CACHE_MAPPINGS) {
                    br = mappingCache.get(range);
                    mbb = br == null ? null : (MappedByteBuffer)br.get();
                } else {
                    mbb = null;
                    br = null;
                }
                boolean foundInCache = mbb != null;
                if (foundInCache) {
                    br.unused = false;
                    LargeMemoryModel.LOGGER.finest("MMMM quick loading " + br.key + " of " + this);
                } else {
                    if (CACHE_MAPPINGS) {
                        reap();
                    }
                    if (MAX_MAPPED_MEMORY > 0) {
                        utilizeMemory(range.length());
                    }
                    mbb = mapWithSeveralAttempts(file.getPath(), fc, mode, range.position(), range.length());
                    // more "safe" variant, in Java-32, than fc.map(mode, range.position(), range.length());
                    mbb.order(byteOrder);
                    if (CACHE_MAPPINGS) {
                        br = UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY ?
                            new UnmappableRangeWeakReference(mbb, fileIndex, file.getPath(), range, reaped) :
                            new RangeWeakReference<ByteBuffer>(mbb, fileIndex, file.getPath(), range, reaped);
                        mappingCache.put(range, br);
                        if (MAX_MAPPED_MEMORY > 0) {
                            ALL_WRITABLE_MAPPINGS.add(br);
                            // - I think that it is impossible that br already exists in ALL_WRITABLE_MAPPINGS,
                            // because then it should exist in mappingCache also (while removing from mappingCache
                            // we also remove it from ALL_WRITABLE_MAPPINGS); but even if I am not fully right,
                            // the maximal problem is forgetting forcing a very little number of MappedByteBuffers
                        }
                        LargeMemoryModel.LOGGER.finest("MMMM caching " + range + " of " + this);
                    }
                }
                return new MappedByteBufferHolder(mbb, br, file.getPath(), range, lazyWriting, foundInCache);
            } catch (IOException ex) {
                throw IOErrorJ5.getInstance(ex);
            }
        }

        public final long length() {
            try {
                return this.raf.length();
            } catch (IOException ex) {
                throw IOErrorJ5.getInstance(ex);
            }
        }

        public final void length(long newLength) {
            try {
                raf.setLength(newLength);
            } catch (IOException ex) {
                throw IOErrorJ5.getInstance(ex);
            }
        }

        public String toString() {
            return file.toString();
        }

        void reap() {
            RangeWeakReference<?> br;
            while ((br = (RangeWeakReference<?>)reaped.poll()) != null) {
                if (MAX_MAPPED_MEMORY > 0) {
                    ALL_WRITABLE_MAPPINGS.remove(br);
                }
                mappingCache.remove(br.key);
                LargeMemoryModel.LOGGER.finest("MMMM removing " + br.key + " of " + this);
            }
        }

        final boolean exists() {
            return file.exists();
        }

        final boolean unsafeUnmapAll() throws PrivilegedActionException {
            if (this.getClass() != MappableFile.class) {
                return true; // applicable only to this class!
            }
            Set<RangeWeakReference<ByteBuffer>> ranges;
            synchronized (mappingCache) {
                synchronized (ALL_WRITABLE_MAPPINGS) {
                    ranges = new TreeSet<RangeWeakReference<ByteBuffer>>(mappingCache.values());
                    // sorting allows better unmapping: buffers will be written in the position increasing order.
                    ALL_WRITABLE_MAPPINGS.removeAll(ranges);
                    mappingCache.clear();
                    // we must not use these MappedByteBuffers to avoid low-level EXCEPTION_ACCESS_VIOLATION
                }
            }

            boolean result = true;
            for (RangeWeakReference<?> br : ranges) {
                MappedByteBuffer mbb = br == null ? null : (MappedByteBuffer)br.get();
                if (mbb != null) {
                    if (br.unused) {
                        unsafeUnmap(mbb); // and we clear all mappingCache above
                    } else {
                        result = false;
                    }
                }
            }
            return result;
        }

        static String byteBufferToString(ByteBuffer bb) {
            if (bb == null)
                return "no buffer";
            StringBuilder sb = new StringBuilder("");
            int len = bb.limit();
            for (int k = 0; k < len; k++) {
                if (k > 0)
                    sb.append(",");
                sb.append(InternalUtils.toHexString(bb.get(k)));
                if (k == 4 && len > 10) {
                    k = len - 6;
                    sb.append("...,");
                }
            }
            return sb.toString();
        }

        private static void utilizeMemory(long mappedLength) {
            int count = 0;
            long totalBytes = 0;
            long t1, t2, t3, t4;
            synchronized (ALL_WRITABLE_MAPPINGS) {
                if (unforcedMappingMemory <= MAX_MAPPED_MEMORY) {
                    unforcedMappingMemory += mappedLength;
                    return;
                }
                // there is no sense to extract the quick previous code out of synchronization:
                // if some thread is now performing long-time memory utilization, all other threads
                // in any case should stop at "ALL_WRITABLE_MAPPINGS.add(br)" operator in "map" method
                unforcedMappingMemory = 0;
                t1 = System.nanoTime();
                List<RangeWeakReference<ByteBuffer>> ranges;
                ranges = new ArrayList<RangeWeakReference<ByteBuffer>>(ALL_WRITABLE_MAPPINGS);
                ALL_WRITABLE_MAPPINGS.clear();
                Collections.sort(ranges);
                // sorting allows better force() method: buffers will be written in the position increasing order
                LargeMemoryModel.LOGGER.fine("MMMM memory utilization: forcing all cache ("
                    + ranges.size() + " blocks)");
                t2 = System.nanoTime();
                for (RangeWeakReference<ByteBuffer> br : ranges) {
                    if (br == null || br.errorWhileForcing) {
                        continue;
                    }
                    MappedByteBuffer mbb = (MappedByteBuffer) br.get();
                    if (mbb == null) {
                        continue;
                    }
                    LargeMemoryModel.LOGGER.finer("MMMM memory utilization: forcing " + br);
                    Error e = forceWithSeveralAttempts(br.fileName, mbb, br.key, true,
                        MEMORY_UTILIZATION_FORCE_TIMEOUT);
                    if (e != null) {
                        br.errorWhileForcing = true;
                    }
                    if (UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY) {
                        assert br instanceof UnmappableRangeWeakReference;
                        ((UnmappableRangeWeakReference) br).unsafeUnmap();
                    }
                    count++;
                    totalBytes += br.key.length();
                    if (MappedDataStorages.shutdownInProgress) {
                        break; // break the long loop to allow immediate reaction to shutdown
                    }
                }
                t3 = System.nanoTime();
                if (GC_ON_EXCEEDING_MAX_MAPPED_MEMORY) {
                    for (int k = 0; k < 2; k++) {
                        // To be on the safe side: finalize some additional objects
                        // that could be referred from data storage finalized before.
                        // Example of such objects: data file models stored in
                        // LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion
                        // WARNING: these operators must be performed AFTER processing ALL_WRITABLE_MAPPINGS,
                        // because after them, indeed, weak references will be cleared.
                        // WARNING: calling System.runFinalization() is dangerous here!
                        // A dead-lock is possible while calling System.runFinalization() here,
                        // because finalizers of some objects can use the same locking.
                        System.gc();
                        try {
                            Thread.sleep(50); // to be on the safe side, sleeping as in java.nio.Bits
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // restore interrupt status
                        }
                    }
                }
                t4 = System.nanoTime();
            }
            if (GC_ON_EXCEEDING_MAX_MAPPED_MEMORY) {
                LargeMemoryModel.LOGGER.log(Arrays.SystemSettings.profilingMode() ? Level.CONFIG : Level.FINE,
                    String.format(Locale.US, "MMMM memory utilization"
                        + (UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY ? " with unmapping: " : ": ")
                        + "%.4f sec, %d blocks are forced "
                        + "(%.2f MB, %.3f ms preparing + %.3f ms + %.3f ms gc, %.3f MB/sec)",
                        (t4 - t1) * 1e-9, count, totalBytes / 1048576.0, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6,
                        totalBytes / 1048576.0 / ((t3 - t2) * 1e-9)));
            } else {
                LargeMemoryModel.LOGGER.log(Arrays.SystemSettings.profilingMode() ? Level.CONFIG : Level.FINE,
                    String.format(Locale.US, "MMMM memory utilization"
                        + (UNSAFE_UNMAP_ON_EXCEEDING_MAX_MAPPED_MEMORY ? " with unmapping: " : ": ")
                        + "%.4f sec, %d blocks are forced "
                        + "(%.2f MB, %.3f ms preparing + %.3f ms, %.3f MB/sec)",
                        (t4 - t1) * 1e-9, count, totalBytes / 1048576.0, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6,
                        totalBytes / 1048576.0 / ((t3 - t2) * 1e-9)));
            }
        }

        static class MappedByteBufferHolder implements DataFile.BufferHolder {
            private MappedByteBuffer mbb;
            private boolean preloaded = false;
            private long lastLoadTime = -1;
            private final RangeWeakReference<?> br;
            private final String fileName;
            private final Range range;
            private final boolean lazyWriting;
            private final boolean fromCache;

            MappedByteBufferHolder(MappedByteBuffer mbb, RangeWeakReference<?> br,
                String fileName, Range range,
                boolean lazyWriting, boolean fromCache)
            {
                this.mbb = mbb;
                this.br = br;
                this.fileName = fileName;
                this.range = range;
                this.lazyWriting = lazyWriting;
                this.fromCache = fromCache;
            }

            public Range range() {
                return range;
            }

            public ByteBuffer data() {
                if (mbb == null)
                    throw new IllegalStateException("Cannot call data() method: "
                        + "the buffer was already unmapped or disposed");
                return mbb;
            }

            public Object mappingObject() {
                return fromCache ? null : mbb;
            }

            public void load() {
                if (mbb == null)
                    throw new IllegalStateException("Cannot call load() method: "
                        + "the buffer was already unmapped or disposed");
                if (!preloaded || System.currentTimeMillis() - lastLoadTime > NEXT_RELOAD_MIN_DELAY) {
                    try {
                        Arrays.SystemSettings.globalDiskSynchronizer().doSynchronously(fileName,
                            new Callable<Object>() {
                                public Object call() throws Exception {
//                                    System.out.print("Loading " + range + "...");
//                                    long t1 = System.nanoTime();
                                    mbb.load();
//                                    long t2 = System.nanoTime();
//                                    System.out.println(" done: " + mbb.limit() + " bytes, "
//                                        + mbb.limit() / 1048576.0 / ((t2 - t1) * 1e-9) + " MB/sec (" + fileName
//                                        + ") in " + Thread.currentThread());
                                    return null;
                                }
                            });
                    } catch (Exception ex) {
                        throw new AssertionError("Unexpected exception: " + ex);
                    }
                    preloaded = true;
                    lastLoadTime = System.currentTimeMillis();
                }
            }

            public void flush(boolean forcePhysicalWriting) {
                if (mbb == null)
                    throw new IllegalStateException("Cannot call flush() method: "
                        + "the buffer was already unmapped or disposed");
                if (forcePhysicalWriting || (!lazyWriting && !br.errorWhileForcing)) {
                    LargeMemoryModel.LOGGER.finer("MMMM flush: forcing " + this);
//                    System.out.print("Flushing " + range + (forcePhysicalWriting ? " (forced)..." : "..."));
//                    long t1 = System.nanoTime();
                    Error e = forceWithSeveralAttempts(fileName, mbb, range, false,
                        forcePhysicalWriting ? FORCE_TIMEOUT : WRITE_THROUGH_FORCE_TIMEOUT);
                    if (e != null) {
                        br.errorWhileForcing = true;
                        if (forcePhysicalWriting)
                           throw e;
                    }
//                    long t2 = System.nanoTime();
//                    System.out.println(" done: " + mbb.limit() + " bytes, "
//                        + mbb.limit() / 1048576.0 / ((t2 - t1) * 1e-9) + " MB/sec (" + fileName
//                        + ") in " + Thread.currentThread());
                }
            }

            public void unmap(boolean forcePhysicalWriting) {
                if (mbb == null)
                    throw new IllegalStateException("Cannot call unmap() method: "
                        + "the buffer was already unmapped or disposed");
                if (CACHE_MAPPINGS) { // necessary to avoid null
                    br.unused = true;
                }
                try {
                    if (forcePhysicalWriting || !lazyWriting) {
                        LargeMemoryModel.LOGGER.finer("MMMM unmap: forcing " + this);
                        forceWithSeveralAttempts(fileName, mbb, range, false, WRITE_THROUGH_FORCE_TIMEOUT);
                        // ignore any possible exceptions here
                    }
                } finally {
                    mbb = null;
                }
            }

            public boolean dispose() {
                if (mbb == null)
                    throw new IllegalStateException("Cannot call dispose() method: "
                        + "the buffer was already unmapped or disposed");
                unmap(false);
                return true;
            }

            public boolean isLoadedFromCache() {
                return fromCache;
            }

            public String toString() {
                return "mapping " + range + " of " + fileName + " [" + byteBufferToString(mbb) + "]";
            }
        }
    }
}