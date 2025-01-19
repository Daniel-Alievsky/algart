/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.BufferArraysImpl.*;
import net.algart.finalizing.Finalizer;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p>The memory model, storing array elements in an external file.</p>
 *
 * <p>The idea of this model is alike mapping files by standard <code>FileChannel.map</code> method.
 * But this model has two important advantages.</p>
 *
 * <p>First, here is no 32-bit restriction for the number of elements.
 * AlgART arrays use 64-bit element addressing.
 * So, an AlgART array created by this memory model can be as large as the files can.
 * In practice it means that the array size is limited only by available disk space.</p>
 *
 * <p>Second, the external files are represented by special abstraction named
 * "Data File Models" ({@link DataFileModel} interface).
 * This "data file" abstraction is much more simple than <code>RandomAccessFile</code> and similar classes,
 * usually representing disk files.
 * The are two ready implementations {@link DefaultDataFileModel}
 * and {@link StandardIODataFileModel}, really corresponding to disk files;
 * but it's not a problem to create custom implementation of the data file,
 * that will correspond to almost any type of an external storage.
 * For example, it's possible to create very trivial implementation,
 * that will interpret a usual <code>byte[]</code> array or <code>String</code> object as a "data file".</p>
 *
 * <p>Unlike the {@link SimpleMemoryModel simple memory model}, this model supports
 * primitive element types only:
 * {@link BitArray}, {@link CharArray}, {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray}, {@link FloatArray}, {@link DoubleArray}.
 * If you need to store objects in external data files, you may use the AlgART arrays,
 * created by this memory model, as a storage for combined arrays,
 * created by {@link CombinedMemoryModel}.</p>
 *
 * <p>The maximal theoretical limit for length and capacity of AlgART arrays,
 * supported by this memory model, is <code>2<sup>63</sup>-64</code> for bit arrays and
 * <code>2<sup>63</sup>/<i>size</i>-1</code> for all other element types,
 * where <code><i>size</i></code> is <code>1,2,2,4,8,4,8</code> for element types
 * <code>byte</code>, <code>char</code>, <code>short</code>, <code>int</code>, <code>long</code>,
 * <code>float</code>, <code>double</code> correspondingly.</p>
 *
 * <p>Arrays, created by this memory model, never implement {@link DirectAccessible} interface.</p>
 *
 * <p>This memory model creates a new array with help of
 * {@link DataFileModel#createTemporary(boolean)} method, that allocates new data file.
 * Immediately after this call, the newly created data file is opened via
 * {@link DataFile#open(boolean) DataFile.open(false)} call and, then, truncated
 * to the {@link DataFileModel#recommendedPrefixSize()} bytes by
 * {@link DataFile#length(long)} call.
 * (After this, if necessary, the file length will be increased by {@link DataFile#length(long) length(long)}
 * according to the array length.)
 * Due to the contract of {@link DataFile#open(boolean)} method, it means that
 * the file will always exists right now after allocating a new array:
 * even if the custom, non-standard implementation of {@link DataFileModel}
 * does not really create new file in {@link DataFileModel#createTemporary(boolean)},
 * it will be created by further {@link DataFile#open(boolean) open(false)} call.</p>
 *
 * <p>Further possible views of the created array, for example, its
 * {@link Array#subArray(long, long) subarrays} or {@link Array#asImmutable() immutable view},
 * will use the same underlying data file.</p>
 *
 * <p>A {@link MutableArray resizable array}, created by this memory model
 * ({@link #newArray(Class, long)}, {@link #newEmptyArray(Class, long)}, {@link #newEmptyArray(Class)} methods),
 * always uses the same data file, even after resizing. Increasing the array length, when necessary,
 * leads to increasing the underlying data file length ({@link DataFile#length(long)} method).
 * (Please compare: resizing arrays, created by {@link SimpleMemoryModel} / {@link BufferMemoryModel},
 * can lead to reallocation of the Java array / <code>ByteBuffer</code> and copying elements to
 * new array / <code>ByteBuffer</code>.)
 * But the contrary assertion <i>is not true</i>: reducing the array size never leads to reducing
 * the data file length. In other words, <i>the data file length only increases, but never decreases</i>.
 *
 * <p>Moreover, for a resizable array, the length of the data file is always multiple of
 * {@link DataFileModel#recommendedBankSize(boolean) DataFileModel.recommendedBankSize(false)}
 * and is increased by this step.
 * It means that resizable arrays may be not efficient enough for storing little number of elements.
 * Unlike this, an unresizable array, created by this memory model
 * ({@link #newUnresizableArray(Class, long)}, {@link #newMatrix(Class, Class, long...)
 * newMatrix(Class, Class, long...)},
 * {@link #valueOf(Object) valueOf(Object)}, etc.),
 * allocates data file with almost minimal length that is enough for storing all array elements.</p>
 *
 * <p>When the data file becomes unused, because there are no active AlgART array instances
 * based on it, it is automatically delete by garbage collector
 * with help of {@link DataFileModel#delete(DataFile)} method.
 * (The only exception is a case if you've called {@link #setTemporary(Array, boolean)
 * setTemporary(largeArray, false)} for this array.)</p>
 *
 * <p>You may also view an existing data file as an AlgART array via
 * {@link #asArray asArray} and {@link #asUpdatableArray asUpdatableArray}
 * methods of this class.</p>
 *
 * <p>Arrays, created by this memory model, have non-trivial implementation of
 * {@link Array#loadResources(ArrayContext)},
 * {@link Array#flushResources(ArrayContext)}, {@link Array#flushResources(ArrayContext, boolean)} and
 * {@link Array#freeResources(ArrayContext, boolean)} methods:
 * Namely:</p>
 *
 * <ul>
 * <li>{@link Array#loadResources(ArrayContext) Array.loadResources(context)}
 * loads the beginning of the current array from the data file and calls
 * {@link DataFile.BufferHolder#load() DataFile.BufferHolder.load()}
 * for the corresponding mapping in the data file.</li>
 *
 * <li>{@link Array#flushResources(ArrayContext, boolean) Array.flushResources(context, false)}
 * and {@link Array#flushResources(ArrayContext) Array.flushResources(context)} call
 * {@link DataFile.BufferHolder#flush(boolean) DataFile.BufferHolder.flush(false)}
 * for all active mappings in the data file.</li>
 *
 * <li>{@link Array#flushResources(ArrayContext, boolean) Array.flushResources(context, true)} calls
 * {@link DataFile#force()} for the underlying data file and
 * {@link DataFile.BufferHolder#flush(boolean) DataFile.BufferHolder.flush(true)}
 * for all active mappings in the data file.</li>
 *
 * <li>{@link Array#freeResources(ArrayContext, boolean) Array.freeResources(context, false)} calls
 * {@link DataFile#close()} for the underlying data file and
 * {@link DataFile.BufferHolder#flush(boolean) DataFile.BufferHolder.flush(false)}
 * for all active mappings in the data file.
 * Please note that it does not guarantee that this all system resources,
 * associated with this data file, will be completely released.
 * For example, the disk file can remain opened.
 * If you need to be sure that all system resources, as file handles,
 * will be freed by this method, you may use {@link StandardIODataFileModel}.</li>
 *
 * <li>{@link Array#freeResources(ArrayContext, boolean) Array.freeResources(context, true)} calls
 * {@link DataFile#force()} and {@link DataFile#close()} for the underlying data file and
 * {@link DataFile.BufferHolder#flush(boolean) DataFile.BufferHolder.flush(true)}
 * for all active mappings in the data file.</li>
 * </ul>
 *
 * <p>This class has the generic argument <code>P</code>, that describes <i>data file paths</i>
 * in the {@link DataFileModel data file model} used by this memory model.
 * The {@link #getInstance()} factory method always returns a file-oriented instance, where
 * <code>P</code> is <code>java.io.File</code>. But the {@link #getInstance(DataFileModel)} factory method
 * allows to create a memory model for any generic type <code>P</code>.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of its instance returned by {@link #getInstance()}
 * or {@link #getInstance(DataFileModel)} methods.</p>
 *
 * @param <P> type of the path to data file.
 * @author Daniel Alievsky
 */
public final class LargeMemoryModel<P> extends AbstractMemoryModel {

    static final Finalizer globalArrayFinalizer = new Finalizer();
    static final Finalizer globalMappingFinalizer = new Finalizer();
    static final Finalizer globalStorageFinalizer = new Finalizer();
    static final Logger LOGGER = Logger.getLogger(LargeMemoryModel.class.getName());

    private static final ReferenceQueue<DataFileModel<?>> reapedDataFileModels = new ReferenceQueue<>();
    static final Map<WeakReference<DataFileModel<?>>, Object> allUsedDataFileModelsWithAutoDeletion =
            new IdentityHashMap<>();
    // must be initialized before creating default instance

    static final int MAX_NUMBER_OF_BANKS_IN_LAZY_FILL_MAP = Math.max(0,
            InternalUtils.getIntProperty(
                    "net.algart.arrays.LargeMemoryModel.maxNumberOfBanksInLazyFillMap",
                    8 * 32 * 1048576));  // 32 MB for the bitmap, or ~512 TB for 2 MB banks

    static final int DELETION_SLEEP_DELAY = 100; // ms
    static final int DELETION_TIMEOUT_IN_DISPOSE = Math.max(0,
            InternalUtils.getIntProperty(
                    "net.algart.arrays.LargeMemoryModel.deletionTimeoutInDispose", 3000));  // 3 sec
    static final int DELETION_TIMEOUT_IN_FINALIZATION = Math.max(0,
            InternalUtils.getIntProperty(
                    "net.algart.arrays.LargeMemoryModel.deletionTimeoutInFinalization", 5));  // 2 attempts with 5 ms interval
    static final int DELETION_TIMEOUT_IN_CLEANER = Math.max(0,
            InternalUtils.getIntProperty(
                    "net.algart.arrays.LargeMemoryModel.deletionTimeoutInCleaner", 100));  // 0.1 sec
    static final int DELETION_LOOPS_IN_CLEANER = DefaultDataFileModel.UNSAFE_UNMAP_ON_EXIT ? 10 : 4;

    /**
     * If the finalizer cannot delete the file in this number of attempts (passes of the garbage collector),
     * the warning is logged and the attempts are stopped: the data storage is freed and collected by GC.
     */
    static final int MAX_NUMBER_OF_DELETIONS_WHILE_FINALIZATION = 100;

    private static final DataFileModel<File> GLOBAL_DATA_FILE_MODEL;

    static {
        DataFileModel<?> dfm = InternalUtils.getClassInstance(
                "net.algart.arrays.LargeMemoryModel.dataFileModel",
                DefaultDataFileModel.class.getName(),
                DataFileModel.class,
                LOGGER, "Default data file model will be used",
                "DEFAULT", DefaultDataFileModel.class.getName(),
                "STANDARD_IO", StandardIODataFileModel.class.getName());
        if (dfm.pathClass() != File.class) {
            LOGGER.severe("Illegal class specified by net.algart.arrays.LargeMemoryModel.dataFileModel: "
                    + "this data file model does not use java.io.File for describing file paths");
            LOGGER.severe("Default data file model will be used");
            dfm = new DefaultDataFileModel();
        }
        GLOBAL_DATA_FILE_MODEL = InternalUtils.cast(dfm);
    }

    private static final LargeMemoryModel<File> DEFAULT_INSTANCE = new LargeMemoryModel<>(GLOBAL_DATA_FILE_MODEL);

    private final DataFileModel<P> dataFileModel;

    private LargeMemoryModel(DataFileModel<P> dataFileModel) {
        Objects.requireNonNull(dataFileModel, "Null dataFileModel argument");
        int numberOfBanks = dataFileModel.recommendedNumberOfBanks();
        int bankSizeR = dataFileModel.recommendedBankSize(false);
        MappedDataStorages.MappingSettings.checkBankArguments(numberOfBanks, bankSizeR);
        int bankSizeU = dataFileModel.recommendedBankSize(true);
        MappedDataStorages.MappingSettings.checkBankArguments(numberOfBanks, bankSizeU);
        this.dataFileModel = dataFileModel;
        if (dataFileModel.isAutoDeletionRequested()) {
            MappedDataStorages.installTempCleaner();
            synchronized (allUsedDataFileModelsWithAutoDeletion) {
                reapDataFileModels();
                allUsedDataFileModelsWithAutoDeletion.put(
                        new WeakReference<>(dataFileModel, reapedDataFileModels), DUMMY);
            }
        }
    }

    public static final String CONSTANT_PROPERTY_NAME = "net.algart.arrays.MatrixProperty.constantValue";

    public static final String TILE_DIMENSIONS_PROPERTY_NAME = "net.algart.arrays.MatrixProperty.tileDimensions";

    /**
     * Returns default instance of this memory model.
     *
     * <p>This instance uses some standard {@link DataFileModel data file model},
     * that is determined while initializing this class
     * from the system property "net.algart.arrays.LargeMemoryModel.dataFileModel".
     * This property must contain the full name of the Java class implementing
     * {@link MemoryModel} interface. The class must have a public constructor
     * without arguments or, as a variant, static <code>getInstance()</code> method
     * without arguments returning an instance of this memory model.
     * Moreover, the class of data file paths, used by this model
     * ({@link DataFileModel#pathClass()}), must be <code>java.io.File</code>.
     * This constructor or <code>getInstance()</code> method will be used for creating the data model instance.
     *
     * <p>If there is not such system property, or if some of described conditions
     * is not fulfilled, the {@link DefaultDataFileModel} will be used as the data file model.
     *
     * <p>The following standard aliases are allowed for the class name
     * in "net.algart.arrays.LargeMemoryModel.dataFileModel" system property:<ul>
     * <li>"DEFAULT" means {@link DefaultDataFileModel},</li>
     * <li>"STANDARD_IO" means {@link StandardIODataFileModel}.</li>
     * </ul>
     *
     * @return default instance of this memory model.
     */
    public static LargeMemoryModel<File> getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Returns new instance of this memory model for the specified data file model.
     *
     * @param <P>           type of the path to data file.
     * @param dataFileModel the data file model that will be used by this memory model instance.
     * @return new instance of this memory model.
     */
    public static <P> LargeMemoryModel<P> getInstance(DataFileModel<P> dataFileModel) {
        LargeMemoryModel<P> defInst = InternalUtils.cast(DEFAULT_INSTANCE);
        return dataFileModel == defInst.dataFileModel ?
                defInst :
                new LargeMemoryModel<>(dataFileModel);
    }

    /**
     * Returns a reference to the data file model used by this memory model instance.
     *
     * @return a reference to the data file model used by this memory model instance.
     * @see #getDataFileModel(Array)
     */
    public DataFileModel<P> getDataFileModel() {
        return this.dataFileModel;
    }

    public MutableArray newEmptyArray(Class<?> elementType) {
        return newArray(elementType, 0, 0);
    }

    public MutableArray newEmptyArray(Class<?> elementType, long initialCapacity) {
        return newArray(elementType, initialCapacity, 0);
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        return newArray(elementType, initialLength, initialLength);
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0) {
            throw new IllegalArgumentException("Negative array length");
        }
        DataStorage storage;
        if (elementType == boolean.class) {
            storage = new MappedDataStorages.MappedBitStorage(getMappingSettings(elementType, true));
            UpdatableBufferBitArray result = new UpdatableBufferBitArray
                    (storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == char.class) {
            storage = new MappedDataStorages.MappedCharStorage(getMappingSettings(elementType, true));
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == byte.class) {
            storage = new MappedDataStorages.MappedByteStorage(getMappingSettings(elementType, true));
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == short.class) {
            storage = new MappedDataStorages.MappedShortStorage(getMappingSettings(elementType, true));
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == int.class) {
            storage = new MappedDataStorages.MappedIntStorage(getMappingSettings(elementType, true));
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == long.class) {
            storage = new MappedDataStorages.MappedLongStorage(getMappingSettings(elementType, true));
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == float.class) {
            storage = new MappedDataStorages.MappedFloatStorage(getMappingSettings(elementType, true));
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == double.class) {
            storage = new MappedDataStorages.MappedDoubleStorage(getMappingSettings(elementType, true));
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else {
            throw new UnsupportedElementTypeException(
                    "Only primitive element types are allowed in LargeMemoryModel (passed type: " + elementType + ")");
        }
    }

    public boolean isElementTypeSupported(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        return elementType.isPrimitive();
    }

    public boolean areAllPrimitiveElementTypesSupported() {
        return true;
    }

    public boolean areAllElementTypesSupported() {
        return false;
    }

    public long maxSupportedLength(Class<?> elementType) {
        return DataStorage.maxSupportedLengthImpl(elementType);
    }

    public boolean isCreatedBy(Array array) {
        return isLargeArray(array)
                && ((MappedDataStorages.MappedStorage) ((AbstractBufferArray) array).storage).ms.dataFileModel
                == this.dataFileModel;
    }

    /**
     * Returns <code>true</code> if the passed array was created by some instance of this memory model.
     * Returns <code>false</code> if the passed array is {@code null}
     * or an AlgART array created by another memory model.
     *
     * @param array the checked array  (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if this array is a large array created by a large memory model.
     */
    public static boolean isLargeArray(Array array) {
        return array instanceof AbstractBufferArray
                && ((AbstractBufferArray) array).storage instanceof MappedDataStorages.MappedStorage;
    }

    /**
     * Returns <code>true</code> is the passed array is temporary, that is if the corresponded
     * {@link DataFile data file} storing all its elements will be automatically deleted
     * when this AlgART array and all other arrays, sharing the same data (as {@link Array#subArray(long, long)
     * subarrays}), will become unreachable and will be finalized.
     *
     * <p>Usually this flag is <code>true</code>, if the array was created in the normal way via
     * {@link #newArray(Class, long)}
     * and similar methods, and <code>false</code>, it the arrays was created as a view of some external
     * file by {@link #asArray asArray} or {@link #asUpdatableArray asUpdatableArray} methods.
     * However, you may change this flag via {@link #setTemporary(Array, boolean)} method.
     *
     * <p>The passed argument must be created by the large memory model: {@link #isLargeArray(Array)}
     * must return <code>true</code>. In another case, <code>IllegalArgumentException</code> will be thrown.
     *
     * @param largeArray the AlgART array.
     * @return <code>true</code> if the corresponded data file is temporary and should be automatically deleted.
     * @throws NullPointerException     if <code>largeArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>largeArray</code> is not large array
     *                                  (i.e. is not create by a large memory model).
     */
    public static boolean isTemporary(Array largeArray) {
        Objects.requireNonNull(largeArray, "Null largeArray argument");
        if (!(isLargeArray(largeArray))) {
            throw new IllegalArgumentException("The passed argument is not a large array");
        }
        return ((MappedDataStorages.MappedStorage) ((AbstractBufferArray) largeArray).storage).isAutoDeleted();
    }

    /**
     * Changes temporary status, returned by {@link #isTemporary(Array)} method.
     *
     * <p>When <code>value=false</code>, this method allows to prevent from deletion of the data file
     * containing results of some algorithm,
     * which created an array with usual {@link #newArray(Class, long)} or similar method.
     * In this case, please not forget to call
     * <code>largeArray.{@link Array#flushResources(ArrayContext) flushResources}(context)</code>
     * after completion of filling this array;
     * in another case, some array elements can be lost while further garbage collection or JVM termination.
     *
     * <p>When <code>value=true</code>, this method allows to safely delete the external data file,
     * mapped to an AlgART array via {@link #asArray asArray} or
     * {@link #asUpdatableArray asUpdatableArray} methods,
     * when the last mapping will be successfully closed.
     *
     * <p>This method also sets the temporary flag for <i>all</i> arrays, that share data with this one.
     *
     * <p>The passed argument must be created by the large memory model: {@link #isLargeArray}
     * must return <code>true</code>. In another case, <code>IllegalArgumentException</code> will be thrown.
     *
     * @param largeArray the AlgART array.
     * @param value      new temporary status for this array.
     * @throws NullPointerException     if <code>largeArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>largeArray</code> is not large array
     *                                  (i.e. is not create by a large memory model).
     */
    public static void setTemporary(Array largeArray, boolean value) {
        Objects.requireNonNull(largeArray, "Null largeArray argument");
        if (!(isLargeArray(largeArray))) {
            throw new IllegalArgumentException("The passed argument is not a large array");
        }
        ((MappedDataStorages.MappedStorage) ((AbstractBufferArray) largeArray).storage).setAutoDeleted(value);
    }

    /**
     * Returns the {@link DataFileModel model} of the {@link DataFile data file}
     * storing all elements of the passed AlgART array.
     *
     * <p>The passed argument must be created by the large memory model: {@link #isLargeArray(Array)}
     * must return <code>true</code>. In another case, <code>IllegalArgumentException</code> will be thrown.
     *
     * @param largeArray the AlgART array.
     * @return the data file model used in this array.
     * @throws NullPointerException     if <code>largeArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>largeArray</code> is not large array
     *                                  (i.e. is not create by a large memory model).
     */
    public static DataFileModel<?> getDataFileModel(Array largeArray) {
        Objects.requireNonNull(largeArray, "Null largeArray argument");
        if (!(isLargeArray(largeArray))) {
            throw new IllegalArgumentException("The passed argument is not a large array");
        }
        return ((MappedDataStorages.MappedStorage) ((AbstractBufferArray) largeArray).storage).getDataFileModel();
    }

    /**
     * Returns the {@link DataFileModel#getPath(DataFile) path} to the {@link DataFile data file}
     * storing all elements of the passed AlgART array.
     *
     * <p>Note: returned data file may contain extra data besides the content
     * of the passed AlgART array.
     *
     * <p>Note: <i>you should not try to work with the returned file until full JVM exit</i>!
     * The data file, used by this array, may be opened and accessed until the moment,
     * when all connected Java objects will be successfully finalized by the garbage collector,
     * and there is no guarantee that it will occur at all.
     * So, the results of attempts of simultaneous access this file from your code are unspecified.
     *
     * <p>But you may use this method for storing file name somewhere, to open it again
     * on the next application start
     * via {@link #asArray asArray} or {@link #asUpdatableArray asUpdatableArray} methods.
     *
     * <p>The passed argument must be created by the large memory model: {@link #isLargeArray(Array)}
     * must return <code>true</code>. In another case, <code>IllegalArgumentException</code> will be thrown.
     *
     * <p>The passed argument <i>may</i> be created by another instance of the large memory model
     * than this one. But the {@link #getDataFileModel(Array) data file model of that memory model},
     * which was used for creating the passed array, must use the same class (or an inheritor)
     * for data file paths (see {@link DataFileModel#pathClass()}),
     * as the data file model of this large memory model. In another case, <code>ClassCastException</code>
     * will be thrown.
     *
     * @param largeArray the AlgART array.
     * @return the data file model used in this array.
     * @throws NullPointerException     if <code>largeArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>largeArray</code> is not large array
     *                                  (i.e. is not created by a large memory model).
     * @throws ClassCastException       if <code>largeArray</code> is created by the large memory model,
     *                                  based on illegal data file model (incompatible with the required
     *                                  <code>P</code> type of data file paths).
     */
    public P getDataFilePath(Array largeArray) {
        Objects.requireNonNull(largeArray, "Null largeArray argument");
        if (!(isLargeArray(largeArray))) {
            throw new IllegalArgumentException("The passed argument is not a large array");
        }
        Object path = ((MappedDataStorages.MappedStorage)
                ((AbstractBufferArray) largeArray).storage).getDataFilePath();
        return dataFileModel.pathClass().cast(path);
    }

    /**
     * Returns this memory model, cast to the specified generic type of the data file paths,
     * or throws <code>ClassCastException</code> if the current type
     * <code>{@link #getDataFileModel() getDataFileModel()}.{@link DataFileModel#pathClass() pathClass()}</code>
     * cannot be cast to the passed type <code>pathClass</code>.
     * This method always returns the reference to this instance
     * and is compiled without "unchecked cast" warning.
     *
     * @param pathClass the type of the data file paths in the returned memory model.
     * @return this memory model, cast to the required type of the data file paths.
     * @throws NullPointerException if the argument is {@code null}.
     * @throws ClassCastException   if the current type of the data file paths cannot be cast to the required type.
     */
    public <U> LargeMemoryModel<U> cast(Class<U> pathClass) {
        if (!pathClass.isAssignableFrom(dataFileModel.pathClass())) {
            throw new ClassCastException("Cannot cast " + LargeMemoryModel.class.getName()
                    + "<" + dataFileModel.pathClass().getName()
                    + "> to the type " + LargeMemoryModel.class.getName()
                    + "<" + pathClass.getName() + ">");
        }
        return InternalUtils.cast(this);
    }

    /**
     * Returns <code>true</code> if the passed large array was created by
     * {@link #asArray(Object, Class, long, long, ByteOrder)} method
     * or one of its versions for concrete element types
     * ({@link #asBitArray(Object, long, long, ByteOrder)},
     * {@link #asCharArray(Object, long, long, ByteOrder)}, etc.),
     * or if the passed argument is a view of such an array.
     *
     * <p>This method allows to check whether the original array, reflecting some external data file,
     * was created immutable. If (and only if) this method returns <code>true</code>,
     * you can be sure that this array and, moreover, <i>all</i> AlgART arrays,
     * possibly sharing the data with this one, are {@link Array#isImmutable() immutable}.
     *
     * <p>(The only way to create such "strongly immutable" array is using
     * {@link #asArray(Object, Class, long, long, ByteOrder)} method
     * and its versions for concrete element types. Usually, an AlgART array
     * is created updatable via allocation methods of {@link MemoryModel},
     * and only some its views, created with help of {@link Array#asImmutable()} method,
     * are immutable.)
     *
     * <p>The passed argument must be created by the large memory model: {@link #isLargeArray(Array)}
     * must return <code>true</code>. In another case, <code>IllegalArgumentException</code> will be thrown.
     *
     * @param largeArray the AlgART array.
     * @return <code>true</code> if this array and any other arrays, sharing data with it, are immutable.
     * @throws NullPointerException     if <code>largeArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>largeArray</code> is not large array
     *                                  (i.e. is not created by a large memory model).
     */
    public static boolean isCreatedReadOnly(Array largeArray) {
        Objects.requireNonNull(largeArray, "Null largeArray argument");
        if (!(isLargeArray(largeArray))) {
            throw new IllegalArgumentException("The passed argument is not a large array");
        }
        return ((MappedDataStorages.MappedStorage) ((AbstractBufferArray) largeArray).storage).ms.readOnly;
    }

    /**
     * The special value for <code>fileAreaSize</code> argument in {@link #asArray asArray}
     * and {@link #asUpdatableArray asUpdatableArray}
     * methods, that means the region from the specified position to the file end.
     * Please note that this value is not applicable for {@link #asUpdatableArray asUpdatableArray}
     * method when its <code>truncate</code> argument is <code>true</code> or if the current file length
     * is less than <code>filePosition</code> argument: in these cases, <code>ALL_FILE</code> value is equivalent
     * to <code>0</code>.
     */
    public static final long ALL_FILE = -1;

    /**
     * Returns an {@link Array#isImmutable() immutable} AlgART array with specified element type,
     * backed by the content of a region of the {@link DataFile data file} with specified name.
     * To access the file, this method will convert the <code>filePath</code> to {@link DataFile} instance
     * by <code>{@link DataFileModel#getDataFile dataFileModel.getDataFile(filePath,byteOrder)}</code> call,
     * where <code>dataFileModel</code> is the {@link #getDataFileModel() current data file model}
     * used by this memory model instance.
     *
     * <p>The array element <code>#0</code> will correspond to the <i>byte</i> <code>#filePosition</code>
     * of the file. So, for bit array, the bit <code>#0</code> of the resulting array will be the low
     * bit of the byte <code>value</code> at the specified position (<code>value&amp;0x1</code>);
     * for <code>int</code> array, the <code>int</code> element <code>#0</code> will occupy
     * the bytes <code>#filePosition..#filePosition+3</code>; etc.
     *
     * <p>The length of the returned array will be equal to <code>fileAreaSize/bytesPerElement</code>,
     * where <code>bytesPerElement</code> is 2, 1, 2, 4, 8, 4, 8 for array of <code>char</code>,
     * <code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code>
     * correspondingly. For a case of bit array, the length of the returned array will be equal
     * to <code>(fileAreaSize/8)*64</code>. So, all array content never exceeds the specified range
     * <code>filePosition..filePosition+fileAreaSize-1</code>, but several bytes at the end of this range
     * may be unused.
     *
     * <p>If the end of specified region, <code>filePosition+fileAreaSize</code>,
     * exceeds the current file length, the <code>IndexOutOfBoundsException</code> is thrown.
     *
     * <p>The <code>fileAreaSize</code> is equal to the special value {@link #ALL_FILE}, then
     * it is automatically replaced with <code>fileLength-filePosition</code>,
     * where <code>fileLength</code> is the current file length in bytes.
     * So, you may pass <code>0</code> as <code>filePosition</code> and {@link #ALL_FILE} value as
     * <code>fileAreaSize</code> to view all the file as an AlgART array.
     *
     * <p>In all cases besides an array of <code>bytes</code>,
     * the order of bytes in every array element, placed in the file, or in some group of elements
     * (probably 64) in a case of bit array, is specified by <code>byteOrder</code> argument.
     * However, for bit array, the order of bits inside every byte is always "little endian":
     * the element <code>#k</code> in the returned bit array corresponds to the bit
     * <code>byteValue&amp;(1&lt;&lt;(k%8))</code>, where <code>byteValue</code> is a byte at position
     * depending on <code>byteOrder</code>.
     *
     * <p>This method opens the data file in read-only mode
     * by {@link DataFile#open(boolean) DataFile.open(true)} method.
     *
     * <p>If the data file with the specified path <code>filePath</code> does not exist yet,
     * <code>FileNotFoundException</code> is thrown.
     *
     * <p>After calling this method, you should not operate with this file in another ways
     * than using returned AlgART array, else the results ot such operations will be unspecified.
     *
     * <p>Please note: the returned AlgART array may work faster than an updatable array
     * returned by {@link #asUpdatableArray} for the same file.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param elementType  the type of array elements.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException               if some I/O error occurred while opening the file. (In a case of any I/O
     *                                   problems while further accesses to returned array,
     *                                   <code>java.io.IOError</code>
     *                                   will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException      if <code>filePath</code>, <code>elementType</code> or
     *                                   <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException  if <code>filePosition</code> is negative or
     *                                   if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE}.
     * @throws IndexOutOfBoundsException if the specified region exceeds the current file length.
     * @see #asUpdatableArray(Object, Class, long, long, boolean, ByteOrder)
     * @see #asBitArray(Object, long, long, ByteOrder)
     * @see #asCharArray(Object, long, long, ByteOrder)
     * @see #asByteArray(Object, long, long, ByteOrder)
     * @see #asShortArray(Object, long, long, ByteOrder)
     * @see #asIntArray(Object, long, long, ByteOrder)
     * @see #asLongArray(Object, long, long, ByteOrder)
     * @see #asFloatArray(Object, long, long, ByteOrder)
     * @see #asDoubleArray(Object, long, long, ByteOrder)
     * @see Arrays#toArray(UpdatablePArray, byte[], ByteOrder)
     * @see Arrays#read(java.io.InputStream, UpdatablePArray, ByteOrder)
     */
    public PArray asArray(
            P filePath,
            Class<?> elementType,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        Objects.requireNonNull(filePath, "Null filePath argument");
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (filePosition < 0) {
            throw new IllegalArgumentException("Negative filePosition argument");
        }
        if (fileAreaSize < 0 && fileAreaSize != ALL_FILE) {
            throw new IllegalArgumentException("Negative fileAreaSize argument");
        }
        Objects.requireNonNull(byteOrder, "Null byteOrder argument");
        try {
            DataFile file = dataFileModel.getDataFile(filePath, byteOrder);
            fileAreaSize = checkFilePositionAndAreaSize(file, filePosition, fileAreaSize, false, true);
            if (elementType == boolean.class && fileAreaSize > Long.MAX_VALUE >>> 3) {
                throw new TooLargeArrayException("Too large desired bit array length: >2^63-1 bits ("
                        + fileAreaSize + " bytes)");
            }
            DataStorage storage;
            AbstractBufferArray result;
            if (elementType == boolean.class) {
                storage = new MappedDataStorages.MappedBitStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = (fileAreaSize >> DataStorage.BYTES_PER_LONG_LOG) << 6;
                result = new BufferBitArray(storage, length, length, 0L, false);
            } else if (elementType == byte.class) {
                storage = new MappedDataStorages.MappedByteStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                result = new BufferByteArray(storage, fileAreaSize, fileAreaSize, 0L, false);
            } else if (elementType == char.class) {
                storage = new MappedDataStorages.MappedCharStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_CHAR_LOG;
                result = new BufferCharArray(storage, length, length, 0L, false);
            } else if (elementType == short.class) {
                storage = new MappedDataStorages.MappedShortStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_SHORT_LOG;
                result = new BufferShortArray(storage, length, length, 0L, false);
            } else if (elementType == int.class) {
                storage = new MappedDataStorages.MappedIntStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_INT_LOG;
                result = new BufferIntArray(storage, length, length, 0L, false);
            } else if (elementType == long.class) {
                storage = new MappedDataStorages.MappedLongStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_LONG_LOG;
                result = new BufferLongArray(storage, length, length, 0L, false);
            } else if (elementType == float.class) {
                storage = new MappedDataStorages.MappedFloatStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_FLOAT_LOG;
                result = new BufferFloatArray(storage, length, length, 0L, false);
            } else if (elementType == double.class) {
                storage = new MappedDataStorages.MappedDoubleStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, true, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_DOUBLE_LOG;
                result = new BufferDoubleArray(storage, length, length, 0L, false);
            } else {
                throw new UnsupportedElementTypeException(
                        "Only primitive element types are allowed in LargeMemoryModel (passed type: "
                                + elementType + ")");
            }
            BufferArraysImpl.forgetOnDeallocation(result);
            if (filePosition == 0) {
                result.setNewReadOnlyViewStatus();
            }
            return (PArray) result;
        } catch (IOError e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            } else {
                throw e;
            }
        }
    }

    /**
     * Returns an {@link Array#isUnresizable() unresizable} AlgART array with specified element type,
     * backed by the content of a region of the {@link DataFile data file} with specified name.
     * To access the file, this method will convert the <code>filePath</code> to {@link DataFile} instance
     * by <code>{@link DataFileModel#getDataFile dataFileModel.getDataFile(filePath,byteOrder)}</code> call,
     * where <code>dataFileModel</code> is the {@link #getDataFileModel() current data file model}
     * used by this memory model instance.
     *
     * <p>The changes performed in the returned array will be reflected in the specified data file.
     * But the changed data can be not really stored at the external device until you call
     * {@link Array#flushResources(ArrayContext) Array.flushResources} or
     * {@link Array#freeResources(ArrayContext) Array.freeResources} method.
     *
     * <p>The array element <code>#0</code> will correspond to the <i>byte</i> <code>#filePosition</code>
     * of the file. So, for bit array, the bit <code>#0</code> of the resulting array will be the low
     * bit of the byte <code>value</code> at the specified position (<code>value&amp;0x1</code>);
     * for <code>int</code> array, the <code>int</code> element <code>#0</code> will occupy
     * the bytes <code>#filePosition..#filePosition+3</code>; etc.
     *
     * <p>The length of the returned array will be equal to <code>fileAreaSize/bytesPerElement</code>,
     * where <code>bytesPerElement</code> is 2, 1, 2, 4, 8, 4, 8 for array of <code>char</code>,
     * <code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code>
     * correspondingly. For a case of bit array, the length of the returned array will be equal
     * to <code>(fileAreaSize/8)*64</code>. So, all array content never exceeds the specified range
     * <code>filePosition..filePosition+fileAreaSize-1</code>, but several bytes at the end of this range
     * may be unused.
     *
     * <p>If the end of specified region (<code>filePosition+fileAreaSize</code>)
     * exceeds the current file length, or if the <code>truncate</code> argument is <code>true</code>,
     * the file length is automatically set to the <code>filePosition+fileAreaSize</code> bytes.
     *
     * <p>The <code>fileAreaSize</code> is equal to the special value {@link #ALL_FILE}, then
     * it is automatically replaced with <code>fileLength-filePosition</code>,
     * where <code>fileLength</code> is the current file length in bytes.
     * So, you may pass <code>0</code> as <code>filePosition</code> and {@link #ALL_FILE} value as
     * <code>fileAreaSize</code> to view all the file as an AlgART array.
     * In this case, if the current file length is less than <code>filePosition</code>
     * or the <code>truncate</code> argument is <code>true</code>,
     * the file length is automatically set to <code>filePosition</code>.
     *
     * <p>In all cases besides an array of <code>bytes</code>,
     * the order of bytes in every array element, placed in the file, or in some group of elements
     * (probably 64) in a case of bit array, is specified by <code>byteOrder</code> argument.
     * However, for bit array, the order of bits inside every byte is always "little endian":
     * the element <code>#k</code> in the returned bit array corresponds to the bit
     * <code>byteValue&amp;(1&lt;&lt;(k%8))</code>, where <code>byteValue</code> is a byte at position
     * depending on <code>byteOrder</code>.
     *
     * <p>This method opens the data file in read-write mode
     * by {@link DataFile#open(boolean) DataFile.open(false)} method.
     *
     * <p>If the data file with the specified path <code>filePath</code> does not exist yet,
     * this method tries to create it at the specified path (see comments to
     * {@link DataFile#open(boolean)}). In this case, the initial file length is set
     * to <code>filePosition+fileAreaSize</code>, and the initial file content is unspecified.
     * If <code>fileAreaSize={@link #ALL_FILE}</code>, the results will be the same as if
     * <code>fileAreaSize=0</code>.
     *
     * <p>After calling this method, you should not operate with this file in another ways
     * than using returned AlgART array, else the results ot such operations will be unspecified.
     *
     * <p>Please note: the returned AlgART array may work slower than an immutable array
     * returned by {@link #asArray} for the same file. If you need only reading
     * the returned array, you should prefer {@link #asArray asArray} method.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param elementType  the type of array elements.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set to
     *                     <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code>, <code>elementType</code> or
     *                                  <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative or
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE}.
     * @see #asArray(Object, Class, long, long, ByteOrder)
     * @see #asUpdatableBitArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableCharArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableByteArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableShortArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableIntArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableLongArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableFloatArray(Object, long, long, boolean, ByteOrder)
     * @see #asUpdatableDoubleArray(Object, long, long, boolean, ByteOrder)
     * @see Arrays#toBytes(byte[], PArray, ByteOrder)
     * @see Arrays#write(java.io.OutputStream, PArray, ByteOrder)
     */
    public UpdatablePArray asUpdatableArray(
            P filePath,
            Class<?> elementType,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        Objects.requireNonNull(filePath, "Null filePath argument");
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (filePosition < 0) {
            throw new IllegalArgumentException("Negative filePosition argument");
        }
        if (fileAreaSize < 0 && fileAreaSize != ALL_FILE) {
            throw new IllegalArgumentException("Negative fileAreaSize argument");
        }
        try {
            DataFile file = dataFileModel.getDataFile(filePath, byteOrder);
            fileAreaSize = checkFilePositionAndAreaSize(file, filePosition, fileAreaSize, truncate, false);
            if (elementType == boolean.class && fileAreaSize > Long.MAX_VALUE >>> 3) {
                throw new TooLargeArrayException("Too large desired bit array length: >2^63-1 bits ("
                        + fileAreaSize + " bytes)");
            }
            DataStorage storage;
            AbstractBufferArray result;
            if (elementType == boolean.class) {
                storage = new MappedDataStorages.MappedBitStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = (fileAreaSize >> DataStorage.BYTES_PER_LONG_LOG) << 6;
                result = new UpdatableBufferBitArray(storage, length, length, 0L, false);
            } else if (elementType == byte.class) {
                storage = new MappedDataStorages.MappedByteStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                result = new UpdatableBufferByteArray(storage,
                        fileAreaSize, fileAreaSize, 0L, false);
            } else if (elementType == char.class) {
                storage = new MappedDataStorages.MappedCharStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_CHAR_LOG;
                result = new UpdatableBufferCharArray(storage, length, length, 0L, false);
            } else if (elementType == short.class) {
                storage = new MappedDataStorages.MappedShortStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_SHORT_LOG;
                result = new UpdatableBufferShortArray(storage, length, length, 0L, false);
            } else if (elementType == int.class) {
                storage = new MappedDataStorages.MappedIntStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_INT_LOG;
                result = new UpdatableBufferIntArray(storage, length, length, 0L, false);
            } else if (elementType == long.class) {
                storage = new MappedDataStorages.MappedLongStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_LONG_LOG;
                result = new UpdatableBufferLongArray(storage, length, length, 0L, false);
            } else if (elementType == float.class) {
                storage = new MappedDataStorages.MappedFloatStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_FLOAT_LOG;
                result = new UpdatableBufferFloatArray(storage, length, length, 0L, false);
            } else if (elementType == double.class) {
                storage = new MappedDataStorages.MappedDoubleStorage(getMappingSettings(
                        dataFileModel, file, filePosition,
                        filePosition + fileAreaSize, false, elementType));
                long length = fileAreaSize >> DataStorage.BYTES_PER_DOUBLE_LOG;
                result = new UpdatableBufferDoubleArray(storage, length, length, 0L, false);
            } else {
                throw new UnsupportedElementTypeException(
                        "Only primitive element types are allowed in LargeMemoryModel (passed type: " +
                                elementType + ")");
            }
            BufferArraysImpl.forgetOnDeallocation(result);
            return (UpdatablePArray) result;
        } catch (Error e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            } else {
                throw e;
            }
        }
    }



    /*Repeat() boolean(?=\.class) ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit                ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double */

    /**
     * Equivalent to <code>(BitArray){@link #asArray
     * asArray(filePath, boolean.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public BitArray asBitArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (BitArray) asArray(filePath, boolean.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableBitArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, boolean.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableBitArray asUpdatableBitArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableBitArray) asUpdatableArray(filePath, boolean.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <code>(CharArray){@link #asArray
     * asArray(filePath, char.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public CharArray asCharArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (CharArray) asArray(filePath, char.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableCharArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, char.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableCharArray asUpdatableCharArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableCharArray) asUpdatableArray(filePath, char.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }

    /**
     * Equivalent to <code>(ByteArray){@link #asArray
     * asArray(filePath, byte.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public ByteArray asByteArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (ByteArray) asArray(filePath, byte.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableByteArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, byte.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableByteArray asUpdatableByteArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableByteArray) asUpdatableArray(filePath, byte.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }

    /**
     * Equivalent to <code>(ShortArray){@link #asArray
     * asArray(filePath, short.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public ShortArray asShortArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (ShortArray) asArray(filePath, short.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableShortArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, short.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableShortArray asUpdatableShortArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableShortArray) asUpdatableArray(filePath, short.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }

    /**
     * Equivalent to <code>(IntArray){@link #asArray
     * asArray(filePath, int.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public IntArray asIntArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (IntArray) asArray(filePath, int.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableIntArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, int.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableIntArray asUpdatableIntArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableIntArray) asUpdatableArray(filePath, int.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }

    /**
     * Equivalent to <code>(LongArray){@link #asArray
     * asArray(filePath, long.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public LongArray asLongArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (LongArray) asArray(filePath, long.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableLongArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, long.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableLongArray asUpdatableLongArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableLongArray) asUpdatableArray(filePath, long.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }

    /**
     * Equivalent to <code>(FloatArray){@link #asArray
     * asArray(filePath, float.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public FloatArray asFloatArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (FloatArray) asArray(filePath, float.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableFloatArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, float.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableFloatArray asUpdatableFloatArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableFloatArray) asUpdatableArray(filePath, float.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }

    /**
     * Equivalent to <code>(DoubleArray){@link #asArray
     * asArray(filePath, double.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public DoubleArray asDoubleArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            ByteOrder byteOrder)
            throws IOException {
        return (DoubleArray) asArray(filePath, double.class, filePosition, fileAreaSize, byteOrder);
    }

    /**
     * Equivalent to <code>(UpdatableDoubleArray){@link #asUpdatableArray
     * asUpdatableArray(filePath, double.class, filePosition, fileAreaSize, byteOrder)}</code>.
     *
     * @param filePath     the name of {@link DataFile data file} that will be viewed as an AlgART array.
     * @param filePosition the starting position of the viewed region in the data file, in bytes.
     * @param fileAreaSize the size of the viewed region in the data file, in bytes.
     * @param truncate     if <code>true</code>, the file length is always set
     *                     to <code>filePosition+fileAreaSize</code>
     *                     (or to <code>filePosition</code> if <code>fileAreaSize=={@link #ALL_FILE}</code>);
     *                     if <code>false</code>, the file length is set to this value only if it is greater
     *                     than the current file length.
     * @param byteOrder    the byte order that will be used for accessing the data file.
     * @return a view of the specified region of the data file as an AlgART array.
     * @throws IOException              if some I/O error occurred while opening the file. (In a case of any I/O
     *                                  problems while further accesses to returned array, <code>java.io.IOError</code>
     *                                  will be thrown instead of <code>IOException</code>.)
     * @throws NullPointerException     if <code>filePath</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>filePosition</code> is negative,
     *                                  if <code>fileAreaSize</code> is negative and not equal to {@link #ALL_FILE},
     *                                  or if the specified region exceeds the current file length.
     */
    public UpdatableDoubleArray asUpdatableDoubleArray(
            P filePath,
            long filePosition,
            long fileAreaSize,
            boolean truncate,
            ByteOrder byteOrder)
            throws IOException {
        return (UpdatableDoubleArray) asUpdatableArray(filePath, double.class,
                filePosition, fileAreaSize, truncate, byteOrder);
    }
    /*Repeat.AutoGeneratedEnd*/

    public static Matrix<? extends PArray> asConstantMatrix(MatrixInfo matrixInfo) throws IllegalInfoSyntaxException {
        Objects.requireNonNull(matrixInfo, "Null matrixInfo argument");
        return getNCopiesMatrix(matrixInfo, matrixInfo.additionalProperties());
    }

    public Matrix<? extends PArray> asMatrix(P filePath, MatrixInfo matrixInfo)
            throws IOException, IllegalInfoSyntaxException {
        Objects.requireNonNull(matrixInfo, "Null matrixInfo argument");
        Map<String, String> additionalProperties = matrixInfo.additionalProperties();
        final Matrix<? extends PArray> nCopiesMatrix = getNCopiesMatrix(matrixInfo, additionalProperties);
        if (nCopiesMatrix != null) {
            return nCopiesMatrix;
        }
        PArray array = asArray(filePath,
                matrixInfo.elementType(),
                matrixInfo.dataOffset(), ALL_FILE,
                matrixInfo.byteOrder());
        array = (PArray) array.subArr(0, Math.min(array.length(), matrixInfo.size()));
        // for example, for bit arrays it can mean uneven number of bytes
        // "min" because matrix information may be damaged: in this case its better to produce exception little later
        if (matrixInfo.dataOffset() == 0) {
            ((AbstractArray) array).setNewReadOnlyViewStatus();
        }
        Matrix<? extends PArray> matrix = array.matrix(matrixInfo.dimensions());
        long[] tileDimensions = getTileDimensions(matrix.dimCount(), matrixInfo.additionalProperties());
        if (tileDimensions != null) {
            matrix = matrix.tile(tileDimensions);
        }
        return matrix;
    }

    public Matrix<? extends UpdatablePArray> asUpdatableMatrix(P filePath, MatrixInfo matrixInfo)
            throws IOException, IllegalInfoSyntaxException {
        Objects.requireNonNull(matrixInfo, "Null matrixInfo argument");
        Map<String, String> additionalProperties = matrixInfo.additionalProperties();
        PArray nCopiesArray = getNCopiesArray(matrixInfo.elementType(), matrixInfo.size(), additionalProperties);
        if (nCopiesArray != null) {
            throw new IOException("Constant matrix cannot be loaded as updatable one");
        }
        UpdatablePArray array = asUpdatableArray(filePath,
                matrixInfo.elementType(),
                matrixInfo.dataOffset(), ALL_FILE, false,
                matrixInfo.byteOrder());
        array = array.subArr(0, Math.min(array.length(), matrixInfo.size()));
        // for example, for bit arrays it can mean uneven number of bytes
        // "min" because matrix information may be damaged: in this case its better to produce exception later
        Matrix<? extends UpdatablePArray> matrix = Matrices.matrix(array, matrixInfo.dimensions());
        long[] tileDimensions = getTileDimensions(matrix.dimCount(), additionalProperties);
        if (tileDimensions != null) {
            matrix = matrix.tile(tileDimensions);
        }
        return matrix;
    }

    public static MatrixInfo getMatrixInfoForSavingInFile(Matrix<? extends PArray> matrix, long dataOffset) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        MatrixInfo matrixInfo = MatrixInfo.valueOf(matrix, dataOffset);
        Map<String, String> properties = new LinkedHashMap<>();
        if (Arrays.isNCopies(matrix.array())) {
            properties.put(CONSTANT_PROPERTY_NAME, getNCopiesArrayDescription(matrix.array()));
        }
        if (matrix.isTiled()) {
            properties.put(TILE_DIMENSIONS_PROPERTY_NAME, getTileDimensionsDescription(matrix.tileDimensions()));
        }
        if (!properties.isEmpty()) {
            matrixInfo = matrixInfo.cloneWithOtherAdditionalProperties(properties);
        }
        return matrixInfo;
    }

    public static PArray getRawArrayForSavingInFile(Matrix<? extends PArray> matrix) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        if (Arrays.isNCopies(matrix.array())) {
            return null;
        }
        if (matrix.isTiled()) {
            return matrix.tileParent().array();
        } else {
            return matrix.array();
        }
    }

    /**
     * See comments to {@link MemoryModel#newLazyCopy(Array)} method.
     *
     * @param array the source array.
     * @return the lazy copy of the source array.
     * @throws NullPointerException            if the argument is {@code null}.
     * @throws UnsupportedElementTypeException if the element type of the passed array
     *                                         is not supported by this memory model.
     */
    public MutableArray newLazyCopy(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        Class<?> elementType = array.elementType();
        long length = array.length();
        DataStorage storage;
        if (elementType == boolean.class) {
            storage = new MappedDataStorages.MappedBitStorage(getMappingSettings((PArray) array, false));
            MutableBufferBitArray result = new MutableBufferBitArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == char.class) {
            storage = new MappedDataStorages.MappedCharStorage(getMappingSettings((PArray) array, false));
            MutableBufferCharArray result = new MutableBufferCharArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == byte.class) {
            storage = new MappedDataStorages.MappedByteStorage(getMappingSettings((PArray) array, false));
            MutableBufferByteArray result = new MutableBufferByteArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == short.class) {
            storage = new MappedDataStorages.MappedShortStorage(getMappingSettings((PArray) array, false));
            MutableBufferShortArray result = new MutableBufferShortArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == int.class) {
            storage = new MappedDataStorages.MappedIntStorage(getMappingSettings((PArray) array, false));
            MutableBufferIntArray result = new MutableBufferIntArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == long.class) {
            storage = new MappedDataStorages.MappedLongStorage(getMappingSettings((PArray) array, false));
            MutableBufferLongArray result = new MutableBufferLongArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == float.class) {
            storage = new MappedDataStorages.MappedFloatStorage(getMappingSettings((PArray) array, false));
            MutableBufferFloatArray result = new MutableBufferFloatArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == double.class) {
            storage = new MappedDataStorages.MappedDoubleStorage(getMappingSettings((PArray) array, false));
            MutableBufferDoubleArray result = new MutableBufferDoubleArray(storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else {
            throw new UnsupportedElementTypeException(
                    "Only primitive element types are allowed in LargeMemoryModel (passed array: " + array + ")");
        }
    }

    /**
     * See comments to {@link MemoryModel#newUnresizableLazyCopy(Array)} method.
     *
     * @param array the source array.
     * @return the lazy unresizable copy of the source array.
     * @throws NullPointerException            if the argument is {@code null}.
     * @throws UnsupportedElementTypeException if the element type of the passed array
     *                                         is not supported by this memory model.
     */
    public UpdatableArray newUnresizableLazyCopy(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        Class<?> elementType = array.elementType();
        long length = array.length();
        DataStorage storage;
        if (elementType == boolean.class) {
            storage = new MappedDataStorages.MappedBitStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferBitArray result = new UpdatableBufferBitArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == char.class) {
            storage = new MappedDataStorages.MappedCharStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferCharArray result = new UpdatableBufferCharArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == byte.class) {
            storage = new MappedDataStorages.MappedByteStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferByteArray result = new UpdatableBufferByteArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == short.class) {
            storage = new MappedDataStorages.MappedShortStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferShortArray result = new UpdatableBufferShortArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == int.class) {
            storage = new MappedDataStorages.MappedIntStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferIntArray result = new UpdatableBufferIntArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == long.class) {
            storage = new MappedDataStorages.MappedLongStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferLongArray result = new UpdatableBufferLongArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == float.class) {
            storage = new MappedDataStorages.MappedFloatStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferFloatArray result = new UpdatableBufferFloatArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == double.class) {
            storage = new MappedDataStorages.MappedDoubleStorage(getMappingSettings((PArray) array, true));
            UpdatableBufferDoubleArray result = new UpdatableBufferDoubleArray(
                    storage, length, length, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else {
            throw new UnsupportedElementTypeException(
                    "Only primitive element types are allowed in LargeMemoryModel (passed array: " + array + ")");
        }
    }

    /**
     * Returns the current number of internal finalization tasks that are scheduled by this model to free
     * unused system resources, but are not fully performed yet.
     *
     * <p>If all tasks are performed, returns 0.
     * In this case, for example, you can be sure that all temporary files, created by this memory model
     * for storing AlgART arrays, are already removed.
     * You may use this fact to create a method working alike {@link Arrays#gcAndAwaitFinalization(int)}.
     *
     * @return the current number of finalization tasks, that are not performed yet.
     */
    public static int activeFinalizationTasksCount() {
        return globalMappingFinalizer.activeTasksCount() + globalStorageFinalizer.activeTasksCount();
    }

    /**
     * Returns the current number of AlgART arrays that were created by not finalized yet by this model.
     * While this value is non-zero, the
     * {@link #activeFinalizationTasksCount() number of internal finalization tasks} cannot become zero.
     * This information may be useful for debugging.
     *
     * @return the current number of AlgART arrays, created by this model,
     * but not deallocated by the garbage collector yet.
     */
    public static int activeArrayFinalizationTasksCount() {
        return globalArrayFinalizer.activeTasksCount();
    }

    /**
     * Returns the current number of
     * {@link DataFile#map(net.algart.arrays.DataFile.Range, boolean) mapped blocks}
     * that were created by not finalized yet by this model.
     * While this value is non-zero, the
     * {@link #activeFinalizationTasksCount() number of internal finalization tasks} cannot become zero.
     * This information may be useful for debugging.
     *
     * @return the current number of mapped blocks, created by this model,
     * but not deallocated by the garbage collector yet.
     */
    public static int activeMappingFinalizationTasksCount() {
        return globalMappingFinalizer.activeTasksCount();
    }

    /**
     * Returns a newly allocated copy of the set of all {@link DataFileModel data file models}, that were used
     * in any instances of this class (as constructor arguments) since the application start.
     * The data file model is included into this set only
     * if its {@link DataFileModel#isAutoDeletionRequested()}
     * method returns <code>true</code>.
     *
     * <p>If some instance of data file model is absolutely unreachable,
     * because it was utilized by the garbage collector, and
     * there are no any AlgART arrays using this data file model,
     * then this instance may be excluded from this set.
     * (The reason is that the used data file models are registered in a global collection
     * via weak references, to avoid possible memory leak while creating a lot of instances
     * of this class.)
     *
     * <p>This method can be used together with {@link DataFileModel#allTemporaryFiles()}
     * to get a list of all temporary files created by this package
     * and not deleted yet.
     *
     * @return a newly allocated copy of the set of all used {@link DataFileModel data file models}.
     */
    public static Set<DataFileModel<?>> allUsedDataFileModelsWithAutoDeletion() {
        Set<DataFileModel<?>> result = new HashSet<>();
        synchronized (allUsedDataFileModelsWithAutoDeletion) {
            reapDataFileModels();
            for (WeakReference<DataFileModel<?>> ref : allUsedDataFileModelsWithAutoDeletion.keySet()) {
                DataFileModel<?> dfm = ref.get();
                if (dfm != null) {
                    result.add(dfm);
                }
            }
        }
        return result;
    }

    /**
     * Returns a brief string description of this memory model.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * information anout the used bank size and number of banks.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "Large memory model [" + getDataFileModel() + "]";
    }

    private MappedDataStorages.MappingSettings<P> getMappingSettings(Class<?> elementClass, boolean unresizable) {
        return MappedDataStorages.MappingSettings.getInstanceForTemporaryFile(
                this.dataFileModel, elementClass, unresizable, null);
    }

    private MappedDataStorages.MappingSettings<P> getMappingSettings(
            DataFileModel<P> dataFileModel, DataFile dataFile,
            long dataFileStartOffset, long dataFileEndOffset,
            boolean readOnly, Class<?> elementClass) {
        return MappedDataStorages.MappingSettings.getInstanceForExistingFile(dataFileModel,
                dataFile, dataFileStartOffset, dataFileEndOffset, readOnly, elementClass);
    }

    private MappedDataStorages.MappingSettings<P> getMappingSettings(PArray lazyFillingPattern, boolean unresizable) {
        return MappedDataStorages.MappingSettings.getInstanceForTemporaryFile(
                this.dataFileModel, lazyFillingPattern.elementType(), unresizable, lazyFillingPattern);
    }

    private long checkFilePositionAndAreaSize(DataFile file, long filePosition, long fileAreaSize,
                                              boolean truncate, boolean readOnly) {
        assert filePosition >= 0;
        assert fileAreaSize == ALL_FILE || fileAreaSize >= 0;
        try {
            DataFile.OpenResult openResult = file.open(readOnly);
            if (readOnly) {
                assert !truncate;
                assert openResult == DataFile.OpenResult.OPENED;
                long fLen = file.length();
                if (filePosition > (fileAreaSize == ALL_FILE ? fLen : fLen - fileAreaSize)) {
                    throw new IndexOutOfBoundsException("The file is too short: the required area is "
                            + filePosition
                            + (fileAreaSize != ALL_FILE ? ".." + (filePosition + fileAreaSize - 1) : "..EndOfFile")
                            + ", but the file length is " + fLen + " (file \"" + file + "\")");
                }
                if (fileAreaSize == ALL_FILE) {
                    fileAreaSize = fLen - filePosition;
                }
            } else {
                long requiredLen = filePosition + (fileAreaSize == ALL_FILE ? 0 : fileAreaSize);
                if (requiredLen < 0) // overflow
                {
                    throw new IllegalArgumentException("filePosition + fileAreaSize is too large (>"
                            + Long.MAX_VALUE + ")");
                }
                long fLen = openResult == DataFile.OpenResult.CREATED || truncate ? -157 : file.length();
                if (fLen < requiredLen) {
                    fLen = requiredLen;
                    file.length(fLen);
                }
                if (fileAreaSize == ALL_FILE) {
                    fileAreaSize = fLen - filePosition;
                }
            }
        } finally {
            file.close();
        }
        return fileAreaSize;
    }

    private MutableArray newArray(Class<?> elementType, long initialCapacity, long initialLength) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialLength < 0) {
            throw new IllegalArgumentException("Negative initial length");
        }
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Negative initial capacity");
        }
        DataStorage storage;
        if (elementType == boolean.class) {
            storage = new MappedDataStorages.MappedBitStorage(getMappingSettings(elementType, false));
            MutableBufferBitArray result = new MutableBufferBitArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == char.class) {
            storage = new MappedDataStorages.MappedCharStorage(getMappingSettings(elementType, false));
            MutableBufferCharArray result = new MutableBufferCharArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == byte.class) {
            storage = new MappedDataStorages.MappedByteStorage(getMappingSettings(elementType, false));
            MutableBufferByteArray result = new MutableBufferByteArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == short.class) {
            storage = new MappedDataStorages.MappedShortStorage(getMappingSettings(elementType, false));
            MutableBufferShortArray result = new MutableBufferShortArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == int.class) {
            storage = new MappedDataStorages.MappedIntStorage(getMappingSettings(elementType, false));
            MutableBufferIntArray result = new MutableBufferIntArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == long.class) {
            storage = new MappedDataStorages.MappedLongStorage(getMappingSettings(elementType, false));
            MutableBufferLongArray result = new MutableBufferLongArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == float.class) {
            storage = new MappedDataStorages.MappedFloatStorage(getMappingSettings(elementType, false));
            MutableBufferFloatArray result = new MutableBufferFloatArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else if (elementType == double.class) {
            storage = new MappedDataStorages.MappedDoubleStorage(getMappingSettings(elementType, false));
            MutableBufferDoubleArray result = new MutableBufferDoubleArray(storage,
                    initialCapacity, initialLength, 0L, true);
            BufferArraysImpl.forgetOnDeallocation(result);
            return result;
        } else {
            throw new UnsupportedElementTypeException(
                    "Only primitive element types are allowed in LargeMemoryModel (passed type: " + elementType + ")");
        }
    }

    private static String getNCopiesArrayDescription(PArray nCopiesArray) {
        if (nCopiesArray instanceof BitArray) {
            return nCopiesArray.isEmpty() ? "false" : String.valueOf(((BitArray) nCopiesArray).getBit(0));
        } else if (nCopiesArray instanceof CharArray) {
            return nCopiesArray.isEmpty() ? "u0" : "u" + ((CharArray) nCopiesArray).getInt(0);
        } else if (nCopiesArray instanceof ByteArray) {
            return nCopiesArray.isEmpty() ? "0" : String.valueOf(((ByteArray) nCopiesArray).getByte(0));
        } else if (nCopiesArray instanceof ShortArray) {
            return nCopiesArray.isEmpty() ? "0" : String.valueOf(((ShortArray) nCopiesArray).getShort(0));
        } else if (nCopiesArray instanceof IntArray) {
            return nCopiesArray.isEmpty() ? "0" : String.valueOf(((IntArray) nCopiesArray).getInt(0));
        } else if (nCopiesArray instanceof LongArray) {
            return nCopiesArray.isEmpty() ? "0" : String.valueOf(((LongArray) nCopiesArray).getLong(0));
        } else if (nCopiesArray instanceof FloatArray) {
            float v;
            return nCopiesArray.isEmpty() ? "0" : (v = ((FloatArray) nCopiesArray).getFloat(0))
                    + "__" + Float.floatToRawIntBits(v);
        } else if (nCopiesArray instanceof DoubleArray) {
            double v;
            return nCopiesArray.isEmpty() ? "0" : (v = nCopiesArray.getDouble(0))
                    + "__" + Double.doubleToRawLongBits(v);
        } else {
            throw new AssertionError("Non-allowed type of passed array: " + nCopiesArray.getClass());
        }
    }

    private static Matrix<? extends PArray> getNCopiesMatrix(
            MatrixInfo matrixInfo,
            Map<String, String> additionalProperties)
            throws IllegalInfoSyntaxException {
        PArray nCopiesArray = getNCopiesArray(matrixInfo.elementType(), matrixInfo.size(), additionalProperties);
        return nCopiesArray != null ? Matrices.matrix(nCopiesArray, matrixInfo.dimensions()) : null;
    }

    private static PArray getNCopiesArray(
            Class<?> elementType,
            long arrayLength,
            Map<String, String> additionalProperties)
            throws IllegalInfoSyntaxException {
        String s = additionalProperties.get(CONSTANT_PROPERTY_NAME);
        if (s == null) {
            return null;
        }
        try {
            if (elementType == boolean.class) {
                boolean value = Boolean.parseBoolean(s);
                return Arrays.nBitCopies(arrayLength, value);
            } else if (elementType == char.class) {
                if (!s.startsWith("u")) {
                    throw new IllegalInfoSyntaxException("Starting 'u' expected in "
                            + CONSTANT_PROPERTY_NAME + " property: \"" + s + "\"");
                }
                int value = Integer.parseInt(s.substring(1));
                if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
                    throw new IllegalInfoSyntaxException("The constant value " + value + " is out of range "
                            + (int) Character.MIN_VALUE + ".." + (int) Character.MAX_VALUE + " in "
                            + CONSTANT_PROPERTY_NAME + " property: \"" + s + "\"");
                }
                return Arrays.nCharCopies(arrayLength, (char) value);
            } else if (elementType == byte.class) {
                int value = Integer.parseInt(s);
                if (value < 0 || value > 255) {
                    throw new IllegalInfoSyntaxException("The constant value " + value + " is out of range "
                            + "0..255 in " + CONSTANT_PROPERTY_NAME + " property: \"" + s + "\"");
                }
                return Arrays.nByteCopies(arrayLength, (byte) value);
            } else if (elementType == short.class) {
                int value = Integer.parseInt(s);
                if (value < 0 || value > 65535) {
                    throw new IllegalInfoSyntaxException("The constant value " + value + " is out of range "
                            + " 0..65535 in " + CONSTANT_PROPERTY_NAME + " property: \"" + s + "\"");
                }
                return Arrays.nShortCopies(arrayLength, (short) value);
            } else if (elementType == int.class) {
                int value = Integer.parseInt(s);
                return Arrays.nIntCopies(arrayLength, value);
            } else if (elementType == long.class) {
                long value = Long.parseLong(s);
                return Arrays.nLongCopies(arrayLength, value);
            } else if (elementType == float.class) {
                int p = s.indexOf("__");
                if (p == -1) {
                    return Arrays.nFloatCopies(arrayLength, Float.parseFloat(s));
                } else {
                    int bits = Integer.parseInt(s.substring(p + 2));
                    return Arrays.nFloatCopies(arrayLength, Float.intBitsToFloat(bits));
                }
            } else if (elementType == double.class) {
                int p = s.indexOf("__");
                if (p == -1) {
                    return Arrays.nDoubleCopies(arrayLength, Double.parseDouble(s));
                } else {
                    long bits = Long.parseLong(s.substring(p + 2));
                    return Arrays.nDoubleCopies(arrayLength, Double.longBitsToDouble(bits));
                }
            } else {
                throw new AssertionError("Only primitive element types are allowed");
            }
        } catch (NumberFormatException e) {
            IllegalInfoSyntaxException ex = new IllegalInfoSyntaxException(
                    "Illegal numeric format in the " + CONSTANT_PROPERTY_NAME + " property: \"" + s + "\"");
            ex.initCause(e);
            throw ex;
        }
    }

    private static String getTileDimensionsDescription(long[] tileDimensions) {
        return JArrays.toString(tileDimensions, "x", 10000);
    }

    private static long[] getTileDimensions(int dimCount, Map<String, String> additionalProperties)
            throws IllegalInfoSyntaxException {
        String s = additionalProperties.get(TILE_DIMENSIONS_PROPERTY_NAME);
        if (s == null) {
            return null;
        }
        String[] tileDimValues = s.split("x", dimCount + 1);
        if (tileDimValues.length != dimCount) {
            throw new IllegalInfoSyntaxException("The number of tile dimensions (in string \""
                    + s + "\") is not equal to the number of matrix dimensions " + dimCount);
        }
        try {
            long[] tileDimensions = new long[tileDimValues.length];
            for (int k = 0; k < tileDimValues.length; k++) {
                tileDimensions[k] = Long.parseLong(tileDimValues[k]);
            }
            return tileDimensions;
        } catch (NumberFormatException e) {
            IllegalInfoSyntaxException ex = new IllegalInfoSyntaxException(
                    "Illegal numeric format in the " + TILE_DIMENSIONS_PROPERTY_NAME + " property: \"" + s + "\"");
            ex.initCause(e);
            throw ex;
        }
    }

    private static final Object DUMMY = new Object();

    private static void reapDataFileModels() {
        WeakReference<?> ref;
        while ((ref = (WeakReference<?>) reapedDataFileModels.poll()) != null) {
            allUsedDataFileModelsWithAutoDeletion.remove(ref);
        }
    }
}
