/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.finalizing.Finalizer;

/**
 * <p>Test for close, resize and delete operations together with file mapping.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.4
 */
public class DisposingMappedFilesTest {
    boolean noPauses = false;
    boolean gcBeforeReducing = false;
    boolean gcBeforeDeleting = false;
    boolean gcBeforeTestFinish = false;
    boolean gcBeforeExit = false;
    boolean ownShutdownCleaner = false;
    boolean deleteInFinalize = false;
    boolean deleteByFinalizer = false;
    boolean reduceByFinalizer = false;
    private void pause(String msg) throws InterruptedException {
        pause(msg, null);
    }

    private void pause(String msg1, String msg2) throws InterruptedException {
        System.out.println(msg1);
        if (noPauses) {
            if (msg2 != null)
                System.out.println("..." + msg2 + "...");
            return;
        }
        if (msg2 == null)
            System.out.print("Press ENTER to continue...");
        else
            System.out.print("Press ENTER to continue: " + msg2 + "...");
        try {
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if (line == null) {
                throw new InterruptedException();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    int maxStringLength = 20;
    private String toString(ByteBuffer bb) {
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < bb.limit(); k++) {
            if (k > 0)
                sb.append(", ");
            sb.append(bb.get(k) & 0xFF);
            if (k == maxStringLength / 2 - 1 && bb.limit() > maxStringLength) {
                sb.append(", ...");
                k = bb.limit() - maxStringLength / 2 - 1;
            }
        }
        return sb.toString();
    }

    private static final int FILL_BLOCK_LEN = 256; // must be 2^k
    private static void fillByteBuffer(ByteBuffer dest, int destPos, int count, byte value) {
        if (count >= FILL_BLOCK_LEN) {
            byte[] arr = new byte[FILL_BLOCK_LEN];
            if (value != 0)
                for (int j = 0; j < arr.length; j++)
                    arr[j] = value;
            ByteBuffer destDup = dest.duplicate();
            destDup.position(destPos);
            for (; count >= FILL_BLOCK_LEN; count -= FILL_BLOCK_LEN)
                destDup.put(arr);
            destDup.put(arr, 0, count);
        } else {
            for (int k = 0; k < count; k++)
                dest.put(destPos + k, value);
        }
    }

    public static class MappedBuffers {
        MappedByteBuffer bb1, bb2, bb3, bb4;
        File fileToDeleteInFinalize;
        public class FinHolder {
            protected void finalize() throws Throwable {
                // This solution never works! References to bb1, bb2, ... are alive while executing this finalizer.
                try {
                    if (fileToDeleteInFinalize != null) {
                        boolean deleteResult = fileToDeleteInFinalize.delete();
                        System.out.println("~~ File is " + (deleteResult ? "" : "NOT ") + "deleted");
                    }
                } finally {
                    super.finalize();
                }
            }
        }
    }

    public void test(final File file, long length) throws IOException, InterruptedException {
        if (file.exists()) {
            System.out.println("This file already exists: please choose another file name.");
            return;
        }
        Finalizer fin = new Finalizer();
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(length);
        pause("File " + file + " is created: " + raf.length() + " bytes");

        byte[] arr1 = new byte[maxStringLength / 2], arr2 = new byte[maxStringLength / 2];
        raf.seek(0); raf.read(arr1);
        raf.seek(raf.length()-arr2.length); raf.read(arr2);
        pause("Bytes at start and end of file: " + toString(ByteBuffer.wrap(arr1))
            + "; " + toString(ByteBuffer.wrap(arr2)));

        MappedBuffers mb = new MappedBuffers();
        FileChannel fc = raf.getChannel();
        mb.bb1 = fc.map(FileChannel.MapMode.READ_WRITE, 0, length);
        pause("File channel is created and mapped: " + mb.bb1.limit() + " bytes " + toString(mb.bb1));

        fillByteBuffer(mb.bb1, 0, Math.min(1000000, mb.bb1.limit()), (byte)1);
        pause("First 1000000 bytes are filled by 1: " + toString(mb.bb1));

        mb.bb2 = fc.map(FileChannel.MapMode.READ_WRITE, 500, length - 500);
        pause("Another map is created: " + mb.bb2.limit() + " bytes " + toString(mb.bb2));

        mb.bb1.force();
        pause("Data are flushed to disk: please view the file right now from a parallel task",
            "the file size will be increased by 2000000");

        try {
            raf.setLength(length + 2000000);
            pause("File " + file + " is resized: " + raf.length() + " bytes");
        } catch (IOException ex) {
            ex.printStackTrace();
            pause("New file length: " + raf.length() + " bytes");
        }
        mb.bb3 = fc.map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
        pause("File is mapped again: " + mb.bb3.limit() + " bytes " + toString(mb.bb3),
            "the file size will be set to 800 bytes");

        if (gcBeforeReducing) {
            mb = new MappedBuffers();
            System.gc();
            pause("[System.gc() called]");
        }
        try {
            raf.setLength(800);
            pause("File " + file + " is resized to 800: " + raf.length() + " bytes");
        } catch (IOException ex) {
            ex.printStackTrace();
            pause("New file length: " + raf.length() + " bytes");
        }
        mb.bb4 = fc.map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
        pause("File is mapped again: " + mb.bb4.limit() + " bytes " + toString(mb.bb4));

        if (reduceByFinalizer) {
            // This call disables file removing on exit!
            fin.invokeOnDeallocation(mb, new Runnable() {
                public void run() {
                    try {
                        RandomAccessFile rafTemp = new RandomAccessFile(file, "rw");
                        rafTemp.setLength(700);
                        System.out.println("-- File " + file + " is resized to 700: " + rafTemp.length() + " bytes");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        System.out.println("-- New file length: " + file.length() + " bytes");
                    }
                }
            });
            pause("setLength(700) is planned on storage deallocation",
                "the file will be closed");
        }
        raf.close();
        pause("File is closed; previous mapping: " + mb.bb4.limit() + " bytes " + toString(mb.bb4),
            "the file will be deleted");

        if (gcBeforeDeleting) {
            mb = new MappedBuffers();
            System.gc();
            pause("[System.gc() called]");
        }
        boolean deleteResult = file.delete();
        pause("File is " + (deleteResult ? "" : "NOT ") + "deleted");

        if (deleteResult && file.exists())
            throw new AssertionError("File was not deleted!");

        if (!deleteResult && deleteInFinalize) {
            mb.fileToDeleteInFinalize = file;
            mb.new FinHolder();
            pause("File will be deleted in finalize()");
        }

        if (!deleteResult && deleteByFinalizer) {
            fin.invokeOnDeallocation(mb, new Runnable() {
                public void run() {
                    boolean deleteResult = file.delete();
                    System.out.println("-- File is " + (deleteResult ? "" : "NOT ") + "deleted");
                }
            });
            pause("File deletion is planned on storage deallocation");
        }

        mb = new MappedBuffers();
        if (gcBeforeTestFinish) {
            System.gc();
            pause("[System.gc() called]");
        }
        if (!deleteResult) {
            if (ownShutdownCleaner) {
                TempFiles.deleteOnExit(file);
                pause("File will be deleted on exit by " + TempFiles.class, "the test will be finished");
            } else {
                file.deleteOnExit();
                pause("File will be deleted on exit", "the test will be finished");
            }
        }
        if (gcBeforeExit) {
            long t = System.currentTimeMillis();
            System.out.println("Active task count: " + fin.activeTasksCount());
            while (fin.activeTasksCount() > 0) {
                System.runFinalization();
                System.gc();
                Thread.sleep(50);
                if (System.currentTimeMillis() - t > 5000)
                    break;
                System.out.println("Active task count: " + fin.activeTasksCount());
            }
            for (int k = 0; k < 5; k++) {
                // finalizing some additional objects that could be
                // referred from finalization tasks performed above
                System.runFinalization();
                System.gc();
            }
            pause("[System.gc() loop was performed]");
        }
        if (file.exists())
            pause("File still exists", "the program will exit");
        else
            pause("File does not exist", "the program will exit");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: " + DisposingMappedFilesTest.class.getName()
                + " nameOfNewTemporaryFile fileSize [flags]");
            System.out.println("Possible flags (case insensitive):");
            System.out.println("    -noPauses");
            System.out.println("    -gcBeforeReducing");
            System.out.println("    -gcBeforeDeleting");
            System.out.println("    -gcLoopBeforeExit");
            System.out.println("    -ownShutdownCleaner");
            System.out.println("    -deleteInFinalize");
            return;
        }

        File file = new File(args[0]);
        long length = Long.parseLong(args[1]);
        DisposingMappedFilesTest test = new DisposingMappedFilesTest();
        for (int k = 2; k < args.length; k++) {
            if (args[k].equalsIgnoreCase("-NoPauses"))
                test.noPauses = true;
            else if (args[k].equalsIgnoreCase("-gcBeforeReducing"))
                test.gcBeforeReducing = true;
            else if (args[k].equalsIgnoreCase("-gcBeforeDeleting"))
                test.gcBeforeDeleting = true;
            else if (args[k].equalsIgnoreCase("-gcLoopBeforeExit"))
                test.gcBeforeExit = true;
            else if (args[k].equalsIgnoreCase("-gcBeforeTestFinish"))
                test.gcBeforeTestFinish = true;
            else if (args[k].equalsIgnoreCase("-ownShutdownCleaner"))
                test.ownShutdownCleaner = true;
            else if (args[k].equalsIgnoreCase("-deleteInFinalize"))
                test.deleteInFinalize = true;
            else if (args[k].equalsIgnoreCase("-deleteByFinalizer"))
                test.deleteByFinalizer = true;
            else if (args[k].equalsIgnoreCase("-reduceByFinalizer"))
                test.reduceByFinalizer = true;
            else {
                System.out.println("Unknown flag " + args[k]);
                return;
            }

        }
        try {
            test.test(file, length);
        } catch (InterruptedException ex) {
            System.out.println("");
            System.out.println("Test is interrupted");
        }
    }
}
