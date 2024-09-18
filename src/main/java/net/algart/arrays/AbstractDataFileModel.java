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

import java.io.IOError;
import java.io.IOException;
import java.io.File;
import java.nio.ByteOrder;
import java.util.*;

/**
 * <p>A skeletal implementation of the {@link DataFileModel} interface to minimize
 * the effort required to implement this interface for processing usual disk files.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractDataFileModel implements DataFileModel<File> {

    /**
     * The internal synchronized set of all non-deleted temporary files:
     * {@link #allTemporaryFiles()} method returns its clone.
     * {@link #createTemporary(boolean)} method adds a path to this set,
     * {@link #delete(DataFile)} method removes the path from it.
     */
    protected final Set<DataFile> allTemporaryFiles = Collections.synchronizedSet(new HashSet<>());

    /**
     * The path where new temporary files will be created by {@link #createTemporaryFile(boolean)} method
     * or {@code null} if the default temporary-file directory is to be used.
     * If it is not {@code null}, it can refer to: 1) an existing directory, 2) existing file or
     * 3) non-existing file/directory. In 1st case, it is considered as the temporary-file directory,
     * in which new temporary files will be created. In 2nd and 3rd case, it is considered
     * as a <i>file</i> name: the name of new temporary file will always equal to this
     * (so, it will be possible to create only one temporary file).
     * It is equal to the first argument of {@link #AbstractDataFileModel(File, long)} constructor.
     *
     * @see #tempPath()
     * @see #isConcreteFile()
     */
    protected final File tempPath;

    /**
     * The value returned by {@link #recommendedPrefixSize()} method.
     * It is equal to the second argument of {@link #AbstractDataFileModel(File, long)} constructor.
     */
    protected final long prefixSize;

    /**
     * Equivalent to {@link #AbstractDataFileModel(File, long) AbstractDataFileModel(null, 0)}.
     */
    protected AbstractDataFileModel() {
        this.tempPath = null;
        this.prefixSize = 0;
    }

    /**
     * Creates a new instance with the specified temporary-file path and the starting gap size
     * in all temporary files.
     *
     * <p>The <code>tempPath</code> argument may refer both to a directory and to a file.
     * The following 4 cases are possible.
     *
     * <ol>
     * <li><code>tempPath</code> is an existing directory or another non-file disk resource;
     * more precisely, <code>tempPath.exists() &amp;&amp; !tempPath.isFile()</code>.
     * In this case, new temporary files, created by {@link #createTemporaryFile(boolean)} method,
     * will be placed in this directory with unique names.</li>
     *
     * <li><code>tempPath</code> is {@code null}. In this case, new temporary files, created by
     * {@link #createTemporaryFile(boolean)} method, will be placed in the default system-dependent
     * temporary-file directory.</li>
     *
     * <li><code>tempPath</code> is an existing file (<code>tempPath.isFile()</code>) or
     *
     * <li><code>tempPath</code> does not exists (<code>!tempPath.exists()</code>).
     * These are <b>special cases</b>! The {@link #createTemporaryFile(boolean)} will create the file
     * at this position (<code>tempPath.createNewFile()</code>) and return <code>tempPath</code>.
     * In other words, this data file model will not be able to create more than one temporary file,
     * and all further temporary files, if will be requested, will overwrite the same file.
     * </li>
     * </ol>
     *
     * <p>The last variants 3 and 4 can be useful for creating a single array mapped to some required file.
     * For example, the following code will create new array, containing 1024 <code>int</code> values,
     * in the file "myfile.dat":
     *
     * <pre>
     * {@link DefaultDataFileModel} dfm = new {@link DefaultDataFileModel#DefaultDataFileModel(File)
     * DefaultDataFileModel}("myfile.dat");
     * {@link LargeMemoryModel LargeMemoryModel&lt;File&gt;} mm = {@link LargeMemoryModel#getInstance(DataFileModel)
     * LargeMemoryModel.getInstance}(dfm);
     * {@link UpdatableIntArray} dest = mm.{@link MemoryModel#newUnresizableIntArray newUnresizableIntArray}(1024);
     * {@link LargeMemoryModel#setTemporary LargeMemoryModel.setTemporary}(dest, false);
     * </pre>
     *
     * <p>Please be careful: if there is an existing subdirectory with the same "myfile.dat" name,
     * the behavior of the data file model will be another (the variant #1).
     *
     * <p>The <code>prefixSize</code> defines the starting gap in all temporary files,
     * returned by {@link DataFileModel#recommendedPrefixSize() recommendedPrefixSize()} implementation
     * in this class. Usually you may pass <code>0</code> here. The only case when it makes sense to
     * pass positive value is if you are planning to clear temporary status of the created array
     * via {@link LargeMemoryModel#setTemporary(Array, boolean)} method and save some additional
     * meta-information in the file prefix &mdash; element type, array length, etc.
     * A good example of using prefix is the tool {@link MatrixInfo}.
     *
     * @param tempPath   the path where new temporary files will be created
     *                   by {@link #createTemporaryFile(boolean)} method
     *                   or {@code null} if the default temporary-file directory is to be used.
     * @param prefixSize the value returned by {@link #recommendedPrefixSize()} implementation in this class.
     */
    protected AbstractDataFileModel(File tempPath, long prefixSize) {
        this.tempPath = tempPath;
        this.prefixSize = prefixSize;
    }

    /**
     * This implementation returns <code>File.class</code>.
     *
     * @return <code>File.class</code>
     */
    public Class<File> pathClass() {
        return File.class;
    }

    public abstract DataFile getDataFile(File path, ByteOrder byteOrder);

    public abstract File getPath(DataFile dataFile);

    /**
     * This implementation creates a temporary file by calling {@link #createTemporaryFile(boolean)}
     * protected method and returns a result of {@link #getDataFile(File, ByteOrder)
     * getDataFile(pathToTemporaryFile, ByteOrder.nativeOrder())}.
     * You may override {@link #createTemporaryFile(boolean)} method
     * to change a location or names of temporary files.
     *
     * <p>The byte order in the created file will be equal to the result of {@link #byteOrderInTemporaryFiles()}
     * protected method. You may override it to specify custom byte order in temporary files.
     *
     * <p>This method adds the created instance into {@link #allTemporaryFiles},
     * if {@link #isAutoDeletionRequested()} method returns <code>true</code>.
     *
     * @param unresizable the argument passed to {@link #createTemporaryFile(boolean)}.
     * @return            new instance of {@link DataFile} object corresponding newly created temporary data file.
     * @throws java.io.IOError if {@link #createTemporaryFile(boolean)} throws <code>IOException</code>.
     */
    public DataFile createTemporary(boolean unresizable) {
        File fileName;
        try {
            fileName = createTemporaryFile(unresizable);
        } catch (IOException ex) {
            throw new IOError(ex);
        }
        DataFile result = getDataFile(fileName, byteOrderInTemporaryFiles());
        if (isAutoDeletionRequested()) {
            allTemporaryFiles.add(result);
        }
        return result;
    }

    /**
     * This implementation removes the file by the call of standard <code>java.io.File.delete()</code> method:
     * <code>{@link #getPath getPath}(dataFile).delete()</code>
     * (if <code>java.io.File.exists()</code> method returns <code>true</code>).
     * After deletion, this implementation removes the passed instance from
     * {@link #allTemporaryFiles}.
     *
     * <p>This implementation is fully synchronized on the internal set
     * returned by {@link #allTemporaryFiles()} method.
     *
     * <p>You should override this method if the data files in this model do not correspond to usual disk files.
     *
     * @param dataFile the data file that should be deleted.
     * @return         <code>true</code> if and only if the data file existed and was successfully deleted,
     *                 <code>false</code> if the data file does not exist (maybe was deleted already).
     * @throws java.io.IOError      in the case of any problems while file deletion.
     * @throws NullPointerException if the passed data file is {@code null}.
     */
    public boolean delete(DataFile dataFile) {
        Objects.requireNonNull(dataFile, "Null dataFile argument");
        File f = getPath(dataFile);
        synchronized(allTemporaryFiles) {
            if (!f.exists()) {
                return false;
            }
//          The following code allows to get more informative IOError in a case when the file cannot be deleted;
//          but it may sometimes lead to a problem in Java 1.7.0-ea-b10.
//          dataFile.open(false);
//          try {
//              dataFile.length(0); // allows to get a more detailed error message
//          } finally {
//              dataFile.close();
//          }
            if (!f.delete()) {
                throw new IOError(new IOException("Cannot delete file " + f));
            }
            allTemporaryFiles.remove(dataFile);
            return true;
        }
    }

    /**
     * This implementation does nothing.
     *
     * @param dataFilePath the path describing unique position of the data file.
     */
    public void finalizationNotify(File dataFilePath, boolean isApplicationShutdown) {
    }

    public Set<DataFile> allTemporaryFiles() {
        TreeSet<DataFile> result = new TreeSet<>((o1, o2) -> getPath(o1).compareTo(getPath(o2)));
        // TreeSet provides sorted list of files that looks better for users.
        synchronized(allTemporaryFiles) {
            result.addAll(allTemporaryFiles);
        }
        return result;
    }

    public void setTemporary(DataFile dataFile, boolean value) {
        Objects.requireNonNull(dataFile, "Null dataFile argument");
        if (isAutoDeletionRequested()) {
            if (value) {
                allTemporaryFiles.add(dataFile);
            } else {
                allTemporaryFiles.remove(dataFile);
            }
        }
    }

    public abstract boolean isAutoDeletionRequested();

    public abstract int recommendedNumberOfBanks();

    public abstract int recommendedBankSize(boolean unresizable);

    /**
     * <p>This implementation returns 0.
     *
     * @return 0.
     */
    public int recommendedSingleMappingLimit() {
        return 0;
    }

    /**
     * <p>This implementation returns {@link #prefixSize} &mdash; the <code>long</code> value passed
     * to the constructor, or <code>0</code> if the constructor without <code>long</code> argument was used.
     *
     * @return {@link #prefixSize}.
     */
    public long recommendedPrefixSize() {
        return prefixSize;
    }

    /**
     * <p>This implementation returns <code>false</code>.
     *
     * @return <code>false</code>.
     */
    public boolean autoResizingOnMapping() {
        return false;
    }

    /**
     * Returns the value of {@link #tempPath} protected field,
     * in other words, the first argument of {@link #AbstractDataFileModel(File, long)} constructor.
     *
     * @return the value of {@link #tempPath} protected field.
     */
    public final File tempPath() {
        return this.tempPath;
    }

    /**
     * Returns <code>true</code> if the {@link #tempPath} field is not {@code null} and
     * corresponds to existing file or non-existing file/directory:
     * <code>tempPath != null &amp;&amp; (!tempPath.exists() || tempPath.isFile())</code>
     * In such situations, this data file model always works with the only one constant file,
     * specified in {@link #tempPath} field.
     *
     * <p>This method returns <code>true</code> is the cases #3 and #4, described in
     * the {@link #AbstractDataFileModel(File, long) comments to the constructor}.
     *
     * @return whether this data file model always works with the fixed file {@link #tempPath}.
     */
    public final boolean isConcreteFile() {
        return tempPath != null && (!tempPath.exists() || tempPath.isFile());
    }

    /**
     * Returns the prefix, used by the current implementation of
     * {@link #createTemporary(boolean)} method while creating temporary files &mdash;
     * see comments to that method. This prefix helps to indicate a concrete
     * data file model class, which was used while creating temporary files.
     *
     * <p>The result of this method must be at least three characters long.
     * In another case, its result will be automatically appended with "_" to fulfil
     * the requirements of the standard <code>File.createTempFile</code> method.
     *
     * <p>This method must not return {@code null}.
     *
     * <p>This implementation returns <code>"lmm"</code>.
     * The implementation from {@link DefaultDataFileModel} class returns <code>"mapmm"</code>.
     * The implementation from {@link StandardIODataFileModel} class returns <code>"stdmm"</code>.
     *
     * @return the prefix (3 characters minimum), which will be added to the name of the temporary files,
     *         created by {@link #createTemporary(boolean)} method.
     */
    public String temporaryFilePrefix() {
        return "lmm";
    }

    /**
     * Creates a new temporary empty disk file and returns the absolute (unique for the file system) path to it.
     * This method is used by {@link #createTemporary(boolean)} method, if it was not overridden,
     * in particular, in both {@link DefaultDataFileModel} and {@link StandardIODataFileModel} classes.
     *
     * <p>If {@link #isConcreteFile()} method returns <code>false</code>
     * (i.e. if {@link #tempPath} is {@code null} or an existing directory:
     * <code>tempPath.exists() &amp;&amp; !tempPath.isFile()</code>),
     * this implementation returns the following result:
     * <code>File.createTempFile(prefix,suffix,{@link #tempPath}).getAbsolutePath()</code>, where
     * <code>tempPath</code> is the constructor argument ({@code null} for constructor without
     * <code>File</code> argument), <code>prefix</code> is the result of {@link #temporaryFilePrefix()}
     * method, padded to length 3 with "_" character if its length &lt;3,
     * <code>suffix</code> is calculated as<pre>
     * String suffix = (unresizable ? ".uarray." : ".marray.")
     * + ({@link #byteOrderInTemporaryFiles()} == ByteOrder.BIG_ENDIAN ? "be.tmp" : "le.tmp");
     * </pre>
     *
     * <p>If {@link #isConcreteFile()} method returns <code>true</code>
     * (i.e. if {@link #tempPath} is not {@code null}, and it is a file or does not exist:
     * <code>!tempPath.exists() || tempPath.isFile()</code>),
     * this implementation calls <code>tempPath.createNewFile()</code> and returns <code>tempPath</code>.
     *
     * <p>You may override this method to change this behavior.
     *
     * <p>This method does not try to use <code>File.deleteOnExit()</code> method
     * for created files.
     * Instead, the temporary files will be deleted via built-in
     * cleanup procedure (described in comments to {@link #allTemporaryFiles()} method),
     * if {@link #isAutoDeletionRequested()} method returns <code>true</code>.
     * The reasons are the Sun's bugs #6359560:
     * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6359560"
     * >(fs)&nbsp;File.deleteOnExit() doesn't work when MappedByteBuffer exists (win)</a>"
     * and #4171239 (Java 1.5 and 1.6):
     * "<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239"
     * >java.io.File.deleteOnExit does not work on open files (win32)</a>".
     *
     * <p>You need to keep in mind that some of these files will not be deleted,
     * if all finalization code will not be performed until system exit.
     * You may call {@link Arrays#gcAndAwaitFinalization(int)} method before system exit
     * to reduce the probability of appearing non-deleted files.
     *
     * @param unresizable <code>true</code> if this file will be used for unresizable arrays only.
     *                    It is an information flag: for example, it may be used for choosing file name
     *                    or directory. If this flag is set, it does not mean that {@link DataFile#length(long)}
     *                    method will not be called to change the file length; it will be called at least once.
     * @return            the absolute path to new created temporary file.
     * @throws IOException in the case of any disk errors.
     */
    protected File createTemporaryFile(boolean unresizable) throws IOException {
        if (isConcreteFile()) {
            tempPath.createNewFile();
            return tempPath.getAbsoluteFile();
        } else {
            String prefix = temporaryFilePrefix();
            while (prefix.length() < 3) {
                prefix += "_";
            }
            String suffix = (unresizable ? ".uarray." : ".marray.")
                + (byteOrderInTemporaryFiles() == ByteOrder.BIG_ENDIAN ? "be.tmp" : "le.tmp");
            return File.createTempFile(prefix, suffix, tempPath).getAbsoluteFile();
        }
    }

    /**
     * Returns byte order that is used for new temporary files by {@link #createTemporary(boolean)} method
     * in this class. Never returns {@code null}.
     *
     * <p>This implementation returns <code>ByteOrder.nativeOrder()</code>: in most situations,
     * the best choice for temporary data, that will be automatically deleted
     * before application exit and, probably, will never be transferred to another computers.
     * You may override this method to specify another byte order;
     * this is useful if you are planning to clear temporary status
     * for some arrays via {@link LargeMemoryModel#setTemporary(Array, boolean)} method.
     *
     * @return byte order that is used for new temporary files.
     */
    protected ByteOrder byteOrderInTemporaryFiles() {
        return ByteOrder.nativeOrder();
    }
}
