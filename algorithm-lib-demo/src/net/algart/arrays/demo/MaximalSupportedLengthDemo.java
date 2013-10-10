package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.util.Locale;

/**
 * <p>Shows the maximal supported array length for all element types.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class MaximalSupportedLengthDemo {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
    public static void main(String[] args) {
        System.out.println("Current model:");
        System.out.println(mm);
        System.out.println("Plese use -Dnet.algart.arrays.globalMemoryModel JVM argument to specify memory model");
        System.out.println();

        Class<?>[] types = {boolean.class, char.class, byte.class, short.class,
            int.class, long.class, float.class, double.class,
            String.class
        };
        for (Class<?> t : types) {
            System.out.printf(Locale.US, "%-16s - maximal AlgART array length is %#18x (%2$,d)%n",
                t.getCanonicalName(), mm.maxSupportedLength(t));
        }
    }
}
