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

package net.algart.arrays.demo;

import net.algart.arrays.*;
import net.algart.contexts.DefaultArrayContext;
import net.algart.contexts.DefaultContext;

import java.io.File;
import java.util.Locale;

/**
 * <p>Simple test that creates one zero-filled file via AlgART arrays.</p>
 *
 * @author Daniel Alievsky
 */
public class CreateEmptyFileDemo {
    public static void main(String[] args) {
        int startArgIndex = 0;
        boolean
            noFlush = false,
            twiceFlush = false,
            forceWriting = false,
            freeResources = false,
            quickenFreeResources = false,
            fullGcAtEnd = false,
            nonZero = false,
            resizable = false;
        int dfmMode;
        if (startArgIndex < args.length && (args[startArgIndex].equalsIgnoreCase("-map")
            || args[startArgIndex].equalsIgnoreCase("-stdIO")))
        {
            dfmMode = args[startArgIndex].equalsIgnoreCase("-map") ? 1 : 2;
            startArgIndex++;
        } else {
            dfmMode = LargeMemoryModel.getInstance().getDataFileModel() instanceof StandardIODataFileModel ? 2 : 1;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noFlush")) {
            noFlush = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-twiceFlush")) {
            twiceFlush = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-force")) {
            forceWriting = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-free")) {
            freeResources = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-quickenFree")) {
            quickenFreeResources = freeResources = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-fullGcAtEnd")) {
            fullGcAtEnd = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-nonZero")) {
            nonZero = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-resizable")) {
            resizable = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.println("Usage: " + CreateEmptyFileDemo.class.getName()
                + " [-map|-stdio] [-noFlush|-twiceFlush] [-force] [-free|-quickenFree] [-fullGcAtEnd]"
                + " [-nonZero] [-resizable] fileLength [destFile]");
            System.out.println("If destFile is omitted, the file will be created in the system temp directory "
                + "and will be immediately removed.");
            System.out.println("\"-map\" and \"-stdIO\" key chooses scheme of data file operations "
                + "(if not specified, default schemed is used).");
            System.out.println("\"-noFlush\" key disables flushing buffers (correct only together with \"-free\".");
            System.out.println("\"-twiceFlush\" key requires flushing buffers twice.");
            System.out.println("\"-force\" key forces immediate physical writing data (ignored if \"-noFlush\").");
            System.out.println("\"-free\" key requires to call freeResource after all operations "
                + "(useless in this test, but sometimes necessary in real applications).");
            System.out.println("\"-quickenFree\" key requires to call freeResource only for subArray(0,0).");
            System.out.println("\"-fullGcAtEnd\" key requires full garbage collection before finish.");
            System.out.println("\"-resizable\" key lead to progressive increasing file length.");
            return;
        }
        DemoUtils.initializeClass();
        long fileLength = Long.parseLong(args[startArgIndex]);
        boolean standardTemp = args.length < startArgIndex + 2;
        DataFileModel<File> dfm = null;
        if (standardTemp) {
            if (dfmMode == 1) dfm = new DefaultDataFileModel(new File("./"), 0, DefaultDataFileModel.defaultLazyWriting());
            else if (dfmMode == 2) dfm = new StandardIODataFileModel(new File("./"), 0, true, StandardIODataFileModel.defaultDirectBuffers());
        } else {
            final File fDest = new File(args[startArgIndex + 1]);
            if (dfmMode == 1) {
                dfm = new DefaultDataFileModel(fDest, 0, DefaultDataFileModel.defaultLazyWriting());
            } else {
                dfm = new StandardIODataFileModel(fDest, 0, false, false);
            }
        }

        LargeMemoryModel<File> lmm = dfm == null ? LargeMemoryModel.getInstance() : LargeMemoryModel.getInstance(dfm);
        System.out.println(lmm);
        System.out.println();
        System.out.printf(Locale.US, "Creating new array in a temporary file, %,d bytes...%n", fileLength);
        long t1 = System.nanoTime();
        UpdatableByteArray dest = resizable ?
            lmm.newEmptyByteArray() :
            lmm.newUnresizableByteArray(fileLength);
        if (!standardTemp)
            LargeMemoryModel.setTemporary(dest, false);
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Array created (%.5f seconds):%n    %s%n", (t2 - t1) * 1e-9, dest);

        final DefaultArrayContext ac = new DefaultArrayContext(new DefaultContext() {
            @Override
            public void updateStatus(String message, boolean force) {
                System.out.print("\r" + message + "    \r");
            }
        });
        t1 = System.nanoTime();
        if (nonZero) {
            for (long p = 0; p < fileLength; p += 1024 * 1024) {
                long size = Math.min(1024 * 1024, fileLength - p);
                if (resizable) {
                    ((MutableArray) dest).length(p + size);
                }
                dest.subArr(p, size).fill((byte) 66);
                System.out.printf(Locale.US, "\r%d MB ready...\r", (p + size) / (1024 * 1024));
            }
        } else {
            if (resizable) {
                ((MutableArray) dest).length(fileLength);
            }
        }
        t2 = System.nanoTime();
        if (nonZero) {
            System.out.printf(Locale.US, "Array filled (%.5f seconds, %.5f MB/sec)%n",
                (t2 - t1) * 1e-9, Arrays.sizeOf(dest) / 1048576.0 / ((t2 - t1) * 1e-9));
        }

        if (!noFlush) {
            for (int k = 0; k < (twiceFlush ? 2 : 1); k++) {
                System.out.println("Flushing buffers...");
                t1 = System.nanoTime();
                dest.flushResources(ac, forceWriting);
                t2 = System.nanoTime();
                System.out.printf(Locale.US, "flushResources method performed " + (forceWriting ? "in force mode " : "")
                    + "(%.5f seconds, %.5f MB/sec)%n",
                    (t2 - t1) * 1e-9, Arrays.sizeOf(dest) / 1048576.0 / ((t2 - t1) * 1e-9));
            }
        }
        if (freeResources) {
            System.out.println("Freeing resources...");
            t1 = System.nanoTime();
            if (quickenFreeResources) {
                dest.subArray(0, 0).freeResources(ac, forceWriting);
            } else {
                dest.freeResources(ac, forceWriting);
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "freeResources method performed in %.5f seconds%n", (t2 - t1) * 1e-9);
        }
        if (fullGcAtEnd) {
            dest = null;
            DemoUtils.fullGC();
        }
    }
}
