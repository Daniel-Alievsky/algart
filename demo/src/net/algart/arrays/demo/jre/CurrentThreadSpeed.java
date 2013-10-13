package net.algart.arrays.demo.jre;

import java.util.Locale;

public class CurrentThreadSpeed {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + CurrentThreadSpeed.class.getName()
                + " loopLength numberOfIterations");
            return;
        }
        final int n = Integer.parseInt(args[0]);
        int numberOfIterations = Integer.parseInt(args[1]);
        Thread[] threads = new Thread[n];
        for (int iteration = 1; iteration <= numberOfIterations; iteration++) {
            System.out.print("Test #" + iteration + ": ");
            long t1 = System.nanoTime();
            Thread thread = null;
            for (int k = 0; k < n; k++) {
                thread = Thread.currentThread();
                threads[k] = thread;
            }
            long t2 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                threads[k] = thread;
            }
            long t3 = System.nanoTime();
            System.out.printf(Locale.US, "%.4f ns Thread.currentThread() (%s), %.4f ns simple filling%n",
                (double)(t2 - t1) / n, thread, (double)(t3 - t2) / n);
        }
    }
}
