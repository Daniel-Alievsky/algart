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

import net.algart.array.*;
import java.io.*;

/**
 * <p>A collection of static methods for reading and writing binary files.
 * Please don't use them for text files (besides {@link #copy(File, File) copy} method):
 * use {@link TextIO} class instead.
 *
 * <p>Methods of this class throw <code>NullPointerException</code>
 * if one of the arguments is null.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public final class BinaryIO {

    // Don't let anyone instantiate this class
    private BinaryIO() {}

    /**
     * Reads all bytes from the current position of the input stream
     * until the end of this stream and returns them as an array of bytes.
     * Useful for reading binary data stored in resources when the
     * <code>Class.getResourceAsStream</code> method is used to
     * access such resources.
     *
     * @param inputStream   the input stream (will be read until its end)
     * @param closeStream   if <code>true</code>, the input stream will be closed for sure
     *    before ending this method
     * @return              all bytes stored in the input stream from the current
     *    position until the end of the stream
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(InputStream inputStream, boolean closeStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("Null inputStream argument in " + BinaryIO.class.getName() + ".read method");
        try {
            byte[] result = new byte[inputStream.available()];
            byte[] buf = new byte[32768];
            int len, ofs = 0;
            while ((len = inputStream.read(buf,0,buf.length)) >= 0) {
                result = (byte[])Arrays.copyEnsured(buf,0,result,ofs,len);
                ofs += len;
            }
            if (result.length == ofs) return result;
            return Arrays.copy(result,0,ofs);
        } finally {
            if (closeStream) {
                try {inputStream.close();} catch (Exception e) {};
            }
        }
    }

    /**
     * Reads all bytes of the given file and returns it as an array of bytes.
     *
     * @param file          the file to be read
     * @return              all bytes of the given file
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(File file) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + BinaryIO.class.getName() + ".read method");
        InputStream inputStream = new FileInputStream(file);
        return read(inputStream,true);
    }


    /**
     * Saves the bytes passed in the <code>data</code> argument in the given file.
     * The file is fully rewritten and will contain this bytes only.
     * In a case of I/O errors, the file will be attempted to be deleted.
     *
     * @param file          the file to be saved
     * @param data          the bytes saved in the file
     * @throws IOException  if an I/O error occurs
     */
    public static void write(File file, byte[] data) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + BinaryIO.class.getName() + ".write method");
        if (data == null) throw new NullPointerException("Null data argument in " + BinaryIO.class.getName() + ".write method");
        OutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(data);
        } catch (IOException e) {
            try {outputStream.close();} catch (Exception e1) {};
            file.delete();
            throw e;
        }
        outputStream.close();
    }

    /**
     * If <code>data == null</code>, deletes <code>file</code>; in other case,
     * equivalent to {@link #write(File, byte[])}.
     *
     * @param file          the file to be saved
     * @param data          the bytes saved in the file
     * @throws IOException  if an I/O error occurs
     */
    public static void writeOrDelete(File file, byte[] data) throws IOException {
        if (data == null) {
            file.delete();
        } else {
            write(file,data);
        }
    }

    /**
     * Fully copies the source file into the target file name.
     * The target file is fully rewritten and will be an exact copy of the source file.
     * In a case of I/O errors, the target file will be attempted to be deleted.
     *
     * @param source        the source file to be copied
     * @param target        the created copy of the source file
     * @throws IOException  if an I/O error occurs
     */
    public static void copy(File source, File target) throws IOException {
        if (source == null) throw new NullPointerException("Null source argument in " + BinaryIO.class.getName() + ".copy method");
        if (target == null) throw new NullPointerException("Null target argument in " + BinaryIO.class.getName() + ".copy method");
        RandomAccessFile input = new RandomAccessFile(source,"r");
        RandomAccessFile output = new RandomAccessFile(target,"rw");
        try {
            byte[] buf = new byte[131072];
            long len = input.length();
            output.setLength(len); // - decrease disk fragmentaion under NTFS
            int bytesRead;
            while ((bytesRead = input.read(buf,0,buf.length)) > 0)
                output.write(buf,0,bytesRead);
        } catch (IOException e) {
            try {input.close();} catch (Exception e1) {};
            try {output.close();} catch (Exception e1) {};
            target.delete();
            throw e;
        }
        try {input.close();} catch (Exception e) {};
        output.close();
    }


    public static Serializable deserialize(InputStream inputStream, boolean closeStream) throws IOException, ClassNotFoundException {
        if (inputStream == null) throw new NullPointerException("Null inputStream argument in " + BinaryIO.class.getName() + ".read method");
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        try {
            return (Serializable)objectInputStream.readObject();
        } finally {
            if (closeStream) {
                try {objectInputStream.close();} catch (Exception e) {};
            }
        }
    }

    public static Serializable deserialize(File file) throws IOException, ClassNotFoundException {
        if (file == null) throw new NullPointerException("Null file argument in " + BinaryIO.class.getName() + ".read method");
        InputStream inputStream = new FileInputStream(file);
        return deserialize(inputStream,true);
    }

    public static void serialize(File file, Serializable data) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + BinaryIO.class.getName() + ".serialize method");
        if (data == null) throw new NullPointerException("Null data argument in " + BinaryIO.class.getName() + ".serialize method");
        OutputStream outputStream = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        try {
            objectOutputStream.writeObject(data);
        } catch (IOException e) {
            try {objectOutputStream.close();} catch (Exception e1) {};
            file.delete();
            throw e;
        }
        objectOutputStream.close();
    }
}



//  /**
//   * An analog of {@link #read(File)} method that returns
//   * its results as <code>int[]</code> instead <code>byte[]</code>.
//   * More precisely, let <code>int[] i</code> is a result of this
//   * method, and <code>byte[] b</code> is a byte arrays that would
//   * be returned by <code>read(file)</code>
//   * in the same situation. Then the following formulas are true:<pre>
//   *    i.length = (b.length+3)/4,
//   *    i[k] = b[4*k]
//   *       | (4*k+1 < b.length? b[4*k+1] &lt;&lt;&lt; 8: 0)
//   *       | (4*k+2 < b.length? b[4*k+2] &lt;&lt;&lt; 16: 0)
//   *       | (4*k+3 < b.length? b[4*k+3] &lt;&lt;&lt; 24: 0),
//   *    where k=0,1,...,i.length-1</pre>
//   * In other words, the bytes are packed into <code>int</code> values
//   * in usual order used by Intel processors, and the last <code>int</code>
//   * is appended by zeros if the number of read bytes is not divided by 4.
//   *
//   * <p>This method can be useful while reading binary files containing
//   * arrays of 32-bit values, such as pixel arrays.
//   *
//   * @param file          the file to be read
//   * @return              all bytes of the given file
//   *    packed into <code>int</code> values
//   * @throws IOException  if an I/O error occurs
//   */
//  public static int[] readAsInts(File file) throws IOException {
//    if (file == null) throw new NullPointerException("Null file argument in " + BinaryIO.class.getName() + ".readAsInts method");
//    InputStream inputStream = new FileInputStream(file);
//    return readAsInts(inputStream,true);
//  }
//