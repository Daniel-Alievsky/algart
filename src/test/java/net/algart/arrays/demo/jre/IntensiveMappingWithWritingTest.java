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

package net.algart.arrays.demo.jre;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * <p>Test for very intensive file mappings. See JVM bugs in comments at the end of the source file.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.4
 */
public class IntensiveMappingWithWritingTest {
    public static void main(String[] args) throws IOException {
        File f;
        int blockSize, numberOfBlocks, numberOfPasses, numberOfTests, gcStep = 0;
        boolean readOnly, doForce = false, doGc = false, doRunFinalization = false;
        try {
            int nextArgIndex = 0;
            if (args[nextArgIndex].equalsIgnoreCase("READ")) {
                readOnly = true; nextArgIndex++;
            } else if (args[nextArgIndex].equalsIgnoreCase("WRITE")) {
                readOnly = false; nextArgIndex++;
            } else
                throw new IllegalArgumentException("Unknown mode " + args[nextArgIndex]);
            f = new File(args[nextArgIndex++].replaceAll("\\%MILLIS\\%",
                String.valueOf(System.currentTimeMillis())));
            blockSize = Integer.parseInt(args[nextArgIndex++]);
            numberOfBlocks = Integer.parseInt(args[nextArgIndex++]);
            numberOfPasses = Integer.parseInt(args[nextArgIndex++]);
            numberOfTests = Integer.parseInt(args[nextArgIndex++]);
            if (args.length > nextArgIndex && args[nextArgIndex].equalsIgnoreCase("-force")) {
                doForce = true; nextArgIndex++;
            }
            if (args.length > nextArgIndex && args[nextArgIndex].equalsIgnoreCase("-gc")) {
                doGc = true; nextArgIndex++;
            }
            if (args.length > nextArgIndex && args[nextArgIndex].equalsIgnoreCase("-rf")) {
                doRunFinalization = true; nextArgIndex++;
            }
            if (args.length > nextArgIndex) {
                gcStep = Integer.parseInt(args[nextArgIndex++]);
            }
        } catch (Exception ex) {
            if (args.length >= 6)
                System.err.println(ex);
            System.out.println("Usage: " + IntensiveMappingWithWritingTest.class.getName()
                + " READ|WRITE tempFileName blockSize blockCount passCount testCount"
                + " [-force] [-gc] [-rf] [gcStep]");
            System.out.println("The file name may contain %MILLIS% substring:"
                + " it will be replaced with the current time in milliseconds");
            return;
        }
        long fileLength = (long)numberOfBlocks * (long)blockSize;
        System.out.println("Mapping the file " + f.getAbsolutePath());
        System.out.println("It is " + (f.exists() ? "an existing file" : "a new file"));
        System.out.println("Block size: " + blockSize + " bytes");
        System.out.println("File size: " + numberOfBlocks + " blocks, " + fileLength + " bytes");
        System.out.println("Number of passes: " + numberOfPasses);
        System.out.println("Number of tests: " + numberOfTests);
        System.out.println(doForce ? "MappedByteBuffer.force() method will be called for any mapping" :
            "MappedByteBuffer.force() method will not be used");
        System.out.println("System.gc() will "
            + (doGc ? "be called before each test"
            + (gcStep > 0 ? " and after every " + gcStep + " blocks" : "")
            : "not be called"));
        System.out.println("System.runFinalization() will "
            + (doRunFinalization ? "be called before each test"
            + (gcStep > 0 ? " and after every " + gcStep + " blocks" : "")
            : "not be called"));
        System.out.println();
        if (!f.exists()) {
            f.createNewFile();
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.setLength(fileLength);
        raf.close();

        for (int testCount = 1; testCount <= numberOfTests; testCount++) {
            System.out.println("Test #" + testCount + " / " + numberOfTests
                + " (" + Runtime.getRuntime().freeMemory() / 1000000.0 + "MB free, "
                + Runtime.getRuntime().totalMemory() / 1000000.0 + "MB total, "
                + Runtime.getRuntime().maxMemory() / 1000000.0 + "MB max)");

            if (doRunFinalization) {
                System.out.print("System.runFinalization()...");
                System.runFinalization();
                System.out.println(" done");
            }
            if (doGc) {
                System.out.print("System.gc()...");
                System.gc();
                System.out.println(" done");
            }
            raf = new RandomAccessFile(f, "rw");
            ByteBuffer pattern = ByteBuffer.allocateDirect(blockSize);
            long patternSum = 0;
            for (int j = 0; j < pattern.limit(); j++) {
                patternSum += (byte)j; pattern.put(j, (byte)j);
            }
            for (int pass = 1; pass <= numberOfPasses; pass++) {
                System.out.print("Pass #" + pass);
                long sum = 0;
                for (int i = 0; i < numberOfBlocks; i++) {
                    MappedByteBuffer mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE,
                        (long)i * (long)blockSize, blockSize);
                    System.out.print(".");
                    if (readOnly) {
                        for (int j = 0, n = mbb.limit(); j < n; j++) {
                            sum += mbb.get(j);
                        }
                    } else {
                        pattern.rewind();
                        mbb.put(pattern);
                        sum += patternSum;
                    }
                    if (doForce) {
                        mbb.force();
                    }
                    if (gcStep > 0 && i % gcStep == 0) {
                        if (doRunFinalization) {
                            System.runFinalization();
                            System.out.print("&");
                        }
                        if (doGc) {
                            System.gc();
                            System.out.print("*");
                        }
                    }
                }
                System.out.println(" done: sum = " + sum);
            }
            raf.close();
        }
    }
}

/*
*******************************
**** Execution example #1: ****
"D:\Program Files\Java\jdk1.6.0\jre\bin\java" net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest \TMP\test.dat 8192 4096 1 50 >log1.txt 2>&1

Mapping the file D:\TMP\test.dat
It is a new file
Block size: 8192 bytes
File size: 4096 blocks, 33554432 bytes
Number of passes: 1
Number of tests: 50
System.gc() will not be called
System.runFinalization() will not be called

Test #1 / 50 (4.974368MB free, 5.177344MB total, 66.650112MB max):
Pass #1... done
Test #2 / 50 (4.529136MB free, 5.177344MB total, 66.650112MB max):
Pass #1...java.lang.Error: Cleaner terminated abnormally
     at sun.misc.Cleaner$1.run(Cleaner.java:130)
     at java.security.AccessController.doPrivileged(Native Method)
     at sun.misc.Cleaner.clean(Cleaner.java:127)
     at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:124)
Caused by: java.io.IOException: The process cannot access the file because another process has locked a portion of the file
     at sun.nio.ch.FileChannelImpl.unmap0(Native Method)
     at sun.nio.ch.FileChannelImpl.access$100(FileChannelImpl.java:32)
     at sun.nio.ch.FileChannelImpl$Unmapper.run(FileChannelImpl.java:680)
     at sun.misc.Cleaner.clean(Cleaner.java:125)
     ... 1 more
*******************************


*******************************
**** Execution example #2: ****
"D:\Program Files\Java\jdk1.6.0\jre\bin\java" -Xmx500m net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest \TMP\test.dat 8192 4096 1 50 -gc >log2.txt 2>&1

Mapping the file D:\TMP\test.dat
It is a new file
Block size: 8192 bytes
File size: 4096 blocks, 33554432 bytes
Number of passes: 1
Number of tests: 50
System.gc() will be called before each test
System.runFinalization() will not be called

Test #1 / 50 (4.974368MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
Pass #1... done
Test #2 / 50 (4.302568MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
Pass #1... done
Test #3 / 50 (3.814008MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
Pass #1... done
Test #4 / 50 (3.346624MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
Pass #1... done
Test #5 / 50 (2.921576MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
Pass #1... done
Test #6 / 50 (2.428136MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
Pass #1...java.lang.Error: Cleaner terminated abnormally
     at sun.misc.Cleaner$1.run(Cleaner.java:130)
     at java.security.AccessController.doPrivileged(Native Method)
     at sun.misc.Cleaner.clean(Cleaner.java:127)
     at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:124)
Caused by: java.io.IOException: The process cannot access the file because another process has locked a portion of the file
     at sun.nio.ch.FileChannelImpl.unmap0(Native Method)
     at sun.nio.ch.FileChannelImpl.access$100(FileChannelImpl.java:32)
     at sun.nio.ch.FileChannelImpl$Unmapper.run(FileChannelImpl.java:680)
     at sun.misc.Cleaner.clean(Cleaner.java:125)
     ... 1 more
*******************************

*******************************
**** Execution example #3: ****
"D:\Program Files\Java\jdk1.6.0\jre\bin\java" -Xmx500m net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest \TMP\test.dat 8192 4096 1 50 -gc -runFinalization >log3.txt 2>&1

Mapping the file D:\TMP\test.dat
It is a new file
Block size: 8192 bytes
File size: 4096 blocks, 33554432 bytes
Number of passes: 1
Number of tests: 50
System.gc() will be called before each test
System.runFinalization() will be called before each test

Test #1 / 50 (4.974368MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #2 / 50 (4.304016MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #3 / 50 (3.813MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #4 / 50 (3.345616MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #5 / 50 (2.90988MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #6 / 50 (2.41676MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1...java.lang.Error: Cleaner terminated abnormally
     at sun.misc.Cleaner$1.run(Cleaner.java:130)
     at java.security.AccessController.doPrivileged(Native Method)
     at sun.misc.Cleaner.clean(Cleaner.java:127)
     at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:124)
Caused by: java.io.IOException: The process cannot access the file because another process has locked a portion of the file
     at sun.nio.ch.FileChannelImpl.unmap0(Native Method)
     at sun.nio.ch.FileChannelImpl.access$100(FileChannelImpl.java:32)
     at sun.nio.ch.FileChannelImpl$Unmapper.run(FileChannelImpl.java:680)
     at sun.misc.Cleaner.clean(Cleaner.java:125)
     ... 1 more
*******************************

*******************************
**** Execution example #4: ****
"D:\Program Files\Java\jdk1.6.0\jre\bin\java" -Xmx500m net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest \TMP\test.dat 8192 4096 1 50 -gc -runFinalization >log4.txt 2>&1

Mapping the file D:\TMP\test.dat
It is an existing file
Block size: 8192 bytes
File size: 4096 blocks, 33554432 bytes
Number of passes: 1
Number of tests: 50
System.gc() will be called before each test
System.runFinalization() will be called before each test

Test #1 / 50 (4.974368MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #2 / 50 (4.304016MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #3 / 50 (3.813MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #4 / 50 (3.345616MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #5 / 50 (2.955408MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #6 / 50 (2.462288MB free, 5.177344MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #7 / 50 (3.02956MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #8 / 50 (2.98252MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #9 / 50 (3.004432MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #10 / 50 (3.00436MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #11 / 50 (3.027712MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #12 / 50 (3.02956MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #13 / 50 (2.96116MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #14 / 50 (2.97916MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #15 / 50 (3.00352MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #16 / 50 (3.0208MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #17 / 50 (3.02956MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #18 / 50 (3.002392MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #19 / 50 (3.008512MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #20 / 50 (3.0088MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #21 / 50 (3.047872MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #22 / 50 (2.980432MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #23 / 50 (2.95996MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #24 / 50 (3.007MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #25 / 50 (2.936728MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #26 / 50 (2.999512MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #27 / 50 (3.02956MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #28 / 50 (2.979832MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #29 / 50 (2.999272MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #30 / 50 (2.98336MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #31 / 50 (2.96224MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #32 / 50 (2.98228MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #33 / 50 (3.022192MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #34 / 50 (3.023872MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #35 / 50 (3.047872MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()... done
Pass #1... done
Test #36 / 50 (2.9578MB free, 5.865472MB total, 520.290304MB max):
System.gc()... done
System.runFinalization()...Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
     at java.lang.Thread.start0(Native Method)
     at java.lang.Thread.start(Thread.java:597)
     at java.lang.ref.Finalizer$1.run(Finalizer.java:113)
     at java.security.AccessController.doPrivileged(Native Method)
     at java.lang.ref.Finalizer.forkSecondaryFinalizer(Finalizer.java:121)
     at java.lang.ref.Finalizer.runFinalization(Finalizer.java:126)
     at java.lang.Runtime.runFinalization0(Native Method)
     at java.lang.Runtime.runFinalization(Runtime.java:688)
     at java.lang.System.runFinalization(System.java:950)
     at net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest.main(IntensiveMapping.java:62)
*****************************

*******************************
**** Execution example #5: ****
"D:\Program Files\Java\jdk1.6.0\jre\bin\java" -Xmx500m net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest \TMP\test.dat 8192 4096 1 50 -runFinalization >log5.txt 2>&1
Mapping the file D:\TMP\test.dat
It is an existing file
Block size: 8192 bytes
File size: 4096 blocks, 33554432 bytes
Number of passes: 1
Number of tests: 50
System.gc() will not be called
System.runFinalization() will be called before each test

Test #1 / 50 (4.974368MB free, 5.177344MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #2 / 50 (4.52928MB free, 5.177344MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #3 / 50 (3.80992MB free, 5.177344MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #4 / 50 (3.390272MB free, 5.177344MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #5 / 50 (2.959248MB free, 5.177344MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #6 / 50 (2.519128MB free, 5.177344MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #7 / 50 (2.907928MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #8 / 50 (2.924704MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #9 / 50 (2.979688MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #10 / 50 (2.942968MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #11 / 50 (2.952424MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #12 / 50 (2.979688MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()... done
Pass #1... done
Test #13 / 50 (2.936968MB free, 5.81632MB total, 520.290304MB max):
System.runFinalization()...Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
     at java.lang.Thread.start0(Native Method)
     at java.lang.Thread.start(Thread.java:597)
     at java.lang.ref.Finalizer$1.run(Finalizer.java:113)
     at java.security.AccessController.doPrivileged(Native Method)
     at java.lang.ref.Finalizer.forkSecondaryFinalizer(Finalizer.java:121)
     at java.lang.ref.Finalizer.runFinalization(Finalizer.java:126)
     at java.lang.Runtime.runFinalization0(Native Method)
     at java.lang.Runtime.runFinalization(Runtime.java:688)
     at java.lang.System.runFinalization(System.java:950)
     at net.algart.arrays.demo.jre.IntensiveMappingWithWritingTest.main(IntensiveMapping.java:62)
*******************************

*/
