package net.algart.math.patterns.demo.jre;

import java.util.Collection;
import java.util.Locale;

/**
 * <p>Simple test that allocates very large Java collections (<tt>List</tt> or <tt>Set</tt>).
 * Illustrates correct (or incorrect) error messages while creating more than 2^31 elements.
 * Should be called in 64-bit JVMs.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
@SuppressWarnings("unchecked")
public class TooLargeCollectionTest {
    static final long NUMBER_OF_VALUES = 4500000000L;
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: " + TooLargeCollectionTest.class.getName()
                + " ArrayList|LinkedList|HashSet|TreeSet");
            return;
        }
        Collection<Long> values = (Collection<Long>)Class.forName("java.util." + args[0]).newInstance();
        for (long k = 0; k < NUMBER_OF_VALUES; k++) {
            values.add(k);
            if (k % 1048576 == 0) {
                Runtime rt= Runtime.getRuntime();
                long used = rt.totalMemory() - rt.freeMemory();
                System.out.printf(Locale.US, "\r%d: %.1f%% (%.3f MB, %.3f bytes/element)",
                    k + 1, (k + 1.0) * 100.0 / NUMBER_OF_VALUES, used / 1048576.0, used / (k + 1.0));
            }
        }
        System.out.println("The miracle: " + values.size() + "=" + NUMBER_OF_VALUES + " elements are allocated");
    }
}
