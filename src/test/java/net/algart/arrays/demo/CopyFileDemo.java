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

package net.algart.arrays.demo;

import net.algart.arrays.*;
import net.algart.contexts.DefaultArrayContext;
import net.algart.contexts.DefaultContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * <p>Simple test that copies one file to another via AlgART arrays.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class CopyFileDemo {
    private static void pause(String msg) throws IOException {
        System.out.print(msg);
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int startArgIndex = 0;
        boolean
            forceWriting = false,
            viaRAM = false,
            fullGcAtEnd = false,
            verbose = false,
            preserveTemp = false,
            deleteSrc = false;
        DataFileModel<File> dfm = null;
        if (startArgIndex < args.length && (args[startArgIndex].equalsIgnoreCase("-map")
            || args[startArgIndex].equalsIgnoreCase("-stdIO")))
        {
            dfm = args[startArgIndex].equalsIgnoreCase("-map") ?
                new DefaultDataFileModel() :
                new StandardIODataFileModel(false, false);
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-force")) {
            forceWriting = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-viaRAM")) {
            viaRAM = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-fullGcAtEnd")) {
            fullGcAtEnd = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-preserveTemp")) {
            preserveTemp = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-deleteSrc")) {
            deleteSrc = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-verbose")) {
            verbose = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.println("Usage: " + CopyFileDemo.class.getName()
                + " [-map|-stdio] [-force] [-viaRAM] [-fullGcAtEnd] [-preserveTemp] [-deleteSrc]"
                + " [-verbose] srcFile [destFile [copyPortionSize]]");
            System.out.println("If destFile is omitted, the srcFile will be copied");
            System.out.println("to a temporary file that will be immediately removed"
                + " (if \"-preserveTemp\" is not set).");
            System.out.println("\"-map\" and \"-stdIO\" key chooses scheme of data file operations "
                + "(if not specified, default schemed is used).");
            System.out.println("\"-viaRAM\" key forces copying via usual Java array"
                + "(only if \"copyPortionSize\" is also specified).");
            System.out.println("\"-force\" key forces immediate writing data, "
                + "if \"copyPortionSize\" is specified - after every copied portion.");
            System.out.println("\"-fullGcAtEnd\" key requires full garbage collection before finish.");
            return;
        }
        DemoUtils.initializeClass();

        LargeMemoryModel<File> lmm = dfm == null ? LargeMemoryModel.getInstance() : LargeMemoryModel.getInstance(dfm);
        System.out.println("Current memory model:");
        System.out.println(lmm);
        System.out.println();
        System.out.println("All used data file models:");
        System.out.println(LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion());
        System.out.println();

        File fSrc = new File(args[startArgIndex]);
        long t1 = System.nanoTime();
        boolean temporary = args.length < startArgIndex + 2;
        Integer copyPortionSize = startArgIndex + 2 < args.length ? Integer.valueOf(args[startArgIndex + 2]) : null;
        ByteArray src = lmm.asByteArray(fSrc, 0, LargeMemoryModel.ALL_FILE, ByteOrder.nativeOrder());
        if (deleteSrc) {
            LargeMemoryModel.setTemporary(src, true);
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Source array mapped (%.5f seconds):%n    %s%n    (%s%s) [0x%x]%n",
            (t2 - t1) * 1e-9,
            src, src.byteOrder(),
            LargeMemoryModel.isCreatedReadOnly(src) ? ", strongly read-only" : "",
            System.identityHashCode(src));
        System.out.println("Views of the source array:");
        Array v;
        System.out.printf("    asImmutable:        %s [0x%x]%n",
            v = src.asImmutable(), System.identityHashCode(v));
        System.out.printf("    asTrustedImmutable: %s [0x%x]%n",
            v = src.asTrustedImmutable(), System.identityHashCode(v));
        System.out.printf("    asCopyOnNextWrite:  %s [0x%x]%n",
            v = src.asCopyOnNextWrite(), System.identityHashCode(v));
        System.out.printf("    shallowClone:       %s [0x%x]%n",
            v = src.shallowClone(), System.identityHashCode(v));

        UpdatableByteArray dest;
        if (!temporary) {
            File fDest = new File(args[startArgIndex + 1]);
            t1 = System.nanoTime();
            dest = lmm.asUpdatableByteArray(fDest, 0, fSrc.length(), true, ByteOrder.nativeOrder());
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Target array mapped (%.5f seconds):%n    %s%n    (%s%s)%n", (t2 - t1) * 1e-9,
                dest, dest.byteOrder(),
                LargeMemoryModel.isCreatedReadOnly(dest.asImmutable()) ? ", strongly read-only" : "");
            // - here asImmutable should have no effect!
            System.out.println("Copying " + fSrc + " to " + fDest + ", " + src.length() + " bytes...");
        } else {
            System.out.println("Creating new array in a temporary file, " + src.length() + " bytes...");
            t1 = System.nanoTime();
            dest = lmm.newUnresizableByteArray(src.length());
            if (preserveTemp) {
                LargeMemoryModel.setTemporary(dest, false);
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Array created (%.5f seconds):%n    %s%n    (%s%s)%n", (t2 - t1) * 1e-9,
                dest, dest.byteOrder(),
                LargeMemoryModel.isCreatedReadOnly(dest.asImmutable()) ? ", strongly read-only" : "");
            // - here asImmutable should have no effect!
            System.out.println("Copying " + fSrc + " to temporary file " +
                lmm.getDataFilePath(dest) + ", " + src.length() + " bytes...");
        }
        final DefaultArrayContext ac = new DefaultArrayContext(new DefaultContext() {
            @Override
            public void updateStatus(String message, boolean force) {
                System.out.print("\r" + message + "    \r");
            }
        });
        t1 = System.nanoTime();
        long tRead = 0, tWrite = 0;
        if (copyPortionSize == null) {
            Arrays.copy(ac, dest, src);
        } else {
            if (copyPortionSize <= 0)
                throw new IllegalArgumentException("copyPortionSize must be positive");
            Object buffer = viaRAM ? src.newJavaArray(copyPortionSize) : null;
            for (long p = 0, lastP = 0; p < src.length(); p += copyPortionSize) {
                int size = (int) Math.min(copyPortionSize, src.length() - p);
                UpdatableArray destPortion = dest.subArr(p, size);
                Array srcPortion = src.subArr(p, size);
                if (viaRAM) {
                    long tt1 = System.nanoTime();
                    src.getData(p, buffer, 0, size);
                    long tt2 = System.nanoTime();
                    dest.setData(p, buffer, 0, size);
                    long tt3 = System.nanoTime();
                    tRead += tt2 - tt1;
                    tWrite += tt3 - tt2;
                    if (p - lastP > 32 * 1048576 || p + size == src.length()) {
                        System.out.printf(Locale.US, "\r%.1f%%, %d MB ready "
                            + "(%.3f MB/sec summary, %.3f MB/sec read, %.3f MB/sec write)...        \r",
                            (p + size) * 100.0 / src.length(),
                            (p + size) / 1048576,
                            (p + size) / 1048576.0 / ((tt3 - t1) * 1e-9),
                            (p + size) / 1048576.0 / (tRead * 1e-9),
                            (p + size) / 1048576.0 / (tWrite * 1e-9));
                        lastP = p;
                    }
                } else {
                    destPortion.copy(srcPortion);
                    if (p - lastP > 32 * 1048576 || p + size == src.length()) {
                        System.out.printf(Locale.US, "\r%.1f%%, %d MB ready (%.3f MB/sec)...     \r",
                            (p + size) * 100.0 / src.length(),
                            (p + size) / 1048576,
                            (p + size) / 1048576.0 / ((System.nanoTime() - t1) * 1e-9));
                        lastP = p;
                    }
                }
                if (forceWriting) {
                    destPortion.flushResources(null, true);
                }
            }
            System.out.println();
        }
        if (!temporary) {
            if (verbose) {
                pause("Press ENTER: all buffers will be flushed");
            }
            System.out.println("Flushing buffers" + (forceWriting ? " (force mode)..." : "..."));
            dest.flushResources(ac, forceWriting);
            if (verbose) {
                pause("Buffers flushed: press ENTER to continue");
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Done: %.5f MB/sec (%.5f sec)%n%n",
            Arrays.sizeOf(src) / 1048576.0 / ((t2 - t1) * 1e-9), (t2 - t1) * 1e-9);
        System.out.println("All used data file models: " + LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion());

//        Arrays.freeAllResources();
        if (fullGcAtEnd) {
            if (verbose) {
                pause("Press ENTER: all temporary resources will be deleted while garbage collection");
            }
            src = null;
            dest = null;
            lmm = null;
            dfm = null;
            t1 = System.nanoTime();
            Arrays.gcAndAwaitFinalization(15000);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "All Java garbage is collected (%.5f seconds)%n", (t2 - t1) * 1e-9);
            if (verbose) {
                pause("Press ENTER to exit");
            }
        }
        System.out.println("All used data file models: " + LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion());
    }
}
