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

package net.algart.arrays.demo.jre;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.security.*;
import java.lang.reflect.Method;
import java.lang.ref.*;
import java.util.List;
import java.util.ArrayList;

/**
 * <p>Simple test for intensive mapping for filling a large file, that can lead to RAM leak.
 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6893654"
 * >http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6893654</a>.</p>
 *
 * @author Daniel Alievsky
 */
public class SimpleMappingNewFileTest {
    static final int BLOCK_SIZE = 8 * 1024 * 1024; // 8 MB

    private static void unsafeUnmap(final MappedByteBuffer mbb) throws PrivilegedActionException {
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
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: " + SimpleMappingNewFileTest.class.getName()
                + " tempFileNameBegin fileSize numberOfFiles [-force]");
            return;
        }
        String tempFileNameBegin = args[0];
        long fileLength = Long.parseLong(args[1]);
        int numberOfFiles = Integer.parseInt(args[2]);
        boolean doForce = args.length > 3 && args[3].equals("-force");
        long numberOfBlocks = (fileLength + BLOCK_SIZE - 1) / BLOCK_SIZE;
        fileLength = numberOfBlocks * BLOCK_SIZE;
        ByteBuffer pattern = ByteBuffer.allocateDirect(BLOCK_SIZE);
        for (int j = 0; j < BLOCK_SIZE; j++) {
            pattern.put((byte)j);
        }
        for (int count = 0; count < numberOfFiles; count++) {
            File file = new File(tempFileNameBegin + "_" + count);
            if (file.exists()) {
                file.delete();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(fileLength);
            for (long i = 0; i < numberOfBlocks; i++) {
                long pos = i * BLOCK_SIZE;
                MappedByteBuffer mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, pos, BLOCK_SIZE);
                pattern.rewind();
                mbb.put(pattern);
                if (doForce) {
                    mbb.force();
                }
//                references.add(new SoftReference<Object>(mbb)); // leads to OutOfMemory in 32 bits
//                unsafeUnmap(mbb);
//                System.gc();
                System.out.printf("\r%s %d MB...", file, (pos + BLOCK_SIZE) / 1048576);
            }
            raf.close();
        }
        System.out.println(" done");
    }
}
