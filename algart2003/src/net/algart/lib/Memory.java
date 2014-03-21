/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib;

/**
 * <p>Miscellaneous tools for memory manager control.
 * Mainly for debugging and tests.
 *
 * <p>This class can be executed since JVM 1.0 and can be compiled since JDK 1.1.
 *
 * @version 1.0
 * @author  Daniel Alievsky
 * @since   JVM1.0, JDK1.1
 */


public final class Memory {

    /**
    * Don't let anyone instantiate this class.
    */
    private Memory() {}

    public static final int MEMORY_OF_CHAR= 2;
    public static final int MEMORY_OF_BYTE= 1;
    public static final int MEMORY_OF_SHORT= 2;
    public static final int MEMORY_OF_INT= 4;
    public static final int MEMORY_OF_LONG= 8;
    public static final int MEMORY_OF_FLOAT= 4;
    public static final int MEMORY_OF_DOUBLE= 8;
    // No constant for boolean: Java doesn't specify sizeof(boolean)

    public static int memoryOfPrimitive(Class clazz) {
        // 0 means void; -1 means unknown or null class
        if (clazz==null) return -1;
        if (clazz==boolean.class) return -1;
        if (clazz==char.class)    return MEMORY_OF_CHAR;
        if (clazz==byte.class)    return MEMORY_OF_BYTE;
        if (clazz==short.class)   return MEMORY_OF_SHORT;
        if (clazz==int.class)     return MEMORY_OF_INT;
        if (clazz==long.class)    return MEMORY_OF_LONG;
        if (clazz==float.class)   return MEMORY_OF_FLOAT;
        if (clazz==double.class)  return MEMORY_OF_DOUBLE;
        if (clazz==void.class)    return 0;
        return -1;
    }

    public static long used() {
        Runtime rt= Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
    public static long free() {
        return Runtime.getRuntime().freeMemory();
    }
    public static long total() {
        return Runtime.getRuntime().totalMemory();
    }
    public static String info() {
        Runtime rt= Runtime.getRuntime();
        return info(rt.freeMemory(),rt.totalMemory(),false);
    }
    public static String info(long free, long total, boolean bytePrecision) {
        return Out.dec((total-free)/1048576.0,3) + "M used +"
            + Out.dec(free/1048576.0,3) + "M free ="
            + Out.dec(total/1048576.0,3) + "M"
            + (bytePrecision? " [" + (total-free) + "/" + total + "]": "");
    }

    public static long gc() {
        return gc(false,false);
    }
    public static long gc(boolean aggressive, boolean printInfo) {
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory(), total = rt.totalMemory();
        for (int count = 1; count <= 100; count++) {
            long t1 = Timing.timems();
            long freeLast = free, totalLast = total;
            if (JVM.JAVA_11_SERIALIZATION_SUPPORTED)
                rt.runFinalization(); // some old JVM 1.0 doesn't perform runFinalization() properly
            rt.gc();
            free = rt.freeMemory(); total = rt.totalMemory();
            long t2 = Timing.timems();
            if (printInfo) Out.println(" **Memory** GC: "
                + (free > freeLast? info(freeLast,totalLast,false) + " => ": "")
                + info(free,total,free <= freeLast)
                + (free <= freeLast? ", no effect - ": " - ")
                + (t2-t1) + " ms"
                + (aggressive? " (" + count + ")": ""));
            if (!aggressive || free <= freeLast)
                return free;
        }
        return free;
    }

}