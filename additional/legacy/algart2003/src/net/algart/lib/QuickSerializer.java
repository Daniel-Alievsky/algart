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
import net.algart.array.*;

/**
 * <p>A simple tool for quick serialization of some data types.
 *
 * <p>Warning: you should not pass incorrect strings in <tt>writeString</tt>,
 * <tt>writeStrings</tt>, <tt>writeStrings2d</tt> methods. Here <i>incorrect</i>
 * <tt>String</tt> means a string containing malformed surrogate char elements, as described at
 * http://java.sun.com/j2se/1.5.0/docs/api/java/io/OutputStreamWriter.html
 * Such strings may damage the serialized file.
 *
 * @author  Daniel Alievsky
 */

public class QuickSerializer {

    public static final int AQS_VERSION = Integer.getInteger("AQS_VERSION", 2);
    private static final String AQS_2_SIG = "~~AQS02~";

    public static final TextIO TEXT_IO = TextIO.UNICODE_BIG;
    private char[] data;
    private int dataLen;
    private int dataOfs;
    private int version;
    public int offset() {return dataOfs;}
    public int length() {return dataLen;}
    public char[] getData() {return data;}

    public QuickSerializer(char[] data) {
        this.data= data;
        this.dataLen= data.length;
        this.version = 1;
        this.dataOfs = 0;
        if (dataLen > AQS_2_SIG.length()) {
            String sig = String.valueOf(data, 0, AQS_2_SIG.length());
            if (sig.equals(AQS_2_SIG)) {
                this.version = 2;
                this.dataOfs = AQS_2_SIG.length();
            }
        }
    }
    public QuickSerializer(String fileName) throws IOException {
        this(new File(fileName));
    }
    public QuickSerializer(File file) throws IOException {
        this(readFilePrivate(file));
    }
    private static char[] readFilePrivate(File file) throws IOException {
        String s = TEXT_IO.read(file);
        char[] result = new char[s.length()];
        s.getChars(0,result.length,result,0);
        return result;
    }
    public QuickSerializer() {
        this.version = AQS_VERSION;
        this.data= new char[256];
        this.dataLen = 0;
        if (version >= 2) {
            AQS_2_SIG.getChars(0, AQS_2_SIG.length(), this.data, 0);
            this.dataLen = AQS_2_SIG.length();
        }
    }
    public void save(String fileName) throws IOException {
        save(new File(fileName));
    }
    public void save(File file) throws IOException {
        TEXT_IO.writeChars(file,Arrays.copy(data,0,dataLen));
    }

    public int version() {
        return this.version;
    }

    public boolean readBoolean() {
        int v= data[dataOfs++];
        return v!=0;
    }
    public void writeBoolean(boolean v) {
        data= (char[])Arrays.ensureCapacity(data,dataLen+1);
        data[dataLen++]= v?(char)1:(char)0;
    }
    public int readInt() {
//    int v= data[dataOfs++];
//    if (v!=0x8000) return (short)v;
//    return data[dataOfs++]|(data[dataOfs++]<<16);
//          - THE PREVIOUS CODE PRODUCES A FATAL ERROR UNDER Java 1.4.2_05 WITH -server KEY
        if (version < 2) {
            return data[dataOfs++] | (data[dataOfs++] << 16);
            // - THIS CODE IS INCORRECT SINCE Java 1.6: SOME int VALUES MAY PRODUCE  MALFORMED SURROGATE PAIRS
            // (0xD800..0xDFFF, see http://java.sun.com/j2se/1.4.2/docs/api/java/io/OutputStreamWriter.html)
        } else {
            return (data[dataOfs++] & 0x0FFF)
                | ((data[dataOfs++] & 0x0FFF) << 12)
                | ((data[dataOfs++] & 0x00FF) << 24);
        }
    }
    public void writeInt(int v) {
//    if (v>=-0x7FFF && v<=0x7FFF) {
//      data= (char[])Arrays.ensureCapacity(data,dataLen+1);
//      data[dataLen++]= (char)v;
//    } else {
//      data= (char[])Arrays.ensureCapacity(data,dataLen+3);
//      data[dataLen++]= (char)0x8000;
//      data[dataLen++]= (char)(v&0xFFFF);
//      data[dataLen++]= (char)(v>>>16);
//    }
//          - THE PREVIOUS CODE PRODUCES A FATAL ERROR UNDER Java 1.4.2_05 WITH -server KEY
        if (version < 2) {
            data = (char[])Arrays.ensureCapacity(data, dataLen + 2);
            data[dataLen++] = (char)(v & 0xFFFF);
            data[dataLen++] = (char)(v >>> 16);
            // - THIS CODE IS INCORRECT SINCE Java 1.6: SOME int VALUES MAY PRODUCE  MALFORMED SURROGATE PAIRS
            // (0xD800..0xDFFF, see http://java.sun.com/j2se/1.4.2/docs/api/java/io/OutputStreamWriter.html)
        } else {
            data = (char[])Arrays.ensureCapacity(data, dataLen + 3);
            data[dataLen++] = (char)(v & 0x0FFF);
            data[dataLen++] = (char)((v >>> 12) & 0x0FFF);
            data[dataLen++] = (char)(v >>> 24);
        }
    }
    public long readLong() {
        return (long)readInt()|((long)readInt())<<32;
    }
    public void writeLong(long v) {
        writeInt((int)v);
        writeInt((int)(v>>>32));
    }
    public String readString() {
        int len= readInt();
        if (len<0) return null;
        String v= new String(data,dataOfs,len);
        dataOfs+= len;
        return v;
    }
    public void writeString(String v) {
        if (v==null) {
            writeInt(-1); return;
        }
        int len= v.length();
        writeInt(len);
        data= (char[])Arrays.ensureCapacity(data,dataLen+len);
        v.getChars(0,len,data,dataLen);
        dataLen+= len;
    }

    public String[] readStrings() {
        int len= readInt();
        if (len<0) return null;
        String[] v= new String[len];
        for (int k=0; k<len; k++) v[k]= readString();
        return v;
    }
    public void writeStrings(String[] v) {
        if (v==null) {
            writeInt(-1); return;
        }
        int len= v.length;
        writeInt(len);
        for (int k=0; k<len; k++) writeString(v[k]);
    }

    public String[][] readStrings2d() {
        int len= readInt();
        if (len<0) return null;
        String[][] v= new String[len][];
        for (int k=0; k<len; k++) v[k]= readStrings();
        return v;
    }
    public void writeStrings2d(String[][] v) {
        if (v==null) {
            writeInt(-1); return;
        }
        int len= v.length;
        writeInt(len);
        for (int k=0; k<len; k++) writeStrings(v[k]);
    }

}
