package net.algart.finalizing.demo;

import java.io.*;

/**
 * <p>Test for very intensive calls of <tt>System.gc()</tt> and <tt>System.runFinalization</tt> methods.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.4
 */
public class IntensiveGcTest {
    public static void main(String[] args) throws IOException {
        boolean doGc = false, doRunFinalization = false;
        int startArgIndex = 0;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("gc")) {
                doGc = true;
                startArgIndex++;
            } else if (arg.equalsIgnoreCase("runFinalization")) {
                doRunFinalization = true;
                startArgIndex++;
            }
        }

        if (args.length < startArgIndex + 1) {
            System.out.println("Usage: " + IntensiveGcTest.class.getName()
                + " [gc] [runFinalization] timeoutInMilliseconds");
            return;
        }

        if (!(doGc || doRunFinalization)) {
            System.out.println("No gc or runFinalization flag is set: nothing to do");
            return;
        }
        long timeoutInMilliseconds = Long.parseLong(args[startArgIndex]);

        long tFix = System.currentTimeMillis(), t;
        int count = 0;
        byte[][] data = new byte[1024][];
        do {
            for (int k = 0; k < data.length; k++)
                data[k] = new byte[100 * k];
            data = new byte[1024][];
            for (int k = 0; k < 100; k++) {
                ++count;
                if (doRunFinalization)
                    System.runFinalization();
                if (doGc)
                    System.gc();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    System.err.println("Unexpected interruption: " + e);
                }
            }
            t = System.currentTimeMillis();
            System.out.print("\r" + count + " calls");
        } while (t - tFix < timeoutInMilliseconds);
        System.out.println();
    }
}
