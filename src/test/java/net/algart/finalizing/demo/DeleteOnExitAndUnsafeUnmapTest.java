/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.finalizing.demo;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.lang.reflect.*;
import java.security.*;

/**
 * <p>Simplest test for <tt>File.deleteOnExit()</tt> method.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class DeleteOnExitAndUnsafeUnmapTest {
    private static void unsafeUnmap(final MappedByteBuffer mbb) {
        try {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length == 0) {
            System.out.println("Usage: " + DeleteOnExitAndUnsafeUnmapTest.class.getName()
                + " standard|ownShutdownCleaner [map [unsafeUnmap]] [gc]");
            return;
        }

        boolean doMap = false, doGc = false, doClean = false;
        for (int k = 1; k < args.length; k++) {
            if (args[k].equalsIgnoreCase("map"))
                doMap = true;
            if (args[k].equalsIgnoreCase("unsafeUnmap"))
                doClean = true;
            if (args[k].equalsIgnoreCase("gc"))
                doGc = true;
        }

        File file;
        if (args[0].equalsIgnoreCase("standard")) {
            file = File.createTempFile("deleteOnExit1_", ".tmp");
            System.out.println("Created: " + file);
            file.deleteOnExit();
            file = File.createTempFile("deleteOnExit2_", ".tmp");
            System.out.println("Created: " + file);
            file.deleteOnExit();
        } else if (args[0].equalsIgnoreCase("ownShutdownCleaner")) {
            file = File.createTempFile("deleteOnExit1_", ".tmp");
            System.out.println("Created: " + file);
            TempFiles.deleteOnExit(file);
            file = File.createTempFile("deleteOnExit2_", ".tmp");
            System.out.println("Created: " + file);
            TempFiles.deleteOnExit(file);
        } else {
            System.err.println("Illegal mode " + args[0]);
            return;
        }

        if (doMap) {
            System.out.println();
            System.out.println("Mapping " + file + " to ByteBuffer...");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(1000);
            MappedByteBuffer bb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1000);
            raf.close();
            if (doClean) {
                System.out.println("Class of returned ByteBuffer and its superclasses:");
                for (Class<?> c = bb.getClass(); c != null; c = c.getSuperclass()) {
                    System.out.println("  " + c);
                    Method[] methods = c.getDeclaredMethods();
                    for (Method m : methods) {
                        System.out.println("    " + m);
                    }
                }
                System.out.println("Unsafe call of " + bb.getClass());
                unsafeUnmap(bb);
                // System.out.println(bb.get(0)); // internal JRE error here: EXCEPTION_ACCESS_VIOLATION
            }
            // absolutely necessary to delete file on exit:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239
        } else {
            System.out.println();
            System.out.println("Opening " + file + "...");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(65536*1000);
            for (int k = 0; k < 1000; k++) {
                ByteBuffer bb = ByteBuffer.allocateDirect(65536);
                raf.getChannel().read(bb);
                bb.put(3, (byte)64);
                bb.rewind();
                raf.getChannel().position(k * 65536).write(bb);
            }
            raf.close();
        }

        System.out.println();
        System.out.println("Sleeping 1 sec...");
        Thread.sleep(1000);

        if (doGc) {
            System.out.println("Calling System.gc() 5 times");
            for (int k = 0; k < 5; k++)
                System.gc();
        }

        System.out.println("Sleeping 1 sec...");
        Thread.sleep(1000);
        System.out.println();
    }
}
