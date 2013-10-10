package net.algart.arrays.demo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;

import net.algart.arrays.*;

/**
 * <p>Simple test that calculates check sum of a file or all files in a directory via AlgART arrays.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
