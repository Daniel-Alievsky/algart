/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.lang.reflect.Field;
import java.io.*;
import java.util.Locale;

import net.algart.arrays.*;
import net.algart.contexts.*;
import net.algart.math.functions.*;

/**
 * <p>The test illustrating performance of lazy filling of file-based AlgART arrays.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class CreateLazyCopyTest {
    private static void pause() {
        System.out.print("Press ENTER to continue...");
        try {
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException ex) {
            // nothing to do
        }
    }

    public static void main(String[] args) {
        int startArgIndex = 0;
        boolean unresizable = false, noRandomAccess = false, flushAfterCreating = false, flushAfterAccess = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-unresizable")) {
            unresizable = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noRandomAccess")) {
            noRandomAccess = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-flushAfterCreating")) {
            flushAfterCreating = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-flushAfterAccess")) {
            flushAfterAccess = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 4) {
            System.out.println("Usage: " + CreateLazyCopyTest.class.getName()
                + " [-unresizable] [-noRandomAccess] [-flushAfterCreating] [-flushAfterAccess]"
                + " primitiveType arrayLength initFiller lazyCopyLength [flushStart flushEnd]");
            System.out.println("initFiller are applicable for LARGE memory model only;");
            System.out.println("in this case the package-protected constants are changed via reflection.");
            System.out.println("lazyCopyLength works non-trivially in LARGE memory model only;");
            System.out.println("negative values will lead to skipping this test.");
            System.out.println("Please compare flushing speed with and without -noRandomAccess flag:");
            System.out.println("random access slows down further flushing in times!");
            return;
        }

        MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        String elementTypeName = args[startArgIndex];
        long len = Long.parseLong(args[startArgIndex + 1]);
        byte zeroFiller = Byte.parseByte(args[startArgIndex + 2]);
        long lazyCopyLength = Long.parseLong(args[startArgIndex + 3]);
        try {
            Class<?> c = Class.forName("net.algart.arrays.DataStorage");
            Field f = c.getDeclaredField("ZERO_INIT_FILLER");
            f.setAccessible(true);
            f.setByte(null, zeroFiller);
            System.out.println("Non-standard filler " + zeroFiller + " for initialization will be used");
        } catch (Exception ex) {
            throw new AssertionError("Cannot set net.algart.arrays.DataStorage.ZERO_INIT_FILLER: " + ex);
        }
        long flushStart = 0, flushEnd = len;
        if (startArgIndex + 5 < args.length) {
            flushStart = Long.parseLong(args[startArgIndex + 4]);
            flushEnd = Long.parseLong(args[startArgIndex + 5]);
        }

        Class<?> clazz =
            elementTypeName.equals("boolean") ? boolean.class :
            elementTypeName.equals("char") ? char.class :
            elementTypeName.equals("byte") ? byte.class :
            elementTypeName.equals("short") ? short.class :
            elementTypeName.equals("int") ? int.class :
            elementTypeName.equals("long") ? long.class :
            elementTypeName.equals("float") ? float.class :
            elementTypeName.equals("double") ? double.class :
            null;
        if (clazz == null)
            throw new IllegalArgumentException("Unknown element type: " + elementTypeName);
        System.out.println(mm);
        System.out.println();
        final ArrayContext ac = new DefaultArrayContext(new DefaultContext() {
            @Override public void updateStatus(String message, boolean force) {
                System.out.print("\r" + message + "    \r");
            }
        });

        long t1 = System.nanoTime();
        UpdatablePArray a;
        if (lazyCopyLength < 0) {
            if (unresizable) {
                a = (UpdatablePArray)mm.newUnresizableArray(clazz, len);
            } else {
                a = (UpdatablePArray)mm.newEmptyArray(clazz).length(len);
            }
        } else {
            PArray pattern = Arrays.asIndexFuncArray(ConstantFunc.getInstance(157),
                Arrays.type(PArray.class, clazz), lazyCopyLength);
            if (unresizable) {
                if (len != lazyCopyLength)
                    throw new IllegalArgumentException("lazyCopyLength must be equal to arrayLength for unresizable");
                a = (UpdatablePArray)mm.newUnresizableLazyCopy(pattern);
            } else {
                a = (UpdatablePArray)mm.newLazyCopy(pattern).length(len);
            }
        }
        long t2 = System.nanoTime();
        System.out.println("Array created: " + a);
        System.out.printf(Locale.US, "Creating array time: %.5f ms, %.3f ns/element, %.3f MB/sec%n",
            (t2 - t1) * 1e-6, (t2 - t1) / (double)len, Arrays.sizeOf(a) / 1048576.0 / ((t2 - t1) * 1e-9));
        pause();

        t1 = System.nanoTime();
        double sum1 = Arrays.sumOf(a);
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Arrays.sumOf time: %.5f ms, %.3f ns/element, %.3f MB/sec (sum = " + sum1 + ")%n",
            (t2 - t1) * 1e-6, (t2 - t1) / (double)len, Arrays.sizeOf(a) / 1048576.0 / ((t2 - t1) * 1e-9));
        pause();

        if (flushAfterCreating) {
            t1 = System.nanoTime();
            a.subArray(flushStart, flushEnd).flushResources(ac, true);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "flushResources time: %.5f ms, %.3f ns/element, %.3f MB/sec%n",
                (t2 - t1) * 1e-6, (t2 - t1) / (double)len, Arrays.sizeOf(a) / 1048576.0 / ((t2 - t1) * 1e-9));
            pause();
            // this flushing fills the file from 0 to flushEnd:
            // because the bit map was not used yet, flushResources uses the simplest
            // and most efficient actualization strategy
        }

        if (!noRandomAccess && len > 0) {
            for (long pos : new long[] {0, 0, len / 2, len / 2, len - 1, len - 1}) {
                t1 = System.nanoTime();
                Object e = a.getElement(pos);
                t2 = System.nanoTime();
                System.out.println("Element #" + pos + ": " + e);
                System.out.printf(Locale.US, "Access time: %.5f ms%n", (t2 - t1) * 1e-6);
                pause();
            }
        }

        if (flushAfterAccess) {
            t1 = System.nanoTime();
            UpdatablePArray funcArray = Arrays.asUpdatableFuncArray(Func.UPDATABLE_IDENTITY, a.updatableType(), a);
            // - for more complex testing: should lead to flushing correct part of the source array
            UpdatablePArray subArray = funcArray.subArray(flushStart, flushEnd);
            subArray.flushResources(ac, true);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "flushResources time: %.5f ms, %.3f ns/element, %.3f MB/sec%n",
                (t2 - t1) * 1e-6, (t2 - t1) / (double)len, Arrays.sizeOf(subArray) / 1048576.0 / ((t2 - t1) * 1e-9));
            pause();
            // this flushing fills the file from flushStart to flushEnd
        }

        t1 = System.nanoTime();
        double sum2 = Arrays.sumOf(a);
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Arrays.sumOf time: %.5f ms, %.3f ns/element, %.3f MB/sec (sum = " + sum2 + ")%n",
            (t2 - t1) * 1e-6, (t2 - t1) / (double)len, Arrays.sizeOf(a) / 1048576.0 / ((t2 - t1) * 1e-9));
        pause();

        t1 = System.nanoTime();
        a.freeResources(ac);
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "freeResources time: %.5f ms, %.3f ns/element, %.3f MB/sec%n",
            (t2 - t1) * 1e-6, (t2 - t1) / (double)len, Arrays.sizeOf(a) / 1048576.0 / ((t2 - t1) * 1e-9));
        pause();
        try {
            Arrays.gcAndAwaitFinalization(15000);
        } catch (InterruptedException ex) {
            // nothing to do here
        }
    }
}
