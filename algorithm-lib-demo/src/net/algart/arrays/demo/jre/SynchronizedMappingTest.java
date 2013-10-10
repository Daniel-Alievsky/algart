package net.algart.arrays.demo.jre;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;


/**
 * <p>Test for synchronized mappings of a large file.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class SynchronizedMappingTest {
    private static final int BLOCK_SIZE = 4096 * 1024;

    public static void main(String... args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: " + SynchronizedMappingTest.class.getName()
                + " fileLength numberOfThreads fileName");
            System.out.println("This file will be created and rewritten");
            return;
        }
        final long len = Long.parseLong(args[0]);
        int numberOfThreads = Integer.parseInt(args[1]);
        RandomAccessFile raf = new RandomAccessFile(args[2], "rw");
        raf.setLength(len);
        final FileChannel fc = raf.getChannel();
        final Thread[] threads = new Thread[numberOfThreads];
        final AtomicLong readyLen = new AtomicLong(0);
        final Object synchronizer = new Object();
        final Object lock = new Object();
        for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
            final int ti = threadIndex;
            final ByteBuffer globalBuffer = ByteBuffer.allocateDirect(BLOCK_SIZE);
            // - some global memory, requiring synchronization
            threads[threadIndex] = new Thread(new Runnable() {
                public void run() {
                    try {
                        for (long pos = ti * BLOCK_SIZE; pos < len; pos += threads.length * BLOCK_SIZE) {
                            String msg = String.format(Locale.US, "%d: %.0f%% %d", ti, pos * 100.0 / len, pos);
                            System.out.println(msg + ": waiting...");
                            synchronized (synchronizer) {
                                long startPosInThisSeries = pos - ti * BLOCK_SIZE;
                                // we start new series per numberOfTasks ranges simultaneously
                                while (readyLen.get() < startPosInThisSeries) {
                                    try {
                                        synchronizer.wait();
                                    } catch (InterruptedException ex) {
                                        ex.printStackTrace();
                                        return;
                                    }
                                }
                            }
                            System.out.println(msg + ": started");
                            synchronized (lock) {
                                int sum = 0;
                                for (int k = 0, n = globalBuffer.limit(); k < n; k++) {
                                    sum += globalBuffer.get(k); // access to the synchronized memory
                                }
                                System.out.println(msg + ": calculated sum = " + sum);
                            }
                            MappedByteBuffer bb;
                            synchronized (lock) {
                                bb = fc.map(FileChannel.MapMode.READ_WRITE, pos, BLOCK_SIZE);
                                System.out.println(ti + ": mapped");
                            }
                            synchronized (lock) {
                                bb.load();
                                System.out.println(msg + " loaded");
                            }
                            for (int k = 0, n = bb.limit(); k < n; k++) {
                                bb.put(k, (byte)(bb.get(k) + 65));
                            }
                            synchronized (lock) {
                                globalBuffer.rewind();
                                globalBuffer.put(bb); // modification of the synchronized memory
                                try {
                                    bb.force();
                                } catch (Exception e) {
                                    e.printStackTrace(); // sometimes possible in usual situation
                                }
                                System.out.println(msg + " corrected and saved");
                            }
                            synchronized (synchronizer) {
                                readyLen.addAndGet(BLOCK_SIZE);
                                synchronizer.notifyAll();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
        }
        long t1 = System.nanoTime();
        for (final Thread thread : threads) {
            thread.start();
        }
        for (final Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.5f sec, %.5f MB/sec",
            (t2 - t1) * 1e-9, len / 1024.0 / 1024.0 / ((t2 - t1) * 1e-9));
    }
}
