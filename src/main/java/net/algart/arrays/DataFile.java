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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>Some "data file" (usually disk file) that supports file-mapping operation.
 * Used by the {@link LargeMemoryModel large memory model}.</p>
 *
 * <p>The instances of this class are returned by methods of {@link DataFileModel} interface.</p>
 *
 * <p>Note: all implementations of this interface from this package do not override standard
 * <code>equals</code> and <code>hashCode</code> methods of <code>Object</code> class.
 * Illegal overriding these methods may lead to losing some file instances by
 * {@link DataFileModel#allTemporaryFiles()} method.</p>
 *
 * <p>Objects implementing this interface may be not <b>immutable</b>
 * and not <b>thread-safe</b>, but must be <b>thread-compatible</b>
 * (allow manual synchronization for multithreading access).</p>
 *
 * @author Daniel Alievsky
 */
public interface DataFile {
    /**
     * Possible results of {@link DataFile#open(boolean)} method.
     */
    enum OpenResult {
        /**
         * Returned by link {@link DataFile#open(boolean)} method if the data file existed before the method call.
         */
        OPENED,
        /**
         * Returned by link {@link DataFile#open(boolean)} method if its argument is <code>false</code>
         * and the data file was created by the method call.
         */
        CREATED
    }

    /**
     * Returns the byte order in all byte buffers returned by {@link #map(Range, boolean)}
     * method.
     *
     * <p>This method never throws exceptions.
     *
     * @return the byte order in all returned byte buffers.
     */
    ByteOrder byteOrder();

    /**
     * Opens the data file. Does nothing if it is already opened.
     * Must be called before any calls of {@link #map(Range, boolean)},
     * {@link #length()}, {@link #length(long)},
     * {@link BufferHolder#unmap(boolean)}, {@link BufferHolder#flush(boolean)} methods.
     * It is not necessary to call this method before deletion the file
     * by {@link DataFileModel#delete(DataFile)} method.
     *
     * <p>If <code>readOnly</code> argument is <code>true</code>, this file
     * will be used only for reading data.
     * In particular, the {@link #length(long)} method will not be called.
     * The read-only mode will be actual until {@link #close() closing} file;
     * the next <code>open</code> method may change this mode.
     *
     * <p>If the file does not exist yet, then behavior of this method depends on
     * <code>readOnly</code> argument. If it is <code>false</code> (read/write mode),
     * this method tries to create it at the position <code>theModelWhichCreatedThisFile.{@link
     * DataFileModel#getPath(DataFile) getPath}(thisFile)</code>.
     * If it is <code>true</code> (read-only mode), this method just throws
     * <code>IOError</code> with the corresponding cause (usually <code>FileNotFoundException</code>).
     * In the first case, the length of the newly created file is always 0.
     *
     * <p>This method returns {@link OpenResult#CREATED} if and only if
     * <code>readOnly</code> argument is <code>false</code> and the data file was successfully created.
     * In all other cases this method returns {@link OpenResult#OPENED}.
     *
     * @param readOnly if <code>true</code>, the file data will be read but will not be changed.
     * @return         {@link OpenResult#CREATED} if this method has successfully created the new file,
     *                 {@link OpenResult#OPENED} in all other cases.
     * @see #close()
     * @throws java.io.IOError in a case of some disk errors or if the argument is <code>true</code>
     *                         and there is no file with the given position.
     */
    OpenResult open(boolean readOnly);

    /**
     * Closes data file. Does nothing if it is already closed.
     * It disables all other further operations, besides {@link #open(boolean)}
     * and {@link DataFileModel#delete(DataFile)} methods.
     *
     * <p>Please note that there are no guarantees that this method
     * completely releases all system resources, associated with this data file.
     * Please see comments to classes {@link DefaultDataFileModel} and {@link StandardIODataFileModel}
     * about behavior of this method in that data file models.
     *
     * @see #open(boolean)
     * @throws java.io.IOError in a case of some disk errors.
     */
    void close();

    /**
     * Tries to write any updates of this data file to the storage device that contains it.
     */
    void force();

    /**
     * Returns the argument passed to last {@link #open(boolean)} method.
     *
     * <p>This method never throws exceptions.
     *
     * @return <code>true</code> if the file is opened in read-only mode.
     */
    boolean isReadOnly();

    /**
     * Maps a region of this data file directly into memory.
     * It is an analog (usually a wrapper) of standard
     * {@link java.nio.channels.FileChannel#map} method.
     * The mapping mode (read-write or read-only) may depend on
     * the argument of the previous call of the {@link #open(boolean)} method.
     *
     * <p>This method is used by this package in very restricted manner.
     * If you implement this method in your own {@link DataFileModel}, you can be sure that
     * the arguments the following conditions are true:
     * <ol>
     * <li><code>range.position()=bankSize*k</code>, where <code>bankSize</code> is the result of
     * {@link DataFileModel#recommendedBankSize(boolean)} method of your data file model
     * and <code>k</code> is some non-negative integer;
     * <li><code>range.length()&lt;=bankSize</code>;
     * <li>the same positions are never mapped 2 or more times simultaneously for one file:
     * {@link BufferHolder#unmap(boolean)} method is always called before the same
     * position will be mapped again.
     * </ol>
     * <p>In particular, it means that the regions mapped by this package
     * never overlap.
     *
     * <p>If <code>notLoadDataFromFile</code> argument is <code>true</code>, this method
     * may ignore the current data in this data file. This method is called by this package with
     * <code>notLoadDataFromFile==true</code> if the returned buffer will be immediately
     * filled by some values, not depending on the previous file content.
     *
     * @param range               the position within the file at which the mapped region
     *                            starts and the size of the region to be mapped.
     *                            In current version, the region size ({@link Range#length() range.length()}
     *                            must not be greater than <code>Integer.MAX_VALUE</code>.
     * @param notLoadDataFromFile if <code>true</code>, this method may discard the previous content
     *                            of the specified region of the data file.
     * @return                    an object allowing to access mapped data.
     * @throws java.io.IOError in the case of some disk errors.
     */
    BufferHolder map(Range range, boolean notLoadDataFromFile);

    /**
     * Returns the current length of the data file.
     *
     * @return the current length of the data file.
     * @throws java.io.IOError in a case of some disk errors.
     */
    long length();

    /**
     * Resizes the data file.
     *
     * @param newLength new length of data file.
     * @throws java.io.IOError in a case of some disk errors.
     */
    void length(long newLength);

    /**
     * <p>Pair of 2 <code>long</code> values <code>position</code>
     * and <code>length</code>, describing the range <code>position..position+length-1</code>
     * of linear addresses in some {@link DataFile data file}.</p>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>.</p>
     *
     * @see DataFile#map(Range, boolean)
     */
    final class Range implements Comparable<Range> {
        private final long position;
        private final long length;

        private Range(long position, long length) {
            if (position < 0)
                throw new IllegalArgumentException("Negative position argument: " + position);
            if (length < 0)
                throw new IllegalArgumentException("Negative length argument: " + position);
            this.position = position;
            this.length = length;
        }

        /**
         * Creates new range <code>position..position+length-1</code>.

         * @param position the starting range position.
         * @param length   the length of range.
         * @return         new range instance.
         * @throws IllegalArgumentException of <code>position</code> or <code>length</code> is negative.
         */
        public static Range valueOf(long position, long length) {
            return new Range(position, length);
        }

        /**
         * Returns the starting range position.
         *
         * @return the starting range position.
         */
        public long position() {
           return this.position;
        }

        /**
         * Returns the range length.
         *
         * @return the range length.
         */
        public long length() {
           return this.length;
        }

        /**
         * Returns -1 if the starting position of this range is less than the starting position of the argument,
         * 1 if it is greater and 0 if they are equal.
         *
         * @param o another range.
         * @return  -1 if the starting position of this range is less than the starting position of the argument,
         *          1 if it is greater and 0 if they are equal.
         */
        public int compareTo(Range o) {
            return Long.compare(position, o.position);
        }


        /**
         * Returns a brief string description of this factory.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return Long.toHexString(position) + "h..+" + Long.toHexString(length) + "h";
        }

        /**
         * Returns the hash code for this range.
         *
         * @return the hash code for this range.
         */
        public int hashCode() {
            int v1 = (int)(position >>> 32) * 37 + (int)position;
            int v2 = (int)(length >>> 32) * 37 + (int)length;
            return v1 * 37 + v2;
        }

        /**
         * Indicates whether some other range is equal to this one, i&#46;e&#46; it
         * contains the same starting position and length.
         *
         * @param obj some another range.
         * @return    <code>true</code> if the passed object is an instance of this class and
         *            contains the same starting position and length.
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof Range r)) {
                return false;
            }
            return r.position == position && r.length == length;
        }
    }

    /**
     * <p>An object allowing to access mapped data, returned by the {@link DataFile#map} method.</p>
     *
     * <p>Objects implementing this interface may be not <b>immutable</b>
     * and not <b>thread-safe</b>, but must be <b>thread-compatible</b>
     * (allow manual synchronization for multithreading access).</p>
     *
     */
    interface BufferHolder {
        /**
         * Returns the mapped region within the file, in bytes.
         * The result is identical to the first argument of {@link DataFile#map(Range, boolean)} method,
         * which was called to create this instance.
         *
         * @return the mapped region within the file, in bytes.
         */
        Range range();

        /**
         * Returns the mapped data. Usually returns <code>MappedByteBuffer</code>/
         *
         * <p>This method never throws exceptions.
         *
         * @return the mapped data.
         */
        ByteBuffer data();

        /**
         * Returns the object which deallocation by the garbage collector allows all
         * manipulations with the source mappable object, including deletion and
         * any resizing. Usually returns the same result as {@link #data()}.
         *
         * <p>Used by implementation of AlgART arrays with
         * {@link net.algart.finalizing.Finalizer#invokeOnDeallocation} method.
         *
         * <p>This method never throws exceptions.
         *
         * @return the object which deallocation allows all manipulations with the source mappable object.
         */
        Object mappingObject();

        /**
         * Makes an effort to ensure that this buffer's content will be resident in physical memory.
         * In other words, this method tries to preload the content of this buffer into RAM
         * to provide fastest access to its content in the nearest future.
         */
        void load();

        /**
         * Forces any changes made to this buffer's content to be written to the
         * storage device containing the mapped object.
         *
         * <p>This method may not perform <i>immediate</i> writing data to the storage devices.
         * But it guarantees that any changes in the buffer's content will not be lost.
         * More precisely, it guarantees that:<ul>
         * <li>this data will be correctly read by
         * possible next call of {@link DataFile#map} method;</li>
         * <li>and, for non-temporary files (i.e. for any instances of {@link DataFile}
         * which were not created by {@link DataFileModel#createTemporary} method),
         * the data will be really stored in the file and will be able to be loaded
         * by another software, at least, after shutting down JVM.
         * (There is no this guarantee for temporary files:
         * such files are used by this application only and automatically deleted
         * while JVM shutdown.)
         * </ul>
         *
         * <p>If the <code>forcePhysicalWriting</code> argument is <code>true</code>, this
         * method tries to write data to the storage device immediately.
         * For data file models implemented in this package,
         * it means that the data will be really stored on the disk and will be immediately
         * available for reading by another applications.
         *
         * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
         *                             to the external device.
         * @throws java.io.IOError in a case of some disk errors.
         */
        void flush(boolean forcePhysicalWriting);

        /**
         * Unmaps the data: releases all system resources associated with this mapping.
         *
         * <p>This method always performs, at least, the same actions as {@link #flush(boolean)
         * flush(forcePhysicalWriting)} method with the same <code>forcePhysicalWriting</code> argument.
         *
         * <p>This method may do not actual unmapping. For example, its implementation
         * for {@link DefaultDataFileModel} does not unmap the data.
         *
         * <p>After calling this method, this instance must not be used, excepting calling methods
         * {@link #mappingObject()}, {@link #isLoadedFromCache()} and <code>toString()</code>.
         * In particular, this method must not be called twice.
         * Moreover, behavior of the <code>ByteBuffer</code> returned by the last call of {@link #data()} method
         * becomes undefined.
         *
         * @param forcePhysicalWriting is it necessary to try forcing physical writing all associated resources
         *                             to the external device.
         * @throws java.io.IOError in a case of some disk errors.
         */
        void unmap(boolean forcePhysicalWriting);

        /**
         * This method either performs the same actions as {@link #unmap(boolean) unmap(false)} method
         * and returns <code>true</code>,
         * or performs some reduced form of unmapping (or even does nothing) and returns <code>false</code>.
         *
         * <p>This method is called by AlgART array manager for temporary data files
         * when we are sure that this buffer's content will never be useful in future
         * and may be lost. Namely, it is called while array finalization and while
         * system shutdown. (In all other cases, {@link #unmap(boolean)} method is called instead.)
         *
         * <p>So, the implementation of this method should release all external resources,
         * if there are such resources associated with this mapping buffer, but may not to force writing
         * buffer's content to an external device, if it requires a long time.
         *
         * <p>In {@link DefaultDataFileModel}, this method is equivalent to
         * {@link #unmap(boolean) unmap(false)}} and always
         * returns <code>true</code>. If {@link StandardIODataFileModel} (that does not associate
         * any resources with every buffer), this method does nothing and always returns <code>false</code>.
         *
         * <p>After calling this method, this instance must not be used, excepting calling methods
         * {@link #mappingObject()}, {@link #isLoadedFromCache()} and <code>toString()</code>.
         * In particular, this method must not be called twice.
         * Moreover, behavior of the <code>ByteBuffer</code> returned by the last call of {@link #data()} method
         * becomes undefined.
         *
         * @return <code>true</code> if this method has performed all actions that are performed by
         *         {@link #unmap(boolean) unmap(false)}.
         *         If (and only if) the result is <code>true</code>, we can be sure that the same data
         *         will be loaded by next call {@link DataFile#map} method.
         *
         * @throws java.io.IOError in a case of some disk errors.
         */
        boolean dispose();
        // Result of dispose() method is ignored in current implementation of AlgART arrays.

        /**
         * Returns <code>true</code> if this object was not actually read from the file by
         * {@link DataFile#map(Range, boolean)} method, but was quickly loaded from some cache.
         *
         * <p>This method is used only for debugging (logging).
         *
         * <p>This method never throws exceptions.
         *
         * @return <code>true</code> if this object was quickly loaded from some cache.
         */
        boolean isLoadedFromCache();
    }
}
