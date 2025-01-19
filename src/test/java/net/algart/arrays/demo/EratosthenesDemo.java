/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.contexts.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Locale;

/**
 * <p>Sieve of Eratoshpenes: test for {@link BitArray} class.</p>
 *
 * @author Daniel Alievsky
 */
public class EratosthenesDemo {
    private boolean useBitSet = false;
    private MutableBitArray startPattern = null;
    private final MemoryModel mm;

    private int n;
    private MutableBitArray a = null;
    private BitSet bs = null;
    private long numberOfPrimes = 0;

    public EratosthenesDemo(Context context) {
        if (context.is(ArrayMemoryContext.class))
            mm = context.as(ArrayMemoryContext.class).getMemoryModel();
        else
            mm = SimpleMemoryModel.getInstance();
    }

    public void setPattern() {
        int startLen = 2 * 3 * 5 * 7 * 11 * 13 * 17;

        startPattern = mm.newBitArray(32 * startLen); // length = 64*integer for best performance
        for (int k = 0; k < 32; k++)
            startPattern.subArr(startLen * k, startLen).copy(new AbstractBitArray(startLen, false) {
                public boolean getBit(long k) {
                    return (k == 2 || k % 2 != 0) &&
                        (k == 3 || k % 3 != 0) &&
                        (k == 5 || k % 5 != 0) &&
                        (k == 7 || k % 7 != 0) &&
                        (k == 11 || k % 11 != 0) &&
                        (k == 13 || k % 13 != 0) &&
                        (k == 17 || k % 17 != 0);
                }
            });
        System.out.println("Service set prepared (" + startPattern + ")");
        System.out.println("    (" +
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
            / 1024.0 / 1024.0 + " MB used)");
    }

    public void setUseBitSet() {
        this.useBitSet = true;
    }

    public void process(Context context, int n) {
        ProgressUpdater pu = context.as(ProgressUpdater.class);
        StatusUpdater su = context.as(StatusUpdater.class);
        this.n = n;
        long t1, t2;
        t1 = System.nanoTime();
        if (useBitSet) {
            bs = new BitSet(n + 1);
        } else {
            a = mm.newBitArray(n + 1);
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Allocated %d bits: %.6f ms%n", n, (t2 - t1) * 1e-6);
        System.out.printf(Locale.US, "    (%.6f MB used)%n",
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0);

        t1 = System.nanoTime();
        if (startPattern != null) {
            if (useBitSet) {
                boolean[] ptn = startPattern.toJavaArray();
                t1 = System.nanoTime(); // not timing our method
                for (int k = 0; k <= n; ) {
                    int kMax = Math.min(k + ptn.length, n + 1);
                    for (int j = 0; k < kMax; j++, k++)
                        if (ptn[j]) // can be more simple under JDK 1.4
                            bs.set(k);
                        else
                            bs.clear(k);
                }
            } else {
                int len = (int) startPattern.length();
                for (int k = 0; k <= n; k += len)
                    a.subArray(k, Math.min(k + len, n + 1)).copy(startPattern);
            }
        } else {
            if (useBitSet) {
                bs.set(0, n + 1);
            } else {
                a.fill(true);
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Filled: %.6f ms, %.3f ns/bit%n", (t2 - t1) * 1e-6, (t2 - t1) / (double)n);

        t1 = System.nanoTime();
        for (int p = 2; p * p <= n; p++) {
            if (useBitSet) {
                if (bs.get(p)) {
                    for (int m = 2 * p; m <= n; m += p)
                        bs.clear(m);
                }
            } else {
                if (a.getBit(p)) {
                    for (int m = 2 * p; m <= n; m += p)
                        a.clearBitNoSync(m);
                }
            }
            if (p % (p < 5 ? 2 : p < 50 ? 5 : p < 200 ? 20 : p < 2000 ? 200 : 2000) == 0) {
                su.updateStatus("          " + p + "/" + (int)Math.sqrt(n) + "...");
                pu.updateProgress(p / Math.sqrt(n), true);
            }
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "Complete: %.6f ms, %.3f ns/bit%n", (t2 - t1) * 1e-6, (t2 - t1) / (double)n);

        double cardTime = Long.MAX_VALUE;
        for (int m = 0; m < 20; m++) { // repeat 20 times for better measuring
            t1 = System.nanoTime();
            if (useBitSet) {
                bs.clear(0);
                bs.clear(1);
                numberOfPrimes = bs.cardinality();
            } else {
                numberOfPrimes = Arrays.cardinality(a.subArray(2, n));
            }
            t2 = System.nanoTime();
            cardTime = Math.min(cardTime, t2 - t1);
        }
        System.out.printf(Locale.US, "Calculating number of primes (%d): %.6f ms, %.3f ns/bit%n",
            numberOfPrimes, cardTime, cardTime / n);
        System.out.println();
    }

    public long numberOfPrimes() {
        return this.numberOfPrimes;
    }

    public IntArray getPrimes() {
        MutableIntArray primes = mm.newEmptyIntArray();
        if (useBitSet) {
            for (int p = 2; p <= n; p++) {
                if (bs.get(p))
                    primes.pushInt(p);
            }
        } else {
            for (int p = 2; p <= n; p++) {
                if (a.getBit(p))
                    primes.pushInt(p);
            }
        }
        return primes;
    }

    public static void main(String[] args) throws IOException {
        int startArgIndex = 0;
        boolean usePattern = false, useBitSet = false;
        for (int k = 0; k < 2 && startArgIndex < args.length; k++) {
            if (args[startArgIndex].equalsIgnoreCase("-patternForFirstPrimes")) {
                usePattern = true; startArgIndex++;
            } else if (args[startArgIndex].equalsIgnoreCase("-BitSet")) {
                useBitSet = true; startArgIndex++;
            }
        }
        if (args.length == startArgIndex) {
            System.out.println("Usage: " + EratosthenesDemo.class.getName()
                + " [-patternForFirstPrimes] [-BitSet] maxTestedInteger [numberOfTests]");
            System.out.println("Results will be stored in \"prime.txt\" file.");
            return;
        }

        Context context = new DefaultContext() {
            @Override
            public void updateStatus(String message, boolean force) {
                System.out.print("\r" + message + "\r");
            }
        };
//        context = new SubContext(context, SimpleMemoryModel.getInstance());
//        context = new SubContext(context, ArrayMemoryContext.class);
        EratosthenesDemo er = new EratosthenesDemo(context);
        System.out.println(er.mm);
        System.out.println();
        if (usePattern)
            er.setPattern();
        if (useBitSet)
            er.setUseBitSet();

        final int n = Integer.parseInt(args[startArgIndex]);
        final int iterationCount = startArgIndex + 1 >= args.length ? 1 : Integer.parseInt(args[startArgIndex + 1]);

        for (int count = 0; count < iterationCount; count++) {
            Context sc = new SubtaskContext(context,
                (double)count / (double)iterationCount,
                (double)(count + 1) / (double)iterationCount);
            sc = sc.as(StatusUpdater.class); // must stay a context of the subtask after this!
            sc = sc.as(ProgressUpdater.class);
            sc = sc.as(InterruptionContext.class);
            System.out.println("*** ITERATION #" + (count + 1)
                + (useBitSet ? " with java.util.BitSet" : "")
                + (usePattern ? " (special check for first 17 primes)" : ""));
            er.process(sc, n);
        }

        long t1 = System.nanoTime();
        IntArray primes = er.getPrimes();
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Found %d prime numbers: %.6f ms, %.3f ns/bit, %.3f ns/number%n",
            primes.length(), (t2 - t1) * 1e-6, (t2 - t1) / (double)n, (t2 - t1) / (double)primes.length());
        System.out.println("    " + primes);
        System.out.printf(Locale.US, "    (%.6f MB used)%n",
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0);
        if (primes.length() != er.numberOfPrimes())
            throw new AssertionError("Bug found: illegal number of primes is calculated");
        er = null;
        System.gc();

        FileWriter fw = new FileWriter("prime.txt");
        for (long k = 0; k < primes.length(); ) {
            long kMax = Math.min(k + 100000, primes.length());
            fw.write(Arrays.toString(primes.subArray(k, kMax), String.format("%n"), Integer.MAX_VALUE));
            fw.write(String.format("%n"));
            k = kMax;
        }
        fw.close();
        System.out.println("Report saved in \"prime.txt\"");

        primes = null; // allows garbage collection
        try {
            Arrays.gcAndAwaitFinalization(10000);
        } catch (InterruptedException ex) {
        }
    }
}
