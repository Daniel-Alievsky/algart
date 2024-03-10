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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * <p>Alternative implementation of {@link DataFileModel}, that creates usual Java files,
 * but emulates mapping via standard read/write operations.
 * More precisely, this implementation is based on
 * {@link #readAllBuffer(FileChannel, long, ByteBuffer)} and
 * {@link #writeAllBuffer(FileChannel, long, ByteBuffer)} methods:
 * see comments to them for more details.</p>
 *
 * <p>The {@link DataFile data files}, returned by this class, creates
 * {@link DataFile.BufferHolder buffer holders} containing NIO byte buffers, which can be direct or non-direct
 * &mdash; it depends on the argument <tt>directBuffers</tt> of the constructors, having such argument,
 * or on the {@link #defaultDirectBuffers()} value for other constructors.
 * The {@link DataFile#map(net.algart.arrays.DataFile.Range, boolean) map} method in a data file
 * allocates such byte buffer (<tt>ByteBuffer.allocateDirect(size)</tt> or <tt>ByteBuffer.allocate(size)</tt>)
 * and loads the file fragment into it ({@link #readAllBuffer(FileChannel, long, ByteBuffer)}).
 * The {@link DataFile.BufferHolder#unmap(boolean) unmap(true/false)} and
 * {@link DataFile.BufferHolder#flush(boolean) flush(true/false)} methods of the created buffer holder
 * writes all data back to file ({@link #writeAllBuffer(FileChannel, long, ByteBuffer)}).
 * Reading data is cached in free Java memory via <tt>WeakReference</tt> technique,
 * if this class was created via {@link #StandardIODataFileModel()} or some other constructor,
 * having no <tt>cacheReading</tt> argument, of via
 * or some constructor with the argument <tt>cacheReading=true</tt>.
 * Writing data is not cached.</p>
 *
 * <p>The {@link DataFile#close() close()} method in data files, returned by this class,
 * completely closes the disk file and releases all connected system resources.</p>
 *
 * <p>The temporary files are created and deleted in the same way as
 * in {@link DefaultDataFileModel}.</p>
 *
 * <p>This data file model is usually not so efficient as
 * the {@link DefaultDataFileModel} that is based on true file mapping operations,
 * especially for large {@link #recommendedBankSize(boolean) bank size}.
 * However, this model <i>can</i> provide better performance, than {@link DefaultDataFileModel},
 * in some applications, when you need sequential access to very large disk files, much larger than available RAM.
 * Some operation systems can be not optimized for a case of very intensive mapping of very large files,
 * which is the base of {@link DefaultDataFileModel}; at the same time, this data file model
 * uses only simple input/output OS calls and can have more predictable behaviour.</p>
 *
 * <p>In addition, this variant is absolutely stable on all platforms,
 * when Java file mapping technique has a serious bug in Java 1.5 and 1.6:
 * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521677"
 * >(fc)&nbsp;"Cleaner terminated abnormally" error in simple mapping test</a>".
 * Right now, this bug is usually avoided in current implementation of
 * {@link DefaultDataFileModel}, but there is a little risk to get
 * unexpected <tt>IOError</tt>.
 * If you need maximal stability with Java 1.5 or 1.6, and the performance is enough,
 * you may choose this data file model.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see DefaultDataFileModel
 */
public class StandardIODataFileModel extends AbstractDataFileModel implements DataFileModel<File> {

    private static final int STANDARD_IO_NUMBER_OF_BANKS =
        MappedDataStorages.MappingSettings.nearestCorrectNumberOfBanks(
            InternalUtils.getIntProperty("net.algart.arrays.StandardIODataFileModel.numberOfBanks", 32));

    private static final int STANDARD_IO_BANK_SIZE = MappedDataStorages.MappingSettings.nearestCorrectBankSize(
        InternalUtils.getIntProperty(
            "net.algart.arrays.StandardIODataFileModel.bankSize", 65536)); // 64 KB

    private static final boolean DEFAULT_DIRECT_BUFFERS =
        InternalUtils.getBooleanProperty(
            "net.algart.arrays.StandardIODataFileModel.directBuffers", true);

    private static final long STANDARD_IO_PREFIX_SIZE = Math.max(0L,
        InternalUtils.getIntProperty(
            "net.algart.arrays.StandardIODataFileModel.prefixSize", 0)); // for debugging only

    private static final int ALLOCATE_NUMBER_OF_ATTEMPTS = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.StandardIODataFileModel.allocateNumberOfAttempts", 2));  // 2 attempts
    private static final int ALLOCATE_NUMBER_OF_ATTEMPTS_WITH_GC = Math.max(0,
        InternalUtils.getIntProperty(
            "net.algart.arrays.StandardIODataFileModel.allocateNumberOfAttemptsWithGc", 6));  // and 6 with gc

    private static final boolean DEFAULT_CACHE_READING = true;
    // - please do not change, excepting debugging needs: "true" value is required by the contract of this class

    private final boolean cacheReading;
    private final boolean directBuffers;

    /**
     * Default {@link #isDirectBuffers() directBuffer} flag, used when this this class
     * is instantiated by a constructor without <tt>directBuffer</tt> argument.
     * More precisely, if there is the system property
     * "<tt>net.algart.arrays.StandardIODataFileModel.directBuffers</tt>",
     * containing "<tt>true</tt>" or "<tt>false</tt>" string (in lower case),
     * this method returns <tt>true</tt> if this property contains "<tt>true</tt>"
     * and <tt>false</tt> if this property contains "<tt>false</tt>"
     * If there is no such property, or if it contains illegal string (not "<tt>true</tt>" or "<tt>false</tt>"),
     * or if some exception occurred while calling <tt>System.getProperty</tt>,
     * this method returns <tt>true</tt> (default value).
     * The value of this system property is loaded and checked only once
     * while initializing {@link StandardIODataFileModel} class.
     *
     * @return default {@link #isDirectBuffers() directBuffer} flag.
     */
    public static boolean defaultDirectBuffers() {
        return DEFAULT_DIRECT_BUFFERS;
    }

    /**
     * Equivalent to <tt>new {@link #StandardIODataFileModel(File, long, boolean, boolean)
     * StandardIODataFileModel}(null, 0, true, {@link #defaultDirectBuffers()})</tt>.
     */
    public StandardIODataFileModel() {
        this(null, STANDARD_IO_PREFIX_SIZE, true, defaultDirectBuffers());
    }

    /**
     * Equivalent to <tt>new {@link #StandardIODataFileModel(File, long, boolean, boolean)
     * StandardIODataFileModel}(null, 0, cacheReading, directBuffers)</tt>.
     *
     * @param cacheReading  whether reading data should be cached in free Java memory.
     * @param directBuffers whether the {@link DataFile data files}, created by this class, should allocate
     *                      byte buffers for mapping by <tt>ByteBuffer.allocateDirect(size)</tt>
     *                      (if this argument is <tt>true</tt>) or by <tt>ByteBuffer.allocate(size)</tt>
     *                      (if this argument is <tt>false</tt>).
     */
    public StandardIODataFileModel(boolean cacheReading, boolean directBuffers) {
        this(null, STANDARD_IO_PREFIX_SIZE, cacheReading, directBuffers);
    }

    /**
     * Equivalent to <tt>new {@link #StandardIODataFileModel(File, long, boolean, boolean)
     * StandardIODataFileModel}(tempPath, 0, true, {@link #defaultDirectBuffers()})</tt>.
     *
     * @param tempPath the path where new temporary files will be created
     *                 by {@link #createTemporaryFile(boolean)} method
     *                 or <tt>null</tt> if the default temporary-file directory is to be used.
     */
    public StandardIODataFileModel(File tempPath) {
        this(tempPath, STANDARD_IO_PREFIX_SIZE, DEFAULT_CACHE_READING, defaultDirectBuffers());
    }

    /**
     * Equivalent to <tt>new {@link #StandardIODataFileModel(File, long, boolean, boolean)
     * StandardIODataFileModel}(tempPath, 0, cacheReading, {@link #defaultDirectBuffers()})</tt>.
     *
     * @param tempPath     the path where new temporary files will be created
     *                     by {@link #createTemporaryFile(boolean)} method
     *                     or <tt>null</tt> if the default temporary-file directory is to be used.
     * @param cacheReading whether reading data should be cached in free Java memory.
     */
    public StandardIODataFileModel(File tempPath, boolean cacheReading) {
        this(tempPath, STANDARD_IO_PREFIX_SIZE, cacheReading, DEFAULT_DIRECT_BUFFERS);
    }

    /**
     * Equivalent to <tt>new {@link #StandardIODataFileModel(File, long, boolean, boolean)
     * StandardIODataFileModel}(tempPath, 0, cacheReading, directBuffers)</tt>.
     *
     * @param tempPath      the path where new temporary files will be created
     *                      by {@link #createTemporaryFile(boolean)} method
     *                      or <tt>null</tt> if the default temporary-file directory is to be used.
     * @param cacheReading  whether reading data should be cached in free Java memory.
     * @param directBuffers whether the {@link DataFile data files}, created by this class, should allocate
     *                      byte buffers for mapping by <tt>ByteBuffer.allocateDirect(size)</tt>
     *                      (if this argument is <tt>true</tt>) or by <tt>ByteBuffer.allocate(size)</tt>
     *                      (if this argument is <tt>false</tt>).
     */
    public StandardIODataFileModel(File tempPath, boolean cacheReading, boolean directBuffers) {
        this(tempPath, STANDARD_IO_PREFIX_SIZE, cacheReading, directBuffers);
    }

    /**
     * Creates new instance of this class.
     *
     * <p>Please see {@link AbstractDataFileModel#AbstractDataFileModel(File, long)} about
     * <tt>tempPath</tt> and <tt>prefixSize</tt> arguments.
     *
     * <p>The data files, created by this model, will cache all loaded data
     * in free Java memory (via <tt>WeakReference</tt> technique),
     * if and only if <tt>cacheReading</tt> argument is <tt>true</tt>.
     * In many cases, caching can improve performance.
     * But if you are sure that the data will be usually read once,
     * it is better to pass <tt>false</tt> as the constructor argument,
     * because needless caching may increase disk swapping.
     *
     * <p>The <tt>directBuffers</tt> argument defines the kind of byte buffers, which
     * will be allocated in Java memory by {@link DataFile data files}, created by this class.
     * If this argument is <tt>true</tt>, their {@link DataFile#map(net.algart.arrays.DataFile.Range, boolean) map}
     * method will use direct byte buffers, i.e. will allocate them by <tt>ByteBuffer.allocateDirect(size)</tt> call.
     * If this argument is <tt>false</tt>, the {@link DataFile#map(net.algart.arrays.DataFile.Range, boolean) map}
     * method will use non-direct byte buffers, i.e. will allocate them by <tt>ByteBuffer.allocate(size)</tt> call.
     *
     * <p>Note: the value <tt>directBuffers=false</tt> can slow down processing AlgART arrays, created with help
     * of this data file model, if the element type is other than <tt>byte</tt>.
     * The reason is that byte buffers, created by this model, are converted to the necessary element type by methods
     * <tt>ByteBuffer.asShortBuffer</tt>, <tt>ByteBuffer.asIntBuffer()</tt>, <tt>ByteBuffer.asDoubleBuffer()</tt>,
     * etc., and accessing to content of the AlgART arrays is performed via these views of the original byte buffers.
     * In a case of direct byte buffers such conversion is usually performed at the low processor level
     * in a native code, and access to their views like <tt>ByteBuffer.asDoubleBuffer()</tt> is performed with
     * the maximal possible speed. Unlike this, in a case of non-direct byte buffers such conversion is
     * performed in Java code, and access to each element of these views requires some calculations.
     * For example, reading any element of <tt>ByteBuffer.asDoubleBuffer()</tt> means reading 8 bytes,
     * joining them into a long by "&lt;&lt;" and "|" operators and calling <tt>Double.longBitsToDouble</tt> method
     * &mdash; and these operations will be performed while any form of reading elements from
     * the AlgART array {@link DoubleArray}. Thus, access to AlgART arrays, excepting {@link ByteArray},
     * becomes slower. The speed difference is usually not very large, but can be appreciable for simple
     * applications with intensive accessing AlgART arrays.
     *
     * <p>On the other hand, please note: the value <tt>directBuffers=true</tt> can increase requirements
     * to memory and the risk of unexpected <tt>OutOfMemoryError</tt>, if you are using
     * {@link #recommendedNumberOfBanks() a lot} of {@link #recommendedBankSize(boolean) large} banks,
     * especially for 32-bit JVM. Direct byte buffers are usually allocated not in the usual Java heap,
     * but in some separate address space. As a result, on 32-bit JVM the maximal amount of memory,
     * which can be allocated in direct byte buffers, can be essentially less than -Xmx JVM argument,
     * when -Xmx is 500&ndash;1000&nbsp;MB or greater.
     * If you need maximally stable application and you are planning to use all or almost all available memory,
     * specified via -Xmx JVM argument, for banks of this data file model (to reduce disk swapping),
     * you should prefer non-direct buffers (<tt>directBuffers=false</tt>).
     * Of course, this problem should not occur while using default settings for {@link #recommendedNumberOfBanks()}
     * and {@link #recommendedBankSize(boolean)} &mdash; 32 banks per 64&nbsp;KB.
     *
     * @param tempPath      the path where new temporary files will be created
     *                      by {@link #createTemporaryFile(boolean)} method
     *                      or <tt>null</tt> if the default temporary-file directory is to be used.
     * @param prefixSize    the value returned by {@link #recommendedPrefixSize()} implementation in this class.
     * @param cacheReading  whether reading data should be cached in free Java memory.
     * @param directBuffers whether the {@link DataFile data files}, created by this class, should allocate
     *                      byte buffers for mapping by <tt>ByteBuffer.allocateDirect(size)</tt>
     *                      (if this argument is <tt>true</tt>) or by <tt>ByteBuffer.allocate(size)</tt>
     *                      (if this argument is <tt>false</tt>).
     * @see #defaultDirectBuffers()
     */
    public StandardIODataFileModel(File tempPath, long prefixSize, boolean cacheReading, boolean directBuffers) {
        super(tempPath, prefixSize);
        this.cacheReading = cacheReading;
        this.directBuffers = directBuffers;
    }

    /**
     * Returns the <tt>directBuffers</tt> argument, passed to
     * {@link #StandardIODataFileModel(File, long, boolean, boolean) the main constructor}
     * (or to other constructors, having such argument) while creating this instance.
     * If this instance was created by constructors, which have no <tt>directBuffers</tt>,
     * the result of this method is equal to {@link #defaultDirectBuffers()} value.
     *
     * @return whether the {@link DataFile data files}, created by this class, should allocate
     *         byte buffers for mapping by <tt>ByteBuffer.allocateDirect(size)</tt>
     *         (if this flag is <tt>true</tt>) or by <tt>ByteBuffer.allocate(size)</tt>
     *         (if this flag is <tt>false</tt>).
     */
    public final boolean isDirectBuffers() {
        return this.directBuffers;
    }

    /**
     * This implementation returns the data file corresponding to usual Java file <tt>new java.io.File(path)</tt>,
     * with {@link DataFile#map(net.algart.arrays.DataFile.Range, boolean) DataFile.map}
     * method that use usual read/write operation instead of Java mapping.
     *
     * <p>This method never throws <tt>java.io.IOError</tt>.
     *
     * @param path      the path to disk file (as the argument of <tt>new java.io.File(path)</tt>).
     * @param byteOrder the byte order that will be always used for mapping this file.
     * @return new instance of {@link DataFile} object.
     * @throws NullPointerException if one of the passed arguments is <tt>null</tt>.
     */
    public DataFile getDataFile(File path, ByteOrder byteOrder) {
        if (path == null)
            throw new NullPointerException("Null path argument");
        if (byteOrder == null)
            throw new NullPointerException("Null byteOrder argument");
        return new StandardIOFile(path, byteOrder, cacheReading, directBuffers);
    }

    /**
     * Returns the absolute path to the disk file (<tt>java.io.File.getAbsoluteFile()</tt>).
     * The argument may be created by this data file model or by {@link DefaultDataFileModel}.
     *
     * <p>This method never throws <tt>java.io.IOError</tt>.
     *
     * @param dataFile the data file.
     * @return the absolute path to the disk file.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     * @throws ClassCastException   if the data file was created by data file model, other than
     *                              {@link DefaultDataFileModel} or {@link StandardIODataFileModel}.
     */
    public File getPath(DataFile dataFile) {
        return ((DefaultDataFileModel.MappableFile) dataFile).file.getAbsoluteFile();
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
     * <tt>Integer.getInteger("net.algart.arrays.StandardIODataFileModel.numberOfBanks",&nbsp;32)</tt>,
     * stored while initializing this {@link StandardIODataFileModel} class,
     * or default value 32 if some exception occurred while calling <tt>Integer.getInteger</tt>.
     * If this value is less than 2, returns 2.
     *
     * @return the recommended number of memory banks.
     */
    @Override
    public int recommendedNumberOfBanks() {
        return STANDARD_IO_NUMBER_OF_BANKS;
    }

    /**
     * <p>This implementation returns the value
     * <tt>Integer.getInteger("net.algart.arrays.StandardIODataFileModel.bankSize",65536)</tt> (64&nbsp;KB)
     * (regardless of the argument),
     * stored while initializing {@link StandardIODataFileModel} class,
     * or default value 65536 if some exception occurred while calling <tt>Integer.getInteger</tt>.
     * If this property contains invalid value (for example, not a power of two),
     * this value is automatically corrected to the nearest valid one.
     *
     * @param unresizable ignored in this implementation.
     * @return the recommended size of every memory bank in bytes.
     */
    @Override
    public int recommendedBankSize(boolean unresizable) {
        return STANDARD_IO_BANK_SIZE;
    }

    /**
     * This implementation returns <tt>"stdmm"</tt>;
     *
     * @return <tt>"stdmm"</tt>.
     */
    @Override
    public String temporaryFilePrefix() {
        return "stdmm";
    }

    /**
     * Returns a brief string description of this class.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "standard I/O data file model: " + recommendedNumberOfBanks()
            + " banks per " + recommendedBankSize(false) + " bytes"
            + (directBuffers ? ", direct buffers" : ", Java heap buffers")
            + (cacheReading ? ", cached reading" : "");
    }

    /**
     * Reads all content of the byte buffer (<tt>dest.limit()</tt> bytes) from the given position
     * of the file channel.
     * Current positions in the file channel and byte buffer are ignored and not changed.
     *
     * <p>Unlike <tt>FileChannel.read(ByteBuffer dst, long position)</tt>, this method
     * <i>guarantees</i> that all <tt>dest.limit()</tt> bytes will be really read from the file channel.
     * (<tt>FileChannel.read</tt> method might not fill all buffer:
     * it returns the number of successfully read bytes.)
     * To provide this guarantee, this method performs a loop of calls of <tt>FileChannel.read</tt> method;
     * if it cannot load all bytes, it throws <tt>EOFException</tt>.
     *
     * <p>In a case of exception, the content of <tt>dest</tt> byte buffer can be partially changed
     * (loaded from the file). The file channel position and byte buffer positions are not changed
     * in any case.
     *
     * @param fileChannel the file channel.
     * @param position    the file position at which the transfer is to begin; must be non-negative.
     * @param dest        the destination byte buffer.
     * @throws IllegalArgumentException if the position is negative.
     * @throws EOFException             if the byte buffer cannot be fully read.
     * @throws IOException              in the same situations as <tt>FileChannel.read(ByteBuffer dst)</tt> method.
     */
    public static void readAllBuffer(FileChannel fileChannel, long position, ByteBuffer dest)
        throws IOException
    {
        if (position < 0)
            throw new IllegalArgumentException("Negative position");
        long savePosition = fileChannel.position();
        try {
            fileChannel.position(position);
            ByteBuffer dup = dest.duplicate();
            dup.rewind();
            for (int ofs = 0, k = 0, n = dup.limit(); ofs < n; k++) {
                if (k > 2 * n) { // too many attempts
                    throw new EOFException("Cannot read " + n + " bytes from the file at the position " + position);
                }
                int res = fileChannel.read(dup);
                if (res < 0)
                    throw new EOFException("Cannot read " + n + " bytes from the file at the position " + position
                        + ": file is exhausted");
                ofs += res;
            }
        } finally {
            fileChannel.position(savePosition);
        }
    }

    /**
     * Writes all content of the byte buffer (<tt>src.limit()</tt> bytes) to the given position
     * of the file channel.
     * Current positions in the file channel and byte buffer are ignored and not changed.
     *
     * <p>Unlike <tt>FileChannel.write(ByteBuffer dst, long position)</tt>, this method
     * <i>guarantees</i> that all <tt>src.limit()</tt> bytes will be really written to the file channel.
     * (<tt>FileChannel.write</tt> method might not write all buffer:
     * it returns the number of successfully written bytes.)
     * To provide this guarantee, this method performs a loop of calls of <tt>FileChannel.write</tt> method;
     * if it cannot write all bytes, it throws <tt>IOException</tt>.
     *
     * <p>In a case of exception, the content of the file can be partially changed
     * (written from the byte buffer). The file channel position and byte buffer positions are not changed
     * in any case.
     *
     * @param fileChannel the file channel.
     * @param position    the file position at which the transfer is to begin; must be non-negative.
     * @param src         the source byte buffer.
     * @throws IllegalArgumentException if the position is negative.
     * @throws IOException              in the same situations as <tt>FileChannel.write(ByteBuffer dst)</tt> method,
     *                                  and also if the byte buffer cannot be fully written.
     */
    public static void writeAllBuffer(FileChannel fileChannel, long position, ByteBuffer src)
        throws IOException
    {
        if (position < 0)
            throw new IllegalArgumentException("Negative position");
//        System.out.print("Writing... " + position + "..+" + src.limit());
//        long t1 = System.nanoTime();
        long savePosition = fileChannel.position();
        try {
            fileChannel.position(position);
            ByteBuffer dup = src.duplicate();
            dup.rewind();
            for (int ofs = 0, k = 0, n = src.limit(); ofs < n; k++) {
                if (k > 2 * n) { // too many attempts
                    throw new EOFException("Cannot write " + n + " bytes to the file at the position " + position);
                }
                int res = fileChannel.write(dup);
                if (res < 0)
                    throw new EOFException("Cannot write " + n + " bytes to the file at the position " + position
                        + ": the disk if probably full");
                ofs += res;
            }
        } finally {
            fileChannel.position(savePosition);
        }
//        long t2 = System.nanoTime();
//        System.out.println(" done: "
//            + src.limit() / 1048576.0 / ((t2 - t1) * 1e-9) + " MB/sec (" + fileChannel
//            + ") in " + Thread.currentThread());
    }

    private static ByteBuffer allocateWithSeveralAttempts(int capacity, boolean directBuffers) {
        long t1 = System.nanoTime();
        int numberOfAttempts = 0, numberOfGc = 0;
        Error error = null;
        ByteBuffer result = null;
        for (int count = ALLOCATE_NUMBER_OF_ATTEMPTS + ALLOCATE_NUMBER_OF_ATTEMPTS_WITH_GC; ; count--) {
            try {
                result = directBuffers ?
                    ByteBuffer.allocateDirect(capacity) :
                    ByteBuffer.allocate(capacity);
                break;
            } catch (Error e) { // probably OutOfMemoryError
                error = e;
            }
            numberOfAttempts++;
            if (count <= 0) {
                break;
            }
            boolean doGc = count <= ALLOCATE_NUMBER_OF_ATTEMPTS_WITH_GC;
            LargeMemoryModel.LOGGER.config(
                "SSSS allocate" + (directBuffers ? "Direct" : "") + ": problem with allocating memory, new attempt #"
                    + numberOfAttempts + (doGc ? " with gc" : ""));
            if (doGc) {
                System.gc();
                numberOfGc++;
            }
        }
        long t2 = System.nanoTime();
        if (result == null) {
            assert error != null;
            LargeMemoryModel.LOGGER.warning(String.format(Locale.US,
                "SSSS allocate" + (directBuffers ? "Direct" : "") + ": cannot allocate data in %.2f sec, "
                    + numberOfAttempts + " attempts" + (numberOfGc > 0 ? ", " + numberOfGc + " with gc" : "")
                    + " (%s)",
                (t2 - t1) * 1e-9, error));
            throw error;
        }
        return result;
    }

    static class StandardIOFile extends DefaultDataFileModel.MappableFile {
        private final List<ByteBuffer> unusedBuffersPool; // avoid creating extra byte buffers
        private final boolean cacheReading;
        private final boolean directBuffers;

        StandardIOFile(File file, ByteOrder byteOrder, boolean cacheReading, boolean directBuffers) {
            super(file, byteOrder, false);
            this.cacheReading = cacheReading;
            this.directBuffers = directBuffers;
            this.unusedBuffersPool = cacheReading ? null : new ArrayList<ByteBuffer>();
        }

        public void close() {
            try {
                super.close();
            } finally {
                if (!cacheReading)
                    unusedBuffersPool.clear(); // allow garbage collection for the pool
            }
        }

        public void force() {
        }

        public BufferHolder map(final Range range, final boolean notLoadDataFromFile) {
            assert range.length() == (int) range.length();
            ByteBuffer bb = null;
            DefaultDataFileModel.RangeWeakReference<ByteBuffer> br = null;
            if (cacheReading) {
                br = mappingCache.get(range);
                bb = br == null ? null : br.get();
            }
            boolean foundInCache = bb != null;
            if (foundInCache) {
//                System.out.println("SSSS quick loading " + br.key + " of " + file);
                LargeMemoryModel.LOGGER.finest("SSSS quick loading " + br.key + " of " + file);
            } else {
                if (cacheReading) {
                    reap();
                    bb = allocateWithSeveralAttempts((int) range.length(), directBuffers);
                } else {
                    int n;
                    while ((n = unusedBuffersPool.size()) > 0) {
                        ByteBuffer unusedBb = unusedBuffersPool.remove(n - 1);
                        if (unusedBb.limit() == range.length()) {
                            // to be on the safe side; current implementation of DataStorages
                            // never creates buffers with different sizes
                            bb = unusedBb;
                            bb.rewind(); // buffer position in stored empty buffer may be incorrect
                            break;
                        } // if not, this buffer will be lost
                    }
                    if (bb == null) {
                        bb = allocateWithSeveralAttempts((int) range.length(), directBuffers);
                    }
                }
                bb.order(byteOrder());
                if (!notLoadDataFromFile) {
//                    System.out.println("SSSS mapping " + range + ", " + range.length() / 1048576.0 + "m");
                    final ByteBuffer bbLocal = bb;
                    try {
                        Arrays.SystemSettings.globalDiskSynchronizer().doSynchronously(file.getPath(),
                            new Callable<Object>() {
                                public Object call() throws Exception {
                                    readAllBuffer(fc, range.position(), bbLocal);
                                    return null;
                                }
                            });
                    } catch (IOException ex) {
                        throw IOErrorJ5.getInstance(ex);
                    } catch (Exception ex) {
                        throw new AssertionError("Unexpected exception type: " + ex);
                    }
                    bb.rewind();
                }
                if (cacheReading) {
                    mappingCache.put(range, new DefaultDataFileModel.RangeWeakReference<ByteBuffer>(
                        bb, fileIndex, file.getPath(), range, reaped));
//                    System.out.println("SSSS caching " + range + " of " + file);
                    LargeMemoryModel.LOGGER.finest("SSSS caching " + range + " of " + file);
                }
            }
            return new DirectByteBufferHolder(bb, file.getPath(), fc, range, foundInCache, isReadOnly(),
                unusedBuffersPool);
        }

        // no necessity to override MappableFile.force() method here: it checks getClass() inside

        static class DirectByteBufferHolder implements DataFile.BufferHolder {
            private ByteBuffer bb;
            private final String fileName;
            private FileChannel fc;
            private final Range range;
            private final boolean fromCache;
            private final boolean readOnly;
            private final List<ByteBuffer> unusedBuffersPool;

            DirectByteBufferHolder(
                ByteBuffer bb, String fileName, FileChannel fc, Range range,
                boolean fromCache, boolean readOnly, List<ByteBuffer> unusedBuffersPool)
            {
                this.bb = bb;
                this.fileName = fileName;
                this.fc = fc;
                this.range = range;
                this.fromCache = fromCache;
                this.readOnly = readOnly;
                this.unusedBuffersPool = unusedBuffersPool;
            }

            public Range range() {
                return range;
            }

            public ByteBuffer data() {
                if (bb == null)
                    throw new IllegalStateException("Cannot call data() method: "
                        + "the buffer was already unmapped or disposed");
                return bb;
            }

            public Object mappingObject() {
                return null;
            }

            public void load() {
                if (bb == null)
                    throw new IllegalStateException("Cannot call load() method: "
                        + "the buffer was already unmapped or disposed");
                // nothing to do more
            }

            public void flush(boolean forcePhysicalWriting) {
                if (bb == null)
                    throw new IllegalStateException("Cannot call flush() method: "
                        + "the buffer was already unmapped or disposed");
                if (!readOnly) {
                    try {
//                        System.out.println("SSSS flushing " + this);
                        Arrays.SystemSettings.globalDiskSynchronizer().doSynchronously(fileName,
                            new Callable<Object>() {
                                public Object call() throws Exception {
                                    writeAllBuffer(fc, range.position(), bb);
                                    return null;
                                }
                            });
                    } catch (IOException ex) {
                        throw IOErrorJ5.getInstance(ex);
                    } catch (Exception ex) {
                        throw new AssertionError("Unexpected exception type: " + ex);
                    }
                }
            }

            public void unmap(boolean forcePhysicalWriting) {
                if (bb == null)
                    throw new IllegalStateException("Cannot call unmap() method: "
                        + "the buffer was already unmapped or disposed");
                try {
                    flush(forcePhysicalWriting);
                } finally {
                    if (unusedBuffersPool != null) {
                        unusedBuffersPool.add(bb); // for future usages
                    }
                    bb = null;
                    fc = null; // allowing garbage collection for FileChannel
                }
            }

            public boolean dispose() {
                if (bb == null)
                    throw new IllegalStateException("Cannot call dispose() method: "
                        + "the buffer was already unmapped or disposed");
                bb = null;
                fc = null; // allowing garbage collection for FileChannel
                return false;
            }

            public boolean isLoadedFromCache() {
                return fromCache;
            }

            public String toString() {
                return "block " + range + " of " + fileName + " [" + byteBufferToString(bb) + "]";
            }
        }
    }
}
