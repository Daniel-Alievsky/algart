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

import java.io.*;

/**
 * <p>A tool for reading and writing text files.
 * Different instances of this class use different encoding
 * ("UTF-8", "ASCII", "UnicodeBig", etc.) To write or read
 * the text file in the given encoding, you may use one
 * of the constants declared in this class (such as {@link
 * #ASCII}) or create a new instance of the {@link ForCharset}
 * inheritor by {@link ForCharset#getInstance(String)} call.
 *
 * <p>Methods of this class throw <code>NullPointerException</code>
 * if one of the arguments is null.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public abstract class TextIO {

    // Don't let anyone instantiate or extend this class
    private TextIO() {}

    /**
    * This <code>TextIO</code> instance writes and reads text files
    * in the current system-dependent encoding.
    */
    public static final SystemDefault SYSTEM_DEFAULT = new SystemDefault();

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 7-bit "ASCII" encoding.
     */
    public static final ForCharset ASCII = ForCharset.getInstanceForStandardCharset("ASCII");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 8-bit "Cp1252" encoding.
     */
    public static final ForCharset CP1252 = ForCharset.getInstanceForStandardCharset("Cp1252");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 8-bit "ISO8859_1" encoding.
     */
    public static final ForCharset ISO8859_1 = ForCharset.getInstanceForStandardCharset("ISO8859_1");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 16-bit "UnicodeBig" encoding. In this encoding:<ul>
     *
     * <li>every character is written as its 16-bit Unicode number:
     * high bytes (bits 8-15) of all characters are stored at the offsets
     * 2*k (k=1,2,...) from the beginning of the file, low bytes (bits 0-7)
     * are stored at the offsets 2*k+1 (k=1,2,...) <b>after</b> bits 8-15;
     *
     * <li>first 16 bits of the text file contain 0xFEFF prefix
     * (0xFE is the first byte of the file, 0xFF is the second).
     * </ul>
     */
    public static final ForCharset UNICODE_BIG = ForCharset.getInstanceForStandardCharset("UnicodeBig");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 16-bit "UnicodeBigUnmarked" encoding. In this encoding:<ul>
     *
     * <li>every character is written as its 16-bit Unicode number:
     * high bytes (bits 8-15) of all characters are stored at the offsets
     * 2*k (k=0,1,...) from the beginning of the file, low bytes (bits 0-7)
     * are stored at the offsets 2*k+1 (k=0,1,...) <b>after</b> bits 8-15;
     *
     * <li>start file prefix is not used, unlike {@link #UNICODE_BIG} encoding.
     * </ul>
     */
    public static final ForCharset UNICODE_BIG_UNMARKED = ForCharset.getInstanceForStandardCharset("UnicodeBigUnmarked");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 16-bit "UnicodeLittle" encoding. In this encoding:<ul>
     *
     * <li>every character is written as its 16-bit Unicode number:
     * low bytes (bits 0-7) of all characters are stored at the offsets
     * 2*k (k=1,2,...) from the beginning of the file, high bytes (bits 8-15)
     * are stored at the offsets 2*k+1 (k=1,2,...) (such an order is usual
     * for Intel processors);
     *
     * <li>first 16 bits of the text file contain 0xFEFF prefix
     * (0xFF is the first byte of the file, 0xFE is the second, unlike
     * {@link #UNICODE_BIG}).
     * </ul>
     */
    public static final ForCharset UNICODE_LITTLE = ForCharset.getInstanceForStandardCharset("UnicodeLittle");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 16-bit "UnicodeLittleUnmarked" encoding. In this encoding:<ul>
     *
     * <li>every character is written as its 16-bit Unicode number:
     * low bytes (bits 0-7) of all characters are stored at the offsets
     * 2*k (k=0,1,...) from the beginning of the file, high bytes (bits 8-15)
     * are stored at the offsets 2*k+1 (k=0,1,...) (such an order is usual
     * for Intel processors);
     *
     * <li>start file prefix is not used, unlike {@link #UNICODE_LITTLE} encoding.
     * </ul>
     */
    public static final ForCharset UNICODE_LITTLE_UNMARKED = ForCharset.getInstanceForStandardCharset("UnicodeLittleUnmarked");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard 16-bit "UTF-16" encoding. In this encoding,
     * unlike {@link #UNICODE_BIG} and {@link #UNICODE_LITTLE},
     * an order of writing bytes in every character can depend on
     * the used operating system. The start 16-bit prefix 0xFEFF
     * is always written in the text file. The read methods use
     * this prefix to determine the order of bytes: if the first byte
     * is 0xFE, it is "big-endian" order (as in {@link #UNICODE_BIG}),
     * if the first byte is 0xFF, it is "little-endian" order
     * (as in {@link #UNICODE_LITTLE}).
     */
    public static final ForCharset UTF16 = ForCharset.getInstanceForStandardCharset("UTF-16");

    /**
     * This <code>TextIO</code> instance writes and reads text files
     * in the standard "UTF-8" encoding. It is the best variant for
     * most needs.
     */
    public static final ForCharset UTF8 = ForCharset.getInstanceForStandardCharset("UTF-8");

    abstract InputStreamReader getReader(InputStream inputStream);
    abstract OutputStreamWriter getWriter(OutputStream outputStream);

    /**
     * Reads the text file from the current position of the input stream
     * until the end of this stream and returns it as a <code>String</code>.
     * Useful for reading text data stored in resources when the
     * <code>Class.getResourceAsStream</code> method is used to
     * access such resources.
     *
     * @param inputStream   the input stream (will be read until its end)
     * @param closeStream   if <code>true</code>, the input stream will be closed for sure
     *    before ending this method
     * @return              the full text stored in the input stream from the current
     *    position until the end of the stream
     * @throws IOException  if an I/O error occurs
     * @see                 #readChars(InputStream, boolean)
     */
    public final String read(InputStream inputStream, boolean closeStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("Null inputStream argument in " + TextIO.class.getName() + ".read method");
        StringBuffer sb = new StringBuffer();
        InputStreamReader reader = getReader(inputStream);
        try {
            char[] buf= new char[32768];
            int len;
            while ((len = reader.read(buf)) >= 0) {
                sb.append(buf, 0, len);
            }
            return sb.toString();
        } finally {
            if (closeStream) {
                try {reader.close();} catch (Exception e) {};
            }
        }
    }

    /**
     * Reads the text file from the current position of the input stream
     * until the end of this stream and returns it as a <code>char[]</code>.
     * Useful for reading text data stored in resources when the
     * <code>Class.getResourceAsStream</code> method is used to
     * access such resources.
     *
     * @param inputStream   the input stream (will be read until its end)
     * @param closeStream   if <code>true</code>, the input stream will be closed for sure
     *    before ending this method
     * @return              the full text stored in the input stream from the current
     *    position until the end of the stream
     * @throws IOException  if an I/O error occurs
     * @see                 #read(InputStream, boolean)
     */
    public final char[] readChars(InputStream inputStream, boolean closeStream) throws IOException {
        if (inputStream == null) throw new NullPointerException("Null inputStream argument in " + TextIO.class.getName() + ".read method");
        char[] result = new char[16];
        int resultLen = 0;
        InputStreamReader reader = getReader(inputStream);
        try {
            char[] buf= new char[32768];
            int len;
            while ((len = reader.read(buf)) >= 0) {
                if (len > 0) {
                    if (resultLen + len > result.length) {
                        int newCapacity = (result.length + 1) * 2;
                        if (newCapacity < 0) {
                            newCapacity = Integer.MAX_VALUE;
                        } else if (newCapacity < resultLen + len) {
                            newCapacity = resultLen + len;
                        }
                        char newResult[] = new char[newCapacity];
                        System.arraycopy(result, 0, newResult, 0, resultLen);
                        result = newResult;
                    }
                    System.arraycopy(buf, 0, result, resultLen, len);
                    resultLen += len;
                }
            }
            if (resultLen < result.length) {
                char[] resultBegin = new char[resultLen];
                System.arraycopy(result, 0, resultBegin, 0, resultLen);
                return resultBegin;
            }
            return result;
        } finally {
            if (closeStream) {
                try {reader.close();} catch (Exception e) {};
            }
        }
    }

    /**
     * Reads the full content of the given text file and returns it as a <code>String</code>.
     *
     * @param file          the file to be read
     * @return              the full text content of the given file
     * @throws IOException  if an I/O error occurs
     * @see                 #readChars(File)
     */
    public final String read(File file) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + TextIO.class.getName() + ".read method");
        InputStream inputStream = new FileInputStream(file);
        return read(inputStream,true);
    }

    /**
     * Reads the full content of the given text file and returns it as a <code>char[]</code>.
     *
     * @param file          the file to be read
     * @return              the full text content of the given file
     * @throws IOException  if an I/O error occurs
     * @see                 #read(File)
     */
    public final char[] readChars(File file) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + TextIO.class.getName() + ".readChars method");
        InputStream inputStream = new FileInputStream(file);
        return readChars(inputStream,true);
    }

    private void writePrivate(File file, Object data) throws IOException {
        OutputStream outputStream = new FileOutputStream(file);
        OutputStreamWriter writer = getWriter(outputStream);
        try {
            if (data instanceof String)
                writer.write((String)data);
            else
                writer.write((char[])data);
        } catch (IOException e) {
            try {writer.close();} catch (Exception e1) {};
            file.delete();
            throw e;
        }
        writer.close();
    }

    /**
     * Saves the text passed in the <code>data</code> argument in the given text file.
     * The file is fully rewritten and will contain this text only.
     * In a case of I/O errors, the file will be attempted to be deleted.
     *
     * @param file          the file to be saved
     * @param data          the text saved in the file (<code>String</code> object)
     * @throws IOException  if an I/O error occurs
     * @see                 #writeChars(File, char[])
     */
    public final void write(File file, String data) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + TextIO.class.getName() + ".write method");
        if (data == null) throw new NullPointerException("Null data argument in " + TextIO.class.getName() + ".write method");
        writePrivate(file,data);
    }

    /**
     * Saves the text passed in the <code>data</code> argument in the given text file.
     * The file is fully rewritten and will contain this text only.
     * In a case of I/O errors, the file will be attempted to be deleted.
     *
     * @param file          the file to be saved
     * @param data          the text saved in the file (array of characters)
     * @throws IOException  if an I/O error occurs
     * @see                 #write(File, String)
     */
    public final void writeChars(File file, char[] data) throws IOException {
        if (file == null) throw new NullPointerException("Null file argument in " + TextIO.class.getName() + ".writeChars method");
        if (data == null) throw new NullPointerException("Null data argument in " + TextIO.class.getName() + ".writeChars method");
        writePrivate(file,data);
    }

    /**
     * If <code>data == null</code>, deletes <code>file</code>; in other case,
     * equivalent to {@link #write(File, String)}.
     *
     * @param file          the file to be saved
     * @param data          the bytes saved in the file
     * @throws IOException  if an I/O error occurs
     */
    public final void writeOrDelete(File file, String data) throws IOException {
        if (data == null) {
            file.delete();
        } else {
            write(file,data);
        }
    }

    /**
     * If <code>data == null</code>, deletes <code>file</code>; in other case,
     * equivalent to {@link #writeChars(File, char[])}.
     *
     * @param file          the file to be saved
     * @param data          the bytes saved in the file
     * @throws IOException  if an I/O error occurs
     */
    public final void writeCharsOrDelete(File file, char[] data) throws IOException {
        if (data == null) {
            file.delete();
        } else {
            writeChars(file,data);
        }
    }

    /**
     * An implementation of {@link TextIO} class that writes and reads text files
     * in the current system-dependent encoding. This class is a singleton:
     * you cannot create its instances, you can only use {@link TextIO#SYSTEM_DEFAULT}
     * constant.
     */
    public static final class SystemDefault extends TextIO {
        // Don't let anyone instantiate this class
        private SystemDefault() {}

        /**
         * Returns a string representation of this object.
         * This method is intended to be used only for debugging purposes,
         * and the content and format of the returned string may vary between
         * implementations.
         * The returned string may be empty but may not be <code>null</code>.
         *
         * @return  a string representation of this rectangle
         */
        public String toString() {
            return "I/O with current default system encoding";
        }
        InputStreamReader getReader(InputStream inputStream) {
            return new InputStreamReader(inputStream);
        }
        OutputStreamWriter getWriter(OutputStream outputStream) {
            return new OutputStreamWriter(outputStream);
        }
    }

    /**
     * An implementation of {@link TextIO} class that writes and reads text files
     * in encodings specified by their names (as the second argument of
     * <code>InputStreamReader</code> and <code>OutputStreamWriter</code> constructors).
     * You can create an instance of this class for any desired encoding by the
     * {@link #getInstance(String)} method or use one of the following constants declared
     * in <code>TextIO</code> class:<ul>
     *
     * <li>{@link TextIO#ASCII}
     * <li>{@link TextIO#CP1252}
     * <li>{@link TextIO#ISO8859_1}
     * <li>{@link TextIO#UNICODE_BIG}
     * <li>{@link TextIO#UNICODE_BIG_UNMARKED}
     * <li>{@link TextIO#UNICODE_LITTLE}
     * <li>{@link TextIO#UNICODE_LITTLE_UNMARKED}
     * <li>{@link TextIO#UTF16}
     * <li>{@link TextIO#UTF8}
     * </ul>
     *
     * These constants correspond some standard encodings that are supported
     * by all possible J2RE installations: see
     * <a href="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">"supported
     * encodings"</a> page on the Sun's site.
     */
    public static final class ForCharset extends TextIO {
        private final String charsetName;
        // Don't let anyone instantiate this class
        private ForCharset(String charsetName) throws UnsupportedEncodingException {
            if (charsetName == null) throw new NullPointerException("Null charsetName argument in " + TextIO.class);
            new InputStreamReader(System.in,charsetName);
            new OutputStreamWriter(System.out,charsetName);
            this.charsetName = charsetName;
        }
        /**
         * Returns the name of the encoding used by this instance. The returned name
         * is the canonical name for <code>java.io</code> and <code>java.lang</code> API.
         * (<code>java.nio</code> API uses another names: see
         * <a href="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">"supported
         * encodings"</a> Sun's page for more details.)
         *
         * @return  canonical name of the used encoding (for <code>java.io</code> and <code>java.lang</code> API)
         */
        public String charsetName() {
            return charsetName;
        }
        /**
         * Creates a new instance of this class that will write and read text files
         * in the encoding specified by <code>charsetName</code> argument.
         * <code>charsetName</code> should be the canonical encoding name for
         * <code>java.io</code> and <code>java.lang</code> API.
         * (<code>java.nio</code> API uses another names: see
         * <a href="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">"supported
         * encodings"</a> Sun's page for more details.)
         *
         * @param charsetName   canonical name of the encoding that will be used by new instance
         * @return              the new created instance of this class
         * @throws UnsupportedEncodingException   if the given encoding is not supported
         */
        public static ForCharset getInstance(String charsetName) throws UnsupportedEncodingException {
            return new ForCharset(charsetName);
        }
        private static ForCharset getInstanceForStandardCharset(String charsetName) {
            try {
                return new ForCharset(charsetName);
            } catch (UnsupportedEncodingException e) {
                throw (AssertionError)new AssertionError("Unsupported charset " + charsetName + " getInstanceForStandardCharset; maybe, it is not standard").initCause(e);
            }
        }

        InputStreamReader getReader(InputStream inputStream) {
            try {
                return new InputStreamReader(inputStream,charsetName);
            } catch (UnsupportedEncodingException e) {
                throw (AssertionError)new AssertionError().initCause(e);
            }
        }
        OutputStreamWriter getWriter(OutputStream outputStream) {
            try {
                return new OutputStreamWriter(outputStream,charsetName);
            } catch (UnsupportedEncodingException e) {
                throw (AssertionError)new AssertionError().initCause(e);
            }
        }

        /**
         * Returns a string representation of this object.
         * This method is intended to be used only for debugging purposes,
         * and the content and format of the returned string may vary between
         * implementations.
         * The returned string may be empty but may not be <code>null</code>.
         *
         * @return  a string representation of this rectangle
         */
        public String toString() {
            return "I/O with " + charsetName + " charset";
        }
        /**
         * Returns the hash code for this instance.
         * @return  a hash code for this instance
         */
        public int hashCode() {
            return charsetName.hashCode();
        }
        /**
         * Determines whether or not two instances are equal.
         * Two instances of <code>ForCharset</code> are equal if they
         * have equal encoding names.
         *
         * @param   obj an object to be compared with this <code>Rect</code>
         * @return  <code>true</code> if the object to be compared is an instance of
         *    <code>ForCharset</code> and has the same encoding name
         */
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != this.getClass()) return false;
            return ((ForCharset)obj).charsetName.equals(this.charsetName);
        }
    }
}
