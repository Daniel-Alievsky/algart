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

package net.algart.arrays.demo.jre;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Locale;

/**
 * <p>Test for intensive multithreading mapping, that can lead to "Map failed" exception.
 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6776490"
 * >http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6776490</a></p>
 *
 * @author Daniel Alievsky
 */
public class IntensiveMappingTest {
    private static final int MAPPING_DELAY = 1000; //ms
    private static final int NUMBER_OF_MAPPING_ATTEMPTS = 10;
    private static MappedByteBuffer mapWithSeveralAttempts(FileChannel fc, FileChannel.MapMode mode,
        long position, long size) throws IOException
    {
        for (int attempt = 1; attempt < NUMBER_OF_MAPPING_ATTEMPTS; attempt++) {
            try {
                return fc.map(mode, position, size);
            } catch (IOException e) {
                boolean doGc = attempt >= NUMBER_OF_MAPPING_ATTEMPTS / 2;
                System.out.println("...cannot map, try again " + attempt + (doGc ? " (with gc)" : ""));
                if (doGc) {
                    System.runFinalization();
                    System.gc();
                }
                try {
                    Thread.sleep(MAPPING_DELAY);
                } catch (InterruptedException x) {
                    Thread.currentThread().interrupt(); // restore interrupt status
                }
            }
        }
        return fc.map(mode, position, size);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: " + IntensiveMappingTest.class.getName()
                + " multi|single blockSize numberOfMappings file1 [file2...]");
            System.out.println("These files will not be modified");
            return;
        }
        boolean multithreading;
        if (args[0].equalsIgnoreCase("multi"))
            multithreading = true;
        else if (args[0].equalsIgnoreCase("single"))
            multithreading = false;
        else
            throw new IllegalArgumentException("First argument must be \"multi\" or \"single\"");
        final int blockSize = Integer.parseInt(args[1]);
        if (blockSize <= 0)
            throw new IllegalArgumentException("blockSize <= 0");
        final int numberOfMappings = Integer.parseInt(args[2]);
        if (numberOfMappings <= 0)
            throw new IllegalArgumentException("numberOfMappings <= 0");
        RandomAccessFile[] raf = new RandomAccessFile[args.length - 3];
        final FileChannel[] fc = new FileChannel[raf.length];
        long minLen = Long.MAX_VALUE;
        for (int k = 0; k < raf.length; k++) {
            raf[k] = new RandomAccessFile(args[k + 3], "r");
            fc[k] = raf[k].getChannel();
            minLen = Math.min(minLen, raf[k].length());
        }
        final long len = minLen;

        final long t1 = System.nanoTime();

        if (multithreading) {
            final Thread[] threads = new Thread[numberOfMappings];
            final AtomicInteger numberOfSimultaneousMappings = new AtomicInteger(0);
            for (int threadIndex = 0; threadIndex < numberOfMappings; threadIndex++) {
                final int ti = threadIndex;
                threads[ti] = new Thread() {
                    public void run() {
                        for (long k = 0, n = len / blockSize; k < n; k++) {
                            int nsm = -1;
                            try {
                                MappedByteBuffer mbb =
//                                    fc[ti % fc.length].map(FileChannel.MapMode.READ_ONLY, k * blockSize, blockSize);
                                    mapWithSeveralAttempts(fc[ti % fc.length],
                                        FileChannel.MapMode.READ_ONLY, k * blockSize, blockSize);
                                nsm = numberOfSimultaneousMappings.incrementAndGet();
                                synchronized (fc) {
                                    mbb.load();
                                }
                                ByteBuffer bb = ByteBuffer.allocate(1000000);
                                for (int j = 0, m = Math.min(mbb.limit(), bb.limit()); j < m; j++) {
                                    bb.put(j, mbb.get(j)); // some operations with mbb
                                }
                                numberOfSimultaneousMappings.decrementAndGet();

                            } catch (IOException e) {
                                synchronized (fc) {
                                    System.out.printf(Locale.US, "%n%nERROR in thread %d!%n", ti);
                                    e.printStackTrace();
                                    System.gc();
                                    try {
                                        System.out.println("Press ENTER to continue...");
                                        new BufferedReader(new InputStreamReader(System.in)).readLine();
                                    } catch (IOException e1) {
                                    }
                                }
                            }
                            long t2 = System.nanoTime();
                            synchronized (fc) {
                                System.out.printf(Locale.US,
                                    "%2d, %2d simultaneous: %.3f MB read in %.5f seconds (%.3f MB/sec)%n",
                                    ti, nsm,
                                    (k + 1) * blockSize / 1048576.0, (t2 - t1) * 1e-9,
                                    (k + 1) * blockSize / 1048576.0 / ((t2 - t1) * 1e-9));
                            }
                        }
                    }
                };
            }
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } else {

            final MappedByteBuffer[] mbb = new MappedByteBuffer[numberOfMappings];
            for (long k = 0, n = len / blockSize; k < n; k++) {
                for (int mi = 0; mi < numberOfMappings; mi++) {
                    try {
                        mbb[mi] = null;
                        mbb[mi] =
//                            fc[mi % fc.length].map(FileChannel.MapMode.READ_ONLY, k * blockSize, blockSize);
                            mapWithSeveralAttempts(fc[mi % fc.length],
                                FileChannel.MapMode.READ_ONLY, k * blockSize, blockSize);
                        mbb[mi].load();
                    } catch (IOException e) {
                        System.out.printf(Locale.US, "%n%nERROR while mapping #%d!%n", mi);
                        e.printStackTrace();
                        try {
                            System.out.println("Press ENTER to continue...");
                            new BufferedReader(new InputStreamReader(System.in)).readLine();
                        } catch (IOException e1) {
                        }
                    }
                    long t2 = System.nanoTime();
                    System.out.printf(Locale.US, "%3d: %.3f MB read in %.5f seconds (%.3f MB/sec)%n",
                        mi, (k + 1) * blockSize / 1048576.0, (t2 - t1) * 1e-9,
                        (k + 1) * blockSize / 1048576.0 / ((t2 - t1) * 1e-9));
                }
            }
        }

        System.out.println("OK");
    }
}
