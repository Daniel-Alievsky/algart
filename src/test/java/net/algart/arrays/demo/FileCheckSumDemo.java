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
import java.util.*;

import net.algart.arrays.*;

/**
 * <p>Simple test that calculates check sum of a file or all files in a directory via AlgART arrays.</p>
 *
 * @author Daniel Alievsky
 */
public class FileCheckSumDemo {
    private static final boolean ACCUMULATE_IN_LIST = true; // should not lead to map failure
    private static final boolean ACCUMULATE_CLONE_IN_LIST = false; // also should not lead to map failure

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("Usage: " + FileCheckSumDemo.class.getName() + " srcFile/srcDir");
            return;
        }

        LargeMemoryModel<File> lmm = LargeMemoryModel.getInstance();
        System.out.printf(Locale.US, lmm + "%n%n");

        File fileOrDir = new File(args[0]);
        File[] files;
        if (fileOrDir.isDirectory()) {
            files = fileOrDir.listFiles();
        } else {
            files = new File[]{fileOrDir};
        }
        int count = 0;
        List<Array> arrayList = new ArrayList<Array>();
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            Array a = lmm.asByteArray(f, 0, LargeMemoryModel.ALL_FILE, ByteOrder.LITTLE_ENDIAN);
            System.out.printf(Locale.US, "Array mapped to the file %s:%n    %s%n", f, a);
            long t1 = System.nanoTime();
            int hash = a.hashCode();
            // For byte array, this hash code is really equal to the standard CRC32
            long t2 = System.nanoTime();
            System.out.printf(Locale.US, "    Hash code calculated in %.5f seconds (%.3f ns/byte):%n    %#x%n",
                (t2 - t1) * 1e-9, (t2 - t1) / (a.length() + 0.0), hash);
            if (ACCUMULATE_IN_LIST) {
                if (ACCUMULATE_CLONE_IN_LIST) {
                    a = a.updatableClone(lmm);
                    System.out.printf(Locale.US, "    Clone: %s%n", a);
                }
                a.freeResources(null); // necessary while adding to a list: without it, the address space will leak
                arrayList.add(a);
            }
            count++;
        }
        System.out.printf("%d files are processed%n", count);
    }
}
