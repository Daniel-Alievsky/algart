package net.algart.arrays.demo;

import net.algart.arrays.Arrays;
import net.algart.arrays.MutableCharArray;

import java.util.Locale;

/**
 * <p>Simple test for {@link MutableCharArray#append(String)} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class AppendStringSpeed {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: " + AppendStringSpeed.class.getName()
                + " numberOfConcatenatedWords");
            return;
        }
        int n = Integer.parseInt(args[0]);
        String w = "Hello! ";
        for (int doEnsureCapacity = 0; doEnsureCapacity < 2; doEnsureCapacity++) {
            System.out.println("Tests with" + (doEnsureCapacity == 0 ? "out" : "") + " ensureCapacity()");
            for (int count = 1; count <= 10; count++) {
                System.out.println("Iteration #" + count);
                long t1 = System.nanoTime();
                MutableCharArray cv = Arrays.SystemSettings.globalMemoryModel().newEmptyCharArray();
                if (doEnsureCapacity > 0)
                    cv.ensureCapacity(n * w.length());
                for (int k = 0; k < n; k++)
                    cv.append(w);
                long t11 = System.nanoTime();
                String s1 = Arrays.asCharSequence(cv).toString();
                long t2 = System.nanoTime();

                StringBuffer sbuf = new StringBuffer();
                if (doEnsureCapacity > 0)
                    sbuf.ensureCapacity(n * w.length());
                for (int k = 0; k < n; k++)
                    sbuf.append(w);
                long t21 = System.nanoTime();
                String s2 = sbuf.toString();
                long t3 = System.nanoTime();

                StringBuilder sb = new StringBuilder();
                if (doEnsureCapacity > 0)
                    sb.ensureCapacity(n * w.length());
                for (int k = 0; k < n; k++)
                    sb.append(w);
                long t31 = System.nanoTime();
                String s3 = sb.toString();
                long t4 = System.nanoTime();

                System.out.printf(Locale.US,
                    "CharArray.appendString + toString: %.6f+%.6f ms, %.3f ns/word (%d chars)%n",
                    (t11 - t1) * 1e-6,  (t2 - t11) * 1e-6, (t2 - t1) * 1.0 / n,  cv.length());
                System.out.printf("    '" + (s1.length() < 100 ? s1 : s1.substring(0, 100) + "...") + "'%n");

                System.out.printf(Locale.US,
                    "StringBuffer.append + toString:     %.6f+%.6f ms, %.3f ns/word%n",
                    (t21 - t2) * 1e-6, (t3 - t21) * 1e-6, (t3 - t2) * 1.0 / n);
                System.out.printf("    '" + (s2.length() < 100 ? s2 : s2.substring(0, 100) + "...") + "'%n");
                if (!s2.equals(s1))
                    throw new AssertionError("Internal error: s1 != s2 (" + s1.length() + ", " + s2.length() + ")");

                System.out.printf(Locale.US,
                    "StringBuilder.append + toString:    %.6f+%.6f ms, %.3f ns/word%n",
                    (t31 - t3) * 1e-6, (t4 - t31) * 1e-6, (t4 - t3) * 1.0 / n);
                if (!s3.equals(s1))
                    throw new AssertionError("Internal error: s1 != s3 (" + s1.length() + ", " + s3.length() + ")");
                System.out.println();
            }
            System.out.println();
        }
    }
}
