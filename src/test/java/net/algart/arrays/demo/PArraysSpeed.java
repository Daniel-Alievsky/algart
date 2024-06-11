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

import java.nio.*;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>Speed of primitive arrays: common speed test</p>
 *
 * @author Daniel Alievsky
 */
public class PArraysSpeed {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();

    private static long getLongTest(long[] array, long index) {
        if (index < 0 || index > array.length) {
            throw new IllegalArgumentException("Illegal index");
        }
        return array[(int) index];
    }

    private static int getIntTest(int[] array, long index) {
        if (index < 0 || index > array.length) {
            throw new IllegalArgumentException("Illegal index");
        }
        return array[(int) index];
    }

    private static int getBooleanTest(boolean[] array, int index) {
        if (index < 0 || index > array.length) {
            throw new IllegalArgumentException("Illegal index");
        }
        return array[index] ? 1 : 0;
    }

    private static void myFill(UpdatablePArray a, int value) {
        for (long k = 0, n = a.length(); k < n; k++) {
            a.setInt(k, value);
        }
    }

    private static int mySumLong(LongArray a) {
        long result = 0;
        for (long k = 0, n = a.length(); k < n; k++) {
            result += a.getLong(k);
        }
        return (int) result;
    }

    private static int mySumInt(IntArray a) {
        int result = 0;
        for (long k = 0, n = a.length(); k < n; k++) {
            result += a.getInt(k);
        }
        return result;
    }

    private static int mySumBit(BitArray a) {
        int result = 0;
        for (long k = 0, n = a.length(); k < n; k++) {
            result += a.getInt(k);
        }
        return result;
    }

    private static int mySum(PArray a) {
        // this method improves HotSpot inlining
        if (a instanceof LongArray) {
            return mySumLong((LongArray) a);
        }
        if (a instanceof IntArray) {
            return mySumInt((IntArray) a);
        } else {
            return mySumBit((BitArray) a);
        }
    }

    private static int mySumBuffered(PArray a) {
        int result = 0;
        DataBuffer buf = a.buffer();
        for (long p = 0, n = a.length(); p < n; p += buf.count()) {
            buf.map(p);
            result += mySumOfArray(buf.data(), buf.from(), buf.cnt(), a instanceof BitArray);
        }
        return result;
    }

    private static int mySumUnevenlyBuffered(PArray a) {
        int result = 0;
        DataBuffer buf = a.buffer();
        long cap = buf.capacity();
        for (long p = 0, n = a.length(); p < n; p += buf.count()) {
            long len = Math.max(cap / 8, n % (cap / 4)); // random block len
            buf.map(p, len);
            result += mySumOfArray(buf.data(), buf.from(), buf.cnt(), a instanceof BitArray);
        }
        return result;
    }

    private static int mySumBlock(PArray a) {
        int bufSize = (int) a.buffer().capacity(); // in this test we are sure that it will not exceed 2^31-1
        if (a instanceof LongArray) {
            long result = 0;
            long[] buf = new long[(int) Math.min(bufSize, a.length())];
            for (long p = 0, n = a.length(); p < n; p += buf.length) {
                a.getData(p, buf);
                result += mySumOfArray(buf, 0, (int) Math.min(buf.length, n - p), false);
            }
            return (int) result;
        } else if (a instanceof IntArray) {
            int result = 0;
            int[] buf = new int[(int) Math.min(bufSize, a.length())];
            for (long p = 0, n = a.length(); p < n; p += buf.length) {
                a.getData(p, buf);
                result += mySumOfArray(buf, 0, (int) Math.min(buf.length, n - p), false);
            }
            return result;
        } else {
            int result = 0;
            long[] buf = new long[(int) PackedBitArrays.packedLength(Math.min(bufSize, a.length()))];
            for (long p = 0, n = a.length(); p < n; p += bufSize) {
                int len = (int) Math.min(bufSize, n - p);
                ((BitArray) a).getBits(p, buf, 0, len);
                result += mySumOfArray(buf, 0, len, true);
            }
            return result;
        }
    }

    private static int mySumOfArray(Object array, long offset, long count, boolean isPackedBit) {
        if (isPackedBit) {
            long[] a = (long[]) array;
            return (int) PackedBitArrays.cardinality(a, offset, offset + count);
        } else if (array instanceof long[]) {
            long result = 0;
            long[] a = (long[]) array;
            for (int k = (int) offset, kMax = k + (int) count; k < kMax; k++) {
                result += a[k];
            }
            return (int) result;
        } else if (array instanceof int[]) {
            int result = 0;
            int[] a = (int[]) array;
            for (int k = (int) offset, kMax = k + (int) count; k < kMax; k++) {
                result += a[k];
            }
            return result;
        } else if (array instanceof boolean[]) {
            int result = 0;
            boolean[] a = (boolean[]) array;
            for (int k = (int) offset, kMax = k + (int) count; k < kMax; k++) {
                if (a[k]) {
                    result++;
                }
            }
            return result;
        }
        throw new AssertionError("Unsupported java array type");
    }

    private static int mySumOfBuffer(Buffer buffer, long offset, long count, boolean isPackedBit) {
        if (isPackedBit) {
            LongBuffer lb = (LongBuffer) buffer;
            return (int) PackedBitBuffers.cardinality(lb, offset, offset + count);
        } else if (buffer instanceof LongBuffer) {
            long result = 0;
            LongBuffer lb = (LongBuffer) buffer;
            for (int k = (int) offset, kMax = k + (int) count; k < kMax; k++) {
                result += lb.get(k);
            }
            return (int) result;
        } else if (buffer instanceof IntBuffer) {
            int result = 0;
            IntBuffer ib = (IntBuffer) buffer;
            for (int k = (int) offset, kMax = k + (int) count; k < kMax; k++) {
                result += ib.get(k);
            }
            return result;
        }
        throw new AssertionError("Unsupported java.nio.Buffer type");
    }

    static Thread[] threads;

    static class MyThread extends Thread {
        private final Class<?> elementType;
        private long n;
        private final int numberOfPasses;
        private final MutablePFixedArray ma1;
        private final MutablePFixedArray ma2;
        private final UpdatablePFixedArray ma3;
        private final boolean useHeap, useGc;
        private double loopLen;

        public MyThread(
            String name, Class<?> elementType, long n, int numberOfPasses,
            MutablePFixedArray ma1, MutablePFixedArray ma2, UpdatablePFixedArray ma3,
            boolean useHeap, boolean useGc)
        {
            super(name);
            this.elementType = elementType;
            this.n = n;
            this.numberOfPasses = numberOfPasses;
            this.ma1 = ma1;
            this.ma2 = ma2;
            this.ma3 = ma3;
            this.useHeap = useHeap;
            this.useGc = useGc;
        }


        private String time(long t1, long t2) {
            return String.format(Locale.US, " (%d ns, %.2f ns/element)",
                t2 - t1, (t2 - t1) * 1.0 / loopLen);
        }

        private String time(long t1, long t2, long n) {
            return String.format(Locale.US, " (%d ns, %.2f ns/element)",
                t2 - t1, (t2 - t1) * 1.0 / n / numberOfPasses);
        }

        public String toString() {
            return getName();
        }

        public void run() {
            long t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21;
            UpdatablePFixedArray a1 = ma1, a2 = ma2, a3 = ma3;
            PArray aConcat = (PArray) Arrays.asConcatenation(a1.asUnresizable(), a2.asUnresizable());
            Object sourceArray = null;
            if (useHeap && n == (int) n) {
                if (elementType == boolean.class) {
                    sourceArray = new boolean[(int) n];
                } else if (elementType == int.class) {
                    sourceArray = new int[(int) n];
                } else {
                    sourceArray = new long[(int) n];
                }
            }
            this.loopLen = (double) n * (double) numberOfPasses;
            for (int m = 0; m < 6; m++) {
//              System.out.println("Sleeping 0.5 sec...");
//              Thread.sleep(500);

                ma1.freeResources();
                ma2.freeResources();
                ma3.freeResources();
                if (useGc) {
                    System.gc();
                }
                synchronized (threads) {
                    System.out.println("Sub-iteration #" + m + " in " + Thread.currentThread());
                    if (useGc) {
                        System.out.println("System.gc()");
                    }
                    if (m == 3) {
                        System.out.println("Extracting SUBARRAYS");
                        a1 = a1.subArray(ma1.length() / 4, 3 * ma1.length() / 4);
                        a2 = a2.subArray(ma2.length() / 4, 3 * ma2.length() / 4);
                        a3 = a3.subArray(ma3.length() / 4, 3 * ma3.length() / 4);
                        n = a1.length();
                        loopLen = (double) n * (double) numberOfPasses;
                        sourceArray = null;
                        if (useHeap && n == (int) n) {
                            if (elementType == boolean.class) {
                                sourceArray = new boolean[(int) n];
                            } else if (elementType == int.class) {
                                sourceArray = new int[(int) n];
                            } else {
                                sourceArray = new long[(int) n];
                            }
                        }
                        aConcat = (PArray) Arrays.asConcatenation(a1, a2);
                        aConcat = (PArray) aConcat.subArray(aConcat.length() / 4, 3 * aConcat.length() / 4);
                    }

                    System.out.println("1st array:     " + a1);
                    System.out.println("2nd array:     " + a2);
                    System.out.println("3rd array:     " + a3);
                    System.out.println("Concatenation: " + aConcat);
                }
                PArray a1sh = (PArray) Arrays.asShifted(a1.asUnresizable(), -17 * 3 + 4 + m * 17);
                t1 = System.nanoTime();
                int sum1 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum1 += mySum(a1);
                }
                t2 = System.nanoTime();
                int sum2 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum2 += mySum(a2);
                }
                t3 = System.nanoTime();
                double sum3 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum3 += Arrays.sumOf(a3);
                }
                t4 = System.nanoTime();
                int sum11 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum11 += (int) Arrays.preciseSumOf(a1);
                }
                t5 = System.nanoTime();
                int sum21 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum21 += mySumBuffered(a2);
                }
                t6 = System.nanoTime();
                int sum31 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum31 += mySumUnevenlyBuffered(a3);
                }
                t7 = System.nanoTime();
                int sum22 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum22 += mySumBlock(a2);
                }
                t8 = System.nanoTime();
                int sum32 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum32 += mySumBlock(a3);
                }
                t9 = System.nanoTime();
                int sumC1 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sumC1 += mySumBuffered(aConcat);
                }
                t10 = System.nanoTime();
                int sumC2 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sumC2 += mySumBlock(aConcat);
                }
                t11 = System.nanoTime();
                int sum12 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum12 += mySumBuffered(a1sh);
                }
                t12 = System.nanoTime();
                int sum13 = 0;
                for (int q = 0; q < numberOfPasses; q++) {
                    sum13 += mySumBlock(a1sh);
                }
                t13 = System.nanoTime();
                int sum14 = 0;
                int[] ja1Direct = null;
                ByteBuffer bb1Direct = null;
                if (a1 instanceof DirectAccessible && ((DirectAccessible) a1).hasJavaArray()
                    && a1 instanceof IntArray)
                {
                    ja1Direct = (int[]) ((DirectAccessible) a1).javaArray();
                    for (int q = 0; q < numberOfPasses; q++) {
                        sum14 += mySumOfArray(ja1Direct, ((DirectAccessible) a1).javaArrayOffset(),
                            n, false);
                    }
                } else if (BufferMemoryModel.isBufferArray(a1)) {
                    bb1Direct = BufferMemoryModel.getByteBuffer(a1);
                    Buffer buffer = bb1Direct.asIntBuffer();
                    if (elementType == boolean.class || elementType == long.class) {
                        // don't use ?: operator here to avoid a bug in Java 1.5 compiler
                        buffer = bb1Direct.asLongBuffer();
                    }
                    for (int q = 0; q < numberOfPasses; q++) {
                        sum14 += mySumOfBuffer(buffer, BufferMemoryModel.getBufferOffset(a1), n,
                            a1 instanceof BitArray);
                    }
                } else {
                    sum14 = sum1;
                }
                t14 = System.nanoTime();
                int sum15 = 0;
                Object ja1Converted = null;
                try {
                    if (useHeap) {
                        ja1Converted = Arrays.toJavaArray(a1);
                    }
                } catch (TooLargeArrayException ex) {
                }
                if (ja1Converted != null) {
                    for (int q = 0; q < numberOfPasses; q++) {
                        sum15 += mySumOfArray(ja1Converted, 0, (int) n, false);
                    }
                } else {
                    sum15 = sum1;
                }
                t15 = System.nanoTime();
                int sum16 = 0;

                if (ja1Converted != null) {
                    for (int q = 0; q < numberOfPasses; q++) {
                        if (elementType == boolean.class) {
                            boolean[] jba = (boolean[]) ja1Converted;
                            for (int k = 0; k < n; k++) {
                                sum16 += getBooleanTest(jba, k);
                            }
                        } else if (elementType == int.class) {
                            int[] jia = (int[]) ja1Converted;
                            for (int k = 0; k < n; k++) {
                                sum16 += getIntTest(jia, k);
                            }
                        } else {
                            long[] jia = (long[]) ja1Converted;
                            for (int k = 0; k < n; k++) {
                                sum16 += (int) getLongTest(jia, k);
                            }
                        }
                    }
                } else {
                    sum16 = sum1;
                }
                t16 = System.nanoTime();
                a1 = a1.updatableClone(mm);
                int someValue = m;
                if (ja1Direct != null) {
                    ja1Direct = (int[]) ((DirectAccessible) a1).javaArray();
                    a1 = SimpleMemoryModel.asUpdatableIntArray(ja1Direct).subArr(
                        ((DirectAccessible) a1).javaArrayOffset(), n);
                    for (int q = 0; q < numberOfPasses; q++) {
                        for (int k = ((DirectAccessible) a1).javaArrayOffset(),
                                 kMax = ((DirectAccessible) a1).javaArrayOffset() + (int) n;
                             k < kMax; k++)
                        {
                            ja1Direct[k] = someValue;
                        }
                    }
                } else {
                    for (int q = 0; q < numberOfPasses; q++) {
                        if (elementType == boolean.class) {
                            ((UpdatableBitArray) a1).fill((someValue & 1) != 0);
                        } else if (elementType == int.class) {
                            ((UpdatableIntArray) a1).fill(someValue);
                        } else {
                            a1.fill(someValue);
                        }
                    }
                }
                t17 = System.nanoTime();
                if (threads.length > 1) {
                    a2 = a2.updatableClone(mm);
                }
                for (int q = 0; q < numberOfPasses; q++) {
                    if (elementType == boolean.class) {
                        myFill(a2, someValue & 1);
                    } else {
                        myFill(a2, someValue);
                    }
                }
                t18 = System.nanoTime();
                if (sourceArray != null) {
                    if (elementType == boolean.class) {
                        boolean[] sba = (boolean[]) sourceArray;
                        for (int q = 0; q < numberOfPasses; q++) {
                            for (int k = 0; k < (int) n; k++) {
                                sba[k] = (someValue & 1) != 0;
                            }
                            a3 = (UpdatablePFixedArray) mm.valueOf(sourceArray);
                        }
                    } else if (elementType == int.class) {
                        int[] sia = (int[]) sourceArray;
                        for (int q = 0; q < numberOfPasses; q++) {
                            for (int k = 0; k < (int) n; k++) {
                                sia[k] = someValue;
                            }
                            a3 = (UpdatablePFixedArray) mm.valueOf(sourceArray);
                        }
                    } else {
                        long[] sla = (long[]) sourceArray;
                        for (int q = 0; q < numberOfPasses; q++) {
                            for (int k = 0; k < (int) n; k++) {
                                sla[k] = someValue;
                            }
                            a3 = (UpdatablePFixedArray) mm.valueOf(sourceArray);
                        }
                    }
                } else {
                    if (threads.length > 1) {
                        a3 = a3.updatableClone(mm);
                    }
                    for (int q = 0; q < numberOfPasses; q++) {
                        if (elementType == boolean.class) {
                            ((UpdatableBitArray) a3).fill((someValue & 1) != 0);
                        } else {
                            ((UpdatableIntArray) a3).fill(someValue);
                        }
                    }
                }
                t19 = System.nanoTime();
                int[] intJavaArray = a1.jaInt();
                t20 = System.nanoTime();
                float[] floatJavaArray = a1.jaFloat();
                t21 = System.nanoTime();
                synchronized (threads) {
                    String gBD = elementType == boolean.class ? "getBits(): " : "getData(): ";
                    long shift = Arrays.getShift(a1sh);
                    if (shift > a1sh.length() / 2) {
                        shift -= a1sh.length();
                    }
                    String sh = String.format(Locale.US, "%-4s", shift + ",");
                    System.out.println("Sub-iteration #" + m + " in " + Thread.currentThread());
                    System.out.println("Sum of 1st array:                      " + sum1 + time(t1, t2));
                    System.out.println("Sum of 2nd array:                      " + sum2 + time(t2, t3));
                    System.out.println("Sum of 3rd array (sumOf, double):      " + sum3 + time(t3, t4));
                    System.out.println("Sum of 1st array (preciseSumOf, long): " + sum11 + time(t4, t5));
                    System.out.println("Sum of 2nd array, DataBuffer (int):    " + sum21 + time(t5, t6));
                    System.out.println("Sum of 3rd array, uneven DataBuffer:   " + sum31 + time(t6, t7));
                    System.out.println("Sum of 2nd array, " + gBD + "          " + sum22 + time(t7, t8));
                    System.out.println("Sum of 3rd array, " + gBD + "          " + sum32 + time(t8, t9));
                    System.out.println("Sum of 1st+2nd arrays, DataBuffer:     "
                        + sumC1 + time(t9, t10, aConcat.length()));
                    System.out.println("Sum of 1st+2nd arrays, " + gBD + "     "
                        + sumC2 + time(t10, t11, aConcat.length()));
                    System.out.println("Sum of 1st array >> " + sh + " DataBuffer:   "
                        + sum12 + time(t11, t12));
                    System.out.println("Sum of 1st array >> " + sh + " " + gBD + "   "
                        + sum13 + time(t12, t13));
                    if (ja1Direct != null) {
                        System.out.println("Sum of 1st array via DirectAccessible: "
                            + sum14 + time(t13, t14));
                    } else if (bb1Direct != null) {
                        System.out.println("Sum of 1st array via getByteBuffer:    "
                            + sum14 + time(t13, t14));
                    }
                    if (ja1Converted != null) {
                        System.out.println("Sum of 1st array via toJavaArray():    "
                            + sum15 + time(t14, t15));
                    }
                    if (ja1Converted != null) {
                        System.out.println("Sum of 1st array via getIntTest:       "
                            + sum16 + time(t15, t16));
                    }
                    if (sum1 != sum2 || sum1 != sum11 || sum1 != sum12 || sum1 != sum13)
                        // sum3 may differ from sum1 for large arrays!
                    {
                        throw new AssertionError("Bug 1 in int/bit array implementation! m = " + m
                            + ": " + sum1 + ", " + sum2 + ", " + sum3 + ", " + sum11
                            + ", " + sum12 + ", " + sum13);
                    }
                    if (sumC1 != sumC2) {
                        throw new AssertionError("Bug 2 in int/bit array implementation! m = " + m
                            + ": " + sumC1 + ", " + sumC2);
                    }
                    if (sum1 != sum14 || sum1 != sum15 || sum1 != sum16) {
                        throw new AssertionError("Bug 3 in int/bit array implementation! m = " + m
                            + ": " + sum1 + ", " + sum14 + ", " + sum15 + ", " + sum16);
                    }
                    if (sum21 != sum2 || sum31 != sum1 || sum22 != sum2 || sum32 != sum1) {
                        throw new AssertionError("Bug 4 in block access to array implementation! m = " + m
                            + ": " + sum1 + ", " + sum21 + ", " + sum31 + ", " + sum22 + ", " + sum32);
                    }
                    System.out.println("Filling 1st array by " + someValue + " via "
                        + (ja1Direct != null ?
                        "DirectAccessible" :
                        "fill method     ") + time(t16, t17));
                    System.out.println("Filling 2nd array by " + someValue + " via "
                        + "setInt method   " + time(t17, t18));
                    if (sourceArray != null) {
                        System.out.println("Creating 3rd array by " + someValue + " via "
                            + "valueOf method " + time(t18, t19));
                    } else {
                        System.out.println("Filling 3nd array by " + someValue + " via "
                            + "fill method    " + time(t18, t19));
                    }
                    System.out.println("jaInt():                              " + time(t19, t20)
                            + " - " + intJavaArray[0]);
                    System.out.println("jaFloat():                            " + time(t20, t21)
                            + " - " + floatJavaArray[0]);
                    System.out.println();
                }
            }
        }
    }

    public static void main(String[] args) {
        int startArgIndex = 0;
        final boolean useHeap, useGc, fullGcAtEnd;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noHeap")) {
            useHeap = false;
            startArgIndex++;
        } else {
            useHeap = true;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noGc")) {
            useGc = false;
            startArgIndex++;
        } else {
            useGc = true;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-fullGcAtEnd")) {
            fullGcAtEnd = true;
            startArgIndex++;
        } else {
            fullGcAtEnd = false;
        }
        if (args.length < startArgIndex + 3) {
            System.out.println("Usage: " + PArraysSpeed.class.getName()
                + " [-noHeap] [-noGc] [-fullGcAtEnd] bit|int|long arrayLength numberOfPasses"
                + " [numberOfThreads [numberOfIterations]]");
            return;
        }
        if (!useHeap) {
            System.out.println("Java arrays will not be used in this test");
        }
        final Class<?> elementType;
        if (args[startArgIndex].equalsIgnoreCase("bit")) {
            elementType = boolean.class;
        } else if (args[startArgIndex].equalsIgnoreCase("int")) {
            elementType = int.class;
        } else if (args[startArgIndex].equalsIgnoreCase("long")) {
            elementType = long.class;
        } else {
            throw new IllegalArgumentException("Unsupported element class " + args[startArgIndex]);
        }

        DemoUtils.freeResourcesBeforeFilling = false; // to correctly test whether unmap() or dispose() is called

        System.out.println(mm.toString());
        System.out.println();
        final long nOriginal = Long.parseLong(args[startArgIndex + 1]);
        final int numberOfPasses = Integer.parseInt(args[startArgIndex + 2]);
        final int numberOfThreads = startArgIndex + 3 < args.length ? Integer.parseInt(args[startArgIndex + 3]) : 1;
        final int numberOfIterations = startArgIndex + 4 < args.length ? Integer.parseInt(args[startArgIndex + 4]) : 3;
        for (int iteration = 1; iteration <= numberOfIterations; iteration++) {
            System.out.println("*** ITERATION #" + iteration);
            System.out.println();

            long n = nOriginal;

            long t1, t2, t3, t4;
            t1 = System.nanoTime();
            final MutablePFixedArray ma1 = (MutablePFixedArray)
                mm.newEmptyArray(elementType);
            if (elementType == boolean.class) {
                MutableBitArray mba = (MutableBitArray) ma1;
                for (int k = 0; k < n; k++) {
                    mba.pushBit((k % 7) == 0);
                }
            } else if (elementType == int.class) {
                MutableIntArray mia = (MutableIntArray) ma1;
                for (int k = 0; k < n; k++) {
                    mia.pushInt(k);
                }
            } else {
                MutableLongArray mla = (MutableLongArray) ma1;
                for (int k = 0; k < n; k++) {
                    mla.pushLong(k);
                }
            }
            t2 = System.nanoTime();
            final MutablePFixedArray ma2 = (MutablePFixedArray)
                mm.newEmptyArray(elementType);
            ma2.ensureCapacity(n);
            if (elementType == boolean.class) {
                MutableBitArray mba = (MutableBitArray) ma2;
                for (int k = 0; k < n; k++) {
                    mba.pushBit((k % 7) == 0);
                }
            } else if (elementType == int.class) {
                MutableIntArray mia = (MutableIntArray) ma2;
                for (int k = 0; k < n; k++) {
                    mia.pushInt(k);
                }
            } else {
                MutableLongArray mla = (MutableLongArray) ma2;
                for (int k = 0; k < n; k++) {
                    mla.pushLong(k);
                }
            }
            t3 = System.nanoTime();
            final UpdatablePFixedArray ma3 = (UpdatablePFixedArray)
                mm.newUnresizableArray(elementType, n);
            if (elementType == boolean.class) {
                UpdatableBitArray uba = (UpdatableBitArray) ma3;
                for (int k = 0; k < n; k++) {
                    uba.setBit(k, (k % 7) == 0);
                }
            } else {
                for (int k = 0; k < n; k++) {
                    ma3.setInt(k, k);
                }
            }

            t4 = System.nanoTime();
            System.out.printf(Locale.US, "Creating mutable (from 0):          %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1e-6 / n);
            System.out.printf(Locale.US, "Creating mutable (from %-12s %.3f ms, %.3f ns/element%n", n + "):",
                (t3 - t2) * 1e-6, (t3 - t2) * 1e-6 / n);
            System.out.printf(Locale.US, "Creating updatable (%-12s    %.3f ms, %.3f ns/element%n", n + "):",
                (t4 - t3) * 1e-6, (t4 - t3) * 1e-6 / n);
            System.out.println("1st array: " + ma1);
            System.out.println("2nd array: " + ma2);
            System.out.println("3rd array: " + ma3);

            t1 = System.nanoTime();
            ma2.trim();
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Trimming the second array: %.3f ms%n", (t2 - t1) * 1e-6);
            System.out.println("2nd array: " + ma2);
            System.out.println(numberOfThreads + " threads will used");
            System.out.println();

            threads = new Thread[numberOfThreads];
            for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
                threads[threadIndex] = new MyThread("thread " + iteration + "." + (threadIndex + 1),
                    elementType, n, numberOfPasses, ma1, ma2, ma3, useHeap, useGc);
                threads[threadIndex].start();
            }
            System.out.println();
            for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
                try {
                    threads[threadIndex].join();
                    System.out.println("Thread #" + threadIndex + " finished");
                } catch (InterruptedException ex) {
                    System.out.println("Thread #" + threadIndex + " was INTERRUPTED");
                }
            }
            System.out.println();
        }
        if (fullGcAtEnd) {
            DemoUtils.fullGC();
        }
    }
}
