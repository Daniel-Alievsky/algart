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

package net.algart.arrays;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * <p>Some internal service functions for this package.
 *
 * @author Daniel Alievsky
 */
class InternalUtils {
    private InternalUtils() {}

    static final boolean JAVA_7;
    static final boolean JAVA_32;
    static {
        boolean java7, java32;
        try {
            String prop = System.getProperty("java.version");
            java7 = prop != null && !(prop.startsWith("1.6.") || prop.startsWith("1.5."));
            // this class cannot be compiled in earlier versions
        } catch (Exception ex) {
            java7 = false;
        }
        try {
            String prop = System.getProperty("os.arch");
            java32 = prop != null && !prop.contains("64") && prop.toLowerCase().contains("x86");
            prop = System.getProperty("sun.arch.data.model"); // if exists, it is more robust
            if (prop != null) {
                if (prop.contains("32")) {
                    java32 = true;
                } else if (prop.contains("64")) {
                    java32 = false;
                }
            }
        } catch (Exception ex) {
            java32 = false;
        }
        JAVA_7 = java7;
        JAVA_32 = java32;
    }

    static final boolean SERVER_OPTIMIZATION = getBooleanProperty(
            "net.algart.arrays.serverOptimization", false);
    static final int MAX_AVAILABLE_PROCESSORS = Math.max(1, Math.min(32768,
        getIntPropertyWithImportant("net.algart.arrays.maxAvailableProcessors", JAVA_32 ? 16 : 16384)));
    static final long MAX_TEMP_JAVA_MEMORY = Math.min(1L << 56, Math.max(0L,
        getLongPropertyWithImportant("net.algart.arrays.maxTempJavaMemory", 67108864))); // 64 MB
    static final long MAX_TEMP_JAVA_MEMORY_FOR_TILING = Math.min(1L << 56, Math.max(0L,
        getLongPropertyWithImportant("net.algart.arrays.maxTempJavaMemoryForTiling",
            Math.max(134217728, MAX_TEMP_JAVA_MEMORY)))); // 128 MB
    static final long MAX_MULTITHREADING_MEMORY = Math.min(1L << 56, Math.max(256L,
        getLongPropertyWithImportant("net.algart.arrays.maxMultithreadingMemory",
                1048576))); // 1 MB
    static final boolean PROFILING;
    static {
        boolean ea = false;
        assert ea = true;
        PROFILING = getBooleanProperty("net.algart.arrays.profiling", ea);
    }
    static long longMulAndException(long a, long b) {
        long result = Arrays.longMul(a, b);
        if (result < 0) // Long.MIN_VALUE or just negative
        {
            throw new TooLargeArrayException("Too large desired array length: " + (double) a * (double) b);
        }
        return result;
    }

    static final long[] DEFAULT_MATRIX_TILE_SIDES = {
        -1,
        65536,
        Math.max(16, getLongProperty("net.algart.arrays.matrixTile2D", 4096)), // 4096x4096
        Math.max(4, getLongProperty("net.algart.arrays.matrixTile3D", 256)), // 256x256x256
        Math.max(4, getLongProperty("net.algart.arrays.matrixTile4D", 64)), // 64^4
        Math.max(4, getLongProperty("net.algart.arrays.matrixTile5D", 32)), // 32^5
        Math.max(4, getLongProperty("net.algart.arrays.matrixTileND", 16)), // 16^6, etc.
    };

    static String toHexString(byte v) {
        char[] s = new char[] {
            HEX_DIGITS[(v >>> 4) & 0xF], HEX_DIGITS[v & 0xF]
        };
        return new String(s);
    }

    static String toHexString(short v) {
        char[] s = new char[] {
            HEX_DIGITS[(v >>> 12) & 0xF], HEX_DIGITS[(v >>> 8) & 0xF],
            HEX_DIGITS[(v >>> 4) & 0xF], HEX_DIGITS[v & 0xF]
        };
        return new String(s);
    }

    static String toHexString(int v) {
        char[] s = new char[] {
            HEX_DIGITS[v >>> 28], HEX_DIGITS[(v >>> 24) & 0xF],
            HEX_DIGITS[(v >>> 20) & 0xF], HEX_DIGITS[(v >>> 16) & 0xF],
            HEX_DIGITS[(v >>> 12) & 0xF], HEX_DIGITS[(v >>> 8) & 0xF],
            HEX_DIGITS[(v >>> 4) & 0xF], HEX_DIGITS[v & 0xF]
        };
        return new String(s);
    }

    static String toHexString(long v) {
        return toHexString((int)(v >>> 32)) + toHexString((int)v);
    }

    private static final char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    };

    /**
     * Implementation of {@link Arrays.SystemSettings#availableProcessors()}.
     *
     * @return the number of processor units among <code>Runtime.getRuntime().availableProcessors()</code>,
     *         allowed for usage by AlgART libraries.
     */
    public static int availableProcessors() {
        int n = Runtime.getRuntime().availableProcessors();
        if (n < 1) {
            throw new InternalError("Illegal result of Runtime.getRuntime().availableProcessors()");
        }
        return Math.min(n, MAX_AVAILABLE_PROCESSORS);
    }

    /*
     * If <code>true</code>, the algorithms, processing AlgART arrays,
     * should suppose that the application is executed in the server mode,
     * in particular, with "-server" JVM option.
     * In this case, the algorithms may suppose that access to <code>ByteBuffer</code> elements by
     * <code>get(int index)</code>, <code>put(int index, byte b)</code> methods
     * works with the same (or almost same) speed as access to usual Java array elements.
     * In another case, the algorithm should prefer to process Java arrays.
     *
     * <p>This value is determined while initializing {@link Arrays} class
     * from the system property "net.algart.arrays.serverOptimization", that can contain
     * "<code>true</code>" or "<code>false</code>" value.
     * If there is no such property, this method returns the default <code>false</code> value.
     * If you call an application, that uses this library, in the server mode ("-server" option),
     * you may try to specify "-Dnet.algart.arrays.serverOptimization=true":
     * it may little increase the execution speed of some algorithms.
     *
     * // Deprecated and become undocumented - this property usually just slows down execution
     *
     * @return the value of "net.algart.arrays.serverOptimization" system property, <code>false</code> by default.
     */
    public static boolean serverOptimizationMode() {
        return SERVER_OPTIMIZATION;
    }

    /**
     * An analog of <code>System.getProperty</code>, but returns <code>defaultValue</code>
     * even in a case of exception.
     *
     * <p>More precisely, unlike <code>System.getProperty</code>,
     * this method catches all exceptions, including <code>SecurityException</code>, and returns
     * <code>defaultValue</code> in a case of an exception.
     * In particular, this method returns <code>defaultValue</code> if <code>propertyName</code> is {@code null}
     * or an empty string (<code>System.getProperty</code> throws exceptions in these cases).
     * There is a guarantee that this method never throws exceptions.
     *
     * @param propertyName the name of property.
     * @param defaultValue default value; {@code null} is an allowed value.
     * @return             the value of string property or default value in a case of any problems.
     */
    static String getStringProperty(String propertyName, String defaultValue) {
        try {
            return System.getProperty(propertyName, defaultValue);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * An analog of <code>Integer.getInteger</code>, but "understands" suffixes K, M, G, that mean "Kilo" (1024),
     * "Mega" (1048576) and "Giga" (1073741824) and returns <code>defaultValue</code>
     * even in a case of any exception (including security exception).
     *
     * @param propertyName the name of property.
     * @param defaultValue default value.
     * @return             the value of integer property or default value in a case of any problems.
     */
    static int getIntProperty(String propertyName, int defaultValue) {
        try {
            return parseIntWithMetricalSuffixes(System.getProperty(
                propertyName, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    static int getIntPropertyWithImportant(String propertyName, int defaultValue) {
        try {
            String s = System.getProperty(propertyName + "Important");
            if (s == null) {
                s = System.getProperty(propertyName, String.valueOf(defaultValue));
            }
            return parseIntWithMetricalSuffixes(s);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * An analog of <code>Integer.getInteger</code>, but returns <code>defaultValue</code>
     * even in a case of any exception (including security exception).
     *
     * @param propertyName the name of property.
     * @param defaultValue default value.
     * @return             the value of integer property or default value in a case of any problems.
     */
    static Integer getIntegerWrapperProperty(String propertyName, Integer defaultValue) {
        try {
            return Integer.getInteger(propertyName, defaultValue);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * An analog of <code>Long.getLong</code>, but "understands" suffixes K, M, G, T, that mean "Kilo" (1024),
     * "Mega" (1048576), "Giga" (1073741824) and "Tera" (1099511627776) and returns <code>defaultValue</code>
     * even in a case of any exception (including security exception).
     *
     * @param propertyName the name of property.
     * @param defaultValue default value.
     * @return             the value of long-integer property or default value in a case of any problems.
     */
    static long getLongProperty(String propertyName, long defaultValue) {
        try {
            return parseLongWithMetricalSuffixes(System.getProperty(propertyName, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    static long getLongPropertyWithImportant(String propertyName, long defaultValue) {
        try {
            String s = System.getProperty(propertyName + "Important");
            if (s == null) {
                s = System.getProperty(propertyName, String.valueOf(defaultValue));
            }
            return parseLongWithMetricalSuffixes(s);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * An analog of <code>Integer.getInteger</code>, but works with double values and
     * returns <code>defaultValue</code>
     * even in a case of any exception (including security exception).
     *
     * @param propertyName the name of property.
     * @param defaultValue default value.
     * @return             the value of double property or default value in a case of any problems.
     */
    static double getDoubleProperty(String propertyName, double defaultValue) {
        try {
            return Double.parseDouble(System.getProperty(propertyName, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * An analog of <code>Boolean.getBoolean</code>, but returns <code>defaultValue</code>
     * if there is no required property, or it doesn't equal "<code>true</code>" or "<code>false</code>",
     * or in a case of any exception. (The test of this string is case insensitive.)
     *
     * <p>Unlike <code>Boolean.getBoolean</code>,
     * this method catches all exceptions, including <code>SecurityException</code>, and returns
     * <code>defaultValue</code> in a case of an exception.
     * There is a guarantee that this method never throws exceptions.
     *
     * @param propertyName the name of property.
     * @param defaultValue default value.
     * @return             the value of boolean property or default value in a case of any problems.
     */
    static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        if (defaultValue) {
            return getBooleanDefTrue(propertyName);
        } else {
            try {
                return Boolean.getBoolean(propertyName);
            } catch (Exception ex) {
                return false;
            }
        }
    }

    static int parseIntWithMetricalSuffixes(String s) {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        int sh = 0;
        if (s.endsWith("K") || s.endsWith("k")) {
            sh = 10; s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("M") || s.endsWith("m")) {
            sh = 20; s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("G") || s.endsWith("g")) {
            sh = 30; s = s.substring(0, s.length() - 1);
        }
        int result = Integer.parseInt(s);
        if (((result << sh) >> sh) != result) // overflow
        {
            throw new NumberFormatException("Too large 32-bit integer value");
        }
        return result << sh;
    }

    static long parseLongWithMetricalSuffixes(String s) {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        int sh = 0;
        if (s.endsWith("K") || s.endsWith("k")) {
            sh = 10; s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("M") || s.endsWith("m")) {
            sh = 20; s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("G") || s.endsWith("g")) {
            sh = 30; s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("T") || s.endsWith("t")) {
            sh = 40; s = s.substring(0, s.length() - 1);
        }
        long result = Long.parseLong(s);
        if (((result << sh) >> sh) != result) // overflow
        {
            throw new NumberFormatException("Too large 64-bit long integer value");
        }
        return result << sh;
    }

    static final String LF = String.format("%n");

    private static boolean getBooleanDefTrue(String name) {
        try {
            return !"false".equalsIgnoreCase(System.getProperty(name));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Returns <code>(T)object</code>. Allows to minimize unchecked warning in another classes.
     *
     * @param object some object.
     * @return T     the same object.
     */
    @SuppressWarnings("unchecked")
    static <T> T cast(Object object) {
        return (T) object;
    }

    static <T> T getClassInstance(String propertyName, String defaultClassName,
        Class<T> requiredClass, Logger logger, String additionalMessage,
        String ...aliases) {
        String className;
        try {
            className = System.getProperty(propertyName);
        } catch (Exception ex) {
            className = null;
        }
        if (className == null) {
            className = defaultClassName;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            for (int k = 0; k < aliases.length; k += 2) {
                if (className.equals(aliases[k])) {
                    className = aliases[k + 1];
                    break;
                }
            }
            try {
                Class<?> clazz = Class.forName(className);
                Object inst;
                try {
                    inst = clazz.getDeclaredConstructor().newInstance();
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                         InvocationTargetException ex) {
                    inst = null;
                }
                if (inst == null) {
                    Method method = clazz.getDeclaredMethod("getInstance");
                    inst = method.invoke(null);
                }
                if (inst == null) {
                    throw new IllegalArgumentException("Illegal class " + className + ": it creates null instance");
                }
                return requiredClass.cast(inst);
            } catch (Exception ex1) {
                if (className.equals(defaultClassName)) {
                    throw new InternalError(ex1.toString());
                }
                logger.severe("Cannot create an instance of " + className + " class: " + ex1);
                logger.severe(additionalMessage);
                className = defaultClassName;
            }
        }
        throw new AssertionError("Should never occur");
    }

    // This method should be located here, not in Arrays, to allow correct initialization of Arrays class
    static void addShutdownTask(Runnable task, Arrays.TaskExecutionOrder whenToExecute) {
        synchronized (Hooks.hookLock) {
            if (whenToExecute == null) {
                Hooks.tasksStandard.add(task);
            } else if (whenToExecute == Arrays.TaskExecutionOrder.BEFORE_STANDARD) {
                Hooks.tasksBefore.add(task);
            } else if (whenToExecute == Arrays.TaskExecutionOrder.AFTER_STANDARD) {
                Hooks.tasksAfter.add(task);
            } else {
                throw new AssertionError("Illegal whenToExecute value");
            }
            if (!Hooks.hookInstalled) {
                Runtime.getRuntime().addShutdownHook(new Hooks());
                Hooks.hookInstalled = true;
            }
        }
    }

    private static class Hooks extends Thread {
        private static final List<Runnable> tasksBefore = new ArrayList<>();
        private static final List<Runnable> tasksStandard = new ArrayList<>();
        private static final List<Runnable> tasksAfter = new ArrayList<>();
        private static final Object hookLock = new Object();
        private static boolean hookInstalled = false;
        public void run() {
            java.util.List<Runnable> allTasks = new java.util.ArrayList<>();
            synchronized (hookLock) {
                allTasks.addAll(tasksBefore);
                allTasks.addAll(tasksStandard);
                allTasks.addAll(tasksAfter);
            }
            for (Runnable task : allTasks) {
                try {
                    task.run();
                } catch (Throwable ex) {
                    System.err.println("Unexpected error while AlgART arrays shutdown hook! " + ex);
                }
            }
        }
    }
}
