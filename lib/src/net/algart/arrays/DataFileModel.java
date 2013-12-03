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

import java.nio.ByteOrder;
import java.util.Set;

/**
 * Data file model: the factory allowing to create and remove some file-like objects
 * ("{@link DataFile data files}").
 * Used by the {@link LargeMemoryModel large memory model}.</p>
 *
 * <p>There are the following standard implementations of this interface:</p>
 *
 * <ul>
 * <li>{@link DefaultDataFileModel}</li>
 * <li>{@link StandardIODataFileModel}</li>
 * </ul>
 *
 * <p>You may create own implementations, or override some methods in standard ones,
 * to get maximal control over the {@link LargeMemoryModel large memory model}.
 * The simplest example is overriding {@link #delete(DataFile)} method
 * to implement custom technique of file deletion, for instance,
 * by moving them to some "Recycle Bin". Another example: you may override
 * the methods {@link #recommendedNumberOfBanks()}, {@link #recommendedBankSize(boolean)}
 * to specify custom values of number of banks and bank size in a concrete data file model.</p>
 *
 * <p>The data file model uses a class, specified as the generic argument <tt>P</tt>
 * and returned by {@link #pathClass()} method, for working with <i>data file paths</i>
 * &mdash; some unique names identifying file position in the file system.
 * Usually, this class is <tt>java.io.File</tt>, and it describes standard path to a disk file.
 * Custom implementations (not inherited from {@link AbstractDataFileModel})
 * may use another classes for specifying file paths.
 *
 * <p>Objects implementing this interface may be not <b>immutable</b>
 * and not <b>thread-safe</b>, but must be <b>thread-compatible</b>
 * (allow manual synchronization for multithread access).</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataFileModel<P> {
    /**
     * Returns the type of the data file paths used by this model.
     * Returned class is equal to the generic type argument of this class.
     *
     * <p>This method never throws exceptions.
     *
     * @return the type of the data file paths used by this model.
     */
    public Class<P> pathClass();

    /**
     * Returns a new instance of {@link DataFile} object corresponding to the given path.
     * This path will be returned by {@link #getPath(DataFile)} method for the returned object.
     *
     * <p>The passed byte order will be used for mapping this file:
     * the {@link DataFile#map(DataFile.Range, boolean)} method of the data file
     * will return <tt>ByteBuffer</tt> with this byte order.
     *
     * <p>The physical object (for example, disk file), described by <tt>path</tt>
     * string, should already exist.
     * This method does not attempt to create physical file;
     * it only creates new Java object associated with an existing file.
     *
     * <p>This method never throws <tt>java.io.IOError</tt>.
     *
     * @param path      the path describing unique position of the existing data file.
     * @param byteOrder the byte order that will be always used for mapping this file.
     * @return          new instance of {@link DataFile} object.
     * @throws NullPointerException if one of passed arguments is <tt>null</tt>.
     */
    public DataFile getDataFile(P path, ByteOrder byteOrder);

    /**
     * Returns the path describing unique position of the data file (usually the absolute path to the disk file).
     *
     * <p>This method never throws <tt>java.io.IOError</tt>.
     *
     * @param dataFile the data file.
     * @return         the path describing unique position of the data file.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     * @throws ClassCastException   if the data file was created by incompatible data file model.
     */
    public P getPath(DataFile dataFile);

    /**
     * Creates new temporary data file and returns a new instance of {@link DataFile}
     * object corresponding to it.
     *
     * <p>The {@link DataFile#map(DataFile.Range, boolean)} method of the created data file
     * will return <tt>ByteBuffer</tt> with some byte order: it depends on implementation.
     *
     * <p>The returned instance is added to some internal collection,
     * returned by {@link #allTemporaryFiles()} method.
     * This action is optional, but performed by all implementations from this package.
     * In your implementation, you are able not to support this collection,
     * if you are absolutely sure that automatic file deletion, performed by this package,
     * as well as any possible custom cleanup procedures for temporary files,
     * are not useful for your data files.
     * (In this case, please keep in mind the Sun's bug #4171239 in Java 1.5 and 1.6:
     * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239"
     * >java.io.File.deleteOnExit does not work on open files (win32)</a>."
     * Automatic deletion performed by this package includes closing the file, that allow
     * to avoid this bug.)
     *
     * @param unresizable <tt>true</tt> if this file will be used for unresizable arrays only.
     *                    It is information flag: for example, it may be used for choosing file name
     *                    or directory. If this flag is set, it does not mean that {@link DataFile#length(long)}
     *                    method will not be called to change the file length; it will be called at least once.
     * @return            new instance of {@link DataFile} object corresponding newly created temporary data file.
     * @throws java.io.IOError in a case of any disk errors.
     */
    public DataFile createTemporary(boolean unresizable);

    /**
     * Deletes the data file.
     * Returns <tt>true</tt> if the file was successfully deleted
     * or <tt>false</tt> this file does not exists (nothing to do).
     * Usually means deletion of the disk file, but some file models
     * may override this behavior (for example, may move the file in some special directory).
     *
     * <p>This method is called automatically for temporary files by garbage collector
     * and by standard cleanup procedure, performed by this package.
     *
     * <p>Warning: unlike <tt>java.io.File.delete()</tt>, this method
     * must throw an exception (<tt>java.io.IOError</tt>) in a case of some problems while file deletion.
     * (<tt>java.io.File.delete()</tt> returns <tt>false</tt> in this situation.)
     *
     * <p>In a case of successful deletion, this method excludes the path of this file from the internal set
     * returned by {@link #allTemporaryFiles()} method.
     *
     * <p>The passed data file must be created by the same data file model.
     *
     * <p>This method should be synchronized, usually in relation to the internal set,
     * the copy or view of which is returned by {@link #allTemporaryFiles()} method.
     * The reason is that it is usually called from several threads:
     * at least, from the main calculation thread, from the garbage collector (finalization thread)
     * and from the built-in shutdown hook. If this method will not be internally synchronized,
     * it may try to remove the same file several times, that will lead to logging a warning
     * that the file "cannot be deleted".
     *
     * @param dataFile the data file that should be deleted.
     * @return         <tt>true</tt> if and only if the data file existed and was successfully deleted,
     *                 <tt>false</tt> if the data file does not exist (maybe was deleted already).
     * @throws NullPointerException if the passed data file is <tt>null</tt>.
     * @throws ClassCastException   if the data file was created by incompatible data file model.
     * @throws java.io.IOError in a case of any problems while file deletion.
     */
    public boolean delete(DataFile dataFile);

    /**
     * This method is automatically called when the data file becomes unreachable,
     * either due to garbage collection (when all AlgART arrays, using this data file,
     * became unreachable), or due to finishing the application
     * (in the standard cleanup procedure, performed by this package).
     *
     * <p>Please compare: unlike {@link #delete(DataFile)}, this method
     * is called not only for temporary files, but also for data files, opened
     * via {@link LargeMemoryModel#asArray LargeMemoryModel.asArray} /
     * {@link LargeMemoryModel#asUpdatableArray LargeMemoryModel.asUpdatableArray} methods,
     * and for underlying data file of arrays, that were declared non-temporary
     * via {@link LargeMemoryModel#setTemporary(Array, boolean)} method.
     *
     * <p>This method is called <i>after</i> all other operations with this data file,
     * in particular, after automatic deleting it by {@link #delete(DataFile)} method.
     *
     * <p>The implementations of this method, provided by this package, do nothing.
     * But you may override it in a custom data file model to inform application
     * that the data file becomes unuseful and, for example,
     * may be deleted by your non-standard file deletion mechanism.
     *
     * <p>Please note: if {@link LargeMemoryModel#asArray LargeMemoryModel.asArray} /
     * {@link LargeMemoryModel#asUpdatableArray LargeMemoryModel.asUpdatableArray} methods
     * are called several times for the same external file, then each call produces separate
     * {@link DataFile} instance. So, this method will be called several times for this file
     * (with the same <tt>dataFilePath</tt> argument).
     *
     * @param dataFilePath          the path describing unique position of the data file.
     * @param isApplicationShutdown <tt>true</tt> if this method is called by the cleanup procedure,
     *                              performed by this package, while finishing the application;
     *                              <tt>false</tt> if it is called from the garbage collector.
     */
    public void finalizationNotify(P dataFilePath, boolean isApplicationShutdown);

    /**
     * Returns the set of all data files, that are
     * temporary and should be automatically deleted while system shutdown.
     * The returned set is an immutable view or a newly allocated copy of an internal set stored in this instance.
     * The returned instance must not be <tt>null</tt> (but may be the empty set).
     *
     * <p>Usually this method returns the set of temporary files
     * that were created by {@link #createTemporary(boolean)} method by this instance of this factory,
     * but not were successfully deleted by {@link #delete(DataFile)} method yet.
     *
     * <p>This package includes automatic cleanup procedure, that is performed in
     * the internal shutdown hook and calls {@link #delete(DataFile)} for all data files,
     * returned by this method for all instances of this class, which
     * were used since the application start and returned <tt>true</tt> as a result
     * of {@link #isAutoDeletionRequested()} method.
     * You may install additional cleanup procedures, that will be called before or after this,
     * via {@link Arrays#addShutdownTask(Runnable, Arrays.TaskExecutionOrder)} method.
     *
     * @return the set of the paths of all created temporary files.
     */
    public Set<DataFile> allTemporaryFiles();

    /**
     * If <tt>value</tt> is <tt>true</tt>, adds the passed data file instance into
     * the internal collection returned by {@link #allTemporaryFiles()} method;
     * if <tt>value</tt> is <tt>false</tt>, removes it from that collection.
     *
     * <p>This method does nothing if {@link #isAutoDeletionRequested()} returns <tt>false</tt>.
     *
     * <p>This method is called in {@link LargeMemoryModel#setTemporary} method only.
     *
     * <p>In some data file models (implemented in another packages) this method may do nothing
     * or throw an exception.
     *
     * @param dataFile the data file.
     * @param value    specifies whether the data file should be included into or excluded from
     *                 the internal collection of temporary files.
     * @throws NullPointerException if the passed data file is <tt>null</tt>.
     */
    public void setTemporary(DataFile dataFile, boolean value);

    /**
     * Returns <tt>true</tt> if the standard cleanup procedure, that deletes all temporary files
     * (as described in comments to {@link #allTemporaryFiles()} method), is necessary for this file model.
     * In this case, the instance of this model is automatically registered,
     * while creating any {@link LargeMemoryModel} instance with this model,
     * in the internal static collection
     * that can be retrieved by {@link LargeMemoryModel#allUsedDataFileModelsWithAutoDeletion()} method.
     *
     * <p>This method returns <tt>true</tt> for all implementations from this package.
     * If you implemented own cleanup procedure in your implementation of this class,
     * you may return <tt>false</tt> in this method. If this method returns <tt>false</tt>,
     * the implementations of {@link #createTemporary(boolean)} method in this package
     * do not add the file name into the internal collection,
     * returned by {@link #allTemporaryFiles()} method.
     *
     * <p>If this method returns <tt>false</tt>, <i>it does not mean</i> that temporary files will
     * not be deleted automatically. It only means that this data file model instance
     * will not be registered in the internal static collection
     * (available via {@link LargeMemoryModel#allUsedDataFileModelsWithAutoDeletion()} method)
     * and that the {@link #createTemporary(boolean)} method will not register the file name in
     * {@link #allTemporaryFiles()} collection. To avoid automatic file deletion,
     * you must call {@link LargeMemoryModel#setTemporary(Array, boolean)} method
     * with <tt>false</tt> second argument.
     *
     * @return <tt>true</tt> if the temporary data files, created by this model, should be automatically
     *         deleted by the standard cleanup procedure.
     */
    public boolean isAutoDeletionRequested();

    /**
     * The number of memory banks, recommended for data files created by this factory.
     * AlgART arrays, based on data file mapping, allocate this number of memory banks
     * and load there portions of large data file.
     *
     * <p>The returned number of banks must not be less than 2.
     * In other case, an attempt to create {@link Array} instance will throw an exception.
     * Usual values are 8-16.
     *
     <!--Repeat.SectionStart recommendedNumberOfBanks_multiprocessor-->
     * <p>Please note that many algorithms, on multiprocessor or multi-core systems,
     * use several parallel threads for processing arrays: see {@link Arrays.ParallelExecutor}.
     * So, the number of banks should be enough for parallel using by all CPU units,
     * to avoid frequently bank swapping.
     * There should be at least 2 banks per each CPU unit,
     * better 3-4 banks (for complex random-access algorithms).
     <!--Repeat.SectionEnd recommendedNumberOfBanks_multiprocessor-->
     *
     * @return the recommended number of memory banks.
     */
    public int recommendedNumberOfBanks();

    /**
     * The size of every memory bank in bytes, recommended for data files created by this factory.
     * AlgART arrays, based on data file mapping, allocate memory banks with this size
     * and load there portions of large data file.
     *
     * <p>The <tt>unresizable</tt> flag specifies whether this bank size will be used for
     * data file, which stores unresizable arrays only. In this case, this method may return greater value
     * than if <tt>unresizable</tt> is <tt>false</tt>. The reason is that the data files,
     * containing resizable arrays, may grow per blocks, which size is equal to the bank size.
     * If bank size is 8 MB, then any resizable array, created by {@link MemoryModel#newIntArray(long)}
     * or similar method, will occupy at least 8 MB of disk space, even its length is only several <tt>int</tt>
     * values. For unresizable arrays, created by {@link MemoryModel#newUnresizableIntArray(long)} and similar
     * methods, the file size is usually fixed while its creation and bank size information is not used.
     *
     * <p>This returned size must be the power of two (2<sup>k</sup>) and must not be less than 256.
     * In other case, an attempt to create {@link Array} instance will throw an exception.
     *
     * <p>We recommend use large banks to reduce bank swapping.
     * But do not specify too large values here: every opened data file
     * use <tt>{@link #recommendedNumberOfBanks()}*{@link #recommendedBankSize(boolean)}</tt>
     * bytes of the address space, which is limited by ~1.0-1.5 GB under 32-bit OS.
     * Typical value is 2-8 MB for unresizable arrays
     * (when the argument is <tt>true</tt>) and 64-256 KB for resizable
     * ones (when the argument is <tt>false</tt>).
     *
     * @param unresizable <tt>true</tt> if this bank size will be used for unresizable arrays only.
     * @return            the recommended size of every memory bank in bytes.
     * @see #recommendedSingleMappingLimit()
     */
    public int recommendedBankSize(boolean unresizable);

    /**
     * If a mapped AlgART array is {@link Array#isUnresizable() unresizable} and it's size, in bytes,
     * is less than or equal to the result of this method, then all data file is mapped by a single large bank.
     * Usual {@link #recommendedBankSize(boolean) bank size} is ignored in this case, and only one bank is used.
     *
     * <p>If this data file model is based on true low-level mapping, as {@link DefaultDataFileModel},
     * that the large value returned by this method allows to improve performance and also helps to avoid
     * the Sun's bug in Java 1.5 and 1.6:
     * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521677"
     * >(fc)&nbsp;"Cleaner terminated abnormally" error in simple mapping test</a>".
     * In modern 32-bit JRE, the value about 16-32 MB looks suitable.
     *
     * <p>We don't recommend to set this limit too large in 32-bit JRE:
     * every mapping reduces available address space, that is limited by 1.0-1.5 GB only.
     *
     * <p>If the result of this method is zero or negative, this behavior is not used.
     *
     * @return the recommended limit for file size, in bytes, so that less files, if they are unresizable,
     *         should be mapped only once by single call of {@link DataFile#map} method.
     */
    public int recommendedSingleMappingLimit();

    /**
     * The size (in bytes) of the starting gap in all temporary files, created by
     * {@link #createTemporary(boolean)} method.
     * The first element of the newly created arrays is placed at this offset in the data file.
     *
     * <p>This gap will not be used for storing array elements, but may be used for saving
     * some additional information (prefix) if you will decide not to remove this file while
     * garbage collection, for example, with help of {@link LargeMemoryModel#setTemporary(Array, boolean)} method.
     *
     * <p>If the result of this method is zero or negative, there will be no starting gap.
     * (Negative values are interpreted as zero.)
     *
     * @return the size of the starting gap in the temporary files, in bytes.
     * @see MatrixInfo
     */
    public long recommendedPrefixSize();

    /**
     * If this method returns <tt>true</tt>, then mapping the data file by
     * {@link DataFile#map(DataFile.Range, boolean) map(position, size)} call
     * automatically increases the file length to <tt>position+size</tt>
     * if the current file length is less than this value.
     * In this case, this package will not call {@link DataFile#length(long)}
     * method for increasing the temporary file length.
     *
     * <p>The described behavior of mapping usually depends on the platform.
     * So, this method should return <tt>false</tt> in most cases.
     *
     * <p>For unresizable files (i.e. for arrays that are created unresizable),
     * the result of this method is not used: these files are never mapped outside their
     * original lengths.
     *
     * @return <tt>true</tt> if mapping outside the file length automatically increase the length.
     */
    public boolean autoResizingOnMapping();
}
