/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.nio.*;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * <p>A set of internal classes and static methods used for implementation of {@link DataStorage}
 * for a case of {@link LargeMemoryModel}.
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class MappedDataStorages {
    private MappedDataStorages() {}

    static final Set<MappedStorage> allNonFinalizedMappedStorages =
        Collections.synchronizedSet(new HashSet<MappedStorage>());

    private static boolean tempCleanerInstalled = false;
    static void installTempCleaner() {
        synchronized(LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion) {
            if (!tempCleanerInstalled) {
                InternalUtils.addShutdownTask(new TempCleaner(), null);
                tempCleanerInstalled = true;
            }
        }
    }

    // LOGGER methods may not work in shutdown hook;
    // in this case, we use system console instead
    private static void severeEvenInHook(String msg, Throwable thrown) {
        if (!shutdownInProgress) {
            LargeMemoryModel.LOGGER.log(Level.SEVERE, msg, thrown);
        }
        System.err.println("SEVERE: " + msg); // to be on the safe side, always double the message on console
        thrown.printStackTrace();
    }

    private static final boolean configLoggable = LargeMemoryModel.LOGGER.isLoggable(Level.CONFIG);
    private static void configEvenInHook(String msg) {
        if (!shutdownInProgress) {
            LargeMemoryModel.LOGGER.config(msg);
        } else if (configLoggable) {
            System.err.println("CONFIG: " + msg);
        }
    }

    private static final boolean warningLoggable = LargeMemoryModel.LOGGER.isLoggable(Level.WARNING);
    private static void warningEvenInHook(String msg) {
        if (!shutdownInProgress) {
            LargeMemoryModel.LOGGER.warning(msg);
        } else if (warningLoggable) {
            System.err.println("WARNING: " + msg);
        }
    }

    private static final boolean finestLoggable = LargeMemoryModel.LOGGER.isLoggable(Level.FINEST);
    private static void finestEvenInHook(String msg) {
        if (!shutdownInProgress) {
            LargeMemoryModel.LOGGER.finest(msg);
        } else if (finestLoggable) {
            System.out.println("FINEST: " + msg); // not System.err here!
        }
    }

    private static String finalizationTasksInfo() {
        return LargeMemoryModel.globalArrayFinalizer.activeTasksCount() + " array tasks, "
            + LargeMemoryModel.globalMappingFinalizer.activeTasksCount() + " mapping tasks, "
            + LargeMemoryModel.globalStorageFinalizer.activeTasksCount() + " storage tasks";
    }

    static volatile boolean shutdownInProgress = false;
    private static class TempCleaner implements Runnable {
        public void run() {
            shutdownInProgress = true;
            int lastNumberOfFiles = -1; // some impossible value
            for (int attemptCount = 1; attemptCount <= LargeMemoryModel.DELETION_LOOPS_IN_CLEANER; attemptCount++) {
                Set<MappedStorage> all;
                synchronized (allNonFinalizedMappedStorages) {
                    all = new HashSet<MappedStorage>(allNonFinalizedMappedStorages);
                }

                /* // for possible debugging
                Set<DataFileModel> models = LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion();
                for (DataFileModel<?> m : models) {
                    Set<DataFile> files = m.allTemporaryFiles();
                    System.err.println("   " + files.size() + " files in " + m);
                    // for (DataFile f : files) System.err.println("      " + f);
                }
                System.err.println("   TOTALLY " + all.size());
                */

                int failedCount = 0;
                for (MappedStorage mappedStorage : all) {
                    boolean ok = false;
                    try {
                        mappedStorage.dispose(MappedStorage.DisposeCaller.SHUTDOWN_HOOK);
                        ok = true;
                    } catch (Throwable ex) {
                        // this exception was already logged by dispose method
                    }
                    if (ok) {
                        allNonFinalizedMappedStorages.remove(mappedStorage);
                    } else {
                        failedCount++;
                    }
                }
                if (failedCount == 0)
                    break;
                int successfulCount = all.size() - failedCount;
                warningEvenInHook(failedCount + " storage files were not deleted in the attempt #" + attemptCount
                    + (successfulCount > 0 ? " (but " + successfulCount + " were deleted)" : "")
                    + "; we shall sleep for " + LargeMemoryModel.DELETION_SLEEP_DELAY + " ms and try again");
                if (all.size() == lastNumberOfFiles)
                    break;
                lastNumberOfFiles = all.size();
                // so, we shall repeat iterations, that cannot delete nothing, 2 times as a maximum
                try {
                    Thread.sleep(LargeMemoryModel.DELETION_SLEEP_DELAY);
                } catch (InterruptedException ex) {
                    // nothing to do here: it is the termination of application
                }
            }
        }

        public String toString() {
            return "Standard AlgART shutdown cleaner";
        }
    }

    private static boolean deleteWithSeveralAttempts(DataFileModel<?> dfm, DataFile df, int timeoutInMillis) {
        if (timeoutInMillis < 0)
            throw new IllegalArgumentException("Negative timeout");
        // We should not use System.currentTimeMillis() below, but need to count calls of Thread.sleep:
        // the system time, theoretically, can be changed during this loop
        for (; ; timeoutInMillis -= LargeMemoryModel.DELETION_SLEEP_DELAY) {
            if (timeoutInMillis <= 0) {
                return dfm.delete(df); // no catching: just return
            }
            Error error;
            try {
                return dfm.delete(df);
            } catch (Error e) {
                error = e;
            }
            try {
                Thread.sleep(Math.min(LargeMemoryModel.DELETION_SLEEP_DELAY, timeoutInMillis));
            } catch (InterruptedException ex) {
                throw error; // return the last exception if interrupted
            }
        }
    }

    static class MappingSettings<P> {
        final DataFileModel<P> dataFileModel; // necessary for deletion and LargeMemoryModel.getDataFileModel
        final DataFile existingDataFile; // may be null: it means it should be created as temporary
        final long dataFileStartOffset;
        final long dataFileEndOffset; // may be Long.MAX_VALUE
        final boolean temporary;
        final boolean readOnly;
        final boolean unresizable;
        final int numberOfBanks; // must be >=2
        final long bankSizeInElements; // must be 2^k (for bits - in bits, not in longs)
        final int bankSizeInElementsLog; // must be 2^k (for bits - in bits, not in longs)
        final int bankSizeInBytes;
        final int bankSizeInBytesLog;
        final PArray lazyFillingPattern;
        private final Class<?> arrayElementClass;

        private MappingSettings(
            DataFileModel<P> dataFileModel,
            DataFile dataFile, long dataFileStartOffset, long dataFileEndOffset,
            boolean temporary, boolean readOnly, boolean unresizable,
            Class<?> arrayElementClass, PArray lazyFillingPattern)
        {
            if (dataFileModel == null)
                throw new NullPointerException("Null dataFileModel argument");
            int numberOfBanks = dataFileModel.recommendedNumberOfBanks();
            int bankSizeInBytes = dataFileModel.recommendedBankSize(unresizable);
            checkBankArguments(numberOfBanks, bankSizeInBytes);
            this.dataFileModel = dataFileModel;
            this.existingDataFile = dataFile;
            this.dataFileStartOffset = dataFileStartOffset;
            this.dataFileEndOffset = dataFileEndOffset;
            this.temporary = temporary;
            this.readOnly = readOnly;
            this.unresizable = unresizable;
            this.numberOfBanks = numberOfBanks;
            this.bankSizeInBytes = bankSizeInBytes;
            this.bankSizeInBytesLog = 31 - Integer.numberOfLeadingZeros(bankSizeInBytes);
            assert this.bankSizeInBytes == 1 << this.bankSizeInBytesLog;
            this.arrayElementClass = arrayElementClass;
            this.lazyFillingPattern = lazyFillingPattern == null ? null : lazyFillingPattern.asImmutable();
            // asImmutable is necessary, in particular, to provide the fixed length in this.lazyFillingPattern
            if (lazyFillingPattern != null && arrayElementClass != lazyFillingPattern.elementType())
                throw new AssertionError("Illegal arrayElementClass argument: " + arrayElementClass
                    + " (must be equal to the element type of the pattern array)");
            this.bankSizeInElements = arrayElementClass == boolean.class ? ((long)bankSizeInBytes) << 3 :
                arrayElementClass == char.class ? bankSizeInBytes >> DataStorage.BYTES_PER_CHAR_LOG :
                arrayElementClass == byte.class ? bankSizeInBytes :
                arrayElementClass == short.class ? bankSizeInBytes >> DataStorage.BYTES_PER_SHORT_LOG :
                arrayElementClass == int.class ? bankSizeInBytes >> DataStorage.BYTES_PER_INT_LOG :
                arrayElementClass == long.class ? bankSizeInBytes >> DataStorage.BYTES_PER_LONG_LOG :
                arrayElementClass == float.class ? bankSizeInBytes >> DataStorage.BYTES_PER_FLOAT_LOG :
                arrayElementClass == double.class ? bankSizeInBytes >> DataStorage.BYTES_PER_DOUBLE_LOG :
                -1;
            if (bankSizeInElements == -1)
                throw new AssertionError("Illegal arrayElementClass argument: " + arrayElementClass);
            this.bankSizeInElementsLog = 63 - Long.numberOfLeadingZeros(bankSizeInElements);
            assert this.bankSizeInElements == 1L << this.bankSizeInElementsLog;
        }

        static <P> MappingSettings<P> getInstanceForExistingFile(
            DataFileModel<P> dataFileModel,
            DataFile dataFile, long dataFileStartOffset, long dataFileEndOffset,
            boolean readOnly,
            Class<?> arrayElementClass)
        {
            if (dataFile == null)
                throw new NullPointerException("Null dataFile argument");
            return new MappingSettings<P>(dataFileModel, dataFile, dataFileStartOffset, dataFileEndOffset,
                false, readOnly, true, arrayElementClass, null);
        }

        static <P> MappingSettings<P> getInstanceForTemporaryFile(
            DataFileModel<P> dataFileModel,
            Class<?> arrayElementClass, boolean unresizable, PArray lazyFillingPattern)
        {
            if (dataFileModel == null)
                throw new NullPointerException("Null dataFileModel argument");
            return new MappingSettings<P>(dataFileModel, null,
                Math.max(0, dataFileModel.recommendedPrefixSize()), Long.MAX_VALUE,
                true, false, unresizable, arrayElementClass, lazyFillingPattern);
        }

        MappingSettings<P> getCompatibleInstanceForNewTemporaryFile(boolean unresizable) {
            return new MappingSettings<P>(this.dataFileModel,
                null, this.dataFileStartOffset, Long.MAX_VALUE,
                true, false, unresizable, this.arrayElementClass, null);
        }

        void finalizationNotify(Object dataFilePath, boolean isApplicationShutdown) {
            this.dataFileModel.finalizationNotify(InternalUtils.<P>cast(dataFilePath), isApplicationShutdown);
        }

        static void checkBankArguments(int numberOfBanks, int bankSizeInBytes) {
            if (numberOfBanks < 2)
                throw new IllegalArgumentException("Number of banks should be 2 or greater");
            if (bankSizeInBytes < 256)
                throw new IllegalArgumentException("Bank size should be 256 or greater");
            if ((bankSizeInBytes & (bankSizeInBytes - 1)) != 0)
                throw new IllegalArgumentException("Bank size should be 2^k");
            // bankSizeInBytes is positive and 2^k, so, bankSizeInBytes<=2^30
        }

        static int nearestCorrectNumberOfBanks(int numberOfBanks) {
            return Math.max(numberOfBanks, 2);
        }

        static int nearestCorrectBankSize(int bankSizeInBytes) {
            return Integer.highestOneBit(Math.max(bankSizeInBytes, 256));
        }
    }

    /**
     * The complex implementation of data storage, based on mapped {@link DataFile data files}.
     * This implementation contains a swapping manager allowing to access large files by blocks.
     */
    static abstract class MappedStorage extends DataStorage {
        /**
         * Mapping settings used by this storage.
         */
        final MappingSettings<?> ms;

        /**
         * Will be this file automatically deleted while finalization or shutdown hook.
         */
        private volatile boolean autoDeleted;

        /**
         * High index bits: all other (low) index describes the position inside a bank.
         */
        long indexHighBits;

        /**
         * Current number of used banks: <tt>0..bh.length-1</tt>.
         * For all unused banks <tt>bh[k]==null</tt>.
         */
        private int bankCount = 0;

        /**
         * Current set of memory banks.
         */
        final DataFile.BufferHolder[] bh;

        /**
         * Specific buffers (ByteBuffer[], CharBuffer[], etc.) corresponding to bh[] elements.
         * <tt>null</tt> for empty banks.
         */
        final Buffer[] specBufs;
        // Should be final to avoid partial object intialization in shutdown hook!

        /**
         * The array element <tt>#bankPos[k]</tt> corresponds to
         * element (bit, char, etc.) <tt>#0</tt> in the bank <tt>#k</tt>.
         * Do not include subarray offsets.
         * -1 indicates empty banks.
         */
        final long[] bankPos;

        /**
         * Work memory for {@link #flushResources(long, long, boolean)} and {@link #freeResources(Array, boolean)}.
         */
        private final int[] bankIndexes;

        /**
         * All banks, starting since {@link MappingSettings#dataFileStartOffset},
         * are supposed to be lazy filled and are not loaded from the file,
         * if the corresponding bits in this array is 1.
         * It is the simple AlgART array, growing together with the storage.
         */
        private MutableBitArray lazyFillMapOfBanks;

        /**
         * All data since the <i>byte</i> <tt>#lazyFillPosInBytes</tt>
         * are supposed to be lazy filled and are not loaded from the file.
         * Start padding ({@link MappingSettings#dataFileStartOffset}) is included into this value:
         * it is the offset from the file begin, not from <tt>dataFileStartOffset</tt>.
         */
        private long lazyFillPosInBytes;

        /**
         * If <tt>true</tt>, only first and second banks will be used and all the file will be mapped always.
         * Is set in the constructor for existing files and in allocate method for temporary unresizable file.
         */
        boolean singleMapping = false;

        /**
         * <tt>true</tt> if all methods must be synchronized via {@link #lock}.
         * May be cleared to <tt>false</tt> by {@link #setSingleMappingModeIfNecessary()} method
         * (only in a case when {@link #singleMapping} is <tt>true</tt>).
         */
        boolean syncNecessary = true;

        /**
         * If {@link #syncNecessary} is <tt>false</tt>, this field is filled by a copy of the
         * only {@link #specBufs specific buffer}, used for file mapping, or by <tt>null</tt>
         * in a case of releasing resources, every time when the file is mapped or released
         * (in a synchronized block). Non-synchronized access method must check this field
         * to be sure that they "see" the actual, fully initialized buffer object.
         */
        volatile Object validSingleSpecBufForThisThread = new Object();

        /**
         * The number of non-finalized attached <tt>Array</tt> instances.
         * If 0, then the {@link #autoDeleted auto-deleted} file will be planned to be deleted.
         */
        private int arrayCounter = 0;

        /**
         * The number of non-finalized mappings.
         * If 0, the {@link #autoDeleted auto-deleted} file will be deleted (if <tt>arrayCounter==0</tt>).
         */
        private int mappingCounter = 0;

        /**
         * The number of ByteBuffers that are in use.
         * Used if UNSAFE_UNMAP_ON_EXIT to not try unmap files that are in use now.
         */
        private AtomicInteger mappingInUseCounter = new AtomicInteger(0);

        /**
         * Indicates whether <tt>mappingInUseCounter</tt> was overflown at least once.
         */
        private volatile boolean mappingInUseCounterOverflow = false;

        /**
         * The data file (an instance of {@link DataFile} class) used by this storage.
         * This field is filled in the constructor and set to <tt>null</tt>
         * before file deletion; no any other modifications of this field are not allowed.
         */
        private volatile DataFile dataFile = null;
        // volatile is necessary because this field is set to <tt>null</tt> while deletion a parralel finalization

        /**
         * This field is the same as {@link #dataFile} with the only exception:
         * if this data storage was finalized, but an error was occurred attemt to delete this file
         * while finalization, then {@link #dataFile} will be assigned to <tt>null</tt>,
         * but this field will not.
         */
        private volatile DataFile dataFileForDeletion = null;

        /**
         * This field is set to <tt>true</tt> after successful disposing via {@link #dispose()} method
         * or standard standard shutdown cleanup procedure. It informs finalizers that there is no
         * sense to try to delete the data file.
         */
        private boolean disposeCalled = false;

        /**
         * This flag informs that this storage was already attempted to be finalized,
         * but finalization failed due to some exceptions while file deletion.
         * For logging goals only. (Not used in current implementation.)
         */
        private volatile boolean deletionErrorWhileFinalization = false;

        /**
         * The number of failed attempts to delete a file while finalization.
         */
        private int countOfFailedDeletionsWhileFinalization = 0;

        /**
         * This flag informs whether {@link #dataFile} is <tt>null</tt> as a result of standard shutdown
         * cleanup procedure, but not as a result of direct calling {@link #dispose()} method.
         * For logging goals only. (Not used in current implementation.)
         */
        private volatile boolean deletedWhileShutdown = false;

        /**
         * The current length of {@link #dataFile}.
         */
        private long dataFileLength;

        /**
         * The name of {@link #dataFile}. Used for "postmortem" logging and for finalization notification
         * when dataFile is already null.
         */
        private final Object dataFilePath;

        /**
         * <tt>true</tt> if this storage is unresizable.
         * It is set by {@link #allocate} method, not in the constructor.
         */
        private boolean unresizable = false;

        /**
         * The lock used for most operations.
         */
        final ReentrantLock lock = new ReentrantLock();

        /**
         * The lock used for counters in finalizers only. Locked for very short time only.
         */
        private final ReentrantLock lockForGc = new ReentrantLock();

        private volatile Object finalizationHolder = null;

        private final boolean finerLoggable = LargeMemoryModel.LOGGER.isLoggable(Level.FINER);

        private final boolean finestLoggable = LargeMemoryModel.LOGGER.isLoggable(Level.FINEST);

        private final ArrayComparator bankOrderComparator = new ArrayComparator() {
            public boolean less(long firstIndex, long secondIndex) {
                int first = (int)firstIndex, second = (int)secondIndex;
                return bh[first] != null && (bh[second] == null || bankPos[first] < bankPos[second]);
            }
        };

        MappedStorage(MappingSettings<?> ms) {
            assert ms.temporary == (ms.existingDataFile == null) : "ms.temporary != (ms.existingDataFile == null)";
            if (ms.temporary)
                assert !ms.readOnly : "ms.readOnly cannot be true for temporary files";
            this.ms = ms;
            this.indexHighBits = ~(ms.bankSizeInElements - 1);
            this.bh = new DataFile.BufferHolder[ms.numberOfBanks];
            if (this instanceof MappedBitStorage)
                this.specBufs = new LongBuffer[ms.numberOfBanks];
            else if (this instanceof MappedByteStorage)
                this.specBufs = new ByteBuffer[ms.numberOfBanks];
            else if (this instanceof MappedCharStorage)
                this.specBufs = new CharBuffer[ms.numberOfBanks];
            else if (this instanceof MappedShortStorage)
                this.specBufs = new ShortBuffer[ms.numberOfBanks];
            else if (this instanceof MappedIntStorage)
                this.specBufs = new IntBuffer[ms.numberOfBanks];
            else if (this instanceof MappedLongStorage)
                this.specBufs = new LongBuffer[ms.numberOfBanks];
            else if (this instanceof MappedFloatStorage)
                this.specBufs = new FloatBuffer[ms.numberOfBanks];
            else if (this instanceof MappedDoubleStorage)
                this.specBufs = new DoubleBuffer[ms.numberOfBanks];
            else
                throw new AssertionError("Illegal inheritor " + this.getClass());
            this.bankPos = new long[ms.numberOfBanks];
            for (int k = 0; k < this.bankPos.length; k++)
                this.bankPos[k] = -1; // unmatched positions
            this.bankIndexes = new int[ms.numberOfBanks];
            final ReentrantLock lock = this.lock;
            lock.lock();
            // synchronization with possible accesses via allNonFinalizedMappedStorages
            synchronized(allNonFinalizedMappedStorages) {
                // atomic creating and deleting temporary files
                try {
                    if (shutdownInProgress)
                        throw IOErrorJ5.getInstance(new IllegalStateException(
                            "Cannot allocate new AlgART array: shutdown is in progress"));
                    if (!ms.temporary) {
                        this.dataFileForDeletion = this.dataFile = ms.existingDataFile;
                        this.unresizable = true;
                        this.dataFile.open(ms.readOnly);
                        // lazyFillPosInBytes will not be used
                    } else {
                        this.dataFileForDeletion = this.dataFile = ms.dataFileModel.createTemporary(ms.unresizable);
                        this.dataFile.open(false);
                        this.dataFile.length(ms.dataFileStartOffset);
                        // - if custom implementation of createTemporary has returned an old file,
                        // we truncate it here to avoid nonaligned lazyFillPosInBytes;
                        // if it has returned non-existing file (has not really create it),
                        // we create it by open(false)
                        this.lazyFillPosInBytes = ms.dataFileStartOffset;
                        this.lazyFillMapOfBanks = SimpleMemoryModel.getInstance().newEmptyBitArray(1024);
                        // - the capacity 1024 requires only 16 longs and already allows to check 2GB for 2MB banks/
                        // Note: it's capacity, not length! Starting size of the lazy map is 0.
                        LargeMemoryModel.LOGGER.fine("Temporary array storage file "
                            + dataFile + " is successfully created");
                    }
                    this.dataFilePath = ms.dataFileModel.getPath(this.dataFile);
                    this.dataFileLength = this.dataFile.length();
                    if (!ms.temporary) {
                        setSingleMappingModeIfNecessary();
                        // for temporary files, will be set later after setting capacity
                    }
                    this.autoDeleted = ms.temporary;
                    allNonFinalizedMappedStorages.add(this);
                } finally {
                    lock.unlock();
                }
            }
            scheduleFinalizationForAllStorage();
        }

        public boolean isAutoDeleted() {
            return this.autoDeleted;
        }

        public void setAutoDeleted(boolean value) {
            DataFile df = this.dataFile;
            this.autoDeleted = value;
            if (df != null)
                ms.dataFileModel.setTemporary(df, value);
        }

        public DataFileModel<?> getDataFileModel() {
            return ms.dataFileModel;
        }

        public Object getDataFilePath() {
            return ms.dataFileModel.getPath(dataFile);
        }

        public String toString() {
            return "mapped" + (ms.lazyFillingPattern == null ? "" : " lazy")
                + " file (" + dataFilePath
                + (dataFile == null ? ": DISPOSED" : "")
                + (isAutoDeleted() ? ", temporary)" : ", external)");
        }

        MemoryModel newCompatibleMemoryModel() {
            return LargeMemoryModel.<Object>getInstance(InternalUtils.<DataFileModel<Object>>cast(ms.dataFileModel));
            // - avoding unchecked warning: dataFileModel is generalized, but MappedStored is not
        }

        /**
         * Returns <tt>index&~indexHighBits</tt>.
         * The same offset will be returned by {@link #translateIndex(long)} method.
         *
         * @param index the index of some data element in the source array (not sub-array).
         * @return      the offset of this element in the bank.
         */
        final long offset(long index) {
            return index & ~indexHighBits; // not "~(ms.bankSizeInElements - 1)": single mapping possible
        }
        /**
         * Equivalent to {@link #translateIndex(long, boolean) translateIndex(index, false)}.
         *
         * @param index the index of the data element in the source array (not sub-array).
         * @return      the index of the data element in the bank #0.
         */
        final long translateIndex(long index) {
            if ((index & indexHighBits) == bankPos[0]) {
                assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                return index - bankPos[0];
            } else {
                return translateFailedIndex(index, false, LoadingMode.DEFAULT);
            }
        }

        /**
         * Returns the index of the element of the <b>first or second</b>
         * {@link #specBufs specific buffer}
         * (bank #0 or #1), depending on <tt>useSecondBank</tt> argument,
         * which contains the data element <tt>#index</tt>.
         * If this data element is not in the first / second bank,
         * changes order of banks and, if necessary, remaps the first / second bank to assure
         * that the element <tt>#index</tt> is in it.
         *
         * <p>If <tt>useSecondBank</tt> is <tt>true</tt>, the content of the bank #0
         * is not changed. So, you may call:<pre>
         * long i1 = translateIndex(index, false);
         * long i2 = translateIndex(index, true);
         * </pre>
         * and be sure that the <tt>i1</tt> will remain actual after the second "translateIndex".
         *
         * <p><b>Important: the reverse statement is not true!</b> If you call:<pre>
         * long i2 = translateIndex(index, true);
         * long i1 = translateIndex(index, false);
         * </pre>
         * then the second "translateIndex" may replace the bank #1 loaded by the first call.
         *
         * <p>For bits, returns the index of the bit
         * (it is the only case when it can be greater than 2^31).
         *
         * @param index         the index of the data element in the source array (not sub-array).
         * @param useSecondBank if <tt>true</tt>, works with bank #1 and doesn't correct bank #0;
         *                      else works with bank #0.
         * @return              the index of the data element in the bank #0 or #1.
         */
        final long translateIndex(long index, boolean useSecondBank) {
            int bank = useSecondBank ? 1 : 0;
            if ((index & indexHighBits) == bankPos[bank]) {
                assert this instanceof MappedBitStorage || index - bankPos[bank] <= Integer.MAX_VALUE;
                return index - bankPos[bank];
            } else {
                return translateFailedIndex(index, useSecondBank, LoadingMode.DEFAULT);
            }
        }

        /**
         * Equivalent to {@link #translateIndex(long)} that, when possible,
         * skips reading data from the data file: previous content
         * of the corresponding file block will be ignored.
         * Used for optimization of large multi-bank writing operations.
         * Never used in the {@link #singleMapping single mapping mode}.
         *
         * @param index the index of the data element in the source array (not sub-array).
         * @return      the index of the data element in the bank #0.
         */
        final long translateIndexWO(long index) {
            if ((index & indexHighBits) == bankPos[0]) {
                assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                return index - bankPos[0];
            } else {
                return translateFailedIndex(index, false, LoadingMode.NOT_LOAD_DATA_FROM_FILE);
            }
        }

        /**
         * Equivalent to {@link #translateIndex(long, boolean)} that, when possible,
         * skips reading data from the data file: previous content
         * of the corresponding file block will be ignored.
         * Used for optimization of large multi-bank writing operations.
         * Never used in the {@link #singleMapping single mapping mode}.
         *
         * @param index         the index of the data element in the source array (not sub-array).
         * @param useSecondBank if <tt>true</tt>, works with bank #1 and doesn't correct bank #0;
         *                      else works with bank #0.
         * @return              the index of the data element in the bank #0 or #1.
         */
        final long translateIndexWO(long index, boolean useSecondBank) {
            int bank = useSecondBank ? 1 : 0;
            if ((index & indexHighBits) == bankPos[bank]) {
                assert this instanceof MappedBitStorage || index - bankPos[bank] <= Integer.MAX_VALUE;
                return index - bankPos[bank];
            } else {
                return translateFailedIndex(index, useSecondBank, LoadingMode.NOT_LOAD_DATA_FROM_FILE);
            }
        }

        /**
         * Equivalent to {@link #translateIndex(long, boolean)} if <tt>notLoadDataFromFile</tt>
         * is <tt>false</tt> or {@link #translateIndexWO(long, boolean)} if it is <tt>true</tt>.
         *
         * @param index               the index of the data element in the source array (not sub-array).
         * @param useSecondBank       if <tt>true</tt>, works with bank #1 and doesn't correct bank #0;
         *                            else works with bank #0.
         * @param notLoadDataFromFile if <tt>true</tt> and there is no required bank in memory,
         *                            the data may be not really loaded from the data file: the old content
         *                            of the corresponding file block will be ignored.
         * @return                    the index of the data element in the bank #0 or #1.
         */
        final long translateIndex(long index, boolean useSecondBank, boolean notLoadDataFromFile) {
            int bank = useSecondBank ? 1 : 0;
            if ((index & indexHighBits) == bankPos[bank]) {
                assert this instanceof MappedBitStorage || index - bankPos[bank] <= Integer.MAX_VALUE;
                return index - bankPos[bank];
            } else {
                return translateFailedIndex(index, useSecondBank,
                    notLoadDataFromFile ? LoadingMode.NOT_LOAD_DATA_FROM_FILE : LoadingMode.DEFAULT);
            }
        }

        static enum LoadingMode {DEFAULT, PRELOAD_DATA_FROM_FILE, NOT_LOAD_DATA_FROM_FILE}
        /**
         * Reduced version of {@link #translateIndex(long, boolean)} and
         * {@link #translateIndexWO(long, boolean)} that works only
         * if the required element is not currently in the bank.
         * (Full method calls this one.)
         *
         * @param index          the index of the data element in the source array (not sub-array).
         * @param useSecondBank  if <tt>true</tt>, works with bank #1 and doesn't correct bank #0;
         *                       else works with bank #0.
         * @param loadingMode    if {@link LoadingMode#NOT_LOAD_DATA_FROM_FILE} and
         *                       there is no required bank in memory,
         *                       the data may be not really loaded from the data file: the old content
         *                       of the corresponding file block will be ignored;
         *                       if {@link LoadingMode#PRELOAD_DATA_FROM_FILE} and
         *                       there is no required bank in memory,
         *                       calls {@link DataFile.BufferHolder#load()} after loading the bank.
         * @return               the index of the data element in the bank #0 or #1.
         */
        final long translateFailedIndex(long index, boolean useSecondBank, LoadingMode loadingMode) {
            assert index >= 0 : "Negative index " + index;
            assert bh.length >= 2 : "Too low total number of banks: " + bh.length;
            final ReentrantLock lock = this.lock;
            lock.lock();
            // All corrections of bh, specBuf, bankPos and bankCount MUST be synchronized always,
            // even if this.syncNecessary was cleared: we still may call freeResourses that changes them.
            try {
                long result;
                final boolean firstTwoBanksAreSame, holeAt0;
                boolean bankWasLoaded = false;
                // The bank set is always supported in the following state:
                //     non-empty or null bank #0
                //     non-empty bank #1
                //     ...
                //     non-empty bank #bankCount-1
                //     null
                //     null
                //     ...
                //     null (at position #bh.length-1)
                // The first bank (#0) can be null (a "hole") if we load data in the second bank many times:
                // this operation, according the contract, does not destroy the bank #0
                // (though it could do it without any risk).
                //
                // The banks #0 and #1 may be the same - but only these ones.
                // All other bank pairs are always different.
                //
                // The unmap() method is always called for bh[bb.length-1] only, excepting the only case
                // of releaseFileAndMapping(), where it is called for all banks
                // (including #0, if it is not equal to #1).
                main: {
                    final int firstBank = useSecondBank ? 1 : 0;
                    if ((index & indexHighBits) == bankPos[firstBank]) {
                        // The index is not really failed. This situation is possible if !this.syncNecessary,
                        // when releaseFileAndMapping() is called and then two threads perform
                        // simultaneous non-synchronized access (for example, by translateIndex method):
                        // the first thread calls this method (translateFailedIndex),
                        // it restores the mapping, and then the second thread calls this method.
                        return index - bankPos[firstBank];
                    }
                    // Now we are sure that the required bank is not firstBank
                    int b = firstBank + 1;
                    long foundBankPos = -1;
                    DataFile.BufferHolder foundBh = null;
                    Buffer foundSpecBuf = null;
                    while (b < bankCount) {
                        foundBankPos = bankPos[b];
                        if ((index & indexHighBits) == foundBankPos) {
                            // It is a suitable bank.
                            foundBh = bh[b];
                            foundSpecBuf = specBufs[b];
                            break;
                            // If the bank was found, b is its index (AFTER firstBank).
                        }
                        b++;
                        // If the bank was not found, b is max(bankCount,firstBank+1).
                    }
                    // Usually, we now shift all banks #firstBank..#b-1 to positions #firstBank+1..#b,
                    // to free the position #firstBank, and then:
                    // A. unmap the last bank if the required bank was not found, or
                    // B. move the found bank #b to the required position #firstBank
                    //    ("rotating" array range #firstBank..#b)
                    firstTwoBanksAreSame = bankPos[0] == bankPos[1]; // (may be -1: empty banks)
                    holeAt0 = !useSecondBank && bh[0] == null;
                    if (firstTwoBanksAreSame
                        // However, it is a special situation: two identical (and unsuitable) buffers
                        // in banks #0 and #1.
                        // We guarantee that duplicates are possible only in banks #0 and #1:
                        // it's necessary to avoid prohibited twice unmapping (the case A).
                        // Now we just need to forget (replace) the bank #firstBank.
                        || holeAt0) {
                        // It is also a special situation: we need to place the required bank at index #0
                        // (firstBank=0), and here is a hole, so we don't need to free position #firstBank
                        if (foundBh != null) {
                            // The bank was found: it is foundBh. Storing it at #firstBank.
                            bh[firstBank] = foundBh;
                            specBufs[firstBank] = foundSpecBuf;
                            bankPos[firstBank] = foundBankPos;
                            // Now we MUST clear the found bank #b to avoid duplicating.
                            // To do this, we need to shift all array and reduce its length.
                            // Here bankCount >= firstBank+2 >= 2 (because we found the bank #b >= firstBank+1).
                            assert bankCount >= firstBank + 2 : "bankCount < firstBank + 2";
                            if (b == 1) {
                                // so, firstBank==0 and we've found required bank at #1: sitation with a hole at #0
                                assert firstBank == 0 : "firstBank != 0";
                                assert holeAt0 : "here should be hole at #0";
                                result = index - foundBankPos;
                                break main; // do not destroy the bank #1: let it stay a duplicate of #0
                            }
                            bankCount--;
                            for (; b < bankCount; b++) {
                                bh[b] = bh[b + 1];
                                specBufs[b] = specBufs[b + 1];
                                bankPos[b] = bankPos[b + 1];
                            }
                            bh[bankCount] = null;
                            specBufs[bankCount] = null;
                            bankPos[bankCount] = -1;
                            result = index - foundBankPos;
                        } else { // The bank was not found: just loading new bank at #firstBank
                            bankCount = Math.max(bankCount, firstBank + 1);
                            // bankCount must be corrected BEFORE loadBank that checks it
                            mapBank(index & indexHighBits, useSecondBank, loadingMode);
                            bankWasLoaded = true;
                            result = index - bankPos[firstBank];
                        }
                    } else {
                        // Ordinary situation: bank #0 and #1 are different,
                        // and (when firstBank=0) there is no "hole" at position #0.
                        // Now b = index of the found bank or, if not found, b = max(bankCount, firstBank+1).
                        assert foundBh != null || b == Math.max(bankCount, firstBank + 1) :
                            "bank not found, but b=" + b + " != max(bankCount,firstBank+1)=max("
                                + bankCount + "," + (firstBank + 1) + ")";
                        if (foundBh == null && b == bh.length) {
                            // The bank was not found, but the bank set is full: the last element must be removed.
                            if (unmapBank(--b, ReleaseMode.DATA_MUST_STAY_ALIVE, false)) {
                                --bankCount;
                                // we MUST unmap all lost buffers: some data file models will not work without it
                            }
                            assert b == bankCount;
                            assert b == bh.length - 1;
                        }
                        // Here it's possible that b=firstBank (not >=firstBank+1) even when foundBh=null,
                        // in the only situation when bb.length=2 and firstBank=1:
                        // we have unmapped the unsuitable bank #1 in the previous operation
                        // (or, maybe, bankCount was 1 and bank #1 was free).

                        // Shifting array: the last element or found bank #b will be replaced.
                        // So, popular banks will be always at the beginning of array.
                        for (; b > firstBank; b--) {
                            bh[b] = bh[b - 1];
                            specBufs[b] = specBufs[b - 1];
                            bankPos[b] = bankPos[b - 1];
                        }
                        // The first position now contains duplicate information.
                        if (foundBh != null) { // The bank was found: it is foundBh. Storing it at the first position.
                            bh[firstBank] = foundBh;
                            specBufs[firstBank] = foundSpecBuf;
                            bankPos[firstBank] = foundBankPos;
                            result = index - foundBankPos;
                            break main;
                        }
                        if (useSecondBank && (index & indexHighBits) == bankPos[0]) {
                            // We MUST use bank #0 in this case instead of the mapping:
                            // twice mapping may not work in some data file models.
                            bh[1] = bh[0];
                            bankPos[1] = bankPos[0];
                            specBufs[1] = specBufs[0];
                            bankCount++;
                            result = index - bankPos[1];
                            break main;
                        }
                        // There is no required bank in bh[] array.
                        bankCount++; // bankCount must be corrected BEFORE loadBank that checks it
                        mapBank(index & indexHighBits, useSecondBank, loadingMode);
                        bankWasLoaded = true;
                        result = index - bankPos[firstBank];
                    }
                } // main:
                assert result >= 0;
                assert this instanceof MappedBitStorage || result <= Integer.MAX_VALUE;
                AssertionError ae;
                assert!finestLoggable || bankWasLoaded || (ae = logAndCheckBanks(
                    index, useSecondBank,
                    firstTwoBanksAreSame ?
                    LogAllBanksCallPlace.translateFailedIndexSame :
                    holeAt0 ?
                    LogAllBanksCallPlace.translateFailedIndexHole :
                    LogAllBanksCallPlace.translateFailedIndex,
                    Level.FINEST)) == null:
                    ae.getMessage();
                // "assert" keyword allows to automatically remove this logging
                // if !finestLoggable (usual situation), logAndCheckBanks will not be called even if assertion enabled
                // due to the standard semantics of || operator in Java
                // if bankWasLoaded, logAndCheckBanks is not needed: it is called inside loadBank
                // logAllBanks always returns true, so no assertion error is possible here
                return result;
            } finally {
                lock.unlock();
            }
        }

        ByteOrder byteOrder() {
            return dataFile.byteOrder();
        }

        void allocate(long capacity, boolean unresizable) {
            lock.lock();
            try {
                changeCapacity(capacity, 0, 0, unresizable);
            } finally {
                lock.unlock();
            }
        }

        DataStorage changeCapacity(long newCapacity, long offset, long length) {
            lock.lock();
            try {
                changeCapacity(newCapacity, offset, length, false);
                return this;
            } finally {
                lock.unlock();
            }
        }

        //[[Repeat.SectionStart mapped_getData_method_impl]]
        void getData(long pos, Object destArray, int destArrayOffset, int count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: getDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            if (isBankLazyAndNotFilledYet(pos)) {
                getUninitializedLazyDataFromBank(pos, destArray, destArrayOffset, len);
            } else { //EndOfLazy !! this comment is necessary for preprocessing by Repeater !!
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    getDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                if (isBankLazyAndNotFilledYet(pos)) {
                    getUninitializedLazyDataFromBank(pos, destArray, destArrayOffset, len);
                } else { //EndOfLazy !! this comment is necessary for preprocessing by Repeater !!
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        getDataFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.SectionEnd mapped_getData_method_impl]]

        void setData(long pos, Object srcArray, int srcArrayOffset, int count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: setDataInFirstBank access the specBuf,
            // that can become null at any moment
            final long bse = ms.bankSizeInElements;
            long ofs = offset(pos);
            assert ofs >= 0;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            if (singleMapping || ofs > 0) { // in other case all tasks can be performed by the loop
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long o = translateIndex(pos);
                        assert o == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    setDataInFirstBank(buf, ofs, srcArray, srcArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
                srcArrayOffset += len;
                pos += len;
                count -= len;
            }
            for (; count > 0; pos += bse, srcArrayOffset += bse, count -= bse) {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        if (count >= bse) {
                            len = (int)bse;
                            ofs = translateIndexWO(pos);
                        } else {
                            len = count;
                            ofs = translateIndex(pos);
                        }
                        assert ofs == 0;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    setDataInFirstBank(buf, 0, srcArray, srcArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
        }

        void fillData(long pos, long count, Object fillerWrapper) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            lock.lock();
            // synchronization is necessary always: fillDataInFirstBank access the specBuf,
            // that can become null at any moment
            try {
                final long bse = ms.bankSizeInElements;
                long ofs = offset(pos);
                assert ofs >= 0;
                long len = singleMapping ? count : Math.min(count, bse - ofs);
                if (singleMapping || ofs > 0) { // in other case all tasks can be performed by the loop
                    long o = translateIndex(pos);
                    assert o == ofs;
                    fillDataInFirstBank(ofs, len, fillerWrapper);
                    pos += len;
                    count -= len;
                }
                for (; count > 0; pos += bse, count -= bse) {
                    if (count >= bse) {
                        len = bse;
                        ofs = translateIndexWO(pos);
                    } else {
                        len = count;
                        ofs = translateIndex(pos);
                    }
                    assert ofs == 0;
                    fillDataInFirstBank(0, len, fillerWrapper);
                }
            } finally {
                lock.unlock();
            }
        }

        boolean copy(DataStorage src, long srcPos, long destPos, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return true;
            if (src == this && srcPos == destPos)
                return true;
            if (!(src instanceof MappedStorage))
                return false;
            MappedStorage stor = (MappedStorage)src;
            boolean useSecondBank = src == this;
            // it is better to avoid extra mappings when possible
            lock.lock();
            stor.lock.lock();
            // synchronization is necessary always: copyFirstBank access the specBuf,
            // that can become null at any moment
            try {
                assert srcPos >= 0;
                assert destPos >= 0;
                assert count >= 0;
                long srcBlock = stor.singleMapping ? Long.MAX_VALUE : stor.ms.bankSizeInElements;
                long destBlock = singleMapping ? Long.MAX_VALUE : ms.bankSizeInElements;
                if (src == this && srcPos <= destPos && srcPos + count > destPos) {
                    // copy in reverse order
                    srcPos += count;
                    destPos += count;
                    long srcOfs = stor.translateIndex(srcPos - 1);
                    long destOfs = offset(destPos - 1);
                    long destO = translateIndex(destPos - 1, true, destOfs == destBlock - 1 && count >= destBlock);
                    assert destO == destOfs;
                    for (; ; ) {
                        assert srcOfs >= 0;
                        assert destOfs >= 0;
                        long srcLen = srcOfs + 1;
                        long destLen = destOfs + 1;
                        assert srcLen >= 0;
                        assert destLen >= 0;
                        if (srcLen < destLen) {
                            if (srcLen >= count) {
                                copyFirstBank(stor, srcLen - count, destLen - count, count, true, true);
                                break;
                            } else {
                                assert !stor.singleMapping;
                                copyFirstBank(stor, 0, destLen - srcLen, srcLen, true, true);
                                srcPos -= srcLen;
                                destPos -= srcLen;
                                count -= srcLen;
                                srcOfs = stor.translateIndex(srcPos - 1);
                                assert srcOfs == srcBlock - 1;
                                destOfs -= srcLen;
                                translateIndex(destPos, true);
                                // - previous translateIndex could destroy the second bank;
                                // this call should not lead to any mappings
                            }
                        } else {
                            if (destLen >= count) {
                                copyFirstBank(stor, srcLen - count, destLen - count, count, true, true);
                                break;
                            } else {
                                assert !singleMapping;
                                copyFirstBank(stor, srcLen - destLen, 0, destLen, true, true);
                                srcPos -= destLen;
                                destPos -= destLen;
                                count -= destLen;
                                if (srcLen == destLen)
                                    srcOfs = stor.translateIndex(srcPos - 1);
                                else
                                    srcOfs -= destLen;
                                destOfs = translateIndex(destPos - 1, true, count >= destBlock);
                                assert destOfs == destBlock - 1;
                            }
                        }
                    }
                } else {
                    // copy in normal order
                    long srcOfs = stor.translateIndex(srcPos);
                    long destOfs = offset(destPos);
                    long destO = translateIndex(destPos, useSecondBank, destOfs == 0 && count >= destBlock);
                    assert destO == destOfs;
                    for (; ; ) {
                        assert srcOfs >= 0;
                        assert destOfs >= 0;
                        long srcLen = srcBlock - srcOfs;
                        long destLen = destBlock - destOfs;
                        assert srcLen >= 0;
                        assert destLen >= 0;
                        if (srcLen < destLen) {
                            if (srcLen >= count) {
                                copyFirstBank(stor, srcOfs, destOfs, count, false, useSecondBank);
                                break;
                            } else {
                                assert !stor.singleMapping;
                                copyFirstBank(stor, srcOfs, destOfs, srcLen, false, useSecondBank);
                                srcPos += srcLen;
                                destPos += srcLen;
                                count -= srcLen;
                                srcOfs = stor.translateIndex(srcPos);
                                assert srcOfs == 0;
                                destOfs += srcLen;
                                if (useSecondBank)
                                    translateIndex(destPos, true);
                                // - previous translateIndex could destroy the second bank;
                                // this call should not lead to any mappings
                            }
                        } else {
                            if (destLen >= count) {
                                copyFirstBank(stor, srcOfs, destOfs, count, false, useSecondBank);
                                break;
                            } else {
                                assert !singleMapping;
                                copyFirstBank(stor, srcOfs, destOfs, destLen, false, useSecondBank);
                                srcPos += destLen;
                                destPos += destLen;
                                count -= destLen;
                                if (srcLen == destLen)
                                    srcOfs = stor.translateIndex(srcPos);
                                else
                                    srcOfs += destLen;
                                destOfs = translateIndex(destPos, useSecondBank, count >= destBlock);
                                assert destOfs == 0;
                            }
                        }
                    }
                }
            } finally {
                stor.lock.unlock();
                lock.unlock();
            }
            return true;
        }

        boolean swap(DataStorage another, long anotherPos, long thisPos, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return true;
            if (another == this && anotherPos == thisPos)
                return true;
            if (!(another instanceof MappedStorage))
                return false;
            MappedStorage stor = (MappedStorage)another;
            boolean useSecondBank = another == this;
            // it is better to avoid extra mappings when possible
            lock.lock();
            stor.lock.lock();
            // synchronization is necessary always: swapDataInFirstBank access the specBuf,
            // that can become null at any moment
            try {
                assert anotherPos >= 0;
                assert thisPos >= 0;
                assert count >= 0;
                long anotherBlock = stor.singleMapping ? Long.MAX_VALUE : stor.ms.bankSizeInElements;
                long thisBlock = singleMapping ? Long.MAX_VALUE : ms.bankSizeInElements;
                long anotherOfs = stor.translateIndex(anotherPos);
                long thisOfs = translateIndex(thisPos, useSecondBank);
                for (; ; ) {
                    assert anotherOfs >= 0;
                    assert thisOfs >= 0;
                    long anotherLen = anotherBlock - anotherOfs;
                    long thisLen = thisBlock - thisOfs;
                    assert anotherLen >= 0;
                    assert thisLen >= 0;
                    if (anotherLen < thisLen) {
                        if (anotherLen >= count) {
                            swapFirstBank(stor, anotherOfs, thisOfs, count, useSecondBank);
                            break;
                        } else {
                            assert !stor.singleMapping;
                            swapFirstBank(stor, anotherOfs, thisOfs, anotherLen, useSecondBank);
                            anotherPos += anotherLen;
                            thisPos += anotherLen;
                            count -= anotherLen;
                            anotherOfs = stor.translateIndex(anotherPos);
                            assert anotherOfs == 0;
                            thisOfs += anotherLen;
                            translateIndex(thisPos, true);
                            // - previous translateIndex could destroy the second bank
                        }
                    } else {
                        if (thisLen >= count) {
                            swapFirstBank(stor, anotherOfs, thisOfs, count, useSecondBank);
                            break;
                        } else {
                            assert !singleMapping;
                            swapFirstBank(stor, anotherOfs, thisOfs, thisLen, useSecondBank);
                            anotherPos += thisLen;
                            thisPos += thisLen;
                            count -= thisLen;
                            if (anotherLen == thisLen)
                                anotherOfs = stor.translateIndex(anotherPos);
                            else
                                anotherOfs += thisLen;
                            thisOfs = translateIndex(thisPos, useSecondBank);
                            assert thisOfs == 0;
                        }
                    }
                }
            } finally {
                stor.lock.unlock();
                lock.unlock();
            }
            return false;
        }

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getData_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getData ==> minData    !! Auto-generated: NOT EDIT !! ]]
        void minData(long pos, Object destArray, int destArrayOffset, int count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: minDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    minDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        minDataFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getData_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getData ==> maxData    !! Auto-generated: NOT EDIT !! ]]
        void maxData(long pos, Object destArray, int destArrayOffset, int count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: maxDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    maxDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        maxDataFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getData_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getData ==> addData;;
        //  Object\s+destArray ==> int[] destArray   !! Auto-generated: NOT EDIT !! ]]
        void addData(long pos, int[] destArray, int destArrayOffset, int count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: addDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    addDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        addDataFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getData_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getData ==> addData;;
        //  (,\s*int\s+count) ==> $1, double mult;;
        //  (,\s*len|,\s*bse\))(?=\);) ==> $1, mult;;
        //  Object\s+destArray ==> double[] destArray   !! Auto-generated: NOT EDIT !! ]]
        void addData(long pos, double[] destArray, int destArrayOffset, int count, double mult) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: addDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    addDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len, mult);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        addDataFromFirstBank(buf, 0, destArray, destArrayOffset, len, mult);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getData_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getData ==> subtractData;;
        //  (,\s*int\s+count) ==> $1, boolean truncateOverflows;;
        //  (,\s*len|,\s*bse\))(?=\);) ==> $1, truncateOverflows   !! Auto-generated: NOT EDIT !! ]]
        void subtractData(long pos, Object destArray, int destArrayOffset, int count, boolean truncateOverflows) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: subtractDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    subtractDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len, truncateOverflows);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        subtractDataFromFirstBank(buf, 0, destArray, destArrayOffset, len, truncateOverflows);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getData_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getData ==> absDiffData;;
        //  (,\s*int\s+count) ==> $1, boolean truncateOverflows;;
        //  (,\s*len|,\s*bse\))(?=\);) ==> $1, truncateOverflows   !! Auto-generated: NOT EDIT !! ]]
        void absDiffData(long pos, Object destArray, int destArrayOffset, int count, boolean truncateOverflows) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: absDiffDataFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            int len = singleMapping ? count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    Buffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = specBufs[0];
                    } finally {
                        lock.unlock();
                    }
                    absDiffDataFromFirstBank(buf, ofs, destArray, destArrayOffset, len, truncateOverflows);
                } finally {
                    decrementMappingInUseCounter();
                }
            }
            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        Buffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = specBufs[0];
                        } finally {
                            lock.unlock();
                        }
                        absDiffDataFromFirstBank(buf, 0, destArray, destArrayOffset, len, truncateOverflows);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        @Override
        void attachArray(Array a) {
            lockForGc.lock();
            try {
                arrayCounter++;
            } finally {
                lockForGc.unlock();
            }
            if (finerLoggable)
                LargeMemoryModel.LOGGER.finer("++++ Array " + Integer.toHexString(System.identityHashCode(a))
                    + " is attached to " + this + ": " + arrayCounter + " arrays, " + mappingCounter + " mappings"
                    + " (" + finalizationTasksInfo() + ") [" + a + "]");
        }

        @Override
        void forgetArray(int arrayIdentityHashCode) {
            decreaseArrayOrMappingCounter(CounterKind.ARRAY_COUNTER);
            if (finerLoggable)
                LargeMemoryModel.LOGGER.finer("---- Array " + Integer.toHexString(arrayIdentityHashCode)
                    + " is deattached from " + this + ": " + arrayCounter + " arrays, " + mappingCounter + " mappings"
                    + " (" + finalizationTasksInfo() + ")");
        }

        @Override
        void actualizeLazyFilling(ArrayContext context, long fromIndex, long toIndex) {
            if (fromIndex == toIndex) // necessary check: fromIndex can refer to the end of the file
                return;
            if (!ms.temporary)
                return; // so, we can access lazyFillMapOfBanks below
            final ReentrantLock lock = this.lock;
            final long bankFromIndex = fromIndex >> ms.bankSizeInElementsLog;
            final long bankToIndex = ((toIndex - 1) >>> ms.bankSizeInElementsLog) + 1;
            // - ">>>" here is used to avoid overflow
            final long bankAfterMapIndex = Math.min(bankToIndex, lazyFillMapOfBanks.length());
            final long lazyFillBankIndex = Math.max(bankAfterMapIndex,
                // - to be on the safe side: needless in current implementation
                (lazyFillPosInBytes - ms.dataFileStartOffset) >> ms.bankSizeInBytesLog);
            final long totalCount = Math.max(0, bankAfterMapIndex - bankFromIndex)
                + Math.max(0, bankToIndex - lazyFillBankIndex);
            for (long k = bankFromIndex; k < bankAfterMapIndex; k++) {
                boolean filled = false;
                lock.lock();
                try {
                    if (lazyFillMapOfBanks.getBit(k)) {
                        translateIndex(k << ms.bankSizeInElementsLog);
                        // This access guarantees that all elements in this bank will be initialized.
                        if (lazyFillMapOfBanks.getBit(k))
                            throw new AssertionError("The lazyFillMapOfBanks[" + k + "] bit was not cleared!");
                        filled = true;
                    }
                } finally {
                    lock.unlock();
                }
                if (context != null && (filled || k == bankToIndex - 1)) { // call context not too often
                    context.checkInterruptionAndUpdateProgress(ms.arrayElementClass,
                        (k - bankFromIndex) << ms.bankSizeInElementsLog,
                        totalCount << ms.bankSizeInElementsLog);
                    // we don't use the number of one-bits in lazyFillMapOfBanks to estimate the percents,
                    // because it may be changed a in parallel threads
                }
            }

            for (long k = lazyFillBankIndex; k < bankToIndex; k++) {
                lock.lock();
                try {
                    // After processing lazyFillMapOfBanks, it is better to actualize banks from the file start to end:
                    // OS will fill the file faster than if we will not access to the end of file at the beginning.
                    // Instead of this, we could access the last element at the beginning of this method,
                    // but the first writing even one bank to the end of file may require a lot of time.
                    translateIndex(k << ms.bankSizeInElementsLog);
                } finally {
                    lock.unlock();
                }
                if (context != null) {
                    context.checkInterruptionAndUpdateProgress(ms.arrayElementClass,
                        (Math.max(0, bankAfterMapIndex - bankFromIndex) + (k - lazyFillBankIndex))
                            << ms.bankSizeInElementsLog,
                        totalCount << ms.bankSizeInElementsLog);
                }
            }
        }

        /**
         * This implementation just loads 1 element at the specified position <tt>fromIndex</tt> and
         * calls {@link DataFile.BufferHolder#load()} for the bank containing this element.
         * The <tt>toIndex</tt> argument is ignored by this method, excepting the only case
         * when it is equal to <tt>fromIndex</tt> (when this method does nothing).
         *
         * @param fromIndex start index (inclusive) in the stored AlgART array (not sub-array).
         * @param toIndex   end index (exclusive) in the stored AlgART array (not sub-array).
         */
        @Override
        void loadResources(long fromIndex, long toIndex) {
            if (fromIndex == toIndex) // necessary check: fromIndex can refer to the end of the file
                return;
            assert fromIndex < toIndex;
            lock.lock(); // there are no sense to preload data of the same file from several threads simultaneously
            try {
                if ((fromIndex & indexHighBits) == bankPos[0]) {
                    assert this instanceof MappedBitStorage || fromIndex - bankPos[0] <= Integer.MAX_VALUE;
                } else {
                    translateFailedIndex(fromIndex, false, LoadingMode.DEFAULT);
                }
                bh[0].load();
            } finally {
                lock.unlock();
            }
        }

        @Override
        void flushResources(long fromIndex, long toIndex, boolean forcePhysicalWriting) {
            assert fromIndex <= toIndex;
            long t1 = System.nanoTime(), t2 = t1;
            final ReentrantLock lock = this.lock;
            int count = 0;
            lock.lock();
            try {
                checkIsDataFileDisposedOrShutdownInProgress();
                if (forcePhysicalWriting) {
                    dataFile.force();
                    // It's better to flush possible internal cache in dataFile BEFORE flushing bh:
                    // then all buffers, in a case of DefaultDataFileModel, will probably written in a good order.
                }
                t2 = System.nanoTime();
                for (int k = 0; k < bankIndexes.length; k++) {
                    bankIndexes[k] = k;
                }
                ArraySorter.getQuickSorter().sortIndexes(bankIndexes, 0, bankIndexes.length, bankOrderComparator);
                // sorting by increasing file offset
                for (int k : bankIndexes) {
                    if (bh[k] != null) {
                        long fromByte = this instanceof MappedBitStorage ?
                            fromIndex >>> 3 :
                            fromIndex << bytesPerBufferElementLog();
                        long toByte = this instanceof MappedBitStorage ?
                            (toIndex + 7) >>> 3 :
                            toIndex << bytesPerBufferElementLog();
                        DataFile.Range r = bh[k].range();
                        if (toByte <= r.position() || fromByte >= r.position() + r.length()) {
                            // bh[k] and the required region do not overlap
                            continue;
                        }
                        bh[k].flush(forcePhysicalWriting);
                        count++;
                    }
                }
            } finally {
                lock.unlock();
            }
            long t3 = System.nanoTime();
            LargeMemoryModel.LOGGER.fine(this + " is successfully flushed (" + count + " banks, "
                + String.format(Locale.US, "%.3f", (t2 - t1) * 1e-6) + " ms flushing data file, "
                + String.format(Locale.US, "%.3f", (t3 - t2) * 1e-6) + " ms flushing active banks)");
        }

        @Override
        void freeResources(Array a, boolean forcePhysicalWriting) {
            long t1 = System.nanoTime();
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                checkIsDataFileDisposedOrShutdownInProgress();
                if (forcePhysicalWriting) {
                    dataFile.force();
                    // It's better to flush possible internal cache in dataFile BEFORE flushing bh:
                    // then all buffers, in a case of DefaultDataFileModel, will probably written in a good order.
                }
                releaseFileAndMapping(ReleaseMode.DATA_MUST_STAY_ALIVE, forcePhysicalWriting);
                // unmap() calls in releaseFileAndMapping() performs
                // the same operations as in flushResources method
            } finally {
                lock.unlock();
            }
            long t2 = System.nanoTime();
            LargeMemoryModel.LOGGER.fine(this +" is successfully released ("
                + String.format(Locale.US, "%.3f", (t2 - t1) * 1e-6) + " ms)");
        }

        @Override
        void dispose() {
            dispose(DisposeCaller.DISPOSE_METHOD);
        }

        final void incrementMappingInUseCounter() {
            if (DefaultDataFileModel.UNSAFE_UNMAP_ON_EXIT) {
                int counter = mappingInUseCounter.incrementAndGet();
                if (counter < 0) // overflow
                    mappingInUseCounterOverflow = true;
            }
        }

        final void decrementMappingInUseCounter() {
            if (DefaultDataFileModel.UNSAFE_UNMAP_ON_EXIT) {
                mappingInUseCounter.decrementAndGet();
            }
        }

        final int mappingInUseCounter() {
            return mappingInUseCounterOverflow ? Integer.MAX_VALUE : mappingInUseCounter.get();
        }

        private static enum DisposeCaller {
            SHUTDOWN_HOOK("AlgART cleaner", LargeMemoryModel.DELETION_TIMEOUT_IN_CLEANER),
            DISPOSE_METHOD("dispose method", LargeMemoryModel.DELETION_TIMEOUT_IN_DISPOSE);

            private final String name;
            final int timeout;
            DisposeCaller(String name, int timeout) {
                this.name = name;
                this.timeout = timeout;
            }

            public String toString() {
                return name;
            }
        }

        private void dispose(DisposeCaller caller) {
            int timeout = caller.timeout;
            Object dfp = null;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                releaseFileAndMapping(ReleaseMode.DATA_MAY_BE_LOST, false);
                DataFile df = this.dataFileForDeletion;
                this.deletedWhileShutdown = caller == DisposeCaller.SHUTDOWN_HOOK;
                // deletedWhileShutdown is a volatile field: all other threads will see the reason correctly
                this.dataFile = null; // dataFile is a volatile field: block any access this storage
                if (df == null) // the storage is already disposed
                    return;
                dfp = ms.dataFileModel.getPath(df);
                if (autoDeleted) {
                    if (df instanceof DefaultDataFileModel.MappableFile
                        && !(df instanceof StandardIODataFileModel.StandardIOFile))
                    // there are no ways to override DefaultDataFileModel.MappableFile outside this package
                    {
                        boolean unmapped = false;
                        DefaultDataFileModel.MappableFile mdf = (DefaultDataFileModel.MappableFile)df;
                        if ((caller == DisposeCaller.SHUTDOWN_HOOK ?
                            DefaultDataFileModel.UNSAFE_UNMAP_ON_EXIT :
                            DefaultDataFileModel.UNSAFE_UNMAP_ON_DISPOSE)
                            && mdf.exists()) // not try to unmap already deleted files
                        {
                            if (mappingInUseCounter() != 0) {
                                warningEvenInHook(caller + " cannot unmap array storage file " + df
                                    + " (some ByteBuffer instances are in use now)");
                            } else {
                                try {
                                    long t1 = System.nanoTime();
                                    unmapped = mdf.unsafeUnmapAll();
                                    if (unmapped)
                                        configEvenInHook(
                                            caller + " has unmapped (unsafe operation) all regions in " + df + " ("
                                                + String.format(Locale.US, "%.3f", (System.nanoTime() - t1) * 1e-6) +
                                                " ms)");
                                    else
                                        warningEvenInHook(caller + " cannot unmap array storage file " + df
                                            + " (some arrays are not released yet)");
                                } catch (PrivilegedActionException ex) {
                                    if (caller == DisposeCaller.SHUTDOWN_HOOK) {
                                        severeEvenInHook(
                                            caller + " cannot perform unmapping (unsafe operation) in " + df, ex);
                                    } else {
                                        Error err = new InternalError("Error while calling undocumented "
                                            + "cleaning methods of DirectByteBuffer!");
                                        err.initCause(ex);
                                        throw err;
                                    }
                                }
                            }
                        }
                        if (caller == DisposeCaller.SHUTDOWN_HOOK && !unmapped)
                            timeout = 0; // if not using unsafe clean, timeout can extremely slow down JVM exit here
                    }

                    try {
                        if (deleteWithSeveralAttempts(ms.dataFileModel, df, timeout)) {
                            configEvenInHook(caller + " has successfully deleted temporary array storage file " + df);
                        } else {
                            configEvenInHook(caller + " has not found temporary array storage file " + df);
                        }
                    } catch (Throwable e) {
                        if (caller == DisposeCaller.SHUTDOWN_HOOK) {
                            configEvenInHook(caller + " cannot delete temporary array storage file " + df
                                + (caller.timeout > 0 ? " in " + caller.timeout + " ms" : "")
                                + " (" + e + ")");
                        }
                        Arrays.throwUncheckedException(e);
                    }
                }
                // - the next operators will not be performed in a case of exception while releasing or file deletion
                this.disposeCalled = true; // block deletion attempts while finalization
                this.dataFileForDeletion = null;
            } finally {
                lock.unlock();
            }
            // - the next operator will not be performed in a case of exception while releasing or file deletion
            if (caller == DisposeCaller.SHUTDOWN_HOOK && dfp != null)
                ms.finalizationNotify(dfp, true);
        }

        abstract void setSpecificBuffer(int bank);

        abstract void getDataFromFirstBank(Buffer buf,
            long firstBankOffset, Object destArray, int destArrayOffset, int count);

        abstract void setDataInFirstBank(Buffer buf,
            long firstBankOffset, Object srcArray, int srcArrayOffset, int count);

        abstract void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper);

        abstract void copyFirstBank(MappedStorage src,
            long srcOffset, long destOffset, long count, boolean reverseOrder, boolean copyToSecondBank);

        abstract void swapFirstBank(MappedStorage another,
            long anotherOffset, long thisOffset, long count, boolean swapWithSecondBank);

        abstract void minDataFromFirstBank(Buffer buf,
            long firstBankOffset, Object destArray, int destArrayOffset, int count);

        abstract void maxDataFromFirstBank(Buffer buf,
            long firstBankOffset, Object destArray, int destArrayOffset, int count);

        abstract void addDataFromFirstBank(Buffer buf,
            long firstBankOffset, int[] destArray, int destArrayOffset, int count);

        abstract void addDataFromFirstBank(Buffer buf,
            long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult);

        abstract void subtractDataFromFirstBank(Buffer buf,
            long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows);

        abstract void absDiffDataFromFirstBank(Buffer buf,
            long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows);

        final boolean isBankLazyAndNotFilledYet(long index) {
            if (!(DO_LAZY_INIT && ms.temporary))
                return false;
            index &= indexHighBits;
            final long mappingPosition = ms.dataFileStartOffset
                + (this instanceof MappedBitStorage ?
                index >>> 3 :
                index << bytesPerBufferElementLog());
            if (mappingPosition >= lazyFillPosInBytes)
                return true;
            final long bankIndex = singleMapping ? 0 : index >> ms.bankSizeInElementsLog;
            // if ms.temporary, then always lazyFillMapOfBanks != null
            if (bankIndex < lazyFillMapOfBanks.length() && lazyFillMapOfBanks.getBit(bankIndex))
                return true;
            return false;
        }

        void actualizeLazyFillingBank(ByteBuffer bankBB, long elementIndex) {
            // default implementation
            actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
        }

        final void actualizeLazyZeroFillingBank(ByteBuffer bankByteBuffer, int from, int to) {
            assert from <= to;
            JBuffers.fillByteBuffer(bankByteBuffer, from, to - from, ZERO_INIT_FILLER);
        }

        private void getUninitializedLazyDataFromBank(long pos,
            Object destArray, int destArrayOffset, int count)
        {
            long length = ms.lazyFillingPattern == null ? 0 : ms.lazyFillingPattern.length();
            if (count > length - pos) {
                int newCount = (int)(length < pos ? 0 : length - pos);
                // maybe count = 0, then newCount = 0 also
                JArrays.zeroFillArray(destArray, destArrayOffset + newCount, count - newCount);
                count = newCount;
            }
            if (count > 0) { // so, length > pos and ms.lazyFillingPattern != null
                ms.lazyFillingPattern.getData(pos, destArray, destArrayOffset, count);
            }
        }

        private void setSingleMappingModeIfNecessary() {
            if (this.unresizable && Math.min(dataFileLength, ms.dataFileEndOffset) <=
                ms.dataFileStartOffset + ms.dataFileModel.recommendedSingleMappingLimit())
            {
                lock.lock();
                try {
                    this.indexHighBits = 0; // always match!
                    this.singleMapping = true;
                    // Preloading banks here is not a good idea:
                    // they may be unloaded at any time by freeResources call,
                    // so we still should check bh[0/1]==null in getXxx/setXxx methods
                    if (!DefaultDataFileModel.UNSAFE_UNMAP_ON_EXIT) {
                        this.syncNecessary = false;
                    }
                } finally {
                    lock.unlock();
                }
                if (LargeMemoryModel.LOGGER.isLoggable(Level.FINE))
                    LargeMemoryModel.LOGGER.fine("Single mapping mode is set for data file " + dataFile
                        + " (its length " + dataFileLength
                        + " or the specified end offset " + ms.dataFileEndOffset
                        + " does not exceed the limit "
                        + ms.dataFileModel.recommendedSingleMappingLimit()
                        + (ms.dataFileStartOffset == 0 ? "" : " after start offset " + ms.dataFileStartOffset)
                        + ")");
            }
        }

        private void changeCapacity(
            final long newCapacity, final long offset, final long length,
            final boolean unresizable)
        {
            assert newCapacity >= 0;
            assert offset >= 0;
            assert length >= 0;
            assert lock.isHeldByCurrentThread() : "changeCapacity is called from non-synchronized code";
            if (this.unresizable)
                throw new InternalError("Internal error in Buffer/LargeMemoryModel implementation "
                    + "(unallowed changeCapacity)");
            final long requiredElements = offset + newCapacity;
            if (requiredElements < 0)
                throw new TooLargeArrayException("Too large desired capacity: "
                    + ((double)offset + (double)newCapacity) + " > 2^63-1");
            if (!(this instanceof MappedBitStorage) &&
                requiredElements > (Long.MAX_VALUE >> bytesPerBufferElementLog()))
                throw new TooLargeArrayException("Too large desired capacity: "
                    + requiredElements + " > 2^" + (63 - bytesPerBufferElementLog()) + "-1");
            long requiredBytes = this instanceof MappedBitStorage ?
                (requiredElements + 7) >>> 3 : // requiredElements + 7 can be negative here!
                requiredElements << bytesPerBufferElementLog();
            final long maxBytes = Long.MAX_VALUE - ms.dataFileStartOffset - ms.bankSizeInBytes + 1;
            // -2^31 < maxBytes < 2^63, 0 <= requiredBytes < 2^63
            if (requiredBytes > maxBytes)
                throw new TooLargeArrayException("Too large desired capacity in bytes: "
                    + ms.dataFileStartOffset + " + " + requiredBytes + " > "
                    + (Long.MAX_VALUE - ms.bankSizeInBytes + 1));
            // now Long.MAX_VALUE - ms.dataFileStartOffset >= maxBytes >= requiredBytes >= 0,
            // so, ms.dataFileStartOffset + requiredBytes is a correct long value
            if (unresizable)
                requiredBytes = (requiredBytes + 31) & ~31;
            else
                requiredBytes = (requiredBytes + ms.bankSizeInBytes - 1) & ~(ms.bankSizeInBytes - 1);
            final long newFileLength = ms.dataFileStartOffset + requiredBytes;
            final long oldFileLength = this.dataFileLength;
            boolean autoResizing = ms.dataFileModel.autoResizingOnMapping();
            if (newFileLength > oldFileLength) {
                checkIsDataFileDisposedOrShutdownInProgress();
                flushResources(0, Long.MAX_VALUE, false); // to be on the safe side
                dataFile.open(false);
                if (!autoResizing)
                    dataFile.length(newFileLength);
                this.dataFileLength = newFileLength;
            }
            if (newFileLength > oldFileLength ? finerLoggable : finestLoggable)
                LargeMemoryModel.LOGGER.log(newFileLength > oldFileLength ? Level.FINER : Level.FINEST,
                    "Data file " + dataFile
                    + (newFileLength <= oldFileLength ?
                    " is not resized (current length is "
                    + oldFileLength + "=0x" + Long.toHexString(oldFileLength)
                    + " bytes, required is "
                    + newFileLength + "=0x" + Long.toHexString(newFileLength) + " bytes)" :
                    autoResizing ?
                    " will be automatically resized from "
                    + oldFileLength + "=0x" + Long.toHexString(oldFileLength) + " to "
                    + newFileLength + "=0x" + Long.toHexString(newFileLength) + " bytes" :
                    " is resized from "
                    + oldFileLength + "=0x" + Long.toHexString(oldFileLength) + " to "
                    + newFileLength + "=0x" + Long.toHexString(newFileLength) + " bytes")
                    + " (desired capacity is " + newCapacity + ")");
            this.unresizable = unresizable;
            setSingleMappingModeIfNecessary();
        }

        private void mapBank(final long position,
            final boolean loadIntoSecondBank,
            final LoadingMode loadingMode)
        {
            assert (position & ~indexHighBits) == 0 : "loadBank is called for not-aligned position "
                + position + " (mask " + indexHighBits + "=0x" + Long.toHexString(indexHighBits) + ")";
            assert!singleMapping || position == 0;
            assert lock.isHeldByCurrentThread() : "mapBank is called from non-synchronized code";
            final int bank = loadIntoSecondBank ? 1 : 0;

            long t1 = 0;
            if (finerLoggable)
                t1 = System.nanoTime();
            checkIsDataFileDisposedOrShutdownInProgress();
            dataFile.open(ms.readOnly);
            final long mappingPosition = ms.dataFileStartOffset
                + (this instanceof MappedBitStorage ?
                position >>> 3 :
                position << bytesPerBufferElementLog());
            final long mappingSize = singleMapping ?
                Math.min(dataFileLength, ms.dataFileEndOffset) - ms.dataFileStartOffset :
                Math.min(ms.bankSizeInBytes,
                Math.min(dataFileLength, ms.dataFileEndOffset) - mappingPosition);

            assert mappingPosition >= 0 && mappingSize > 0 && mappingSize <= Integer.MAX_VALUE:
                "Illegal mappingPosition " + mappingPosition + " or mappingSize " + mappingSize
                + " (position = " + position + ", bankSizeInBytes = " + ms.bankSizeInBytes
                + ", dataFileLength = " + dataFileLength + ", dataFileStartOffset = " + ms.dataFileStartOffset
                + ", singleMapping = " + singleMapping + ")";
            assert ((mappingPosition - ms.dataFileStartOffset) & (ms.bankSizeInBytes - 1)) == 0:
                "non-aligned mapping position "
                + mappingPosition + " (bank size in bytes is " + ms.bankSizeInBytes
                + "=0x" + Long.toHexString(ms.bankSizeInBytes) + ")";
            if (ms.temporary) {
                assert ((lazyFillPosInBytes - ms.dataFileStartOffset) & (ms.bankSizeInBytes - 1)) == 0:
                    "non-aligned lazyFillPosInBytes-ms.dataFileStartOffset="
                    + (lazyFillPosInBytes - ms.dataFileStartOffset)
                    + " (bank size in bytes is " + ms.bankSizeInBytes
                    + "=0x" + Long.toHexString(ms.bankSizeInBytes) + ")";
                assert ms.dataFileStartOffset + (lazyFillMapOfBanks.length() << ms.bankSizeInBytesLog)
                    <= lazyFillPosInBytes:
                    "too large lazyFillMapOfBanks: " + lazyFillMapOfBanks.length() + " bits"
                    + " (bank size in bytes is " + ms.bankSizeInBytes
                    + "=0x" + Long.toHexString(ms.bankSizeInBytes) + ")";
            }
            final long thisBankIndex;
            if (DO_LAZY_INIT && ms.temporary && !singleMapping) {
                thisBankIndex = (mappingPosition - ms.dataFileStartOffset) >> ms.bankSizeInBytesLog;
            } else {
                thisBankIndex = 0;
            }
            if (DO_LAZY_INIT && ms.temporary && lazyFillPosInBytes < mappingPosition) {
                t1 = System.nanoTime();
                assert !singleMapping : "illegal singleMapping with low lazyFillPosInBytes=" + lazyFillPosInBytes;
                // - when singleMapping, position=0 always: see asserts above
                final long needfulFillMapSize = thisBankIndex;
                assert lazyFillMapOfBanks.length() < needfulFillMapSize:
                    "illegal lazyFillMapOfBanks.length()=" + lazyFillMapOfBanks.length();
                // - because lazyFillPosInBytes < mappingPosition: see the assert above
                final long newFillMapSize = Math.min(needfulFillMapSize,
                    LargeMemoryModel.MAX_NUMBER_OF_BANKS_IN_LAZY_FILL_MAP);
                lazyFillMapOfBanks.length(newFillMapSize);
                final long lazyFillBankIndex = (lazyFillPosInBytes - ms.dataFileStartOffset) >> ms.bankSizeInBytesLog;
                if (lazyFillBankIndex < newFillMapSize) {
                    // System.err.println("Request for filling " + lazyFillBankIndex + ".." + newFillMapSize);
                    lazyFillMapOfBanks.subArray(lazyFillBankIndex, newFillMapSize).fill(true);
                    // request for future lazy filling
                }
                lazyFillPosInBytes = Math.max(lazyFillPosInBytes,
                    ms.dataFileStartOffset + (newFillMapSize << ms.bankSizeInBytesLog));
                final long lazyFillPos = this instanceof MappedBitStorage ?
                    (lazyFillPosInBytes - ms.dataFileStartOffset) << 3 :
                    (lazyFillPosInBytes - ms.dataFileStartOffset) >> bytesPerBufferElementLog();
                assert lazyFillPos >= 0:
                    "negative array index " + lazyFillPos
                    + ", corresponding to lazy fill position " + lazyFillPosInBytes;
                for (long p = lazyFillPos, mp = lazyFillPosInBytes;
                    mp < mappingPosition;
                    p += ms.bankSizeInElements, mp += ms.bankSizeInBytes)
                    // direct filling: cannot do it in future due to too small lazyFillMapOfBanks
                {
//                    System.err.println("Filling " + p + "/" + mp);
                    assert newFillMapSize < needfulFillMapSize : "extra lazy filling loop";
                    assert p < position : "p>=position";
//                    System.out.printf("\r%d: %d / %d", mp/ms.bankSizeInBytes, mp/1048576, mappingPosition/1048576);
                    DataFile.BufferHolder tempBh = dataFile.map(DataFile.Range.valueOf(mp, ms.bankSizeInBytes), true);
                    // this bank was never loaded before; so, we can ignore standard translateFailedIndex logic
                    scheduleFinalizationForMapping(tempBh);
                    actualizeLazyFillingBank(tempBh.data(), p);
                    // p is passed only for a case of filling by the pattern array,
                    // to inform where the necessary fragment of the pattern begins
                    tempBh.unmap(false);
                    checkIsDataFileDisposedOrShutdownInProgress(); // we need to leave long synchronized block
                }
                long t2 = System.nanoTime();
                if (finerLoggable) {
                    LargeMemoryModel.LOGGER.finer(
                        "**** The region #" + Long.toHexString(lazyFillPosInBytes)
                            + "h.." + Long.toHexString(mappingPosition)
                            + "h of " + dataFilePath
                            + "is cleared (" + String.format(Locale.US, "%.3f", (t2 - t1) * 1e-6) + " ms)"
                            + " (" + finalizationTasksInfo() + ")");
                }
                t1 = t2;
            }
            boolean lazyFillBit = false;
            final boolean thisBankMustBeFilled = DO_LAZY_INIT && ms.temporary
                && (mappingPosition >= lazyFillPosInBytes ||
                (lazyFillBit = thisBankIndex < lazyFillMapOfBanks.length() && lazyFillMapOfBanks.getBit(thisBankIndex)));
            // if ms.temporary, then always lazyFillMapOfBanks != null
            bh[bank] = dataFile.map(
                DataFile.Range.valueOf(mappingPosition, mappingSize),
                loadingMode == LoadingMode.NOT_LOAD_DATA_FROM_FILE || thisBankMustBeFilled);
            if (loadingMode == LoadingMode.PRELOAD_DATA_FROM_FILE) {
                bh[bank].load();
            }
            if (thisBankMustBeFilled) {
//                System.err.println("Filling " + position);
                actualizeLazyFillingBank(bh[bank].data(), position);
                if (lazyFillBit)
                    lazyFillMapOfBanks.clearBit(thisBankIndex);
            }
            lazyFillPosInBytes = Math.max(lazyFillPosInBytes, mappingPosition + ms.bankSizeInBytes);

            bankPos[bank] = position;
            setSpecificBuffer(bank);
            if (!syncNecessary) {
                assert singleMapping;
                // in this case, only two banks 0/1 are used and bh[0]/bh[1] are always set to the same value
                validSingleSpecBufForThisThread = specBufs[bank];
                assert validSingleSpecBufForThisThread != null;
            }

            final boolean fromCache = bh[bank].isLoadedFromCache();
            final boolean loggable = fromCache ? finestLoggable : finerLoggable;
            final int id = System.identityHashCode(bh[bank].mappingObject());
            if (loggable) {
                long t2 = System.nanoTime();
                LargeMemoryModel.LOGGER.log(fromCache ? Level.FINEST : Level.FINER,
                    "**** The bank #" + bank + " is mapped"
                        + (fromCache ?
                        " (quickly): " + bh[bank]
                        : ": " + bh[bank] + " @" + Integer.toHexString(id))
                        + " (" + String.format(Locale.US, "%.3f", (t2 - t1) * 1e-6) + " ms)"
                        + " (" + finalizationTasksInfo() + ")");
            }
            AssertionError ae = logAndCheckBanks(position, loadIntoSecondBank,
                LogAllBanksCallPlace.loadBank, fromCache ? Level.FINEST : Level.FINER);
            if (ae != null)
                throw ae;
            scheduleFinalizationForMapping(bh[bank]);
            // the following call is necessary when bh[bank].mappingObject() is null, as in StandardIODataFileModel
            scheduleFinalizationForAllStorage();
        }

        private static enum ReleaseMode {DATA_MUST_STAY_ALIVE, DATA_MAY_BE_LOST}
        // Returns false if bh[bank] is empty (null)
        private boolean unmapBank(int bank, ReleaseMode mode, boolean forcePhysicalWriting) {
            // may be called from non-synchronized code: finalizing by decreaseArrayOrMappingCounter
            DataFile.BufferHolder tempBh = bh[bank];
            if (tempBh != null) {
                long t1 = 0;
                if (finerLoggable)
                    t1 = System.nanoTime();
                if (mode == ReleaseMode.DATA_MUST_STAY_ALIVE) {
                    tempBh.unmap(forcePhysicalWriting);
                } else {
                    tempBh.dispose();
                }
                bh[bank] = null; // allows garbage collector to finalize this buffer
                specBufs[bank] = null; // necessary for quick brange in getXxx/setXxx
                if (finerLoggable) {
                    long t2 = System.nanoTime();
                    LargeMemoryModel.LOGGER.finer(
                        "**** The bank #" + bank + ": " + tempBh + " is "
                            + (mode == ReleaseMode.DATA_MUST_STAY_ALIVE ? "unmapped (" : "disposed (")
                            + String.format(Locale.US, "%.3f", (t2 - t1) * 1e-6) + " ms)");
                }
                return true;
            } else {
                return false;
            }
        }

        private void scheduleFinalizationForMapping(DataFile.BufferHolder bufferHolder) {
            Object checkedData = bufferHolder.mappingObject();
            if (checkedData != null) {
                boolean loggable = bufferHolder.isLoadedFromCache() ? finestLoggable : finerLoggable;
                final String bhToString = loggable ? bufferHolder.toString() : null;
                final int id = System.identityHashCode(checkedData);
                lockForGc.lock();
                try {
                    mappingCounter++;
                } finally {
                    lockForGc.unlock();
                }
                LargeMemoryModel.globalMappingFinalizer.invokeOnDeallocation(checkedData, new Runnable() {
                    public void run() {
                        if (bhToString != null) { // i.e. if loggable
                            LargeMemoryModel.LOGGER.finer("~~~~ " + bhToString + " @"
                                + Integer.toHexString(id) + " is deallocated"
                                + " (" + finalizationTasksInfo() + ")");
                        }
                        decreaseArrayOrMappingCounter(CounterKind.MAPPING_COUNTER);
                    }
                });
            }
        }

        private void scheduleFinalizationForAllStorage() {
            if (finalizationHolder == null) {
                finalizationHolder = new Object();
                lockForGc.lock();
                try {
                    mappingCounter++;
                } finally {
                    lockForGc.unlock();
                }
                LargeMemoryModel.LOGGER.fine("FF++ " + this + ": finalization is scheduled"
                    + " (" + finalizationTasksInfo() + ")");
                LargeMemoryModel.globalStorageFinalizer.invokeOnDeallocation(finalizationHolder, new Runnable() {
                    public void run() {
                        LargeMemoryModel.LOGGER.fine("FF-- " + this + " is released"
                            + " (" + finalizationTasksInfo() + ")");
                        decreaseArrayOrMappingCounter(CounterKind.MAPPING_COUNTER);
                    }
                });
            }
        }

        private static enum LogAllBanksCallPlace {
            translateFailedIndex, translateFailedIndexSame, translateFailedIndexHole, loadBank
        }

        private AssertionError logAndCheckBanks(long index, boolean useSecondBank,
            LogAllBanksCallPlace whereCalled, Level level)
        {
            String msg = null;
            errorCheck: {
                if (bankCount < 0 || bankCount > bh.length) {
                    msg = "invalid bankCount = " + bankCount;
                    break errorCheck;
                }
                for (int k = 1; k < bankCount; k++) { // the bank #0, but only it, can ne empty when bankCount>0
                    if (bh[k] == null) {
                        msg = "bh[" + k + "] is null, but "
                            + (bankPos[k] != -1 ? "bankPos[" + k + "] != -1" : "bankCount is " + bankCount);
                        break errorCheck;
                    }
                    if (bankPos[k] == -1) {
                        msg = "bankPos[" + k + "] is -1, but "
                            + (bh[k] != null ? "bh[" + k + "] != null" : "bankCount is " + bankCount);
                        break errorCheck;
                    }
                }
                for (int k = bankCount; k < bh.length; k++) {
                    if (bh[k] != null) {
                        msg = "bh[" + k + "] is not null, but bankCount is " + bankCount;
                        break errorCheck;
                    }
                    if (bankPos[k] != -1) {
                        msg = "bankPos[" + k + "] is not -1, but bankCount is " + bankCount;
                        break errorCheck;
                    }
                }
                for (int k = 0; k < bankCount; k++) {
                    if (k >= 2 && bankPos[k] != -1) {
                        for (int j = 0; j < k; j++) {
                            if (bankPos[k] == bankPos[j]) {
                                msg = "banks #" + j + " and #" + k + " are duplicates";
                                break errorCheck;
                            }
                        }
                    }
                }
            }
            AssertionError result = null;
            if (msg != null) {
                result = new AssertionError("Internal error found in " + whereCalled + ": " + msg);
                level = Level.SEVERE;
            }
            if (LargeMemoryModel.LOGGER.isLoggable(level)) {
                StringBuilder sb = new StringBuilder(bankCount + " banks, accessing index "
                    + Long.toHexString(index) + "h");
                for (int k = 0; k < (result == null ? bankCount : bh.length); k++) {
                    sb.append(InternalUtils.LF
                        + "    [" + whereCalled + "] mapping "
                        + (useSecondBank ? "(at second bank) " : "")
                        + k + ": " + bh[k]);
                }
                sb.append(InternalUtils.LF + "The reason:");
                StackTraceElement[] se = Thread.currentThread().getStackTrace();
                for (int k = 2; k < se.length; k++)
                    sb.append(InternalUtils.LF + "    " + se[k]);
                if (result == null)
                    LargeMemoryModel.LOGGER.log(level, sb.toString());
                else
                    LargeMemoryModel.LOGGER.log(level, sb.toString(), result);
            }
            return result;
        }

        private void releaseFileAndMapping(ReleaseMode mode, boolean forcePhysicalWriting) {
            assert lock.isHeldByCurrentThread() : "releaseFileAndMapping is called from non-synchronized code";
            // Here mode can be equal to DATA_MAY_BE_LOST only in finalization/cleaner,
            // that always means that the file is temporary,
            // or as a result of direct dispose() call,
            // that does not need to write something into the file, because it will be deleted now
            long t1 = System.nanoTime();
            int count = 0;
            int bStart = 0;
            if (!syncNecessary) {
                validSingleSpecBufForThisThread = null;
            }
            if (bankPos[0] == bankPos[1]) {
                bankPos[0] = -1;
                bh[0] = null;
                specBufs[0] = null;
                bStart++;
            }
            for (int k = 0; k < bankIndexes.length; k++) {
                bankIndexes[k] = k;
            }
            ArraySorter.getQuickSorter().sortIndexes(bankIndexes, bStart, bankIndexes.length, bankOrderComparator);
            // sorting by increasing file offset
            for (int b = bStart; b < bh.length; b++) {
                int k = bankIndexes[b];
                assert k >= bStart && k < bh.length;
                bankPos[k] = -1;
                if (unmapBank(k, mode, forcePhysicalWriting)) {
                    count++;
                }
            }
            finalizationHolder = null;
            bankCount = 0;
            long t2 = System.nanoTime();
            if (count > 0 && finerLoggable) {
                LargeMemoryModel.LOGGER.finer("All buffers are released (" + count + " banks, "
                    + String.format(Locale.US, "%.3f", (t2 - t1) * 1e-6) + " ms)");
            }
            DataFile df = this.dataFile;
            if (df != null) { // can be null, if dispose() was called
                df.close();
                if (finerLoggable)
                    LargeMemoryModel.LOGGER.finer("Data file " + df + " is closed");
            }
        }

        private void checkIsDataFileDisposedOrShutdownInProgress() {
            if (shutdownInProgress) {
                throw IOErrorJ5.getInstance(new IllegalStateException(
                    "AlgART array @<" + this + "> is inaccessible: system shutdown in progress"));
            }
            if (dataFile == null) // for a case when the file was disposed via dispose() method
                throw IOErrorJ5.getInstance(new IllegalStateException(
                    "AlgART array @<" + this + "> is inaccessible: the storage was already disposed and deleted"));
        }

        private static enum CounterKind {ARRAY_COUNTER, MAPPING_COUNTER}
        private void decreaseArrayOrMappingCounter(CounterKind counterKind) {
            DataFile df = null;
            Object dfp = null;
            boolean needToClearBanks = false;
            boolean needToDeleteFile = false;
            boolean needToForgetStorage = false;
            lockForGc.lock();
            try {
                if (counterKind == CounterKind.ARRAY_COUNTER)
                    arrayCounter--;
                if (arrayCounter < 0)
                    throw new AssertionError("Negative number of attached arrays!");
                if (counterKind == CounterKind.MAPPING_COUNTER)
                    mappingCounter--;
                if (mappingCounter < 0)
                    throw new AssertionError("Negative number of mappings!");
                if (counterKind == CounterKind.ARRAY_COUNTER && arrayCounter == 0 && mappingCounter > 0)
                    needToClearBanks = true;
                if (arrayCounter == 0 && mappingCounter == 0) {
                    df = this.dataFileForDeletion;
                    dfp = this.dataFilePath;
                    // can be already null, if dispose() was called and successfully performed
                    this.dataFile = null;
                    needToDeleteFile = autoDeleted && df != null;
                    needToForgetStorage = true;
                }
                if (finerLoggable)
                    LargeMemoryModel.LOGGER.finer("~~-- Decreasing "
                        + (counterKind == CounterKind.ARRAY_COUNTER ? "array" : "mapping")
                        + " counter: " + arrayCounter + " arrays, " + mappingCounter + " mappings"
                        + " (" + finalizationTasksInfo() + ")"
                        + (needToDeleteFile ? "; file " + df + " should be deleted" : ""));
            } finally {
                lockForGc.unlock();
            }
            try {
                if (needToClearBanks) {
                    final ReentrantLock lock = this.lock;
                    lock.lock();
                    // Synchronization is necessary: in other case, freeResources method will be able
                    // to close the unmapping buffers here, that can lead to error in some data file models
                    try {
                        releaseFileAndMapping(ReleaseMode.DATA_MAY_BE_LOST, false);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Throwable ex) {
                LargeMemoryModel.LOGGER.log(Level.SEVERE, "Cannot release array file/mapping resources", ex);
                return;
            }
            if (needToDeleteFile) {
                final ReentrantLock lock = this.lock;
                lock.lock();
                try {
                    if (!disposeCalled) { // always set and check it inside the synchronized block
                        final int timeout = LargeMemoryModel.DELETION_TIMEOUT_IN_FINALIZATION;
                        try {
                            if (deleteWithSeveralAttempts(ms.dataFileModel, df, timeout)) {
                                LargeMemoryModel.LOGGER.fine("Finalization: temporary array storage file "
                                    + df + " is successfully deleted"
                                    + (countOfFailedDeletionsWhileFinalization > 0 ?
                                    " (" + countOfFailedDeletionsWhileFinalization + " attempts)" : ""));
                            } else {
                                LargeMemoryModel.LOGGER.finer("Finalization: no temporary array storage file " + df);
                            }
                        } catch (Throwable ex) {
                            String msg = "Finalization: cannot delete temporary array storage file " + df
                                + (timeout > 0 ? " in " + timeout + " ms" : "");
                            if (ex.getClass().getName().equals("java.io.IOError") || ex instanceof IOErrorJ5) {
                                if (++countOfFailedDeletionsWhileFinalization
                                    >= LargeMemoryModel.MAX_NUMBER_OF_DELETIONS_WHILE_FINALIZATION) {
                                    warningEvenInHook(msg
                                        + " (" + ex
                                        + "). It was the attempt #" + countOfFailedDeletionsWhileFinalization
                                        + ": deletion of this file by the finalizer is canceled!");
                                } else {
                                    LargeMemoryModel.LOGGER.fine(msg
                                        + ". Deletion is scheduled on the next garbage collector execution. "
                                        + "(" + ex + ")");
                                    scheduleFinalizationForAllStorage(); // - we shall try to remove it at the next gc
                                    finalizationHolder = null; // - immmediately allow gc for this storage
                                    dfp = null; // - cancel finalization notification in this GC pass
                                }
                            } else {
                                LargeMemoryModel.LOGGER.log(Level.SEVERE, msg + " (Unexpected exception!)", ex);
                            }
                            this.deletionErrorWhileFinalization = true;
                            return;
                        }
                        // - the next operator will not be performed in a case of exception while releasing or file deletion
                        this.dataFileForDeletion = null;
                    }
                } finally {
                    lock.unlock();
                }
            }
            if (needToForgetStorage) {
                // - the next operator will not be performed in a case of exception while releasing or file deletion
                allNonFinalizedMappedStorages.remove(this);
                if (dfp != null)
                    ms.finalizationNotify(dfp, false);
            }
        }
    }

    static class MappedBitStorage extends MappedStorage implements DataBitStorage {
        private final LongBuffer[] lb;
        private final DataBitBuffer dbuf;

        MappedBitStorage(MappingSettings<?> ms) {
            super(ms);
            this.lb = (LongBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataBitBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedBitStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_LONG_LOG;
        }

        @Override
        final boolean getBit(long index) {
            final ReentrantLock lock = this.lock;
            LongBuffer lb0 = lb[0];
            final boolean sync = this.syncNecessary || !(lb0 == validSingleSpecBufForThisThread && lb0 != null);
            // Without synchronization, we may use lb0 only if it is identical to
            // volatile validSingleSpecBufForThisThread: that reference cannot
            // refer to partially initialized buffer.
            if (sync)
                lock.lock();
            try {
                long i;
                if (sync) {
                    if ((index & indexHighBits) == bankPos[0]) {
                        // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                        i = index - bankPos[0];
                    } else {
                        i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                    }
                    lb0 = lb[0];
                } else {
                    i = index;
                }
                int ii = (int)(i >>> 6), bit = ((int)i) & 63;
                return (lb0.get(ii) & (1L << bit)) != 0L;
            } finally {
                if (sync)
                    lock.unlock();
            }
        }

        @Override
        final void setBit(long index, boolean value) {
            final ReentrantLock lock = this.lock;
            LongBuffer lb1 = lb[1];
            final boolean sync = this.syncNecessary || !(lb1 == validSingleSpecBufForThisThread && lb1 != null);
            // Without synchronization, we may use lb1 only if it is identical to
            // volatile validSingleSpecBufForThisThread: that reference cannot
            // refer to partially initialized buffer.
            if (sync)
                lock.lock();
            try {
                long i;
                if (sync) {
                    if ((index & indexHighBits) == bankPos[1]) {
                        // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                        i = index - bankPos[1];
                    } else {
                        i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                    }
                    lb1 = lb[1];
                } else {
                    i = index;
                }
                int ii = (int)(i >>> 6), bit = ((int)i) & 63;
                synchronized(lb1) {
                    if (value)
                        lb1.put(ii, lb1.get(ii) | 1L << bit);
                    else
                        lb1.put(ii, lb1.get(ii) & ~(1L << bit));
                }
            } finally {
                if (sync)
                    lock.unlock();
            }
        }

        @Override
        final long indexOfBit(long lowIndex, long highIndex, boolean value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final long ofs = offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = PackedBitBuffers.indexOfBit(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = PackedBitBuffers.indexOfBit(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final long ofs = offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = PackedBitBuffers.lastIndexOfBit(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = PackedBitBuffers.lastIndexOfBit(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        //[[Repeat.SectionStart mapped_getBits_method_impl]]
        public void getBits(long pos, long[] destArray, long destArrayOffset, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: getBitsFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            if (isBankLazyAndNotFilledYet(pos)) {
                getUninitializedLazyBitsFromBank(pos, destArray, destArrayOffset, len);
            } else { //EndOfLazy !! this comment is necessary for preprocessing by Repeater !!
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    getBitsFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = Math.min(count, bse);
                if (isBankLazyAndNotFilledYet(pos)) {
                    getUninitializedLazyBitsFromBank(pos, destArray, destArrayOffset, len);
                } else { //EndOfLazy !! this comment is necessary for preprocessing by Repeater !!
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        getBitsFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.SectionEnd mapped_getBits_method_impl]]

        public void setBits(long pos, long[] srcArray, long srcArrayOffset, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: setBitsInFirstBank access the specBuf,
            // that can become null at any moment
            final long bse = ms.bankSizeInElements;
            long ofs = offset(pos);
            assert ofs >= 0;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            if (singleMapping || ofs > 0) { // in other case all tasks can be performed by the loop
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long o = translateIndex(pos);
                        assert o == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    setBitsInFirstBank(buf, ofs, srcArray, srcArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
                srcArrayOffset += len;
                pos += len;
                count -= len;
            }
            for (; count > 0; pos += bse, srcArrayOffset += bse, count -= bse) {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        if (count >= bse) {
                            len = (int)bse;
                            ofs = translateIndexWO(pos);
                        } else {
                            len = count;
                            ofs = translateIndex(pos);
                        }
                        assert ofs == 0;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    setBitsInFirstBank(buf, 0, srcArray, srcArrayOffset, Math.min(count, bse));
                } finally {
                    decrementMappingInUseCounter();
                }
            }
        }


        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getBits_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getBits ==> andBits    !! Auto-generated: NOT EDIT !! ]]
        public void andBits(long pos, long[] destArray, long destArrayOffset, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: andBitsFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    andBitsFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        andBitsFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getBits_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getBits ==> orBits    !! Auto-generated: NOT EDIT !! ]]
        public void orBits(long pos, long[] destArray, long destArrayOffset, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: orBitsFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    orBitsFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        orBitsFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getBits_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getBits ==> xorBits    !! Auto-generated: NOT EDIT !! ]]
        public void xorBits(long pos, long[] destArray, long destArrayOffset, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: xorBitsFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    xorBitsFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        xorBitsFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, mapped_getBits_method_impl)
        //  if\s*\(isBankLazyAndNotFilledYet.*?\{\s*\/\/EndOfLazy.*?((?:\r(?!\n)|\n|\r\n)\s*) ==> {$1;;
        //  getBits ==> andNotBits    !! Auto-generated: NOT EDIT !! ]]
        public void andNotBits(long pos, long[] destArray, long destArrayOffset, long count) {
            if (count == 0) // necessary check: our swapping algorithm does not work with zero-length file
                return;
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: andNotBitsFromFirstBank access the specBuf,
            // that can become null at any moment
            long ofs = offset(pos);
            assert ofs >= 0;
            final long bse = ms.bankSizeInElements;
            long len = singleMapping ? count : Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(pos);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    andNotBitsFromFirstBank(buf, ofs, destArray, destArrayOffset, len);
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            destArrayOffset += len;
            pos += len;
            count -= len;
            for (; count > 0; pos += bse, destArrayOffset += bse, count -= bse) {
                len = Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(pos);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        andNotBitsFromFirstBank(buf, 0, destArray, destArrayOffset, len);
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
        }
        //[[Repeat.IncludeEnd]]

        final void copy(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setBit(destIndex, getBit(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i1 = translateIndex(firstIndex);
                assert i1 >= 0;
                int ii1 = (int)(i1 >>> 6), bit1 = ((int)i1) & 63;
                long i2 = translateIndex(secondIndex, true);
                assert i2 >= 0;
                int ii2 = (int)(i2 >>> 6), bit2 = ((int)i2) & 63;
                synchronized(lb) {
                    long l1 = lb[0].get(ii1);
                    long l2 = lb[1].get(ii2);
                    boolean v1 = (l1 & (1L << bit1)) != 0L;
                    boolean v2 = (l2 & (1L << bit2)) != 0L;
                    if (v1 != v2) {
                        if (v2)
                            lb[0].put(ii1, l1 | 1L << bit1);
                        else
                            lb[0].put(ii1, l1 & ~(1L << bit1));
                        l2 = lb[1].get(ii2); // for swapping 2 bits in the same long
                        if (v1)
                            lb[1].put(ii2, l2 | 1L << bit2);
                        else
                            lb[1].put(ii2, l2 & ~(1L << bit2));
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            lb[bank] = bh[bank] == null ? null : bh[bank].data().asLongBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, booleanZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            PackedBitBuffers.unpackBits((boolean[])destArray, destArrayOffset, (LongBuffer)buf,
                firstBankOffset, count);
        }

        void setDataInFirstBank(Buffer buf,
            long firstBankOffset, Object srcArray, int srcArrayOffset, int count)
        {
            PackedBitBuffers.packBits((LongBuffer)buf, firstBankOffset, (boolean[])srcArray, srcArrayOffset, count);
//            for (int k = 0; k < count; k++) { // Java 1.7 bug here!
//                if (PackedBitBuffers.getBit((LongBuffer)buf, firstBankOffset + k)
//                    != ((boolean[])srcArray)[srcArrayOffset + k]) {
//                    StringBuilder sb1 = new StringBuilder(), sb2 = new StringBuilder();
//                    for (int j = 0; j < count; j++) {
//                        if (j == k) {
//                            sb1.append("    ");
//                            sb2.append("    ");
//                        }
//                        sb1.append(PackedBitBuffers.getBit((LongBuffer)buf, firstBankOffset + j) ? "1" : "0");
//                        sb2.append(((boolean[])srcArray)[srcArrayOffset + j] ? "1" : "0");
//                    }
//                    throw new AssertionError("k=" + k + ", count=" + count + ", srcArrayOffset=" + srcArrayOffset
//                        + ", firstBankOffset=" + firstBankOffset + ", srcArray["
//                        + ((boolean[])srcArray).length + "]," + (LongBuffer)buf + "\n" + sb1 + "\n" + sb2);
//                }
//            }
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            PackedBitBuffers.fillBits(lb[0], destOffset, count, ((Boolean)fillerWrapper).booleanValue());
        }

        void copyFirstBank(MappedStorage src,
            long srcOffset, long destOffset, long count, boolean reverseOrder, boolean copyToSecondBank) {
            PackedBitBuffers.copyBits(lb[copyToSecondBank ? 1 : 0], destOffset,
                ((MappedBitStorage)src).lb[0], srcOffset, count, reverseOrder);
        }

        void swapFirstBank(MappedStorage another,
            long anotherOffset, long thisOffset, long count, boolean swapWithSecondBank) {
            PackedBitBuffers.swapBits(((MappedBitStorage)another).lb[0], anotherOffset,
                lb[swapWithSecondBank ? 1 : 0], thisOffset, count);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            throw new UnsupportedOperationException("minDataFromFirstBank is not supported for bit storages");
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            throw new UnsupportedOperationException("maxDataFromFirstBank is not supported for bit storages");
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            throw new UnsupportedOperationException("addDataFromFirstBank is not supported for bit storages");
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            throw new UnsupportedOperationException("addDataFromFirstBank is not supported for bit storages");
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            throw new UnsupportedOperationException("subtractDataFromFirstBank is not supported for bit storages");
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            throw new UnsupportedOperationException("absDiffDataFromFirstBank is not supported for bit storages");
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 63) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                LongBuffer bankSpecBuf = bankBB.asLongBuffer();
                long count = ((long)bankSpecBuf.limit()) << 6;
                if (count > length - elementIndex) {
                    count = length - elementIndex;
                    actualizeLazyZeroFillingBank(bankBB, (int)(count >> 3), bankBB.limit());
                    // if necessary, here we fill several extra bits,
                    // corresponding to the last byte of ms.lazyFillingPattern
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    long len = dbuf.count();
                    assert len > 0;
                    PackedBitBuffers.copyBits(bankSpecBuf, ofs, LongBuffer.wrap(dbuf.data()), dbuf.from(), len);
                    ofs += len;
                }
            }
        }

        private void getUninitializedLazyBitsFromBank(long pos,
            long[] destArray, long destArrayOffset, long count)
        {
            long length = ms.lazyFillingPattern == null ? 0 : ms.lazyFillingPattern.length();
            if (count > length - pos) {
                long newCount = length < pos ? 0 : length - pos;
                PackedBitArrays.fillBits(destArray, destArrayOffset + newCount, count - newCount, false);
                count = newCount;
            }
            if (count > 0) { // so, length > pos and ms.lazyFillingPattern != null
                ((BitArray)ms.lazyFillingPattern).getBits(pos, destArray, destArrayOffset, count);
            }
        }

        private void getBitsFromFirstBank(LongBuffer buf,
            long firstBankOffset, long[] destArray, long destArrayOffset, long count)
        {
            PackedBitBuffers.copyBits(LongBuffer.wrap(destArray), destArrayOffset, buf, firstBankOffset, count);
        }

        private void setBitsInFirstBank(LongBuffer buf,
            long firstBankOffset, long[] srcArray, long srcArrayOffset, long count)
        {
            PackedBitBuffers.copyBits(buf, firstBankOffset, LongBuffer.wrap(srcArray), srcArrayOffset, count);
        }

        private void andBitsFromFirstBank(LongBuffer buf,
            long firstBankOffset, long[] destArray, long destArrayOffset, long count)
        {
            PackedBitBuffers.andBits(destArray, destArrayOffset, buf, firstBankOffset, count);
        }

        private void orBitsFromFirstBank(LongBuffer buf,
            long firstBankOffset, long[] destArray, long destArrayOffset, long count)
        {
            PackedBitBuffers.orBits(destArray, destArrayOffset, buf, firstBankOffset, count);
        }

        private void xorBitsFromFirstBank(LongBuffer buf,
            long firstBankOffset, long[] destArray, long destArrayOffset, long count)
        {
            PackedBitBuffers.xorBits(destArray, destArrayOffset, buf, firstBankOffset, count);
        }

        private void andNotBitsFromFirstBank(LongBuffer buf,
            long firstBankOffset, long[] destArray, long destArrayOffset, long count)
        {
            PackedBitBuffers.andNotBits(destArray, destArrayOffset, buf, firstBankOffset, count);
        }
    }

    /*Repeat() byte(?!s) ==> char,,short,,int,,long,,float,,double;;
               \bByte\b  ==> Character,,Short,,Integer,,Long,,Float,,Double;;
               Byte(?!Buffer\s+bankBB) ==> Char,,Short,,Int,,Long,,Float,,Double;;
               BYTE(?!S) ==> CHAR,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE;;
               bb        ==> cb,,sb,,ib,,lb,,fb,,db;;
               (bh\[bank\]\.data\(\)|=\s*bankBB)(?:\.duplicate\(\))? ==> $1.asCharBuffer(),,$1.asShortBuffer(),,
                        $1.asIntBuffer(),,$1.asLongBuffer(),,
                        $1.asFloatBuffer(),,$1.asDoubleBuffer();;
               (count)(,\s*truncateOverflows) ==> $1$2,,$1$2,,$1$2,,$1,,...;;
               (JBuffers\.absDiffOfIntArrayAndBuffer\(.*?)\); ==> $1, truncateOverflows);,,...

     */
    static class MappedByteStorage extends MappedStorage {
        private final ByteBuffer[] bb;
        private final DataByteBuffer dbuf;

        MappedByteStorage(MappingSettings<?> ms) {
            super(ms);
            this.bb = (ByteBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataByteBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedByteStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_BYTE_LOG;
        }

        @Override
        final byte getByte(long index) {
            final ByteBuffer bb0 = bb[0];
            if (this.syncNecessary || !(bb0 == validSingleSpecBufForThisThread && bb0 != null)) {
                // Without synchronization, we may use bb0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getByteSync(index);
            } else { // single mapping mode and bank is ready
                return bb0.get((int)index);
            }
        }

        private byte getByteSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return bb[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setByte(long index, byte value) {
            final ByteBuffer bb1 = bb[1];
            if (this.syncNecessary || !(bb1 == validSingleSpecBufForThisThread && bb1 != null)) {
                // Without synchronization, we may use bb1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setByteSync(index, value);
            } else { // single mapping mode and bank is ready
                bb1.put((int)index, value);
            }
        }

        private void setByteSync(long index, byte value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                bb[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfByte(long lowIndex, long highIndex, byte value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    ByteBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = bb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfByte(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        ByteBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = bb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfByte(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    ByteBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = bb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfByte(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        ByteBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = bb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfByte(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final ByteBuffer bb0 = bb[0], bb1 = bb[1];
            if (this.syncNecessary || !(bb0 == validSingleSpecBufForThisThread && bb1 == bb0 && bb0 != null)) {
                // Without synchronization, we may use bb0/bb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                bb1.put((int)destIndex, bb0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setByte(destIndex, getByte(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final ByteBuffer bb0 = bb[0], bb1 = bb[1];
            if (this.syncNecessary  || !(bb0 == validSingleSpecBufForThisThread && bb1 == bb0 && bb0 != null)) {
                // Without synchronization, we may use bb0/bb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                byte v1 = bb0.get(i1);
                byte v2 = bb1.get(i2);
                bb0.put(i1, v2);
                bb1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                byte v1 = bb[0].get(i1);
                byte v2 = bb[1].get(i2);
                bb[0].put(i1, v2);
                bb[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            bb[bank] = bh[bank] == null ? null : bh[bank].data();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, byteZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            ByteBuffer dup = ((ByteBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((byte[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            ByteBuffer dup = ((ByteBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((byte[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillByteBuffer(bb[0], ofs, cnt, ((Byte)fillerWrapper).byteValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyByteBuffer(bb[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedByteStorage)src).bb[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapByteBuffer(((MappedByteStorage)another).bb[0], anotherOfs,
                bb[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minByteArrayAndBuffer((byte[])destArray, destArrayOffset,
                (ByteBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxByteArrayAndBuffer((byte[])destArray, destArrayOffset,
                (ByteBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addByteBufferToArray(destArray, destArrayOffset,
                (ByteBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addByteBufferToArray(destArray, destArrayOffset,
                (ByteBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractByteBufferFromArray((byte[])destArray, destArrayOffset,
                (ByteBuffer)buf, (int)firstBankOffset, count, truncateOverflows);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfByteArrayAndBuffer((byte[])destArray, destArrayOffset,
                (ByteBuffer)buf, (int)firstBankOffset, count);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                ByteBuffer bankSpecBuf = bankBB.duplicate();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_BYTE_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static class MappedCharStorage extends MappedStorage {
        private final CharBuffer[] cb;
        private final DataCharBuffer dbuf;

        MappedCharStorage(MappingSettings<?> ms) {
            super(ms);
            this.cb = (CharBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataCharBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedCharStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_CHAR_LOG;
        }

        @Override
        final char getChar(long index) {
            final CharBuffer cb0 = cb[0];
            if (this.syncNecessary || !(cb0 == validSingleSpecBufForThisThread && cb0 != null)) {
                // Without synchronization, we may use cb0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getCharSync(index);
            } else { // single mapping mode and bank is ready
                return cb0.get((int)index);
            }
        }

        private char getCharSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return cb[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setChar(long index, char value) {
            final CharBuffer cb1 = cb[1];
            if (this.syncNecessary || !(cb1 == validSingleSpecBufForThisThread && cb1 != null)) {
                // Without synchronization, we may use cb1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setCharSync(index, value);
            } else { // single mapping mode and bank is ready
                cb1.put((int)index, value);
            }
        }

        private void setCharSync(long index, char value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                cb[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfChar(long lowIndex, long highIndex, char value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    CharBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = cb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfChar(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        CharBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = cb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfChar(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    CharBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = cb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfChar(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        CharBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = cb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfChar(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final CharBuffer cb0 = cb[0], cb1 = cb[1];
            if (this.syncNecessary || !(cb0 == validSingleSpecBufForThisThread && cb1 == cb0 && cb0 != null)) {
                // Without synchronization, we may use cb0/cb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                cb1.put((int)destIndex, cb0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setChar(destIndex, getChar(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final CharBuffer cb0 = cb[0], cb1 = cb[1];
            if (this.syncNecessary  || !(cb0 == validSingleSpecBufForThisThread && cb1 == cb0 && cb0 != null)) {
                // Without synchronization, we may use cb0/cb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                char v1 = cb0.get(i1);
                char v2 = cb1.get(i2);
                cb0.put(i1, v2);
                cb1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                char v1 = cb[0].get(i1);
                char v2 = cb[1].get(i2);
                cb[0].put(i1, v2);
                cb[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            cb[bank] = bh[bank] == null ? null : bh[bank].data().asCharBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, charZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            CharBuffer dup = ((CharBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((char[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            CharBuffer dup = ((CharBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((char[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillCharBuffer(cb[0], ofs, cnt, ((Character)fillerWrapper).charValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyCharBuffer(cb[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedCharStorage)src).cb[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapCharBuffer(((MappedCharStorage)another).cb[0], anotherOfs,
                cb[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minCharArrayAndBuffer((char[])destArray, destArrayOffset,
                (CharBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxCharArrayAndBuffer((char[])destArray, destArrayOffset,
                (CharBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addCharBufferToArray(destArray, destArrayOffset,
                (CharBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addCharBufferToArray(destArray, destArrayOffset,
                (CharBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractCharBufferFromArray((char[])destArray, destArrayOffset,
                (CharBuffer)buf, (int)firstBankOffset, count, truncateOverflows);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfCharArrayAndBuffer((char[])destArray, destArrayOffset,
                (CharBuffer)buf, (int)firstBankOffset, count);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                CharBuffer bankSpecBuf = bankBB.asCharBuffer();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_CHAR_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }

    static class MappedShortStorage extends MappedStorage {
        private final ShortBuffer[] sb;
        private final DataShortBuffer dbuf;

        MappedShortStorage(MappingSettings<?> ms) {
            super(ms);
            this.sb = (ShortBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataShortBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedShortStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_SHORT_LOG;
        }

        @Override
        final short getShort(long index) {
            final ShortBuffer sb0 = sb[0];
            if (this.syncNecessary || !(sb0 == validSingleSpecBufForThisThread && sb0 != null)) {
                // Without synchronization, we may use sb0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getShortSync(index);
            } else { // single mapping mode and bank is ready
                return sb0.get((int)index);
            }
        }

        private short getShortSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return sb[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setShort(long index, short value) {
            final ShortBuffer sb1 = sb[1];
            if (this.syncNecessary || !(sb1 == validSingleSpecBufForThisThread && sb1 != null)) {
                // Without synchronization, we may use sb1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setShortSync(index, value);
            } else { // single mapping mode and bank is ready
                sb1.put((int)index, value);
            }
        }

        private void setShortSync(long index, short value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                sb[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfShort(long lowIndex, long highIndex, short value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    ShortBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = sb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfShort(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        ShortBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = sb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfShort(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    ShortBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = sb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfShort(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        ShortBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = sb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfShort(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final ShortBuffer sb0 = sb[0], sb1 = sb[1];
            if (this.syncNecessary || !(sb0 == validSingleSpecBufForThisThread && sb1 == sb0 && sb0 != null)) {
                // Without synchronization, we may use sb0/sb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                sb1.put((int)destIndex, sb0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setShort(destIndex, getShort(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final ShortBuffer sb0 = sb[0], sb1 = sb[1];
            if (this.syncNecessary  || !(sb0 == validSingleSpecBufForThisThread && sb1 == sb0 && sb0 != null)) {
                // Without synchronization, we may use sb0/sb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                short v1 = sb0.get(i1);
                short v2 = sb1.get(i2);
                sb0.put(i1, v2);
                sb1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                short v1 = sb[0].get(i1);
                short v2 = sb[1].get(i2);
                sb[0].put(i1, v2);
                sb[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            sb[bank] = bh[bank] == null ? null : bh[bank].data().asShortBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, shortZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            ShortBuffer dup = ((ShortBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((short[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            ShortBuffer dup = ((ShortBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((short[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillShortBuffer(sb[0], ofs, cnt, ((Short)fillerWrapper).shortValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyShortBuffer(sb[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedShortStorage)src).sb[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapShortBuffer(((MappedShortStorage)another).sb[0], anotherOfs,
                sb[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minShortArrayAndBuffer((short[])destArray, destArrayOffset,
                (ShortBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxShortArrayAndBuffer((short[])destArray, destArrayOffset,
                (ShortBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addShortBufferToArray(destArray, destArrayOffset,
                (ShortBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addShortBufferToArray(destArray, destArrayOffset,
                (ShortBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractShortBufferFromArray((short[])destArray, destArrayOffset,
                (ShortBuffer)buf, (int)firstBankOffset, count, truncateOverflows);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfShortArrayAndBuffer((short[])destArray, destArrayOffset,
                (ShortBuffer)buf, (int)firstBankOffset, count);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                ShortBuffer bankSpecBuf = bankBB.asShortBuffer();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_SHORT_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }

    static class MappedIntStorage extends MappedStorage {
        private final IntBuffer[] ib;
        private final DataIntBuffer dbuf;

        MappedIntStorage(MappingSettings<?> ms) {
            super(ms);
            this.ib = (IntBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataIntBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedIntStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_INT_LOG;
        }

        @Override
        final int getInt(long index) {
            final IntBuffer ib0 = ib[0];
            if (this.syncNecessary || !(ib0 == validSingleSpecBufForThisThread && ib0 != null)) {
                // Without synchronization, we may use ib0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getIntSync(index);
            } else { // single mapping mode and bank is ready
                return ib0.get((int)index);
            }
        }

        private int getIntSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return ib[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setInt(long index, int value) {
            final IntBuffer ib1 = ib[1];
            if (this.syncNecessary || !(ib1 == validSingleSpecBufForThisThread && ib1 != null)) {
                // Without synchronization, we may use ib1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setIntSync(index, value);
            } else { // single mapping mode and bank is ready
                ib1.put((int)index, value);
            }
        }

        private void setIntSync(long index, int value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                ib[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfInt(long lowIndex, long highIndex, int value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    IntBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = ib[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfInt(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        IntBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = ib[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfInt(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    IntBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = ib[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfInt(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        IntBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = ib[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfInt(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final IntBuffer ib0 = ib[0], ib1 = ib[1];
            if (this.syncNecessary || !(ib0 == validSingleSpecBufForThisThread && ib1 == ib0 && ib0 != null)) {
                // Without synchronization, we may use ib0/ib1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                ib1.put((int)destIndex, ib0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setInt(destIndex, getInt(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final IntBuffer ib0 = ib[0], ib1 = ib[1];
            if (this.syncNecessary  || !(ib0 == validSingleSpecBufForThisThread && ib1 == ib0 && ib0 != null)) {
                // Without synchronization, we may use ib0/ib1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                int v1 = ib0.get(i1);
                int v2 = ib1.get(i2);
                ib0.put(i1, v2);
                ib1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                int v1 = ib[0].get(i1);
                int v2 = ib[1].get(i2);
                ib[0].put(i1, v2);
                ib[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            ib[bank] = bh[bank] == null ? null : bh[bank].data().asIntBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, intZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            IntBuffer dup = ((IntBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((int[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            IntBuffer dup = ((IntBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((int[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillIntBuffer(ib[0], ofs, cnt, ((Integer)fillerWrapper).intValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyIntBuffer(ib[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedIntStorage)src).ib[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapIntBuffer(((MappedIntStorage)another).ib[0], anotherOfs,
                ib[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minIntArrayAndBuffer((int[])destArray, destArrayOffset,
                (IntBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxIntArrayAndBuffer((int[])destArray, destArrayOffset,
                (IntBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addIntBufferToArray(destArray, destArrayOffset,
                (IntBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addIntBufferToArray(destArray, destArrayOffset,
                (IntBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractIntBufferFromArray((int[])destArray, destArrayOffset,
                (IntBuffer)buf, (int)firstBankOffset, count, truncateOverflows);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfIntArrayAndBuffer((int[])destArray, destArrayOffset,
                (IntBuffer)buf, (int)firstBankOffset, count, truncateOverflows);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                IntBuffer bankSpecBuf = bankBB.asIntBuffer();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_INT_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }

    static class MappedLongStorage extends MappedStorage {
        private final LongBuffer[] lb;
        private final DataLongBuffer dbuf;

        MappedLongStorage(MappingSettings<?> ms) {
            super(ms);
            this.lb = (LongBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataLongBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedLongStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_LONG_LOG;
        }

        @Override
        final long getLong(long index) {
            final LongBuffer lb0 = lb[0];
            if (this.syncNecessary || !(lb0 == validSingleSpecBufForThisThread && lb0 != null)) {
                // Without synchronization, we may use lb0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getLongSync(index);
            } else { // single mapping mode and bank is ready
                return lb0.get((int)index);
            }
        }

        private long getLongSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return lb[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setLong(long index, long value) {
            final LongBuffer lb1 = lb[1];
            if (this.syncNecessary || !(lb1 == validSingleSpecBufForThisThread && lb1 != null)) {
                // Without synchronization, we may use lb1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setLongSync(index, value);
            } else { // single mapping mode and bank is ready
                lb1.put((int)index, value);
            }
        }

        private void setLongSync(long index, long value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                lb[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfLong(long lowIndex, long highIndex, long value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfLong(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfLong(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    LongBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = lb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfLong(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        LongBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = lb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfLong(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final LongBuffer lb0 = lb[0], lb1 = lb[1];
            if (this.syncNecessary || !(lb0 == validSingleSpecBufForThisThread && lb1 == lb0 && lb0 != null)) {
                // Without synchronization, we may use lb0/lb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                lb1.put((int)destIndex, lb0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setLong(destIndex, getLong(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final LongBuffer lb0 = lb[0], lb1 = lb[1];
            if (this.syncNecessary  || !(lb0 == validSingleSpecBufForThisThread && lb1 == lb0 && lb0 != null)) {
                // Without synchronization, we may use lb0/lb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                long v1 = lb0.get(i1);
                long v2 = lb1.get(i2);
                lb0.put(i1, v2);
                lb1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                long v1 = lb[0].get(i1);
                long v2 = lb[1].get(i2);
                lb[0].put(i1, v2);
                lb[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            lb[bank] = bh[bank] == null ? null : bh[bank].data().asLongBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, longZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            LongBuffer dup = ((LongBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((long[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            LongBuffer dup = ((LongBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((long[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillLongBuffer(lb[0], ofs, cnt, ((Long)fillerWrapper).longValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyLongBuffer(lb[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedLongStorage)src).lb[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapLongBuffer(((MappedLongStorage)another).lb[0], anotherOfs,
                lb[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minLongArrayAndBuffer((long[])destArray, destArrayOffset,
                (LongBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxLongArrayAndBuffer((long[])destArray, destArrayOffset,
                (LongBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addLongBufferToArray(destArray, destArrayOffset,
                (LongBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addLongBufferToArray(destArray, destArrayOffset,
                (LongBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractLongBufferFromArray((long[])destArray, destArrayOffset,
                (LongBuffer)buf, (int)firstBankOffset, count);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfLongArrayAndBuffer((long[])destArray, destArrayOffset,
                (LongBuffer)buf, (int)firstBankOffset, count);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                LongBuffer bankSpecBuf = bankBB.asLongBuffer();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_LONG_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }

    static class MappedFloatStorage extends MappedStorage {
        private final FloatBuffer[] fb;
        private final DataFloatBuffer dbuf;

        MappedFloatStorage(MappingSettings<?> ms) {
            super(ms);
            this.fb = (FloatBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataFloatBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedFloatStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_FLOAT_LOG;
        }

        @Override
        final float getFloat(long index) {
            final FloatBuffer fb0 = fb[0];
            if (this.syncNecessary || !(fb0 == validSingleSpecBufForThisThread && fb0 != null)) {
                // Without synchronization, we may use fb0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getFloatSync(index);
            } else { // single mapping mode and bank is ready
                return fb0.get((int)index);
            }
        }

        private float getFloatSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return fb[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setFloat(long index, float value) {
            final FloatBuffer fb1 = fb[1];
            if (this.syncNecessary || !(fb1 == validSingleSpecBufForThisThread && fb1 != null)) {
                // Without synchronization, we may use fb1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setFloatSync(index, value);
            } else { // single mapping mode and bank is ready
                fb1.put((int)index, value);
            }
        }

        private void setFloatSync(long index, float value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                fb[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfFloat(long lowIndex, long highIndex, float value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    FloatBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = fb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfFloat(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        FloatBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = fb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfFloat(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    FloatBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = fb[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfFloat(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        FloatBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = fb[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfFloat(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final FloatBuffer fb0 = fb[0], fb1 = fb[1];
            if (this.syncNecessary || !(fb0 == validSingleSpecBufForThisThread && fb1 == fb0 && fb0 != null)) {
                // Without synchronization, we may use fb0/fb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                fb1.put((int)destIndex, fb0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setFloat(destIndex, getFloat(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final FloatBuffer fb0 = fb[0], fb1 = fb[1];
            if (this.syncNecessary  || !(fb0 == validSingleSpecBufForThisThread && fb1 == fb0 && fb0 != null)) {
                // Without synchronization, we may use fb0/fb1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                float v1 = fb0.get(i1);
                float v2 = fb1.get(i2);
                fb0.put(i1, v2);
                fb1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                float v1 = fb[0].get(i1);
                float v2 = fb[1].get(i2);
                fb[0].put(i1, v2);
                fb[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            fb[bank] = bh[bank] == null ? null : bh[bank].data().asFloatBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, floatZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            FloatBuffer dup = ((FloatBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((float[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            FloatBuffer dup = ((FloatBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((float[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillFloatBuffer(fb[0], ofs, cnt, ((Float)fillerWrapper).floatValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyFloatBuffer(fb[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedFloatStorage)src).fb[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapFloatBuffer(((MappedFloatStorage)another).fb[0], anotherOfs,
                fb[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minFloatArrayAndBuffer((float[])destArray, destArrayOffset,
                (FloatBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxFloatArrayAndBuffer((float[])destArray, destArrayOffset,
                (FloatBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addFloatBufferToArray(destArray, destArrayOffset,
                (FloatBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addFloatBufferToArray(destArray, destArrayOffset,
                (FloatBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractFloatBufferFromArray((float[])destArray, destArrayOffset,
                (FloatBuffer)buf, (int)firstBankOffset, count);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfFloatArrayAndBuffer((float[])destArray, destArrayOffset,
                (FloatBuffer)buf, (int)firstBankOffset, count);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                FloatBuffer bankSpecBuf = bankBB.asFloatBuffer();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_FLOAT_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }

    static class MappedDoubleStorage extends MappedStorage {
        private final DoubleBuffer[] db;
        private final DataDoubleBuffer dbuf;

        MappedDoubleStorage(MappingSettings<?> ms) {
            super(ms);
            this.db = (DoubleBuffer[])this.specBufs;
            this.dbuf = ms.lazyFillingPattern == null ? null :
                (DataDoubleBuffer)Arrays.bufferInternal(ms.lazyFillingPattern, DataBuffer.AccessMode.READ);
        }

        DataStorage newCompatibleEmptyStorage(boolean unresizable) {
            return new MappedDoubleStorage(ms.getCompatibleInstanceForNewTemporaryFile(unresizable));
        }

        int bytesPerBufferElementLog() {
            return BYTES_PER_DOUBLE_LOG;
        }

        @Override
        final double getDouble(long index) {
            final DoubleBuffer db0 = db[0];
            if (this.syncNecessary || !(db0 == validSingleSpecBufForThisThread && db0 != null)) {
                // Without synchronization, we may use db0 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                return getDoubleSync(index);
            } else { // single mapping mode and bank is ready
                return db0.get((int)index);
            }
        }

        private double getDoubleSync(long index) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[0]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[0] <= Integer.MAX_VALUE;
                    i = index - bankPos[0];
                } else {
                    i = translateFailedIndex(index, false, LoadingMode.DEFAULT);
                }
                return db[0].get((int)i);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final void setDouble(long index, double value) {
            final DoubleBuffer db1 = db[1];
            if (this.syncNecessary || !(db1 == validSingleSpecBufForThisThread && db1 != null)) {
                // Without synchronization, we may use db1 only if it is identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                setDoubleSync(index, value);
            } else { // single mapping mode and bank is ready
                db1.put((int)index, value);
            }
        }

        private void setDoubleSync(long index, double value) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                long i;
                if ((index & indexHighBits) == bankPos[1]) {
                    // assert this instanceof MappedBitStorage || index - bankPos[1] <= Integer.MAX_VALUE;
                    i = index - bankPos[1];
                } else {
                    i = translateFailedIndex(index, true, LoadingMode.DEFAULT);
                }
                db[1].put((int)i, value);
            } finally {
                lock.unlock();
            }
        }

        @Override
        final long indexOfDouble(long lowIndex, long highIndex, double value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(lowIndex);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, bse - ofs);
            {
                incrementMappingInUseCounter();
                try {
                    DoubleBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(lowIndex);
                        assert translateIndexResult == ofs;
                        buf = db[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.indexOfDouble(buf, ofs, ofs + len, value);
                    if (result != -1)
                        return result + lowIndex - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            lowIndex += len;
            count -= len;
            for (; count > 0; lowIndex += bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        DoubleBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(lowIndex);
                            assert translateIndexResult == 0;
                            buf = db[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.indexOfDouble(buf, 0, len, value);
                        if (result != -1)
                            return result + lowIndex;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        @Override
        final long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            if (highIndex <= lowIndex) { // necessary check: our algorithm does not work with zero-length file
                return -1;
            }
            final ReentrantLock lock = this.lock;
            // synchronization is necessary always: below we access the specBuf,
            // that can become null at any moment
            final int ofs = (int)offset(highIndex - 1);
            assert ofs >= 0;
            long count = highIndex - lowIndex;
            final int bse = (int)ms.bankSizeInElements;
            assert bse == ms.bankSizeInElements;
            int len = singleMapping ? (int)count : (int)Math.min(count, ofs + 1);
            {
                incrementMappingInUseCounter();
                try {
                    DoubleBuffer buf;
                    lock.lock();
                    try {
                        long translateIndexResult = translateIndex(highIndex - 1);
                        assert translateIndexResult == ofs;
                        buf = db[0];
                    } finally {
                        lock.unlock();
                    }
                    long result = JBuffers.lastIndexOfDouble(buf, ofs - len + 1, ofs + 1, value);
                    if (result != -1)
                        return result + highIndex - 1 - ofs;
                } finally {
                    decrementMappingInUseCounter();
                }
            }

            highIndex -= len;
            count -= len;
            for (; count > 0; highIndex -= bse, count -= bse) {
                len = (int)Math.min(count, bse);
                {
                    incrementMappingInUseCounter();
                    try {
                        DoubleBuffer buf;
                        lock.lock();
                        try {
                            long translateIndexResult = translateIndex(highIndex - 1);
                            assert translateIndexResult == bse - 1;
                            buf = db[0];
                        } finally {
                            lock.unlock();
                        }
                        long result = JBuffers.lastIndexOfDouble(buf, bse - len, bse, value);
                        if (result != -1)
                            return result + highIndex - bse;
                    } finally {
                        decrementMappingInUseCounter();
                    }
                }
            }
            return -1;
        }

        final void copy(long destIndex, long srcIndex) {
            final DoubleBuffer db0 = db[0], db1 = db[1];
            if (this.syncNecessary || !(db0 == validSingleSpecBufForThisThread && db1 == db0 && db0 != null)) {
                // Without synchronization, we may use db0/db1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                copySync(destIndex, srcIndex);
            } else { // single mapping mode and banks are ready
                db1.put((int)destIndex, db0.get((int)srcIndex));
            }
        }
        private void copySync(long destIndex, long srcIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                setDouble(destIndex, getDouble(srcIndex));
            } finally {
                lock.unlock();
            }
        }

        final void swap(long firstIndex, long secondIndex) {
            final DoubleBuffer db0 = db[0], db1 = db[1];
            if (this.syncNecessary  || !(db0 == validSingleSpecBufForThisThread && db1 == db0 && db0 != null)) {
                // Without synchronization, we may use db0/db1 only if they are identical to
                // volatile validSingleSpecBufForThisThread: that reference cannot
                // refer to partially initialized buffer.
                swapSync(firstIndex, secondIndex);
            } else { // single mapping mode and banks are ready
                int i1 = (int)firstIndex;
                int i2 = (int)secondIndex;
                double v1 = db0.get(i1);
                double v2 = db1.get(i2);
                db0.put(i1, v2);
                db1.put(i2, v1);
            }
        }

        private void swapSync(long firstIndex, long secondIndex) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i1 = (int)translateIndex(firstIndex);
                assert i1 >= 0;
                int i2 = (int)translateIndex(secondIndex, true);
                assert i2 >= 0;
                double v1 = db[0].get(i1);
                double v2 = db[1].get(i2);
                db[0].put(i1, v2);
                db[1].put(i2, v1);
            } finally {
                lock.unlock();
            }
        }

        void setSpecificBuffer(int bank) {
            db[bank] = bh[bank] == null ? null : bh[bank].data().asDoubleBuffer();
        }

        void clearData(long pos, long count) {
            fillData(pos, count, doubleZero);
        }

        void getDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            DoubleBuffer dup = ((DoubleBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.get((double[])destArray, destArrayOffset, count);
        }

        void setDataInFirstBank(Buffer buf, long firstBankOffset, Object srcArray, int srcArrayOffset, int count) {
            int ofs = (int)firstBankOffset;
            assert firstBankOffset == ofs;
            DoubleBuffer dup = ((DoubleBuffer)buf).duplicate(); // necessary for multithread access if !this.syncNecessary
            dup.position(ofs);
            dup.put((double[])srcArray, srcArrayOffset, count);
        }

        void fillDataInFirstBank(long destOffset, long count, Object fillerWrapper) {
            int ofs = (int)destOffset;
            int cnt = (int)count;
            assert destOffset == ofs;
            assert count == cnt;
            JBuffers.fillDoubleBuffer(db[0], ofs, cnt, ((Double)fillerWrapper).doubleValue());
        }

        void copyFirstBank(MappedStorage src, long srcOffset, long destOffset, long count,
            boolean reverseOrder, boolean copyToSecondBank)
        {
            int srcOfs = (int)srcOffset;
            int destOfs = (int)destOffset;
            int cnt = (int)count;
            assert srcOffset == srcOfs;
            assert destOffset == destOfs;
            assert count == cnt;
            JBuffers.copyDoubleBuffer(db[copyToSecondBank ? 1 : 0], destOfs,
                ((MappedDoubleStorage)src).db[0], srcOfs, cnt, reverseOrder);
        }

        void swapFirstBank(MappedStorage another, long anotherOffset, long thisOffset, long count,
            boolean swapWithSecondBank)
        {
            int anotherOfs = (int)anotherOffset;
            int thisOfs = (int)thisOffset;
            int cnt = (int)count;
            assert anotherOffset == anotherOfs;
            assert thisOffset == thisOfs;
            assert count == cnt;
            JBuffers.swapDoubleBuffer(((MappedDoubleStorage)another).db[0], anotherOfs,
                db[swapWithSecondBank ? 1 : 0], thisOfs, cnt);
        }

        void minDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.minDoubleArrayAndBuffer((double[])destArray, destArrayOffset,
                (DoubleBuffer)buf, (int)firstBankOffset, count);
        }

        void maxDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset, int count) {
            JBuffers.maxDoubleArrayAndBuffer((double[])destArray, destArrayOffset,
                (DoubleBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, int[] destArray, int destArrayOffset, int count) {
            JBuffers.addDoubleBufferToArray(destArray, destArrayOffset,
                (DoubleBuffer)buf, (int)firstBankOffset, count);
        }

        void addDataFromFirstBank(Buffer buf, long firstBankOffset, double[] destArray, int destArrayOffset, int count,
            double mult)
        {
            JBuffers.addDoubleBufferToArray(destArray, destArrayOffset,
                (DoubleBuffer)buf, (int)firstBankOffset, count, mult);
        }

        void subtractDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.subtractDoubleBufferFromArray((double[])destArray, destArrayOffset,
                (DoubleBuffer)buf, (int)firstBankOffset, count);
        }

        void absDiffDataFromFirstBank(Buffer buf, long firstBankOffset, Object destArray, int destArrayOffset,
            int count, boolean truncateOverflows)
        {
            JBuffers.absDiffOfDoubleArrayAndBuffer((double[])destArray, destArrayOffset,
                (DoubleBuffer)buf, (int)firstBankOffset, count);
        }

        @Override
        void actualizeLazyFillingBank(ByteBuffer bankBB, final long elementIndex) {
            assert lock.isHeldByCurrentThread() : "actualizeLazyFillingBank is called from non-synchronized code";
            assert (elementIndex & 7) == 0; // index is specified in elements and is even enough
            long length;
            if (ms.lazyFillingPattern == null || (length = ms.lazyFillingPattern.length()) <= elementIndex) {
                actualizeLazyZeroFillingBank(bankBB, 0, bankBB.limit());
            } else {
                DoubleBuffer bankSpecBuf = bankBB.asDoubleBuffer();
                int count = bankSpecBuf.limit();
                if (count > length - elementIndex) {
                    count = (int)(length - elementIndex);
                    actualizeLazyZeroFillingBank(bankBB, count << BYTES_PER_DOUBLE_LOG, bankBB.limit());
                }
                for (long ofs = 0; ofs < count; ) {
                    dbuf.map(elementIndex + ofs, count - ofs);
                    int len = dbuf.cnt();
                    assert len > 0;
                    bankSpecBuf.put(dbuf.data(), dbuf.from(), len); // this call shifts bankSpecBuf.position()
                    ofs += len;
                }
            }
        }

    }
    /*Repeat.AutoGeneratedEnd*/
}
