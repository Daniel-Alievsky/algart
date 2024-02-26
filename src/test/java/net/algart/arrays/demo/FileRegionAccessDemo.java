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

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>The test of accessing the region of file via AlgART array.</p>
 *
 * @author Daniel Alievsky
 */
public class FileRegionAccessDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        int startArgIndex = 0;
        boolean doWrite = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-write")) {
            doWrite = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.println("Usage: " + FileRegionAccessDemo.class.getName()
                + " [-write] file regionStart regionSize");
            System.out.println("You may call this test with FINER logging level "
                + "to check what bytes are really read/written.");
            return;
        }

        File file = new File(args[startArgIndex]);
        long filePosition = Long.parseLong(args[startArgIndex + 1]);
        long fileAreaSize = Long.parseLong(args[startArgIndex + 2]);

        LargeMemoryModel<File> lmm = LargeMemoryModel.getInstance();
        System.out.printf(Locale.US, lmm + "%n%n");

        Array a = doWrite ?
            lmm.asUpdatableByteArray(file, filePosition, fileAreaSize, false, ByteOrder.LITTLE_ENDIAN) :
            lmm.asByteArray(file, filePosition, fileAreaSize, ByteOrder.LITTLE_ENDIAN);
        System.out.printf(Locale.US, "Array mapped to the file:%n    %s%n", a);
        byte[] data = new byte[32];
        long t1 = System.nanoTime();
        a.getData(0, data);
        long t2 = System.nanoTime();
        System.out.println("First 32 bytes at position " + filePosition + ":");
        System.out.println("    " + Arrays.toHexString(SimpleMemoryModel.asUpdatableByteArray(data), ", ", 200));
        System.out.printf(Locale.US, "Reading time: %.3f ms%n", (t2 - t1) * 1e-6);
        if (doWrite) {
            t1 = System.nanoTime();
            ((UpdatableArray)a).setData(0, "Hello, world!".getBytes());
            a.flushResources(null, true);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Writing time: %.3f ms%n", (t2 - t1) * 1e-6);
        }
    }
}
