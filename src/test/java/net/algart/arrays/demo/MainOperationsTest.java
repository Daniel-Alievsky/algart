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
import net.algart.arrays.demo.CombinedArraysDemo.Circle;
import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.Range;
import net.algart.math.functions.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>Thorough test for AlgART array copy, swap, buffer mapping and other operations.</p>
 *
 * <p>An example of call (64-bit Java):</p>
 *
 * <p><tt>java -ea -server -Xmx200m -Dnet.algart.arrays.globalMemoryModel=LARGE
 * -Dnet.algart.arrays.serverOptimization=true
 * -Dnet.algart.arrays.CPUCount=4
 * -Dnet.algart.arrays.DefaultDataFileModel.resizableBankSize=256
 * -Dnet.algart.arrays.DefaultDataFileModel.bankSize=2048
 * -Dnet.algart.arrays.DefaultDataFileModel.singleMappingLimit=0
 * -Dnet.algart.arrays.LargeMemoryModel.dataFileModel=DEFAULT
 * net.algart.arrays.demo.MainOperationsTest ALL 1000 2000
 * </tt></p>
 *
 * <p>Please test with different values of the listed properties and arguments:
 * they change the behavior of the tested algorithms.
 * The array length must not be too large: the length near 2^30-2^31 may lead to false "bugs".</p>
 *
 * <p>Please not set low values for net.algart.arrays.LargeMemoryModel.maxNumberOfBanksInLazyFillMap property.</p>
 *
 * @author Daniel Alievsky
 */
public strictfp class MainOperationsTest implements Cloneable {
    private static boolean objectEquals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    private static void myFill(Object javaArray, int from, int to, int startValue, boolean packedBits) {
        if (packedBits) {
            long[] ja = (long[]) javaArray;
            for (int k = from; k < to; k++)
                PackedBitArrays.setBit(ja, k, ((k - from + startValue) & 1) == 1);
        } else if (javaArray instanceof boolean[]) {
            boolean[] ja = (boolean[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = ((k - from + startValue) & 1) == 1;
        } else if (javaArray instanceof char[]) {
            char[] ja = (char[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = (char) (k - from + startValue);
        } else if (javaArray instanceof byte[]) {
            byte[] ja = (byte[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = (byte) (k - from + startValue);
        } else if (javaArray instanceof short[]) {
            short[] ja = (short[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = (short) (k - from + startValue);
        } else if (javaArray instanceof int[]) {
            int[] ja = (int[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = k - from + startValue;
        } else if (javaArray instanceof long[]) {
            long[] ja = (long[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = k - from + startValue;
        } else if (javaArray instanceof float[]) {
            float[] ja = (float[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = k - from + startValue;
        } else if (javaArray instanceof double[]) {
            double[] ja = (double[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = k - from + startValue;
        } else if (javaArray instanceof String[]) {
            String[] ja = (String[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = k - from + startValue + "";
        } else if (javaArray instanceof Circle[]) {
            Circle[] ja = (Circle[]) javaArray;
            for (int k = from; k < to; k++)
                ja[k] = new Circle(k - from + startValue, -(k - from + startValue), k - from + startValue);
        } else {
            throw new AssertionError("Unknown object type");
        }
    }

    private final static Method mDefaultCopy;
    private final static Method mDefaultSwap;

    static {
        try {
            mDefaultCopy = AbstractArray.class.getDeclaredMethod("defaultCopy",
                UpdatableArray.class, Array.class);
            mDefaultCopy.setAccessible(true);
            mDefaultSwap = AbstractArray.class.getDeclaredMethod("defaultSwap",
                UpdatableArray.class, UpdatableArray.class);
            mDefaultSwap.setAccessible(true);
        } catch (Exception ex) {
            throw new InternalError(ex.toString());
        }
    }

    private static final Class<?>[] ALL_P_CLASSES = {
        boolean.class, short.class, byte.class, short.class,
        int.class, long.class, float.class, double.class
    };

    private static long mask(Class<?> elementType, double value) {
        if (elementType == byte.class)
            return (long) value & 0xFF;
        if (elementType == short.class)
            return (long) value & 0xFFFF;
        if (elementType == char.class)
            return (long) value & 0xFFFF;
        if (elementType == int.class)
            return (int) (long) value;
        return (long) value;
    }

    private PArray parallelClone(PArray src) {
        // the following copy method with null context will use the default pool factory,
        // according to -Dnet.algart.arrays.CPUCount system property
        UpdatablePArray clone = (UpdatablePArray) gmm.newUnresizableArray(src);
        Arrays.copy(null, clone, src, 0, false);
        return clone;
    }

    static class UsageException extends Exception {
        private static final long serialVersionUID = 6523080205146461200L; // avoiding the warning
    }

    private final MemoryModel gmm = Arrays.SystemSettings.globalMemoryModel();
    private final LargeMemoryModel<File> lmmStdIO = LargeMemoryModel.getInstance(new StandardIODataFileModel());
    private final boolean noGc, noSlowTests, noShuffle, noCheckStability, funcOnly, multithreading;
    private final int numberOfTests, blockSize;
    private final BitSet testIndexSet;
    private final long startSeed;
    private final String[] allTypeNames;
    private final ThreadPoolExecutor thp;
    private final Random rnd;
    private int threadIndex;
    private int len;
    private MemoryModel cmm;
    private UpdatableArray a;
    private UpdatableArray workStdIO;
    private UpdatableArray work1;
    private UpdatableArray work2;
    private UpdatableArray work3;
    private UpdatableArray work4;
    private UpdatableBitArray bits, bitsSimple, workBits;
    private MutableArray mwork1;
    private Object workJA1;
    private long[] workJBits;
    private Object e1, e2;

    MainOperationsTest(String[] args) throws UsageException {
        int startArgIndex = 0;
        if (noGc = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noGc"))
            startArgIndex++;
        if (noSlowTests = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noSlowTests"))
            startArgIndex++;
        if (noShuffle = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noShuffle"))
            startArgIndex++;
        if (noCheckStability = startArgIndex < args.length && args[startArgIndex]
            .equalsIgnoreCase("-noCheckStability"))
            startArgIndex++;
        if (funcOnly = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-funcOnly"))
            startArgIndex++;
        if (multithreading = startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-multithreading"))
            startArgIndex++;
        if (args.length < startArgIndex + 3) {
            throw new UsageException();
        }

        String passedElementTypeName = args[startArgIndex];
        allTypeNames = passedElementTypeName.equalsIgnoreCase("all") ?
            DemoUtils.allElementTypes :
            new String[]{passedElementTypeName};
        len = Integer.parseInt(args[startArgIndex + 1]);
        numberOfTests = Integer.parseInt(args[startArgIndex + 2]);
        if (args.length >= startArgIndex + 4) {
            blockSize = Integer.parseInt(args[startArgIndex + 3]);
        } else {
            blockSize = 1;
        }
        if (args.length < startArgIndex + 5) {
            startSeed = new Random().nextLong();
        } else {
            long v = Long.parseLong(args[startArgIndex + 4]);
            startSeed = v != -1 ? v : new Random().nextLong();
        }
        rnd = new Random(startSeed);
        if (args.length < startArgIndex + 6) {
            testIndexSet = null;
        } else {
            testIndexSet = new BitSet();
            String[] params = args[startArgIndex + 5].split("\\.\\.");
            if (params.length == 2) {
                int startIndex = Integer.parseInt(params[0]);
                int endIndex = Integer.parseInt(params[1]);
                testIndexSet.set(startIndex, endIndex + 1);
            } else {
                params = args[startArgIndex + 5].split(",");
                for (String param : params) {
                    testIndexSet.set(Integer.parseInt(param));
                }
            }
        }

        thp = DefaultThreadPoolFactory.globalThreadPool();

        System.out.println("Main model: " + gmm);
        System.out.println("Alternative model: " + lmmStdIO);
        System.out.println("Maximal number of parallel tasks: "
            + DefaultThreadPoolFactory.getDefaultThreadPoolFactory()
            .recommendedNumberOfTasks(Arrays.nFloatCopies(len, 0)));
        if (thp != null) {
            System.out.println("Maximal number of threads in pool: " + thp.getMaximumPoolSize());
        }
        System.out.println();
        System.out.println("Testing " + len + " elements " + numberOfTests
            + " times with start random seed " + startSeed);
        if (blockSize > 1)
            System.out.println("All offsets are aligned: block size = " + blockSize);
        System.out.println();
    }

    protected MainOperationsTest clonePart(int from, int to) {
        MainOperationsTest result;
        try {
            result = (MainOperationsTest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        result.len = to - from;
        result.a = a.subArray(from, to);
        if (workStdIO != null) {
            result.workStdIO = workStdIO.subArray(from, to);
        }
        result.work1 = work1.subArray(from, to);
        result.work2 = work2.subArray(from, to);
        result.work3 = work3.subArray(from, to);
        result.work4 = work4.subArray(from, to);
        result.bits = bits.subArray(from, to);
        result.bitsSimple = bitsSimple.subArray(from, to);
        result.workBits = workBits.subArray(from, to);
        try {
            result.mwork1 = cmm.newArray(result.a.elementType(), 0);
        } catch (UnsupportedOperationException e) { // possible in CombinedArraysDemo.MyCombinerPacked
            result.mwork1 = null; // for packed combiner
        }
        result.workJA1 = result.a.newJavaArray(result.len);
        result.workJBits = new long[(int) PackedBitArrays.packedLength(result.len)];
        return result;
    }

    void testAll() {
        for (String elementTypeName : allTypeNames) {
            testElementType(elementTypeName);
        }
        System.out.printf(Locale.US, "All O'k: testing time %.3f seconds%n",
            0.001 * (System.currentTimeMillis() - DemoUtils.tStart));
    }

    private boolean title(int testIndex, String msg) {
        if (this.testIndexSet == null || this.testIndexSet.get(testIndex)) {
            synchronized (MainOperationsTest.class) {
                if (multithreading) {
                    System.out.printf(Locale.US, "%-7s %s%n", "(" + testIndex + ":" + threadIndex + ")", msg);
                } else {
                    System.out.printf(Locale.US, "%-4s %s%n", "(" + testIndex + ")", msg);
                }
            }
            return true;
        } else {
            return false;
        }
    }


    static long tFix = System.currentTimeMillis();

    void showProgress(int iteration, int numberOfTests) {
        synchronized (MainOperationsTest.class) {
            long t = System.currentTimeMillis();
            if (t - tFix > 500 || iteration >= numberOfTests - 1) {
                tFix = t;
                if (multithreading) {
                    System.out.print("\r" + threadIndex + ":" + iteration + "     ");
                } else {
                    System.out.print("\r" + iteration + "     ");
                }
                if (iteration >= numberOfTests - 1) {
                    System.out.print(" \r                 \r");
                }
            }
        }
    }

    private void testElementType(final String elementTypeName) {
        System.out.println("Testing " + elementTypeName
            + (testIndexSet == null ? "" : " (the tests #" + testIndexSet + " only)"));

        System.out.println("Creating test array... ");
        try {
            a = DemoUtils.createTestUnresizableArray(elementTypeName, len);
            assert a.length() == len;
        } catch (UnsupportedElementTypeException ex) {
            System.out.println("This type is not supported by this memory model");
            System.out.println();
            return;
        }
        cmm = DemoUtils.memoryModel(elementTypeName);
        bits = (UpdatableBitArray) DemoUtils.createTestUnresizableArray("boolean", len);
        if (!noShuffle) {
            long shuffleLen = Math.min(1000000, a.length());
            System.out.println("Shuffling first " + shuffleLen + " elements... ");
            java.util.Collections.shuffle(Arrays.asList(a.subArr(0, shuffleLen), Object.class), rnd);
            java.util.Collections.shuffle(Arrays.asList(bits.subArr(0, shuffleLen), Object.class), rnd);
        }
        System.out.println("Allocating work memory... ");
        workStdIO = null;
        if (lmmStdIO.isElementTypeSupported(a.elementType())) {
            workStdIO = lmmStdIO.newUnresizableArray(a.elementType(), a.length());
        }
        work1 = cmm.newUnresizableArray(a);
        work2 = cmm.newUnresizableArray(a);
        work3 = cmm.newUnresizableArray(a);
        work4 = cmm.newUnresizableArray(a);
        bitsSimple = SimpleMemoryModel.getInstance().newBitArray(len).copy(bits);
        workBits = gmm.newUnresizableBitArray(len);
        mwork1 = null; // created in clonePart

        System.out.println("Recommended number of parallel ranges: "
            + Arrays.ParallelExecutor.recommendedNumberOfRanges(a, true));
        Runtime rt = Runtime.getRuntime();
        long m1 = rt.totalMemory() - rt.freeMemory();
        System.out.printf(Locale.US, "Used memory: 6*%.3f MB%n", m1 / 6.0 / 1048576.0);
        workJA1 = null; // created in clonePart
        workJBits = null; // created in clonePart
        long m2 = rt.totalMemory() - rt.freeMemory();
        System.out.printf(Locale.US, "Used memory for Java arrays: %.3f MB%n", (m2 - m1) / 1048576.0);
        System.out.println("Tested array: " + (a instanceof CharArray ?
            Arrays.toHexString(a, ", ", 100) : Arrays.toString(a, ", ", 100)));
        System.out.println("Bit mask for unpacking bits: " + Arrays.toHexString(bits, "", 100));

        int numberOfThreads = multithreading ?
            DefaultThreadPoolFactory.getDefaultThreadPoolFactory().recommendedNumberOfTasks() : 1;
        Thread[] threads = new Thread[numberOfThreads];
        for (int threadIndex = 0; threadIndex < threads.length; threadIndex++) {
            int from = (int) (len * (double) threadIndex / (double) threads.length);
            int to = Math.min(len, (int) (len * (double) (threadIndex + 1) / (double) threads.length));
            final MainOperationsTest test = clonePart(from, to);
            test.threadIndex = threadIndex;
            threads[threadIndex] = new Thread() {
                @Override
                public void run() {
                    try {
                        int testIndex = 0;
                        test.testCopyEqualsHashCode(++testIndex);
                        test.testCopyAlternateSrcModel(++testIndex);
                        test.testCopyAlternateDestModel(++testIndex);
                        test.testFill(++testIndex);
                        test.testIndexOf(++testIndex);
                        test.testAsCopyOnNextWrite(++testIndex, elementTypeName.indexOf("Packed") != -1);
                        // CombinedArraysDemo.MyCombinerPacked does not support resizable arrays
                        test.testGetSetBits(++testIndex);
                        test.testBufferMapping(++testIndex);
                        test.testLazyCopy(++testIndex, DemoUtils.memoryModel(elementTypeName),
                            elementTypeName.indexOf("Packed") != -1);
                        test.testDefaultCopy(++testIndex);
                        test.testCopyInside(++testIndex);
                        test.testCopySameArrayNoOverlap(++testIndex);
                        test.testDefaultCopySameArrayNoOverlap(++testIndex);
                        test.testSwapDifferentArrays(++testIndex);
                        test.testDefaultSwapDifferentArrays(++testIndex);
                        test.testSwapInsideNoOverlap(++testIndex);
                        test.testSwapSameArrayNoOverlap(++testIndex);
                        test.testDefaultSwapSameArrayNoOverlap(++testIndex);
                        test.testPackBits(++testIndex);
                        test.testUnpackBits(++testIndex);
                        test.testZeroFillAndIsZeroFilled(++testIndex);
                        test.testSerialization(++testIndex);
                        test.testRangeOf(++testIndex);
                        test.testSumOf(++testIndex);
                        test.testPreciseSumOf(++testIndex);
                        test.testHistogramOf(++testIndex);
                        test.testAsConcatenation(++testIndex);
                        test.testAsShifted(++testIndex);
                        test.testTileMatrix(++testIndex);
                        test.testSubMatrix(++testIndex, Matrix.ContinuationMode.NONE);
                        test.testSubMatrix(++testIndex, Matrix.ContinuationMode.CYCLIC);
                        test.testSubMatrix(++testIndex, Matrix.ContinuationMode.PSEUDO_CYCLIC);
                        test.testSubMatrix(++testIndex, Matrix.ContinuationMode.MIRROR_CYCLIC);
                        test.testSubMatrix(++testIndex, Matrix.ContinuationMode.getConstantMode(157.0));
                        test.testCopyRegion(++testIndex);
                        test.testMinMaxFunc2Args(++testIndex);
                        test.testMinMaxFunc3Args(++testIndex);
                        test.testMinMaxFunc2ArgsWithShift(++testIndex);
                        test.testMinMaxFuncNArgsWithShift(++testIndex);
                        test.testLinearFunc1ArgAndUpdatableFunc(++testIndex);
                        test.testLinearFunc2Args(++testIndex);
                        test.testPowerFuncAndUpdatableFunc(++testIndex);
                        test.testCoordFunc(++testIndex);
                        test.testResized(++testIndex);

                        if (test.a instanceof PArray && !(test.a instanceof BitArray)) {
                            // Filling the tested array by random values:
                            // further operations should be tested for overflow
                            // and does not require little values
                            // (BitArrays should not be tested by this way: almost all elements will be 1.)
                            UpdatablePArray pa = (UpdatablePArray) test.a;
                            for (long k = 0, n = test.a.length(); k < n; k++) {
                                pa.setLong(k, test.rnd.nextLong());
                            }
                        }

                        test.testMinusAndDiffFuncWithShift(++testIndex);
                        test.testInverseNumberFuncAndUpdatableFunc(++testIndex);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.err.println("Start random seed: " + test.startSeed);
                        System.exit(1); // exiting thread and skipping fullGC: test for the default cleanup procedure
                    }
                }
            };
            threads[threadIndex].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println();
        if (!noGc)
            System.gc();
    }

    private void testCopyEqualsHashCode(int ti) {
        if (!title(ti, "Testing \"copy(Array src)\", \"equals\" and \"hashCode\" methods, "
            + "two different arrays (both unresizable)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            Arrays.zeroFill(work1);
            Arrays.zeroFill(work2);
            Array sa1 = a.subArr(srcPos, count);
            UpdatableArray sa2 = work1.subArr(destPos, count);
            sa2.copy(sa1);
            for (int k = 0; k < count; k++)
                work2.setElement(destPos + k, a.getElement(srcPos + k));
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);

            if (!(sa1.equals(sa2)))
                throw new AssertionError("The bug in subarray equals found in test #" + testCount);
            if (sa1.hashCode() != sa2.hashCode())
                throw new AssertionError("The bug in subarray hash code found in test #" + testCount);
            if (!work1.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testCopyAlternateSrcModel(int ti) {
        if (funcOnly || workStdIO == null)
            return;
        if (!title(ti, "Testing \"copy(Array src)\", two different unresizable arrays, alternative src model..."))
            return;
        workStdIO.copy(a);
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            Arrays.zeroFill(work1);
            Arrays.zeroFill(work2);
            Array sa1 = workStdIO.subArr(srcPos, count);
            UpdatableArray sa2 = work1.subArr(destPos, count);
            sa2.copy(sa1);
            for (int k = 0; k < count; k++)
                work2.setElement(destPos + k, a.getElement(srcPos + k));
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);

            if (!(sa1.equals(sa2)))
                throw new AssertionError("The bug in subarray equals found in test #" + testCount);
            if (sa1.hashCode() != sa2.hashCode())
                throw new AssertionError("The bug in subarray hash code found in test #" + testCount);
            if (!work1.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testCopyAlternateDestModel(int ti) {
        if (funcOnly || workStdIO == null)
            return;
        if (!title(ti, "Testing \"copy(Array src)\", two different unresizable arrays, "
            + "alternative this instance model..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            Arrays.zeroFill(workStdIO);
            Arrays.zeroFill(work2);
            Array sa1 = a.subArr(srcPos, count);
            UpdatableArray sa2 = workStdIO.subArr(destPos, count);
            sa2.copy(sa1);
            for (int k = 0; k < count; k++)
                work2.setElement(destPos + k, a.getElement(srcPos + k));
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = workStdIO.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);

            if (!(sa1.equals(sa2)))
                throw new AssertionError("The bug in subarray equals found in test #" + testCount);
            if (sa1.hashCode() != sa2.hashCode())
                throw new AssertionError("The bug in subarray hash code found in test #" + testCount);
            if (!workStdIO.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testFill(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"UpdatableP/ObjectArray.fill\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - destPos) / blockSize * blockSize;
            work1.copy(a);
            work2.copy(a);
            work3.copy(a);
            if (a instanceof PArray) {
                double doubleFiller = rnd.nextDouble() * 1000;
                UpdatablePArray sa = (UpdatablePArray) work1.subArr(destPos, count);
                sa.fill(doubleFiller);
                ((UpdatablePArray) work2).fill(destPos, count, doubleFiller);
                for (int k = 0; k < count; k++)
                    ((UpdatablePArray) work3).setDouble(destPos + k, doubleFiller);
            } else {
                UpdatableObjectArray<?> sa = (UpdatableObjectArray<?>) work1.subArr(destPos, count);
                sa.fill(null);
                ((UpdatableObjectArray<?>) work2).fill(destPos, count, null);
                for (int k = 0; k < count; k++)
                    ((UpdatableObjectArray<?>) work3).set(destPos + k, null);
            }
            for (int k = 0; k < len; k++) {
                if (!objectEquals(e1 = work1.getElement(k), e2 = work3.getElement(k)))
                    throw new AssertionError("The bug in fill(double) found in test #" + testCount + ": "
                        + "destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
                if (!objectEquals(e1 = work2.getElement(k), e2))
                    throw new AssertionError("The bug in fill(long, long, double) found in test #" + testCount + ": "
                        + "destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            }
            if (!work1.equals(work3))
                throw new AssertionError("The bug in equals or fill(double) found in test #" + testCount);
            if (!work2.equals(work3))
                throw new AssertionError("The bug in equals or fill(long, long, double) found in test #" + testCount);
            if (a instanceof PArray) {
                work1.copy(a);
                work2.copy(a);
                work3.copy(a);
                UpdatablePArray sa = (UpdatablePArray) work1.subArr(destPos, count);
                long longFiller = rnd.nextLong();
                ((UpdatablePArray) work2).fill(destPos, count, longFiller);
                sa.fill(longFiller);
                for (int k = 0; k < count; k++)
                    ((UpdatablePArray) work3).setLong(destPos + k, longFiller);
                for (int k = 0; k < len; k++) {
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work3.getElement(k)))
                        throw new AssertionError("The bug in fill(long) found in test #" + testCount + ": "
                            + "destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                    if (!objectEquals(e1 = work2.getElement(k), e2))
                        throw new AssertionError("The bug in fill(long, long, long) found in test #"
                            + testCount + ": "
                            + "destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                }
                if (!work1.equals(work2))
                    throw new AssertionError("The bug in equals or fill(long) found in test #" + testCount);
                if (!work2.equals(work3))
                    throw new AssertionError("The bug in equals or fill(long, long, long) found in test #"
                        + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private static final double[] SEARCHED_VALUES = {
        0.0, -1.7, 124, 120.1, 300, 300.1, 40000, 40000.1, 3000000000L, Double.NaN,
    };

    private void testIndexOf(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"indexOf\" and \"lastIndexOf\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            int desiredIndex = count == 0 || rnd.nextBoolean() ? -1 : rnd.nextInt(count);
            int desiredLastIndex = desiredIndex;
            double value = SEARCHED_VALUES[rnd.nextInt(SEARCHED_VALUES.length)];
            Array sa;
            boolean notFound = false;
            if (rnd.nextInt(3) > 0) {
                sa = work1.subArr(srcPos, count);
                if (rnd.nextInt(3) == 0) {
                    sa = work1;
                    srcPos = 0;
                    count = len;
                }
                if (a instanceof PArray) {
                    ((UpdatablePArray) work1).fill(1.0);
                } else if (a.elementType() == String.class) {
                    ((UpdatableObjectArray<?>) work1).cast(String.class).fill("1");
                } else {
                    ((UpdatableObjectArray<?>) work1).cast(Circle.class).fill(new Circle(0, 0, 1));
                }
                if (desiredIndex != -1) {
                    if (a instanceof PArray) {
                        ((UpdatablePArray) work1).setDouble(srcPos + desiredIndex, value);
                    } else if (a.elementType() == String.class) {
                        work1.setElement(srcPos + desiredIndex, "0");
                    } else {
                        work1.setElement(srcPos + desiredIndex, new Circle(0, 0, (int) value & 0xFFF));
                        // (int) & 0xFFF is necessary for MyCombinerPacked: it cannot support even large shorts
                    }
                }
                if (rnd.nextInt(3) == 0) {
                    sa = Arrays.asShifted(sa, 3); // checking AbstractXxxArray
                    if (desiredIndex != -1 && count > 0) {
                        desiredLastIndex = desiredIndex = (desiredIndex + 3) % count;
                    }
                }
            } else { // checking nXxxCopies
                notFound = rnd.nextBoolean();
                if (a instanceof PArray) {
                    sa = Arrays.nPCopies(count, a.elementType(), notFound ? 1.0 : value);
                } else if (a.elementType() == String.class) {
                    sa = Arrays.nObjectCopies(count, notFound ? "1" : "0");
                    value = 0;
                } else {
                    sa = Arrays.nObjectCopies(count,
                        notFound ? new Circle(1, 1, 1) : new Circle(0, 0, (int) value & 0xFFF));
                    // (int) & 0xFFF is necessary for MyCombinerPacked: it cannot support even large shorts
                }
            }
            long index, indexSub, lastIndex, lastIndexSub, indexFixed, lastIndexFixed;
            long indexReq = desiredIndex, lastIndexReq = desiredLastIndex;
            long indexFixedReq = desiredIndex, lastIndexFixedReq = desiredLastIndex;
            long low = -100, high = Long.MAX_VALUE;
            if (rnd.nextBoolean()) {
                low = -2 + rnd.nextInt((int) sa.length() + 4);
                high = -2 + rnd.nextInt((int) sa.length() + 4);
            } else if (rnd.nextInt(3) == 0) {
                low = rnd.nextLong();
                high = Long.MIN_VALUE; // dangerous case!
            }
            long corLow = Math.max(low, 0);
            long corHigh = Math.min(high, sa.length());
            if (Arrays.isNCopies(sa)) {
                if (low >= high || low >= sa.length() || high <= 0 || sa.length() == 0) {
                    notFound = true;
                }
                indexReq = indexFixedReq = desiredIndex = notFound ? -1 :
                    Math.max(0, (int) low);
                lastIndexReq = lastIndexFixedReq = desiredLastIndex = notFound ? -1 :
                    (int) Math.min(sa.length(), high) - 1;
            } else {
                if (desiredIndex < low || desiredIndex >= high) {
                    indexReq = indexFixedReq = -1;
                }
                if (desiredLastIndex < low || desiredLastIndex >= high) {
                    lastIndexReq = lastIndexFixedReq = -1;
                }
            }
            if (a instanceof PArray) {
                // desired index may differ from actual index due to rounding
                index = ((PArray) sa).indexOf(low, high, value);
                indexSub = corLow >= corHigh ? -1 :
                    ((PArray) sa.subArray(corLow, corHigh)).indexOf(0, corHigh - corLow, value);
                if (indexSub != -1)
                    indexSub += corLow;
                indexReq = -1;
                for (long k = corLow, n = corHigh; k < n; k++) {
                    if (((PArray) sa).getDouble(k) == value) {
                        indexReq = k;
                        break;
                    }
                }
                lastIndex = ((PArray) sa).lastIndexOf(low, high, value);
                lastIndexSub = corLow >= corHigh ? -1 :
                    ((PArray) sa.subArray(corLow, corHigh)).lastIndexOf(0, corHigh - corLow, value);
                if (lastIndexSub != -1)
                    lastIndexSub += corLow;
                lastIndexReq = -1;
                for (long k = corHigh; k > corLow; ) {
                    if (((PArray) sa).getDouble(--k) == value) {
                        lastIndexReq = k;
                        break;
                    }
                }
                indexFixed = index;
                lastIndexFixed = lastIndex;
                indexFixedReq = indexReq;
                lastIndexFixedReq = lastIndexReq;
                if (a instanceof PFixedArray) {
                    indexFixed = ((PFixedArray) sa).indexOf(low, high, (long) value);
                    lastIndexFixed = ((PFixedArray) sa).lastIndexOf(low, high, (long) value);
                    indexFixedReq = -1;
                    for (long k = Math.max(low, 0), n = Math.min(high, sa.length()); k < n; k++) {
                        if (((PFixedArray) sa).getLong(k) == (long) value) {
                            indexFixedReq = k;
                            break;
                        }
                    }
                    lastIndexFixedReq = -1;
                    for (long k = Math.min(sa.length(), high); k > Math.max(low, 0); ) {
                        if (((PFixedArray) sa).getLong(--k) == (long) value) {
                            lastIndexFixedReq = k;
                            break;
                        }
                    }
                }
            } else if (a.elementType() == String.class) {
                index = ((ObjectArray<?>) sa).cast(String.class).indexOf(low, high, "0");
                indexSub = corLow >= corHigh ? -1 :
                    ((ObjectArray<?>) sa.subArray(corLow, corHigh)).cast(String.class)
                        .indexOf(0, corHigh - corLow, "0");
                if (indexSub != -1)
                    indexSub += corLow;
                lastIndex = ((ObjectArray<?>) sa).cast(String.class).lastIndexOf(low, high, "0");
                lastIndexSub = corLow >= corHigh ? -1 :
                    ((ObjectArray<?>) sa.subArray(corLow, corHigh)).cast(String.class)
                        .lastIndexOf(0, corHigh - corLow, "0");
                if (lastIndexSub != -1)
                    lastIndexSub += corLow;
                indexFixed = index;
                lastIndexFixed = lastIndex;
            } else {
                Circle zero = new Circle(0, 0, (int) value & 0xFFF);
                index = ((ObjectArray<?>) sa).cast(Circle.class).indexOf(low, high, zero);
                indexSub = corLow >= corHigh ? -1 :
                    ((ObjectArray<?>) sa.subArray(corLow, corHigh)).cast(Circle.class)
                        .indexOf(0, Long.MAX_VALUE, zero);
                if (indexSub != -1)
                    indexSub += corLow;
                lastIndex = ((ObjectArray<?>) sa).cast(Circle.class).lastIndexOf(low, high, zero);
                lastIndexSub = corLow >= corHigh ? -1 :
                    ((ObjectArray<?>) sa.subArray(corLow, corHigh)).cast(Circle.class)
                        .lastIndexOf(0, Long.MAX_VALUE, zero);
                if (lastIndexSub != -1)
                    lastIndexSub += corLow;
                indexFixed = index;
                lastIndexFixed = lastIndex;
            }
            if (index != indexReq)
                throw new AssertionError("The bug A in indexOf(double/Object) found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", low = " + low + ", high = " + high
                    + ", value = " + value + ", desired index = " + desiredIndex
                    + (Arrays.isNCopies(sa) ? ", copies array" : Arrays.isShifted(sa) ? ", shifted array" : "")
                    + ": " + index + " instead of " + indexReq);
            if (indexSub != indexReq)
                throw new AssertionError("The bug B in indexOf(double/Object) found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", low = " + low + ", high = " + high
                    + ", value = " + value + ", desired index = " + desiredIndex
                    + (Arrays.isNCopies(sa) ? ", copies array" : Arrays.isShifted(sa) ? ", shifted array" : "")
                    + ": " + index + " instead of " + indexReq);
            if (lastIndex != lastIndexReq)
                throw new AssertionError("The bug C in lastIndexOf(double/Object) found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", low = " + low + ", high = " + high
                    + ", value = " + value + ", desired index = " + desiredLastIndex
                    + (Arrays.isNCopies(sa) ? ", copies array" : Arrays.isShifted(sa) ? ", shifted array" : "")
                    + ": " + lastIndex + " instead of " + lastIndexReq);
            if (lastIndexSub != lastIndexReq)
                throw new AssertionError("The bug D in lastIndexOf(double/Object) found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", low = " + low + ", high = " + high
                    + ", value = " + value + ", desired index = " + desiredLastIndex
                    + (Arrays.isNCopies(sa) ? ", copies array" : Arrays.isShifted(sa) ? ", shifted array" : "")
                    + ": " + lastIndex + " instead of " + lastIndexReq);
            if (indexFixed != indexFixedReq)
                throw new AssertionError("The bug E in indexOf(long) found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", low = " + low + ", high = " + high
                    + ", value = " + value + ", desired index = " + desiredIndex
                    + (Arrays.isNCopies(sa) ? ", copies array" : Arrays.isShifted(sa) ? ", shifted array" : "")
                    + ": " + indexFixed + " instead of " + indexFixedReq);
            if (lastIndexFixed != lastIndexFixedReq)
                throw new AssertionError("The bug F in lastIndexOf(long) found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", low = " + low + ", high = " + high
                    + ", value = " + value + ", desired index = " + desiredLastIndex
                    + (Arrays.isNCopies(sa) ? ", copies array" : Arrays.isShifted(sa) ? ", shifted array" : "")
                    + ": " + lastIndexFixed + " instead of " + lastIndexFixedReq);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testAsCopyOnNextWrite(int ti, boolean onlyUnresizable) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"asCopyOnNextWrite()\" method..."))
            return;
        Object zero = cmm.newUnresizableArray(a.elementType(), 1).getElement(0);
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextBoolean() ? 0 : rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextBoolean() ? len - srcPos : rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            boolean mutable = rnd.nextBoolean() && !onlyUnresizable;
            boolean doSwap = rnd.nextBoolean();
            work1.copy(a);
            work2.copy(a);
            Array sa = count == len ? work1 : work1.subArr(srcPos, count);
            if (mutable) {
                sa = sa.mutableClone(cmm);
            }
            UpdatableArray conw = (UpdatableArray) sa.asCopyOnNextWrite();
            if (!(sa.equals(conw)))
                throw new AssertionError("The bug A in asCopyOnNextWrite found in test #" + testCount);
            if (mutable) {
                Arrays.insertEmptyRange((MutableArray) conw, 0, 100);
            } else {
                if (count > 0) {
                    if (!doSwap) {
                        for (int j = 0; j < 10; j++) {
                            int k = rnd.nextInt(count);
                            conw.setElement(k, zero);
                            work2.setElement(srcPos + k, zero);
                        }
                    } else {
                        for (int j = 0; j < 10; j++) {
                            int k1 = rnd.nextInt(count), k2 = rnd.nextInt(count);
                            conw.swap(k1, k2);
                            work2.swap(srcPos + k1, srcPos + k2);
                        }
                    }
                }
            }
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = a.getElement(k)))
                    throw new AssertionError("The bug B in asCopyOnNextWrite found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count + (doSwap ? " (swapping)" : "")
                        + ", error found at " + k + ": " + e1 + " (damaged) instead of " + e2);
            if (!(a.equals(work1)))
                throw new AssertionError("The bug C in asCopyOnNextWrite (equals) found in test #" + testCount);
            for (int k = 0; k < count; k++)
                if (!objectEquals(e1 = conw.getElement(mutable ? k + 100 : k), e2 = work2.getElement(srcPos + k)))
                    throw new AssertionError("The bug D in asCopyOnNextWrite found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count + (mutable ? " (mutable)" : "")
                        + ", error found at " + k + ": " + e1 + " (unchanged?) instead of " + e2);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testGetSetBits(int ti) {
        if (funcOnly)
            return;
        if (!(a instanceof BitArray))
            return;
        if (!title(ti, "Testing \"getBits\" and \"setBits\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            work1.copy(a);
            work2.copy(a);
            ((BitArray) a).getBits(srcPos, workJBits, 0, count);
            ((UpdatableBitArray) work1).setBits(destPos, workJBits, 0, count);
            work2.subArr(destPos, count).copy(a.subArr(srcPos, count));
            for (int k = 0; k < count; k++) {
                if (((BitArray) a).getBit(srcPos + k) != PackedBitArrays.getBit(workJBits, k))
                    throw new AssertionError("The bug A in getBits found in test #"
                        + testCount + ": destPos = " + destPos + ", count = " + count + ", "
                        + ", error found at " + k);
                if (((BitArray) work1).getBit(destPos + k) != PackedBitArrays.getBit(workJBits, k))
                    throw new AssertionError("The bug B in setBits found in test #"
                        + testCount + ": destPos = " + destPos + ", count = " + count + ", "
                        + ", error found at " + k);
            }
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug C in get/setBits or copy found in test #"
                        + testCount + ": destPos = " + destPos + ", count = " + count + ", "
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work1.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testBufferMapping(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"buffer().map\" method, changing + forcing..."))
            return;
        DataBuffer buf = work1.buffer(DataBuffer.AccessMode.READ_WRITE,
            Math.max(250, len / 10) + rnd.nextInt(32));
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - destPos) / blockSize * blockSize;
            work1.copy(a);
            work2.copy(a);
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = a.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug full copy found in test #"
                        + testCount + ": destPos = " + destPos + ", count = " + count + ", " + buf
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            work2.getData(0, workJA1);
            if (a instanceof BitArray) {
                for (int k = 0; k < len; k++)
                    if (((BitArray) work2).getBit(k) != ((boolean[]) workJA1)[k])
                        throw new AssertionError("The bug in getData found in test #"
                            + testCount + ": destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
            }
            myFill(workJA1, destPos, destPos + count, destPos, false);
            for (int ofs = destPos; ofs < destPos + count; ) {
                int maxCount = rnd.nextInt((int) buf.capacity() + 1) / blockSize * blockSize;
                buf.map(ofs, maxCount);
                int m = Math.min(buf.cnt(), destPos + count - ofs);
                if (a instanceof BitArray) { // a complex Java 1.7 bug was found here for bits
                    for (int k = 0; k < m; k++)
                        if (((BitArray) a).getBit(ofs + k) !=
                            PackedBitArrays.getBit((long[]) buf.data(), buf.from() + k))
                            throw new AssertionError("The bug in map (reading) found in test #"
                                + testCount + ": destPos = " + destPos + ", count = " + count + ", " + buf
                                + ", error found at " + k);
                }
                myFill(buf.data(), buf.from(), buf.from() + m, ofs, a instanceof BitArray);
                buf.force(buf.from(), buf.from() + m);
                if (a instanceof BitArray) {
                    for (int k = 0; k < m; k++)
                        if (((BitArray) work1).getBit(ofs + k)
                            != PackedBitArrays.getBit((long[]) buf.data(), buf.from() + k))
                            throw new AssertionError("The bug in map (writing) found in test #"
                                + testCount + ": destPos = " + destPos + ", count = " + count + ", " + buf
                                + ", error found at " + k);
                }
                ofs += m;
            }
            work2.setData(0, workJA1);
            if (a instanceof BitArray) {
                assert work2.length() == len;
                assert ((boolean[]) workJA1).length == len;
                for (int k = 0; k < len; k++)
                    if (((BitArray) work2).getBit(k) != ((boolean[]) workJA1)[k])
                        throw new AssertionError("The bug in setData found in test #"
                            + testCount + ": destPos = " + destPos + ", count = " + count
                            + ", error found at " + k);
            }
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug in map, get/setData or full copy found in test #"
                        + testCount + ": destPos = " + destPos + ", count = " + count + ", " + buf
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work1.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testLazyCopy(int ti, MemoryModel mm, boolean onlyUnresizable) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"newLazyCopy(Array array)\", "
            + "\"newUnresizableLazyCopy(Array array)\" and \"flushResources\" methods..."))
            return;
        Object zero = cmm.newUnresizableArray(a.elementType(), 1).getElement(0);
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            Array sa = a.subArr(srcPos, count);
            if (!onlyUnresizable && rnd.nextInt(4) == 0) {
                sa = sa.mutableClone(cmm); // allows newLazyCopy returning asCopyOnNextWrite
            }
            for (int m = 0; m < 8; m++) {
                work1.copy(sa);
                int lazyLen = m < 4 || onlyUnresizable ? count : rnd.nextInt(len + 1);
                if (lazyLen > count)
                    Arrays.zeroFill(work1.subArray(count, lazyLen));
                // Now work1 should be identical to the future "lazy" array
                boolean doFlush = m % 2 == 1;
                UpdatableArray lazy = m < 2 || onlyUnresizable ?
                    mm.newUnresizableLazyCopy(sa) :
                    mm.newLazyCopy(sa).length(lazyLen);
                if (doFlush) {
                    lazy.flushResources(null);
                }
                if (rnd.nextBoolean())
                    if (!work1.subArr(0, lazyLen).equals(lazy)) // no actualization here
                        throw new AssertionError("The bug A in new" + (m < 2 ? "Unresizable" : "")
                            + "LazyCopy" + (doFlush ? "+flushResources" : "") + " found in test #"
                            + testCount
                            + ": srcPos = " + srcPos + ", count = " + count
                            + ", new length = " + lazyLen
                            + ", source subarray: " + sa + ", lazy copy: " + lazy);
                if (!noSlowTests) {
                    for (int i = 0; i < 2 * lazyLen; i++) {
                        assert lazyLen > 0; // because i<2*lazyLen
                        int k = rnd.nextInt(lazyLen);
                        e1 = work1.getElement(k);
                        e2 = lazy.getElement(k);
                        if (!objectEquals(e1, e2)) {
                            // random access + actualization: the current version
                            // actualizes banks while reading 1 element
                            throw new AssertionError("The bug B in new" + (m < 2 ? "Unresizable" : "")
                                + "LazyCopy" + (doFlush ? "+flushResources" : "") + " found in test #"
                                + testCount
                                + ": srcPos = " + srcPos + ", count = " + count
                                + ", new length = " + lazyLen
                                + ", error found at " + k + ": " + e2 + " instead of " + e1
                                + ", source subarray: " + sa + ", lazy copy: " + lazy);
                        }
                        if (m >= 6 && i < 20) {
                            lazy.setElement(k, zero);
                            work1.setElement(k, zero);
                            if (!objectEquals(zero, e2 = lazy.getElement(k))) {
                                throw new AssertionError("The bug C in new" + (m < 2 ? "Unresizable" : "")
                                    + "LazyCopy" + (doFlush ? "+flushResources" : "") + " found in test #"
                                    + testCount
                                    + ": srcPos = " + srcPos + ", count = " + count
                                    + ", new length = " + lazyLen
                                    + ", error after zeroing found at " + k + ": " + e2
                                    + " instead of " + zero
                                    + ", source subarray: " + sa + ", lazy copy: " + lazy);
                            }
                            int pos = rnd.nextInt(lazyLen + 1) / blockSize * blockSize;
                            int cnt = rnd.nextInt(lazyLen + 1 - pos) / blockSize * blockSize;
                            if (!work1.subArr(pos, cnt).equals(lazy.subArr(pos, cnt))) {
                                throw new AssertionError("The bug D in new" + (m < 2 ? "Unresizable" : "")
                                    + "LazyCopy" + (doFlush ? "+flushResources" : "")
                                    + " found in test #" + testCount
                                    + ": srcPos = " + srcPos + ", count = " + count
                                    + ", new length = " + lazyLen
                                    + ", k = " + k + ", pos = " + pos + ", cnt = " + cnt
                                    + ", source subarray: " + sa + ", lazy copy: " + lazy);
                            }
                        }
                    }
                }
                if (!work1.subArr(0, lazyLen).equals(lazy))
                    throw new AssertionError("The bug E in new" + (m < 2 ? "Unresizable" : "")
                        + "LazyCopy" + (doFlush ? "+flushResources" : "")
                        + " found in test #" + testCount
                        + ": srcPos = " + srcPos + ", count = " + count + ", new length = " + lazyLen
                        + ", source subarray: " + sa + ", lazy copy: " + lazy);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testDefaultCopy(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"AbstractArray.defaultCopy\" method, two different arrays..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            Arrays.zeroFill(work1);
            Arrays.zeroFill(work2);
            try {
                mDefaultCopy.invoke(null, work1.subArr(destPos, count), a.subArr(srcPos, count));
            } catch (Exception ex) {
                throw new InternalError(ex.toString());
            }
            work2.subArr(destPos, count).copy(a.subArr(srcPos, count));
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work1.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testCopyInside(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"copy(long destIndex, long srcIndex, long count)\" method, inside a single array..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            work1.copy(a);
            work2.copy(a);
            if (!work1.equals(a))
                throw new AssertionError("The bug in simple copy found in test #" + testCount);
            work1.copy(destPos, srcPos, count);
            work2.subArr(destPos, count).copy(a.subArr(srcPos, count));
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work1.equals(work2))
                throw new AssertionError("The bug in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testCopySameArrayNoOverlap(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"copy(Array src)\" method, the same array, no overlap..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            if (Math.abs(srcPos - destPos) >= count) {
                work1.copy(a);
                work2.copy(a);
                work1.subArr(destPos, count).copy(work1.subArr(srcPos, count));
                work2.subArr(destPos, count).copy(a.subArr(srcPos, count));
                for (int k = 0; k < len; k++)
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                if (!work1.equals(work2))
                    throw new AssertionError("The bug in equals found in test #" + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testDefaultCopySameArrayNoOverlap(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"AbstractArray.defaultCopy\" method, "
            + "inside a single array, no overlap..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            if (Math.abs(srcPos - destPos) >= count) {
                work1.copy(a);
                work2.copy(a);
                try {
                    mDefaultCopy.invoke(null, work1.subArr(destPos, count), work1.subArr(srcPos, count));
                } catch (Exception ex) {
                    throw new InternalError(ex.toString());
                }
                work2.subArr(destPos, count).copy(a.subArr(srcPos, count));
                for (int k = 0; k < len; k++)
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug in copy found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                if (!work1.equals(work2))
                    throw new AssertionError("The bug in equals found in test #" + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testSwapDifferentArrays(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"swap(Array src)\" method, two different arrays..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            Arrays.zeroFill(work1);
            Arrays.zeroFill(work2);
            work3.copy(a);
            work4.copy(a);
            work1.subArr(destPos, count).swap(work3.subArr(srcPos, count));
            for (int k = 0; k < count; k++) {
                Object temp = work2.getElement(destPos + k);
                work2.setElement(destPos + k, work4.getElement(srcPos + k));
                work4.setElement(srcPos + k, temp);
            }
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug A in swap found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work1.equals(work2))
                throw new AssertionError("The bug A in equals found in test #" + testCount);
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work3.getElement(k), e2 = work4.getElement(k)))
                    throw new AssertionError("The bug B in swap found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work3.equals(work4))
                throw new AssertionError("The bug B in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testDefaultSwapDifferentArrays(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"AbstractArray.defaultSwap\" method, two different arrays..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            Arrays.zeroFill(work1);
            Arrays.zeroFill(work2);
            work3.copy(a);
            work4.copy(a);
            try {
                mDefaultSwap.invoke(null, work1.subArr(destPos, count), work3.subArr(srcPos, count));
            } catch (Exception ex) {
                throw new InternalError(ex.toString());
            }
            work2.subArr(destPos, count).swap(work4.subArr(srcPos, count));
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug A in swap found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work1.equals(work2))
                throw new AssertionError("The bug A in equals found in test #" + testCount);
            for (int k = 0; k < len; k++)
                if (!objectEquals(e1 = work3.getElement(k), e2 = work4.getElement(k)))
                    throw new AssertionError("The bug B in swap found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            if (!work3.equals(work4))
                throw new AssertionError("The bug B in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testSwapInsideNoOverlap(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"swap(long destIndex, long srcIndex, long count)\" method, "
            + "inside a single array, no overlap..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            if (Math.abs(srcPos - destPos) >= count) {
                work1.copy(a);
                work2.copy(a);
                work1.swap(destPos, srcPos, count);
                for (int k = 0; k < count; k++) {
                    Object temp = work2.getElement(destPos + k);
                    work2.setElement(destPos + k, work2.getElement(srcPos + k));
                    work2.setElement(srcPos + k, temp);
                }
                for (int k = 0; k < len; k++)
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug in swap found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                if (!work1.equals(work2))
                    throw new AssertionError("The bug in equals found in test #" + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testSwapSameArrayNoOverlap(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"swap(Array src)\" method, inside a single array, no overlap..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            if (Math.abs(srcPos - destPos) >= count) {
                work1.copy(a);
                work2.copy(a);
                work1.subArr(destPos, count).swap(work1.subArr(srcPos, count));
                work2.swap(destPos, srcPos, count);
                for (int k = 0; k < len; k++)
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug in swap found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                if (!work1.equals(work2))
                    throw new AssertionError("The bug in equals found in test #" + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testDefaultSwapSameArrayNoOverlap(int ti) {
        if (funcOnly)
            return;
        if (!title(ti, "Testing \"AbstractArray.defaultSwap\" method, "
            + "inside a single array, no overlap..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            if (Math.abs(srcPos - destPos) >= count) {
                work1.copy(a);
                work2.copy(a);
                try {
                    mDefaultSwap.invoke(null, work1.subArr(destPos, count), work1.subArr(srcPos, count));
                } catch (Exception ex) {
                    throw new InternalError(ex.toString());
                }
                work2.swap(destPos, srcPos, count);
                for (int k = 0; k < len; k++)
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug in swap found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", error found at " + k + ": " + e1 + " instead of " + e2);
                if (!work1.equals(work2))
                    throw new AssertionError("The bug in equals found in test #" + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testPackBits(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.packBitsXXX\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            double threshold =
                rnd.nextBoolean() ? -3e9 + 6e9 * rnd.nextDouble() :
                    rnd.nextBoolean() ? -0.5e20 + 1e20 * rnd.nextDouble() :
                        rnd.nextBoolean() ? -250 + 500 * rnd.nextDouble() :
                            rnd.nextBoolean() ? rnd.nextInt(2) : rnd.nextDouble();
            if (rnd.nextBoolean())
                threshold = (int) threshold;
            PArray pa = (PArray) a.subArr(srcPos, count);
            //[[Repeat() (Greater|Less) ==> $1OrEqual;;
            //  (>|<)(\s*threshold) ==> $1=$2 ]]
            workBits.copy(bits);
            Arrays.packBitsGreater(workBits.subArr(destPos, count), pa, threshold);
            for (int k = 0; k < len; k++) {
                boolean b = workBits.getBit(k);
                Object vReq = k >= destPos && k < destPos + count ? pa.getDouble(k - destPos) : bits.getBit(k);
                boolean bReq = k >= destPos && k < destPos + count ?
                    pa.getDouble(k - destPos) > threshold :
                    bits.getBit(k);
                if (b != bReq)
                    throw new AssertionError("The bug in packBitsGreater found in test #" + testCount
                        + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", threshold = " + threshold
                        + ", error found at " + k + ": "
                        + b + " instead of " + bReq + " for " + vReq);
            }
            workBits.copy(bits);
            Arrays.packBitsLess(workBits.subArr(destPos, count), pa, threshold);
            for (int k = 0; k < len; k++) {
                boolean b = workBits.getBit(k);
                Object vReq = k >= destPos && k < destPos + count ? pa.getDouble(k - destPos) : bits.getBit(k);
                boolean bReq = k >= destPos && k < destPos + count ?
                    pa.getDouble(k - destPos) < threshold :
                    bits.getBit(k);
                if (b != bReq)
                    throw new AssertionError("The bug in packBitsLess found in test #" + testCount
                        + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", threshold = " + threshold
                        + ", error found at " + k + ": "
                        + b + " instead of " + bReq + " for " + vReq);
            }
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
            workBits.copy(bits);
            Arrays.packBitsGreaterOrEqual(workBits.subArr(destPos, count), pa, threshold);
            for (int k = 0; k < len; k++) {
                boolean b = workBits.getBit(k);
                Object vReq = k >= destPos && k < destPos + count ? pa.getDouble(k - destPos) : bits.getBit(k);
                boolean bReq = k >= destPos && k < destPos + count ?
                    pa.getDouble(k - destPos) >= threshold :
                    bits.getBit(k);
                if (b != bReq)
                    throw new AssertionError("The bug in packBitsGreaterOrEqual found in test #" + testCount
                        + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", threshold = " + threshold
                        + ", error found at " + k + ": "
                        + b + " instead of " + bReq + " for " + vReq);
            }
            workBits.copy(bits);
            Arrays.packBitsLessOrEqual(workBits.subArr(destPos, count), pa, threshold);
            for (int k = 0; k < len; k++) {
                boolean b = workBits.getBit(k);
                Object vReq = k >= destPos && k < destPos + count ? pa.getDouble(k - destPos) : bits.getBit(k);
                boolean bReq = k >= destPos && k < destPos + count ?
                    pa.getDouble(k - destPos) <= threshold :
                    bits.getBit(k);
                if (b != bReq)
                    throw new AssertionError("The bug in packBitsLessOrEqual found in test #" + testCount
                        + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", threshold = " + threshold
                        + ", error found at " + k + ": "
                        + b + " instead of " + bReq + " for " + vReq);
            }
            //[[Repeat.AutoGeneratedEnd]]
            showProgress(testCount, numberOfTests);
        }
    }

    private void testUnpackBits(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.unpackBits\", \"Arrays.unpacUnitkBits\" "
            + "and \"Arrays.unpackZeroBits\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            double filler0 =
                rnd.nextBoolean() ? -3e9 + 6e9 * rnd.nextDouble() :
                    rnd.nextBoolean() ? -0.5e20 + 1e20 * rnd.nextDouble() :
                        rnd.nextBoolean() ? -500 + 1000 * rnd.nextDouble() :
                            rnd.nextBoolean() ? rnd.nextInt(2) : rnd.nextDouble();
            double filler1 =
                rnd.nextBoolean() ? -3e9 + 6e9 * rnd.nextDouble() :
                    rnd.nextBoolean() ? -0.5e20 + 1e20 * rnd.nextDouble() :
                        rnd.nextBoolean() ? -500 + 1000 * rnd.nextDouble() :
                            rnd.nextBoolean() ? rnd.nextInt(2) : rnd.nextDouble();
            work2.copy(a);
            for (int k = 0; k < count; k++) {
                ((UpdatablePArray) work2).setDouble(destPos + k, bits.getBit(srcPos + k) ? filler1 : filler0);
            }
            for (char m = 'A'; m <= 'B'; m++) {
                work1.copy(a);
                Arrays.unpackBits((UpdatablePArray) work1.subArr(destPos, count),
                    (BitArray) (m == 'A' ? bits : bitsSimple).subArr(srcPos, count), filler0, filler1);
                for (int k = 0; k < len; k++) {
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug " + m + " in unpackBits found in test #" + testCount
                            + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", filler0 = " + filler0 + ", filler1 = " + filler1
                            + ", error found at " + k + ": "
                            + ((PArray) work1).getDouble(k) + " instead of " + ((PArray) work2).getDouble(k));
                }
            }
            work2.copy(a);
            for (int k = 0; k < count; k++) {
                if (bits.getBit(srcPos + k))
                    ((UpdatablePArray) work2).setDouble(destPos + k, filler1);
            }
            for (char m = 'A'; m <= 'B'; m++) {
                work1.copy(a);
                Arrays.unpackUnitBits((UpdatablePArray) work1.subArr(destPos, count),
                    (BitArray) (m == 'A' ? bits : bitsSimple).subArr(srcPos, count), filler1);
                for (int k = 0; k < len; k++) {
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug " + m + " in unpackUnitBits found in test #" + testCount
                            + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", filler1 = " + filler1
                            + ", error found at " + k + ": "
                            + ((PArray) work1).getDouble(k) + " instead of " + ((PArray) work2).getDouble(k));
                }
            }
            work2.copy(a);
            for (int k = 0; k < count; k++) {
                if (!bits.getBit(srcPos + k))
                    ((UpdatablePArray) work2).setDouble(destPos + k, filler0);
            }
            for (char m = 'A'; m <= 'B'; m++) {
                work1.copy(a);
                Arrays.unpackZeroBits((UpdatablePArray) work1.subArr(destPos, count),
                    (BitArray) (m == 'A' ? bits : bitsSimple).subArr(srcPos, count), filler0);
                for (int k = 0; k < len; k++) {
                    if (!objectEquals(e1 = work1.getElement(k), e2 = work2.getElement(k)))
                        throw new AssertionError("The bug " + m + " in unpackZeroBits found in test #" + testCount
                            + ": srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                            + ", filler0 = " + filler0
                            + ", error found at " + k + ": "
                            + ((PArray) work1).getDouble(k) + " instead of " + ((PArray) work2).getDouble(k));
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testZeroFillAndIsZeroFilled(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.zeroFill\" and \"PArray.isZeroFilled\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextBoolean() ? srcPos : rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            work1.copy(a);
            Arrays.zeroFill(work1.subArr(destPos, count));
            for (int k = 0; k < len; k++) {
                double v = k >= destPos && k < destPos + count ? 0.0 : ((PArray) a).getDouble(k);
                if (((PArray) work1).getDouble(k) != v)
                    throw new AssertionError("The bug in zeroFill found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + ((PArray) work1).getDouble(k) + " instead of " + v);
            }
            PArray pa = (PArray) (work1.subArr(srcPos, count));
            boolean zero = true;
            for (int k = 0; k < count; k++) {
                if (pa.getDouble(k) != 0.0) {
                    zero = false;
                    break;
                }
            }
            if (zero != pa.isZeroFilled())
                throw new AssertionError("The bug in isZeroFilled found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testSerialization(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.copyArrayToBytes/copyBytesToArray\" methods..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int destPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos, destPos)) / blockSize * blockSize;
            work1.copy(a);
            PArray srcPArray = (PArray) a.subArr(srcPos, count);
            UpdatablePArray destPArray = (UpdatablePArray) work1.subArr(destPos, count);
            byte[] bytes = rnd.nextBoolean() ?
                new byte[(int) (a instanceof BitArray ? (srcPArray.length() + 7) / 8 : Arrays.sizeOf(srcPArray))] :
                null;
            ByteOrder byteOrder = rnd.nextBoolean() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            bytes = Arrays.copyArrayToBytes(bytes, srcPArray, byteOrder);
            for (int k = 0; k < count; k++) {
                boolean equal;
                if (srcPArray instanceof BitArray) {
                    equal = ((BitArray) srcPArray).getBit(k) == ((bytes[k / 8] & (1 << (k % 8))) != 0);
                } else if (srcPArray instanceof ByteArray) {
                    equal = (byte) ((ByteArray) srcPArray).getByte(k) == bytes[k];
                } else if (srcPArray instanceof CharArray) {
                    equal = ((CharArray) srcPArray).getChar(k) ==
                        (byteOrder == ByteOrder.LITTLE_ENDIAN ?
                            (bytes[2 * k] & 0xFF) + ((bytes[2 * k + 1] & 0xFF) << 8) :
                            (bytes[2 * k + 1] & 0xFF) + ((bytes[2 * k] & 0xFF) << 8));
                } else if (srcPArray instanceof ShortArray) {
                    equal = ((ShortArray) srcPArray).getInt(k) ==
                        (byteOrder == ByteOrder.LITTLE_ENDIAN ?
                            (bytes[2 * k] & 0xFF) + ((bytes[2 * k + 1] & 0xFF) << 8) :
                            (bytes[2 * k + 1] & 0xFF) + ((bytes[2 * k] & 0xFF) << 8));
                } else if (srcPArray instanceof IntArray) {
                    equal = ((IntArray) srcPArray).getInt(k) ==
                        (byteOrder == ByteOrder.LITTLE_ENDIAN ?
                            (bytes[4 * k] & 0xFF)
                                + ((bytes[4 * k + 1] & 0xFF) << 8)
                                + ((bytes[4 * k + 2] & 0xFF) << 16)
                                + ((bytes[4 * k + 3] & 0xFF) << 24) :
                            (bytes[4 * k + 3] & 0xFF)
                                + ((bytes[4 * k + 2] & 0xFF) << 8)
                                + ((bytes[4 * k + 1] & 0xFF) << 16)
                                + ((bytes[4 * k] & 0xFF) << 24));
                } else if (srcPArray instanceof LongArray) {
                    equal = ((LongArray) srcPArray).getLong(k) ==
                        (byteOrder == ByteOrder.LITTLE_ENDIAN ?
                            ((long) bytes[8 * k] & 0xFF)
                                + (((long) bytes[8 * k + 1] & 0xFF) << 8)
                                + (((long) bytes[8 * k + 2] & 0xFF) << 16)
                                + (((long) bytes[8 * k + 3] & 0xFF) << 24)
                                + (((long) bytes[8 * k + 4] & 0xFF) << 32)
                                + (((long) bytes[8 * k + 5] & 0xFF) << 40)
                                + (((long) bytes[8 * k + 6] & 0xFF) << 48)
                                + (((long) bytes[8 * k + 7] & 0xFF) << 56) :
                            ((long) bytes[8 * k + 7] & 0xFF)
                                + (((long) bytes[8 * k + 6] & 0xFF) << 8)
                                + (((long) bytes[8 * k + 5] & 0xFF) << 16)
                                + (((long) bytes[8 * k + 4] & 0xFF) << 24)
                                + (((long) bytes[8 * k + 3] & 0xFF) << 32)
                                + (((long) bytes[8 * k + 2] & 0xFF) << 40)
                                + (((long) bytes[8 * k + 1] & 0xFF) << 48)
                                + (((long) bytes[8 * k] & 0xFF) << 56));
                } else if (srcPArray instanceof FloatArray) {
                    equal = Float.floatToRawIntBits(((FloatArray) srcPArray).getFloat(k)) ==
                        (byteOrder == ByteOrder.LITTLE_ENDIAN ?
                            (bytes[4 * k] & 0xFF)
                                + ((bytes[4 * k + 1] & 0xFF) << 8)
                                + ((bytes[4 * k + 2] & 0xFF) << 16)
                                + ((bytes[4 * k + 3] & 0xFF) << 24) :
                            (bytes[4 * k + 3] & 0xFF)
                                + ((bytes[4 * k + 2] & 0xFF) << 8)
                                + ((bytes[4 * k + 1] & 0xFF) << 16)
                                + ((bytes[4 * k] & 0xFF) << 24));
                } else if (srcPArray instanceof DoubleArray) {
                    equal = Double.doubleToRawLongBits(((DoubleArray) srcPArray).getDouble(k)) ==
                        (byteOrder == ByteOrder.LITTLE_ENDIAN ?
                            ((long) bytes[8 * k] & 0xFF)
                                + (((long) bytes[8 * k + 1] & 0xFF) << 8)
                                + (((long) bytes[8 * k + 2] & 0xFF) << 16)
                                + (((long) bytes[8 * k + 3] & 0xFF) << 24)
                                + (((long) bytes[8 * k + 4] & 0xFF) << 32)
                                + (((long) bytes[8 * k + 5] & 0xFF) << 40)
                                + (((long) bytes[8 * k + 6] & 0xFF) << 48)
                                + (((long) bytes[8 * k + 7] & 0xFF) << 56) :
                            ((long) bytes[8 * k + 7] & 0xFF)
                                + (((long) bytes[8 * k + 6] & 0xFF) << 8)
                                + (((long) bytes[8 * k + 5] & 0xFF) << 16)
                                + (((long) bytes[8 * k + 4] & 0xFF) << 24)
                                + (((long) bytes[8 * k + 3] & 0xFF) << 32)
                                + (((long) bytes[8 * k + 2] & 0xFF) << 40)
                                + (((long) bytes[8 * k + 1] & 0xFF) << 48)
                                + (((long) bytes[8 * k] & 0xFF) << 56));
                } else
                    throw new AssertionError("Illegal type");
                if (!equal)
                    throw new AssertionError("The bug in copyArrayToBytes found in test #" + testCount + ": "
                        + "byteOrder = " + byteOrder
                        + ", srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count
                        + ", error found at " + k + ": " + srcPArray.getDouble(k));
            }
            Arrays.copyBytesToArray(destPArray, bytes, byteOrder);
            if (!srcPArray.equals(destPArray))
                throw new AssertionError("The bug in copyBytesToArray found in test #" + testCount + ": "
                    + "byteOrder = " + byteOrder
                    + ", srcPos = " + srcPos + ", destPos = " + destPos + ", count = " + count + " ("
                    + Arrays.toString(srcPArray, ", ", 200) + " and " + Arrays.toString(destPArray, ", ", 200) + ")");
            showProgress(testCount, numberOfTests);
        }
    }

    private void testRangeOf(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.rangeOf\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            PArray pa = (PArray) (a.subArr(srcPos, count));
            Arrays.MinMaxInfo info = new Arrays.MinMaxInfo();
            int numberOfTasks = DefaultThreadPoolFactory.getDefaultThreadPoolFactory().recommendedNumberOfTasks(pa);
            Range range = Arrays.rangeOf(pa, info);
            if (count > 0) {
                for (int k = 0; k < info.indexOfMin(); k++)
                    if (numberOfTasks <= 1 ? pa.getDouble(k) <= range.min()
                        : pa.getDouble(k) < range.min())
                        throw new AssertionError("The bug A in rangeOf found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", count = " + count + ", " + info
                            + ", pa.getDouble(" + k + ") = " + pa.getDouble(k));
                if (pa.getDouble(info.indexOfMin()) != range.min())
                    throw new AssertionError("The bug B in rangeOf found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count + ", " + info);
                for (int k = (int) info.indexOfMin() + 1; k < count; k++)
                    if (pa.getDouble(k) < range.min())
                        throw new AssertionError("The bug C in rangeOf found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", count = " + count + ", " + info);
                for (int k = 0; k < info.indexOfMax(); k++)
                    if (numberOfTasks <= 1 ? pa.getDouble(k) >= range.max()
                        : pa.getDouble(k) > range.max())
                        throw new AssertionError("The bug D in rangeOf found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", count = " + count + ", " + info
                            + ", pa.getDouble(" + k + ") = " + pa.getDouble(k));
                if (pa.getDouble(info.indexOfMax()) != range.max())
                    throw new AssertionError("The bug E in rangeOf found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count + ", " + info);
                for (int k = (int) info.indexOfMax() + 1; k < count; k++)
                    if (pa.getDouble(k) > range.max())
                        throw new AssertionError("The bug F in rangeOf found in test #" + testCount + ": "
                            + "srcPos = " + srcPos + ", count = " + count + ", " + info);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testSumOf(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.sumOf\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            double sum1 = Arrays.sumOf((PArray) a.subArr(srcPos, count));
            double sum2 = 0;
            for (long k = srcPos; k < srcPos + count; k++)
                sum2 += ((PArray) a).getDouble(k);
            if (sum1 != sum2)
                throw new AssertionError("The bug in sumOf found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ": " + sum1 + " instead of " + sum2);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testPreciseSumOf(int ti) {
        if (!(a instanceof PFixedArray))
            return;
        if (!title(ti, "Testing \"Arrays.preciseSumOf\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            long sum1 = Arrays.preciseSumOf((PFixedArray) a.subArr(srcPos, count), false);
            long sum2 = 0;
            for (long k = srcPos; k < srcPos + count; k++)
                sum2 += ((PFixedArray) a).getLong(k);
            if (sum1 != sum2)
                throw new AssertionError("The bug in preciseSumOf found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ": " + sum1 + " instead of " + sum2);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testHistogramOf(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.histogramOf\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            long[] histogram1 = new long[1 + rnd.nextInt(10000)];
            long[] histogram2 = new long[histogram1.length];
            PArray pa = (PArray) a.subArr(srcPos, count);
            Range range = Arrays.rangeOf(pa);
            double from = rnd.nextBoolean() ? range.min() :
                range.min() - range.size() + 3 * rnd.nextDouble() * range.size();
            double to = rnd.nextBoolean() ? range.max() :
                range.min() - range.size() + 3 * rnd.nextDouble() * range.size();
            boolean allInside1 = Arrays.histogramOf(pa, histogram1, from, to);
            boolean allInside2 = true;
            double mult = histogram2.length / (to - from);
            if (from < to) {
                for (long k = 0; k < count; k++) {
                    double v = pa.getDouble(k);
                    v = (v - from) * mult;
                    int m = (int) StrictMath.floor(v);
                    if (m >= 0 && m < histogram1.length)
                        histogram2[m]++;
                    else
                        allInside2 = false;
                }
            } else {
                allInside2 = false;
            }
            for (int k = 0; k < histogram1.length; k++)
                if (histogram1[k] != histogram2[k])
                    throw new AssertionError("The bug A in histogramOf found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count
                        + ", from = " + from + ", to = " + to + ", histogram.length = " + histogram1.length
                        + ", range = " + range + ", multiplier = " + mult + ", 0*multiplier => " + (int) (0.0 * mult)
                        + ": histogram[" + k + "] = " + histogram1[k] + " instead of " + histogram2[k]);
            if (allInside1 != allInside2)
                throw new AssertionError("The bug B in histogramOf found in test #" + testCount + ": "
                    + "srcPos = " + srcPos + ", count = " + count
                    + ", from = " + from + ", to = " + to + ", histogram.length = " + histogram1.length
                    + ", range = " + range + ", multiplier = " + mult + ", 0*multiplier => " + (int) (0.0 * mult)
                    + ": " + allInside1 + " instead of " + allInside2);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testAsConcatenation(int ti) {
        if (funcOnly)
            return;
        if (mwork1 == null)
            return;
        if (!title(ti, "Testing \"Arrays.asConcatenation\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            Array[] arrays = new Array[1 + rnd.nextInt(4)];
            long sumLen = 0;
            for (int m = 0; m < arrays.length; m++) {
                int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
                int count = rnd.nextInt(Math.min(len + 1 - (int) sumLen, len + 1 - srcPos))
                    / blockSize * blockSize;
                sumLen += count;
                arrays[m] = a.subArr(srcPos, count);
            }
            Array ac = Arrays.asConcatenation(arrays);
            if (sumLen > a.length())
                throw new AssertionError("Too large sum of lengths");
            mwork1.length(0);
            for (int k = 0; k < arrays.length; k++) {
                mwork1.append(arrays[k]);
            }
            if (ac.length() != sumLen)
                throw new AssertionError("The bug A in asConcatenation found in test #" + testCount
                    + ": length = " + ac.length() + " instead of " + sumLen);
            if (mwork1.length() != sumLen)
                throw new AssertionError("The bug B in append found in test #" + testCount + ": "
                    + "length = " + mwork1.length() + " instead of " + sumLen);
            for (int m = 0; m < 5; m++) {
                Array sac, swork;
                int count;
                if (m == 0) {
                    count = (int) ac.length();
                    sac = ac;
                    swork = mwork1;
                } else {
                    int srcPos = rnd.nextInt((int) sumLen + 1) / blockSize * blockSize;
                    count = rnd.nextInt((int) sumLen + 1 - srcPos) / blockSize * blockSize;
                    sac = ac.subArr(srcPos, count);
                    swork = mwork1.subArr(srcPos, count);
                    if (sac.length() != count)
                        throw new AssertionError("The bug C in append found in test #" + testCount
                            + ": subarray length = " + sac.length() + " instead of " + count);
                }
                UpdatableArray sacClone = cmm.newUnresizableArray(sac);
                Arrays.copy(null, sacClone, sac);
                for (int k = 0; k < count; k++) {
                    if (!objectEquals(e1 = sac.getElement(k), e2 = swork.getElement(k)))
                        throw new AssertionError("The bug D in asConcatenation found in test #" +
                            testCount
                            + " (m=" + m + ") at " + k + ": " + e1 + " instead of " + e2
                            + " (" + sac + ")");
                    if (!objectEquals(e1 = sacClone.getElement(k), e2))
                        throw new AssertionError("The bug E in asConcatenation found in test #" +
                            testCount
                            + " (m=" + m + ") at " + k + ": " + e1 + " instead of " + e2
                            + " (" + sacClone + ")");
                }
                if (!sac.equals(swork))
                    throw new AssertionError("The bug D' in equals found in test #" + testCount);
                if (!sacClone.equals(swork))
                    throw new AssertionError("The bug E' in equals found in test #" + testCount);
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testAsShifted(int ti) {
        if (!title(ti, "Testing \"Arrays.asShifted\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            int shift = rnd.nextInt();
            work1.copy(a);
            UpdatableArray a = work1.subArr(srcPos, count);
            Array as = Arrays.asShifted(a, shift);
            UpdatableArray asClone = cmm.newUnresizableArray(as);
            Arrays.copy(null, asClone, as);
            for (int m = 0; m < 5; m++) {
                Array sas;
                final int cnt, pos;
                if (m == 0) {
                    pos = 0;
                    cnt = (int) as.length();
                    sas = as;
                } else {
                    pos = rnd.nextInt(count + 1) / blockSize * blockSize;
                    cnt = rnd.nextInt(count + 1 - pos) / blockSize * blockSize;
                    sas = as.subArr(pos, cnt);
                    if (sas.length() != cnt)
                        throw new AssertionError("The bug A in asShifted found in test #" + testCount
                            + ": subarray length = " + sas.length() + " instead of " + cnt);
                }
                UpdatableArray sasClone = cmm.newUnresizableArray(as);
                Arrays.copy(null, sasClone, sas);
                for (int k = pos; k < pos + cnt; k++) {
                    e1 = sas.getElement(k - pos);
                    long kShifted = (k - shift) % count;
                    if (kShifted < 0)
                        kShifted += count;
                    e2 = a.getElement(kShifted);
                    if (!objectEquals(e1, e2)) {
                        throw new AssertionError("The bug B in asShifted found in test #" + testCount
                            + " (m=" + m + "): srcPos = " + srcPos + ", count = " + count);
                    }
                    if (!objectEquals(sasClone.getElement(k - pos), e1))
                        throw new AssertionError("The bug C in asShifted found in test #" + testCount
                            + " (m=" + m + "): srcPos = " + srcPos + ", count = " + count);
                }
                if (!sas.equals(asClone.subArr(pos, cnt)))
                    throw new AssertionError("The bug D in asShifted found in test #" + testCount
                        + " (m=" + m + "): srcPos = " + srcPos + ", count = " + count);
            }
            Array ass = Arrays.asShifted(as, -shift);
            if (!a.equals(ass))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            ass = Arrays.asShifted(asClone, -shift);
            if (!a.equals(ass))
                throw new AssertionError("The bug E in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testTileMatrix(int ti) { // should be tested also with some large maxTempJavaMemory
        if (funcOnly) {
            return;
        }
        if (!title(ti, "Testing \"Matrix.tile\" method...")) {
            return;
        }
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            long[] dim = new long[1 + rnd.nextInt(4)];
            int product = 1;
            for (int i = 0; i < dim.length; i++) {
                int lim = product == 0 ? len + 1 : len / product + 1;
                // - at least one zero dimension should lead to trivial results
                lim = Math.min(lim, rnd.nextInt(4) == 0 ? len :
                    rnd.nextBoolean() ? (int) Math.sqrt(len) : (int) Math.cbrt(len));
                dim[i] = (rnd.nextInt(3) > 0 && lim > 10 ? lim - 1 - rnd.nextInt(lim / 10) : rnd.nextInt(lim))
                    / blockSize * blockSize;
                product *= dim[i];
                assert product <= len;
            }
            Matrix<? extends UpdatableArray> matr = Matrices.matrix(work1.subArr(0, product), dim);
            work1.copy(a);
            work2.copy(a);
            long[] tileDim;
            tileDim = new long[dim.length];
            Matrix<? extends UpdatableArray> tileMatr;
            if (rnd.nextInt(5) == 0) {
                if (rnd.nextBoolean()) {
                    for (int i = 0; i < tileDim.length; i++) {
                        tileDim[i] = 1 << rnd.nextInt(6);
                    }
                } else {
                    for (int i = 0; i < tileDim.length; i++) {
                        tileDim[i] = 1 + rnd.nextInt(100);
                    }
                }
                if (rnd.nextBoolean()) {
                    for (int i = 0, iMax = rnd.nextInt(4); i < dim.length && i < iMax; i++) {
                        if (dim[i] != 0) {
                            tileDim[i] = dim[i]; // testing collapsing lowest (first) dimensions
                        }
                    }
                }
                tileMatr = matr.tile(tileDim);
            } else {
                tileMatr = matr.tile();
                tileDim = tileMatr.tileDimensions();
            }
            long[] coords = new long[dim.length];
            long[] tileCoords = new long[dim.length];
            long[] coordsInTile = new long[dim.length];
            long[] baseOrTileDim = new long[dim.length];
            long[] cStart = new long[dim.length];
            long[] currentTileDim = new long[dim.length];
//            System.out.println(JArrays.toString(dim, ";", 100) + " by " + JArrays.toString(tileDim, ";", 100));
            for (int k = 0, n = (int) matr.size(); k < n; k++) {
                matr.coordinates(k, coords);
                for (int i = 0; i < dim.length; i++) {
                    tileCoords[i] = coords[i] / tileDim[i];
                    coordsInTile[i] = coords[i] % tileDim[i];
                    cStart[i] = coords[i] - coordsInTile[i];
                    currentTileDim[i] = Math.min(tileDim[i], dim[i] - cStart[i]);
                }
                Matrix<BitArray> inTileIndexer = Matrices.asCoordFuncMatrix(
                    ConstantFunc.getInstance(0), BitArray.class, currentTileDim);
                // We need to calculate previousVolume =
                //     = dim[0] * dim[1] * ... * dim[n-3] * dim[n-2] * cStart[n-1]
                //     + dim[0] * dim[1] * ... * dim[n-3] * cStart[n-2] * currentTileDim[n-1]
                //     + . . .
                //     + cStart[0] * currentTileDim[1] * ... * currentTileDim[n-1]
                System.arraycopy(dim, 0, baseOrTileDim, 0, dim.length);
                baseOrTileDim[dim.length - 1] = cStart[dim.length - 1];
                long previousVolume = Arrays.longMul(baseOrTileDim);
                for (int i = dim.length - 2; i >= 0; i--) {
                    baseOrTileDim[i + 1] = currentTileDim[i + 1];
                    baseOrTileDim[i] = cStart[i];
                    previousVolume += Arrays.longMul(baseOrTileDim);
                }
                long index = previousVolume + inTileIndexer.index(coordsInTile);
                e1 = tileMatr.array().getElement(k);
                e2 = matr.array().getElement(index);
                if (!objectEquals(e1, e2))
                    throw new AssertionError("The bug A in tiling found in test #" + testCount + ": "
                        + "dim = " + JArrays.toString(dim, ";", 100)
                        + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                        + ", k = " + k + ", index in source = " + index
                        + ", coords = " + JArrays.toString(coords, ";", 100)
                        + ", tileCoords = " + JArrays.toString(tileCoords, ";", 100)
                        + ", coordsInTile = " + JArrays.toString(coordsInTile, ";", 100)
                        + ", " + tileMatr
                        + ": " + e1 + " instead of " + e2);
                tileMatr.array().setElement(k, a.getElement(0));
                e2 = matr.array().getElement(index);
                if (!objectEquals(a.getElement(0), e2))
                    throw new AssertionError("The bug B in tiling found in test #" + testCount + ": "
                        + "dim = " + JArrays.toString(dim, ";", 100)
                        + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                        + ", k = " + k + ", index in source = " + index
                        + ", coords = " + JArrays.toString(coords, ";", 100)
                        + ", tileCoords = " + JArrays.toString(tileCoords, ";", 100)
                        + ", coordsInTile = " + JArrays.toString(coordsInTile, ";", 100)
                        + ", " + tileMatr
                        + ": " + e2 + " instead of " + a.getElement(0));
                tileMatr.array().setElement(k, e1);
                e2 = matr.array().getElement(index);
                if (!objectEquals(e1, e2))
                    throw new AssertionError("The bug C in tiling found in test #" + testCount + ": "
                        + "dim = " + JArrays.toString(dim, ";", 100)
                        + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                        + ", k = " + k + ", index in source = " + index
                        + ", coords = " + JArrays.toString(coords, ";", 100)
                        + ", tileCoords = " + JArrays.toString(tileCoords, ";", 100)
                        + ", coordsInTile = " + JArrays.toString(coordsInTile, ";", 100)
                        + ", " + tileMatr
                        + ": " + e2 + " instead of " + e1);
            }
            int pos = rnd.nextInt((int) matr.size() + 1) / blockSize * blockSize;
            int cnt = rnd.nextInt((int) matr.size() + 1 - pos) / blockSize * blockSize;
            work2.copy(tileMatr.array().subArr(pos, cnt)); // block copying into tiled matrix
            for (int k = 0; k < cnt; k++) {
                if (!objectEquals(e1 = work2.getElement(k), e2 = tileMatr.array().getElement(pos + k)))
                    throw new AssertionError("The bug D in tiling found in test #" + testCount + ": "
                        + "dim = " + JArrays.toString(dim, ";", 100)
                        + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                        + ", k = " + k
                        + ", " + tileMatr
                        + ": " + e1 + " instead of " + e2);
            }
            tileMatr.array().subArr(pos, cnt).copy(a); // block copying from tiled matrix
            for (int k = 0; k < cnt; k++) {
                if (!objectEquals(e1 = tileMatr.array().getElement(pos + k), e2 = a.getElement(k)))
                    throw new AssertionError("The bug E in tiling found in test #" + testCount + ": "
                        + "dim = " + JArrays.toString(dim, ";", 100)
                        + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                        + ", k = " + k
                        + ", " + tileMatr
                        + ": " + e1 + " instead of " + e2);
            }
            work1.copy(a); // filling tiled matrix by random values
            if (rnd.nextBoolean()) {
                cnt = (int) a.length(); // >tileMatr.size()
            } else if (rnd.nextBoolean()) {
                cnt = (int) tileMatr.size(); // =tileMatr.size()
            } // else <tileMatr.size()
            int minLen = Math.min(cnt, (int) tileMatr.size());
            Arrays.CopyStatus status = Arrays.copy(null, tileMatr.array(), a.subArr(0, cnt)); // tiling
            if (!tileMatr.array().subArr(0, minLen).equals(a.subArr(0, minLen)))
                throw new AssertionError("The bug F in tiling found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                    + ", non-tiled array length = " + cnt + ", " + tileMatr
                    + ", copying method " + status
                    + ", " + tileMatr
                    + ", " + Arrays.toString(tileMatr.array().subArr(0, minLen), ",", 200)
                    + " instead of " + Arrays.toString(a.subArr(0, minLen), ",", 200));
            work2.copy(work1); // filling work2 by shuffled values
            status = Arrays.copy(null, work2.subArr(0, cnt), tileMatr.array()); // untiling
            if (!work2.subArr(0, minLen).equals(tileMatr.array().subArr(0, minLen)))
                throw new AssertionError("The bug G in tiling found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                    + ", non-tiled array length = " + cnt + ", " + tileMatr
                    + ", copying method " + status
                    + ", " + tileMatr
                    + ", " + Arrays.toString(work2.subArr(0, minLen), ",", 200)
                    + " instead of " + Arrays.toString(tileMatr.array().subArr(0, minLen), ",", 200));
            cnt = (int) tileMatr.array().length();
            tileMatr.array().copy(a);
            int lowIndex = -2 + rnd.nextInt(len + 4);
            int highIndex = -2 + rnd.nextInt(len + 4);
            long index, requiredIndex, lastIndex, requiredLastIndex;
            if (a instanceof PArray) {
                double value = ((PArray) a).getDouble(minLen / 2);
                index = ((PArray) tileMatr.array()).indexOf(lowIndex, highIndex, value);
                lastIndex = ((PArray) tileMatr.array()).lastIndexOf(lowIndex, highIndex, value);
                requiredIndex = ((PArray) a.subArr(0, cnt)).indexOf(lowIndex, highIndex, value);
                requiredLastIndex = ((PArray) a.subArr(0, cnt)).lastIndexOf(lowIndex, highIndex, value);
            } else {
                Object value = a.getElement(minLen / 2);
                ObjectArray<?> oa = ((ObjectArray<?>) tileMatr.array());
                index = oa.cast(Object.class).indexOf(lowIndex, highIndex, value);
                lastIndex = oa.cast(Object.class).lastIndexOf(lowIndex, highIndex, value);
                oa = (ObjectArray<?>) a.subArr(0, cnt);
                requiredIndex = oa.cast(Object.class).indexOf(lowIndex, highIndex, value);
                requiredLastIndex = oa.cast(Object.class).lastIndexOf(lowIndex, highIndex, value);
            }
            if (index != requiredIndex) {
                throw new AssertionError("The bug H in tiling found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                    + ", region = " + lowIndex + ".." + highIndex
                    + ", " + tileMatr
                    + ": index " + index + " instead of " + requiredIndex);
            }
            if (lastIndex != requiredLastIndex) {
                throw new AssertionError("The bug I in tiling found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                    + ", region = " + lowIndex + ".." + highIndex
                    + ", " + tileMatr
                    + ": last index " + lastIndex + " instead of " + requiredLastIndex);
            }
            pos = rnd.nextInt((int) tileMatr.size() + 1);
            cnt = rnd.nextInt((int) tileMatr.size() + 1 - pos);
            tileMatr.array().copy(a);
            work2.copy(a);
            if (a instanceof PArray) {
                double value = ((PArray) a).getDouble(minLen / 2);
                if (rnd.nextBoolean()) {
                    ((UpdatablePArray) tileMatr.array()).fill(pos, cnt, value);
                } else {
                    ((UpdatablePArray) tileMatr.array().subArr(pos, cnt)).fill(value);
                }
                ((UpdatablePArray) work2).fill(pos, cnt, value);
            } else {
                Object value = a.getElement(minLen / 2);
                if (rnd.nextBoolean()) {
                    ((UpdatableObjectArray<?>) tileMatr.array()).cast(Object.class).fill(pos, cnt, value);
                } else {
                    ((UpdatableObjectArray<?>) tileMatr.array()).cast(Object.class).subArr(pos, cnt).fill(value);
                }
                ((UpdatableObjectArray<?>) work2).cast(Object.class).fill(pos, cnt, value);
            }
            if (!work2.subArr(0, tileMatr.size()).equals(tileMatr.array()))
                throw new AssertionError("The bug J in tiling found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + ", tileDim = " + JArrays.toString(tileDim, ";", 100)
                    + ", pos = " + pos + ", cnt = " + cnt
                    + ", " + tileMatr);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testSubMatrix(int ti, Matrix.ContinuationMode continuationMode) {
        if (funcOnly) {
            return;
        }
        final boolean fullyInside = continuationMode == Matrix.ContinuationMode.NONE;
        double outsideDouble = a instanceof BitArray ? 1.0 : 157.0;
        Object outsideValue = a instanceof PArray ? outsideDouble : a.elementType() == String.class ? "Hi" : null;
        if (continuationMode.isConstant() && !(a instanceof PFloatingArray)) {
            continuationMode = Matrix.ContinuationMode.getConstantMode(outsideValue);
        }
        assert Matrix.ContinuationMode.NULL_CONSTANT.continuationConstant() == null : "not null NULL_CONSTANT";
        assert Matrix.ContinuationMode.ZERO_CONSTANT.continuationConstant().getClass() == Double.class
            && Matrix.ContinuationMode.ZERO_CONSTANT.continuationConstant().equals(0.0d) : "illegal ZERO_CONSTANT";
        assert Matrix.ContinuationMode.NULL_CONSTANT.isPrimitiveTypeOrNullConstant();
        assert Matrix.ContinuationMode.ZERO_CONSTANT.isPrimitiveTypeOrNullConstant();
        assert Matrix.ContinuationMode.getConstantMode(null) == Matrix.ContinuationMode.NULL_CONSTANT :
            "bad implementation of getConstantMode(null)";
        assert Matrix.ContinuationMode.getConstantMode(0.0d) == Matrix.ContinuationMode.ZERO_CONSTANT :
            "bad implementation of getConstantMode(0.0d)";
        assert Matrix.ContinuationMode.getConstantMode(new Double(0.0d)) == Matrix.ContinuationMode.ZERO_CONSTANT :
            "bad implementation of getConstantMode(new Double(0.0d))";
        assert Matrix.ContinuationMode.getConstantMode(0.0f) != Matrix.ContinuationMode.ZERO_CONSTANT;
        assert Matrix.ContinuationMode.getConstantMode(-0.0d) != Matrix.ContinuationMode.ZERO_CONSTANT;
        assert Matrix.ContinuationMode.getConstantMode(Double.NaN) == Matrix.ContinuationMode.NAN_CONSTANT :
            "bad implementation of getConstantMode(Double.NaN)";
        assert Matrix.ContinuationMode.getConstantMode(new Double(Double.NaN))
            == Matrix.ContinuationMode.NAN_CONSTANT : "bad implementation of getConstantMode(new Double(Double.NaN))";
        if (!title(ti, "Testing \"Matrix.subMatr\" method, " + continuationMode + "...")) {
            return;
        }
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            long[] dim = new long[1 + rnd.nextInt(4)];
            int product = 1;
            for (int i = 0; i < dim.length; i++) {
                int lim = product == 0 ? len + 1 : len / product + 1;
                // - at least one zero dimension should lead to trivial results
                lim = Math.min(lim, rnd.nextInt(4) == 0 ? len :
                    rnd.nextBoolean() ? (int) Math.sqrt(len) : (int) Math.cbrt(len));
                dim[i] = (rnd.nextInt(3) > 0 && lim > 10 ? lim - 1 - rnd.nextInt(lim / 10) : rnd.nextInt(lim))
                    / blockSize * blockSize;
                product *= dim[i];
                assert product <= len;
            }
            Matrix<? extends Array> srcMatr = Matrices.matrix(a.subArr(0, product), dim);
            Matrix<? extends UpdatableArray> destMatr = Matrices.matrix(work1.subArr(0, product), dim);
            long[] tileDim = new long[dim.length];
            for (int i = 0; i < tileDim.length; i++) {
                tileDim[i] = 1 + rnd.nextInt(rnd.nextBoolean() ? 100 : (int) dim[i] + 1);
            }
            if (rnd.nextInt(3) == 0) {
                srcMatr = srcMatr.tile(tileDim);
            }
            if (rnd.nextInt(3) == 0) {
                destMatr = destMatr.tile(tileDim);
            }
            long[] srcPos = new long[dim.length];
            long[] destPos = new long[dim.length];
            long[] count = new long[dim.length];
            for (int i = 0; i < dim.length; i++) {
                int min = fullyInside ? 0 : -(int) dim[i] / 2;
                int max = fullyInside ? (int) dim[i] : (int) (3 * dim[i] / 2);
                srcPos[i] = min + rnd.nextInt(max - min + 1) / blockSize * blockSize;
                destPos[i] = min + rnd.nextInt(max - min + 1) / blockSize * blockSize;
                count[i] = rnd.nextInt((int) (fullyInside ? dim[i] - Math.max(srcPos[i], destPos[i]) : dim[i]) + 1)
                    / blockSize * blockSize;
            }
            if (rnd.nextInt(4) == 0 && !fullyInside) {
                for (int i = 0, iMax = rnd.nextInt(4); i < dim.length && i < iMax; i++) {
                    srcPos[i] = 0;
                    count[i] = dim[i]; // testing collapsing lowest (first) dimensions
                }
            }
            if (rnd.nextInt(4) == 0 && !fullyInside) {
                for (int i = 0, iMax = rnd.nextInt(4); i < dim.length && i < iMax; i++) {
                    destPos[i] = 0;
                    count[i] = dim[i]; // testing collapsing lowest (first) dimensions
                }
            }
            long[] coords1 = new long[dim.length];
            long[] coords2 = new long[dim.length];
            long[] coordsCyclic = new long[dim.length];
            long[] coordsMirrorCyclic = new long[dim.length];
            work1.copy(a); // destMatr is based on work1
            work2.copy(a); // extra elements (after destMatr.size()) are copied from "a" array
            work2.copy(destMatr.array()); // work2 is initially identical do destMatr.array() (!= work1 when tiled)
            Matrix<? extends Array> srcSubMatr = fullyInside ?
                srcMatr.subMatr(srcPos, count) :
                srcMatr.subMatr(srcPos, count, continuationMode);
            if (!continuationMode.equals(srcSubMatr.subMatrixContinuationMode()))
                throw new AssertionError("Bug in equals() in " + Matrix.ContinuationMode.class
                    + ": " + continuationMode + " != " + srcSubMatr.subMatrixContinuationMode());
            Matrix<? extends UpdatableArray> destSubMatr = fullyInside ?
                destMatr.subMatr(destPos, count) :
                continuationMode.isConstant() && !(a instanceof PFloatingArray) ?
                    destMatr.subMatr(destPos, count, Matrix.ContinuationMode.NULL_CONSTANT) :
                    destMatr.subMatr(destPos, count, continuationMode);
            for (int k = 0; k < 100; k++) {
                coords2[rnd.nextInt(dim.length)] = rnd.nextInt();
                for (int i = 0; i < dim.length; i++) {
                    coordsCyclic[i] = coordsMirrorCyclic[i] = 0;
                    if (dim[i] > 0) {
                        long repeatIndex = coords2[i] / dim[i];
                        coordsCyclic[i] = coordsMirrorCyclic[i] = coords2[i] - repeatIndex * dim[i];
                        assert coordsCyclic[i] == coords2[i] % dim[i] :
                            coordsCyclic[i] + " != " + coords2[i] + "%" + dim[i];
                        if (coordsCyclic[i] < 0) {
                            coordsCyclic[i] = coordsMirrorCyclic[i] = coordsCyclic[i] + dim[i];
                            repeatIndex--;
                        }
                        if (repeatIndex % 2 != 0) { //+1 or -1
                            coordsMirrorCyclic[i] = dim[i] - 1 - coordsMirrorCyclic[i];
                        }
                    }
                }
                if (srcMatr.cyclicIndex(coords2) != IPoint.valueOf(coordsCyclic).toOneDimensional(dim, false))
                    throw new AssertionError("The indexing bug 1 in IPoint/Matrix found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", coordinates = " + JArrays.toString(coords2, ";", 100)
                        + ", cyclicIndex = " + srcMatr.cyclicIndex(coords2)
                        + " instead of " + IPoint.valueOf(coordsCyclic).toOneDimensional(dim, false));
                if (srcMatr.pseudoCyclicIndex(coords2) != IPoint.valueOf(coords2).toOneDimensional(dim, true))
                    throw new AssertionError("The indexing bug 2 in IPoint/Matrix found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", coordinates = " + JArrays.toString(coords2, ";", 100)
                        + ", pseudoCyclicIndex = " + srcMatr.pseudoCyclicIndex(coords2)
                        + ", but toOneDimensional = "
                        + IPoint.valueOf(coords2).toOneDimensional(srcMatr.dimensions(), true));
                if (srcMatr.mirrorCyclicIndex(coords2) !=
                    IPoint.valueOf(coordsMirrorCyclic).toOneDimensional(dim, false))
                    throw new AssertionError("The indexing bug 3 in IPoint/Matrix found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", coordinates = " + JArrays.toString(coords2, ";", 100)
                        + ", mirrorCyclicIndex = " + srcMatr.mirrorCyclicIndex(coords2)
                        + " instead of " + IPoint.valueOf(coordsMirrorCyclic).toOneDimensional(dim, false));
            } // checking indexing only
            for (int k = 0, n = (int) srcSubMatr.size(); k < n; k++) {
                e1 = srcSubMatr.array().getElement(k);
                srcSubMatr.coordinates(k, coords1);
                destSubMatr.coordinates(k, coords2);
                if (!java.util.Arrays.equals(coords1, coords2))
                    throw new AssertionError("The indexing bug 4 in subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", destPos = " + JArrays.toString(destPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", " + JArrays.toString(coords1, ";", 100)
                        + " instead of " + JArrays.toString(coords2, ";", 100));
                for (int i = 0; i < dim.length; i++) {
                    coords2[i] = coords1[i] + srcPos[i];
                    assert dim[i] > 0; // because we have coordinates of a real element #k
                    long repeatIndex = coords2[i] / dim[i];
                    coordsCyclic[i] = coordsMirrorCyclic[i] = coords2[i] - repeatIndex * dim[i];
                    if (coordsCyclic[i] < 0) {
                        coordsCyclic[i] = coordsMirrorCyclic[i] = coordsCyclic[i] + dim[i];
                        repeatIndex--;
                    }
                    if (repeatIndex % 2 != 0) { //+1 or -1
                        coordsMirrorCyclic[i] = dim[i] - 1 - coordsMirrorCyclic[i];
                    }
                }
                // coords2 is coords1 translated into srcMatr coordinate system
                boolean inside = srcMatr.inside(coords2);
                long cyclicIndex = srcMatr.cyclicIndex(coords2);
                if (cyclicIndex != srcMatr.index(coordsCyclic))
                    throw new AssertionError("The indexing bug 5 in IPoint/Matrix found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", coordinates = " + JArrays.toString(coords2, ";", 100)
                        + ", cyclicIndex = " + cyclicIndex
                        + " instead of " + srcMatr.index(coordsCyclic));
                long pseudoCyclicIndex = srcMatr.pseudoCyclicIndex(coords2);
                if (pseudoCyclicIndex != IPoint.valueOf(coords2).toOneDimensional(dim, true))
                    throw new AssertionError("The indexing bug 6 in IPoint/Matrix found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", coordinates = " + JArrays.toString(coords2, ";", 100)
                        + ", pseudoCyclicIndex = " + pseudoCyclicIndex
                        + ", but toOneDimensional = "
                        + IPoint.valueOf(coords2).toOneDimensional(srcMatr.dimensions(), true));
                long mirrorCyclicIndex = srcMatr.mirrorCyclicIndex(coords2);
                if (mirrorCyclicIndex != srcMatr.index(coordsMirrorCyclic))
                    throw new AssertionError("The indexing bug 7 in IPoint/Matrix found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", coordinates = " + JArrays.toString(coords2, ";", 100)
                        + ", mirrorCyclicIndex = " + mirrorCyclicIndex
                        + " instead of " + srcMatr.index(coordsMirrorCyclic));
                e2 = inside ? srcMatr.array().getElement(srcMatr.index(coords2)) :
                    continuationMode == Matrix.ContinuationMode.CYCLIC ?
                        srcMatr.array().getElement(cyclicIndex) :
                        continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC ?
                            srcMatr.array().getElement(pseudoCyclicIndex) :
                            continuationMode == Matrix.ContinuationMode.MIRROR_CYCLIC ?
                                srcMatr.array().getElement(mirrorCyclicIndex) :
                                outsideValue;
                if (!inside && a instanceof PArray) {
                    double v1 = ((PArray) srcSubMatr.array()).getDouble(k);
                    double v2 = continuationMode == Matrix.ContinuationMode.CYCLIC ?
                        ((PArray) srcMatr.array()).getDouble(cyclicIndex) :
                        continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC ?
                            ((PArray) srcMatr.array()).getDouble(pseudoCyclicIndex) :
                            continuationMode == Matrix.ContinuationMode.MIRROR_CYCLIC ?
                                ((PArray) srcMatr.array()).getDouble(mirrorCyclicIndex) :
                                outsideDouble;
                    if (v1 != v2)
                        throw new AssertionError("The bug A in subMatr found in test #" + testCount + ": "
                            + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                            + ", destPos = " + JArrays.toString(destPos, ";", 100)
                            + ", count = " + JArrays.toString(count, ";", 100)
                            + ", dimensions = " + JArrays.toString(dim, ";", 100)
                            + ", error found at " + JArrays.toString(coords1, ";", 100) + ": "
                            + v1 + " instead of " + v2);
                } else {
                    if (!objectEquals(e1, e2))
                        throw new AssertionError("The bug A in subMatr found in test #" + testCount + ": "
                            + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                            + ", destPos = " + JArrays.toString(destPos, ";", 100)
                            + ", count = " + JArrays.toString(count, ";", 100)
                            + ", dimensions = " + JArrays.toString(dim, ";", 100)
                            + ", error found at " + JArrays.toString(coords1, ";", 100) + ": " + e1
                            + " instead of " + e2);
                }
                destSubMatr.array().setElement(k, e1);
                for (int i = 0; i < dim.length; i++) {
                    coords2[i] = coords1[i] + destPos[i];
                }
                if (destMatr.inside(coords2)) {
                    work2.setElement(destMatr.index(coords2), e1);
                } else if (continuationMode == Matrix.ContinuationMode.CYCLIC) {
                    work2.setElement(destMatr.cyclicIndex(coords2), e1);
                } else if (continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                    work2.setElement(destMatr.pseudoCyclicIndex(coords2), e1);
                } else if (continuationMode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                    work2.setElement(destMatr.mirrorCyclicIndex(coords2), e1);
                }
                if (k < len) {
                    work3.setElement(k, e1);
                }
            } // checking indexing + get/setElement: manual copying destSubMatr <- srcSubMatr
            for (int k = 0; k < destMatr.size(); k++)
                if (!objectEquals(e1 = destMatr.array().getElement(k), e2 = work2.getElement(k)))
                    throw new AssertionError("The bug B in subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", destPos = " + JArrays.toString(destPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", destMatr = " + destMatr + ", srcMatr = " + srcMatr
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
            assert srcSubMatr.size() == destSubMatr.size();
            int checkedLen = (int) Math.min(srcSubMatr.size(), len);
            if (fullyInside || !CombinedMemoryModel.isCombinedArray(a)) {
                // else a bug (null != "empty element")
                if (!srcSubMatr.array().subArr(0, checkedLen).equals(work3.subArr(0, checkedLen)))
                    throw new AssertionError("The bug C in subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", destPos = " + JArrays.toString(destPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr);
            }
            // now work2 is a copy of destMatr.array() + the rest of a (after destMatr.size())
            assert work2.subArray(0, destMatr.size()).equals(destMatr.array());
            assert work2.subArray(destMatr.size(), work2.length()).equals(a.subArray(destMatr.size(), a.length()));
            work1.copy(a);
            destSubMatr.array().copy(srcSubMatr.array().asImmutable().updatableClone(cmm));
            // - quick copying destSubMatr <- srcSubMatr
            for (int k = 0; k < len; k++) {
                if (!objectEquals(e1 = (k < destMatr.size() ? destMatr.array() : work1) // not the same when tiled
                    .getElement(k), e2 = work2.getElement(k)))
                {
                    throw new AssertionError("The bug D in subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", destPos = " + JArrays.toString(destPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr
                        + ", fullyInside = " + fullyInside
                        + ", error found at " + k + ": " + e1 + " instead of " + e2);
                }
            }
            if (fullyInside && !destSubMatr.array().subArr(0, destSubMatr.size()).updatableClone(cmm)
                .equals(srcSubMatr.array().updatableClone(cmm)))
                throw new AssertionError("The bug E in subMatr found in test #" + testCount + ": "
                    + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                    + ", destPos = " + JArrays.toString(destPos, ";", 100)
                    + ", count = " + JArrays.toString(count, ";", 100)
                    + ", dimensions = " + JArrays.toString(dim, ";", 100)
                    + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr);

            int lowIndex = -2 + rnd.nextInt(checkedLen + 4);
            int highIndex = -2 + rnd.nextInt(checkedLen + 4);
            work3.copy(srcSubMatr.array());
            long index, requiredIndex, lastIndex, requiredLastIndex;
            if (a instanceof PArray) {
                double value = ((PArray) work3).getDouble(rnd.nextInt(Math.max(checkedLen, 1)));
                index = ((PArray) srcSubMatr.array().subArr(0, checkedLen)).indexOf(lowIndex, highIndex, value);
                lastIndex = ((PArray) srcSubMatr.array().subArr(0, checkedLen)).lastIndexOf(
                    lowIndex, highIndex, value);
                requiredIndex = ((PArray) work3.subArr(0, checkedLen)).indexOf(lowIndex, highIndex, value);
                requiredLastIndex = ((PArray) work3.subArr(0, checkedLen)).lastIndexOf(lowIndex, highIndex, value);
            } else {
                Object value = work3.getElement(rnd.nextInt(Math.max(checkedLen, 1)));
                if (value instanceof String) {
                    value = new String((String) value); // new instance, to check "==" or "equals" in indexOf
                }
                ObjectArray<?> oa = ((ObjectArray<?>) srcSubMatr.array().subArr(0, checkedLen));
                index = oa.cast(Object.class).indexOf(lowIndex, highIndex, value);
                lastIndex = oa.cast(Object.class).lastIndexOf(lowIndex, highIndex, value);
                oa = (ObjectArray<?>) work3.subArr(0, checkedLen);
                requiredIndex = oa.cast(Object.class).indexOf(lowIndex, highIndex, value);
                requiredLastIndex = oa.cast(Object.class).lastIndexOf(lowIndex, highIndex, value);
            }
            if (!(continuationMode.isConstant() && CombinedMemoryModel.isCombinedArray(a))) {
                // else a bug (outside null != "empty element")
                if (index != requiredIndex) {
                    throw new AssertionError("The bug F in subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", region = " + lowIndex + ".." + highIndex + " from " + checkedLen + " elements"
                        + ", searching for " + work3.getElement(checkedLen / 2)
                        + ", srcSubMatr = " + srcSubMatr
                        + ": index " + index + " instead of " + requiredIndex
                        + " (" + Arrays.toString(srcSubMatr.array(), ";", 100)
                        + " and " + Arrays.toString(work3, ";", 100) + ")");
                }
                if (lastIndex != requiredLastIndex) {
                    throw new AssertionError("The bug G in subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", dimensions = " + JArrays.toString(dim, ";", 100)
                        + ", region = " + lowIndex + ".." + highIndex + " from " + checkedLen + " elements"
                        + ", searching for " + work3.getElement(checkedLen / 2)
                        + ", srcSubMatr = " + srcSubMatr
                        + ": last index " + lastIndex + " instead of " + requiredLastIndex
                        + " (" + Arrays.toString(srcSubMatr.array(), ";", 100)
                        + " and " + Arrays.toString(work3, ";", 100) + ")");
                }
            }
            int pos = rnd.nextInt((int) destSubMatr.size() + 1);
            int cnt = rnd.nextInt((int) destSubMatr.size() + 1 - pos);
            work1.copy(a);
            work2.copy(destMatr.array());
            Matrix<? extends UpdatableArray> testMatr = Matrices.matrix(work2.subArr(0, product), dim);
            Matrix<? extends UpdatableArray> testSubMatr = fullyInside ?
                testMatr.subMatr(destPos, count) :
                continuationMode.isConstant() && !(a instanceof PFloatingArray) ?
                    testMatr.subMatr(destPos, count, Matrix.ContinuationMode.NULL_CONSTANT) :
                    testMatr.subMatr(destPos, count, continuationMode);
            if (a instanceof PArray) {
                double value = ((PArray) a).getDouble(checkedLen / 2);
                if (rnd.nextBoolean()) {
                    ((UpdatablePArray) destSubMatr.array()).fill(pos, cnt, value);
                } else {
                    ((UpdatablePArray) destSubMatr.array().subArr(pos, cnt)).fill(value);
                }
                ((UpdatablePArray) work3).fill(value);
                testSubMatr.array().subArr(pos, cnt).copy(work3);
            } else {
                Object value = a.getElement(checkedLen / 2);
                if (rnd.nextBoolean()) {
                    ((UpdatableObjectArray<?>) destSubMatr.array()).cast(Object.class).fill(pos, cnt, value);
                } else {
                    ((UpdatableObjectArray<?>) destSubMatr.array()).cast(Object.class).subArr(pos, cnt).fill(value);
                }
                ((UpdatableObjectArray<?>) work3).cast(Object.class).fill(value);
                testSubMatr.array().subArr(pos, cnt).copy(work3);
            }
            if (!testSubMatr.array().subArr(0, checkedLen).equals(destSubMatr.array().subArr(0, checkedLen)))
                throw new AssertionError("The bug H in subMatr found in test #" + testCount + ": "
                    + "destPos = " + JArrays.toString(destPos, ";", 100)
                    + ", count = " + JArrays.toString(count, ";", 100)
                    + ", dimensions = " + JArrays.toString(dim, ";", 100)
                    + ", pos = " + pos + ", cnt = " + cnt
                    + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testCopyRegion(int ti) {
        if (!title(ti, "Testing \"Matrices.copyRegion\" method..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            long[] srcDim = new long[1 + rnd.nextInt(4)];
            int product = 1;
            for (int i = 0; i < srcDim.length; i++) {
                int lim = product == 0 ? len + 1 : len / product + 1;
                // - at least one zero dimension should lead to trivial results
                lim = Math.min(lim, rnd.nextInt(4) == 0 ? len :
                    rnd.nextBoolean() ? (int) Math.sqrt(len) : (int) Math.cbrt(len));
                srcDim[i] = (rnd.nextInt(3) > 0 && lim > 10 ? lim - 1 - rnd.nextInt(lim / 10) : rnd.nextInt(lim))
                    / blockSize * blockSize;
                product *= srcDim[i];
                assert product <= len;
            }
            Matrix<? extends Array> srcMatr = Matrices.matrix(a.subArr(0, product), srcDim);
            long[] destDim = srcDim.clone();
            if (rnd.nextBoolean()) {
                product = 1;
                for (int i = 0; i < destDim.length; i++) {
                    int lim = product == 0 ? len + 1 : len / product + 1;
                    // - at least one zero dimension should lead to trivial results
                    lim = Math.min(lim, rnd.nextInt(4) == 0 ? len :
                        rnd.nextBoolean() ? (int) Math.sqrt(len) : (int) Math.cbrt(len));
                    destDim[i] = (rnd.nextInt(3) > 0 && lim > 10 ? lim - 1 - rnd.nextInt(lim / 10) : rnd.nextInt(lim))
                        / blockSize * blockSize;
                    product *= destDim[i];
                    assert product <= len;
                }
            }
            Matrix<UpdatableArray> destMatr1 = Matrices.matrix(work1.subArr(0, product), destDim);
            Matrix<UpdatableArray> destMatr2 = Matrices.matrix(work2.subArr(0, product), destDim);
            long[] tileDim = new long[destDim.length];
            for (int i = 0; i < tileDim.length; i++) {
                tileDim[i] = 1 + rnd.nextInt(rnd.nextBoolean() ? 100 : (int) srcDim[i] + 1);
            }
            if (rnd.nextInt(3) == 0) {
                srcMatr = srcMatr.tile(tileDim);
            }
            if (rnd.nextInt(3) == 0) {
                destMatr1 = destMatr1.tile(tileDim);
                destMatr2 = destMatr2.tile(tileDim);
            }
            long[] srcPos = new long[srcDim.length];
            long[] destPos = new long[srcDim.length];
            long[] count = new long[srcDim.length];
            IRange[] destRanges = new IRange[srcDim.length];
            long[] shifts = new long[srcDim.length];
            boolean trivial = false;
            for (int i = 0; i < srcDim.length; i++) {
                int min = -(int) srcDim[i] / 4;
                int max = (int) (5 * srcDim[i] / 4);
                srcPos[i] = min + rnd.nextInt(max - min + 1) / blockSize * blockSize;
                min = -(int) destDim[i] / 4;
                max = (int) (5 * destDim[i] / 4);
                destPos[i] = min + rnd.nextInt(max - min + 1) / blockSize * blockSize;
                count[i] = rnd.nextInt((int) (Math.max(1,
                    3 * Math.min(srcDim[i] - srcPos[i], destDim[i] - destPos[i]) / 2))) / blockSize * blockSize;
                if (count[i] == 0) {
                    trivial = true;
                } else {
                    destRanges[i] = IRange.valueOf(destPos[i], destPos[i] + count[i] - 1);
                }
                shifts[i] = destPos[i] - srcPos[i];
            }
            work1.copy(a);
            work2.copy(a);
            double outsideDouble = a instanceof BitArray ? 1.0 : 157.0;
            Object outsideValue = a instanceof PArray ? outsideDouble : a.elementType() == String.class ? "Hi" : null;
            for (int srcFullyInside = 0; srcFullyInside < 2; srcFullyInside++) {
                if (trivial) {
                    continue;
                }
                Matrix<? extends Array> srcSubMatr;
                Matrix<UpdatableArray> destSubMatr;
                if (srcFullyInside == 0) {
                    srcSubMatr = srcMatr.subMatr(srcPos, count,
                        Matrix.ContinuationMode.getConstantMode(outsideValue));
                    destSubMatr = destMatr2.subMatr(destPos, count, Matrix.ContinuationMode.NULL_CONSTANT);
                } else {
                    try {
                        srcSubMatr = srcMatr.subMatr(srcPos, count);
                    } catch (IndexOutOfBoundsException e) {
                        srcSubMatr = null; // indicates invalid region
                    }
                    try {
                        destSubMatr = destMatr2.subMatr(destPos, count);
                    } catch (IndexOutOfBoundsException e) {
                        destSubMatr = null; // indicates invalid region
                    }
                }

                Matrices.Region region = rnd.nextBoolean() ?
                    Matrices.Region.getHyperparallelepiped(IRectangularArea.valueOf(destRanges).ranges()) :
                    // simple additional check of IRectangularArea
                    Matrices.Region.getConvexHyperpolyhedron(new double[0], new double[0], destRanges);
                if (srcDim.length == 2 && rnd.nextBoolean()) {
                    region = Matrices.Region.getPolygon2D(new double[][]{
                        {destRanges[0].min(), destRanges[1].min()},
                        {destRanges[0].min(), destRanges[1].max()},
                        {destRanges[0].max(), destRanges[1].max()},
                        {destRanges[0].max(), destRanges[1].min()}});
                }
                if (rnd.nextInt(2) == 0 && region.isContainsSupported()) {
                    final Matrices.Region parent = region;
                    final boolean onlyDisableInternalOptimizations = rnd.nextInt(3) == 0;
                    region = new Matrices.Region(parent.coordRanges()) {
                        @Override
                        public boolean contains(long... coordinates) {
                            return parent.contains(coordinates);
                        }

                        @Override
                        public Matrices.Region[] sectionAtLastCoordinate(long sectionCoordinateValue) {
                            return onlyDisableInternalOptimizations ?
                                parent.sectionAtLastCoordinate(sectionCoordinateValue) :
                                super.sectionAtLastCoordinate(sectionCoordinateValue);
                        }

                        @Override
                        public String toString() {
                            return "simplified-1 (" + onlyDisableInternalOptimizations + ") " + parent;
                        }
                    };
                }
//                System.out.println(region + "; " + JArrays.toString(srcDim, ",", 100));
                boolean illegalRegion = false;
                if (srcFullyInside == 0) {
                    Matrices.copyRegion(null, destMatr1, srcMatr, region, shifts, outsideValue);
                } else {
                    try {
                        Matrices.copyRegion(null, destMatr1, srcMatr, region, shifts);
                    } catch (IndexOutOfBoundsException e) {
                        illegalRegion = true;
                    }
                }
                boolean nonOptimizedCopy = rnd.nextBoolean();
                if (srcSubMatr != null && destSubMatr != null) {
                    if (nonOptimizedCopy) { // avoiding submatrix-tiling optimization of copy method
                        Matrices.copy(null, destSubMatr, srcSubMatr);
                    } else {
                        destSubMatr.array().copy(srcSubMatr.array());
                    }
                }
                if (illegalRegion != (srcSubMatr == null || destSubMatr == null))
                    throw new AssertionError("The bug A in copyRegion/subMatr found in test #" + testCount + ": "
                        + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                        + ", destPos = " + JArrays.toString(destPos, ";", 100)
                        + ", count = " + JArrays.toString(count, ";", 100)
                        + ", region = " + region
                        + ", illegalRegion = " + illegalRegion
                        + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr);
                if (!illegalRegion && !CombinedMemoryModel.isCombinedArray(a)) {
                    // else a bug (null != "empty element")
                    if (!work1.equals(work2))
                        throw new AssertionError("The bug B in copyRegion/subMatr found in test #" + testCount + ": "
                            + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                            + ", destPos = " + JArrays.toString(destPos, ";", 100)
                            + ", count = " + JArrays.toString(count, ";", 100)
                            + ", region = " + region
                            + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr
                            + ", copyRegion performed with" + (nonOptimizedCopy ? "out" : "") + " optimization");
                }
                Matrices.Region[] regions = null;
                if (srcDim.length == 2) {
                    if (destRanges[0].size() > 1 && destRanges[1].size() > 1) {
                        regions = new Matrices.Region[]{
                            Matrices.Region.getTriangle2D(
                                destRanges[0].min(), destRanges[1].min(),
                                destRanges[0].min(), destRanges[1].max(),
                                destRanges[0].max(), destRanges[1].max()),
                            Matrices.Region.getTriangle2D(
                                destRanges[0].min(), destRanges[1].min(),
                                destRanges[0].max(), destRanges[1].min(),
                                destRanges[0].max(), destRanges[1].max()),
                        };
                    }
                } else if (srcDim.length == 3) {
                    if (destRanges[0].size() > 1 && destRanges[1].size() > 1 && destRanges[2].size() > 1) {
                        regions = new Matrices.Region[]{
                            Matrices.Region.getTetrahedron3D(
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].min(), destRanges[2].max(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].max()),
                            Matrices.Region.getTetrahedron3D(
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].max()),
                            Matrices.Region.getTetrahedron3D(
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].min(), destRanges[1].max(), destRanges[2].min(),
                                destRanges[0].min(), destRanges[1].max(), destRanges[2].max(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].max()),
                            Matrices.Region.getTetrahedron3D(
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].min(), destRanges[1].max(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].min(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].max()),
                            Matrices.Region.getTetrahedron3D(
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].max(),
                                destRanges[0].min(), destRanges[1].max(), destRanges[2].max(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].max()),
                            Matrices.Region.getTetrahedron3D(
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].min(),
                                destRanges[0].min(), destRanges[1].min(), destRanges[2].max(),
                                destRanges[0].max(), destRanges[1].min(), destRanges[2].max(),
                                destRanges[0].max(), destRanges[1].max(), destRanges[2].max()),
                        };
                    }
                }
                if (regions != null) {
                    work1.copy(a);
                    illegalRegion = false;
                    try {
                        for (Matrices.Region r : regions) {
                            if (rnd.nextInt(4) == 0 && r.isContainsSupported()) {
                                final Matrices.Region parent = r;
                                final boolean onlyDisableInternalOptimizations = rnd.nextInt(3) == 0;
                                r = new Matrices.Region(parent.coordRanges()) {
                                    @Override
                                    public boolean contains(long... coordinates) {
                                        return parent.contains(coordinates);
                                    }

                                    @Override
                                    public Matrices.Region[] sectionAtLastCoordinate(long sectionCoordinateValue) {
                                        return onlyDisableInternalOptimizations ?
                                            parent.sectionAtLastCoordinate(sectionCoordinateValue) :
                                            super.sectionAtLastCoordinate(sectionCoordinateValue);
                                    }

                                    @Override
                                    public String toString() {
                                        return "simplified-2 (" + onlyDisableInternalOptimizations + ") " + parent;
                                    }
                                };
                            }
                            if (srcFullyInside == 0) {
                                Matrices.copyRegion(null, destMatr1, srcMatr, r, shifts, outsideValue);
                            } else {
                                Matrices.copyRegion(null, destMatr1, srcMatr, r, shifts);
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        illegalRegion = true;
                    }
                    if (illegalRegion != (srcSubMatr == null || destSubMatr == null))
                        throw new AssertionError("The bug C in copyRegion/subMatr found in test #" + testCount + ": "
                            + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                            + ", destPos = " + JArrays.toString(destPos, ";", 100)
                            + ", count = " + JArrays.toString(count, ";", 100)
                            + ", region = " + region
                            + ", illegalRegion = " + illegalRegion
                            + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr);
                    if (!illegalRegion && !CombinedMemoryModel.isCombinedArray(a)) {
                        // else a bug (null != "empty element")
                        if (!work1.equals(work2))
                            throw new AssertionError("The bug D in copyRegion/subMatr found in test #"
                                + testCount + ": "
                                + "srcPos = " + JArrays.toString(srcPos, ";", 100)
                                + ", destPos = " + JArrays.toString(destPos, ";", 100)
                                + ", count = " + JArrays.toString(count, ";", 100)
                                + ", regions = " + JArrays.toString(regions, "; ", 1000)
                                + ", srcSubMatr = " + srcSubMatr + ", destSubMatr = " + destSubMatr
                                + ", copyRegion performed with" + (nonOptimizedCopy ? "out" : "") + " optimization");
                    }
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testMinusAndDiffFuncWithShift(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asShifted\" + \"Arrays.asFuncArray\" "
            + "method (sometimes random shift + x-y, y-x, max(x-y,0) and |x-y|)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos1 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int srcPos2 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos1, srcPos2)) / blockSize * blockSize;
            int shift1 = rnd.nextInt();
            int shift2 = rnd.nextBoolean() ? shift1 : rnd.nextInt();
            Class<?> eType = rnd.nextBoolean() ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            work1.copy(a);
            UpdatablePArray upa1 = (UpdatablePArray) work1.subArr(srcPos1, count);
            UpdatablePArray upa2 = (UpdatablePArray) work1.subArr(srcPos2, count);
            PArray pa1 = upa1;
            PArray pa2 = upa2;
            if (rnd.nextBoolean())
                pa1 = (PArray) Arrays.asShifted(pa1, shift1);
            if (rnd.nextBoolean())
                pa2 = (PArray) Arrays.asShifted(pa2, shift2);
            PArray df = Arrays.asFuncArray(truncateOverflows, Func.X_MINUS_Y, rType, pa1, pa2);
            PArray idf = Arrays.asFuncArray(truncateOverflows, Func.Y_MINUS_X, rType, pa1, pa2);
            PArray pdf = Arrays.asFuncArray(truncateOverflows, Func.POSITIVE_DIFF, rType, pa1, pa2);
            PArray adf = Arrays.asFuncArray(truncateOverflows, Func.ABS_DIFF, rType, pa1, pa2);
            PArray dfClone = parallelClone(df);
            PArray idfClone = parallelClone(idf);
            PArray pdfClone = parallelClone(pdf);
            PArray adfClone = parallelClone(adf);
            for (int k = 0; k < count; k++) {
                double v1 = pa1.getDouble(k);
                double v2 = pa2.getDouble(k);
                double vReqD = v1 - v2;
                double vReqID = v2 - v1;
                double vReqPD = Math.max(v1 - v2, 0.0);
                double vReqAD = Math.abs(v1 - v2);
                if (df instanceof BitArray) {
                    vReqD = vReqD == 0.0 ? 0.0 : 1.0;
                    vReqID = vReqID == 0.0 ? 0.0 : 1.0;
                    vReqPD = vReqPD == 0.0 ? 0.0 : 1.0;
                    vReqAD = vReqAD == 0.0 ? 0.0 : 1.0;
                } else if (df instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) df).minPossibleValue();
                        double vMax = ((PFixedArray) df).maxPossibleValue();
                        vReqD = vReqD < vMin ? vMin : vReqD > vMax ? vMax : vReqD;
                        vReqID = vReqID < vMin ? vMin : vReqID > vMax ? vMax : vReqID;
                        vReqPD = vReqPD < vMin ? vMin : vReqPD > vMax ? vMax : vReqPD;
                        vReqAD = vReqAD < vMin ? vMin : vReqAD > vMax ? vMax : vReqAD;
                    } else {
                        vReqD = mask(eType, vReqD);
                        vReqID = mask(eType, vReqID);
                        vReqPD = mask(eType, vReqPD);
                        vReqAD = mask(eType, vReqAD);
                    }
                }
                if (Math.abs(df.getDouble(k) - vReqD) > Math.abs(vReqD) * 1e-5) {
                    // float type may lead to rounding
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount
                        + ": " + df.getDouble(k) + " instead of " + vReqD + " = " + v1 + " - " + v2
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + df.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (Math.abs(idf.getDouble(k) - vReqID) > Math.abs(vReqID) * 1e-5) {
                    // float type may lead to rounding
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount
                        + ": " + idf.getDouble(k) + " instead of " + vReqID + " = " + v2 + " - " + v1
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + idf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (Math.abs(pdf.getDouble(k) - vReqPD) > Math.abs(vReqPD) * 1e-5) {
                    // float type may lead to rounding
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount
                        + ": " + pdf.getDouble(k) + " instead of " + vReqPD + " = max(" + v1 + " - " + v2
                        + ", 0.0), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + pdf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (Math.abs(adf.getDouble(k) - vReqAD) > Math.abs(vReqAD) * 1e-5) {
                    // float type may lead to rounding
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount
                        + ": " + adf.getDouble(k) + " instead of " + vReqAD + " = |" + v1 + " - " + v2
                        + "|, srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + adf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (dfClone.getDouble(k) != df.getDouble(k)) {
                    throw new AssertionError("The bug E in asFuncArray found in test #" + testCount
                        + ": " + dfClone.getDouble(k) + " instead of " + df.getDouble(k)
                        + " = " + v1 + " - " + v2
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + df.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (idfClone.getDouble(k) != idf.getDouble(k)) {
                    throw new AssertionError("The bug F in asFuncArray found in test #" + testCount
                        + ": " + idfClone.getDouble(k) + " instead of " + idf.getDouble(k)
                        + " = " + v2 + " - " + v1
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + idf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (pdfClone.getDouble(k) != pdf.getDouble(k)) {
                    throw new AssertionError("The bug G in asFuncArray found in test #" + testCount
                        + ": " + pdfClone.getDouble(k) + " instead of " + pdf.getDouble(k)
                        + " = max(" + v1 + " - " + v2
                        + ", 0.0), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + pdf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (adfClone.getDouble(k) != adf.getDouble(k)) {
                    throw new AssertionError("The bug H in asFuncArray found in test #" + testCount
                        + ": " + adfClone.getDouble(k) + " instead of " + adf.getDouble(k)
                        + " = |" + v1 + " - " + v2
                        + "|, srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + adf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
            }
            if (!dfClone.equals(df))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!idfClone.equals(idf))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            if (!pdfClone.equals(pdf))
                throw new AssertionError("The bug C' in equals found in test #" + testCount);
            if (!adfClone.equals(adf))
                throw new AssertionError("The bug D' in equals found in test #" + testCount);
            if (df.elementType() == pa1.elementType() && pa1 == upa1 && pa2 == upa2) {
                int srcPos0 = rnd.nextInt(Math.min(srcPos1, srcPos2) + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(df);
                for (int k = 0; k < count; k++)
                    if (pa0.getDouble(k) != dfClone.getDouble(k))
                        throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" +
                            testCount
                            + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                pa0.copy(idf);
                for (int k = 0; k < count; k++)
                    if (pa0.getDouble(k) != idfClone.getDouble(k))
                        throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" +
                            testCount
                            + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                pa0.copy(pdf);
                for (int k = 0; k < count; k++)
                    if (pa0.getDouble(k) != pdfClone.getDouble(k))
                        throw new AssertionError("The in-place bug C'' in asFuncArray found in test #" +
                            testCount
                            + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                pa0.copy(adf);
                for (int k = 0; k < count; k++)
                    if (pa0.getDouble(k) != adfClone.getDouble(k))
                        throw new AssertionError("The in-place bug D'' in asFuncArray found in test #" +
                            testCount
                            + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                if (srcPos1 <= srcPos2) {
                    upa1.copy(df);
                    for (int k = 0; k < count; k++)
                        if (pa1.getDouble(k) != dfClone.getDouble(k))
                            throw new AssertionError("The in-place bug E'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    upa1.copy(idf);
                    for (int k = 0; k < count; k++)
                        if (pa1.getDouble(k) != idfClone.getDouble(k))
                            throw new AssertionError("The in-place bug F'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    upa1.copy(pdf);
                    for (int k = 0; k < count; k++)
                        if (pa1.getDouble(k) != pdfClone.getDouble(k))
                            throw new AssertionError("The in-place bug G'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    upa1.copy(adf);
                    for (int k = 0; k < count; k++)
                        if (pa1.getDouble(k) != adfClone.getDouble(k))
                            throw new AssertionError("The in-place bug H'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                }
                if (srcPos2 <= srcPos1) {
                    upa2.copy(df);
                    for (int k = 0; k < count; k++)
                        if (pa2.getDouble(k) != dfClone.getDouble(k))
                            throw new AssertionError("The in-place bug I'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    upa2.copy(idf);
                    for (int k = 0; k < count; k++)
                        if (pa2.getDouble(k) != idfClone.getDouble(k))
                            throw new AssertionError("The in-place bug J'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    upa2.copy(pdf);
                    for (int k = 0; k < count; k++)
                        if (pa2.getDouble(k) != pdfClone.getDouble(k))
                            throw new AssertionError("The in-place bug K'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    upa2.copy(adf);
                    for (int k = 0; k < count; k++)
                        if (pa2.getDouble(k) != adfClone.getDouble(k))
                            throw new AssertionError("The in-place bug L'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testMinMaxFunc2Args(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asFuncArray\" method (min and max, 2 arguments)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos1 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int srcPos2 = rnd.nextInt(len + 1) / blockSize * blockSize;
            if (rnd.nextInt(8) == 0)
                srcPos2 = srcPos1;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos1, srcPos2)) / blockSize * blockSize;
            Class<?> eType = rnd.nextInt(3) == 0 ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            work1.copy(a);
            UpdatablePArray pa1 = (UpdatablePArray) work1.subArr(srcPos1, count);
            UpdatablePArray pa2 = (UpdatablePArray) work1.subArr(srcPos2, count);
            PArray minf = Arrays.asFuncArray(truncateOverflows, Func.MIN, rType, pa1, pa2);
            PArray maxf = Arrays.asFuncArray(truncateOverflows, Func.MAX, rType, pa1, pa2);
            PArray minfClone = parallelClone(minf);
            PArray maxfClone = parallelClone(maxf);
            for (int k = 0; k < count; k++) {
                double v1 = pa1.getDouble(k);
                double v2 = pa2.getDouble(k);
                double vReqMin = Math.min(v1, v2);
                double vReqMax = Math.max(v1, v2);
                if (minf instanceof BitArray) {
                    vReqMin = vReqMin == 0.0 ? 0.0 : 1.0;
                    vReqMax = vReqMax == 0.0 ? 0.0 : 1.0;
                } else if (minf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) minf).minPossibleValue();
                        double vMax = ((PFixedArray) minf).maxPossibleValue();
                        vReqMin = vReqMin < vMin ? vMin : vReqMin > vMax ? vMax : vReqMin;
                        vReqMax = vReqMax < vMin ? vMin : vReqMax > vMax ? vMax : vReqMax;
                    } else {
                        vReqMin = mask(eType, vReqMin);
                        vReqMax = mask(eType, vReqMax);
                    }
                }
                if (minf.getDouble(k) != vReqMin) {
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount +
                        ": " + minf.getDouble(k) + " instead of " + vReqMin + " = min(" + v1 + "," + v2
                        + "), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + minf.elementType() + (truncateOverflows ? ", truncateOverflows)" :
                        ", raw)"));
                }
                if (maxf.getDouble(k) != vReqMax) {
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount +
                        ": " + maxf.getDouble(k) + " instead of " + vReqMax + " = max(" + v1 + "," + v2
                        + "), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + maxf.elementType() + (truncateOverflows ? ", truncateOverflows)" :
                        ", raw)"));
                }
                if (minfClone.getDouble(k) != minf.getDouble(k)) {
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + "srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
                if (maxfClone.getDouble(k) != maxf.getDouble(k)) {
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
            }
            if (!minfClone.equals(minf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!maxfClone.equals(maxf))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            if (minf.elementType() == pa1.elementType()) {
                int srcPos0 = rnd.nextInt(Math.min(srcPos1, srcPos2) + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(minf);
                for (int k = 0; k < count; k++)
                    if (pa0.getDouble(k) != minfClone.getDouble(k))
                        throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" +
                            testCount
                            + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                pa0.copy(maxf);
                for (int k = 0; k < count; k++)
                    if (pa0.getDouble(k) != maxfClone.getDouble(k))
                        throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" +
                            testCount
                            + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                if (srcPos1 <= srcPos2) {
                    pa1.copy(minf);
                    for (int k = 0; k < count; k++)
                        if (pa1.getDouble(k) != minfClone.getDouble(k))
                            throw new AssertionError("The in-place bug C'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    pa1.copy(maxf);
                    for (int k = 0; k < count; k++)
                        if (pa1.getDouble(k) != maxfClone.getDouble(k))
                            throw new AssertionError("The in-place bug D'' in asFuncArray found in test #"
                                + testCount
                                + ": index = " + k + ", srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                                + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                }
                if (srcPos2 <= srcPos1) {
                    pa2.copy(minf);
                    if (!pa2.equals(minfClone))
                        throw new AssertionError("The in-place bug E'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                    pa2.copy(maxf);
                    if (!pa2.equals(maxfClone))
                        throw new AssertionError("The in-place bug F'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testMinMaxFunc3Args(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asFuncArray\" method (min and max, 3 arguments)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos1 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int srcPos2 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int srcPos3 = rnd.nextInt(len + 1) / blockSize * blockSize;
            if (rnd.nextInt(8) == 0) {
                srcPos2 = srcPos1;
                if (rnd.nextBoolean())
                    srcPos3 = srcPos1;
            }
            int count = rnd.nextInt(len + 1 - Math.max(srcPos1, Math.max(srcPos2, srcPos3)))
                / blockSize * blockSize;
            Class<?> eType = rnd.nextInt(3) == 0 ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            work1.copy(a);
            UpdatablePArray pa1 = (UpdatablePArray) work1.subArr(srcPos1, count);
            UpdatablePArray pa2 = (UpdatablePArray) work1.subArr(srcPos2, count);
            UpdatablePArray pa3 = (UpdatablePArray) work1.subArr(srcPos3, count);
            PArray minf = Arrays.asFuncArray(truncateOverflows, Func.MIN, rType, pa1, pa2, pa3);
            PArray maxf = Arrays.asFuncArray(truncateOverflows, Func.MAX, rType, pa1, pa2, pa3);
            PArray minfClone = parallelClone(minf);
            PArray maxfClone = parallelClone(maxf);
            for (int k = 0; k < count; k++) {
                double v1 = pa1.getDouble(k);
                double v2 = pa2.getDouble(k);
                double v3 = pa3.getDouble(k);
                double vReqMin = Math.min(v1, Math.min(v2, v3));
                double vReqMax = Math.max(v1, Math.max(v2, v3));
                if (minf instanceof BitArray) {
                    vReqMin = vReqMin == 0.0 ? 0.0 : 1.0;
                    vReqMax = vReqMax == 0.0 ? 0.0 : 1.0;
                } else if (minf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) minf).minPossibleValue();
                        double vMax = ((PFixedArray) minf).maxPossibleValue();
                        vReqMin = vReqMin < vMin ? vMin : vReqMin > vMax ? vMax : vReqMin;
                        vReqMax = vReqMax < vMin ? vMin : vReqMax > vMax ? vMax : vReqMax;
                    } else {
                        vReqMin = mask(eType, vReqMin);
                        vReqMax = mask(eType, vReqMax);
                    }
                }
                if (minf.getDouble(k) != vReqMin) {
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount +
                        ": " + minf.getDouble(k) + " instead of " + vReqMin + " = min("
                        + v1 + "," + v2 + "," + v3
                        + "), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + minf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (maxf.getDouble(k) != vReqMax) {
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount +
                        ": " + maxf.getDouble(k) + " instead of " + vReqMax + " = max("
                        + v1 + "," + v2 + "," + v3
                        + "), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + maxf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (minfClone.getDouble(k) != minf.getDouble(k)) {
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + "srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
                if (maxfClone.getDouble(k) != maxf.getDouble(k)) {
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
            }
            if (!minfClone.equals(minf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!maxfClone.equals(maxf))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            if (minf.elementType() == pa1.elementType()) {
                int srcPos0 = rnd.nextInt(Math.min(srcPos1, Math.min(srcPos2, srcPos3)) + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(minf);
                if (!pa0.equals(minfClone))
                    throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" + testCount
                        + ": srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                        + ", srcPos2 = " + srcPos2 + ", srcPos3 = " + srcPos3 + ", count = " + count);
                work1.copy(a);
                pa0.copy(maxf);
                if (!pa0.equals(maxfClone))
                    throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" + testCount
                        + ": srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                        + ", srcPos2 = " + srcPos2 + ", srcPos3 = " + srcPos3 + ", count = " + count);
                work1.copy(a);
                if (srcPos1 <= srcPos2 && srcPos1 <= srcPos3) {
                    pa1.copy(minf);
                    if (!pa1.equals(minfClone))
                        throw new AssertionError("The in-place bug C'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2
                            + ", srcPos3 = " + srcPos3 + ", count = " + count);
                    work1.copy(a);
                    pa1.copy(maxf);
                    if (!pa1.equals(maxfClone))
                        throw new AssertionError("The in-place bug D'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2
                            + ", srcPos3 = " + srcPos3 + ", count = " + count);
                    work1.copy(a);
                }
                if (srcPos3 <= srcPos1 && srcPos3 <= srcPos2) {
                    pa3.copy(minf);
                    if (!pa3.equals(minfClone))
                        throw new AssertionError("The in-place bug E'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2
                            + ", srcPos3 = " + srcPos3 + ", count = " + count);
                    work1.copy(a);
                    pa3.copy(maxf);
                    if (!pa3.equals(maxfClone))
                        throw new AssertionError("The in-place bug F'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2
                            + ", srcPos3 = " + srcPos3 + ", count = " + count);
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testMinMaxFunc2ArgsWithShift(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asShifted\" + \"Arrays.asFuncArray\" "
            + "method (random shift + min and max, 2 arguments)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos1 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int srcPos2 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos1, srcPos2)) / blockSize * blockSize;
            int shift1 = rnd.nextInt();
            int shift2 = rnd.nextBoolean() ? shift1 : rnd.nextInt();
            Class<?> eType = ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            work1.copy(a);
            PArray pa1 = (PArray) work1.subArr(srcPos1, count);
            PArray pa2 = (PArray) work1.subArr(srcPos2, count);
            pa1 = (PArray) Arrays.asShifted(pa1, shift1);
            pa2 = (PArray) Arrays.asShifted(pa2, shift2);
            PArray minf = Arrays.asFuncArray(truncateOverflows, Func.MIN, rType, pa1, pa2);
            PArray maxf = Arrays.asFuncArray(truncateOverflows, Func.MAX, rType, pa1, pa2);
            PArray minfClone = parallelClone(minf);
            PArray maxfClone = parallelClone(maxf);
            for (int k = 0; k < count; k++) {
                double v1 = pa1.getDouble(k);
                double v2 = pa2.getDouble(k);
                double vReqMin = Math.min(v1, v2);
                double vReqMax = Math.max(v1, v2);
                if (minf instanceof BitArray) {
                    vReqMin = vReqMin == 0.0 ? 0.0 : 1.0;
                    vReqMax = vReqMax == 0.0 ? 0.0 : 1.0;
                } else if (minf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) minf).minPossibleValue();
                        double vMax = ((PFixedArray) minf).maxPossibleValue();
                        vReqMin = vReqMin < vMin ? vMin : vReqMin > vMax ? vMax : vReqMin;
                        vReqMax = vReqMax < vMin ? vMin : vReqMax > vMax ? vMax : vReqMax;
                    } else {
                        vReqMin = mask(eType, vReqMin);
                        vReqMax = mask(eType, vReqMax);
                    }
                }
                if (minf.getDouble(k) != vReqMin) {
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount +
                        ": " + minf.getDouble(k) + " instead of " + vReqMin + " = min(" + v1 + "," + v2
                        + "), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + minf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (maxf.getDouble(k) != vReqMax) {
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount +
                        ": " + maxf.getDouble(k) + " instead of " + vReqMax + " = max(" + v1 + "," + v2
                        + "), srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + maxf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (minfClone.getDouble(k) != minf.getDouble(k)) {
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + "srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
                if (maxfClone.getDouble(k) != maxf.getDouble(k)) {
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
            }
            if (!minfClone.equals(minf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!maxfClone.equals(maxf))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testMinMaxFuncNArgsWithShift(int ti) {
        if (!(a instanceof PArray))
            return;
        final int maxNumberOfArguments = a instanceof BitArray ? 70 : 20;
        if (!title(ti, "Testing \"Arrays.asShifted\" + \"Arrays.asFuncArray\" "
            + "method (random shift + min and max, n <= " + maxNumberOfArguments + " arguments)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            PArray[] pa = new PArray[1 + rnd.nextInt(maxNumberOfArguments)];
            int count = rnd.nextInt(len + 1) / blockSize * blockSize;
            work1.copy(a);
            for (int m = 0; m < pa.length; m++) {
                int srcPos = rnd.nextInt(len + 1 - count) / blockSize * blockSize;
                pa[m] = (PArray) work1.subArr(srcPos, count);
                if (rnd.nextBoolean()) {
                    pa[m] = (PArray) Arrays.asShifted(pa[m], rnd.nextInt());
                }
            }
            Class<?> eType = ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            PArray minf = Arrays.asFuncArray(truncateOverflows, Func.MIN, rType, pa);
            PArray maxf = Arrays.asFuncArray(truncateOverflows, Func.MAX, rType, pa);
            PArray minfClone = parallelClone(minf);
            PArray maxfClone = parallelClone(maxf);
            for (int k = 0; k < count; k++) {
                // we suppose here that getDouble method was already tested above for MinFunc and MaxFunc
                if (minfClone.getDouble(k) != minf.getDouble(k)) {
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + "count = " + count + ", " + pa.length + " arrays:" + String.format("%n    ")
                        + JArrays.toString(pa, String.format("%n    "), 1000));
                }
                if (maxfClone.getDouble(k) != maxf.getDouble(k)) {
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "count = " + count + ", " + pa.length + " arrays:" + String.format("%n    ")
                        + JArrays.toString(pa, String.format("%n    "), 1000));
                }
            }
            if (!minfClone.equals(minf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!maxfClone.equals(maxf))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            showProgress(testCount, numberOfTests);
        }
    }

    private void testLinearFunc1ArgAndUpdatableFunc(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asFuncArray\" and "
            + "\"Arrays.asUpdatableFuncArray\" methods (ax+b)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            double ca = rnd.nextBoolean() ? 5 * rnd.nextDouble() : rnd.nextBoolean() ? 1.0 : -1.0;
            double cb = rnd.nextBoolean() ? -50 + 100 * rnd.nextDouble() : rnd.nextBoolean() ? 1.0 : 0.0;
            // cb=1, ca=-1 corresponds to Func.INVERSE and processed by a special branch
            // cb=0, ca=1 corresponds to Func.IDENTITY and processed by a special branch
            Class<?> eType = rnd.nextInt(3) == 0 ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            Class<? extends UpdatablePArray> urType = Arrays.type(UpdatablePArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            boolean checkSubArray = rnd.nextBoolean();
            work1.copy(a);
            work2.copy(a);
            UpdatablePArray pa = (UpdatablePArray) (checkSubArray ? work1 : work1.subArr(srcPos, count));
            UpdatablePArray pa2 = (UpdatablePArray) (checkSubArray ? work2 : work2.subArr(srcPos, count));
            PArray lf = Arrays.asFuncArray(truncateOverflows,
                LinearFunc.getInstance(cb, ca), rType, pa);
            PArray lfMulti = Arrays.asFuncArray(truncateOverflows,
                LinearFunc.getInstance(cb, ca, 0.0), rType, pa, Arrays.nIntCopies(pa.length(), 157));
            UpdatablePArray ulf = Arrays.asUpdatableFuncArray(truncateOverflows,
                LinearFunc.getUpdatableInstance(cb, ca), urType, pa);
            UpdatablePArray ulf2 = Arrays.asUpdatableFuncArray(truncateOverflows,
                LinearFunc.getUpdatableInstance(cb, ca), urType, pa2);
            if (checkSubArray) {
                pa = pa.subArr(srcPos, count);
                pa2 = pa2.subArr(srcPos, count);
                lf = (PArray) lf.subArr(srcPos, count);
                lfMulti = (PArray) lfMulti.subArr(srcPos, count);
                ulf = ulf.subArr(srcPos, count);
                ulf2 = ulf2.subArr(srcPos, count);
            }
            PArray lfClone = parallelClone(lf);
            PArray lfMultiClone = parallelClone(lfMulti);
            PArray ulfClone = parallelClone(ulf);
            PArray ulfImm = ulf.asImmutable();
            for (int k = 0; k < count; k++) {
                double v = lf.getDouble(k);
                if (lfMulti.getDouble(k) != v)
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count
                        + ": " + v + " for 1 argument, " + lfMulti.getDouble(k) + " for 2 arguments"
                        + " (" + ca + "x+" + cb + ", x=" + pa.getDouble(k) + ", "
                        + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                if (ulf.getDouble(k) != v)
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                if (lfClone.getDouble(k) != v)
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + lfClone.getDouble(k) + " instead of " + v + ", srcPos = " + srcPos + ", count = "
                        + count + " (" + ca + "x+" + cb + ", " + lf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                if (lfMultiClone.getDouble(k) != v)
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                if (ulfClone.getDouble(k) != v)
                    throw new AssertionError("The bug E in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                if (ulfImm.getDouble(k) != v)
                    throw new AssertionError("The bug F in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                double vReq = ca * pa.getDouble(k) + cb;
                if (lf instanceof BitArray) {
                    vReq = vReq == 0.0 ? 0.0 : 1.0;
                } else if (lf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) lf).minPossibleValue();
                        double vMax = ((PFixedArray) lf).maxPossibleValue();
                        vReq = vReq < vMin ? vMin : vReq > vMax ? vMax : vReq;
                    } else {
                        vReq = mask(eType, vReq);
                    }
                }
                if (lf instanceof FloatArray) {
                    vReq = (float) vReq; // FloatArray cannot store precise double values
                }
                if (Math.abs(v - vReq) > 1.001)
                    throw new AssertionError("The bug V in asFuncArray found in test #"
                        + testCount + ": "
                        + v + " instead of " + vReq + ", srcPos = " + srcPos + ", count = " + count
                        + " (" + ca + "x+" + cb + ", " + lf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
            }
            if (!lfMulti.equals(lf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount + ": "
                    + lfMulti + " = " + Arrays.toString(lfMulti, ";", 1000) + " not equal to "
                    + lf + " = " + Arrays.toString(lf, ";", 1000));
            if (!lf.equals(ulf))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            if (!lf.equals(lfClone))
                throw new AssertionError("The bug C' in equals found in test #" + testCount);
            if (!lf.equals(lfMultiClone))
                throw new AssertionError("The bug D' in equals found in test #" + testCount);
            if (!ulfClone.equals(lf))
                throw new AssertionError("The bug E' in equals found in test #" + testCount);
            if (!ulfImm.equals(lf))
                throw new AssertionError("The bug F' in equals found in test #" + testCount);
            if (lf.elementType() == pa.elementType()) {
                int srcPos0 = rnd.nextInt(srcPos + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(lf);
                if (!pa0.equals(lfClone))
                    throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" + testCount
                        + ": srcPos0 = " + srcPos0 + ", srcPos = " + srcPos + ", count = " + count
                        + " (" + ca + "x+" + cb + ", " + lf.elementType()
                        + (truncateOverflows ? ", truncateOverflows" : ", raw")
                        + ", checkSubArray=" + checkSubArray + ")");
                work1.copy(a);
                pa.copy(lf);
                if (!pa.equals(lfClone))
                    throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" + testCount
                        + ": srcPos = " + srcPos + ", count = " + count
                        + " (" + ca + "x+" + cb + ", " + lf.elementType()
                        + (truncateOverflows ? ", truncateOverflows" : ", raw")
                        + ", checkSubArray=" + checkSubArray + ")");
            }
            if (Math.abs(ca) > 0.1) {
                UpdatablePArray data = (UpdatablePArray) SimpleMemoryModel.getInstance().newUnresizableArray(
                    ulf.elementType(), count);
                for (int k = 0; k < count; k++) {
                    double v = rnd.nextBoolean() ? -5000 + 50000 * rnd.nextDouble() :
                        rnd.nextBoolean() ? rnd.nextGaussian() :
                            rnd.nextBoolean() ? 0.0 :
                                rnd.nextBoolean() ? 1.0 :
                                    rnd.nextBoolean() ? rnd.nextGaussian() * 1e5 :
                                        rnd.nextGaussian() * 1e12;
                    ulf.setDouble(k, v);
                    data.setDouble(k, v);
                    v = data.getDouble(k); // really used value
                    double x = pa.getDouble(k);
                    double xReq = (v - cb) * (1.0 / ca);
                    if (pa instanceof FloatArray) {
                        xReq = (float) xReq; // FloatArray cannot store precise double values
                    } else if (pa instanceof BitArray) {
                        xReq = xReq == 0.0 ? 0.0 : 1.0;
                    } else if (pa instanceof PFixedArray) {
                        if (truncateOverflows) {
                            double xMin = ((PFixedArray) pa).minPossibleValue();
                            double xMax = ((PFixedArray) pa).maxPossibleValue();
                            xReq = xReq < xMin ? xMin : xReq > xMax ? xMax : xReq;
                        } else {
                            xReq = mask(pa.elementType(), xReq);
                        }
                    }
                    if (Math.abs(x - xReq) > 1.001)
                        throw new AssertionError("The bug X in asUpdatableFuncArray found in test #"
                            + testCount + ": "
                            + x + " instead of " + xReq + ", srcPos = " + srcPos + ", count = " + count
                            + " (" + ca + "x+" + cb + "=" + v + ", " + lf.elementType()
                            + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
//                    System.out.println(k + ": " + v + " -> " + x + "/" + xReq);
                }
                ulf2.copy(data);
                if (!pa.equals(pa2))
                    throw new AssertionError("The bug Y in asUpdatableFuncArray found in test #" + testCount
                        + ": srcPos = " + srcPos + ", count = " + count
                        + " (" + ca + "x+" + cb + ", " + lf.elementType()
                        + (truncateOverflows ? ", truncateOverflows" : ", raw")
                        + ", checkSubArray=" + checkSubArray + "), "
                        + Arrays.toHexString(pa2, " ", 1000) + " != " + Arrays.toHexString(pa, " ", 1000));

            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testLinearFunc2Args(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asFuncArray\" method (ax+by+c)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos1 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int srcPos2 = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - Math.max(srcPos1, srcPos2)) / blockSize * blockSize;
            double ca1 = rnd.nextBoolean() ? 100 * rnd.nextDouble() : 1.0;
            double ca2 = rnd.nextBoolean() ? 100 * rnd.nextDouble() : rnd.nextBoolean() ? ca1 : 1.0;
            double cb = rnd.nextBoolean() ? -500 + 1000 * rnd.nextDouble() : 0.0;
            Class<?> eType = rnd.nextInt(3) == 0 ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            work1.copy(a);
            UpdatablePArray pa1 = (UpdatablePArray) work1.subArr(srcPos1, count);
            UpdatablePArray pa2 = (UpdatablePArray) work1.subArr(srcPos2, count);
            Class<?> secondType = ALL_P_CLASSES[rnd.nextInt(8)];
            PArray pa2DiffType = secondType == boolean.class ? Arrays.nBitCopies(count, rnd.nextBoolean()) :
                Arrays.nPCopies(count, secondType, (short) rnd.nextInt());
            // (short)nextInt() for floating points allows to avoid testing too big numbers
            PArray lf = Arrays.asFuncArray(truncateOverflows,
                LinearFunc.getInstance(cb, ca1, ca2), rType, pa1, pa2);
            PArray lfDiffType = Arrays.asFuncArray(truncateOverflows,
                LinearFunc.getInstance(cb, ca1, ca2), rType, pa1, pa2DiffType);
            PArray lfClone = parallelClone(lf);
            PArray lfDiffTypeClone = parallelClone(lfDiffType);
            for (int k = 0; k < count; k++) {
                double v = lf.getDouble(k);
                double vDiffType = lfDiffType.getDouble(k);
                double v1 = pa1.getDouble(k);
                double v2 = pa2.getDouble(k);
                double v2DiffType = pa2DiffType.getDouble(k);
                double vReq = ca1 * v1 + ca2 * v2 + cb;
                double vReqDiffType = ca1 * v1 + ca2 * v2DiffType + cb;
                if (lf instanceof BitArray) {
                    vReq = vReq == 0.0 ? 0.0 : 1.0;
                    vReqDiffType = vReqDiffType == 0.0 ? 0.0 : 1.0;
                } else if (lf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) lf).minPossibleValue();
                        double vMax = ((PFixedArray) lf).maxPossibleValue();
                        vReq = vReq < vMin ? vMin : vReq > vMax ? vMax : vReq;
                        vReqDiffType = vReqDiffType < vMin ? vMin : vReqDiffType > vMax ? vMax :
                            vReqDiffType;
                    } else {
                        vReq = mask(eType, vReq);
                        vReqDiffType = mask(eType, vReqDiffType);
                    }
                }
                if (lf instanceof FloatArray) {
                    vReq = (float) vReq; // FloatArray cannot store precise double values
                }
                if (lfDiffType instanceof FloatArray) {
                    vReqDiffType = (float) vReqDiffType; // FloatArray cannot store precise double values
                }

                if (Math.abs(v - vReq) > 1.001)
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount
                        + ": " + v + " instead of " + vReq
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                        + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                if (Math.abs(vDiffType - vReqDiffType) > 1.001)
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount
                        + ": " + vDiffType + " instead of " + vReqDiffType
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                        + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                if (Math.abs((float) lfClone.getDouble(k) - (float) v) > 1.001) {
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount
                        + ": " + lfClone.getDouble(k) + " instead of " + v
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                        + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
                if (Math.abs((float) lfDiffTypeClone.getDouble(k) - (float) vDiffType) > 1.001) {
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + ": " + lfDiffTypeClone.getDouble(k) + " instead of " + v
                        + ", srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                        + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                        + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
            }
            if (!lfClone.equals(lf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount
                    + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                    + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                    + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
            if (!lfDiffTypeClone.equals(lfDiffType))
                throw new AssertionError("The bug B' in equals found in test #" + testCount
                    + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                    + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                    + lfDiffType.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
            if (!noCheckStability) {
                for (int k = 0; k < 10; k++) {
                    if (!parallelClone(lf).equals(lf)) // unstable behavior!
                        throw new AssertionError("The bug E in asFuncArray found in test #" + testCount
                            + (k > 0 ? " (unstable at the call #" + k + ")" : "")
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                            + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                            + lf.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                    if (!parallelClone(lfDiffType).equals(lfDiffType)) // unstable behavior!
                        throw new AssertionError("The bug F in asFuncArray found in test #" + testCount
                            + (k > 0 ? " (unstable at the call #" + k + ")" : "")
                            + ": srcPos1 = " + srcPos1 + ", srcPos2 = " + srcPos2 + ", count = " + count
                            + " (" + ca1 + "x+" + ca2 + "y+" + cb + ", "
                            + lfDiffType.elementType() + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
            }
            if (lf.elementType() == pa1.elementType()) {
                int srcPos0 = rnd.nextInt(Math.min(srcPos1, srcPos2) + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(lf);
                if (!pa0.equals(lfClone))
                    throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" + testCount
                        + ": srcPos0 = " + srcPos0 + ", srcPos1 = " + srcPos1
                        + ", srcPos2 = " + srcPos2 + ", count = " + count);
                work1.copy(a);
                if (srcPos1 <= srcPos2) {
                    pa1.copy(lf);
                    if (!pa1.equals(lfClone))
                        throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                    work1.copy(a);
                }
                if (srcPos2 <= srcPos1) {
                    pa2.copy(lf);
                    if (!pa2.equals(lfClone))
                        throw new AssertionError("The in-place bug C'' in asFuncArray found in test #" +
                            testCount
                            + ": srcPos1 = " + srcPos1
                            + ", srcPos2 = " + srcPos2 + ", count = " + count);
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private static double myPow(double x, double c, double scale) {
        // This implementation must be FULLY identical to the implementation in PowerFunc class
        double result = c == 1.0 ? scale * x : c == 2.0 ? scale * x * x : c == 3.0 ? scale * x * x * x :
            c == 0.5 ? scale * Math.sqrt(x) : c == 1.0 / 3.0 ? scale * Math.cbrt(x) :
                scale * Math.pow(x, c);
        if (x > 0.0 && x < 2000) {
            assert Math.abs(result - scale * Math.pow(x, c)) <= 0.1;
        }
        return result;
    }

    private static strictfp double myStrictPow(double x, double c, double scale) {
        // This implementation must be FULLY identical to the implementation in PowerFunc class
        double result = c == 1.0 ? scale * x : c == 2.0 ? scale * x * x : c == 3.0 ? scale * x * x * x :
            c == 0.5 ? scale * StrictMath.sqrt(x) : c == 1.0 / 3.0 ? scale * StrictMath.cbrt(x) :
                scale * StrictMath.pow(x, c);
        if (x > 0.0 && x < 2000) {
            assert Math.abs(result - scale * Math.pow(x, c)) <= 0.1;
        }
        return result;
    }

    private void testPowerFuncAndUpdatableFunc(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asFuncArray\" and "
            + "\"Arrays.asUpdatableFuncArray\" methods (x^c)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            Class<?> eType = rnd.nextInt(3) == 0 ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            Class<? extends UpdatablePArray> urType = Arrays.type(UpdatablePArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            boolean strict = rnd.nextBoolean();
            double c = new double[]{1.0, 2.0, 3.0, 0.5, 1.0 / 3.0, 1.7}[rnd.nextInt(6)];
            double scale = new double[]{1.0, 1.5, 0.7}[rnd.nextInt(3)];
            Func f = strict ?
                PowerFunc.getStrictInstance(c, scale) :
                PowerFunc.getInstance(c, scale);
            Func.Updatable uf = strict ?
                PowerFunc.getUpdatableStrictInstance(c, scale) :
                PowerFunc.getUpdatableInstance(c, scale);
            work1.copy(a);
            UpdatablePArray pa = (UpdatablePArray) work1.subArr(srcPos, count);
            PArray pf = Arrays.asFuncArray(truncateOverflows, f, rType, pa);
            UpdatablePArray upf = Arrays.asUpdatableFuncArray(truncateOverflows, uf, urType, pa);
            PArray pfClone = parallelClone(pf);
            PArray upfClone = parallelClone(upf);
            PArray upfImm = upf.asImmutable();
            for (int k = 0; k < count; k++) {
                double v = pf.getDouble(k);
                if (!Double.valueOf(upf.getDouble(k)).equals(v)) // NaN possible
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount + ": "
                        + upf.getDouble(k) + " instead of " + v
                        + ", c = " + c + ", scale = " + scale + ", strict = " + strict
                        + ", srcPos = " + srcPos + ", count = " + count);
                if (!Double.valueOf(pfClone.getDouble(k)).equals(v)) // NaN possible
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount + ": "
                        + "c = " + c + ", scale = " + scale + ", strict = " + strict
                        + ", srcPos = " + srcPos + ", count = " + count);
                if (!Double.valueOf(upfClone.getDouble(k)).equals(v)) // NaN possible
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + "c = " + c + ", scale = " + scale + ", strict = " + strict
                        + ", srcPos = " + srcPos + ", count = " + count);
                if (!Double.valueOf(upfImm.getDouble(k)).equals(v)) // NaN possible
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "c = " + c + ", scale = " + scale + ", strict = " + strict
                        + ", srcPos = " + srcPos + ", count = " + count);
                double x = pa.getDouble(k);
                double vReqDouble = strict ? myStrictPow(x, c, scale) : myPow(x, c, scale);
                double vReq = vReqDouble;
                if (pf instanceof BitArray) {
                    vReq = vReq == 0.0 ? 0.0 : 1.0;
                } else if (pf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = ((PFixedArray) pf).minPossibleValue();
                        double vMax = ((PFixedArray) pf).maxPossibleValue();
                        vReq = vReq < vMin ? vMin : vReq > vMax ? vMax : vReq;
                    } else {
                        vReq = mask(eType, vReq);
                    }
                }
                if (pf instanceof FloatArray) {
                    vReq = (float) vReq; // FloatArray cannot store precise double values
                }
                if (Math.abs(v - vReq) > 1.001)
                    throw new AssertionError("The bug V in asFuncArray found in test #"
                        + testCount + ": "
                        + v + " (" + f.get(x) + ") instead of " + vReq + " (cast " + vReqDouble + ")"
                        + ", x = " + x + ", c = " + c + ", scale = " + scale + ", strict = " + strict
                        + ", k = " + k + ", srcPos = " + srcPos + ", count = " + count
                        + " (" + pf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
            }
            if (!pf.equals(upf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!pf.equals(pfClone))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            if (!upfClone.equals(pf))
                throw new AssertionError("The bug C' in equals found in test #" + testCount);
            if (!upfImm.equals(pf))
                throw new AssertionError("The bug D' in equals found in test #" + testCount);
            if (pf.elementType() == pa.elementType()) {
                int srcPos0 = rnd.nextInt(srcPos + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(pf);
                if (!pa0.equals(pfClone))
                    throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" + testCount
                        + ": srcPos0 = " + srcPos0 + ", srcPos = " + srcPos + ", count = " + count);
                work1.copy(a);
                pa.copy(pf);
                if (!pa.equals(pfClone))
                    throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" + testCount
                        + ": srcPos = " + srcPos + ", count = " + count);
            }
            UpdatablePArray tmp = (UpdatablePArray) SimpleMemoryModel.getInstance().newUnresizableArray(
                upf.elementType(), 1);
            for (int k = 0; k < count; k++) {
                double v = rnd.nextBoolean() ? 5000 + 2000 * rnd.nextDouble() :
                    rnd.nextBoolean() ? rnd.nextGaussian() :
                        rnd.nextGaussian() * 1e-5;
                if (v <= 0.0)
                    continue;
                upf.setDouble(k, v);
                tmp.setDouble(0, v);
                v = tmp.getDouble(0); // really used value
                double x = pa.getDouble(k);
                double xReqDouble = Math.pow(v / scale, 1.0 / c);
                double xReq = xReqDouble;
                if (pa instanceof FloatArray) {
                    xReq = (float) xReq; // FloatArray cannot store precise double values
                } else if (pa instanceof BitArray) {
                    xReq = xReq == 0.0 ? 0.0 : 1.0;
                } else if (pa instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double xMin = Arrays.minPossibleIntegerValue(((PFixedArray) pa).getClass());
                        double xMax = Arrays.maxPossibleIntegerValue(((PFixedArray) pa).getClass());
                        xReq = xReq < xMin ? xMin : xReq > xMax ? xMax : xReq;
                    } else {
                        xReq = mask(pa.elementType(), xReq);
                    }
                }
                if (!Double.isNaN(x) && Math.abs(x - xReq) > 1.001) {
                    throw new AssertionError("The bug X in asUpdatableFuncArray found in test #"
                        + testCount + ": "
                        + x + " instead of " + xReq + " (cast " + xReqDouble
                        + "), srcPos = " + srcPos + ", count = " + count
                        + " (" + v + ", " + pf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testInverseNumberFuncAndUpdatableFunc(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Arrays.asFuncArray\" and "
            + "\"Arrays.asUpdatableFuncArray\" methods (1/x)..."))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            int srcPos = rnd.nextInt(len + 1) / blockSize * blockSize;
            int count = rnd.nextInt(len + 1 - srcPos) / blockSize * blockSize;
            Class<?> eType = rnd.nextInt(3) == 0 ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            Class<? extends UpdatablePArray> urType = Arrays.type(UpdatablePArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            work1.copy(a);
            UpdatablePArray pa = (UpdatablePArray) work1.subArr(srcPos, count);
            PArray inf = Arrays.asFuncArray(truncateOverflows,
                InverseNumberFunc.getInstance(1.78), rType, pa);
            UpdatablePArray uinf = Arrays.asUpdatableFuncArray(truncateOverflows,
                InverseNumberFunc.getUpdatableInstance(1.78), urType, pa);
            PArray infClone = parallelClone(inf);
            PArray uinfClone = parallelClone(uinf);
            PArray uinfImm = uinf.asImmutable();
            for (int k = 0; k < count; k++) {
                double v = inf.getDouble(k);
                if (uinf.getDouble(k) != v)
                    throw new AssertionError("The bug A in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                if (infClone.getDouble(k) != v)
                    throw new AssertionError("The bug B in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                if (uinfClone.getDouble(k) != v)
                    throw new AssertionError("The bug C in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                if (uinfImm.getDouble(k) != v)
                    throw new AssertionError("The bug D in asFuncArray found in test #" + testCount + ": "
                        + "srcPos = " + srcPos + ", count = " + count);
                double vReq = 1.78 / pa.getDouble(k);
                if (inf instanceof BitArray) {
                    vReq = vReq == 0.0 ? 0.0 : 1.0;
                } else if (inf instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double vMin = Arrays.minPossibleIntegerValue(((PFixedArray) inf).getClass());
                        double vMax = Arrays.maxPossibleIntegerValue(((PFixedArray) inf).getClass());
                        vReq = vReq < vMin ? vMin : vReq > vMax ? vMax : vReq;
                    } else {
                        vReq = mask(eType, vReq);
                    }
                }
                if (inf instanceof FloatArray) {
                    vReq = (float) vReq; // FloatArray cannot store precise double values
                }
                if (Math.abs(v - vReq) > 1.001)
                    throw new AssertionError("The bug V in asFuncArray found in test #"
                        + testCount + ": "
                        + v + " instead of " + vReq + ", srcPos = " + srcPos + ", count = " + count
                        + " (" + inf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
            }
            if (!inf.equals(uinf))
                throw new AssertionError("The bug A' in equals found in test #" + testCount);
            if (!inf.equals(infClone))
                throw new AssertionError("The bug B' in equals found in test #" + testCount);
            if (!uinfClone.equals(inf))
                throw new AssertionError("The bug C' in equals found in test #" + testCount);
            if (!uinfImm.equals(inf))
                throw new AssertionError("The bug D' in equals found in test #" + testCount);
            if (inf.elementType() == pa.elementType()) {
                int srcPos0 = rnd.nextInt(srcPos + 1);
                UpdatablePArray pa0 = (UpdatablePArray) work1.subArr(srcPos0, count);
                pa0.copy(inf);
                if (!pa0.equals(infClone))
                    throw new AssertionError("The in-place bug A'' in asFuncArray found in test #" + testCount
                        + ": srcPos0 = " + srcPos0 + ", srcPos = " + srcPos + ", count = " + count);
                work1.copy(a);
                pa.copy(inf);
                if (!pa.equals(infClone))
                    throw new AssertionError("The in-place bug B'' in asFuncArray found in test #" + testCount
                        + ": srcPos = " + srcPos + ", count = " + count);
            }
            UpdatablePArray tmp = (UpdatablePArray) SimpleMemoryModel.getInstance().newUnresizableArray(
                uinf.elementType(), 1);
            for (int k = 0; k < count; k++) {
                double v = rnd.nextBoolean() ? -5000 + 50000 * rnd.nextDouble() :
                    rnd.nextBoolean() ? rnd.nextGaussian() :
                        rnd.nextBoolean() ? rnd.nextGaussian() * 1e-5 :
                            rnd.nextGaussian() * 1e-12;
                uinf.setDouble(k, v);
                tmp.setDouble(0, v);
                v = tmp.getDouble(0); // really used value
                double x = pa.getDouble(k);
                double xReq = 1.78 / v;
                if (pa instanceof FloatArray) {
                    xReq = (float) xReq; // FloatArray cannot store precise double values
                } else if (pa instanceof BitArray) {
                    xReq = xReq == 0.0 ? 0.0 : 1.0;
                } else if (pa instanceof PFixedArray) {
                    if (truncateOverflows) {
                        double xMin = Arrays.minPossibleIntegerValue(((PFixedArray) pa).getClass());
                        double xMax = Arrays.maxPossibleIntegerValue(((PFixedArray) pa).getClass());
                        xReq = xReq < xMin ? xMin : xReq > xMax ? xMax : xReq;
                    } else {
                        xReq = mask(pa.elementType(), xReq);
                    }
                }
                if (Math.abs(x - xReq) > 1.001) {
                    throw new AssertionError("The bug X in asFuncArray found in test #"
                        + testCount + ": "
                        + x + " instead of " + xReq + " (cast " + 1.78 / v
                        + "), srcPos = " + srcPos + ", count = " + count
                        + " (" + v + ", " + inf.elementType()
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)"));
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testCoordFunc(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Matrices.asCoordFuncMatrix\" method"))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            long[] dim = new long[1 + rnd.nextInt(4)];
            int product = 1;
            for (int i = 0; i < dim.length; i++) {
                int lim = product == 0 ? len + 1 : len / product + 1;
                // - at least one zero dimension should lead to trivial results
                lim = Math.min(lim, (int) Math.cbrt(len));
                dim[i] = rnd.nextInt(lim) / blockSize * blockSize;
                product *= dim[i];
                assert product <= len;
            }
            Class<?> eType = rnd.nextBoolean() ? a.elementType() :
                ALL_P_CLASSES[rnd.nextInt(ALL_P_CLASSES.length)];
            Class<? extends PArray> rType = Arrays.type(PArray.class, eType);
            boolean truncateOverflows = rnd.nextBoolean();
            boolean doShift = rnd.nextBoolean();
            double[] bCoeff = new double[dim.length];
            long[] shifts = new long[dim.length];
            if (doShift) {
                for (int i = 0; i < dim.length; i++) {
                    shifts[i] = rnd.nextInt((int) (dim[i] / 2 + 1)) - dim[i] / 4;
                    bCoeff[i] = shifts[i];
                }
            }
            double outsideDouble = a instanceof BitArray ? 1.0 : 157.0;
            PArray pa = (PArray) a.subArr(0, product);
            Matrix<? extends PArray> pm = Matrices.matrix(pa, dim);
            Func f = Matrices.asInterpolationFunc(pm, Matrices.InterpolationMethod.STEP_FUNCTION, outsideDouble);
            Operator op = Operator.IDENTITY;
            if (doShift) {
                op = LinearOperator.getShiftInstance(bCoeff);
            }
            f = op.apply(f);
            Matrix<? extends PArray> cfm = Matrices.asCoordFuncMatrix(truncateOverflows, f, rType, dim);
            Matrix<? extends PArray> sm = pm;
            if (doShift) {
                sm = sm.subMatr(shifts, dim, Matrix.ContinuationMode.getConstantMode(outsideDouble));
            }
            Matrix<? extends PArray> idm = eType == a.elementType() ? sm :
                Matrices.asFuncMatrix(truncateOverflows, Func.IDENTITY, rType, sm);
            for (int k = 0, n = (int) pm.size(); k < n; k++) {
                double v = cfm.array().getDouble(k);
                double vReq = idm.array().getDouble(k);
                if (v != vReq) {
                    long[] coordinatess = pm.coordinates(k, null);
                    double[] doubleCoords = Arrays.toJavaArray(Arrays.asFuncArray(Func.IDENTITY, DoubleArray.class,
                        SimpleMemoryModel.asUpdatableLongArray(coordinatess)));
                    double[] transformed = new double[doubleCoords.length];
                    if (doShift)
                        ((CoordinateTransformationOperator) op).map(transformed, doubleCoords);
                    throw new AssertionError("The bug A in asCoordFuncMatrix found in test #" + testCount
                        + " at " + k + ": "
                        + cfm.array().getDouble(k) + " instead of " + vReq
                        + ", original value " + sm.array().getDouble(k)
                        + ", transformed function result " + f.get(doubleCoords)
                        + ", dim = " + JArrays.toString(dim, ";", 100)
                        + ", indexes = [" + JArrays.toString(coordinatess, ",", 100) + "]"
                        + ", transformed = [" + JArrays.toString(transformed, ",", 100) + "]"
                        + " (" + idm.elementType()
                        + (doShift ? ", with shifting by " + JArrays.toString(shifts, ";", 100) : "")
                        + (truncateOverflows ? ", truncateOverflows)" : ", raw)")
                        + " for " + pm);
                }
            }
            if (!cfm.array().equals(idm.array()))
                throw new AssertionError("The bug B in asCoordFuncMatrix found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + " (" + idm.elementType()
                    + (doShift ? ", with shifting by " + JArrays.toString(shifts, ";", 100) : ", no shift")
                    + (truncateOverflows ? ", truncateOverflows)" : ", raw)")
                    + " for " + pm);
            if (!noCheckStability) {
                for (int k = 0; k < 10; k++) {
                    PArray clone = parallelClone(cfm.array());
                    if (!clone.equals(idm.array())) // unstable behavior!
                        throw new AssertionError("The bug C in asCoordFuncMatrix found in test #" + testCount
                            + (k > 0 ? " (unstable at the call #" + k + ")" : "")
                            + ": dim = " + JArrays.toString(dim, ";", 100)
                            + " (" + idm.elementType()
                            + (doShift ? ", with shifting by " + JArrays.toString(shifts, ";", 100) : ", no shift")
                            + (truncateOverflows ? ", truncateOverflows)" : ", raw)")
                            + " for " + pm);
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    private void testResized(int ti) {
        if (!(a instanceof PArray))
            return;
        if (!title(ti, "Testing \"Matrices.resize/asResized\" methods"))
            return;
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            Matrices.ResizingMethod[] rValues = new Matrices.ResizingMethod[]{
                Matrices.ResizingMethod.SIMPLE, Matrices.ResizingMethod.AVERAGING,
                Matrices.ResizingMethod.POLYLINEAR_INTERPOLATION, Matrices.ResizingMethod.POLYLINEAR_AVERAGING,
                new Matrices.ResizingMethod.Averaging(Matrices.InterpolationMethod.STEP_FUNCTION) {
                    @Override
                    protected Func getAveragingFunc(long[] apertureDim) {
                        return LinearFunc.getAveragingInstance((int) Arrays.longMul(apertureDim));
                    }
                }
            };
            Matrices.ResizingMethod rMethod = rValues[rnd.nextInt(rValues.length)];
            boolean compressionIn2Times = rnd.nextBoolean(); // if so, the test will calculate standard results itself
            boolean compressionInIntegerNumberOfTimes = rnd.nextBoolean(); // enables internal AlgART optimizations
            long[] dim = new long[1 + rnd.nextInt(4)];
            long[] newDim = new long[dim.length];
            int product = 1;
            int apertureSize = 1;
            for (int i = 0; i < dim.length; i++) {
                int lim = product == 0 ? len + 1 : len / product + 1;
                // - at least one zero dimension should lead to trivial results
                lim = Math.min(lim, (int) Math.cbrt(len));
                dim[i] = rnd.nextInt(lim) / blockSize * blockSize;
                if (compressionIn2Times) {
                    if (rMethod.interpolation()
                        // in this case, the simple algorithm below will not work
                        // for non-integer resizing for odd sizes to even sizes
                        || dim[i] <= 5)
                    {
                        // in this case, resizing in 2.5 or 3 times will lead to aperture >2x2x...
                        // and the simple algorithm below will not work
                        dim[i] &= ~1;
                    }
                    newDim[i] = dim[i] / 2; // compression in 2 times by all coordinates
                    apertureSize *= 2;
                } else if (compressionInIntegerNumberOfTimes) {
                    dim[i] = Math.max(dim[i], 1);
                    newDim[i] = (1 + rnd.nextInt((int) dim[i])) / blockSize * blockSize;
                    if (newDim[i] > 0) {
                        dim[i] -= dim[i] % newDim[i];
                    }
                } else {
                    newDim[i] = rnd.nextInt(lim) / blockSize * blockSize;
                }
                product *= Math.max(dim[i], newDim[i]);
                assert product <= len;
            }
            Matrix<? extends PArray> pm = Matrices.matrixAtSubArray((PArray) a, 0, dim);
            long[] tileDim = new long[dim.length];
            for (int i = 0; i < tileDim.length; i++) {
                tileDim[i] = 1 + rnd.nextInt(rnd.nextBoolean() ? 100 : (int) dim[i] + 1);
            }
            if (rnd.nextInt(3) == 0) {
                pm = pm.tile(tileDim);
            }
            Matrix<? extends PArray> rm = Matrices.asResized(rMethod, pm, newDim);
            for (int k = 0, n = (int) rm.size(); k < n; k++) {
                long[] destCoords = rm.coordinates(k, null);
                long[] srcCoords = new long[destCoords.length];
                double v = rm.array().getDouble(k);
                double vReq;
                if (compressionIn2Times) {
                    if (rMethod.averaging()) {
                        double sum = 0.0;
                        for (int m = 0; m < apertureSize; m++) {
                            // use bits of m to enumerate all points in 2x2x...x2 aperture
                            boolean inside = true;
                            for (int j = 0; j < destCoords.length; j++) {
                                srcCoords[j] = (m & (1 << j)) == 0 ? 2 * destCoords[j] : 2 * destCoords[j] + 1;
                                if (srcCoords[j] >= pm.dim(j)) {
                                    inside = false;
                                }
                            }
                            if (inside) { // in other case, 0.0 is used as outside value in asResized
                                sum += pm.array().getDouble(pm.index(srcCoords));
                            }
                        }
                        vReq = sum * (1.0 / apertureSize);
                    } else {
                        for (int j = 0; j < destCoords.length; j++) {
                            srcCoords[j] = 2 * destCoords[j];
                        }
                        vReq = pm.array().getDouble(pm.index(srcCoords));
                    }
                } else {
                    vReq = v;
                }
                ((UpdatablePArray) work1).setDouble(k, vReq);
                vReq = ((PArray) work1).getDouble(k); // casting to the required type
                if (v != vReq) {
                    throw new AssertionError("The bug A in asResized found in test #" + testCount
                        + " at " + k + ": "
                        + rm.array().getDouble(k) + " instead of " + vReq
                        + ", dim = " + JArrays.toString(dim, ";", 100)
                        + ", indexes = [" + JArrays.toString(destCoords, ",", 100) + "]"
                        + " (" + rm.elementType() + ", " + rMethod + ")"
                        + " for " + rm);
                }
            }
            if (!rm.array().equals(work1.subArr(0, rm.size())))
                throw new AssertionError("The bug B in asResized found in test #" + testCount + ": "
                    + "dim = " + JArrays.toString(dim, ";", 100)
                    + " (" + rm.elementType() + ")"
                    + " for " + rm);
            PArray clone = parallelClone(rm.array());
            PArray req = (PArray) work1.subArr(0, rm.size());
            if (rMethod.interpolation()) {
                // without interpolation, little floating-point errors in coordinates can lead to serious difference
                for (int k = 0, n = (int) rm.size(); k < n; k++) {
                    double v = clone.getDouble(k);
                    double vReq = req.getDouble(k);
                    if (Math.abs(v - vReq) > (req instanceof PFixedArray ? 1.001 : 0.001)) {
                        throw new AssertionError("The bug C in asResized found in test #" + testCount
                            + " at " + k + ", "
                            + "indexes = [" + JArrays.toString(rm.coordinates(k, null), ",", 100) + "]: "
                            + v + " instead of " + vReq
                            + ", comparing with standard results: " + compressionIn2Times
                            + ", dim = " + JArrays.toString(dim, ";", 100)
                            + " (" + rm.elementType() + ", " + rMethod + ")"
                            + " for " + rm + ", " + String.format("%n")
                            + Arrays.toString(clone, ";", 100) + " instead of " + String.format("%n")
                            + Arrays.toString(req, ";", 100) + ", built from " + String.format("%n")
                            + Arrays.toString(pm.array(), ";", 300));
                    }
                }
            }
            work1.copy(clone);
            Matrix<? extends UpdatablePArray> pmDest = Matrices.matrixAtSubArray((UpdatablePArray) work2, 0, newDim);
            Matrices.resize(rnd.nextInt(3) == 0 ? ArrayContext.DEFAULT_SINGLE_THREAD : null, rMethod, pmDest, pm);
            if (rMethod.interpolation() || compressionIn2Times) {
                // without interpolation, little floating-point errors in coordinates can lead to serious difference
                for (int k = 0, n = (int) rm.size(); k < n; k++) {
                    double v = pmDest.array().getDouble(k);
                    double vReq = ((PArray) work1).getDouble(k);
                    if (Math.abs(v - vReq) > (compressionIn2Times ? 0.0 :
                        req instanceof PFixedArray ? 1.001 : 0.001))
                    {
                        throw new AssertionError("The bug D in resize/asResized found in test #" + testCount
                            + " at " + k + ", "
                            + "indexes = [" + JArrays.toString(rm.coordinates(k, null), ",", 100) + "]: "
                            + v + " instead of " + vReq
                            + " (lazy is " + rm.array().getDouble(k) + ")"
                            + ", comparing with standard results: " + compressionIn2Times
                            + ", newDim = " + JArrays.toString(newDim, ";", 100)
                            + ", dim = " + JArrays.toString(dim, ";", 100)
                            + " (" + rm.elementType() + ", " + rMethod + ")"
                            + " for " + rm + ", " + String.format("%n")
                            + Arrays.toString(pmDest.array(), ";", 100) + " instead of " + String.format("%n")
                            + Arrays.toString(clone, ";", 100) + ", built from " + String.format("%n")
                            + Arrays.toString(pm.array(), ";", 300));
                    }
                }
            }
            if (!noCheckStability) {
                for (int k = 0; k < 10; k++) {
                    clone = parallelClone(rm.array());
                    req = (PArray) work1.subArr(0, rm.size());
                    if (!clone.equals(req)) // unstable behavior!
                        throw new AssertionError("The bug E in asResized found in test #" + testCount
                            + " (unstable at the call #" + k + ")"
                            + ": dim = " + JArrays.toString(dim, ";", 100)
                            + " (" + rm.elementType() + ", " + rMethod + ")"
                            + " for " + rm);
                }
            }
            showProgress(testCount, numberOfTests);
        }
    }

    public static void main(String[] args) {
//        int a = 10, b = 30000, v = -55333;
//        System.out.println(Arrays.truncate(v, b, a));

        DemoUtils.initializeClass();
        MainOperationsTest test;
        try {
            test = new MainOperationsTest(args);
        } catch (UsageException e) {
            System.out.println("Usage: " + MainOperationsTest.class.getName()
                + " [-noGc] [-noSlowTests] [-noShuffle] [-noCheckStability] "
                + " [-funcOnly] [-multithreading] ALL|" + DemoUtils.possibleArg(true)
                + " arrayLength numberOfTests [blockSize [randSeed [testIndexes]]]");
            System.out.println("If blockSize is specified, all positions and length will be k*blockSize");
            System.out.println("If randSeed is -1 or not specified, the random generator will be "
                + "initialized automatically");
            System.out.println("testIndexes can be specified as a single index \"29\", a range \"10..13\" or "
                + "a set \"5,7,10,24\" (test indexes can be any)");
            return;
        }
        test.testAll();
        if (!test.noGc) {
            test = null; // allows garbage collection
            DemoUtils.fullGC();
        }
    }
}
