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
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
