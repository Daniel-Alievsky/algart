package net.algart.arrays.demo.jre;

import java.util.Locale;

/**
 * <p>Speed of System.arraycopy and long[].clone methods in comparison with a simple loop</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class ArraycopyAndCloneSpeed {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: " + ArraycopyAndCloneSpeed.class.getName()
                + " maxArrayLength loopLength numberOfIterations");
            return;
        }
        final int maxLen = Integer.parseInt(args[0]);
        final int n = Integer.parseInt(args[1]);
        int numberOfIterations = Integer.parseInt(args[2]);
        for (int iteration = 1; iteration <= numberOfIterations; iteration++) {
            System.out.println("Test #" + iteration);
            for (int len = 0; len < maxLen; len++) {
                long[] a = new long[len], b = new long[len];
                long t1 = System.nanoTime();
                for (int k = 0; k < n; k++) {
                    for (int j = 0; j < a.length; j++)
                        b[j] = a[j];
                }
                long t2 = System.nanoTime();
                for (int k = 0; k < n; k++) {
                    System.arraycopy(a, 0, b, 0, a.length);
                }
                long t3 = System.nanoTime();
                for (int k = 0; k < n; k++) {
                    b = a.clone();
                }
                long t4 = System.nanoTime();
                System.out.printf(Locale.US, "%d elements: %.3f ns simple loop, %.3f ns arraycopy, %.3f ns clone%n",
                    len, (double)(t2 - t1) / n, (double)(t3 - t2) / n, (double)(t4 - t3) / n);
            }
            System.out.println();
        }
    }
}
