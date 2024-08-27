
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

package net.algart.arrays;

import net.algart.math.Range;
import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.SelectConstantFunc;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// NOTE: this class MUST NOT BE USED while initializing classes of any memory models from this package!
// The reason is that this class creates memory model instances while its initialization:
// GLOBAL_MEMORY_MODEL constant.
// So, all memory model classes must be completely initialized BEFORE this class.

/**
 * <p>A set of static methods useful for working with {@link Array AlgART arrays}.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class Arrays {
    private Arrays() {
    }

    static final Logger LOGGER = Logger.getLogger(Arrays.class.getName());
    static final boolean CONFIG_LOGGABLE = LargeMemoryModel.LOGGER.isLoggable(Level.CONFIG);

    static final boolean DEBUG_MODE = false; // enables or disables internal check of parallel copying
    private static final int MAX_NUMBER_OF_RANGES_FOR_PRECISE_LENGTH_PER_TASK_ESTIMATION = 10000;

    static {
        Class<?> dummy = InternalUtils.class;
        SimpleMemoryModel.getInstance();
        BufferMemoryModel.getInstance();
        LargeMemoryModel.getInstance();
        dummy = MappedDataStorages.class;
        dummy = DirectDataStorages.class;
        SystemSettings.globalMemoryModel();
        // - Initialization of the main classes: necessary to avoid a strange deadlock in JVM,
        // that occurs if the first access to our classes is performed from the shutdown hook.
        // In any case, it is useful to be sure that initialization of our classes,
        // that includes synchronization while creating default instances,
        // will not be performed at unexpected moment.
        // Important: this initialization must be performed AFTER initialization previous static constants
        // of Arrays class, in particular, after LOGGER initialization: some classes from this list
        // use LOGGER to report about initialization errors.
    }

    /**
     * <p>A set of static methods for getting some important global settings,
     * stored in system properties and used for customizing modules processing {@link Array AlgART arrays}.</p>
     *
     * <p>All settings returned by this class are determined
     * (loaded from the system properties) while initializing {@link Arrays} class
     * and cannot be modified after this moment.</p>
     *
     * <p>This class cannot be instantiated.</p>
     */
    public static class SystemSettings {
        private SystemSettings() {
        }

        private static final String VERSION = "1.4.21";
        private static final int[] PARSED_VERSION;

        static {
            PARSED_VERSION = java.util.Arrays.stream(VERSION.split("\\.")).mapToInt(Integer::parseInt).toArray();
            if (PARSED_VERSION.length > 4) {
                throw new ExceptionInInitializerError("Too many elements (>4) in AlgART version: " + VERSION);
            }
        }

        private static final MemoryModel GLOBAL_MEMORY_MODEL = getGlobalMemoryModel();

        private static final double MAX_FREE_HEAP_SPACE_USAGE_FOR_TILING = 0.8; // 80%

        private static MemoryModel getGlobalMemoryModel() {
            MemoryModel mm = InternalUtils.getClassInstance(
                    "net.algart.arrays.globalMemoryModel",
                    "SIMPLE",
                    MemoryModel.class,
                    LOGGER, "Simple memory model will be used",
                    "SIMPLE", SimpleMemoryModel.class.getName(),
                    "BUFFER", BufferMemoryModel.class.getName(),
                    "LARGE", LargeMemoryModel.class.getName());
            if (!(mm.areAllPrimitiveElementTypesSupported())) {
                LOGGER.severe("Illegal global memory model " + mm.getClass().getName() + ": "
                        + "it does not support all primitive element types");
                LOGGER.severe("Simple memory model will be used");
            }
            return mm;
        }

        private static final DiskSynchronizer GLOBAL_DISK_SYNCHRONIZER = InternalUtils.getClassInstance(
                "net.algart.arrays.globalDiskSynchronizer",
                DefaultDiskSynchronizerLocking.class.getName(),
                DiskSynchronizer.class,
                LOGGER, "Default (single-thread) disk synchronizer will be used");

        /**
         * The maximal number of processors, that are allowed for simultaneous usage by AlgART libraries.
         * See {@link #availableProcessors()}.
         *
         * <p>This value is determined while initializing {@link Arrays} class
         * from the system property "net.algart.arrays.maxAvailableProcessors" by the call
         * <code>System.getProperty("net.algart.arrays.maxAvailableProcessors","16384")</code>
         * (on 64-bit Java machines) or
         * <code>System.getProperty("net.algart.arrays.maxAvailableProcessors","16")</code> (on 32-bit Java machines).
         * If this property contains zero or a negative integer, it is supposed to be 1,
         * that means disabling any attempts of multiprocessor optimization.
         * If this property contains an integer greater than the limit 32768,
         * this limit is used instead.
         * If some exception occurs while calling <code>System.getProperty</code> or if it is not an integer,
         * it will contain the default value 16384/16 (on 64/32-bit Java machines correspondingly).
         *
         * <p>32-bit and 64-bit Java are distinguished via {@link #isJava32()} method.
         * Please remember that the result of that method is not 100% robust.
         *
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks()
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks(Array)
         */
        public static final int MAX_AVAILABLE_PROCESSORS = InternalUtils.MAX_AVAILABLE_PROCESSORS;

        /**
         * The name of system property (<code>{@value}</code>),
         * returned by {@link #cpuCountProperty()} method.
         *
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks()
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks(Array)
         */
        public static final String CPU_COUNT_PROPERTY_NAME = "net.algart.arrays.CPUCount";

        /**
         * The recommended time interval, in milliseconds, for calling
         * {@link ArrayContext#checkInterruption} and {@link ArrayContext#updateProgress ArrayContext.updateProgress}
         * methods by {@link Arrays.ParallelExecutor} and {@link AbstractIterativeArrayProcessor} classes.
         *
         * <p>The value of this constant is {@value} (milliseconds).
         */
        public static final int RECOMMENDED_TIME_OF_NON_INTERRUPTIBLE_PROCESSING = 250;

//        /**
//         * The maximal time interval, in milliseconds, for calling
//         * {@link ArrayContext#checkInterruption} and {@link ArrayContext#updateProgress ArrayContext.updateProgress}
//         * methods by {@link Arrays.ParallelExecutor} class.
//         * That class tries to estimate the calculation speed and choose the number of elements,
//         * processed in a single loop iteration, little enough to provide calling
//         * {@link ArrayContext#checkInterruption()} and {@link ArrayContext#updateProgress ArrayContext
//         .updateProgress}
//         * not less frequently than every <code>MAX_TIME_OF_NON_INTERRUPTIBLE_PROCESSING</code> milliseconds.
//         * It is useful for slow algorithms, when the default buffer size (tens of thousands elements)
//         * is processed during many seconds or even hours: in this case, the buffer size is automatically reduced
//         * according to this constant.
//         *
//         * <p>The value of this constant is {@value} (milliseconds).
//         */
//        public static final int MAX_TIME_OF_NON_INTERRUPTIBLE_PROCESSING = 2000;

        /**
         * Minimal elapsed time, in milliseconds, of long-working operations, when some classes
         * write (to logs) additional profiling information about such operations
         * (in a case when {@link #profilingMode()} returns <code>true</code>).
         *
         * <p>The value of this constant is {@value} (milliseconds).
         */
        public static final int RECOMMENDED_ELAPSED_TIME_FOR_ADDITIONAL_LOGGING = 3000;

        /**
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * this optimization uses a buffer in RAM ({@link SimpleMemoryModel}'s array), the size of which in bytes
         * is the maximum from this value and {@link #maxTempJavaMemory()}.
         * This value can be used for similar needs in another modules.
         *
         * <p>The default value of this constant is 524288 (512 KB) and can be customized
         * via (undocumented) system properties.
         */
        public static final int MIN_OPTIMIZATION_JAVA_MEMORY = Math.min(1073741824, // 1GB
                InternalUtils.getIntProperty("net.algart.arrays.minOptimizationJavaMemory", 524288));

        /**
         * <i>(Internal)</i>
         * The flag enabling optimization of {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)}
         * method in "non-strict" mode.
         * Should be <code>true</code> always; <code>false</code> value is useful for debugging only.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final boolean BLOCK_OPTIMIZATION_FOR_COORDINATE_TRANSFORMATION =
                InternalUtils.getBooleanProperty("net.algart.arrays.blockOptimizationForCoordinateTransformation",
                        true);

        /**
         * <i>(Internal)</i>
         * The flag enabling optimization of {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)}
         * method in "non-strict" mode in a case of resizing whole matrix, created by
         * {@link Matrices#asResized(Matrices.ResizingMethod, Matrix, long...)} method.
         * Should be <code>true</code> always; <code>false</code> value is useful for debugging only.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final boolean BLOCK_OPTIMIZATION_FOR_RESIZING_WHOLE_MATRIX =
                InternalUtils.getBooleanProperty("net.algart.arrays.blockOptimizationForResizingWholeMatrix", true);

        /**
         * <i>(Internal)</i>
         * The flag enabling optimization of {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)}
         * method in "non-strict" mode in a case of resizing coordinate transformation.
         * Should be <code>true</code> always; <code>false</code> value is useful for debugging only.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final boolean BLOCK_OPTIMIZATION_FOR_RESIZING_TRANSFORMATION =
                InternalUtils.getBooleanProperty("net.algart.arrays.blockOptimizationForResizingTransformation", true);

        /**
         * <i>(Internal)</i>
         * The flag enabling optimization of {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)}
         * method in "non-strict" mode in a case of affine coordinate transformation.
         * Should be <code>true</code> always; <code>false</code> value is useful for debugging only.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final boolean BLOCK_OPTIMIZATION_FOR_AFFINE_TRANSFORMATION =
                InternalUtils.getBooleanProperty("net.algart.arrays.blockOptimizationForAffineTransformation", true);

        /**
         * <i>(Internal)</i>
         * The flag enabling optimization of {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)}
         * method in "non-strict" mode in a case of projective coordinate transformation.
         * Should be <code>true</code> always; <code>false</code> value is useful for debugging only.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final boolean BLOCK_OPTIMIZATION_FOR_PROJECTIVE_TRANSFORMATION =
                InternalUtils.getBooleanProperty("net.algart.arrays.blockOptimizationForProjectiveTransformation",
                        true);

        /**
         * <i>(Internal)</i>
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * this optimization tries to preload whole original matrix in RAM (if it is little enough)
         * only if the resulting matrix depends on the specified part of the original one.
         * In another case (for example, if the resulting matrix is fully defined by
         * a little fragment of the original one) we use the common algorithms
         * of splitting the matrix into layers or tiles.
         *
         * <p>The value of this constant is 0.7 (70%).
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final double MIN_USED_PART_FOR_PRELOADING_WHOLE_MATRIX_WITH_BLOCK_OPTIMIZATION = 0.7;

        /**
         * <i>(Internal)</i>
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "strict" mode:
         * this optimization tries to preload whole original matrix in RAM (if it is little enough)
         * only if the resulting matrix depends on the specified part of the original one.
         * In another case (for example, if the resulting matrix is fully defined by
         * a very little fragment of the original one) we do not optimize copying:
         * even slow access to the original matrix elements (when it is created by
         * {@link LargeMemoryModel}) is better idea than downloading extra elements in RAM.
         *
         * <p>The value of this constant is 0.01 (1%). In other words, if more than 99% of the original matrix
         * is not used, we prefer to use slow access to elements than to download &gt;99% unused elements.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final double MIN_USED_PART_FOR_PRELOADING_WHOLE_MATRIX_WITHOUT_BLOCK_OPTIMIZATION = 0.01;

        /**
         * <i>(Internal)</i>
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * layer optimization is not used in a case of simple compressing resizing without averaging,
         * if total compression is stronger than this value.
         * In this situation, even slow access to the original matrix elements (when it is created by
         * {@link LargeMemoryModel}) is better idea than downloading extra elements in RAM.
         *
         * <p>The value of this constant is {@value} (times).
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final double MIN_NON_OPTIMIZED_SIMPLE_COORDINATE_COMPRESSION = 200.0;

        /**
         * <i>(Internal)</i>
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode,
         * the case of resizing coordinate transformation:
         * layer optimization is used only if the resulting matrix depends on the specified part of every layer.
         * In another case, we prefer to use tiling algorithm, which does not load extra parts of the matrix.
         *
         * <p>The value of this constant is 0.7 (70%).
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final double MIN_LAYER_USED_PART_FOR_LAYER_OPTIMIZATION = 0.7;

        /**
         * <i>(Internal)</i>
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode,
         * the case of affine coordinate transformation:
         * tile optimization is not used in a case of a common affine transformation without averaging,
         * if it is a compression, stronger than this value.
         * In this situation, even slow access to the original matrix elements (when it is created by
         * {@link LargeMemoryModel}) is better idea than downloading extra elements in RAM.
         *
         * <p>The value of this constant is {@value} (times).
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final double MIN_NON_OPTIMIZED_COMPRESSION_FOR_TILING = 100.0;

        /**
         * <i>(Internal)</i>
         * The control parameter for optimization of
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode,
         * the case of projective coordinate transformation:
         * recursive splitting algorithm does not try to split a tile, if
         * the total number of its elements is less than this value.
         * For very small matrices it is much faster to calculate all elements directly,
         * than to perform a complex recursive algorithm.
         *
         * <p>The value of this constant is {@value} (elements). Must be positive.
         *
         * <p>Please do not use this constant in your code: it may be deleted or renamed in future versions.
         */
        public static final int MIN_OPTIMIZATION_RESULT_TILE_VOLUME = 100;

        /**
         * Returns the current version of this AlgART libraries.
         * Version format: "&lt;major&gt;.&lt;medium&gt;.&lt;minor&gt;[.&lt;patch&gt;]", for example, "1.3.0".
         *
         * @return AlgART version as a string.
         */
        public static String version() {
            return VERSION;
        }

        /**
         * Returns the current version as an array <code>v[4]</code> of 4 integer values:
         * v[0] is the major version, v[1] is the medium version number,
         * v[2] is the minor version number, v[3] is the patch number or zero if the patch is not specified.
         *
         * @return AlgART version as <code>int[]</code> array.
         */
        public static int[] parsedVersion() {
            return java.util.Arrays.copyOf(PARSED_VERSION, 4);
        }

        /**
         * The number of processor units that are allowed for simultaneous usage by AlgART libraries:
         * the minimum from <code>Runtime.getRuntime().availableProcessors()</code> value and
         * the {@link #MAX_AVAILABLE_PROCESSORS} constant.
         * The value of that constant is read from system property
         * "net.algart.arrays.maxAvailableProcessors" and equal to 16384 by default
         * (or 16 on 32-bit JVM). See comments to that constant for more details.
         *
         * <p>This method is used by AlgART libraries in all situations, where it is necessary to
         * know to actual number of processor units, in particular, in {@link #cpuCount()} method,
         * in {@link DefaultThreadPoolFactory} class while detecting the size of the global thread pool
         * and in {@link DefaultDataFileModel} class while calculation of the recommended banks number.
         * We also recommend you to use this method instead of direct
         * <code>Runtime.getRuntime().availableProcessors()</code> call.
         * It is very helpful on multiprocessor systems, containing many CPU kernels (several tens),
         * to limit CPU usage by your application.
         *
         * <p>Note: this method has another sense than {@link #cpuCount()}.
         * This method provides a global limitation for usage of CPU kernels in any situations,
         * while {@link #cpuCount()} usually controls the number of threads while multithreading optimization.
         *
         * @return the number of processor units among <code>Runtime.getRuntime().availableProcessors()</code>,
         * allowed for usage by AlgART libraries.
         * @see #MAX_AVAILABLE_PROCESSORS
         */
        public static int availableProcessors() {
            return InternalUtils.availableProcessors();
        }

        /**
         * Returns the recommended number of used system processors, specified by {@link #CPU_COUNT_PROPERTY_NAME}
         * system property (<code>{@value #CPU_COUNT_PROPERTY_NAME}</code>); the result 0 means
         * a recommendation to use the result of {@link #availableProcessors()} method.
         *
         * <p>More precisely, if this system property exists and contains non-negative integer value,
         * this method returns minimum from this value,
         * <code>Integer.getInteger({@link #CPU_COUNT_PROPERTY_NAME})</code>,
         * and the limit {@link #MAX_AVAILABLE_PROCESSORS}.
         * Zero value is also possible; in this case, it is supposed that
         * {@link #availableProcessors()} should be used instead.
         * If there is no such property, or if it contains a negative integer or not a number,
         * or if some exception occurred while calling <code>Integer.getInteger</code>,
         * this method returns the default value <code>0</code>
         * (that means, as supposed, {@link #availableProcessors()} processors).
         *
         * <p>This value is used by {@link DefaultThreadPoolFactory} class for creating thread pools,
         * used for parallel execution of algorithms processing AlgART arrays.
         * You can use this property for analogous goals.
         *
         * <p>Please note: unlike most other system properties, used by this package,
         * the <code>{@value #CPU_COUNT_PROPERTY_NAME}</code> property is loaded at the moment of calling this method.
         * So, the classes using this method, like {@link DefaultThreadPoolFactory}, always use the actual value
         * of this property, that can be customized in the application without restarting JVM.
         *
         * @return the supposed number of processors, specified by {@link #CPU_COUNT_PROPERTY_NAME} system property,
         * or <code>0</code> if there is no such property, or {@link #MAX_AVAILABLE_PROCESSORS}
         * if this property contains an integer greater than this limit. The value 0 means
         * a recommendation to use {@link #availableProcessors()}.
         * @see #cpuCount()
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks()
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks(Array)
         */
        public static int cpuCountProperty() {
            Integer prop = InternalUtils.getIntegerWrapperProperty(CPU_COUNT_PROPERTY_NAME, null);
            if (prop == null) {
                return 0;
            }
            if (prop < 0) {
                return 0;
            } else if (prop > MAX_AVAILABLE_PROCESSORS) {
                return MAX_AVAILABLE_PROCESSORS;
            } else {
                return prop;
            }
        }

        /**
         * Returns the recommended number of used system processors as the result
         * of {@link #cpuCountProperty()} method, if it is a positive number,
         * or the result of {@link #availableProcessors()}, if it is zero.
         * This value is typically used for multiprocessing optimization:
         * see {@link DefaultThreadPoolFactory}.
         *
         * @return the recommended number of used processors.
         * @see #cpuCountProperty()
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks()
         * @see DefaultThreadPoolFactory#recommendedNumberOfTasks(Array)
         */
        public static int cpuCount() {
            int result = Arrays.SystemSettings.cpuCountProperty();
            if (result == 0) {
                result = availableProcessors();
            }
            return result;
        }

        /**
         * Returns the global {@link MemoryModel memory model} that is recommended to use
         * in the application for creating large AlgART arrays.
         *
         * <p>The global memory model is determined while initializing {@link Arrays} class
         * from the system property "net.algart.arrays.globalMemoryModel".
         * This property must contain the full name of the Java class implementing
         * {@link MemoryModel} interface. The class must have a public constructor
         * without arguments or, as a variant, static <code>getInstance()</code> method
         * without arguments returning an instance of this memory model.
         * This constructor or method will be used for creating the global memory model instance.
         *
         * <p>If there is not such system property, the {@link SimpleMemoryModel}
         * will be used as the global model.
         *
         * <p>The following standard aliases are allowed for the class name
         * in "net.algart.arrays.globalMemoryModel" system property:<ul>
         * <li>"SIMPLE" means {@link SimpleMemoryModel},</li>
         * <li>"BUFFER" means {@link BufferMemoryModel},</li>
         * <li>"LARGE" means {@link LargeMemoryModel}.</li>
         * </ul>
         *
         * <p>Important: the global memory model must always
         * {@link MemoryModel#areAllPrimitiveElementTypesSupported() support all primitive types}!
         * (All 3 standard models, "simple", "buffer" and "large", do support all primitive types.)
         * If it is not true for a model specified in "net.algart.arrays.globalMemoryModel"
         * system property, or in a case of any problem
         * while reading the system property or while instantiation, the corresponded message will be logged
         * by the standard Java logger (<code>Logger.getLogger(Arrays.class.getName()</code>)
         * with <code>SEVERE</code> level,
         * and the {@link SimpleMemoryModel} will be used as the global model.
         *
         * <p>Note: the global memory model may not support non-primitive element types!
         * You may use {@link #globalMemoryModel(Class)} method to choose appropriate memory model.
         *
         * <p>If you are sure that your AlgART arrays will not be too large,
         * you may ignore this method and use the {@link SimpleMemoryModel}.
         * It is the quickest model for small arrays, but may be inefficient
         * for very large arrays (100-500 MB or more)
         * and does not allow work with arrays greater than 2 GB.
         *
         * @return the global memory model recommended for creating large AlgART arrays.
         * @see #globalMemoryModel(Class)
         */
        public static MemoryModel globalMemoryModel() {
            return GLOBAL_MEMORY_MODEL;
        }

        /**
         * Returns {@link #globalMemoryModel() global memory model}, if it
         * {@link MemoryModel#isElementTypeSupported(Class) supports} the passed type
         * of array elements, or {@link SimpleMemoryModel} instance otherwise.
         *
         * @param elementType the type of array elements.
         * @return the global memory model if it supports this type, {@link SimpleMemoryModel} otherwise.
         * @throws NullPointerException if the argument is {@code null}.
         * @see #globalMemoryModel()
         */
        public static MemoryModel globalMemoryModel(Class<?> elementType) {
            return GLOBAL_MEMORY_MODEL.isElementTypeSupported(elementType) ?
                    GLOBAL_MEMORY_MODEL :
                    SimpleMemoryModel.getInstance();
        }

        /**
         * Specifies the maximal amount of usual Java memory, in bytes, which may be freely used
         * by methods, processing AlgART arrays, for internal needs and for creating results.
         * In other words, if the size of the resulting {@link Array AlgART array} or {@link Matrix matrix},
         * or some temporary set of arrays or matrices,
         * is not greater than this limit, then the method <i>may</i> (though not <i>must</i>)
         * use {@link SimpleMemoryModel} for creating such AlgART arrays (matrices)
         * or may allocate usual Java arrays instead of AlgART arrays.
         * For allocating greater amount of memory, all methods should use, when possible,
         * some more advanced and controlled memory model, for example, {@link #globalMemoryModel()}.
         *
         * <p>This value is determined while initializing {@link Arrays} class
         * from the system property "net.algart.arrays.maxTempJavaMemory" by the call
         * <code>Long.getLong("net.algart.arrays.maxTempJavaMemory",33554432)</code> (32 MB).
         * If this property contains a negative integer, it is supposed to be zero,
         * that means that this limit will be ignored and this optimization ability should not be used.
         * If this property contains an integer greater than the limit 2<sup>56</sup>~7.2*10<sup>16</sup>,
         * this limit is used instead: it helps to guarantee that using this value will not lead to integer overflow.
         * If some exception occurs while calling <code>Long.getLong</code>,
         * it will contain the default <code>33554432</code> value.
         *
         * <p>Warning: if you specify too large value for this limit in "net.algart.arrays.maxTempJavaMemory"
         * system property, it can lead to unexpected <code>OutOfMemoryError</code> in some methods,
         * if the amount of available Java memory is not enough.
         *
         * @return the value of "net.algart.arrays.maxTempJavaMemory" system property, <code>33554432</code> by default.
         * @see #maxTempJavaMemoryForTiling()
         */
        public static long maxTempJavaMemory() {
            return InternalUtils.MAX_TEMP_JAVA_MEMORY;
        }

        /**
         * Analog of {@link #maxTempJavaMemory()} value for a special case: conversion AlgART matrices
         * into the {@link Matrix#tile(long...) tiled form} and inversely from the tiled form, i&#46;e&#46; for
         * copying a usual matrix to the matrix created by the {@link Matrix#tile(long...)} method and, inversely,
         * for copying a result of {@link Matrix#tile(long...)} method into a usual matrix.
         * If a method of this package, such as {@link Matrices#copy(ArrayContext, Matrix, Matrix)},
         * detects that one from two matrices is tiled, and it should be copied into/from another usual (untiled)
         * matrix, and that the tiled matrix is not created by a very quick memory model alike
         * {@link SimpleMemoryModel}, then the method allocates a buffer in the Java heap space
         * up to {@link #maxTempJavaMemoryForTiling()} bytes
         * (more precisely, up to {@link #availableTempJavaMemoryForTiling()} bytes)
         * and use it for reducing swapping while tiling/untiling.
         *
         * <p>Usually this limit is chosen greater than {@link #maxTempJavaMemory()}, because conversion
         * matrices into/from the tiled form strongly needs RAM for quick processing and can extremely slow down
         * if there is not enough memory to download the minimal converted block into RAM.
         *
         * <p>This value is determined while initializing {@link Arrays} class
         * from the system property "net.algart.arrays.maxTempJavaMemoryForTiling" by the call
         * <code>Long.getLong("net.algart.arrays.maxTempJavaMemoryForTiling",
         * Math.max(134217728, {@link #maxTempJavaMemory()}))</code> (134217728=128&nbsp;MB).
         * If this property contains a negative integer, it is supposed to be zero,
         * that means that this limit will be ignored and this optimization ability should not be used.
         * If this property contains an integer greater than the limit 2<sup>56</sup>~7.2*10<sup>16</sup>,
         * this limit is used instead: it helps to guarantee that using this value will not lead to integer overflow.
         * If some exception occurs while calling <code>Long.getLong</code>,
         * it will contain the default value: maximum from <code>134217728</code> and {@link #maxTempJavaMemory()}.
         *
         * <p>Note: if this limit is greater than the amount of available Java memory,
         * then tiling/untiling methods use only available amount (depending on "<code>-Xmx</code>" JVM argument)
         * and don't try to allocate more than ~80% of available free Java heap space:
         * see {@link #availableTempJavaMemoryForTiling()}.
         * So, large values of this limit should probably not lead to unexpected <code>OutOfMemoryError</code>.
         *
         * @return the value of "net.algart.arrays.maxTempJavaMemoryForTiling" system property,
         * <code>max(134217728,{@link #maxTempJavaMemory()})</code> by default.
         */
        public static long maxTempJavaMemoryForTiling() {
            return InternalUtils.MAX_TEMP_JAVA_MEMORY_FOR_TILING;
        }

        /**
         * Returns the minimum from {@link #maxTempJavaMemoryForTiling()} and 80% of the amount of memory, that can be
         * probably allocated in Java heap without the risk of <code>OutOfMemoryError</code>.
         * This value is used for estimating the temporary work memory by tiling/untiling methods of this package:
         * see {@link #maxTempJavaMemoryForTiling()}.
         *
         * @return minimum from {@link #maxTempJavaMemoryForTiling()} and 80% of the amount of memory, that can be
         * allocated in Java heap at this moment.
         */
        public static long availableTempJavaMemoryForTiling() {
            Runtime rt = Runtime.getRuntime();
            long available = rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
            return Math.min(maxTempJavaMemoryForTiling(), (long) (available * MAX_FREE_HEAP_SPACE_USAGE_FOR_TILING));
        }

        /**
         * Specifies the maximal size of memory block, in bytes, that should be processed in several threads
         * for optimization on multiprocessor or multicore computers.
         *
         * <p>Many algorithms, in particular, based on
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} method and
         * {@link Arrays.ParallelExecutor} class,
         * use multithreading processing for more optimal usage of available processors.
         * But parallel processing works fine only for limited amount of memory.
         * Multithreading processing very large array, for example, larger than available RAM,
         * leads to a lot of simultaneous disk accesses and usually extremely slows down calculations.
         * For AlgART arrays, created by {@link LargeMemoryModel}, the good practice is
         * multithreading processing a sequential range of an array, not greater than
         * {@link DataFileModel#recommendedBankSize one bank}.
         *
         * <p>This constant is used for calculating length of sequential subarrays (ranges),
         * processed in several threads by {@link Arrays.ParallelExecutor} class and methods based on that class.
         * See {@link Arrays.ParallelExecutor#recommendedNumberOfRanges(Array, boolean)} method for more details.
         * You also may use this constant in your code for similar needs.
         *
         * <p>This value is determined while initializing {@link Arrays} class
         * from the system property "net.algart.arrays.maxMultithreadingMemory" by the call
         * <code>Long.getLong("net.algart.arrays.maxMultithreadingMemory",1048576)</code> (1 MB).
         * If this property contains negative integer or a value less than 256, it is supposed to be 256 (bytes):
         * the minimal allowed amount of memory for multithreading.
         * If this property contains an integer greater than the limit 2<sup>56</sup>~7.2*10<sup>16</sup>,
         * this limit is used instead: it helps to guarantee that using this value will not lead to integer overflow.
         * If some exception occurs while calling <code>Long.getLong</code>,
         * it will contain the default <code>1048576</code> value.
         *
         * @return the maximal size of memory block, in bytes, that should be processed in several threads
         * for optimization on multiprocessor or multicore computers,
         * <code>1048576</code> by default.
         */
        public static long maxMultithreadingMemory() {
            return InternalUtils.MAX_MULTITHREADING_MEMORY;
        }

        /**
         * Returns the global {@link Arrays.SystemSettings.DiskSynchronizer disk synchronizer}
         * that is recommended to use in the application for any disk I/O operations.
         *
         * <p>The global disk synchronizer is determined while initializing {@link Arrays} class
         * from the system property "net.algart.arrays.globalDiskSynchronizer".
         * This property must contain the full name of the Java class implementing
         * {@link Arrays.SystemSettings.DiskSynchronizer} interface.
         * The class must have a public constructor
         * without arguments or, as a variant, static <code>getInstance()</code> method
         * without arguments returning an instance of this disk synchronizer.
         * This constructor or method will be used for creating the global disk synchronizer instance.
         *
         * <p>If there is not such system property, the default implementation of
         * {@link Arrays.SystemSettings.DiskSynchronizer} interface will be used,
         * that performs simple global synchronization of all operations.
         * See comments to this interface for more details about the default implementation.
         *
         * <p>In a case of any problem while reading the system property or while instantiation
         * of the required class, the corresponded message will be logged
         * by the standard Java logger (<code>Logger.getLogger(Arrays.class.getName()</code>)
         * with <code>SEVERE</code> level, and the default implementation of
         * {@link Arrays.SystemSettings.DiskSynchronizer} interface will be used.
         *
         * @return the current synchronizer of all disk operations.
         */
        public static DiskSynchronizer globalDiskSynchronizer() {
            return GLOBAL_DISK_SYNCHRONIZER;
        }

        /**
         * Returns <code>true</code> on 32-bit Java machines, <code>false</code> on 64-bit ones
         * (or probably better JVM in the future: for example, 128-bit).
         * It is used by {@link DefaultDataFileModel#recommendedBankSize(boolean)}
         * and {@link DefaultDataFileModel#recommendedSingleMappingLimit()} methods
         * and while determining the value of {@link #MAX_AVAILABLE_PROCESSORS} property.
         *
         * <p>Warning: this method does not provide 100% guaranteed result, because the current Java specification
         * doesn't publish robust methods for distinguishing 32-bit and 64-bit Java.
         * So, you cannot be completely sure that the system is 64-bit (or better)
         * if this method returns <code>false</code>.
         *
         * <p>This method checks <code>System.getProperty("sun.arch.data.model")</code>.
         * If this property exists, can be read without exceptions and contains "32" or "64" substring,
         * this method returns <code>true</code> if it contains "32" and <code>false</code> if it contains "64".
         * Otherwise (for example, in Java applets, which have no permissions to read this property)
         * this method makes a decision on the base of <code>System.getProperty("os.arch")</code>.
         * If it cannot make a decision (for example, due to some exception in the last <code>getProperty</code>
         * call), it returns <code>false</code> by default.
         * All this analyse is performed only once while initializing {@link Arrays} class.
         *
         * @return <code>true</code> on 32-bit Java machines.
         */
        public static boolean isJava32() {
            return InternalUtils.JAVA_32;
        }

        /**
         * Returns <code>true</code> if Java is called with "-ea" JVM flag (assertions are enabled).
         * However, if there is the system property "net.algart.arrays.profiling", that can contain
         * "<code>true</code>" or "<code>false</code>" value, this function returns the value of that property.
         *
         * <p>If this function returns <code>true</code>, the algorithms, processing AlgART arrays,
         * may use more detailed logging.
         * In particular, {@link Arrays#copy(ArrayContext, UpdatableArray, Array)}
         * and {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int)} methods
         * log the time of copying in this case.
         *
         * @return the value of "net.algart.arrays.profiling" system property or the "-ea" flag by default.
         */
        public static boolean profilingMode() {
            return InternalUtils.PROFILING;
        }

        /**
         * An analog of <code>Integer.parseInt</code> that "understands" suffixes K, M, G, that mean "Kilo" (1024),
         * "Mega" (1048576) and "Giga" (1073741824). The suffixes can be written both in upper and lower case.
         * For example, for "3k" string this method returns 3*1024=3072, for "1M" it returns 1048576,
         * for "1783" it returns 1783.
         *
         * <p>More precisely, if the specified string ends with "K", "k", "M", "m", "G" or "g", then
         * this method calls <code>Integer.parseInt(s.substring(0,s.length()-1))</code>
         * and multiplies the result by 1024 (2<sup>10</sup>), 1048576 (2<sup>20</sup>) or
         * 1073741824 (2<sup>30</sup>); in a case of overflow while multiplying,
         * <code>NumberFormatException</code> is thrown.
         * If the specified string does not end with "K", "k", "M", "m", "G" or "g", then
         * this method is equivalent to <code>Integer.parseInt(s)</code>.
         *
         * <p>This method is convenient while analysing some system properties or another settings,
         * usually describing amounts of memory.
         *
         * @param s a string containing the <code>int</code> representation to be parsed, maybe with K, M, G suffixes.
         * @return the integer value represented by the argument in decimal.
         * @throws NumberFormatException if the string does not contain a parsable integer.
         */
        public static int parseIntWithMetricalSuffixes(String s) {
            return InternalUtils.parseIntWithMetricalSuffixes(s);
        }

        /**
         * An analog of <code>Long.parseLong</code> that "understands" suffixes K, M, G, T, that mean "Kilo" (1024),
         * "Mega" (1048576), "Giga" (1073741824) and "Tera" (1099511627776). The suffixes can be written
         * both in upper and lower case. For example, for "3k" string this method returns 3*1024=3072,
         * for "1M" it returns 1048576, for "1783" it returns 1783.
         *
         * <p>More precisely, if the specified string ends with "K", "k", "M", "m", "G", "g", "T" or "t", then
         * this method calls <code>Long.parseLong(s.substring(0,s.length()-1))</code>
         * and multiplies the result by 1024 (2<sup>10</sup>), 1048576 (2<sup>20</sup>),
         * 1073741824 (2<sup>30</sup>) or 1099511627776 (2<sup>40</sup>); in a case of overflow while multiplying,
         * <code>NumberFormatException</code> is thrown.
         * If the specified string does not end with "K", "k", "M", "m", "G", "g", "T" or "t", then
         * this method is equivalent to <code>Long.parseLong(s)</code>.
         *
         * <p>This method is convenient while analysing some system properties or another settings,
         * usually describing amounts of memory.
         *
         * @param s a string containing the <code>long</code> representation to be parsed, maybe with K, M, G, T
         *          suffixes.
         * @return the long integer value represented by the argument in decimal.
         * @throws NumberFormatException if the string does not contain a parsable integer.
         */
        public static long parseLongWithMetricalSuffixes(String s) {
            return InternalUtils.parseLongWithMetricalSuffixes(s);
        }

        /**
         * An analog of <code>System.getProperty</code>, but returns <code>defaultValue</code>
         * even in a case of exception.
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     return System.getProperty(propertyName, defaultValue);
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code>,
         * this method just returns <code>defaultValue</code>.
         * In particular, this method returns <code>defaultValue</code>
         * if <code>propertyName</code> is {@code null}
         * or an empty string (<code>System.getProperty</code> throws exceptions in these cases).
         * There is a guarantee that this method never throws exceptions.
         *
         * @param propertyName the name of the property.
         * @param defaultValue default value; {@code null} is an allowed value.
         * @return the value of the string property or the default value in a case of any problems.
         */
        public static String getStringProperty(String propertyName, String defaultValue) {
            return InternalUtils.getStringProperty(propertyName, defaultValue);
        }

        /**
         * An analog of <code>System.getenv</code>, but returns <code>defaultValue</code>
         * in a case, when <code>System.getenv</code> returns {@code null} or throws any exception.
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     String s = System.getenv(envVarName);
         *     return s != null ? s : defaultValue;
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code>,
         * this method just returns <code>defaultValue</code>.
         * In particular, this method returns <code>defaultValue</code>
         * if <code>propertyName</code> is {@code null}
         * (<code>System.getenv</code> throws exceptions in these cases).
         * There is a guarantee that this method never throws exceptions.
         *
         * @param envVarName   the name of the environment variable.
         * @param defaultValue default value; {@code null} is an allowed value.
         * @return the value of the environment variable or the default value in a case of any problems.
         */
        public static String getStringEnv(String envVarName, String defaultValue) {
            try {
                String s = System.getenv(envVarName);
                return s != null ? s : defaultValue;
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        /**
         * An analog of <code>Integer.getInteger</code>, but "understands" suffixes K, M, G, that mean "Kilo" (1024),
         * "Mega" (1048576) and "Giga" (1073741824) and returns <code>defaultValue</code>
         * in a case of any exception (including security exception).
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     return {@link #parseIntWithMetricalSuffixes parseIntWithMetricalSuffixes}(System.getProperty(
         *         propertyName, String.valueOf(defaultValue)));
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code>
         * and <code>NumberFormatException</code>,
         * this method just returns <code>defaultValue</code>.
         * There is a guarantee that this method never throws exceptions.
         *
         * @param propertyName the name of the property.
         * @param defaultValue default value.
         * @return the value of the integer property or the default value in a case of any problems.
         */
        public static int getIntProperty(String propertyName, int defaultValue) {
            return InternalUtils.getIntProperty(propertyName, defaultValue);
        }

        /**
         * An analog of {@link #getIntProperty(String, int)}, which reads the number from
         * the environment variable instead of the system property.
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     String s = System.getenv(envVarName);
         *     return s == null ? defaultValue : {@link #parseIntWithMetricalSuffixes
         *     parseIntWithMetricalSuffixes}(s);
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code>
         * and <code>NumberFormatException</code>,
         * this method just returns <code>defaultValue</code>.
         * There is a guarantee that this method never throws exceptions.
         *
         * @param envVarName   the name of the environment variable.
         * @param defaultValue default value.
         * @return the value of the integer environment variable or the default value in a case of any problems.
         */
        public static int getIntEnv(String envVarName, int defaultValue) {
            try {
                String s = System.getenv(envVarName);
                return s == null ? defaultValue : InternalUtils.parseIntWithMetricalSuffixes(s);
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        /**
         * An analog of <code>Long.getLong</code>, but "understands" suffixes K, M, G, T, that mean "Kilo" (1024),
         * "Mega" (1048576), "Giga" (1073741824) and "Tera" (1099511627776) and returns <code>defaultValue</code>
         * in a case of any exception (including security exception).
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     return {@link #parseLongWithMetricalSuffixes parseLongWithMetricalSuffixes}(System.getProperty(
         *         propertyName, String.valueOf(defaultValue)));
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code> and
         * <code>NumberFormatException</code>,
         * this method just returns <code>defaultValue</code>.
         * There is a guarantee that this method never throws exceptions.
         *
         * @param propertyName the name of the property.
         * @param defaultValue default value.
         * @return the value of the long-integer property or the default value in a case of any problems.
         */
        public static long getLongProperty(String propertyName, long defaultValue) {
            return InternalUtils.getLongProperty(propertyName, defaultValue);
        }

        /**
         * An analog of {@link #getLongProperty(String, long)}, which reads the number from
         * the environment variable instead of the system property.
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     String s = System.getenv(envVarName);
         *     return s == null ? defaultValue : {@link #parseLongWithMetricalSuffixes
         *     parseLongWithMetricalSuffixes}(s);
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code> and
         * <code>NumberFormatException</code>,
         * this method just returns <code>defaultValue</code>.
         * There is a guarantee that this method never throws exceptions.
         *
         * @param envVarName   the name of the environment variable.
         * @param defaultValue default value.
         * @return the value of the long-integer environment variable or the default value in a case of any problems.
         */
        public static long getLongEnv(String envVarName, long defaultValue) {
            try {
                String s = System.getenv(envVarName);
                return s == null ? defaultValue : InternalUtils.parseLongWithMetricalSuffixes(s);
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        /**
         * An analog of <code>Boolean.getBoolean</code>, but returns <code>defaultValue</code>
         * if there is no required property, or if it doesn't equal "<code>true</code>" or "<code>false</code>",
         * or in a case of any exception. (The test of this string is case-insensitive.)
         *
         * <p>Unlike <code>Boolean.getBoolean</code>,
         * this method catches all exceptions, including <code>SecurityException</code>, and returns
         * <code>defaultValue</code> in a case of an exception.
         * There is a guarantee that this method never throws exceptions.
         *
         * @param propertyName the name of property.
         * @param defaultValue default value.
         * @return the value of the boolean property or the default value in a case of any problems.
         */
        public static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
            return InternalUtils.getBooleanProperty(propertyName, defaultValue);
        }

        /**
         * An analog of {@link #getBooleanProperty(String, boolean)}, which tests
         * the environment variable instead of the system property.
         *
         * <p>More precisely, this method does the following:
         * <pre>
         * try {
         *     String s = System.getenv(envVarName);
         *     return s == null ? defaultValue
         *         : s.equalsIgnoreCase("true") ? true
         *         : s.equalsIgnoreCase("false") ? false
         *         : defaultValue;
         * } catch (Exception ex) {
         *     return defaultValue;
         * }
         * </pre>
         * So, in a case of any exception, including <code>SecurityException</code>
         * and <code>NumberFormatException</code>,
         * this method just returns <code>defaultValue</code>.
         * There is a guarantee that this method never throws exceptions.
         *
         * @param envVarName   the name of the environment variable.
         * @param defaultValue default value.
         * @return the value of the environment variable or the default value in a case of any problems.
         */
        public static boolean getBooleanEnv(String envVarName, boolean defaultValue) {
            try {
                String s = System.getenv(envVarName);
                //noinspection SimplifiableConditionalExpression
                return s == null ? defaultValue
                        : s.equalsIgnoreCase("true") ? true
                        : s.equalsIgnoreCase("false") ? false
                        : defaultValue;
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        /**
         * <p>Global synchronizer, used for all disk operations, performed by this package.
         * More precisely, the implementation of interface, returned by
         * {@link Arrays.SystemSettings#globalDiskSynchronizer()} method, is used for all operations with
         * disk files, performed by {@link DefaultDataFileModel} and {@link StandardIODataFileModel}.</p>
         *
         * <p>Access to AlgART arrays, created via {@link LargeMemoryModel}, can require loading or saving
         * some portion of data at an external device, according to the used {@link DataFileModel}.
         * However, access to AlgART arrays can be performed from several threads:
         * for example, each window of your application can use own thread for calculations.
         * If the device for storing data is a usual disk, then simultaneous multithreading access to it
         * can lead to extreme slowing down all calculations: unlike RAM, most of disk devices
         * do not provide quick parallel I/O operation with different files or portions of a file.</p>
         *
         * <p>To solve this problem, this library performs any action, connected with disk I/O operations &mdash;
         * reading or writing file regions, mapping, preloading or flushing mapped buffers
         * (<code>MappedByteBuffer.load/force</code> methods) &mdash; not directly, but via calling
         * {@link #doSynchronously(String, Callable)} method of the global implementation
         * of this interface, returned by {@link Arrays.SystemSettings#globalDiskSynchronizer()} method.
         * For example, {@link DefaultDataFileModel} class performs disk mapping in the following manner:</p>
         *
         * <pre>
         * MappedByteBuffer buffer = {@link Arrays.SystemSettings#globalDiskSynchronizer()
         * Arrays.SystemSettings.globalDiskSynchronizer()}.{@link #doSynchronously
         * doSynchronously}(
         * &#32;   fileName,
         * &#32;   new Callable&lt;MappedByteBuffer&gt;() {
         * &#32;       public MappedByteBuffer call() throws IOException {
         * &#32;           return fileChannel.map(mode, position, size);
         * &#32;       }
         * &#32;   });
         * </pre>
         *
         * <p>You can use (and usually should use) the analogous "wrapping" of your own disk operations.</p>
         *
         * <p>By default, the implementation of
         * {@link #doSynchronously(String fileName, Callable action)} method
         * performs the desired action with global synchronization, alike in the following code:</p>
         *
         * <pre>
         * synchronized (globalLock) { // some private static field
         * &#32;   return action.call();
         * }
         * </pre>
         *
         * <p>It means that all disk operations, which are performed via this disk synchronizer,
         * will be performed sequentially always. It is a good solution for most of disk devices.</p>
         *
         * <p>However, some disk controllers provide quick parallel disk operation, usually with different disks.
         * If your application should be optimized for such systems, you may provide your own custom
         * implementation of this interface and specify it via "net.algart.arrays.globalDiskSynchronizer"
         * system property (see {@link Arrays.SystemSettings#globalDiskSynchronizer() globalDiskSynchronizer()}).
         * Your implementation, for example, can analyse the file name, detect the physical device,
         * corresponding to this file, and allow simultaneous operation at different devices.</p>
         *
         * <p>It is obvious that any implementation of this interface is <b>thread-safe</b>.</p>
         */
        public interface DiskSynchronizer {
            /**
             * Performs the specified action synchronously.
             * Depending on implementation, it is probably performed with some form of global synchronization.
             *
             * <p>The specified action is supposed to be I/O operation with a file,
             * the absolute path to which is passed via <code>fileName</code> argument.
             * This name can help to control synchronization depending on the physical disk
             * where the file is located: for example, this method can allow simultaneous reading 2 files,
             * if they are placed on disks C: and D: (controlled by different disk controllers),
             * but can disable simultaneous reading files from the same disk.
             *
             * <p>The default implementation does not use this argument.
             *
             * @param <T>      the result type for callable task.
             * @param fileName the symbolic name of file, probably its canonical path.
             * @param action   some I/O operations that should be synchronously performed with this file.
             * @return result of the operation.
             * @throws Exception if unable to compute a result.
             */
            <T> T doSynchronously(String fileName, Callable<T> action) throws Exception;
        }

        /**
         * Default implementation of {@link Arrays.SystemSettings.DiskSynchronizer}.
         */
        static class DefaultDiskSynchronizerLocking implements DiskSynchronizer {
            private static final ReentrantLock globalLock = new ReentrantLock();

            public <T> T doSynchronously(String fileName, Callable<T> action) throws Exception {
                globalLock.lock();
                try {
                    return action.call();
                } finally {
                    globalLock.unlock();
                }
            }
        }

        /**
         * Alternate implementation of {@link Arrays.SystemSettings.DiskSynchronizer}, based on
         * the pool with 1 thread. Not used in current version.
         */
        static class DefaultDiskSynchronizerPooling implements DiskSynchronizer {
            public <T> T doSynchronously(String fileName, Callable<T> action) throws Exception {
                Future<T> result = DiskThreadPoolHolder.globalDiskThreadPool.submit(action);
                try {
                    return result.get(); // waiting for finishing
                } catch (ExecutionException e) {
                    throwUncheckedException(e.getCause());
                    throw new AssertionError("Cannot occur");
                } catch (InterruptedException e) {
                    throw new AssertionError("Unexpected InterruptedException in " + this + ": " + e);
                }
            }
        }

        private static class DiskThreadPoolHolder {
            private static final ExecutorService globalDiskThreadPool = Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "AlgART-disk-thread");
                        t.setDaemon(true);
                        return t;
                    });
        }
    }

    /**
     * Contains {@link SimpleMemoryModel#getInstance()}.
     * It is just a more compact alternative for the expression "{@link SimpleMemoryModel#getInstance()}".
     */
    public static final SimpleMemoryModel SMM = SimpleMemoryModel.INSTANCE;

    /**
     * Contains {@link BufferMemoryModel#getInstance()}.
     * It is just a more compact alternative for the expression "{@link BufferMemoryModel#getInstance()}".
     */
    public static final BufferMemoryModel BMM = BufferMemoryModel.INSTANCE;

    /**
     * Unmodifiable list of all primitive types.
     * All they, as well as any object types, can be elements of AlgART arrays.
     */
    public static List<Class<?>> PRIMITIVE_TYPES =
            List.of(
                    boolean.class,
                    char.class,
                    byte.class,
                    short.class,
                    int.class,
                    long.class,
                    float.class,
                    double.class);


    /**
     * The number of bits per every element of bit array: 1.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link BitArray}, {@link UpdatableBitArray}, {@link MutableBitArray}.
     */
    public static final int BITS_PER_BIT = 1;

    /**
     * The number of bits per every element of character array: 16.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link CharArray}, {@link UpdatableCharArray}, {@link MutableCharArray}.
     */
    public static final int BITS_PER_CHAR = 16;

    /**
     * The number of bits per every element of <code>byte</code> array: 8.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link ByteArray}, {@link UpdatableByteArray}, {@link MutableByteArray}.
     */
    public static final int BITS_PER_BYTE = 8;

    /**
     * The number of bits per every element of <code>short</code> array: 16.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link ShortArray}, {@link UpdatableShortArray}, {@link MutableShortArray}.
     */
    public static final int BITS_PER_SHORT = 16;

    /**
     * The number of bits per every element of <code>int</code> array: 32.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link IntArray}, {@link UpdatableIntArray}, {@link MutableIntArray}.
     */
    public static final int BITS_PER_INT = 32;

    /**
     * The number of bits per every element of <code>long</code> array: 64.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link LongArray}, {@link UpdatableLongArray}, {@link MutableLongArray}.
     */
    public static final int BITS_PER_LONG = 64;

    /**
     * The number of bits per every element of <code>float</code> array: 32.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link FloatArray}, {@link UpdatableFloatArray}, {@link MutableFloatArray}.
     */
    public static final int BITS_PER_FLOAT = 32;

    /**
     * The number of bits per every element of <code>double</code> array: 64.
     * This value is returned by {@link PArray#bitsPerElement()} method
     * for instances of {@link DoubleArray}, {@link UpdatableDoubleArray}, {@link MutableDoubleArray}.
     */
    public static final int BITS_PER_DOUBLE = 64;

    static final int BITS_PER_OBJECT = -1;

    /**
     * Returns the type of elements corresponding to the passed class of primitive arrays.
     * The passed class must be one of basic interfaces
     * <code>{@link BitArray}.class</code>, <code>{@link CharArray}.class</code>,
     * <code>{@link ByteArray}.class</code>, <code>{@link ShortArray}.class</code>,
     * <code>{@link IntArray}.class</code>, <code>{@link LongArray}.class</code>,
     * <code>{@link FloatArray}.class</code>, <code>{@link DoubleArray}.class</code>,
     * or their subinterfaces (like <code>{@link UpdatableByteArray}.class</code>
     * or <code>{@link MutableIntArray}.class</code>), or a class implementing one of these interfaces.
     *
     * <p>More precisely, this method returns:
     * <ul>
     * <li><code>boolean.class</code> for <code>{@link BitArray}.class</code>,</li>
     * <li><code>char.class</code> for <code>{@link CharArray}.class</code>,</li>
     * <li><code>byte.class</code> for <code>{@link ByteArray}.class</code>,</li>
     * <li><code>short.class</code> for <code>{@link ShortArray}.class</code>,</li>
     * <li><code>int.class</code> for <code>{@link IntArray}).class</code>,</li>
     * <li><code>long.class</code> for <code>{@link LongArray}.class</code>,</li>
     * <li><code>float.class</code> for <code>{@link FloatArray}.class</code>,</li>
     * <li><code>double.class</code> for <code>{@link DoubleArray}.class</code>.</li>
     * </ul>
     *
     * @param arrayType the type of some primitive array.
     * @return the corresponding element type.
     * @throws NullPointerException     if the passed argument is {@code null}.
     * @throws IllegalArgumentException if the passed argument is not a class of primitive array.
     * @see Array#elementType()
     */
    public static Class<?> elementType(Class<? extends PArray> arrayType) {
        Objects.requireNonNull(arrayType, "Null arrayType argument");
        if (BitArray.class.isAssignableFrom(arrayType)) {
            return boolean.class;
        } else if (CharArray.class.isAssignableFrom(arrayType)) {
            return char.class;
        } else if (ByteArray.class.isAssignableFrom(arrayType)) {
            return byte.class;
        } else if (ShortArray.class.isAssignableFrom(arrayType)) {
            return short.class;
        } else if (IntArray.class.isAssignableFrom(arrayType)) {
            return int.class;
        } else if (LongArray.class.isAssignableFrom(arrayType)) {
            return long.class;
        } else if (FloatArray.class.isAssignableFrom(arrayType)) {
            return float.class;
        } else if (DoubleArray.class.isAssignableFrom(arrayType)) {
            return double.class;
        } else {
            throw new IllegalArgumentException("Only primitive array types " +
                    "BitArray, CharArray, ByteArray, ShortArray, IntArray, LongArray, " +
                    "FloatArray, DoubleArray and their inheritors allowed here " +
                    "(passed type: " + arrayType + ")");
        }
    }

    /**
     * Returns the type of immutable, unresizable or resizable arrays, which is a subtype of
     * (or same type as) <code>arraySupertype</code> and corresponds to the passed element type.
     *
     * <p>Namely, if <code>arraySupertype</code> is not {@link UpdatableArray} or its inheritor, returns:
     * <ul>
     * <li><code>{@link BitArray}.class</code> for <code>boolean.class</code>,</li>
     * <li><code>{@link CharArray}.class</code> for <code>char.class</code>,</li>
     * <li><code>{@link ByteArray}.class</code> for <code>byte.class</code>,</li>
     * <li><code>{@link ShortArray}.class</code> for <code>short.class</code>,</li>
     * <li><code>{@link IntArray}.class</code> for <code>int.class</code>,</li>
     * <li><code>{@link LongArray}.class</code> for <code>long.class</code>,</li>
     * <li><code>{@link FloatArray}.class</code> for <code>float.class</code>,</li>
     * <li><code>{@link DoubleArray}.class</code> for <code>double.class</code>,</li>
     * <li><code>{@link ObjectArray}.class</code> for all other classes.</li>
     * </ul>
     *
     * <p>If <code>arraySupertype</code> is {@link UpdatableArray} or its inheritor, returns:
     * <ul>
     * <li><code>{@link UpdatableBitArray}.class</code> for <code>boolean.class</code>,</li>
     * <li><code>{@link UpdatableCharArray}.class</code> for <code>char.class</code>,</li>
     * <li><code>{@link UpdatableByteArray}.class</code> for <code>byte.class</code>,</li>
     * <li><code>{@link UpdatableShortArray}.class</code> for <code>short.class</code>,</li>
     * <li><code>{@link UpdatableIntArray}.class</code> for <code>int.class</code>,</li>
     * <li><code>{@link UpdatableLongArray}.class</code> for <code>long.class</code>,</li>
     * <li><code>{@link UpdatableFloatArray}.class</code> for <code>float.class</code>,</li>
     * <li><code>{@link UpdatableDoubleArray}.class</code> for <code>double.class</code>,</li>
     * <li><code>{@link UpdatableObjectArray}.class</code> for all other classes.</li>
     * </ul>
     *
     * <p>If <code>arraySupertype</code> is {@link MutableArray} or its inheritor, returns:
     * <ul>
     * <li><code>{@link MutableBitArray}.class</code> for <code>boolean.class</code>,</li>
     * <li><code>{@link MutableCharArray}.class</code> for <code>char.class</code>,</li>
     * <li><code>{@link MutableByteArray}.class</code> for <code>byte.class</code>,</li>
     * <li><code>{@link MutableShortArray}.class</code> for <code>short.class</code>,</li>
     * <li><code>{@link MutableIntArray}.class</code> for <code>int.class</code>,</li>
     * <li><code>{@link MutableLongArray}.class</code> for <code>long.class</code>,</li>
     * <li><code>{@link MutableFloatArray}.class</code> for <code>float.class</code>,</li>
     * <li><code>{@link MutableDoubleArray}.class</code> for <code>double.class</code>,</li>
     * <li><code>{@link MutableObjectArray}.class</code> for all other classes.</li>
     * </ul>
     *
     * @param <T>            the generic type of AlgART array.
     * @param arraySupertype required array supertype.
     * @param elementType    required element type.
     * @return the corresponding array type.
     * @throws NullPointerException if one of the passed arguments is {@code null}.
     * @throws ClassCastException   if <code>arraySupertype</code> does not allow storing the required element type
     *                              (for example, it is {@link PArray}, but <code>elementType</code> is not
     *                              a primitive type).
     */
    public static <T extends Array> Class<T> type(Class<T> arraySupertype, Class<?> elementType) {
        Objects.requireNonNull(arraySupertype, "Null arraySupertype argument");
        Objects.requireNonNull(elementType, "Null elementType argument");
        final boolean updatable = UpdatableArray.class.isAssignableFrom(arraySupertype);
        final boolean mutable = MutableArray.class.isAssignableFrom(arraySupertype);
        Class<?> result;
        if (elementType == boolean.class) {
            result = mutable ? MutableBitArray.class : updatable ? UpdatableBitArray.class : BitArray.class;
        } else if (elementType == char.class) {
            result = mutable ? MutableCharArray.class : updatable ? UpdatableCharArray.class : CharArray.class;
        } else if (elementType == byte.class) {
            result = mutable ? MutableByteArray.class : updatable ? UpdatableByteArray.class : ByteArray.class;
        } else if (elementType == short.class) {
            result = mutable ? MutableShortArray.class : updatable ? UpdatableShortArray.class : ShortArray.class;
        } else if (elementType == int.class) {
            result = mutable ? MutableIntArray.class : updatable ? UpdatableIntArray.class : IntArray.class;
        } else if (elementType == long.class) {
            result = mutable ? MutableLongArray.class : updatable ? UpdatableLongArray.class : LongArray.class;
        } else if (elementType == float.class) {
            result = mutable ? MutableFloatArray.class : updatable ? UpdatableFloatArray.class : FloatArray.class;
        } else if (elementType == double.class) {
            result = mutable ? MutableDoubleArray.class : updatable ? UpdatableDoubleArray.class : DoubleArray.class;
        } else {
            result = mutable ? MutableObjectArray.class : updatable ? UpdatableObjectArray.class : ObjectArray.class;
        }
        if (!arraySupertype.isAssignableFrom(result)) {
            throw new ClassCastException("The passed array supertype " + arraySupertype.getName()
                    + " cannot contain required " + elementType.getCanonicalName() + " elements");
        }
        return InternalUtils.cast(result);
    }

    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link BitArray}.class</code>,
     * <code>{@link UpdatableBitArray}.class</code> or <code>{@link MutableBitArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for bit arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART bit arrays.
     */
    public static boolean isBitType(Class<? extends Array> arrayType) {
        return arrayType == BitArray.class ||
                arrayType == UpdatableBitArray.class ||
                arrayType == MutableBitArray.class;
    }

    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link CharArray}.class</code>,
     * <code>{@link UpdatableCharArray}.class</code> or <code>{@link MutableCharArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for character arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART character arrays.
     */
    public static boolean isCharType(Class<? extends Array> arrayType) {
        return arrayType == CharArray.class ||
                arrayType == UpdatableCharArray.class ||
                arrayType == MutableCharArray.class;
    }

    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link ByteArray}.class</code>,
     * <code>{@link UpdatableByteArray}.class</code> or <code>{@link MutableByteArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for <code>byte</code> arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART <code>byte</code> arrays.
     */
    public static boolean isByteType(Class<? extends Array> arrayType) {
        return arrayType == ByteArray.class ||
                arrayType == UpdatableByteArray.class ||
                arrayType == MutableByteArray.class;
    }

    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link ShortArray}.class</code>,
     * <code>{@link UpdatableShortArray}.class</code> or <code>{@link MutableShortArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for <code>short</code> arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART <code>short</code> arrays.
     */
    public static boolean isShortType(Class<? extends Array> arrayType) {
        return arrayType == ShortArray.class ||
                arrayType == UpdatableShortArray.class ||
                arrayType == MutableShortArray.class;
    }

    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link IntArray}.class</code>,
     * <code>{@link UpdatableIntArray}.class</code> or <code>{@link MutableIntArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for <code>int</code> arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART <code>int</code> arrays.
     */
    public static boolean isIntType(Class<? extends Array> arrayType) {
        return arrayType == IntArray.class ||
                arrayType == UpdatableIntArray.class ||
                arrayType == MutableIntArray.class;
    }

    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link LongArray}.class</code>,
     * <code>{@link UpdatableLongArray}.class</code> or <code>{@link MutableLongArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for <code>long</code> arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART <code>long</code> arrays.
     */
    public static boolean isLongType(Class<? extends Array> arrayType) {
        return arrayType == LongArray.class ||
                arrayType == UpdatableLongArray.class ||
                arrayType == MutableLongArray.class;
    }


    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link FloatArray}.class</code>,
     * <code>{@link UpdatableFloatArray}.class</code> or <code>{@link MutableFloatArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for <code>float</code> arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART <code>float</code> arrays.
     */
    public static boolean isFloatType(Class<? extends Array> arrayType) {
        return arrayType == FloatArray.class ||
                arrayType == UpdatableFloatArray.class ||
                arrayType == MutableFloatArray.class;
    }


    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link DoubleArray}.class</code>,
     * <code>{@link UpdatableDoubleArray}.class</code> or <code>{@link MutableDoubleArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for <code>double</code> arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART <code>double</code> arrays.
     */
    public static boolean isDoubleType(Class<? extends Array> arrayType) {
        return arrayType == DoubleArray.class ||
                arrayType == UpdatableDoubleArray.class ||
                arrayType == MutableDoubleArray.class;
    }


    /**
     * Return <code>true</code> if and only if the passed class is <code>{@link ObjectArray}.class</code>,
     * <code>{@link UpdatableObjectArray}.class</code> or <code>{@link MutableObjectArray}.class</code>
     * (but <i>not</i> some of their subinterfaces or subclasses).
     * These 3 classes are named <i>canonical AlgART array types</i> for object arrays:
     * see {@link Array#type()}, {@link Array#updatableType()}, {@link Array#mutableType()} methods.
     * Returns <code>false</code> if the argument is {@code null}.
     *
     * @param arrayType the checked array class.
     * @return whether the checked class is one of 3 canonical types of AlgART object arrays.
     */
    public static boolean isObjectType(Class<? extends Array> arrayType) {
        return arrayType == ObjectArray.class ||
                arrayType == UpdatableObjectArray.class ||
                arrayType == MutableObjectArray.class;
    }

    /**
     * Estimates the size of the array in bytes.
     *
     * <p>More precisely:
     * <ol>
     * <li>For {@link BitArray},
     * returns <code>8*{@link PackedBitArrays#packedLength(long)
     * PackedBitArrays.packedLength}(array.{@link Array#length() length()})</code>
     * (8 is the number of bytes in one <code>long</code> value).</li>
     *
     * <li>For other variants of {@link PArray},
     * returns <code>array.{@link Array#length() length()}*<i>sizeOfElement</i></code>,
     * where <code><i>sizeOfElement</i>=((PArray)array).{@link PArray#bitsPerElement()
     * bitsPerElement()}/8</code>.</li>
     *
     * <li>For {@link CombinedMemoryModel#isCombinedArray combined arrays},
     * returns the sum of the results of this method, recursively called
     * for all AlgART arrays contained in the underlying internal storage.</li>
     *
     * <li>In all other cases, returns <code>-1</code> ("unknown size").
     * Also returns <code>-1</code> in the case #3, if the result of this method
     * for some of arrays in the internal storage is <code>-1</code>.</li>
     * </ol>
     *
     * <p>In a case of calculation overflow, this method returns <code>Long.MAX_VALUE</code>
     * always. (For example, it's possible for
     * an array generated by {@link #nDoubleCopies(long, double)
     * nDoubleCopies(Long.MAX_VALUE, <i>someFiller</i>)} call.)
     *
     * @param array some AlgART array.
     * @return the estimated size of this array in bytes.
     * @throws NullPointerException if the argument is {@code null}.
     * @see #sizeOf(Class, long)
     * @see #sizeOf(Class)
     * @see Matrices#sizeOf(Matrix)
     * @see #sizeOfBytes(PArray)
     */
    public static long sizeOf(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (array instanceof BitArray) {
            return PackedBitArrays.packedLength(array.length()) << 3;
        } else if (array instanceof PArray) {
            long len = array.length();
            long bytesPerElementLog = 31 - Integer.numberOfLeadingZeros(
                    (int) ((PArray) array).bitsPerElement() >>> 3);
            if (len > Long.MAX_VALUE >> bytesPerElementLog) {
                return Long.MAX_VALUE;
            }
            return len << bytesPerElementLog;
        } else if (CombinedMemoryModel.isCombinedArray(array)) {
            Array[] storage = ((CombinedMemoryModel.CombinedArray<?>) array).storage;
            long result = 0;
            for (Array a : storage) {
                long size = sizeOf(a);
                if (size == -1) {
                    return -1;
                }
                if (result > Long.MAX_VALUE - size) {
                    return Long.MAX_VALUE;
                }
                result += size;
            }
            return result;
        } else {
            return -1;
        }
    }

    /**
     * Estimates the size in bytes of the array with the given primitive element type and the given length.
     *
     * <p>More precisely:
     * <ul>
     * <li>for <code>boolean.class</code>,
     * returns <code>8*{@link PackedBitArrays#packedLength(long)
     * PackedBitArrays.packedLength}(arrayLength)</code>
     * (8 is the number of bytes in one <code>long</code> value);</li>
     * <li>for <code>char.class</code>, returns <code>2*arrayLength</code>;</li>
     * <li>for <code>byte.class</code>, returns <code>arrayLength</code>;</li>
     * <li>for <code>short.class</code>, returns <code>2*arrayLength</code>;</li>
     * <li>for <code>int.class</code>, returns <code>4*arrayLength</code>;</li>
     * <li>for <code>long.class</code>, returns <code>8*arrayLength</code>;</li>
     * <li>for <code>float.class</code>, returns <code>4*arrayLength</code>;</li>
     * <li>for <code>double.class</code>, returns <code>8*arrayLength</code>;</li>
     * <li>for all other cases, returns <code>-1</code> ("unknown size").</li>
     * </ul>
     *
     * <p>There are two exceptions from these rules:</p>
     * <ol>
     * <li>in a case of calculation overflow, this method returns <code>Long.MAX_VALUE</code> always;</li>
     * <li>in a case of negative argument (<code>arrayLength&lt;0</code>), this method
     * returns <code>-1</code> always.</li>
     * </ol>
     *
     * <p>This method never throws exceptions.
     *
     * @param elementType some primitive element type; can be {@code null}, then &minus;1 is returned.
     * @param arrayLength the desired length of the AlgART array.
     * @return the estimated size of the AlgART array in bytes or &minus;1 if it is unknown.
     * @see #sizeOf(Array)
     */
    public static long sizeOf(Class<?> elementType, long arrayLength) {
        if (arrayLength < 0) {
            return -1;
        } else if (elementType == boolean.class) {
            return PackedBitArrays.packedLength(arrayLength) << 3;
        } else if (elementType == char.class) {
            return arrayLength > Long.MAX_VALUE >> 1 ? Long.MAX_VALUE : arrayLength << 1;
        } else if (elementType == byte.class) {
            return arrayLength;
        } else if (elementType == short.class) {
            return arrayLength > Long.MAX_VALUE >> 1 ? Long.MAX_VALUE : arrayLength << 1;
        } else if (elementType == int.class) {
            return arrayLength > Long.MAX_VALUE >> 2 ? Long.MAX_VALUE : arrayLength << 2;
        } else if (elementType == long.class) {
            return arrayLength > Long.MAX_VALUE >> 3 ? Long.MAX_VALUE : arrayLength << 3;
        } else if (elementType == float.class) {
            return arrayLength > Long.MAX_VALUE >> 2 ? Long.MAX_VALUE : arrayLength << 2;
        } else if (elementType == double.class) {
            return arrayLength > Long.MAX_VALUE >> 3 ? Long.MAX_VALUE : arrayLength << 3;
        } else {
            return -1;
        }
    }

    /**
     * Returns the exact size in bytes,
     * required to store each element of an array with the specified primitive element type.
     *
     * <p>More precisely:
     * <ul>
     * <li>for <code>boolean.class</code>, returns <code>0.125</code> (<sup>1</sup>/<sub>8</sub>);</li>
     * <li>for <code>char.class</code>, returns <code>2.0</code>;</li>
     * <li>for <code>byte.class</code>, returns <code>1.0</code>;</li>
     * <li>for <code>short.class</code>, returns <code>2.0</code>;</li>
     * <li>for <code>int.class</code>, returns <code>4.0</code>;</li>
     * <li>for <code>long.class</code>, returns <code>8.0</code>;</li>
     * <li>for <code>float.class</code>, returns <code>4.0</code>;</li>
     * <li>for <code>double.class</code>, returns <code>8.0</code>;</li>
     * <li>for all other cases, returns <code>-1.0</code> ("unknown size").</li>
     * </ul>
     *
     * @param elementType some primitive element type; can be {@code null}, then &minus;1 is returned.
     * @return the exact size of each element of the AlgART array in bytes or &minus;1 for non-primitive types.
     * @see #sizeOf(Array)
     * @see #bytesPerElement(Class)
     * @see #bitsPerElement(Class)
     */
    public static double sizeOf(Class<?> elementType) {
        if (elementType == boolean.class) {
            return 0.125;
        } else if (elementType == char.class) {
            return 2.0;
        } else if (elementType == byte.class) {
            return 1.0;
        } else if (elementType == short.class) {
            return 2.0;
        } else if (elementType == int.class) {
            return 4.0;
        } else if (elementType == long.class) {
            return 8.0;
        } else if (elementType == float.class) {
            return 4.0;
        } else if (elementType == double.class) {
            return 8.0;
        } else {
            return -1.0;
        }
    }

    /**
     * Returns the minimal integer number of bytes,
     * required to store each element of an array with the specified primitive element type.
     *
     * <p>More precisely:
     * <ul>
     * <li>for <code>boolean.class</code>, returns <code>1</code>;</li>
     * <li>for <code>char.class</code>, returns <code>2</code>;</li>
     * <li>for <code>byte.class</code>, returns <code>1</code>;</li>
     * <li>for <code>short.class</code>, returns <code>2</code>;</li>
     * <li>for <code>int.class</code>, returns <code>4</code>;</li>
     * <li>for <code>long.class</code>, returns <code>8</code>;</li>
     * <li>for <code>float.class</code>, returns <code>4</code>;</li>
     * <li>for <code>double.class</code>, returns <code>8</code>;</li>
     * <li>for all other cases, returns <code>-1</code> ("unknown size").</li>
     * </ul>
     *
     * @param elementType some primitive element type; can be {@code null}, then &minus;1 is returned.
     * @return the integer number of bytes, necessary to store such elements, or &minus;1 for non-primitive types.
     * @see #sizeOf(Class)
     */
    public static int bytesPerElement(Class<?> elementType) {
        if (elementType == boolean.class) {
            return 1;
        } else if (elementType == char.class) {
            return 2;
        } else if (elementType == byte.class) {
            return 1;
        } else if (elementType == short.class) {
            return 2;
        } else if (elementType == int.class) {
            return 4;
        } else if (elementType == long.class) {
            return 8;
        } else if (elementType == float.class) {
            return 4;
        } else if (elementType == double.class) {
            return 8;
        } else {
            return -1;
        }
    }

    /**
     * Returns the number of bits, required for each element of an array with the given primitive element type.
     *
     * <p>More precisely:
     * <ul>
     * <li>for <code>boolean.class</code>, returns <code>1</code>;
     * <li>for <code>char.class</code>, returns <code>16</code>;</li>
     * <li>for <code>byte.class</code>, returns <code>8</code>;</li>
     * <li>for <code>short.class</code>, returns <code>16</code>;</li>
     * <li>for <code>int.class</code>, returns <code>32</code>;</li>
     * <li>for <code>long.class</code>, returns <code>64</code>;</li>
     * <li>for <code>float.class</code>, returns <code>32</code>;</li>
     * <li>for <code>double.class</code>, returns <code>64</code>;</li>
     * <li>for all other cases, returns <code>-1</code> ("unknown size").</li>
     * </ul>
     *
     * @param elementType some primitive element type; can be {@code null}, then &minus;1 is returned.
     * @return the size of each element of the AlgART array in bits or &minus;1 if it is unknown.
     * @see #sizeOf(Class)
     * @see PArray#bitsPerElement()
     * @see Matrix#bitsPerElement()
     */
    public static long bitsPerElement(Class<?> elementType) {
        if (elementType == boolean.class) {
            return BITS_PER_BIT;
        } else if (elementType == char.class) {
            return BITS_PER_CHAR;
        } else if (elementType == byte.class) {
            return BITS_PER_BYTE;
        } else if (elementType == short.class) {
            return BITS_PER_SHORT;
        } else if (elementType == int.class) {
            return BITS_PER_INT;
        } else if (elementType == long.class) {
            return BITS_PER_LONG;
        } else if (elementType == float.class) {
            return BITS_PER_FLOAT;
        } else if (elementType == double.class) {
            return BITS_PER_DOUBLE;
        } else {
            return -1;
        }
    }

    /**
     * Returns <code>true</code> if the passed element type is <code>byte.class</code>,
     * <code>short.class</code>, <code>int.class</code>, <code>long.class</code>,
     * <code>float.class</code>, <code>double.class</code>,
     * <code>boolean.class</code> or <code>char.class</code>.
     * Equivalent to {@link Class#isPrimitive() elementType.isPrimitive()}, but
     * does not throw exception for {@code null} argument (<code>false</code> is returned for {@code null}).
     *
     * @param elementType some element type; can be {@code null}, then <code>false</code> is returned.
     * @return whether this element type is a primitive type.
     */
    public static boolean isPrimitiveElementType(Class<?> elementType) {
        return elementType != null && elementType.isPrimitive();
    }

    /**
     * Returns <code>true</code> if the passed element type is <code>byte.class</code>,
     * <code>short.class</code>, <code>int.class</code>, <code>long.class</code>,
     * <code>float.class</code> or <code>double.class</code>.
     *
     * @param elementType some element type; can be {@code null}, then <code>false</code> is returned.
     * @return whether this element type is a numeric primitive type (not <code>boolean</code> or <code>char</code>).
     */
    public static boolean isNumberElementType(Class<?> elementType) {
        return elementType == byte.class || elementType == short.class ||
                elementType == int.class || elementType == long.class ||
                elementType == float.class || elementType == double.class;
    }

    /**
     * Returns <code>true</code> if the passed element type is <code>float.class</code>
     * or <code>double.class</code>.
     *
     * @param elementType some element type; can be {@code null}, then <code>false</code> is returned.
     * @return whether this element type is a floating-point primitive type.
     */
    public static boolean isFloatingPointElementType(Class<?> elementType) {
        return elementType == float.class || elementType == double.class;
    }

    /**
     * Returns <code>true</code> if the passed element type is <code>boolean.class</code>,
     * <code>char.class</code>, <code>byte.class</code> or <code>short.class</code>.
     *
     * @param elementType some element type; can be {@code null}, then <code>false</code> is returned.
     * @return whether this element type is primitive and should be interpreted as unsigned primitive type
     * according AlgART agreements.
     */
    public static boolean isUnsignedElementType(Class<?> elementType) {
        return elementType == boolean.class
                || elementType == char.class
                || elementType == byte.class
                || elementType == short.class;
    }

    /**
     * Returns one of primitive element types <code>boolean.class</code>,
     * <code>short.class</code>, <code>byte.class</code>, <code>int.class</code>, <code>long.class</code>
     * (when <code>floatingPoint=false</code>) or one of <code>float.class</code> or <code>double.class</code>
     * (when <code>floatingPoint=true</code>) according to the specified number of bits per element.
     * The result of {@link #bitsPerElement(Class)} function, called for the resulting class,
     * will be equal to the <code>bitsPerElement</code> argument,
     * and the result of {@link #isFloatingPointElementType(Class)}
     * will be equal to the <code>floatingPoint</code> argument.
     *
     * <p>Note that this method never returns <code>char.class</code>: for 16 bits/element and
     * <code>floatingPoint=false</code> it returns <code>short.class</code>.</p>
     *
     * <p>Note: although the required number of bits per element is specified in <code>long</code> argument,
     * valid values do not require such precision: they must always be in the range 1..64.</p>
     *
     * @param bitsPerElement the number of bits per element for the returned primitive type.
     * @param floatingPoint whether the return type should be <code>float</code> / <code>double</code>.
     * @return the primitive type corresponding to the function arguments.
     * @throws IllegalArgumentException if there is no required primitive element type.
     */
    public static Class<?> primitiveType(long bitsPerElement, boolean floatingPoint) {
        if (bitsPerElement <= 0) {
            throw new IllegalArgumentException("Zero or negative number of bits/element = " + bitsPerElement);
        }
        if (bitsPerElement > 64) {
            throw new IllegalArgumentException("Too large number of bits/element = " + bitsPerElement +
                    ": there are no primitive types with >64 bits/element");
        }
        return floatingPoint ?
                switch ((int) bitsPerElement) {
                    case 32 -> float.class;
                    case 64 -> double.class;
                    default -> throw new IllegalArgumentException(bitsPerElement +
                            " bits/element is impossible for floating-point primitive types");
                } :
                switch ((int) bitsPerElement) {
                    case 1 -> boolean.class;
                    case 8 -> byte.class;
                    case 16 -> short.class;
                    case 32 -> int.class;
                    case 64 -> long.class;
                    default -> throw new IllegalArgumentException(bitsPerElement +
                            " bits/element is impossible for fixed-point primitive types");
                };
    }

    /**
     * Returns the minimal possible value, that can be stored in elements of the fixed-point primitive array
     * (<code>byte</code> and <code>short</code> elements are interpreted as unsigned).
     * Namely, returns:
     * <ul>
     * <li>0 for <code>{@link BitArray}.class</code> (and its inheritors),</li>
     * <li>0 for <code>{@link CharArray}.class</code> (and its inheritors),</li>
     * <li>0 for <code>{@link ByteArray}.class</code> (and its inheritors),</li>
     * <li>0 for <code>{@link ShortArray}.class</code> (and its inheritors),</li>
     * <li><code>Integer.MIN_VALUE</code> for <code>{@link IntArray}.class</code> (and its inheritors),</li>
     * <li><code>Long.MIN_VALUE</code> for <code>{@link LongArray}.class</code> (and its inheritors).</li>
     * </ul>
     * In all other cases, this method throws <code>IllegalArgumentException</code>.
     *
     * @param arrayType the type of some fixed-point primitive array.
     * @return the minimal possible value, that can be stored in such arrays.
     * @throws NullPointerException     if the passed argument is {@code null}.
     * @throws IllegalArgumentException if the passed argument is not {@link BitArray}, {@link CharArray},
     *                                  {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray}
     *                                  or an inheritor / implementing class of some of these interfaces.
     * @see PFixedArray#minPossibleValue()
     */
    public static long minPossibleIntegerValue(Class<? extends PFixedArray> arrayType) {
        Objects.requireNonNull(arrayType, "Null arrayType argument");
        if (BitArray.class.isAssignableFrom(arrayType)) {
            return 0;
        } else if (CharArray.class.isAssignableFrom(arrayType)) {
            return 0;
        } else if (ByteArray.class.isAssignableFrom(arrayType)) {
            return 0;
        } else if (ShortArray.class.isAssignableFrom(arrayType)) {
            return 0;
        } else if (IntArray.class.isAssignableFrom(arrayType)) {
            return Integer.MIN_VALUE;
        } else if (LongArray.class.isAssignableFrom(arrayType)) {
            return Long.MIN_VALUE;
        } else {
            throw new IllegalArgumentException(
                    "Only BitArray, CharArray, ByteArray, ShortArray, IntArray, LongArray and their inheritors "
                            + "are allowed here (passed type: " + arrayType + ")");
        }
    }

    /**
     * Returns the maximal possible value, that can be stored in elements of the fixed-point primitive array
     * (<code>byte</code> and <code>short</code> elements are interpreted as unsigned).
     * Namely, returns:
     * <ul>
     * <li>1 for <code>{@link BitArray}.class</code> (and its inheritors),</li>
     * <li>0xFFFF for <code>{@link CharArray}.class</code> (and its inheritors),</li>
     * <li>0xFF for <code>{@link ByteArray}.class</code> (and its inheritors),</li>
     * <li>0xFFFF for <code>{@link ShortArray}.class</code> (and its inheritors),</li>
     * <li><code>Integer.MAX_VALUE</code> for <code>{@link IntArray}.class</code> (and its inheritors),</li>
     * <li><code>Long.MAX_VALUE</code> for <code>{@link LongArray}.class</code> (and its inheritors).</li>
     * </ul>
     * In all other cases, this method throws <code>IllegalArgumentException</code>.
     *
     * @param arrayType the type of some fixed-point primitive array.
     * @return the maximal possible value, that can be stored in such arrays.
     * @throws NullPointerException     if the passed argument is {@code null}.
     * @throws IllegalArgumentException if the passed argument is not {@link BitArray}, {@link CharArray},
     *                                  {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray}
     *                                  or an inheritor / implementing class of some of these interfaces.
     * @see PFixedArray#maxPossibleValue()
     */
    public static long maxPossibleIntegerValue(Class<? extends PFixedArray> arrayType) {
        Objects.requireNonNull(arrayType, "Null arrayType argument");
        if (BitArray.class.isAssignableFrom(arrayType)) {
            return 1;
        } else if (CharArray.class.isAssignableFrom(arrayType)) {
            return 0xFFFF;
        } else if (ByteArray.class.isAssignableFrom(arrayType)) {
            return 0xFF;
        } else if (ShortArray.class.isAssignableFrom(arrayType)) {
            return 0xFFFF;
        } else if (IntArray.class.isAssignableFrom(arrayType)) {
            return Integer.MAX_VALUE;
        } else if (LongArray.class.isAssignableFrom(arrayType)) {
            return Long.MAX_VALUE;
        } else {
            throw new IllegalArgumentException(
                    "Only BitArray, CharArray, ByteArray, ShortArray, IntArray, LongArray and their inheritors "
                            + "are allowed here (passed type: " + arrayType + ")");
        }
    }

    /**
     * If <code>arrayType</code> is {@link BitArray}, {@link CharArray}, {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray} or an inheritor / implementing class of some of these interfaces,
     * this method is equivalent to
     * <code>(double){@link #minPossibleIntegerValue minPossibleIntegerValue(arrayType)}</code>;
     * if <code>arrayType</code> is {@link FloatArray}, {@link DoubleArray}, {@link ObjectArray}
     * or an inheritor / implementing class of some of these interfaces,
     * returns <code>valueForFloatingPoint</code>.
     * In all other cases &mdash; for example, if the passed class is a common interface as {@link PArray},
     * that can have different minimal elements &mdash; this method throws <code>IllegalArgumentException</code>.
     *
     * @param arrayType             the type of some AlgART array.
     * @param valueForFloatingPoint the value returned for floating-point or object array type.
     * @return the minimal possible value, that can be stored in such fixed-point primitive
     * arrays, or <code>valueForFloatingPoint</code> for other array types.
     * @throws NullPointerException     if the passed <code>arrayType</code> is {@code null}.
     * @throws IllegalArgumentException if the passed <code>arrayType</code>
     *                                  is not {@link BitArray}, {@link CharArray},
     *                                  {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray},
     *                                  {@link FloatArray}, {@link DoubleArray}, {@link ObjectArray}
     *                                  or an inheritor / implementing class of some of these interfaces.
     * @see PArray#minPossibleValue(double)
     */
    public static double minPossibleValue(Class<? extends Array> arrayType, double valueForFloatingPoint) {
        Objects.requireNonNull(arrayType, "Null arrayType argument");
        if (PFixedArray.class.isAssignableFrom(arrayType)) {
            return minPossibleIntegerValue(arrayType.asSubclass(PFixedArray.class));
        } else if (DoubleArray.class.isAssignableFrom(arrayType) ||
                FloatArray.class.isAssignableFrom(arrayType) ||
                ObjectArray.class.isAssignableFrom(arrayType)) {
            return valueForFloatingPoint;
        } else {
            throw new IllegalArgumentException(
                    "Only BitArray, CharArray, ByteArray, ShortArray, IntArray, LongArray, "
                            + "FloatArray, DoubleArray, ObjectArray and their inheritors "
                            + "are allowed here (passed type: " + arrayType + ")");
        }
    }

    /**
     * If <code>arrayType</code> is {@link BitArray}, {@link CharArray}, {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray} or an inheritor / implementing class of some of these interfaces,
     * this method is equivalent to
     * <code>(double){@link #maxPossibleIntegerValue maxPossibleIntegerValue(arrayType)}</code>;
     * if <code>arrayType</code> is {@link FloatArray}, {@link DoubleArray}, {@link ObjectArray}
     * or an inheritor / implementing class of some of these interfaces,
     * returns <code>valueForFloatingPoint</code>.
     * In all other cases &mdash; for example, if the passed class is a common interface as {@link PArray},
     * that can have different maximal elements &mdash; this method throws <code>IllegalArgumentException</code>.
     *
     * @param arrayType             the type of some AlgART array.
     * @param valueForFloatingPoint the value returned for floating-point or object array type.
     * @return the maximal possible value, that can be stored in such fixed-point primitive
     * arrays, or <code>valueForFloatingPoint</code> for other array types.
     * @throws NullPointerException     if the passed <code>arrayType</code> is {@code null}.
     * @throws IllegalArgumentException if the passed <code>arrayType</code> is not {@link BitArray}, {@link CharArray},
     *                                  {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray},
     *                                  {@link FloatArray}, {@link DoubleArray}, {@link ObjectArray}
     *                                  or an inheritor / implementing class of some of these interfaces.
     * @see PArray#maxPossibleValue(double)
     */
    public static double maxPossibleValue(Class<? extends Array> arrayType, double valueForFloatingPoint) {
        Objects.requireNonNull(arrayType, "Null arrayType argument");
        if (PFixedArray.class.isAssignableFrom(arrayType)) {
            return maxPossibleIntegerValue(arrayType.asSubclass(PFixedArray.class));
        } else if (DoubleArray.class.isAssignableFrom(arrayType) ||
                FloatArray.class.isAssignableFrom(arrayType) ||
                ObjectArray.class.isAssignableFrom(arrayType)) {
            return valueForFloatingPoint;
        } else {
            throw new IllegalArgumentException(
                    "Only BitArray, CharArray, ByteArray, ShortArray, IntArray, LongArray, "
                            + "FloatArray, DoubleArray, ObjectArray and their inheritors "
                            + "are allowed here (passed type: " + arrayType + ")");
        }
    }

    /**
     * Returns {@link #minPossibleValue(Class, double) minPossibleValue(arrayType, 0.0)}.
     * It is a good default for most application.
     *
     * @param arrayType the type of some AlgART array.
     * @return the minimal possible value, that can be stored in such fixed-point primitive
     * arrays, or <code>0.0</code> for other array types.
     * @throws NullPointerException     if the passed <code>arrayType</code> is {@code null}.
     * @throws IllegalArgumentException if the passed <code>arrayType</code>
     *                                  is not {@link BitArray}, {@link CharArray},
     *                                  {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray},
     *                                  {@link FloatArray}, {@link DoubleArray}, {@link ObjectArray}
     *                                  or an inheritor / implementing class of some of these interfaces.
     */
    public static double minPossibleValue(Class<? extends Array> arrayType) {
        return minPossibleValue(arrayType, 0.0);
    }

    /**
     * Returns {@link #minPossibleValue(Class, double) minPossibleValue(arrayType, 1.0)}.
     * It is a good default for most application.
     *
     * @param arrayType the type of some AlgART array.
     * @return the maximal possible value, that can be stored in such fixed-point primitive
     * arrays, or <code>1.0</code> for other array types.
     * @throws NullPointerException     if the passed <code>arrayType</code> is {@code null}.
     * @throws IllegalArgumentException if the passed <code>arrayType</code>
     *                                  is not {@link BitArray}, {@link CharArray},
     *                                  {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray},
     *                                  {@link FloatArray}, {@link DoubleArray}, {@link ObjectArray}
     *                                  or an inheritor / implementing class of some of these interfaces.
     * @see PArray#maxPossibleValue(double)
     */
    public static double maxPossibleValue(Class<? extends Array> arrayType) {
        return maxPossibleValue(arrayType, 1.0);
    }

    /*Repeat() (?<!ArrayContext,\s)boolean|bit   ==> char,,byte,,short,,int,,long,,float,,double;;
               Bit                               ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double */

    /**
     * Constructs an immutable unresizable bit array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static BitArray nBitCopies(long n, boolean element) {
        return new CopiesArraysImpl.CopiesBitArray(n, element);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Constructs an immutable unresizable char array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static CharArray nCharCopies(long n, char element) {
        return new CopiesArraysImpl.CopiesCharArray(n, element);
    }

    /**
     * Constructs an immutable unresizable byte array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static ByteArray nByteCopies(long n, byte element) {
        return new CopiesArraysImpl.CopiesByteArray(n, element);
    }

    /**
     * Constructs an immutable unresizable short array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static ShortArray nShortCopies(long n, short element) {
        return new CopiesArraysImpl.CopiesShortArray(n, element);
    }

    /**
     * Constructs an immutable unresizable int array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static IntArray nIntCopies(long n, int element) {
        return new CopiesArraysImpl.CopiesIntArray(n, element);
    }

    /**
     * Constructs an immutable unresizable long array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static LongArray nLongCopies(long n, long element) {
        return new CopiesArraysImpl.CopiesLongArray(n, element);
    }

    /**
     * Constructs an immutable unresizable float array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static FloatArray nFloatCopies(long n, float element) {
        return new CopiesArraysImpl.CopiesFloatArray(n, element);
    }

    /**
     * Constructs an immutable unresizable double array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static DoubleArray nDoubleCopies(long n, double element) {
        return new CopiesArraysImpl.CopiesDoubleArray(n, element);
    }
    /*Repeat.AutoGeneratedEnd*/


    /**
     * Constructs an immutable unresizable Object array consisting of <code>n</code> copies of the
     * specified <code>element</code>. The newly allocated data object is tiny
     * (it contains a single instance of the element).
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param <T>     the generic type of array elements.
     * @param n       the number of elements in the returned array.
     * @param element the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws IllegalArgumentException if <code>n &lt; 0</code>.
     */
    public static <T> ObjectArray<T> nObjectCopies(long n, T element) {
        return new CopiesArraysImpl.CopiesObjectArray<>(n, element);
    }

    /**
     * Constructs an immutable unresizable array consisting of <code>n</code> copies of the specified double
     * <code>element</code>, cast to the specified element type.
     * More precisely, this method returns:
     * <ul>
     * <li><code>{@link #nBitCopies(long, boolean) nBitCopies}(n, element != 0)</code>
     * if <code>elementType==boolean.class</code>,</li>
     * <li><code>{@link #nCharCopies(long, char) nCharCopies}(n, (char)element)</code>
     * if <code>elementType==char.class</code>,</li>
     * <li><code>{@link #nByteCopies(long, byte) nByteCopies}(n, (byte)element)</code>
     * if <code>elementType==byte.class</code>,</li>
     * <li><code>{@link #nShortCopies(long, short) nShortCopies}(n, (short)element)</code>
     * if <code>elementType==short.class</code>,</li>
     * <li><code>{@link #nIntCopies(long, int) nIntCopies}(n, (int)element)</code>
     * if <code>elementType==int.class</code>,</li>
     * <li><code>{@link #nLongCopies(long, long) nLongCopies}(n, (long)element)</code>
     * if <code>elementType==long.class</code>,</li>
     * <li><code>{@link #nFloatCopies(long, float) nFloatCopies}(n, (float)element)</code>
     * if <code>elementType==float.class</code> or</li>
     * <li><code>{@link #nDoubleCopies(long, double) nDoubleCopies}(n, element)</code>
     * if <code>elementType==double.class</code>.</li>
     * </ul>
     * Non-primitive element types are not allowed.
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n           the number of elements in the returned array.
     * @param elementType the element type (primitive) of the returned array.
     * @param element     the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws NullPointerException     if <code>elementType</code> is {@code null}.
     * @throws IllegalArgumentException if <code>n &lt; 0</code> or if <code>elementType</code> is not
     *                                  <code>boolean.class</code>, <code>char.class</code>, <code>byte.class</code>,
     *                                  <code>short.class</code>, <code>int.class</code>, <code>long.class</code>,
     *                                  <code>float.class</code> or <code>double.class</code>.
     */
    public static PArray nPCopies(long n, Class<?> elementType, double element) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (elementType == boolean.class) {
            return nBitCopies(n, element != 0);
        } else if (elementType == char.class) {
            return nCharCopies(n, (char) element);
        } else if (elementType == byte.class) {
            return nByteCopies(n, (byte) element);
        } else if (elementType == short.class) {
            return nShortCopies(n, (short) element);
        } else if (elementType == int.class) {
            return nIntCopies(n, (int) element);
        } else if (elementType == long.class) {
            return nLongCopies(n, (long) element);
        } else if (elementType == float.class) {
            return nFloatCopies(n, (float) element);
        } else if (elementType == double.class) {
            return nDoubleCopies(n, element);
        } else {
            throw new IllegalArgumentException(
                    "Only primitive types are allowed here (passed element type: " + elementType + ")");
        }
    }

    /**
     * Constructs an immutable unresizable array consisting of <code>n</code> copies of the specified integer
     * <code>element</code>, cast to the specified element type.
     * More precisely, this method returns:
     * <ul>
     * <li><code>{@link #nBitCopies(long, boolean) nBitCopies}(n, element != 0)</code>
     * if <code>elementType==boolean.class</code>,</li>
     * <li><code>{@link #nCharCopies(long, char) nCharCopies}(n, (char)element)</code>
     * if <code>elementType==char.class</code>,</li>
     * <li><code>{@link #nByteCopies(long, byte) nByteCopies}(n, (byte)element)</code>
     * if <code>elementType==byte.class</code>,</li>
     * <li><code>{@link #nShortCopies(long, short) nShortCopies}(n, (short)element)</code>
     * if <code>elementType==short.class</code>,</li>
     * <li><code>{@link #nIntCopies(long, int) nIntCopies}(n, (int)element)</code>
     * if <code>elementType==int.class</code> or</li>
     * <li><code>{@link #nLongCopies(long, long) nLongCopies}(n, element)</code>
     * if <code>elementType==long.class</code>.</li>
     * </ul>
     * All other element types are not allowed.
     *
     * <p>In the returned array:<ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the passed <code>n</code> argument;</li>
     * <li>{@link Array#isNew()} method will return <code>true</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method will return <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will do nothing;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param n           the number of elements in the returned array.
     * @param elementType the element type of the returned array (<code>boolean.class</code>, <code>char.class</code>,
     *                    <code>byte.class</code>, <code>short.class</code>, <code>int.class</code>
     *                    or <code>long.class</code>).
     * @param element     the element to appear repeatedly in the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> copies of the specified element.
     * @throws NullPointerException     if <code>elementType</code> is {@code null}.
     * @throws IllegalArgumentException if <code>n &lt; 0</code> or if <code>elementType</code> is not
     *                                  <code>boolean.class</code>, <code>char.class</code>, <code>byte.class</code>,
     *                                  <code>short.class</code>, <code>int.class</code> or <code>long.class</code>.
     */
    public static PFixedArray nPFixedCopies(long n, Class<?> elementType, long element) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        if (elementType == boolean.class) {
            return nBitCopies(n, element != 0);
        } else if (elementType == char.class) {
            return nCharCopies(n, (char) element);
        } else if (elementType == byte.class) {
            return nByteCopies(n, (byte) element);
        } else if (elementType == short.class) {
            return nShortCopies(n, (short) element);
        } else if (elementType == int.class) {
            return nIntCopies(n, (int) element);
        } else if (elementType == long.class) {
            return nLongCopies(n, element);
        } else {
            throw new IllegalArgumentException(
                    "Only boolean.class, char.class, byte.class, short.class, int.class and long.class "
                            + "are allowed here (passed element type: " + elementType + ")");
        }
    }

    /**
     * The version of {@link #nObjectCopies(long, Object) nObjectCopies(n, element)} method
     * with <code>element=null</code> that allows to specify the {@link Array#elementType() element type}
     * of the returned array. (For that method, the element type will be <code>element.getClass()</code>,
     * but if <code>element</code> is {@code null}, the element type is always <code>Object.class</code>,
     * that leads to problems while copying the created array to another object arrays.)
     *
     * @param <E>         the generic type of array elements.
     * @param n           the number of null elements in the returned array.
     * @param elementType the element type of the returned array.
     * @return an immutable unresizable array consisting of <code>n</code> {@code null} elements.
     * @throws IllegalArgumentException if n &lt; 0.
     */
    public static <E> ObjectArray<E> nNullCopies(long n, Class<E> elementType) {
        CopiesArraysImpl.CopiesObjectArray<E> result = new CopiesArraysImpl.CopiesObjectArray<>(n, null);
        result.elementType = elementType;
        return result;
    }

    /**
     * Returns <code>true</code> if the passed array is not {@code null} and was created
     * by one of <code>n<i>Xxx</i>Copies</code>
     * methods of this class and, so, all array elements have the same constant value.
     * This fact can be used for optimize loops where some elements are loaded from an array
     * many times, for example:
     *
     * <pre>
     * for (long disp = 0; disp &lt; a.length(); disp += 1024) {
     * &#32;   if (!(disp &gt; 0 &amp;&amp; Arrays.isNCopies(a))) {
     * &#32;       // there is no sense to read data more than 1 time in a case of a constant array
     * &#32;       a.getData(disp, buffer);
     * &#32;   }
     * &#32;   // processing "buffer" Java array
     * }
     * </pre>
     *
     * <p>Such optimization is performed automatically while accessing a constant array via
     * a {@link DataBuffer data buffer}.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array was created by one of <code>n<i>Xxx</i>Copies</code>
     * methods of this class.
     */
    public static boolean isNCopies(Array array) {
        return array instanceof CopiesArraysImpl.CopiesArray;
    }

    /**
     * Equivalent to {@link #asIndexFuncArray(boolean, Func, Class, long)
     * asIndexFuncArray(true, f, requiredType, length)}.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function used for calculating all result array elements.
     * @param requiredType desired type of the returned array.
     * @param length       the length of the returned array.
     * @return the array, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}
     *                                  or if one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asIndexFuncArray(boolean, Func, Class, long)} method.
     */
    public static <T extends PArray> T asIndexFuncArray(Func f, Class<? extends T> requiredType, long length) {
        return asIndexFuncArray(true, f, requiredType, length);
    }

    /**
     * An analog of the {@link #asFuncArray(boolean, Func, Class, PArray...)} method,
     * where the passed function is applied not to the elements of some source array,
     * but to the index of the resulting array.
     * In other words, each element <code>#k</code> of the returned array is a result of calling
     * <code>f.{@link Func#get(double...) get}(k)</code>.
     * So, this method does not require any source arrays.
     *
     * <p>The typical example is using {@link ConstantFunc}, when this method works alike
     * <code>nXxxCopies</code> methods.
     *
     * <p>Please read comments to {@link #asFuncArray(boolean, Func, Class, PArray...)} method
     * about precise details of forming the elements of the returned array on the base
     * of the real result of calling {@link Func#get(double...)} method.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>,
     *                          <code>short</code>, <code>byte</code>
     *                          and <code>char</code> resulting values (see comments to
     *                          {@link #asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function used for calculating all result array elements.
     * @param requiredType      desired type of the returned array.
     * @param length            the length of the returned array.
     * @return the array, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>length</code> is negative,
     *                                  or if <code>requiredType</code> is an inheritor of {@link UpdatableArray}
     *                                  (returned array must be immutable),
     *                                  or if <code>requiredType</code> is not one of classes
     *                                  <code>{@link BitArray}.class</code>, <code>{@link CharArray}.class</code>,
     *                                  <code>{@link ByteArray}.class</code>, <code>{@link ShortArray}.class</code>,
     *                                  <code>{@link IntArray}.class</code>, <code>{@link LongArray}.class</code>,
     *                                  <code>{@link FloatArray}.class</code> or
     *                                  <code>{@link DoubleArray}.class</code>.
     *                                  (Also <code>IndexOutOfBoundsException</code>
     *                                  is possible while further attempts to read elements
     *                                  from the returned AlgART array, if the passed function
     *                                  requires some arguments.)
     */
    public static <T extends PArray> T asIndexFuncArray(
            boolean truncateOverflows,
            final Func f, Class<? extends T> requiredType, long length) {
        return ArraysFuncImpl.asCoordFuncMatrix(truncateOverflows,
                f, requiredType, new long[]{length});
    }

    /**
     * Equivalent to {@link #asFuncArray(boolean, Func, Class, PArray...)
     * asFuncArray(true, f, requiredType, x)}.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to all passed AlgART arrays.
     * @param requiredType desired type of the returned array.
     * @param x            several AlgART arrays; at least one array must be passed.
     * @return a view of the passed <code>x</code> arrays, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}
     *                                  or if one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException in the same situations as
     *                                  {@link #asFuncArray(boolean, Func, Class, PArray...)} method.
     * @throws SizeMismatchException    if <code>x.length&gt;1</code> and some of passed arrays have different lengths.
     */
    public static <T extends PArray> T asFuncArray(
            Func f, Class<? extends T> requiredType, PArray... x) {
        return asFuncArray(true, f, requiredType, x);
    }

    /**
     * Returns an immutable view of the passed <code>x</code> AlgART arrays (with primitive element type),
     * where each element <code>#k</code> is a result of calling
     * <code>f.{@link Func#get(double...) get}(x[0].{@link PArray#getDouble
     * getDouble}(k), x[1].{@link PArray#getDouble getDouble}(k), ...)</code>,
     * i&#46;e&#46; the result of the passed function for arguments, equal
     * to the corresponding elements <code>#k</code>
     * in all passed arrays.
     *
     * <p>The array, returned by this method, is immutable, and its class implements one of the basic interfaces
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray} or {@link DoubleArray}.
     * The class of desired interface (one of 8 possible classes) must be passed as
     * <code>requiredType</code> argument.
     * For example, if <code>requiredType={@link ByteArray}.class</code>,
     * he returned array implements {@link ByteArray}.
     *
     * <p>If the required result type is not a {@link DoubleArray}, then some results
     * of the calculated mathematical function <code>f</code> may not be directly interpreted as elements
     * of the returned array: typecasting is necessary. This method performs necessary typecasting
     * by the following algorithm.
     *
     * <p>Let's suppose that the source double value (result of calling <code>f</code> function) is <code>v</code>,
     * and the necessary cast value (result of specific <code>getXxx</code> method in the returned array,
     * for example, {@link ByteArray#getByte(long)}) is <code>w</code>.
     *
     * <ul>
     * <li>The simplest case is when the required type is {@link DoubleArray}: then <code>w=v</code>.</li>
     *
     * <li>If the required type is {@link FloatArray}, then <code>w=(float)v</code>.</li>
     *
     * <li>If the required type is {@link IntArray}, {@link ShortArray}, {@link ByteArray} or {@link CharArray},
     * then the behavior depends on the <code>truncateOverflows</code> argument.
     * <ul>
     * <li>If <code>truncateOverflows</code> is <code>false</code>:
     * <ul>
     * <li><code>w=(int)(long)v</code> for {@link IntArray},</li>
     * <li><code>w=(short)(long)v&amp;0xFFFF</code> for {@link ShortArray},</li>
     * <li><code>w=(byte)(long)v&amp;0xFF</code> for {@link ByteArray},</li>
     * <li><code>w=(char)(long)v</code> for {@link CharArray}.</li>
     * </ul>
     * ({@link ByteArray} and {@link ShortArray} are considered to be unsigned.)
     * In other words, <code>v</code> is cast to the maximally "wide" <code>long</code> type and
     * then the low 32, 16 or 8 bits of this <code>long</code> value are extracted.
     * </li>
     * <li>If <code>truncateOverflows</code> is <code>true</code>:
     * <ul>
     * <li><code>w=(int)v</code> for {@link IntArray},</li>
     * <li><code>w=(int)v&lt;0?0:(int)v&gt;0xFFFF?0xFFFF:(int)v</code> for {@link ShortArray},</li>
     * <li><code>w=(int)v&lt;0?0:(int)v&gt;0xFF?0xFF:(int)v</code> for {@link ByteArray},</li>
     * <li><code>w=(int)v&lt;0?0:(int)v&gt;0xFFFF?0xFFFF:(char)(int)v</code> for {@link CharArray}.</li>
     * </ul>
     * In other words, <code>v</code> is truncated to the allowed range and then cast to
     * the required integer type.
     * </li>
     * </ul>
     *
     * <li>If the required type is {@link LongArray}, then <code>w=(long)v</code>. According the Java specification,
     * it means <code>v</code>, truncated to <code>long</code> value,
     * if <code>Long.MIN_VALUE&lt;=v&lt;=Long.MAX_VALUE</code>,
     * or the nearest from <code>Long.MIN_VALUE / Long.MAX_VALUE</code> if <code>v</code> is out of this range.
     * In comparison with other integer types, it's possible to say that
     * the <code>truncateOverflows</code> argument is always supposed to be <code>true</code>.</li>
     *
     * <li>If the required type is {@link BitArray}, then <code>w=true</code> if <code>v!=0.0</code>
     * or <code>w=false</code> if <code>v==0.0</code> (C-like agreement).</li>
     * </ul>
     *
     * <p>The <code>x</code> argument is "vararg"; so you may pass a Java array as this argument.
     * In this case, no references to the passed Java array are stored in the result object:
     * it is cloned by this method.
     *
     * <p>The {@link Array#length() lengths} of all passed <code>x</code> arrays must be equal;
     * in another case, an exception is thrown.
     *
     * <p>In the returned array:
     *
     * <ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the length of the passed <code>x</code> arrays;</li>
     * <li>{@link Array#isNew()} method returns <code>false</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method returns <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will call the same methods for all <code>x</code> arrays;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * <p><b>Important note.</b> All calculations in the returned view are performed
     * in the correct left-to-right order. More precisely, it means that any method,
     * accessing the data in the returned array, works in the following (or strictly equivalent) way:
     * <ul>
     * <li>every data block (sequence of elements) is first fully calculated
     * (on the base the underlying arrays, via the passed function);</li>
     * <li>then it is returned to the client (loaded into Java array, copied into another AlgART array, etc.):
     * the result of accessing method is never modified before the data block
     * is completely calculated;</li>
     * <li>if there are several calculated data blocks, then the blocks with less indexes of elements
     * are always processed before blocks with greater indexes.</li>
     * </ul>
     *
     * <p>This behavior allows using this method for performing "in-place" algorithms,
     * when the result of this method is copied into one of its <code>x</code> arguments
     * (usually built by {@link SimpleMemoryModel}).
     * Moreover, such algorithms will work correctly even
     * if the arguments and the destination array are overlapping subarrays of a single AlgART array,
     * if the starting offset of the destination array is not greater than the starting offsets
     * of the arguments.
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>,
     *                          <code>short</code>, <code>byte</code>
     *                          and <code>char</code> resulting values (see above).
     * @param f                 the mathematical function applied to all passed AlgART arrays.
     * @param requiredType      desired type of the returned array.
     * @param x                 several AlgART arrays; at least one array must be passed.
     * @return a view of the passed <code>x</code> arrays, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}
     *                                  or if one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException if <code>x.length==0</code> (no arrays passed),
     *                                  or if <code>requiredType</code> is an inheritor of {@link UpdatableArray}
     *                                  (returned array must be immutable),
     *                                  or if <code>requiredType</code> is not one of classes
     *                                  <code>{@link BitArray}.class</code>, <code>{@link CharArray}.class</code>,
     *                                  <code>{@link ByteArray}.class</code>, <code>{@link ShortArray}.class</code>,
     *                                  <code>{@link IntArray}.class</code>, <code>{@link LongArray}.class</code>,
     *                                  <code>{@link FloatArray}.class</code> or
     *                                  <code>{@link DoubleArray}.class</code>,
     *                                  or if the number of passed <code>x</code> arrays is insufficient
     *                                  for calculating the passed <code>f</code> function.
     *                                  (In the last situation, <code>IllegalArgumentException</code>
     *                                  may not occur immediately, but <code>IndexOutOfBoundsException</code>
     *                                  may occur instead of while further attempts to read elements
     *                                  from the returned AlgART array.)
     * @throws SizeMismatchException    if <code>x.length&gt;1</code> and some of passed arrays have different lengths.
     * @see #asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)
     */
    public static <T extends PArray> T asFuncArray(
            boolean truncateOverflows,
            final Func f,
            Class<? extends T> requiredType,
            PArray... x) {
        Objects.requireNonNull(x, "Null x");
        if (x.length == 0) {
            throw new IllegalArgumentException("Empty x[] (array of AlgART arrays)");
        }
        Objects.requireNonNull(x[0], "Null x[0] argument");
        return ArraysFuncImpl.asFuncArray(truncateOverflows, f, requiredType, x, x[0].length());
    }

    /**
     * Equivalent to {@link
     * #asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)
     * asUpdatableFuncArray(true, f, requiredType, x)}.
     *
     * @param <T>          the generic type of AlgART array.
     * @param f            the mathematical function applied to all passed AlgART arrays.
     * @param requiredType desired type of the returned array.
     * @param x            several AlgART arrays; at least one array must be passed.
     * @return an updatable view of the passed arrays, defined by the passed function.
     * @throws NullPointerException     if <code>requiredType</code> or <code>x</code> argument is {@code null}
     *                                  or if one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException in the same situations as {@link
     *                                  #asUpdatableFuncArray(boolean,
     *                                  net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)} method.
     */
    public static <T extends UpdatablePArray> T asUpdatableFuncArray(
            final Func.Updatable f, Class<? extends T> requiredType, final UpdatablePArray... x) {
        return ArraysFuncImpl.asUpdatableFuncArray(true, f, requiredType, x);
    }

    /**
     * An extended analog of the {@link #asFuncArray(boolean, Func, Class, PArray...)} method
     * allowing to create updatable view of the source array.
     *
     * <p>The {@link #asFuncArray(boolean, Func, Class, PArray...)} method allows
     * creating immutable views only: you cannot change the original arrays via the returned view.
     * Unlike that, this method returns a mutable (updatable) view. Any changes of the elements
     * in the returned array lead to corresponding changes of the elements of the original (underlying) array.
     * Namely, setting some element <code>#k</code> of the returned view to the value <code>w</code>
     * is equivalent to the following operations:
     * <ol>
     * <li>
     * getting the elements <code>#k</code> of the original arrays <code>x</code>
     * by <code>x[0].{@link PArray#getDouble getDouble}(k),
     * x[1].{@link PArray#getDouble getDouble}(k), ...
     * x[n-1].{@link PArray#getDouble getDouble}(k)</code> calls
     * into a temporary array <code>double args[n]</code>;</li>
     * <li>calling <code>f.{@link net.algart.math.functions.Func.Updatable#set set}(args, w)</code> &mdash;
     * this call should correct <code>args</code> elements (usually the first element);</li>
     * <li>back saving the elements <code>#k</code> into the original arrays <code>x</code>
     * by <code>x[0].{@link UpdatablePArray#setDouble setDouble}(k),
     * x[1].{@link UpdatablePArray#setDouble setDouble}(k), ...
     * x[n-1].{@link UpdatablePArray#setDouble setDouble}(k)</code> calls.</li>
     * </ol>
     * <p>The stage #1 <i>may be skipped</i> if the number of <code>x</code> arrays is 1.
     * In this case, this method supposes that the
     * <code>f.{@link net.algart.math.functions.Func.Updatable#set set}(args,&nbsp;w)</code> has enough information
     * to calculate <code>args[0]</code>: the result of the inverse function for <code>w</code> value.
     *
     * <p>As in the {@link #asFuncArray(boolean, Func, Class, PArray...)} method,
     * typecasting is required sometimes while setting elements. The rules of typecasting are the same
     * as in that method; in particular, the <code>truncateOverflows</code> is used in the same manner.
     *
     * <p>The array, returned by this method, is unresizable, and its class implements one of the basic interfaces
     * {@link UpdatableBitArray}, {@link UpdatableCharArray},
     * {@link UpdatableByteArray}, {@link UpdatableShortArray},
     * {@link UpdatableIntArray}, {@link UpdatableLongArray},
     * {@link UpdatableFloatArray} or {@link UpdatableDoubleArray}.
     * The class of desired interface (one of 8 possible classes) must be passed as <code>requiredType</code> argument.
     * For example, if <code>requiredType={@link UpdatableByteArray}.class</code> or <code>requiredType</code>
     * is some class implementing {@link UpdatableByteArray}, the returned array implements {@link UpdatableByteArray}.
     *
     * <p>In the returned array:
     *
     * <ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the length of the passed <code>x</code> arrays;</li>
     * <li>{@link Array#isNew()} method returns <code>false</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method returns <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will call the same methods for all <code>x</code> arrays;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param <T>               the generic type of AlgART array.
     * @param truncateOverflows specifies behavior of typecasting to <code>int</code>,
     *                          <code>short</code>, <code>byte</code>
     *                          and <code>char</code> resulting values (see comments to
     *                          {@link #asFuncArray(boolean, Func, Class, PArray...)} method).
     * @param f                 the mathematical function applied to all passed AlgART arrays.
     * @param requiredType      desired type of the returned array.
     * @param x                 several AlgART arrays; at least one array must be passed.
     * @return an updatable view of the passed arrays, defined by the passed function.
     * @throws NullPointerException     if <code>f</code> or <code>requiredType</code> argument is {@code null}
     *                                  or if one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException if <code>x.length==0</code> (no arrays passed),
     *                                  or if <code>requiredType</code> is an inheritor of {@link MutableArray}
     *                                  (returned array must be immutable),
     *                                  or if <code>requiredType</code> is not one of classes
     *                                  <code>{@link UpdatableBitArray}.class</code> /
     *                                  <code>{@link BitArray}.class</code>,
     *                                  <code>{@link UpdatableCharArray}.class</code> /
     *                                  <code>{@link CharArray}.class</code>,
     *                                  <code>{@link UpdatableByteArray}.class</code> /
     *                                  <code>{@link ByteArray}.class</code>,
     *                                  <code>{@link UpdatableShortArray}.class</code> /
     *                                  <code>{@link ShortArray}.class</code>,
     *                                  <code>{@link UpdatableIntArray}.class</code> /
     *                                  <code>{@link IntArray}.class</code>,
     *                                  <code>{@link UpdatableLongArray}.class</code> /
     *                                  <code>{@link LongArray}.class</code>,
     *                                  <code>{@link UpdatableFloatArray}.class</code> /
     *                                  <code>{@link FloatArray}.class</code>
     *                                  or <code>{@link UpdatableDoubleArray}.class</code> /
     *                                  <code>{@link DoubleArray}.class</code>,
     *                                  or if the number of passed <code>x</code> arrays is insufficient
     *                                  for calculating the passed <code>f</code> function.
     *                                  (In the last situation, <code>IllegalArgumentException</code>
     *                                  may not occur immediately, but <code>IndexOutOfBoundsException</code>
     *                                  may occur instead of while further attempts to read or write elements
     *                                  from / to the returned AlgART array.)
     * @throws SizeMismatchException    if <code>x.length&gt;1</code> and some of passed arrays have different lengths.
     * @see #asFuncArray(boolean, Func, Class, PArray...)
     */
    public static <T extends UpdatablePArray> T asUpdatableFuncArray(
            final boolean truncateOverflows,
            Func.Updatable f, Class<? extends T> requiredType, final UpdatablePArray... x) {
        return ArraysFuncImpl.asUpdatableFuncArray(truncateOverflows, f, requiredType, x);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, Func, UpdatablePArray, PArray...)
     * applyFunc(null, f, result, x)}.
     *
     * @param f      the mathematical function applied to the source AlgART arrays.
     * @param result the destination array.
     * @param x      the source arrays; may be empty;
     *               may include <code>result</code> array to provide "in-place" operations.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException if <code>x.length==0</code> (no arrays passed),
     *                                  or if the number of passed <code>x</code> arrays is insufficient
     *                                  for calculating the passed <code>f</code> function.
     *                                  (In the last situation, <code>IndexOutOfBoundsException</code>
     *                                  may occur instead of while copying data to the result.)
     * @throws SizeMismatchException    if <code>x.length&gt;1</code> and some of passed arrays have different lengths.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(Func f, UpdatablePArray result, PArray... x) {
        applyFunc(null, f, result, x);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)
     * applyFunc(context, true, f, result, x)}.
     *
     * @param context the context of copying; can be {@code null}.
     * @param f       the mathematical function applied to the source AlgART arrays.
     * @param result  the destination array.
     * @param x       the source arrays; may be empty;
     *                may include <code>result</code> array to provide "in-place" operations.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException if <code>x.length==0</code> (no arrays passed),
     *                                  or if the number of passed <code>x</code> arrays is insufficient
     *                                  for calculating the passed <code>f</code> function.
     *                                  (In the last situation, <code>IndexOutOfBoundsException</code>
     *                                  may occur instead of while copying data to the result.)
     * @throws SizeMismatchException    if <code>x.length&gt;1</code> and some of passed arrays have different lengths.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(ArrayContext context, Func f, UpdatablePArray result, PArray... x) {
        applyFunc(context, true, 0, true, f, result, x);
    }

    /**
     * Equivalent to {@link #applyFunc(ArrayContext, boolean, int, boolean, Func, UpdatablePArray, PArray...)
     * applyFunc(context, f, truncateOverflows, 0, true, result, x)}.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows the behavior of typecasting: see
     *                          {@link #asFuncArray(boolean, Func, Class, PArray...)}.
     * @param f                 the mathematical function applied to the source AlgART arrays.
     * @param result            the destination array.
     * @param x                 the source arrays; may be empty;
     *                          may include <code>result</code> array to provide "in-place" operations.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException if the number of passed <code>x</code> arrays is insufficient
     *                                  for calculating the passed <code>f</code> function.
     *                                  (In the last situation, <code>IndexOutOfBoundsException</code>
     *                                  may occur instead of while copying data to the result.)
     * @throws SizeMismatchException    if some of passed arrays (<code>result</code> and some of <code>x</code> arrays)
     *                                  have different lengths.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            Func f,
            UpdatablePArray result,
            PArray... x) {
        applyFunc(context, truncateOverflows, 0, true, f, result, x);
    }

    /**
     * Creates a "lazy" array by <code>lazy = {@link #asFuncArray(boolean, Func, Class, PArray...)
     * asFuncArray(truncateOverflows, f, result.type(), x)}</code> call
     * and copies it into the <code>result</code> argument by
     * <code>{@link #copy(ArrayContext, UpdatableArray, Array, int, boolean)
     * copy(context, result, lazy, numberOfTasks, strictMode)}</code> call.
     *
     * <p>In addition, this method checks, whether all passed arrays have the
     * same length as <code>result</code> one, and throws an exception in another case.
     *
     * <p>If no source arrays are passed (<code>x.length==0</code>), the "lazy" array is created by
     * another way: <code>lazy = {@link #asIndexFuncArray(boolean, Func, Class, long)
     * asIndexFuncArray(truncateOverflows, f, result.type(), result.length())}</code>.
     *
     * @param context           the context of copying; can be {@code null}.
     * @param truncateOverflows the behavior of typecasting: see
     *                          {@link #asFuncArray(boolean, Func, Class, PArray...)}.
     * @param numberOfTasks     the desired number of parallel tasks;
     *                          can be <code>0</code>, then it will be chosen automatically.
     * @param strictMode        if <code>false</code>, optimization is allowed even
     *                          if it can lead to little differences
     *                          between the lazy and copied elements.
     * @param f                 the mathematical function applied to the source AlgART arrays.
     * @param result            the destination array.
     * @param x                 the source arrays; may be empty;
     *                          may include <code>result</code> array to provide "in-place" operations.
     * @throws NullPointerException     if <code>f</code>, <code>result</code>, <code>x</code>
     *                                  or one of <code>x</code> arrays is {@code null}.
     * @throws IllegalArgumentException if the number of passed <code>x</code> arrays is insufficient
     *                                  for calculating the passed <code>f</code> function.
     *                                  (In the last situation, <code>IndexOutOfBoundsException</code>
     *                                  may occur instead of while copying data to the result.)
     * @throws SizeMismatchException    if some of passed arrays
     *                                  (<code>result</code> and some of <code>x</code> arrays)
     *                                  have different lengths.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call.
     */
    public static void applyFunc(
            ArrayContext context,
            boolean truncateOverflows,
            int numberOfTasks,
            boolean strictMode,
            Func f,
            UpdatablePArray result,
            PArray... x) {
        Objects.requireNonNull(result, "Null result argument");
        long len = result.length();
        for (int k = 0; k < x.length; k++) {
            Objects.requireNonNull(x[k], "Null x[" + k + "] argument");
            if (x[k].length() != len) {
                throw new SizeMismatchException("x[" + k + "].length() and result.length() mismatch");
            }
        }
        PArray lazy = x.length == 0 ?
                asIndexFuncArray(truncateOverflows, f, result.type(), result.length()) :
                asFuncArray(truncateOverflows, f, result.type(), x);
        copy(context, result, lazy, numberOfTasks, strictMode);
    }

    /**
     * Returns <code>true</code> if the passed array is not {@code null}
     * <i>functional</i> array, created by this package.
     * More precisely, if returns <code>true</code>
     * if and only if the array is a result of one of the following methods:
     * <ul>
     * <li>{@link #asIndexFuncArray(Func, Class, long)},
     * <li>{@link #asIndexFuncArray(boolean, Func, Class, long)},
     * <li>{@link #asFuncArray(Func, Class, PArray...)},
     * <li>{@link #asFuncArray(boolean, Func, Class, PArray...)},
     * <li>{@link #asUpdatableFuncArray(net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)},
     * <li>{@link #asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)},
     * </ul>
     * or if the array is the {@link Matrix#array() built-in array}
     * of some matrix, created by analogous methods in {@link Matrices} class:
     * <ul>
     * <li><code>asCoordFuncMatrix</code>,
     * <li><code>asFuncMatrix</code>,
     * <li><code>asUpdatableFuncMatrix</code>.
     * </ul>
     * In a case of <code>asFuncArray</code>, <code>asUpdatableFuncArray</code>,
     * <code>asFuncMatrix</code>, <code>asUpdatableFuncMatrix</code> methods,
     * you may get the underlying arrays, passed to those methods in the last argument
     * (or built-in arrays of underlying matrices in a case of {@link Matrices} methods)
     * by <code>{@link #getUnderlyingArrays(Array) getUnderlyingArrays(array)}</code>
     * call, and get the used mathematical function by {@link #getFunc(Array)} call.
     *
     * <p>If the array is an {@link Array#asImmutable() immutable view} of another array, created by
     * {@link #asUpdatableFuncArray(net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)} or
     * {@link #asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)},
     * this method also returns <code>true</code>, and {@link #getFunc(Array)} also allows to get
     * the used mathematical function. However, if the array is a {@link Array#subArray subarray}
     * of some functional array, this method returns <code>false</code>.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array a functional one.
     * @see #isIndexFuncArray(Array)
     */
    public static boolean isFuncArray(Array array) {
        return array instanceof ArraysFuncImpl.FuncArray;
    }

    /**
     * Returns <code>true</code> if the passed array is not {@code null} <i>functional</i> array,
     * created by this package,
     * calculated on the base of array indexes only, not depending on another arrays/matrices.
     * More precisely, it returns <code>true</code>
     * if and only if the array is a result of one of the following calls:
     * <ul>
     * <li>{@link #asIndexFuncArray(Func, Class, long)},
     * <li>{@link #asIndexFuncArray(boolean, Func, Class, long)},
     * <li><code>{@link Matrices#asCoordFuncMatrix(Func, Class, long...)}.{@link Matrix#array() array()}</code>,
     * <li><code>{@link Matrices#asCoordFuncMatrix(boolean, Func, Class, long...)}.{@link
     * Matrix#array() array()}</code>
     * </ul>
     * or by equivalent methods like {@link Matrices#asResized(Matrices.ResizingMethod, Matrix, long...)},
     * based on these calls.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array a functional one, calculated on the base of array indexes only.
     * @see #isFuncArray(Array)
     * @see Matrices#isCoordFuncMatrix(Matrix)
     */
    public static boolean isIndexFuncArray(Array array) {
        return array instanceof ArraysFuncImpl.CoordFuncArray;
    }

    /**
     * Returns the mathematical function, used for creating the passed {@link #isFuncArray functional} array,
     * or throws an exception if this array is not functional.
     * The returned function is the same function that was passed to the method, called to create the passed array:
     * <ul>
     * <li>{@link #asIndexFuncArray(Func, Class, long)},
     * <li>{@link #asIndexFuncArray(boolean, Func, Class, long)},
     * <li>{@link #asFuncArray(Func, Class, PArray...)},
     * <li>{@link #asFuncArray(boolean, Func, Class, PArray...)},
     * <li>{@link #asUpdatableFuncArray(net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)},
     * <li>{@link
     * #asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)},
     * <li>or one of analogous {@link Matrices} methods <code>asCoordFuncMatrix</code>,
     * <code>asFuncMatrix</code>, <code>asUpdatableFuncMatrix</code>.
     * </ul>
     *
     * @param array the functional AlgART array.
     * @return the mathematical function, used for creating it.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is not functional, i.e.
     *                                  if {@link #isFuncArray(Array)} returns <code>false</code> for it.
     * @see #isFuncArray(Array)
     */
    public static Func getFunc(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (!isFuncArray(array)) {
            throw new IllegalArgumentException("The passed argument is not a functional array");
        }
        return ((ArraysFuncImpl.FuncArray) array).f();
    }

    /**
     * Returns the value of <code>truncateOverflows</code> argument of
     * {@link #asFuncArray(boolean, Func, Class, PArray...)} method
     * (or one of its overloaded versions),
     * used for creating the passed {@link #isFuncArray functional} array,
     * or throws an exception if this array is not functional.
     *
     * @param array the functional AlgART array.
     * @return the truncation mode, used for creating it.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is not functional, i.e.
     *                                  if {@link #isFuncArray(Array)} returns <code>false</code> for it.
     * @see #isFuncArray(Array)
     */
    public static boolean getTruncationMode(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (!isFuncArray(array)) {
            throw new IllegalArgumentException("The passed argument is not a functional array");
        }
        return ((ArraysFuncImpl.FuncArray) array).truncateOverflows();
    }

    /**
     * Returns the dimensions of the matrix, coordinates of which are used as arguments of the underlying function
     * in the specified {@link #isIndexFuncArray(Array) index-based functional array},
     * or throws an exception if this array is not a functional array,
     * calculated on the base of indexes.
     * More precisely, if the passed array is created by the methods
     * <ul>
     * <li>{@link #asIndexFuncArray(Func, Class, long)} or
     * <li>{@link #asIndexFuncArray(boolean, Func, Class, long)},
     * </ul>
     * this method returns a Java array with the only element <code>{array.length()}</code>;
     * if the passed array is an {@link Matrix#array() underlying array} of some matrix <code>m</code>,
     * created by the methods
     * <ul>
     * <li>{@link Matrices#asCoordFuncMatrix(Func, Class, long...)} or
     * <li>{@link Matrices#asCoordFuncMatrix(boolean, Func, Class, long...)},
     * </ul>
     * this method returns the matrix dimensions <code>m.{@link Matrix#dimensions() dimensions()}</code>;
     * in other cases this method throws <code>IllegalArgumentException</code>.
     *
     * @param array the index-based functional array.
     * @return the dimensions of the matrix,
     * coordinates of which are used as arguments of the underlying function.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is not index-based functional, i.e.
     *                                  if {@link #isIndexFuncArray(Array)} returns <code>false</code> for it.
     * @see #isIndexFuncArray(Array)
     */
    public static long[] getIndexDimensions(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (!isIndexFuncArray(array)) {
            throw new IllegalArgumentException("The passed argument is not an index-based functional array");
        }
        return ((ArraysFuncImpl.CoordFuncArray) array).dimensions();
    }

    /**
     * Returns an immutable view of the passed sequence of AlgART arrays
     * as their concatenation.
     * More precisely, the returned array has the length, equal to the sum of lengths
     * <i>l</i><sub>0</sub>, <i>l</i><sub>1</sub>, ..., <i>l</i><sub><i>n</i>&minus;1</sub> of all passed arrays,
     * and the element #<i>k</i> of the returned array is the element
     * #<i>k</i>-(<i>l</i><sub>0</sub>+<i>l</i><sub>1</sub>+...+<i>l</i><sub><i>i</i></sub>)
     * in <code>arrays[<i>i</i>]</code>, where <i>i</i> is the last index, for which
     * <i>k</i>&gt;=<i>l</i><sub>0</sub>+<i>l</i><sub>1</sub>+...+<i>l</i><sub><i>i</i></sub>.
     * Query operations on the returned array "read through"
     * to corresponding source arrays.
     *
     * <p>All passed arrays must be {@link UpdatableArray#asUnresizable() unresizable}
     * and have the same {@link Array#elementType() element type};
     * in another case, an exception is thrown.
     *
     * <p>The array, returned by this method, is immutable.
     * Its class implements the same basic interface
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray}, {@link DoubleArray}
     * or {@link ObjectArray}
     * as all passed arrays. The element type of the returned array is the same
     * as the element type of the passed ones.
     *
     * <p>In the returned array:
     *
     * <ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the sum of lengths of the passed arrays;</li>
     * <li>{@link Array#subArray subArray} and {@link Array#subArr subArr}
     * will create new concatenation for the corresponding subsequence of
     * the arrays, passed to this method, or, maybe, will return
     * an {@link Array#asImmutable() immutable view} of a subarray of one the passed arrays,
     * when it is possible (i.e. when all elements of the subarray belong to a single
     * array from the sequence);</li>
     * <li>{@link Array#isNew()} method returns <code>false</code>;</li>
     * <li>{@link Array#isNewReadOnlyView()} method returns <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will call the same methods for the concatenated arrays;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param arrays the sequence of AlgART arrays.
     * @return the concatenation of these arrays.
     * @throws NullPointerException     if <code>arrays</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>arrays.length==0</code> (no arrays passed),
     *                                  of if some of the passed arrays are not unresizable,
     *                                  or if they have different {@link Array#elementType() element types}.
     * @throws TooLargeArrayException   if the sum of lengths of the passed arrays is greater than
     *                                  <code>Long.MAX_VALUE=2<sup>63</sup>&minus;1</code>.
     */
    public static Array asConcatenation(Array... arrays) {
        return ArraysOpImpl.asConcatenation(arrays);
    }

    /**
     * Returns <code>true</code> if the passed array is a <i>concatenation view</i>, i&#46;e&#46;
     * is a result of {@link #asConcatenation(Array...)} method.
     * In this case, you may get the underlying array, passed to those methods in the last argument,
     * by <code>{@link #getUnderlyingArrays(Array) getUnderlyingArrays(array)}</code>
     * call.
     *
     * <p>Please note: if the array is a {@link Array#subArray subarray}
     * of some concatenation view, this method usually returns <code>true</code>,
     * but may return <code>false</code> if the subarray does not intersect
     * bounds between concatenated arrays.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array a concatenation view of other arrays.
     */
    public static boolean isConcatenation(Array array) {
        return array instanceof ArraysOpImpl.ConcatenatedArray;
    }

    /**
     * Returns an immutable view of the passed AlgART array,
     * cyclically shifted to the right by the specified number of elements.
     * More precisely, the returned array has the same length,
     * and the element <code>#k</code> of the returned array is the element <code>#((k-shift) mod length)</code>
     * of the passed array, where <code>a mod b</code> (<code>b&gt;=0</code>) is a "positive remainder":
     * <code>a%b</code> if <code>a&gt;=0</code> or <code>b+a%b</code> if <code>a&lt;0</code>.
     * (If the array <code>length=0</code>, the <code>shift</code> argument is ignored
     * and the returned array contains no elements.)
     *
     * <p>The passed array must be {@link UpdatableArray#asUnresizable() unresizable};
     * in another case, an exception is thrown.
     *
     * <p>The array, returned by this method, is immutable.
     * Its class implements the same basic interface
     * {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray},
     * {@link FloatArray}, {@link DoubleArray}
     * or {@link ObjectArray}
     * as the passed array. The element type of the returned array is the same
     * as the element type of the passed one.
     *
     * <p>In the returned array:
     *
     * <ul>
     * <li>{@link Array#capacity() capacity} and {@link Array#length() length}
     * will be equal to the length of the passed array;</li>
     * <li>{@link Array#subArray subArray} and {@link Array#subArr subArr}
     * may return return an {@link Array#asImmutable() immutable view} of a subarray of the passed array,
     * when it is possible (i.e. when the subarray corresponds to a single
     * subarray of the original array);</li>
     * <li>{@link Array#isNew()} method returns <code>false</code>;</li>
     * <li>{@link Array#loadResources(ArrayContext)}, {@link Array#flushResources(ArrayContext)},
     * {@link Array#flushResources(ArrayContext, boolean)} and {@link Array#freeResources(ArrayContext, boolean)}
     * methods will call the same methods for the passed array;</li>
     * <li>{@link UpdatableArray}, {@link MutableArray}, {@link DirectAccessible}
     * interfaces are not implemented.</li>
     * </ul>
     *
     * @param array the source AlgART array.
     * @param shift the shift (to the right) of the index in the returned view.
     * @return a shifted view of the passed array.
     * @throws NullPointerException     if <code>array</code> argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is not unresizable.
     */
    public static Array asShifted(Array array, long shift) {
        return ArraysOpImpl.asShifted(array, shift);
    }

    /**
     * Returns <code>true</code> if the passed array is not {@code null} <i>shifted</i> array, i&#46;e&#46;
     * is a result of {@link #asShifted(Array, long)} method.
     * In this case, you may get the underlying array, passed to those methods in the last argument,
     * by <code>{@link #getUnderlyingArrays(Array) getUnderlyingArrays(array)}[0]</code>
     * call, and get the used shift by {@link #getShift(Array)} call.
     *
     * <p>Please note: if the array is a {@link Array#subArray subarray}.
     * of some shifted array, this method returns <code>false</code>.
     * <p>Please note: if the array is a {@link Array#subArray subarray}
     * of some shifted array, this method may return both <code>true</code> and <code>false</code>,
     * depending on bounds of the subarray.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array a shifted array.
     */
    public static boolean isShifted(Array array) {
        return array instanceof ArraysOpImpl.ShiftedArray;
    }

    /**
     * Returns the shift, used for creating the passed {@link #isShifted shited} array,
     * or throws an exception if this array is not shifted.
     * The returned shift is equal to the corresponding argument that was passed to
     * {@link #asShifted(Array, long)} method, which was called to create the passed array.
     *
     * @param array the functional AlgART array.
     * @return the mathematical function, used for creating it.
     * @throws NullPointerException     if the argument is {@code null}.
     * @throws IllegalArgumentException if the passed array is not shifted, i.e.
     *                                  if {@link #isShifted(Array)} returns <code>false</code> for it.
     * @see #isShifted(Array)
     */
    public static long getShift(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (!isShifted(array)) {
            throw new IllegalArgumentException("The passed argument is not a shifted array");
        }
        return ((ArraysOpImpl.ShiftedArray) array).shift();
    }

    /**
     * Equivalent to <code>{@link #copy(ArrayContext, UpdatableArray, Array)
     * copy}(ArrayContext.{@link ArrayContext#DEFAULT_SINGLE_THREAD DEFAULT_SINGLE_THREAD}, dest, src)</code>.
     *
     * @param dest the destination array.
     * @param src  the source array.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     */
    public static CopyStatus copy(UpdatableArray dest, Array src) {
        return copy(ArrayContext.DEFAULT_SINGLE_THREAD, dest, src);
    }

    /**
     * Copies <code>min(dest.{@link Array#length() length()}, src.{@link Array#length() length()})</code> elements
     * of <code>src</code> array, starting from index 0, to dest array, starting from index 0.
     *
     * <p>This method works alike {@link UpdatableArray#copy(Array)} with two important differences.
     *
     * <ol>
     * <li>This method performs copying in several threads. It may be very important on multiprocessor
     * or multicore computers for the case, when the source array is a "lazy" view of another arrays,
     * that is when reading data from it means performing complex algorithm calculating the data.
     * The good example is an array returned by {@link #asFuncArray(Func, Class, PArray...)},
     * when the result of this method is calculated on the base of tens or hundreds source arrays
     * passed in the last argument.
     * In this situation, performing this method really means actual execution of the algorithm,
     * and several threads allow to use all system processors for this purpose.
     * <br>
     * Multithreading copying is performed by a thread pool: <code>java.util.concurrent.ExecutorService</code>.
     * The full copying procedure is split into several tasks, where each task
     * copies some set of regions of <code>src</code> array to the corresponding regions of <code>dest</code> array.
     * All tasks are submitted to the thread pool, and then this method
     * waits until all tasks will be completed.
     * The thread pool, performing the multithreading copying, and the number of copying tasks
     * are gotten by the methods of
     * <code>context.{@link ArrayContext#getThreadPoolFactory() getThreadPoolFactory()}</code> object;
     * if <code>context</code> argument is {@code null},
     * the {@link DefaultThreadPoolFactory} instance is created and used.
     * <br>
     * Note: if this method may suppose that the passed array is not "lazy",
     * namely, if <code>!src.{@link Array#isLazy() isLazy()}</code>, it uses only 1 task
     * and ignores result of {@link ThreadPoolFactory#recommendedNumberOfTasks(Array)} method.
     * In this situation, it's very probable that copying is simple transferring data
     * between memory and/or disk; such operations are not optimized usually by
     * multithreading technique.
     * <br>
     * Note: if the number of parallel tasks is 1, this method performs copying in the current thread
     * and does not use the thread pool at all.
     * <br>&nbsp;
     * </li>
     *
     * <li>If the <code>context</code> argument is not {@code null},
     * this method periodically calls its {@link ArrayContext#updateProgress updateProgress} and
     * {@link ArrayContext#checkInterruption checkInterruption} methods.
     * More precisely, this method periodically calls {@link ArrayContext#checkInterruption()} method,
     * catching any possible <code>RuntimeException</code> thrown by it,
     * and, immediately after this, calls {@link ArrayContext#updateProgress(net.algart.arrays.ArrayContext.Event)}
     * with correctly filled {@link ArrayContext.Event} instance.
     * If {@link ArrayContext#checkInterruption()} has thrown some <code>RuntimeException</code>,
     * all running threads are interrupted as soon as possible,
     * and, when they will be finished, the same exception is thrown by this method.
     * This technique allows to show the progress of execution to the end user
     * and to interrupt the copying by some UI command if this method works for too long time.
     * </li>
     * </ol>
     *
     * <p>Usually this method performs copying by a sequence of calls
     * <code>dest.{@link Array#subArr subArr}(p, len).{@link UpdatableArray#copy(Array)
     * copy}(array.{@link Array#subArr subArr}(p, len))</code>,
     * where <code>p</code> is the position inside arrays and <code>len</code> is the length
     * of the copied portion, usually several tens of thousands elements.
     * But in some special cases this method uses more complex algorithms of copying.
     *
     * <p>In addition to calling {@link ArrayContext#checkInterruption()},
     * this method also interrupts all running threads, if the current thread,
     * that calls this method,
     * is interrupted by the standard <code>Thread.interrupt()</code> call.
     * In this (and only this) case, this method throws <code>java.io.IOError</code>.
     * Usually, you should avoid interrupting the threads, processing AlgART arrays,
     * via <code>Thread.interrupt()</code> technique: see the package description
     * about runtime exceptions issue.
     *
     * <p>Alike {@link UpdatableArray#copy(Array)} method,
     * some elements may be copied incorrectly if this array and <code>src</code>
     * are views of the same data, and the swapped data areas overlap.
     * Unlike {@link UpdatableArray#copy(Array)}, this problem occurs even for arrays,
     * created by {@link SimpleMemoryModel}.
     * But if the copied areas of the underlying data are <i>the same</i>
     * (for example, if <code>src</code> if some view of this array generated by
     * {@link #asIndexFuncArray asFuncArray}, but not {@link #asShifted asShifted} method),
     * this method works correctly: any elements will be read before they will be updated.
     *
     * <p>This method works only if the element types of this and <code>src</code> arrays
     * (returned by {@link Array#elementType()}) are the same, or (for non-primitive elements)
     * if this element type is a superclass of the source element type.
     *
     * <p>For non-primitive element type ({@link ObjectArray}, {@link UpdatableObjectArray},
     * {@link MutableObjectArray} subinterfaces), this method may copy only references to elements,
     * but also may copy the content of elements: it depends on implementation.
     *
     * @param context the context of copying; can be {@code null}.
     * @param dest    the destination array.
     * @param src     the source array.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call
     *                                  (this technique may be not supported in some cases).
     * @see #copy(ArrayContext, UpdatableArray, Array, int)
     * @see #copy(ArrayContext, UpdatableArray, Array, int, boolean)
     * @see Arrays.ParallelExecutor
     * @see Arrays.Copier
     */
    public static CopyStatus copy(ArrayContext context, UpdatableArray dest, Array src) {
        return copy(context, dest, src, 0);
    }

    /**
     * This method is an analog of {@link #copy(ArrayContext, UpdatableArray, Array)},
     * allowing to specify the number of parallel tasks.
     * More precisely, if <code>numberOfTasks</code> argument is positive,
     * then the number of tasks submitted to the thread pool will be equal to this argument,
     * regardless of the result of {@link ThreadPoolFactory#recommendedNumberOfTasks(Array)} method
     * and of the {@link Array#isLazy() "lazy"} or {@link Array#isNew() "new"} status of the source array.
     * If <code>numberOfTasks==0</code>, this method is strictly equivalent to
     * {@link #copy(ArrayContext, UpdatableArray, Array)}.
     *
     * <p>This method can be convenient, for example, if you are sure that multithreading
     * is not useful in a concrete algorithm and the number of tasks should be <code>1</code>.
     *
     * @param context       the context of copying; can be {@code null}.
     * @param dest          the destination array.
     * @param src           the source array.
     * @param numberOfTasks the desired number of parallel tasks;
     *                      can be <code>0</code>, then it will be chosen automatically.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match,
     *                                  or if the <code>numberOfThreads</code> argument is negative.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call
     *                                  (this technique may be not supported in some cases).
     * @see #copy(ArrayContext, UpdatableArray, Array)
     * @see #copy(ArrayContext, UpdatableArray, Array, int, boolean)
     * @see Arrays.ParallelExecutor
     * @see Arrays.Copier
     */
    public static CopyStatus copy(ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks) {
        return ArraysOpImpl.copy(context, dest, src, numberOfTasks, true, false);
    }

    /**
     * This method is an analog of {@link #copy(ArrayContext, UpdatableArray, Array)} and
     * {@link #copy(ArrayContext, UpdatableArray, Array, int)}
     * providing a special "non-strict" copying mode.
     *
     * <p>Namely, if <code>strictMode</code> argument is <code>false</code>,
     * then in some cases this method may copy elements with little errors (distortions).
     * The probability and numeric value of errors is very low.
     * The only reason of possible errors is limited precision of floating-point calculations.</p>
     *
     * <p>The purpose of the non-strict mode is optimization: copying of some "lazy" arrays
     * is performed much faster in this mode.
     * For example, the result of
     * {@link Matrices#asResized Matrices.asResized}
     * (its {@link Matrix#array() built-in array}) is copied faster in 10 and more times,
     * when the source (non-resized) matrix is not created by {@link SimpleMemoryModel}.
     *
     * <p>If <code>strictMode=true</code>, this method is equivalent to
     * {@link #copy(ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks)}.
     *
     * @param context       the context of copying; can be {@code null}.
     * @param dest          the destination array.
     * @param src           the source array.
     * @param numberOfTasks the desired number of parallel tasks;
     *                      can be <code>0</code>, then it will be chosen automatically.
     * @param strictMode    if <code>false</code>, optimization is allowed even if it can lead to little differences
     *                      between the source and copied elements.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match,
     *                                  or if the <code>numberOfThreads</code> argument is negative.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call
     *                                  (this technique may be not supported in some cases).
     * @see #copy(ArrayContext, UpdatableArray, Array)
     */
    public static CopyStatus copy(
            ArrayContext context,
            UpdatableArray dest,
            Array src,
            int numberOfTasks,
            boolean strictMode) {
        return ArraysOpImpl.copy(context, dest, src, numberOfTasks, strictMode, false);
    }

    /**
     * This method is an analog of {@link #copy(ArrayContext, UpdatableArray, Array)}
     * allowing to know whether the destination array was really modified while copying.
     * Namely, this method performs the same things as
     * {@link #copy(ArrayContext context, UpdatableArray dest, Array src)}
     * and returns the status, where {@link Arrays.ComparingCopyStatus#changed()} method
     * returns <code>true</code> if and only if at least one element of the <code>dest</code>
     * array was changed as a result of copying from <code>src</code> array,
     * in terms of the {@link Array#equals(Object)} method.
     *
     * @param context the context of copying; can be {@code null}.
     * @param dest    the destination array.
     * @param src     the source array.
     * @return the information about copying.
     * @throws NullPointerException     if <code>src</code> or <code>dest</code> argument is {@code null}.
     * @throws IllegalArgumentException if the source and destination element types do not match.
     * @throws java.io.IOError          if the current thread is interrupted by the standard
     *                                  <code>Thread.interrupt()</code> call
     *                                  (this technique may be not supported in some cases).
     */
    public static ComparingCopyStatus compareAndCopy(ArrayContext context, UpdatableArray dest, Array src) {
        return (ComparingCopyStatus) ArraysOpImpl.copy(context, dest, src, 0, true, true);
    }

    /**
     * Equivalent to {@link #clone(MemoryModel, PArray) clone(Arrays.SMM, array)}.
     *
     * <p>Note: this operation can optimize access to this array in many times, if it is lazy-calculated
     * and not too large (can be placed in available Java memory).
     * It performs cloning with maximal speed via multithreading optimization. We recommend to call
     * it after lazy calculations.</p>
     *
     * @param array the array to be cloned.
     * @return an exact updatable clone of the passed matrix.
     * @throws NullPointerException if the argument is {@code null}.
     */
    public static UpdatablePArray clone(PArray array) {
        return clone(Arrays.SMM, array);
    }

    /**
     * Returns an exact updatable clone of the given array, created in the given memory model.
     * Equivalent to the following operators:
     * <pre>
     *     final UpdatablePArray result = (UpdatablePArray) memoryModel.{@link MemoryModel#newUnresizableArray(Array)
     *     newUnresizableArray}(array);
     *     {@link Arrays#copy(ArrayContext, UpdatableArray, Array)
     *     Arrays.copy}(null, result, matrix); // - maximally fast multithreading copying
     *     (return result)
     * </pre>
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @param array       the array to be cloned.
     * @return an exact updatable clone of the passed matrix.
     * @throws NullPointerException if one of the arguments is {@code null}.
     */
    public static UpdatablePArray clone(MemoryModel memoryModel, PArray array) {
        return (UpdatablePArray) clone(memoryModel, (Array) array);
    }

    /**
     * Returns an exact updatable clone of the given array, created in the given memory model.
     * Equivalent to the following operators:
     * <pre>
     *     final UpdatableArray result = memoryModel.{@link MemoryModel#newUnresizableArray(Array)
     *     newUnresizableArray}(array);
     *     {@link Arrays#copy(ArrayContext, UpdatableArray, Array)
     *     Arrays.copy}(null, result, matrix); // - maximally fast multithreading copying
     *     (return result)
     * </pre>
     *
     * @param memoryModel the memory model, used for allocation a new copy of this array.
     * @param array       the array to be cloned.
     * @return an exact updatable clone of the passed array.
     * @throws NullPointerException if one of the arguments is {@code null}.
     */
    public static UpdatableArray clone(MemoryModel memoryModel, Array array) {
        Objects.requireNonNull(memoryModel, "Null memory model");
        Objects.requireNonNull(array, "Null array");
        final UpdatableArray result = memoryModel.newUnresizableArray(array);
        Arrays.copy(null, result, array);
        // - maximally fast multithreading copying
        return result;
    }

    /**
     * Equivalent to {@link #rangeOf(ArrayContext, PArray, MinMaxInfo) rangeOf(null, array, minMaxInfo)}.
     *
     * @param array      some primitive array.
     * @param minMaxInfo the object where to store the indexes and values of found minimum and maximum;
     *                   can be {@code null} if you need only the values of the minimum and maximum.
     * @return the values of the found minimal and maximal elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static Range rangeOf(PArray array, MinMaxInfo minMaxInfo) {
        return rangeOf(null, array, minMaxInfo);
    }

    /**
     * Returns a {@link Range#valueOf(double, double) Range.valueOf(min, max)},
     * where <code>min</code> is equal to the minimal array element
     * and <code>min</code> is equal to the maximal array element.
     * The elements are extracted by <code>array.{@link PArray#getDouble(long)
     * getDouble(k)}</code> method (or by an equivalent technique).
     *
     * <p>Please remember that {@link ByteArray} and {@link ShortArray} are interpreted as unsigned.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * <p>If the <code>minMaxInfo</code> argument is not {@code null},
     * the indexes of the found minimum / maximum, as well as the returned range, are stored in this object,
     * and this object becomes {@link Arrays.MinMaxInfo#isInitialized() initialized}.
     * In the future, this information may be retrieved by the corresponding methods of {@link MinMaxInfo} class.
     * If there are several elements equal to the minimum,
     * the index of minimum, stored in this object, will contain an index of the <b>first</b> such element.
     * If there are several elements equal to the maximum,
     * the index of maximum, stored in this object, may contain an index of the <b>first</b> such element.
     *
     * <p>If the passed array is empty (its length is 0), the method returns <code>0..0</code> range
     * ({@link Range#valueOf(double, double) Range.valueOf(0.0, 0.0)}). In this case,
     * if the <code>minMaxInfo</code> argument is not {@code null},
     * the indexes of the minimum / maximum, stored in this object, will be equal to <code>-1</code>.
     *
     * <p>If the passed array is {@link PFloatingArray}, please note that <code>NaN</code> values are
     * exlcuded from comparison: this method finds minimums and maximum among all elements, excepting
     * <code>NaN</code>. If all elements of the array are <code>NaN</code>, it is a special case:
     * this method returns <code>Range.valueOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)</code>.
     * You can detect this situation with help of {@link MinMaxInfo#allNaN() allNaN()} method of
     * <code>minMaxInfo</code> argument.
     *
     * <p>Please note that the {@link MinMaxInfo} class is internally synchronized and, so,
     * <b>thread-safe</b>.
     * It can be useful if you call this method in a separate thread and need to use
     * the calculated information after finishing this method.
     *
     * @param context    the context of calculations; can be {@code null}.
     * @param array      some primitive array.
     * @param minMaxInfo the object where to store the indexes and values of found minimum and maximum;
     *                   can be {@code null} if you need only the values of the minimum and maximum.
     * @return the values of the found minimal and maximal elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static Range rangeOf(ArrayContext context, PArray array, MinMaxInfo minMaxInfo) {
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array)) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
        }
        if (minMaxInfo == null) {
            minMaxInfo = new Arrays.MinMaxInfo();
        }
        ArraysOpImpl.RangeCalculator rangeCalculator = new ArraysOpImpl.RangeCalculator(context, array, minMaxInfo);
        rangeCalculator.process();
        return minMaxInfo.range();
    }

    /**
     * Equivalent to {@link #rangeOf(ArrayContext, PArray, MinMaxInfo) rangeOf(null, array, null)}.
     *
     * @param array some primitive array.
     * @return the values of the found minimal and maximal elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static Range rangeOf(PArray array) {
        return rangeOf(null, array, null);
    }

    /**
     * Equivalent to {@link #rangeOf(ArrayContext, PArray, MinMaxInfo) rangeOf(context, array, null)}.
     *
     * @param context the context of calculations; can be {@code null}.
     * @param array   some primitive array.
     * @return the values of the found minimal and maximal elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static Range rangeOf(ArrayContext context, PArray array) {
        return rangeOf(context, array, null);
    }

    /**
     * Equivalent to {@link #sumOf(ArrayContext, PArray) sumOf(null, array)}.
     *
     * @param array some primitive array.
     * @return the sum of all array elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static double sumOf(PArray array) {
        return sumOf(null, array);
    }

    /**
     * Returns the sum of all array elements: the results of <code>array.{@link PArray#getDouble(long)
     * getDouble(k)}</code> for all <code>k==0,1,...,array.length()-1</code>.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * However, unlike that method, this method <i>is always performed in a single thread
     * and does not use multithreading for optimization</i>.
     * It is necessary to provide absolutely identical results while different calls
     * (because the floating-point sum can depend on the order of summing).
     *
     * <p>Please remember that <code>ByteArray</code> and <code>ShortArray</code> are interpreted as unsigned.
     *
     * @param context the context of calculations; can be {@code null}.
     * @param array   some primitive array.
     * @return the sum of all array elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static double sumOf(ArrayContext context, PArray array) {
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array)) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
        }
        ArraysOpImpl.Summator summator = new ArraysOpImpl.Summator(context, array);
        summator.process();
        return summator.result();
    }

    /**
     * Equivalent to {@link #preciseSumOf(ArrayContext, PFixedArray, boolean)
     * preciseSumOf(null, array, checkOverflow)}.
     *
     * @param array         some primitive array, excluding floating point arrays.
     * @param checkOverflow whether overflow should lead to <code>ArithmeticException</code>
     * @return the sum of all array elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws ArithmeticException  if <code>checkOverflow</code> is <code>true</code> and the result
     *                              or some partial sum cannot be represented by <code>long</code> value.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static long preciseSumOf(PFixedArray array, boolean checkOverflow)
            throws ArithmeticException {
        return preciseSumOf(null, array, checkOverflow);
    }

    /**
     * Returns the integer sum of all array elements: the results of <code>array.{@link PFixedArray#getLong(long)
     * getLong(k)}</code> for all <code>k==0,1,...,array.length()-1</code>.
     *
     * <p>The result is the same as the sum calculated by the following loop:<pre>
     * long sum = 0;
     * for (long k = 0; k &lt; array.length(); k++)
     * &#32;   sum += array.getLong(k);
     * </pre>
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * <p>In a case of overflow (when 64 bits are not enough to represent the result or some partial sum),
     * the behavior depends on <code>checkOverflow</code> argument. If it is <code>false</code>,
     * the result will be incorrect: it will contain 64 lowest bits of the precise sum.
     * If <code>checkOverflow</code> is <code>true</code>, the <code>ArithmeticException</code> will be thrown;
     * in this situation, if the exception is not thrown, you can be sure that this method
     * returns the correct result.
     *
     * <p>Please remember that <code>ByteArray</code> and <code>ShortArray</code> are interpreted as unsigned.
     *
     * @param context       the context of calculations; can be {@code null}.
     * @param array         some primitive array, excluding floating point arrays.
     * @param checkOverflow whether overflow should lead to <code>ArithmeticException</code>
     * @return the sum of all array elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws ArithmeticException  if <code>checkOverflow</code> is <code>true</code> and the result
     *                              or some partial sum cannot be represented by <code>long</code> value.
     */
    public static long preciseSumOf(ArrayContext context, PFixedArray array, boolean checkOverflow)
            throws ArithmeticException {
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array)) {
            array = (PFixedArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
        }
        ArraysOpImpl.PreciseSummator summator = new ArraysOpImpl.PreciseSummator(context, array, checkOverflow);
        summator.process();
        return summator.result();
    }

    /**
     * Equivalent to <code>{@link #preciseSumOf(ArrayContext, PFixedArray, boolean)
     * preciseSumOf(null, array, false)}</code>.
     *
     * @param array some primitive array, excluding floating point arrays.
     * @return the sum of all array elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static long preciseSumOf(PFixedArray array) {
        return preciseSumOf(null, array, false);
    }

    /**
     * Equivalent to <code>{@link #preciseSumOf(ArrayContext, PFixedArray, boolean)
     * preciseSumOf(context, array, false)}</code>.
     *
     * @param context the context of calculations; can be {@code null}.
     * @param array   some primitive array, excluding floating point arrays.
     * @return the sum of all array elements.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static long preciseSumOf(ArrayContext context, PFixedArray array) {
        return preciseSumOf(context, array, false);
    }

    /**
     * Returns the number of bits set to <code>true</code> in this array.
     * Equivalent to <code>{@link #preciseSumOf(ArrayContext, PFixedArray)
     * preciseSumOf(context, bitArray)}</code>.
     *
     * @param context  the context of calculations; can be {@code null}.
     * @param bitArray the bit array.
     * @return the number of bits set to <code>true</code> in this array.
     * @throws NullPointerException if <code>bitArray</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static long cardinality(ArrayContext context, BitArray bitArray) {
        return preciseSumOf(context, bitArray, false);
    }

    /**
     * Returns the number of bits set to <code>true</code> in this array.
     * Equivalent to <code>{@link #preciseSumOf(PFixedArray)
     * preciseSumOf(bitArray)}</code>.
     *
     * @param bitArray the bit array.
     * @return the number of bits set to <code>true</code> in this array.
     * @throws NullPointerException if <code>bitArray</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static long cardinality(BitArray bitArray) {
        return preciseSumOf(null, bitArray, false);
    }

    /**
     * Equivalent to {@link #histogramOf(ArrayContext, PArray, long[], double, double)
     * histogramOf(null, array, histogram, from, to)}.
     *
     * @param array     some primitive array.
     * @param histogram the histogram to be incremented: <code>histogram.length</code> columns
     *                  between <code>from</code> and <code>to</code>.
     * @param from      the low boundary for the counted values, inclusive.
     * @param to        the high boundary for the counted values, exclusive.
     * @return whether all elements of the <code>array</code> are inside the range <code>from..to</code>.
     * @throws NullPointerException if <code>array</code> or <code>histogram</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static boolean histogramOf(PArray array, long[] histogram, double from, double to) {
        return histogramOf(null, array, histogram, from, to);
    }

    /**
     * Increments the histogram of frequency of values in the specified array.
     * The histogram corresponds to values in the range <code>from&nbsp;&lt;=&nbsp;v&nbsp;&lt;&nbsp;to</code>.
     *
     * <p>More precisely, if <code>from&nbsp;&lt;&nbsp;to</code>, then for every element <code>#k</code> of the
     * passed AlgART array this method:
     * <ol>
     * <li>gets its real value <code>v = array.{@link PArray#getDouble(long) getDouble}(k)</code>;</li>
     * <li>calculates the index of the column in the histogram:
     * <code>m = (int)StrictMath.floor((v - from) * multiplier)</code>,
     * where <code>multiplier = histogram.length / (to - from)</code>;</li>
     * <li>if this index lies in the range <code>0 &lt;= m &lt; histogram.length</code>,
     * increments the corresponding element of the passed <code>histogram</code> Java array:
     * <code>histogram[m]++</code></li>
     * </ol>
     *
     * <p>The result of this method will be <code>true</code> if the index <code>m</code> at the step 3 always fulfilled
     * the condition <code>0&nbsp;&lt;=&nbsp;m&nbsp;&lt;&nbsp;histogram.length</code>, or <code>false</code>
     * if this index was out of range at least for one element.
     * If <code>from&nbsp;&gt;=&nbsp;to</code>, this method does nothing and returns <code>false</code>.
     * If the passed array is empty (<code>array.{@link Array#length() length()}==0</code>), this method returns
     * <code>true</code> if <code>from&nbsp;&lt;&nbsp;to</code> and <code>false</code>
     * if <code>from&nbsp;&gt;=&nbsp;to</code>.
     *
     * <p>Please draw attention to the formula at the step 2. It differs from the more obvious formula
     * <code>(int)((v-from)/(to-from)*histogram.length)</code>
     * and theoretically can lead to little other results.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * <p>Please remember that <code>ByteArray</code> and <code>ShortArray</code> are interpreted as unsigned.
     *
     * @param context   the context of calculations; can be {@code null}.
     * @param array     some primitive array.
     * @param histogram the histogram to be incremented: <code>histogram.length</code> columns
     *                  between <code>from</code> and <code>to</code>.
     * @param from      the low boundary for the counted values, inclusive.
     * @param to        the high boundary for the counted values, exclusive.
     * @return whether all elements of the <code>array</code> are inside the range <code>from..to</code>.
     * @throws NullPointerException if <code>array</code> or <code>histogram</code> argument is {@code null}.
     * @throws java.io.IOError      if the current thread is interrupted by the standard
     *                              <code>Thread.interrupt()</code> call
     *                              (this technique may be not supported in some cases).
     */
    public static boolean histogramOf(ArrayContext context, PArray array, long[] histogram, double from, double to) {
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array)) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
        }
        Objects.requireNonNull(histogram, "Null histogram argument");
        if (histogram.length == 0) {
            throw new IllegalArgumentException("Empty histogram argument (histogram.length=0)");
        }
        ArraysOpImpl.HistogramCalculator histogramCalculator = new ArraysOpImpl.HistogramCalculator(
                context, array, histogram, from, to);
        histogramCalculator.process();
        return histogramCalculator.allInside;
    }

    /*Repeat() (Greater|Less) ==> $1OrEqual */

    /**
     * Equivalent to {@link #packBitsGreater(ArrayContext, UpdatableBitArray, PArray, double)
     * packBitsGreater}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, bits, array, threshold).
     *
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void packBitsGreater(
            UpdatableBitArray bits, PArray array, double threshold) {
        packBitsGreater(ArrayContext.DEFAULT_SINGLE_THREAD, bits, array, threshold);
    }

    /**
     * Sets every bit <code>#k</code> of the passed updatable bit array to the boolean value
     * <code>array.{@link PArray#getDouble(long) getDouble}(k) &gt;= threshold</code>.
     * Works much faster than the simple loop of such checks.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * @param context   the context of calculations; can be {@code null}.
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #packBitsLess(ArrayContext, UpdatableBitArray, PArray, double)
     */
    public static void packBitsGreater(
            ArrayContext context, UpdatableBitArray bits, PArray array,
            double threshold) {
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (UpdatableBitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.BitsGreaterPacker packer = new ArraysOpImpl.BitsGreaterPacker(context,
                bits, array, threshold);
        packer.process();
    }

    /**
     * Equivalent to {@link #packBitsLess(ArrayContext, UpdatableBitArray, PArray, double)
     * packBitsLess}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, bits, array, threshold).
     *
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void packBitsLess(
            UpdatableBitArray bits, PArray array, double threshold) {
        packBitsLess(ArrayContext.DEFAULT_SINGLE_THREAD, bits, array, threshold);
    }

    /**
     * Sets every bit <code>#k</code> of the passed updatable bit array to the boolean value
     * <code>array.{@link PArray#getDouble(long) getDouble}(k) &lt;= threshold</code>.
     * Works much faster than the simple loop of such checks.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * @param context   the context of calculations; can be {@code null}.
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #packBitsGreater(ArrayContext, UpdatableBitArray, PArray, double)
     */
    public static void packBitsLess(
            ArrayContext context, UpdatableBitArray bits, PArray array,
            double threshold) {
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (UpdatableBitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.BitsLessPacker packer = new ArraysOpImpl.BitsLessPacker(context,
                bits, array, threshold);
        packer.process();
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to {@link #packBitsGreaterOrEqual(ArrayContext, UpdatableBitArray, PArray, double)
     * packBitsGreaterOrEqual}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, bits, array, threshold).
     *
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void packBitsGreaterOrEqual(
            UpdatableBitArray bits, PArray array, double threshold) {
        packBitsGreaterOrEqual(ArrayContext.DEFAULT_SINGLE_THREAD, bits, array, threshold);
    }

    /**
     * Sets every bit <code>#k</code> of the passed updatable bit array to the boolean value
     * <code>array.{@link PArray#getDouble(long) getDouble}(k) &gt;= threshold</code>.
     * Works much faster than the simple loop of such checks.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * @param context   the context of calculations; can be {@code null}.
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #packBitsLessOrEqual(ArrayContext, UpdatableBitArray, PArray, double)
     */
    public static void packBitsGreaterOrEqual(
            ArrayContext context, UpdatableBitArray bits, PArray array,
            double threshold) {
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (UpdatableBitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.BitsGreaterOrEqualPacker packer = new ArraysOpImpl.BitsGreaterOrEqualPacker(context,
                bits, array, threshold);
        packer.process();
    }

    /**
     * Equivalent to {@link #packBitsLessOrEqual(ArrayContext, UpdatableBitArray, PArray, double)
     * packBitsLessOrEqual}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, bits, array, threshold).
     *
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void packBitsLessOrEqual(
            UpdatableBitArray bits, PArray array, double threshold) {
        packBitsLessOrEqual(ArrayContext.DEFAULT_SINGLE_THREAD, bits, array, threshold);
    }

    /**
     * Sets every bit <code>#k</code> of the passed updatable bit array to the boolean value
     * <code>array.{@link PArray#getDouble(long) getDouble}(k) &lt;= threshold</code>.
     * Works much faster than the simple loop of such checks.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * @param context   the context of calculations; can be {@code null}.
     * @param bits      the bit array that will contain results of comparison of the passed array with the threshold.
     * @param array     some primitive array that should be compared with the threshold.
     * @param threshold the threshold that will be compared with all elements of <code>array</code>.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #packBitsGreaterOrEqual(ArrayContext, UpdatableBitArray, PArray, double)
     */
    public static void packBitsLessOrEqual(
            ArrayContext context, UpdatableBitArray bits, PArray array,
            double threshold) {
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (PArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (UpdatableBitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.BitsLessOrEqualPacker packer = new ArraysOpImpl.BitsLessOrEqualPacker(context,
                bits, array, threshold);
        packer.process();
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to <code>{@link #unpackBits(ArrayContext, UpdatablePArray, BitArray, double, double)
     * unpackBits}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, array, bits, filler0, filler1)</code>.
     *
     * @param array   some primitive array.
     * @param bits    the bit array that should be "unpacked" to <code>array</code>.
     * @param filler0 the value that will be set in <code>array</code> for zero bits.
     * @param filler1 the value that will be set in <code>array</code> for unit bits.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void unpackBits(UpdatablePArray array, BitArray bits, double filler0, double filler1) {
        unpackBits(ArrayContext.DEFAULT_SINGLE_THREAD, array, bits, filler0, filler1);
    }

    /**
     * Sets every element <code>#k</code> of the passed updatable array to <code>filler0</code> value,
     * if the corresponding element <code>#k</code> of <code>bits</code> array is <code>false</code> (0),
     * or to <code>filler1</code>, if this bit is <code>true</code> (1).
     * Equivalent to the loop of the operators
     *
     * <pre>
     * array.{@link UpdatablePArray#setDouble(long, double) setDouble}(k, bits.{@link BitArray#getBit(long)
     * getBit}(k) ? filler1 : filler0)
     * </pre>
     *
     * <p>for all indexes <code>k</code>, but works much faster than the simple loop.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * <p>This method is also equivalent to the following code:</p>
     *
     * <pre>
     * Class et = array.elementType();
     * if (et == byte.class || et == short.class || et == int.class || et == char.class) {
     * &#32;   filler0 = (int)filler0;
     * &#32;   filler1 = (int)filler1;
     * }
     * {@link #applyFunc(ArrayContext, boolean, Func, UpdatablePArray, PArray...)
     * Arrays.applyFunc}(context, false,
     * &#32;   {@link SelectConstantFunc#getInstance SelectConstantFunc.getInstance}(filler0, filler1), array, bits);
     * </pre>
     *
     * <p>(Additional casting to <code>int</code> is necessary here due to different type cast rules in
     * {@link UpdatablePArray#setDouble(long, double)} and
     * {@link #asFuncArray(boolean, Func, Class, PArray...) asFuncArray} methods.)
     *
     * @param context the context of calculations; can be {@code null}.
     * @param array   some primitive array.
     * @param bits    the bit array that should be "unpacked" to <code>array</code>.
     * @param filler0 the value that will be set in <code>array</code> for zero bits.
     * @param filler1 the value that will be set in <code>array</code> for unit bits.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #unpackUnitBits(ArrayContext, UpdatablePArray, BitArray, double)
     * @see #unpackZeroBits(ArrayContext, UpdatablePArray, BitArray, double)
     */
    public static void unpackBits(
            ArrayContext context, UpdatablePArray array, BitArray bits,
            double filler0, double filler1) {
        // Obsolete (little slower) version:
        // Class<?> et = array.elementType();
        // if (et == byte.class || et == short.class || et == int.class || et == char.class) {
        //     filler0 = (int) filler0;
        //     filler1 = (int) filler1;
        // }
        // applyFunc(context, false, SelectConstantFunc.getInstance(filler0, filler1), array, bits);
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (UpdatablePArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (BitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.BothBitsUnpacker unpacker = new ArraysOpImpl.BothBitsUnpacker(
                context, array, bits, filler0, filler1);
        unpacker.process();
    }

    /**
     * Equivalent to <code>{@link #unpackUnitBits(ArrayContext, UpdatablePArray, BitArray, double)
     * unpackUnitBits}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, array, bits, filler1)</code>.
     *
     * @param array   some primitive array.
     * @param bits    the bit array, the unit elements of which should be "unpacked" to <code>array</code>.
     * @param filler1 the value that will be set in <code>array</code> for unit bits.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void unpackUnitBits(UpdatablePArray array, BitArray bits, double filler1) {
        unpackUnitBits(ArrayContext.DEFAULT_SINGLE_THREAD, array, bits, filler1);
    }

    /**
     * Sets every element <code>#k</code> of the passed updatable array to <code>filler1</code> value,
     * if the corresponding element <code>#k</code> of <code>bits</code> array is <code>true</code> (1),
     * or doesn't change it, if this bit is <code>false</code> (0).
     * Equivalent to the loop of the operators
     *
     * <pre>
     * if (bits.{@link BitArray#getBit(long) getBit}(k))
     * &#32;   array.{@link UpdatablePArray#setDouble(long, double) setDouble}(k, filler1)
     * </pre>
     *
     * <p>for all indexes <code>k</code>, but works much faster than the simple loop.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * @param context the context of calculations; can be {@code null}.
     * @param array   some primitive array.
     * @param bits    the bit array, the unit elements of which should be "unpacked" to <code>array</code>.
     * @param filler1 the value that will be set in <code>array</code> for unit bits.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #unpackBits(ArrayContext, UpdatablePArray, BitArray, double, double)
     * @see #unpackZeroBits(ArrayContext, UpdatablePArray, BitArray, double)
     */
    public static void unpackUnitBits(ArrayContext context, UpdatablePArray array, BitArray bits, double filler1) {
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (UpdatablePArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (BitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.UnitBitsUnpacker unpacker = new ArraysOpImpl.UnitBitsUnpacker(context, array, bits, filler1);
        unpacker.process();
    }

    /**
     * Equivalent to <code>{@link #unpackZeroBits(ArrayContext, UpdatablePArray, BitArray, double)
     * unpackZeroBits}({@link ArrayContext#DEFAULT_SINGLE_THREAD}, array, bits, filler0)</code>.
     *
     * @param array   some primitive array.
     * @param bits    the bit array, the zero elements of which should be "unpacked" to <code>array</code>.
     * @param filler0 the value that will be set in <code>array</code> for zero bits.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     */
    public static void unpackZeroBits(UpdatablePArray array, BitArray bits, double filler0) {
        unpackZeroBits(ArrayContext.DEFAULT_SINGLE_THREAD, array, bits, filler0);
    }

    /**
     * Sets every element <code>#k</code> of the passed updatable array to <code>filler0</code> value,
     * if the corresponding element <code>#k</code> of <code>bits</code> array is <code>false</code> (0),
     * or doesn't change it, if this bit is <code>true</code> (1).
     * Equivalent to the loop of the operators
     *
     * <pre>
     * if (!bits.{@link BitArray#getBit(long) getBit}(k))
     * &#32;   array.{@link UpdatablePArray#setDouble(long, double) setDouble}(k, filler0)
     * </pre>
     *
     * <p>for all indexes <code>k</code>, but works much faster than the simple loop.
     *
     * <p>The <code>context</code> argument is used in the same manner as in
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     * Namely, the context allows to specify the desired number of parallel tasks,
     * provide the pool factory, interrupt this method and show its progress.
     * If the <code>context</code> argument is {@code null},
     * then this method still may use several threads,
     * if {@link DefaultThreadPoolFactory} instance recommends it,
     * and may be interruptible by <code>Thread.interrupt()</code>, alike
     * {@link #copy(ArrayContext, UpdatableArray, Array)} method.
     *
     * @param context the context of calculations; can be {@code null}.
     * @param array   some primitive array.
     * @param bits    the bit array, the zero elements of which should be "unpacked" to <code>array</code>.
     * @param filler0 the value that will be set in <code>array</code> for zero bits.
     * @throws NullPointerException  if <code>array</code> or <code>bits</code> argument is {@code null}.
     * @throws SizeMismatchException if <code>array.length()!=bits.length()</code>.
     * @see #unpackBits(ArrayContext, UpdatablePArray, BitArray, double, double)
     * @see #unpackUnitBits(ArrayContext, UpdatablePArray, BitArray, double)
     */
    public static void unpackZeroBits(ArrayContext context, UpdatablePArray array, BitArray bits, double filler0) {
        Objects.requireNonNull(bits, "Null bits argument");
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array) && isTiled(bits)
                && java.util.Arrays.equals(tiledMatrixDimensions(array), tiledMatrixDimensions(bits))
                && java.util.Arrays.equals(tileDimensions(array), tileDimensions(bits))) {
            array = (UpdatablePArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
            bits = (BitArray) ((ArraysTileMatrixImpl.TileMatrixArray) bits).baseMatrix().array();
        }
        ArraysOpImpl.ZeroBitsUnpacker unpacker = new ArraysOpImpl.ZeroBitsUnpacker(context, array, bits, filler0);
        unpacker.process();
    }

    /**
     * Returns an immutable view of the passed AlgART array, cast to another primitive element type
     * (other precision) with automatic scaling, so that 0.0 is cast to 0.0 and
     * {@link PArray#maxPossibleValue(double) maximal possible value} of the source array
     * is scaled to maximal possible value of the result. (For <code>float</code> and <code>double</code>
     * elements we suppose that maximal possible value is 1.0.)
     *
     * <p>More precisely, if <code>newElementType==array.elementType()</code>, this function just returns
     * the <code>array</code> argument without changes, in another case it is equivalent to the following operators:
     * <pre>
     *     final Class&lt;PArray&gt; newType = Arrays.type(PArray.class, newElementType);
     *     final Range destRange = Range.valueOf(0.0, {@link Arrays#maxPossibleValue(Class)
     *     Arrays.maxPossibleValue}(newType));
     *     final Range srcRange = Range.valueOf(0.0, array.{@link PArray#maxPossibleValue(double)
     *     maxPossibleValue(1.0)});
     *     return {@link Arrays#asFuncArray(Func, Class, PArray...)
     *     Arrays.asFuncArray}(LinearFunc.getInstance(destRange, srcRange), newType, array);
     * </pre>
     *
     * @param array          the source AlgART array.
     * @param newElementType required element type.
     * @return the array with the required element type, where every element is equal to
     * the corresponding element of the source array, multiplied
     * by the automatically chosen scale.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if the required element type is not a primitive type.
     */
    public static PArray asPrecision(PArray array, Class<?> newElementType) {
        Objects.requireNonNull(array, "Null array");
        Objects.requireNonNull(newElementType, "Null newElementType");
        if (Arrays.bitsPerElement(newElementType) <= 0) {
            throw new IllegalArgumentException("Element type must be primitive "
                    + "(boolean, char, byte, short, int, long, float or double");
        }
        if (newElementType == array.elementType()) {
            return array;
        }
        final Class<PArray> newType = Arrays.type(PArray.class, newElementType);
        final Range destRange = Range.valueOf(0.0, Arrays.maxPossibleValue(newType));
        final Range srcRange = Range.valueOf(0.0, array.maxPossibleValue(1.0));
        // Note: ranges may be identical for some element type like boolean/float/double
        return asFuncArray(LinearFunc.getInstance(destRange, srcRange), newType, array);
    }

    /**
     * Equivalent to {@link #applyPrecision(ArrayContext, UpdatablePArray, PArray)
     * applyPrecision(null, result, array)}.
     *
     * @param result the destination array.
     * @param array  the source array.
     * @throws NullPointerException  if <code>result</code> or <code>array</code> is {@code null}.
     * @throws SizeMismatchException if passed arrays have different lengths.
     */
    public static void applyPrecision(UpdatablePArray result, PArray array) {
        applyPrecision(null, result, array);
    }

    /**
     * Equivalent to creating a "lazy" array by <code>lazy = {@link #asPrecision(PArray, Class)
     * asPrecision(array, result.elementType()}</code> call
     * and copying it into the <code>result</code> argument by
     * <code>{@link #copy(ArrayContext, UpdatableArray, Array) copy(context, result, lazy)}</code> call.
     *
     * <p>In addition, this method checks, whether the passed arrays have the same length,
     * and throws an exception in another case.
     *
     * <p>If the source and result array have the same element type, this method just copies <code>array</code>
     * to <code>result</code>.
     *
     * @param context the context of copying; can be {@code null}.
     * @param result  the destination array.
     * @param array   the source array.
     * @throws NullPointerException  if <code>result</code> or <code>array</code> is {@code null}.
     * @throws SizeMismatchException if passed arrays have different lengths.
     * @throws java.io.IOError       if the current thread is interrupted by the standard
     *                               <code>Thread.interrupt()</code> call.
     */
    public static void applyPrecision(ArrayContext context, UpdatablePArray result, PArray array) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(array, "Null array");
        if (result.length() != array.length()) {
            throw new SizeMismatchException("array.length() and result.length() mismatch");
        }
        if (result.elementType() == array.elementType()) {
            copy(context, result, array);
        } else if (array.elementType() == boolean.class) {
            // optimization
            unpackBits(context, result, (BitArray) array, 0, result.maxPossibleValue(1.0));
        } else {
            copy(context, result, asPrecision(array, result.elementType()));
        }
    }

    /**
     * Fills all the elements of the array by zero (0 for numbers, <code>false</code> for boolean values,
     * {@code null} or some "empty" objects for non-primitive elements).
     *
     * @param array the filled array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @see UpdatablePArray#fill(double)
     * @see UpdatablePArray#fill(long)
     */
    public static void zeroFill(UpdatableArray array) {
        Objects.requireNonNull(array, "Null array argument");
        if (isTiled(array)) {
            array = (UpdatableArray) ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().array();
        }
        if (array instanceof BitArray) {
            ((UpdatableBitArray) array).fill(false);
        } else if (array instanceof CharArray) {
            ((UpdatableCharArray) array).fill((char) 0);
        } else if (array instanceof ByteArray) {
            ((UpdatableByteArray) array).fill((byte) 0);
        } else if (array instanceof ShortArray) {
            ((UpdatableShortArray) array).fill((short) 0);
        } else if (array instanceof IntArray) {
            ((UpdatableIntArray) array).fill(0);
        } else if (array instanceof LongArray) {
            ((UpdatableLongArray) array).fill(0L);
        } else if (array instanceof FloatArray) {
            ((UpdatableFloatArray) array).fill(0.0f);
        } else if (array instanceof DoubleArray) {
            ((UpdatableDoubleArray) array).fill(0.0);
        } else if (array instanceof ObjectArray<?>) {
            ((UpdatableObjectArray<?>) array).fill(null);
        } else {
            throw new AssertionError("The array does not implement necessary interfaces: " + array.getClass());
        }
    }

    /**
     * Copies all elements of the specified AlgART array (2nd argument) into the <code>bytes</code> Java array
     * (1st argument, or into a newly created <code>byte[]</code> array if <code>bytes==null</code>), and returns
     * the resulting <code>byte[]</code> array.
     *
     * <p>The length of <code>bytes</code> array must be enough for storing all elements of the given AlgART
     * array (but may be greater than necessary). More precisely, <code>bytes.length</code> must be
     * <code>&ge;Arrays.{@link #sizeOfBytes(PArray)
     * sizeOfBytes}(array)</code>.
     * Note that <code>Arrays.{@link #sizeOf(Array) sizeOf}(array)</code> is a suitable length
     * for all types (though for {@link BitArray} it can be little greater than necessary).
     *
     * <p>The <code>bytes</code> argument can be {@code null}; in this case, this method automatically allocates
     * <code>byte[]</code> array with minimal necessary length <code>Arrays.{@link #sizeOfBytes(PArray)
     * sizeOfBytes}(array)</code>, copies all elements of the AlgART array
     * into it and returns it.
     *
     * <p>The array element #<i>k</i> (<code>array.{@link Array#getElement(long) getElement}(<i>k</i>))</code>
     * is stored at the following position in the <code>bytes</code> Java array:
     * <ul>
     * <li>for {@link BitArray}, bit #<i>k</i> is stored in the bit #<i>k</i>%8 in the byte
     * <code>bytes[<i>k</i>/8]</code>, i.e. in <code>bytes[<i>k</i>/8]&nbsp;&amp;&nbsp;(1&lt;&lt;(<i>k</i>%8))</code>;
     * several last unused bits of the last byte are cleared to zero
     * (the number of these bits is <code>8*(<i>N</i>/8)-<i>N</i></code>,
     * where <code><i>N</i>=array.{@link Array#length() length()}</code>);</li>
     *
     * <li>for {@link ByteArray}, byte #<i>k</i> is stored in the byte <code>bytes[<i>k</i>]</code>;</li>
     *
     * <li>for other variants of {@link PArray}, element #<i>k</i> is stored in <code>bytesPerElement</code>
     * sequential bytes
     * <code>bytes[<i>k</i>*bytesPerElement]...bytes[<i>k</i>*bytesPerElement+bytesPerElement-1]</code>,
     * where <code>bytesPerElement</code> is 2, 2, 4, 8, 4, 8 for an array of <code>char</code>,
     * <code>short</code>, <code>int</code>, <code>long</code>, <code>float</code>, <code>double</code>
     * correspondingly;</li>
     *
     * <li>all elements, greater than 1 byte &mdash;
     * <code>char</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code>, <code>double</code> &mdash; are stored as sequences of 2, 2, 4, 8, 4, 8 bytes
     * (correspondingly) according to the byte order, specified in the last argument
     * (see <code>java.nio.ByteOrder</code>);</li>
     *
     * <li>every <code>float</code> element of {@link FloatArray} is stored as a sequence of
     * 4 bytes of <code>int</code> value, got by <code>Float.floatToRawIntBits</code> method
     * (from lowest to highest);
     * every <code>double</code> element of {@link DoubleArray} is stored as a sequence of
     * 8 bytes of <code>long</code> value, got by <code>Double.doubleToRawLongBits</code> method
     * (from lowest to highest).</li>
     * </ul>
     *
     * <p>This method can be used for serialization of AlgART arrays into a form, suitable for writing into
     * <code>OutputStream</code>, for example, for storing on external devices or passing through the network.
     *
     * <p>This conversion is performed according the specified byte order, like in
     * <code>ByteBuffer.asXxxBuffer</code> methods (after <code>byteBuffer.order(byteOrder)</code> call),
     * excepting the case of {@link BitArray}, when <code>ByteOrder.LITTLE_ENDIAN</code> order is used always.
     *
     * <p>Note: unlike {@link #write(OutputStream, PArray, ByteOrder)} method,
     * for {@link BitArray} case this method requires little less bytes:
     * {@link #sizeOfBytes(PArray) sizeOfBytes(array)} instead of
     * {@link #sizeOf(Array) sizeOf(array)}. If you are interested in compatibility with
     * {@link #write(OutputStream, PArray, ByteOrder) write} /
     * {@link #read(InputStream, UpdatablePArray, ByteOrder) read} methods, you should allocate
     * and serialize <code>(int)Arrays.{@link #sizeOf(Array) sizeOf}(array)</code> bytes instead of
     * <code>Arrays.{@link #sizeOfBytes(PArray) sizeOfBytes}(array)</code>.
     *
     * <p>We recommend calling this method for relatively small arrays only, up to several megabytes,
     * to avoid extra usage of RAM. If you need to serialize a large AlgART array,
     * you may apply this method sequentially for its {@link Array#subArray(long, long) subarrays}.
     *
     * @param array     the source AlgART array.
     * @param bytes     Java array, to which the content of the source array will be copied;
     *                  can be {@code null}, then it will be allocated automatically.
     * @param byteOrder the byte order for element types, greater than 1 byte;
     *                  it is not used in cases of {@link ByteArray} and {@link BitArray}.
     * @return Java array with resulting data;
     * if <code>bytes</code> argument is not {@code null}, a reference to this argument is returned.
     * @throws NullPointerException     if <code>array</code> or <code>byteOrder</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>bytes!=null</code> and the length of <code>bytes</code> array
     *                                  is not enough for storing all elements of the source AlgART array.
     * @throws TooLargeArrayException   if the required result array length is greater
     *                                  than <code>Integer.MAX_VALUE</code> bytes.
     * @see #toArray(UpdatablePArray, byte[], ByteOrder)
     * @see #write(OutputStream, PArray, ByteOrder)
     * @see LargeMemoryModel#asUpdatableArray(Object, Class, long, long, boolean, ByteOrder)
     * @see JArrays#arrayToBytes(byte[], Object, long, ByteOrder)
     */
    public static byte[] toBytes(byte[] bytes, PArray array, ByteOrder byteOrder) {
        Objects.requireNonNull(array, "Null array");
        Objects.requireNonNull(byteOrder, "Null byteOrder");
        final long requiredLength = Arrays.sizeOfBytes(array);
        assert requiredLength == (int) requiredLength;
        if (bytes == null) {
            bytes = new byte[(int) requiredLength];
        } else {
            if (bytes.length < requiredLength) {
                throw new IllegalArgumentException("Not enough space to copy the AlgART array into byte[] array: "
                        + requiredLength + " bytes required, but only " + bytes.length + " available");
            }
        }
        if (array instanceof ByteArray) {
            assert requiredLength == array.length();
            array.getData(0, bytes, 0, (int) requiredLength);
        } else if (array instanceof BitArray a) {
            final long[] data = a.jaBit();
            assert (long) data.length * 8L >= requiredLength;
            PackedBitArraysPer8.toByteArray(bytes, data, array.length());
        } else if (array instanceof CharArray a) {
            final char[] data = a.ja();
            JArrays.charArrayToBytes(bytes, data, data.length, byteOrder);
        } else if (array instanceof ShortArray a) {
            final short[] data = a.ja();
            JArrays.shortArrayToBytes(bytes, data, data.length, byteOrder);
        } else if (array instanceof IntArray a) {
            final int[] data = a.ja();
            JArrays.intArrayToBytes(bytes, data, data.length, byteOrder);
        } else if (array instanceof LongArray a) {
            final long[] data = a.ja();
            JArrays.longArrayToBytes(bytes, data, data.length, byteOrder);
        } else if (array instanceof FloatArray a) {
            final float[] data = a.ja();
            JArrays.floatArrayToBytes(bytes, data, data.length, byteOrder);
        } else if (array instanceof DoubleArray a) {
            final double[] data = a.ja();
            JArrays.doubleArrayToBytes(bytes, data, data.length, byteOrder);
        } else {
            throw new AssertionError("Unallowed type of passed array: " + array.getClass());
        }
        return bytes;
    }

    /**
     * Copies the elements, stored in the <code>bytes</code> Java array (2nd argument)
     * by previous {@link #toBytes(byte[], PArray, ByteOrder)}
     * call, back into the specified AlgART array (1st argument).
     *
     * <p>As in {@link #toBytes(byte[], PArray, ByteOrder) toBytes} method,
     * the length of <code>bytes</code> array must be enough for storing all elements of the given AlgART
     * array. More precisely, <code>bytes.length</code> must be
     * <code>&ge;Arrays.{@link #sizeOfBytes(PArray)
     * sizeOfBytes}(array)</code>.
     * Note that <code>Arrays.{@link #sizeOf(Array) sizeOf}(array)</code> is a suitable length
     * for all types (though for {@link BitArray} it can be little greater than necessary).
     * This method always copies all <code>array.{@link Array#length() length()}</code>
     * elements of the passed <code>array</code>, regardless on the length of <code>bytes</code> array.
     *
     * <p>The array element #<i>k</i> (<code>array.{@link Array#getElement(long) getElement}(<i>k</i>))</code>
     * is retrieved from the same position in the <code>bytes</code> Java array, where it is stored by
     * {@link #toBytes(byte[], PArray, ByteOrder)} method (see comments to it).
     * The elements <code>float</code>
     * and <code>double</code> are retrieved from the corresponding byte sequences via
     * <code>Float.intBitsToFloat</code> and <code>Double.longBitsToDouble</code> methods.
     *
     * <p>This method can be used for deserialization of AlgART arrays from a form, created by
     * {@link #toBytes(byte[], PArray, ByteOrder) toBytes} method and loaded from
     * <code>InputStream</code>, for example, after reading from external devices or passing through the network.
     *
     * <p>This conversion is performed according the specified byte order, like in
     * <code>ByteBuffer.asXxxBuffer</code> methods (after <code>byteBuffer.order(byteOrder)</code> call),
     * excepting the case of {@link BitArray}, when <code>ByteOrder.LITTLE_ENDIAN</code> order is used always.
     *
     * <p>We recommend calling this method for relatively small arrays only, up to several megabytes,
     * to avoid extra usage of RAM. If you need to deserialize a large AlgART array,
     * you may apply this method sequentially for its {@link UpdatableArray#subArray(long, long) subarrays}.
     *
     * @param array     the target AlgART array, all elements of which should be copied from the Java array.
     * @param bytes     the source Java array, filled according the specification of
     *                  {@link #toBytes(byte[], PArray, ByteOrder) toBytes} method.
     * @param byteOrder the byte order for element types, greater than 1 byte;
     *                  it is not used in cases of {@link ByteArray} and {@link BitArray}.
     * @throws NullPointerException     if any of the arguments is {@code null}.
     * @throws IllegalArgumentException if the length of <code>bytes</code> array
     *                                  is not enough for storing all elements of the target AlgART array.
     * @see #read(InputStream, UpdatablePArray, ByteOrder)
     * @see LargeMemoryModel#asArray(Object, Class, long, long, ByteOrder)
     * @see JArrays#bytesToArray(Object, byte[], long, Class, ByteOrder)
     */
    public static void toArray(UpdatablePArray array, byte[] bytes, ByteOrder byteOrder) {
        Objects.requireNonNull(array, "Null array");
        Objects.requireNonNull(bytes, "Null bytes Java array");
        Objects.requireNonNull(byteOrder, "Null byteOrder");
        final long requiredLength = Arrays.sizeOfBytes(array);
        if (bytes.length < requiredLength) {
            throw new IllegalArgumentException("byte[] array is too short to copy into all elements of "
                    + "the AlgART array: " + requiredLength + " bytes required, but only " +
                    bytes.length + " available");
        }
        if (array instanceof UpdatableByteArray) {
            assert requiredLength == array.length();
            array.setData(0, bytes, 0, (int) requiredLength);
        } else if (array instanceof UpdatableBitArray a) {
            final long packedLength = PackedBitArrays.packedLength(a.length());
            assert packedLength <= Integer.MAX_VALUE; // because requiredLength <= bytes.length <= Integer.MAX_VALUE
            assert packedLength * 8L >= requiredLength;
            final long[] data = PackedBitArraysPer8.toLongArray(bytes, a.length());
            a.setBits(0, data, 0, a.length());
        } else if (array instanceof UpdatableCharArray) {
            final char[] data = JArrays.bytesToCharArray(bytes, array.length32(), byteOrder);
            // note that length32() cannot lead to exception (here and below): requiredLength <= bytes.length
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableShortArray) {
            final short[] data = JArrays.bytesToShortArray(bytes, array.length32(), byteOrder);
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableIntArray) {
            final int[] data = JArrays.bytesToIntArray(bytes, array.length32(), byteOrder);
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableLongArray) {
            final long[] data = JArrays.bytesToLongArray(bytes, array.length32(), byteOrder);
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableFloatArray) {
            final float[] data = JArrays.bytesToFloatArray(bytes, array.length32(), byteOrder);
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableDoubleArray) {
            final double[] data = JArrays.bytesToDoubleArray(bytes, array.length32(), byteOrder);
            array.setData(0, data, 0, data.length);
        } else {
            throw new AssertionError("Unallowed type of passed array: " + array.getClass());
        }
    }

    /**
     * Returns the minimal size of <code>byte[]</code> array, enough for copying there the given AlgART array
     * by {@link #toBytes(byte[], PArray, ByteOrder)} method.
     * More precisely, returns
     * <code>array.{@link Array#length() length()}+7/8</code> for {@link BitArray}
     * or <code>Arrays.{@link #sizeOf(Array) sizeOf}(array)</code> for other primitive types.
     *
     * @param array the source AlgART array.
     * @return the minimal size of <code>byte[]</code> array, enough for calling
     * {@link #toBytes(byte[], PArray, ByteOrder)} method.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the result (required Java array length) is greater
     *                                than <code>Integer.MAX_VALUE</code> elements.
     * @see #sizeOf(Array)
     */
    public static int sizeOfBytes(PArray array) {
        Objects.requireNonNull(array, "Null array");
        final long requiredLength = array instanceof BitArray ? (array.length() + 7) >>> 3 : Arrays.sizeOf(array);
        if (requiredLength > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Cannot calculate required number of bytes for "
                    + " copying AlgART array to byte[] array, because it is too large: " + array);
        }
        return (int) requiredLength;
    }

    /**
     * Writes all elements of the passed array to the passed output stream from its current position.
     *
     * <p>More precisely, this method writes <code>{@link #sizeOf(Array) sizeOf}(array)</code> bytes
     * to the output stream (starting from the current position) by necessary number of sequential calls of
     * <code>outputStream.write(byteArray,0,len)</code>.
     * The necessary byte array <code>byteArray</code> is built by conversion (unpacking)
     * of the Java array <code>packedArray</code> and reading from the sequential ranges of the array: it is
     * <code>long[]</code>, <code>char[]</code>, <code>byte[]</code>, <code>short[]</code>, <code>int[]</code>,
     * <code>long[]</code>, <code>float[]</code>,  <code>double[]</code>,
     * if the array is correspondingly {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray}, {@link IntArray},
     * {@link LongArray}, {@link FloatArray}, {@link DoubleArray}
     * (if the array is already {@link ByteArray}, the read byte array is used without conversion).
     * This conversion is performed according the specified byte order, like in
     * <code>ByteBuffer.asXxxBuffer</code> methods (after <code>byteBuffer.order(byteOrder)</code> call),
     * excepting the case of {@link BitArray}, when <code>ByteOrder.LITTLE_ENDIAN</code> order is used always.
     * The <code>packedArray</code> is sequentially read from the specified AlgART array,
     * from its element #0 to its last element #<code>array.length()-1</code>,
     * by sequential calls of <code>{@link BitArray#getBits(long, long[], long, long)
     * getBits}(position,packedArray,0,count)</code> in a case of {@link BitArray}
     * or <code>{@link Array#getData(long, Object, int, int)
     * getData}(position,packedArray,0,count)</code> in other cases.
     *
     * <p>Note that this method always writes in the stream the integer number of blocks per
     * 8, 2, 1, 2, 4, 8, 4, 8 bytes for cases of (correspondingly) {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray}, {@link IntArray},
     * {@link LongArray}, {@link FloatArray}, {@link DoubleArray}.
     * The total number of written bytes is <code>{@link #sizeOf(Array) sizeOf}(array)</code>.
     * If the array is {@link BitArray} and its length <i>n</i>=<code>array.{@link Array#length() length()}</code>
     * is not divisible by 64, then the last unused <i>r</i>=64&minus;(<i>n</i>%64) bits
     * in the last <code>long</code> value are filled by zero. (These last <i>r</i> unused bits,
     * in a case of little-endian byte order, are stored in the last
     * (<i>r</i>+7)/8 bytes of the output stream;
     * in a case of big-endian byte order they are stored in the first (<i>r</i>+7)/8  bytes
     * of the last 8-byte block.)</p>
     *
     * <p>This method does not flush or close the passed stream.
     *
     * @param outputStream the output stream.
     * @param array        the array that should be written.
     * @param byteOrder    the byte order in the stream;
     *                     it is not used in cases of {@link ByteArray} and {@link BitArray}.
     * @throws NullPointerException if <code>outputStream</code>, <code>array</code> or <code>byteOrder</code>
     *                              argument is {@code null}.
     * @throws IOException          in the same situations as in the standard <code>OutputStream.write</code> method.
     * @see #toBytes(byte[], PArray, ByteOrder)
     * @see #read(InputStream, UpdatablePArray, ByteOrder)
     */
    public static void write(OutputStream outputStream, PArray array, ByteOrder byteOrder) throws IOException {
        ArraysSerializationImpl.write(outputStream, array, byteOrder);
    }

    /**
     * Reads all elements of the passed array from the passed input stream from its current position.
     *
     * <p>More precisely, this method reads <code>{@link #sizeOf(Array) sizeOf}(array)</code> bytes from
     * the input stream (starting from the current position) by necessary number of sequential calls of
     * <code>dataInputStream.readFully(byteArray,0,len)</code>, where
     * <code>dataInputStream = new DataInputStream(inputStream)</code> (this object
     * is created in the beginning with the only purpose to provide <code>readFully</code> functionality).
     * Then all read bytes are converted (packed) to Java array <code>packedArray</code>: it is
     * <code>long[]</code>, <code>char[]</code>, <code>byte[]</code>, <code>short[]</code>, <code>int[]</code>,
     * <code>long[]</code>, <code>float[]</code>,  <code>double[]</code>,
     * if the array is correspondingly {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray}, {@link IntArray},
     * {@link LongArray}, {@link FloatArray}, {@link DoubleArray}
     * (if the array is already {@link ByteArray}, the read byte array is used without conversion).
     * This conversion is performed according the specified byte order, like in
     * <code>ByteBuffer.asXxxBuffer</code> methods (after <code>byteBuffer.order(byteOrder)</code> call),
     * excepting the case of {@link BitArray}, when <code>ByteOrder.LITTLE_ENDIAN</code> order is used always.
     * After this, the converted elements are sequentially written to the specified AlgART array,
     * from its element #0 to its last element #<code>array.length()-1</code>,
     * by sequential calls of <code>{@link UpdatableBitArray#setBits(long, long[], long, long)
     * setBits}(position,packedArray,0,count)</code> in a case of {@link BitArray}
     * or <code>{@link UpdatableArray#setData(long, Object, int, int)
     * setData}(position,packedArray,0,count)</code> in other cases.
     *
     * <p>Note that this method always reads from the stream the integer number of blocks per
     * 8, 2, 1, 2, 4, 8, 4, 8 bytes for cases of (correspondingly) {@link BitArray}, {@link CharArray},
     * {@link ByteArray}, {@link ShortArray}, {@link IntArray},
     * {@link LongArray}, {@link FloatArray}, {@link DoubleArray}.
     * The total number of read bytes is <code>{@link #sizeOf(Array) sizeOf}(array)</code>.
     * If there is no such number of available bytes in the input stream, <code>EOFException</code> is thrown.
     *
     * <p>This method does not close the passed stream.
     *
     * @param inputStream the input stream.
     * @param array       the updatable array that should be read.
     * @param byteOrder   the byte order in the stream;
     *                    it is not used in cases of {@link ByteArray} and {@link BitArray}.
     * @throws NullPointerException if <code>inputStream</code>, <code>array</code> or <code>byteOrder</code>
     *                              argument is {@code null}.
     * @throws IOException          in the same situations as in the standard
     *                              <code>DataInputStream.readFully</code> method.
     * @see #toArray(UpdatablePArray, byte[], ByteOrder)
     * @see #write(OutputStream, PArray, ByteOrder)
     */
    public static void read(InputStream inputStream, UpdatablePArray array, ByteOrder byteOrder) throws IOException {
        ArraysSerializationImpl.read(inputStream, array, byteOrder);
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static Object toJavaArray(Array array) {
        return array.toJavaArray();
    }

    /*Repeat() char ==> boolean,,byte,,short,,int,,long,,float,,double;;
               Char ==> Bit,,Byte,,Short,,Int,,Long,,Float,,Double */

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static char[] toJavaArray(CharArray array) {
        return array.toJavaArray();
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static boolean[] toJavaArray(BitArray array) {
        return array.toJavaArray();
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static byte[] toJavaArray(ByteArray array) {
        return array.toJavaArray();
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static short[] toJavaArray(ShortArray array) {
        return array.toJavaArray();
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static int[] toJavaArray(IntArray array) {
        return array.toJavaArray();
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static long[] toJavaArray(LongArray array) {
        return array.toJavaArray();
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static float[] toJavaArray(FloatArray array) {
        return array.toJavaArray();
    }

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated  alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static double[] toJavaArray(DoubleArray array) {
        return array.toJavaArray();
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Equivalent to {@link Array#toJavaArray() array.toJavaArray()} (this is a deprecated alias for that method).
     *
     * @param array the source AlgART array.
     * @return Java array containing all the elements in this array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     */
    @Deprecated(forRemoval=true)
    public static <E> E[] toJavaArray(ObjectArray<E> array) {
        return array.toJavaArray();
    }

    /**
     * Returns a string containing all characters in this character array (in the same
     * order as in this array). The length of the string will be equal to
     * {@link Array#length() charArray.length()}.
     *
     * @param charArray the source array.
     * @return a string containing all characters in this character array.
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>.
     * @see CharArray#toString()
     */
    public static String toString(CharArray charArray) {
        Objects.requireNonNull(charArray, "Null charArray argument");
        char[] array = (char[]) javaArrayInternal(charArray);
        int offset, count;
        if (array != null) {
            offset = javaArrayOffsetInternal(charArray);
            count = (int) charArray.length();
        } else {
            array = charArray.toJavaArray();
            offset = 0;
            count = array.length;
        }
        return new String(array, offset, count);
    }

    /**
     * Joins and returns as a string the standard string representations for all
     * elements of the AlgART array, separating elements by the given <code>separator</code>.
     *
     * <p>For arrays of <code>boolean</code> elements ({@link BitArray}), "false" and "true"
     * words are used as representation.
     * For integer arrays ({@link ByteArray}, {@link ShortArray},
     * {@link IntArray}, {@link LongArray}), the signed decimal representation is used.
     * For arrays of <code>char</code> elements ({@link CharArray}),
     * the unsigned decimal representation is used.
     * For arrays of objects ({@link ObjectArray}),
     * the {@code null} elements are represented as <code>"null"</code> string,
     * and all non-null elements converted to strings by their <code>toString</code> method.
     *
     * <p>If the necessary string length exceeds <code>maxStringLength</code> characters,
     * this method break joining after the element, which leads to exceeding this limit,
     * and adds "..." instead of all further elements. So, the length of returning
     * string will never be essentially larger than <code>maxStringLength</code> characters.
     * You may specify <code>Integer.MAX_VALUE</code> as <code>maxStringLength</code> value
     * to be sure that all array elements will be joined.
     *
     * <p>If the passed array is empty, returns the empty string (<code>""</code>).
     *
     * @param array           the source AlgART array.
     * @param separator       the string used for separating elements.
     * @param maxStringLength the maximal allowed length of returned string (longer results are truncated
     *                        with adding "..." at the end).
     * @return the string representations of all elements joined into one string.
     * @throws NullPointerException     if <code>array</code> or <code>separator</code> argument is {@code null}
     * @throws IllegalArgumentException if <code>maxStringLength</code> &lt;= 0.
     * @see #toString(Array, Locale, String, String, int)
     * @see #toHexString(Array, String, int)
     * @see JArrays#toString(boolean[], String, int)
     * @see JArrays#toString(char[], String, int)
     * @see JArrays#toString(byte[], String, int)
     * @see JArrays#toString(short[], String, int)
     * @see JArrays#toString(int[], String, int)
     * @see JArrays#toString(long[], String, int)
     * @see JArrays#toString(float[], String, int)
     * @see JArrays#toString(double[], String, int)
     * @see JArrays#toString(Object[], String, int)
     */
    public static String toString(Array array, String separator, int maxStringLength) {
        Objects.requireNonNull(array, "Null array argument");
        Objects.requireNonNull(separator, "Null separator argument");
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("maxStringLength argument must be positive");
        }
        long n = array.length();
        if (n == 0) {
            return "";
        }
        MutableCharArray ca = SimpleMemoryModel.getInstance().newEmptyCharArray();
        if (array instanceof BitArray a) {
            ca.append(String.valueOf(a.getBit(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getBit(k)));
            }
        } else if (array instanceof CharArray a) {
            ca.append(String.valueOf(a.getChar(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getChar(k)));
            }
        } else if (array instanceof ByteArray a) {
            ca.append(String.valueOf(a.getByte(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getByte(k)));
            }
        } else if (array instanceof ShortArray a) {
            ca.append(String.valueOf(a.getShort(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getShort(k)));
            }
        } else if (array instanceof IntArray a) {
            ca.append(String.valueOf(a.getInt(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getInt(k)));
            }
        } else if (array instanceof LongArray a) {
            ca.append(String.valueOf(a.getLong(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getLong(k)));
            }
        } else if (array instanceof FloatArray a) {
            ca.append(String.valueOf(a.getFloat(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getFloat(k)));
            }
        } else if (array instanceof DoubleArray a) {
            ca.append(String.valueOf(a.getDouble(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.getDouble(k)));
            }
        } else if (array instanceof ObjectArray<?> a) {
            ca.append(String.valueOf(a.get(0)));
            for (long k = 1; k < n; k++) {
                if (ca.length() >= maxStringLength) {
                    ca.append(separator).append("...");
                    break;
                }
                ca.append(separator).append(String.valueOf(a.get(k)));
            }
        } else {
            throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
        }
        return toString(ca);
    }


    /**
     * Joins and returns as a string the string representations for all
     * elements of the AlgART array, separating elements by the given <code>separator</code>,
     * using <code>format</code> string for formatting numeric elements.
     *
     * <p>This method works alike {@link #toString(Array, String, int)}, with the only
     * difference that <code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>float</code> and <code>double</code> elements are converted to string
     * with the specified format by <code>String.format</code> method.
     * For array types {@link BitArray}, {@link CharArray} and {@link ObjectArray}
     * this method is equivalent to
     * {@link #toString(Array, String, int) toString(array, separator, maxStringLength)}.
     *
     * @param array           the source AlgART array.
     * @param locale          the locale that will be passed to <code>String.format(locale,format,v)</code> call.
     * @param format          format string for numeric elements: each element <code>v</code> is converted
     *                        to string by <code>String.format(locale,format,v)</code> call.
     * @param separator       the string used for separating elements.
     * @param maxStringLength the maximal allowed length of returned string (longer results are truncated
     *                        with adding "..." at the end).
     * @return the string representations of all elements joined into one string.
     * @throws NullPointerException     if <code>array</code> or <code>separator</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>maxStringLength</code> &lt;= 0.
     * @see #toString(Array, String, int)
     * @see JArrays#toString(byte[], Locale, String, String, int)
     * @see JArrays#toString(short[], Locale, String, String, int)
     * @see JArrays#toString(int[], Locale, String, String, int)
     * @see JArrays#toString(long[], Locale, String, String, int)
     * @see JArrays#toString(float[], Locale, String, String, int)
     * @see JArrays#toString(double[], Locale, String, String, int)
     */
    public static String toString(Array array, Locale locale, String format, String separator, int maxStringLength) {
        Objects.requireNonNull(array, "Null array argument");
        Objects.requireNonNull(separator, "Null separator argument");
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("maxStringLength argument must be positive");
        }
        long n = array.length();
        if (n == 0) {
            return "";
        }
        MutableCharArray cv = SimpleMemoryModel.getInstance().newEmptyCharArray();
        if (array instanceof ByteArray a) {
            cv.append(String.format(locale, format, a.getByte(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(String.format(locale, format, a.getByte(k)));
            }
        } else if (array instanceof ShortArray a) {
            cv.append(String.format(locale, format, a.getShort(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(String.format(locale, format, a.getShort(k)));
            }
        } else if (array instanceof IntArray a) {
            cv.append(String.format(locale, format, a.getInt(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(String.format(locale, format, a.getInt(k)));
            }
        } else if (array instanceof LongArray a) {
            cv.append(String.format(locale, format, a.getLong(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(String.format(locale, format, a.getLong(k)));
            }
        } else if (array instanceof FloatArray a) {
            cv.append(String.format(locale, format, a.getFloat(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(String.format(locale, format, a.getFloat(k)));
            }
        } else if (array instanceof DoubleArray a) {
            cv.append(String.format(locale, format, a.getDouble(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(String.format(locale, format, a.getDouble(k)));
            }
        } else {
            return toString(array, separator, maxStringLength);
        }
        return toString(cv);
    }

    /**
     * Joins and returns as a string the "hexadecimal" string representations for all
     * elements of the AlgART array, separating elements by the given <code>separator</code>.
     *
     * <p>The result will be the same as for {@link #toString(Array, String, int)} method,
     * beside the following:<ul>
     * <li>for arrays of <code>boolean</code> elements ({@link BitArray}), numeric representation
     * ("0" and "1") is used instead of "false" and "true";</li>
     * <li>for arrays of <code>char</code>, <code>byte</code>,
     * <code>short</code>, <code>int</code>, <code>long</code> elements
     * ({@link CharArray}, {@link ByteArray}, {@link ShortArray}, {@link IntArray}, {@link LongArray}),
     * the unsigned hexadecimal representation is used.</li>
     * </ul>
     *
     * @param array           the source AlgART array.
     * @param separator       the string used for separating elements.
     * @param maxStringLength the maximal allowed length of returned string (longer results are truncated
     *                        with adding "..." at the end).
     * @return the string representations of all elements joined into one string.
     * @throws NullPointerException     if <code>array</code> or <code>separator</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>maxStringLength</code> &lt;= 0.
     * @see #toString(Array, String, int)
     * @see JArrays#toBinaryString(boolean[], String, int)
     * @see JArrays#toHexString(char[], String, int)
     * @see JArrays#toHexString(byte[], String, int)
     * @see JArrays#toHexString(short[], String, int)
     * @see JArrays#toHexString(int[], String, int)
     * @see JArrays#toHexString(long[], String, int)
     */
    public static String toHexString(Array array, String separator, int maxStringLength) {
        Objects.requireNonNull(array, "Null array argument");
        Objects.requireNonNull(separator, "Null separator argument");
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("maxStringLength argument must be positive");
        }
        long n = array.length();
        if (n == 0) {
            return "";
        }
        MutableCharArray cv = SimpleMemoryModel.getInstance().newEmptyCharArray();
        if (array instanceof BitArray a) {
            cv.append(a.getBit(0) ? "1" : "0");
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(a.getBit(k) ? "1" : "0");
            }
        } else if (array instanceof CharArray a) {
            cv.append(InternalUtils.toHexString((short) a.getChar(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(InternalUtils.toHexString((short) a.getChar(k)));
            }
        } else if (array instanceof ByteArray a) {
            cv.append(InternalUtils.toHexString((byte) a.getByte(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(InternalUtils.toHexString((byte) a.getByte(k)));
            }
        } else if (array instanceof ShortArray a) {
            cv.append(InternalUtils.toHexString((short) a.getShort(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(InternalUtils.toHexString((short) a.getShort(k)));
            }
        } else if (array instanceof IntArray a) {
            cv.append(InternalUtils.toHexString(a.getInt(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(InternalUtils.toHexString(a.getInt(k)));
            }
        } else if (array instanceof LongArray a) {
            cv.append(InternalUtils.toHexString(a.getLong(0)));
            for (long k = 1; k < n; k++) {
                if (cv.length() >= maxStringLength) {
                    cv.append(separator).append("...");
                    break;
                }
                cv.append(separator).append(InternalUtils.toHexString(a.getLong(k)));
            }
        } else {
            return toString(array, separator, maxStringLength);
        }
        return toString(cv);
    }

    /**
     * Equivalent to {@link MutableArray#length(long)} method
     * with the only exception that <code>newUnsignedLength</code> is considered as <i>unsigned</i> long value.
     * More precisely, this method throws {@link TooLargeArrayException}
     * if <code>newUnsignedLength&lt;0</code>
     * (for unsigned values, it means that the number <code>&gt;=2<sup>63</sup></code>);
     * in another case, this method just calls <code>array.length(newUnsignedLength)</code>.
     *
     * <p>This method is convenient if you need to set the new length to a sum <code>a+b</code> of 2
     * <code>long</code> values <code>a</code> and <code>b</code> (usual signed positive <code>long</code> values),
     * but you are not sure that this sum cannot exceed <code>Long.MAX_VALUE=2<sup>63</sup>-1</code> limit.
     * For example, let <code>a</code> is some resizable array, and you need to increase its length
     * by some positive <code>incr</code> value. If, due to some bug, <code>incr</code> is too large
     * (as <code>Long.MAX_VALUE</code>), then the call <code>a.length(a.length()+incr)</code> can
     * <i>reduce</i> the array length, but the call <code>Arrays.length(a, a.length()+incr)</code>
     * works correctly: throws an exception.
     *
     * @param array             an array to be resized.
     * @param newUnsignedLength new array length (unsigned).
     * @throws NullPointerException   if <code>array</code> argument is {@code null}.
     * @throws TooLargeArrayException if the sum <code>newUnsignedLength</code> (as unsigned value) exceeds
     *                                <code>Long.MAX_VALUE</code> limit.
     */
    public static void lengthUnsigned(MutableArray array, long newUnsignedLength) {
        if (newUnsignedLength < 0) {
            throw new TooLargeArrayException("Too large desired array length (>=2^63)");
        }
        array.length(newUnsignedLength);
    }

    /**
     * Returns the minimal <code>offset=0..maxAvailableGap-1</code>, for which the calls
     * {@link BitArray#getBits(long, long[], long, long) getBits(position, destArray, offset, someCount)}
     * and (for updatable array)
     * {@link UpdatableBitArray#setBits(long, long[], long, long) setBits(position, srcArray, offset, someCount)}
     * work essentially faster than for most of the other ("slow") positions,
     * or <code>0</code> if there is no such offset,
     * in particular, if <code>position&gt;={@link Array#length() length()}</code>.
     *
     * <p>This result is based on the call <code>bitArray.{@link BitArray#nextQuickPosition nextQuickPosition}</code>.
     * More precisely, this method returns<br>
     * <code>&nbsp;&nbsp;&nbsp;&nbsp;(int)(position - quickPosition) % maxAvailableGap</code>,<br>
     * where<br>
     * <code>&nbsp;&nbsp;&nbsp;&nbsp;quickPosition = bitArray.{@link BitArray#nextQuickPosition
     * nextQuickPosition}(position - Math.max(maxAvailableGap, 64))</code>.<br>
     * (Subtracting <code>max(maxAvailableGap, 64)</code> does not change the result in a usual case,
     * but allows to return a better result if position is near bitArray.length().)
     *
     * <p>The <code>maxAvailableGap</code> argument should be a positive power of two (<code>2<sup>k</sup></code>)
     * not less than 64 (number of bits in 1 <code>long</code> value).
     * Otherwise, it is automatically replaced with the maximal power of two less than it
     * (<code>Integer.highestOneBit(maxAvailableGap)</code>) at the very beginning of the calculations.
     * If <code>maxAvailableGap=0</code>, this method always returns <code>0</code>.
     *
     * <p>This method is convenient in the following typical situation.
     * Let us have some work <code>long[]</code> array, and we need to load there
     * <code>n</code> packed bits from {@link BitArray} from the given position.
     * The simple call {@link BitArray#getBits(long, long[], long, long)
     * getBits(position, workArray, 0, n)} will work slowly if the position in the bit arrays is not aligned.
     * But we may create a little "gap" at the beginning of the work array
     * (so the array length should be enough to store <code>gap+n</code> bits).
     * Good values for such gap is 64 or, maybe, 256 bits (1 or 4 <code>long</code> elements).
     * This method allows to find the best offset withing <code>0..gap-1</code> range:
     *
     * <pre>
     * long ofs = {@link #goodStartOffsetInArrayOfLongs goodStartOffsetInArrayOfLongs}(bitArray, position, 256);
     * {@link BitArray#getBits(long, long[], long, long) getBits(position, workArray, ofs, n)};
     * // processing packed bits #ofs..#ofs+n-1 in workArray
     * </pre>
     *
     * <p>We recommend to use 256 bits (or large powers of two) for the gap at the beginning of the work array.
     * Maybe, some implementations of {@link BitArray} will use alignment per 256 or more bits
     * and will provide the best performance for aligned data blocks.
     *
     * @param bitArray        the bit array
     * @param position        the index inside this bit array where you need to read or write packed bits.
     * @param maxAvailableGap the little free starting area (number of free bits)
     *                        in some <code>long[]</code> Java array, usually 128 or 256.
     * @return the offset (index of a bit) in this <code>long[]</code> array
     * inside <code>0..maxAvailableGap-1</code> range, providing the best speed
     * of accessing bits via
     * {@link BitArray#getBits(long, long[], long, long)
     * getBits(position, destArray, offset, someCount)} and
     * {@link UpdatableBitArray#setBits(long, long[], long, long)
     * setBits(position, srcArray, offset, someCount)} methods.
     * @throws NullPointerException     if <code>bitArray</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>maxAvailableGap</code> argument is negative.
     */
    public static int goodStartOffsetInArrayOfLongs(BitArray bitArray, long position, int maxAvailableGap) {
        Objects.requireNonNull(bitArray, "Null bitArray argument");
        if (maxAvailableGap < 0) {
            throw new IllegalArgumentException("Negative maxAvailableGap argument");
        }
        if (maxAvailableGap == 0) {
            return 0;
        }
        maxAvailableGap = Integer.highestOneBit(maxAvailableGap); // maximal 2^k <= maxAvailableGap
        position -= Math.max(maxAvailableGap, 64);
        long qp = bitArray.nextQuickPosition(position);
        if (qp == -1) {
            return 0;
        } else {
            if (qp < position || qp >= bitArray.length()) {
                throw new AssertionError("Illegal nextQuickPosition implementation in " + bitArray
                        + ": nextQuickPosition(" + position + ") = " + qp
                        + (qp < position ? " < " + position : " >= array.length()"));
            }
            // the copying will be quick if position=qp+offset+64*j: see comments to nextQuickPosition
            return (int) (position - qp) & (maxAvailableGap - 1);
        }
    }

    /*Repeat() boolean   ==> char,,int,,long,,double;;
               (Updatable|Mutable)Bit(Array) ==> $1Char$2,,$1PInteger$2,,$1PInteger$2,,$1PFloating$2;;
               Bit       ==> Char,,Int,,Long,,Double;;
               bit       ==> char,,int,,long,,double */

    /**
     * Inserts the specified <code>boolean</code> value at the specified position in the bit array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds 1 to their indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (index &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertBit method");
     * if (index &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("Index (" +index + ") &gt; length (" + len
     * &#32;       + ") in insertBit method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + 1);
     * array.{@link MutableArray#copy(long, long, long) copy}(index + 1, index, len - index);
     * array.{@link UpdatableBitArray#setBit(long, boolean) setBit}(index, value);
     * </pre>
     *
     * @param array an array where you need to insert element.
     * @param index index at which the specified bit is to be inserted.
     * @param value bit to be inserted.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal index value.
     */
    public static void insertBit(MutableBitArray array, long index, boolean value) {
        long len = array.length();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertBit method");
        }
        if (index > len) {
            throw new IndexOutOfBoundsException("Index (" + index + ") > length (" + len
                    + ") in insertBit method");
        }
        lengthUnsigned(array, len + 1);
        array.copy(index + 1, index, len - index);
        array.setBit(index, value);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Inserts the specified <code>char</code> value at the specified position in the char array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds 1 to their indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (index &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertChar method");
     * if (index &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("Index (" +index + ") &gt; length (" + len
     * &#32;       + ") in insertChar method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + 1);
     * array.{@link MutableArray#copy(long, long, long) copy}(index + 1, index, len - index);
     * array.{@link UpdatableCharArray#setChar(long, char) setChar}(index, value);
     * </pre>
     *
     * @param array an array where you need to insert element.
     * @param index index at which the specified char is to be inserted.
     * @param value char to be inserted.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal index value.
     */
    public static void insertChar(MutableCharArray array, long index, char value) {
        long len = array.length();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertChar method");
        }
        if (index > len) {
            throw new IndexOutOfBoundsException("Index (" + index + ") > length (" + len
                    + ") in insertChar method");
        }
        lengthUnsigned(array, len + 1);
        array.copy(index + 1, index, len - index);
        array.setChar(index, value);
    }

    /**
     * Inserts the specified <code>int</code> value at the specified position in the int array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds 1 to their indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (index &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertInt method");
     * if (index &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("Index (" +index + ") &gt; length (" + len
     * &#32;       + ") in insertInt method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + 1);
     * array.{@link MutableArray#copy(long, long, long) copy}(index + 1, index, len - index);
     * array.{@link UpdatablePIntegerArray#setInt(long, int) setInt}(index, value);
     * </pre>
     *
     * @param array an array where you need to insert element.
     * @param index index at which the specified int is to be inserted.
     * @param value int to be inserted.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal index value.
     */
    public static void insertInt(MutablePIntegerArray array, long index, int value) {
        long len = array.length();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertInt method");
        }
        if (index > len) {
            throw new IndexOutOfBoundsException("Index (" + index + ") > length (" + len
                    + ") in insertInt method");
        }
        lengthUnsigned(array, len + 1);
        array.copy(index + 1, index, len - index);
        array.setInt(index, value);
    }

    /**
     * Inserts the specified <code>long</code> value at the specified position in the long array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds 1 to their indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (index &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertLong method");
     * if (index &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("Index (" +index + ") &gt; length (" + len
     * &#32;       + ") in insertLong method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + 1);
     * array.{@link MutableArray#copy(long, long, long) copy}(index + 1, index, len - index);
     * array.{@link UpdatablePIntegerArray#setLong(long, long) setLong}(index, value);
     * </pre>
     *
     * @param array an array where you need to insert element.
     * @param index index at which the specified long is to be inserted.
     * @param value long to be inserted.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal index value.
     */
    public static void insertLong(MutablePIntegerArray array, long index, long value) {
        long len = array.length();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertLong method");
        }
        if (index > len) {
            throw new IndexOutOfBoundsException("Index (" + index + ") > length (" + len
                    + ") in insertLong method");
        }
        lengthUnsigned(array, len + 1);
        array.copy(index + 1, index, len - index);
        array.setLong(index, value);
    }

    /**
     * Inserts the specified <code>double</code> value at the specified position in the double array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds 1 to their indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (index &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertDouble method");
     * if (index &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("Index (" +index + ") &gt; length (" + len
     * &#32;       + ") in insertDouble method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + 1);
     * array.{@link MutableArray#copy(long, long, long) copy}(index + 1, index, len - index);
     * array.{@link UpdatablePFloatingArray#setDouble(long, double) setDouble}(index, value);
     * </pre>
     *
     * @param array an array where you need to insert element.
     * @param index index at which the specified double is to be inserted.
     * @param value double to be inserted.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal index value.
     */
    public static void insertDouble(MutablePFloatingArray array, long index, double value) {
        long len = array.length();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertDouble method");
        }
        if (index > len) {
            throw new IndexOutOfBoundsException("Index (" + index + ") > length (" + len
                    + ") in insertDouble method");
        }
        lengthUnsigned(array, len + 1);
        array.copy(index + 1, index, len - index);
        array.setDouble(index, value);
    }
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Inserts the specified <code>Object</code> value at the specified position in the object array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds 1 to their indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (index &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertObject method");
     * if (index &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("Index (" +index + ") &gt; length (" + len
     * &#32;       + ") in insertObject method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + 1);
     * array.{@link MutableArray#copy(long, long, long) copy}(index + 1, index, len - index);
     * array.{@link UpdatableObjectArray#set(long, Object) set}(index, value);
     * </pre>
     *
     * @param <E>   the generic type of array elements.
     * @param array an array where you need to insert element.
     * @param index index at which the specified object is to be inserted.
     * @param value object to be inserted.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal index value.
     */
    public static <E> void insertObject(MutableObjectArray<E> array, long index, E value) {
        long len = array.length();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index (" + index + ") in insertObject method");
        }
        if (index > len) {
            throw new IndexOutOfBoundsException("Index (" + index + ") > length (" + len
                    + ") in insertObject method");
        }
        lengthUnsigned(array, len + 1);
        array.copy(index + 1, index, len - index);
        array.set(index, value);
    }

    /**
     * Increase the array length by <code>count</code> and
     * shifts all elements starting from <code>position</code> to the right
     * (adds <code>count</code> to their indexes).
     * Elements <code>#position..#position+count-1</code> <i>are not changed</i>.
     * You may assign new values to these elements after calling this method
     * with help of <code>{@link UpdatableArray#setData(long, Object)
     * setData}(position, someArray)</code> or another methods.
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (position &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative position (" + position + ") in insertEmptyRange method");
     * if (count &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative number of elements (" + count
     * &#32;       + ") in insertEmptyRange method");
     * {@link #lengthUnsigned(MutableArray, long) Arrays.lengthUnsigned}(array, len + count);
     * array.{@link MutableArray#copy(long, long, long) copy}(position + count, position, len - position);
     * </pre>
     *
     * @param array    an array where you need to insert <code>count</code> elements.
     * @param position start position (inclusive) of the inserted elements.
     * @param count    number of elements that you want to insert.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static void insertEmptyRange(MutableArray array, long position, long count) {
        long len = array.length();
        if (position < 0) {
            throw new IndexOutOfBoundsException("Negative position (" + position + ") in insertEmptyRange method");
        }
        if (count < 0) {
            throw new IndexOutOfBoundsException("Negative number of elements (" + count
                    + ") in insertEmptyRange method");
        }
        if (count > 0) {
            lengthUnsigned(array, len + count);
            array.copy(position + count, position, len - position);
        }
    }

    /**
     * Removes 1 element at the specified position in the array.
     * Shifts any subsequent elements to the left (subtracts 1 from their
     * indexes).
     *
     * <p>This method is equivalent to the following call:<pre>
     * {@link #removeRange(MutableArray, long, long) Arrays.removeRange}(array, position, 1);
     * </pre>
     *
     * @param array    an array where to remove element.
     * @param position index of removed element.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal position value.
     */
    public static void remove(MutableArray array, long position) {
        removeRange(array, position, 1);
    }

    /**
     * Removes all elements at positions <code>position..position+count-1</code> in the array.
     * Shifts any subsequent elements to the left (subtracts <code>count</code> from their
     * indexes).
     *
     * <p>This method is equivalent to the following operators:<pre>
     * long len = array.length();
     * if (position &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative position (" + position + ") in removeRange method");
     * if (count &lt; 0)
     * &#32;   throw new IndexOutOfBoundsException("Negative number of elements (" + count + ") in removeRange method");
     * if (position + count &gt; len)
     * &#32;   throw new IndexOutOfBoundsException("High index (" + (position + count - 1) + ") &gt;= length (" + len
     * &#32;       + ") in removeRange method");
     * array.{@link MutableArray#copy(long, long, long) copy}(position, position + count, len - (position + count));
     * array.{@link MutableArray#length(long) length}(len - count);
     * </pre>
     *
     * @param array    an array where to remove elements.
     * @param position start position (inclusive) of the removed range.
     * @param count    number of removed elements.
     * @throws NullPointerException      if <code>array</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException for illegal position and count
     *                                   (position &lt; 0 || count &lt; 0 || position + count &gt; length).
     */
    public static void removeRange(MutableArray array, long position, long count) {
        long len = array.length();
        if (position < 0) {
            throw new IndexOutOfBoundsException("Negative position (" + position + ") in removeRange method");
        }
        if (count < 0) {
            throw new IndexOutOfBoundsException("Negative number of elements (" + count + ") in removeRange method");
        }
        if (position + count > len) {
            throw new IndexOutOfBoundsException("High index (" + (position + count - 1) + ") > length (" + len
                    + ") in removeRange method");
        }
        array.copy(position, position + count, len - (position + count));
        array.length(len - count);
    }

    /**
     * Sorts the array in increasing order, according to passed <code>comparator</code>.
     * This method is equivalent to the following call:<pre>
     * {@link ArraySorter#getQuickSorter()}.{@link
     * ArraySorter#sort sort}(0, array.{@link UpdatableArray#length() length()}, comparator, array);
     * </pre>
     * But for a case of {@link UpdatableBitArray}, this method uses more efficient algorithm.
     *
     * @param array      the sorted array.
     * @param comparator defines the order of sorted elements.
     * @throws NullPointerException if one of the arguments is {@code null}.
     */
    public static void sort(UpdatableArray array, ArrayComparator comparator) {
        Objects.requireNonNull(array, "Null array argument");
        if (array instanceof UpdatableBitArray bitArray) { // no sense to use Quick-Sort here
            MinMaxInfo mmInfo = new MinMaxInfo();
            rangeOf(null, bitArray, mmInfo);
            if (mmInfo.min() == mmInfo.max()) {
                return; // nothing to do: constant array
            }
            boolean lessElement = comparator.less(mmInfo.indexOfMin(), mmInfo.indexOfMax()) ? false : true;
            boolean greaterElement = !lessElement;
            long len = bitArray.length();
            long cardinality = Arrays.cardinality(bitArray);
            long numberOfLessElements = lessElement ? cardinality : len - cardinality;
            bitArray.fill(0, numberOfLessElements, lessElement);
            bitArray.fill(numberOfLessElements, len - numberOfLessElements, greaterElement);
        } else {
            ArraySorter.getQuickSorter().sort(0, array.length(), comparator, array);
        }
    }

    /**
     * Returns comparator allowing to sort the array in normal (increasing) order via
     * {@link #sort sort} method.
     * <p>For <code>byte</code> and <code>short</code> element types, this comparator is based on usual Java comparison:
     * it compares <i>signed</i> values, where the byte <code>-1</code> is interpreted as
     * <code>-1</code> (not <code>255</code>,
     * like in {@link ByteArray#getByte(long)} method).
     * <p>For <code>float</code> and <code>double</code> element types, this comparator is based on
     * <code>Float.compare(float, float)</code> and <code>Double.compare(double, double)</code> methods.
     * So, <code>NaN</code> is considered to be equal to itself and greater than all other float/double values
     * (including <code>POSITIVE_INFINITY</code>), and <code>0.0 </code>is considered
     * be greater than <code>-0.0</code>.
     * <p>For non-primitive arrays, this comparator will use standard <code>Comparable</code>
     * interface (all objects must implement this interface).
     *
     * @param array the array for sorting.
     * @return the comparator for sorting the array in increasing order.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static ArrayComparator normalOrderComparator(UpdatableArray array) {
        return ArraysOpImpl.defaultComparator(array, false);
    }

    /**
     * Returns comparator allowing to sort the array in reverse (decreasing) order via
     * {@link #sort sort} method.
     * <p>For <code>byte</code> and <code>short</code> element types,
     * this comparator is based on usual Java comparison:
     * it compares <i>signed</i> values, where the byte <code>-1</code>
     * is interpreted as <code>-1</code> (not <code>255</code>,
     * like in {@link ByteArray#getByte(long)} method).
     * <p>For <code>float</code> and <code>double</code> element types, this comparator is based on
     * <code>Float.compare(float, float)</code> and <code>Double.compare(double, double)</code> methods.
     * So, <code>NaN</code> is considered to be equal to itself and greater than all other float/double values
     * (including <code>POSITIVE_INFINITY</code>), and <code>0.0 </code>is considered
     * be greater than <code>-0.0</code>.
     * <p>For non-primitive arrays, this comparator will use standard <code>Comparable</code>
     * interface (all objects must implement this interface).
     *
     * @param array the array for sorting.
     * @return the comparator for sorting the array in increasing order.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static ArrayComparator reverseOrderComparator(UpdatableArray array) {
        return ArraysOpImpl.defaultComparator(array, true);
    }

    /**
     * Returns a view of the character array implementing standard <code>CharSequence</code> interface.
     * The methods of the returned view do the following:<ul>
     * <li><code>length()</code> returns <code>(int)charArray.{@link Array#length() length}()</code>;</li>
     * <li><code>charAt(int index)</code> returns
     * <code>charArray.{@link CharArray#getChar(long) getChar}(index)</code>;</li>
     * <li><code>subSequence(int start, int end)</code> returns an analogous view for
     * <code>(CharArray)charArray.{@link Array#subArray(long, long) subArray}(start, end)</code>;</li>
     * <li><code>toString()</code> {@link Arrays#toString Arrays.toString}(charArray).</li>
     * </ul>
     *
     * <p>Please note that <code>toString()</code> method of {@link CharArray} returns <i>another</i> result,
     * according to the common contract specified for {@link Array#toString()} method.
     * It is the reason why {@link CharArray} cannot implement <code>CharSequence</code> interface itself.
     *
     * @param charArray the AlgART character array.
     * @return <code>CharSequence</code> view of this character array.
     * @throws NullPointerException if <code>charArray</code> argument is {@code null}.
     */
    public static CharSequence asCharSequence(CharArray charArray) {
        return new AlgARTArrayCharSequence(charArray);
    }

    /**
     * Returns a list backed by the specified array. (Changes to
     * the returned list "write through" to the array.) This method acts
     * as bridge between AlgART-array-based and collection-based APIs,
     * together with <code>Collection.toArray</code> and {@link SimpleMemoryModel#asUpdatableArray(Object)}.
     * The returned list is serializable and implements {@link java.util.RandomAccess}.
     *
     * <p>The generic type of the list elements is <code>listElementType</code>.
     * It must be the same class as, superclass of or superinterface of
     * <code>array.{@link Array#elementType() elementType()}</code>.
     * If the array element type is primitive, the passed <code>listElementType</code>
     * must be the corresponding wrapped class or its superclass, for example, <code>Object.class</code>.
     *
     * <p>If the passed array is {@link PArray}, all primitive elements will be
     * automatically converted to/from the corresponding wrappers (<code>Boolean</code>,
     * <code>Byte</code>, etc.).
     *
     * <p>The returned list will resize if the source array will resize.
     * However, the resizing methods of the returned list will throw <code>UnsupportedOperationException</code>.
     *
     * <p>The returned list will be immutable if the source array does not
     * implement {@link UpdatableArray} interface. In this case,
     * all modification methods of the returned list will throw <code>UnsupportedOperationException</code>.
     *
     * @param <E>             the generic type of list elements.
     * @param array           the AlgART array by which the list will be backed.
     * @param listElementType the generic element type in the returned list.
     * @return a list view of this AlgART array.
     * @throws NullPointerException   if one of the arguments is {@code null}.
     * @throws ClassCastException     if not <code>listElementType.isAssignableFrom(arrayElementType)</code>,
     *                                where <code>arrayElementType</code> is
     *                                <code>array.{@link Array#elementType() elementType()}</code>
     *                                or, for a case of primitive type, its wrapper class.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>
     *                                (also thrown by <code>List</code> methods, if the array length will
     *                                exceed this threshold later).
     * @see SimpleMemoryModel#asUpdatableArray(Object)
     */
    public static <E> List<E> asList(Array array, Class<E> listElementType) {
        Objects.requireNonNull(array, "Null array argument");
        Objects.requireNonNull(listElementType, "Null listElementType argument");
        Class<?> eType = array.elementType();
        if (eType == boolean.class) {
            eType = Boolean.class;
        } else if (eType == char.class) {
            eType = Character.class;
        } else if (eType == byte.class) {
            eType = Byte.class;
        } else if (eType == short.class) {
            eType = Short.class;
        } else if (eType == int.class) {
            eType = Integer.class;
        } else if (eType == long.class) {
            eType = Long.class;
        } else if (eType == float.class) {
            eType = Float.class;
        } else if (eType == double.class) {
            eType = Double.class;
        }
        if (!listElementType.isAssignableFrom(eType)) {
            throw new ClassCastException("The passed array element type cannot be stored in List<"
                    + listElementType + "> (" + array + ")");
        }
        return new AlgARTArrayList<>(array);
    }

    /**
     * Equivalent to {@link #asList(Array, Class) asList}(array, array.elementType()}.
     *
     * @param <E>   the generic type of list elements.
     * @param array the AlgART array by which the list will be backed.
     * @return a list view of this AlgART array.
     * @throws NullPointerException   if the argument is {@code null}.
     * @throws TooLargeArrayException if the array length is greater than <code>Integer.MAX_VALUE</code>
     *                                (also thrown by <code>List</code> methods, if the array length will
     *                                exceed this threshold later).
     */
    public static <E> List<E> asList(ObjectArray<E> array) {
        Objects.requireNonNull(array, "Null array argument");
        return new AlgARTArrayList<>(array);
    }

    /**
     * Returns Java array of the underlying AlgART arrays of the given AlgART array.
     *
     * <p>More precisely, lets <code>Array[] underlyingArrays</code> are the following arrays:
     * <ol>
     * <li>all arrays passed to
     * <ul>
     * <li>{@link #asFuncArray(Func, Class, PArray...)},</li>
     * <li>{@link #asFuncArray(boolean, Func, Class, PArray...)},</li>
     * <li>{@link #asUpdatableFuncArray(net.algart.math.functions.Func.Updatable, Class, UpdatablePArray...)},</li>
     * <li>{@link #asUpdatableFuncArray(boolean, net.algart.math.functions.Func.Updatable,
     * Class, UpdatablePArray...)},</li>
     * <li>{@link #asShifted(Array, long)} or</li>
     * <li>{@link #asConcatenation(Array...)}</li>
     * </ul>method, if the <code>array</code> argument was constructed by one of those methods;</li>
     *
     * <li>the underlying array of the <code>matrix</code> (<code>matrix.</code>{@link Matrix#array() array()}),
     * if the <code>array</code> argument the {@link Matrix#array() underlying array} of its submatrix,
     * created by one of its <code>subMatrix</code> or <code>subMatr</code> methods, for example:
     * <code>array=matrix.{@link Matrix#subMatrix(long[], long[]) subMatrix(from,to)}.{@link Matrix#array()
     * array()}</code>;</li>
     *
     * <li>the <code>underlyingArrays</code> argument of the
     * {@link AbstractArray#AbstractArray(long, long, Array...) constructor of this class}
     * or the constructor of the {@link AbstractArray} inheritors from this package
     * (as {@link AbstractByteArray}, {@link AbstractUpdatableByteArray},
     * {@link AbstractCharArray}, {@link AbstractUpdatableCharArray}, etc.),
     * if the <code>array</code> argument was created by one of that constructor
     * in the custom subclass of that classes;</li>
     *
     * <li>the underlying arrays, specified in positions 1&ndash;3, of some parent AlgART array,
     * if the <code>array</code> argument is a view of that parent array created by
     * {@link Array#subArray}, {@link Array#subArr} or {@link Array#asImmutable()} methods.
     * (Please draw attention: there is no way to get the "parent" array, if we have its subarray:
     * this method returns the underlying arrays of the parent instead. An array and its subarrays
     * are considered to be peer: the containing array has no advantages over its subarrays.)</li>
     * </ol>
     *
     * <p>Then the result of this method consists of arrays, produced by operators
     * <code>underlyingArrays[k].{@link Array#asImmutable() asImmutable()}</code>,
     * if the passed array {@link Array#isImmutable is immutable}, or
     * <code>underlyingArrays[k].{@link Array#shallowClone() shallowClone()}</code> in other cases.
     *
     * <p>If the passed array is not created by one of the ways listed above,
     * this method returns the empty <code>Array[0]</code> Java array.
     * This method never returns {@code null}.
     *
     * @param array the checked AlgART array.
     * @return Java array of immutable views / shallow copies of its underlying arrays.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     * @see #getUnderlyingArraysNewStatus(Array)
     */
    public static Array[] getUnderlyingArrays(Array array) {
        return getUnderlyingArrays(array, false);
    }

    /**
     * Returns <code>boolean[]</code> array, containing
     * <code>underlyingArrays[k].{@link Array#isNew() isNew()}</code> values,
     * for all underlying arrays of the passed one.
     * Please see {@link #getUnderlyingArrays(Array)} method
     * to clarify the precise sense of "underlying arrays" term.
     * This method is necessary in addition to {@link #getUnderlyingArrays(Array)},
     * because the arrays, returned by that method, are always non-new
     * (they are produced by {@link Array#asImmutable()} method, which returns not a new array,
     * but a view of an array).
     *
     * <p>If the passed array has no underlying arrays,
     * that can be recognized by this package,
     * this method returns the empty <code>boolean[0]</code> Java array.
     * This method never returns {@code null}.
     *
     * <p>Note: we do not need an analogous method for getting
     * <code>underlyingArrays[k].{@link Array#isNewReadOnlyView() isNewReadOnlyView()}</code> values,
     * because {@link Array#asImmutable()} method always preserves the <i>new-read-only-view</i> status
     * (see the last note in the comments to {@link Array#asImmutable()}).
     *
     * @param array the checked AlgART array.
     * @return Java array of "new" statuses of all its underlying arrays.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static boolean[] getUnderlyingArraysNewStatus(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (!(array instanceof AbstractArray)) {
            return new boolean[0];
        }
        Array[] underlyingArrays = ((AbstractArray) array).underlyingArrays;
        boolean[] result = new boolean[underlyingArrays.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = underlyingArrays[k].isNew();
        }
        return result;
    }

    /**
     * Returns the number of the underlying AlgART arrays of the given AlgART array.
     * Please see {@link #getUnderlyingArrays(Array)} method
     * to clarify the precise sense of "underlying arrays" term.
     * This method is useful in addition to {@link #getUnderlyingArrays(Array)},
     * because it does not allocate any memory and works very quickly.
     *
     * <p>If the passed array has no underlying arrays,
     * that can be recognized by this package,
     * this method returns 0.
     *
     * @param array the checked AlgART array.
     * @return the number of the underlying AlgART arrays of the given AlgART array.
     * @throws NullPointerException if <code>array</code> argument is {@code null}.
     */
    public static int getUnderlyingArraysCount(Array array) {
        Objects.requireNonNull(array, "Null array argument");
        if (!(array instanceof AbstractArray)) {
            return 0;
        }
        return ((AbstractArray) array).underlyingArrays.length;
    }

    /**
     * Analog of <code>StrictMath.round</code>, but producing nearest <code>int</code> result
     * in range <code>-Integer.MAX_VALUE..+Integer.MAX_VALUE</code> (with saturation in a case of overflow).
     *
     * <p>Note: <code>Integer.MIN_VALUE</code> is also impossible in the result: it will be replaced with
     * <code>-Integer.MAX_VALUE</code> (<code>=Integer.MIN_VALUE+1</code>).
     *
     * @param value a floating-point value to be rounded to a {@code int}.
     * @return the value of the argument rounded to the nearest {@code int} value in range
     * <code>-Integer.MAX_VALUE..+Integer.MAX_VALUE</code>.
     */
    public static int round32(double value) {
        final long i = StrictMath.round(value);
        return i < -Integer.MAX_VALUE ? -Integer.MAX_VALUE : i > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) i;
    }

    /**
     * Returns the product of passed multipliers from the index, specified in <code>from</code> argument (inclusive),
     * until the index, specified in <code>to</code> argument (exclusive), i&#46;e&#46;
     * <code>multipliers[from]*multipliers[from+1]*...*multipliers[to-1]</code>,
     * if this product is in <code>-2<sup>63</sup>+1..2<sup>63</sup>-1</code> range,
     * or <code>Long.MIN_VALUE</code> (<code>-2<sup>63</sup></code>) in other cases ("overflow").
     *
     * <p>Must be <code>0&lt;=from&lt;=to&lt;=multipliers.length</code>. If <code>from==to</code>, returns 1.
     *
     * <p>Note: if the product is <code>Long.MIN_VALUE</code>, this situation cannot
     * be distinguished from the overflow.
     *
     * <p>Also note: if at least one of the passed multipliers is 0, then the result will be always 0,
     * even if product of some other multipliers is out of <code>-2<sup>63</sup>+1..2<sup>63</sup>-1</code> range.
     *
     * @param multipliers the <code>long</code> values to be multiplied.
     * @param from        the initial index in <code>array</code>, inclusive.
     * @param to          the end index in <code>array</code>, exclusive.
     * @return the product of all multipliers or <code>Long.MIN_VALUE</code> if a case of overflow.
     * @throws NullPointerException      if <code>multipliers</code> argument is {@code null}.
     * @throws IndexOutOfBoundsException if <code>from&lt;0</code> or <code>to&gt;multipliers.length</code>.
     * @throws IllegalArgumentException  if <code>from&gt;to</code>.
     * @see #longMul(long...)
     * @see #longMul(long, long)
     */
    public static long longMul(long[] multipliers, int from, int to) {
        if (to > multipliers.length || from < 0) { // simultaneously checks multipliers!=null
            throw new IndexOutOfBoundsException("Indexes out of bounds 0.." + multipliers.length
                    + ": from = " + from + ", to = " + to);
        }
        if (from > to) {
            throw new IllegalArgumentException("Illegal indexes: from = " + from + " > to = " + to);
        }
        if (from == to) {
            return 1;
        }
        long result = multipliers[from];
        for (int k = from + 1; k < to; k++) {
            result = longMul(result, multipliers[k]);
        }
        return result;
    }

    /**
     * Returns the product of all passed multipliers (<code>multipliers[0]*multipliers[1]*...</code>),
     * if it is in <code>-2<sup>63</sup>+1..2<sup>63</sup>-1</code> range,
     * or <code>Long.MIN_VALUE</code> (<code>-2<sup>63</sup></code>) in other cases ("overflow").
     * Equivalent to <code>{@link #longMul(long[], int, int) longMul}(multipliers,0,multipliers.length)</code>.
     *
     * <p>If the multipliers array is empty (<code>longMul()</code> call), returns 1.
     *
     * <p>Note: if the product is <code>Long.MIN_VALUE</code>, this situation cannot
     * be distinguished from the overflow.
     *
     * <p>Also note: if at least one of the passed multipliers is 0, then the result will be always 0,
     * even if product of some other multipliers is out of <code>-2<sup>63</sup>+1..2<sup>63</sup>-1</code> range.
     *
     * @param multipliers the <code>long</code> values to be multiplied.
     * @return the product of all multipliers or <code>Long.MIN_VALUE</code> if a case of overflow.
     * @throws NullPointerException if <code>multipliers</code> argument is {@code null}.
     * @see #longMul(long[], int, int)
     * @see #longMul(long, long)
     */
    public static long longMul(long... multipliers) {
        if (multipliers.length == 0) {
            return 1;
        }
        long result = multipliers[0];
        for (int k = 1; k < multipliers.length; k++) {
            result = longMul(result, multipliers[k]);
        }
        return result;
    }

    /**
     * Returns the product <code>a*b</code>, if it is in <code>-2<sup>63</sup>+1..2<sup>63</sup>-1</code> range,
     * or <code>Long.MIN_VALUE</code> (<code>-2<sup>63</sup></code>) in other cases ("overflow").
     *
     * <p>Note: if the product is <code>Long.MIN_VALUE</code>, this situation cannot
     * be distinguished from the overflow.
     *
     * <p>Also note: if one of the multipliers <code>a</code> and <code>b</code> is equal to the "overflow marker"
     * <code>Long.MIN_VALUE</code>, then, as it is follows from the common rule, the result of this method will also
     * be equal to <code>Long.MIN_VALUE</code> &mdash; excepting the only case, when one of the multipliers
     * <code>a</code> and <code>b</code> is zero. If <code>a==0</code> or <code>b==0</code>, the result is always 0.
     *
     * @param a the first <code>long</code> value.
     * @param b the second <code>long</code> value.
     * @return the product <code>a*b</code> or <code>Long.MIN_VALUE</code> if a case of overflow.
     * @see #longMul(long...)
     * @see #longMul(long[], int, int)
     */
    public static long longMul(long a, long b) {
        boolean sign = false;
        if (a < 0L) {
            a = -a;
            sign = true;
        }
        if (b < 0L) {
            b = -b;
            sign = !sign;
        }
        long aL = a & 0xFFFFFFFFL, aH = a >>> 32;
        long bL = b & 0xFFFFFFFFL, bH = b >>> 32;
        // |a*b| = aH*bH * 2^64 + (aH*bL + aL*bH) * 2^32 + aL*bL (all unsigned, aH,bH < 2^31)
        // let c = aH*bL + aL*bH, d = aL*bL (unsigned)
        // aH*bH must be zero (in another case, there is overflow)
        // so, |a*b| = c * 2^32 + d; it must be < 2^31
        long c, d;
        if (aH == 0L) {
            if (bH == 0L) { // |a*b| = d, if d < 2^63
                d = aL * bL;
                if (d < 0L) { // d >= 2^63
                    return Long.MIN_VALUE;
                }
                return sign ? -d : d;
            } else {
                d = aL * bL;
                if (d < 0L) { // d >= 2^63
                    return Long.MIN_VALUE;
                }
                c = aL * bH;
            }
        } else {
            if (bH == 0L) {
                d = aL * bL;
                if (d < 0L) { // d >= 2^63
                    return Long.MIN_VALUE;
                }
                c = aH * bL;
            } else { // aH != 0, bH != 0
                return Long.MIN_VALUE;
            }
        }
        // here d < 2^63; c = aH*bL or aL*bH, so, c < 2^63
        if (c > Integer.MAX_VALUE) { // c >= 2^31: it means overflow (|a*b| >= c * 2^32 >= 2^63)
            return Long.MIN_VALUE;
        }
        long result = (c << 32) + d;
        if (result < 0L) { // c * 2^32 + d >= 2^63
            return Long.MIN_VALUE;
        }
        return sign ? -result : result;
    }

    /**
     * Finds the shortest cyclic range, containing all passed positions
     * in the cyclic range <code>0..length-1</code>,
     * returns the beginning of this range as the method result,
     * cyclically subtracts it from all elements of the passed array
     * and returns the sorted array of resulting positions in the <code>positions</code> argument.
     *
     * <p>More precisely, let <code>positions</code> argument contains any numbers from <code>0</code>
     * to <code>length-1</code>. Let's consider the circle with length, equal to the <code>length</code> argument
     * (with <code>length/</code>&pi; diameter).
     * Let <i>p<sub>k</sub></i> is the point at this circle, corresponding to <code>position[<i>k</i>]</code>,
     * i.e. such point that the (anticlockwise) arc
     * from the rightmost point <i>P</i>&nbsp;=&nbsp;(<code>length/</code>2&pi;,0) of the circle to <i>p<sub>k</sub></i>
     * has the length equal to <code>position[<i>k</i>]</code>.
     * (For typical applications of this method, all points are relatively compact group at the circle,
     * usually near the starting <i>P</i> point.)
     *
     * <p>This method finds the shortest (anticlockwise) arc (<i>p<sub>i</sub></i><i>p<sub>j</sub></i>),
     * that contains all other points.
     * The start of the found arc, that is the length of (anticlockwise) arc
     * (<i>Pp<sub>i</sub></i>)=<code>positions[<i>i</i>]</code>, will be the result of the method.
     * Then this method rotates all points clockwise by this value, so that the
     * <i>p<sub>i</sub></i> is moved to the starting point <i>P</i>.
     * (In other words, it replaces every <code>positions[<i>k</i>]</code> with
     * <code>positions[<i>k</i>]-sh&gt;=0 ? positions[<i>k</i>]-sh : positions[<i>k</i>]-sh+length</code>,
     * where <code>sh=positions[<i>i</i>]</code> is the result of this method.)
     * At last, the method sorts the positions by increasing (i.e. sorts points anticlockwise).
     * The sorted array is returned in <code>positions</code> argument.
     *
     * <p>If <code>positions.length==0</code> (empty array), this method returns 0 and does nothing.
     * If <code>positions.length==1</code> (1 point at the circle), this method sets <code>positions[0]=0</code>
     * and returns the previous value of <code>positions[0]</code>.
     * If <code>length==0</code> or <code>length&lt;0</code>, this method throws <code>IllegalArgumentException</code>:
     * such length is not allowed.
     *
     * <p>This method is used by some algorithms processing several AlgART arrays, cyclically shifted
     * by {@link #asShifted(Array, long)} or similar method.
     * In this case, the <code>length</code> argument corresponds to the array length,
     * and <code>positions</code> correspond to the shifts of all arrays.
     *
     * @param length    the length of the cycle (circle).
     * @param positions some positions inside <code>0..length-1</code> range;
     *                  they will be corrected by this method: elements are sorted by increasing,
     *                  and some element (returned as the result) is cyclically subtracted
     *                  from all other elements, to provide the minimal value of the maximal element.
     * @return the value of the element of the original <code>positions</code> array,
     * which is the start of the shortest cyclic range arc.
     * @throws NullPointerException     if <code>positions</code> argument is {@code null}.
     * @throws IllegalArgumentException if <code>length&lt;=0</code>,
     *                                  or if some of passed positions are not in <code>0..length-1</code> range.
     */
    public static long compactCyclicPositions(long length, long[] positions) {
        if (length <= 0) {
            throw new IllegalArgumentException("Negative or zero length argument: " + length);
        }
        Objects.requireNonNull(positions, "Null positions array");
        for (int k = 0; k < positions.length; k++) {
            if (positions[k] < 0 || positions[k] >= length) {
                throw new IllegalArgumentException("positions[" + k + "] is out of range 0..length-1=" + (length - 1));
            }
            // must be checked before corrections of positions array to provide failure atomicity
        }
        if (positions.length == 0) {
            return 0;
        }
        if (positions.length == 1) {
            long result = positions[0];
            positions[0] = 0;
            return result;
        }
        long half = length / 2;
        boolean sorted = true;
        for (int k = 0; k < positions.length; k++) {
            positions[k] -= half; // it's a popular case when all SIGNED positions are near 0 and already sorted
            if (k > 0 && positions[k] < positions[k - 1]) {
                sorted = false;
            }
        }
        // now all positions are in -half..half-1 range
        if (!sorted) {
            java.util.Arrays.sort(positions);
        }
        // assert positions.length >= 2; // important: in another case, maxGap below will be set to length instead of 0
        long maxGap = positions[0] - positions[positions.length - 1] + length;
        int maxGapRightIndex = 0;
        if (maxGap > half) {
            assert positions[positions.length - 1] - positions[0] <= half;
            // so, maxGap is really the largest gap
        } else {
            for (int k = 1; k < positions.length; k++) {
                assert positions[k] >= positions[k - 1] : "not sorted!";
                if (positions[k] - positions[k - 1] > maxGap) {
                    maxGap = positions[k] - positions[k - 1];
                    maxGapRightIndex = k;
                }
            }
        }
        long result = positions[maxGapRightIndex];
        for (int k = 0; k < positions.length; k++) {
            positions[k] -= result;
            if (positions[k] < 0) { // impossible when maxGapRightIndex is 0
                assert maxGapRightIndex > 0;
                positions[k] += length;
            }
        }
        if (maxGapRightIndex > 0) {
            rotate(positions, positions.length - maxGapRightIndex); // restore increasing order
        }
        return result + half;
    }

    public static void splitToRanges(int[] result, int n) {
        Objects.requireNonNull(result, "Null result");
        if (result.length == 0) {
            throw new IllegalArgumentException("Result ranges array must contain at least 1 element");
        }
        if (n < 0) {
            throw new IllegalArgumentException("Negative number of elements: " + n);
        }
        final int numberOfRanges = result.length - 1;
        result[0] = 0;
        result[numberOfRanges] = n;
        for (int k = 1; k < numberOfRanges; k++) {
            result[k] = (int) ((double) n * (double) k / (double) numberOfRanges);
        }
    }

    public static void splitToRanges(long[] result, long n) {
        Objects.requireNonNull(result, "Null result");
        if (result.length == 0) {
            throw new IllegalArgumentException("Result ranges array must contain at least 1 element");
        }
        if (n < 0) {
            throw new IllegalArgumentException("Negative number of elements: " + n);
        }
        final int numberOfRanges = result.length - 1;
        result[0] = 0;
        result[numberOfRanges] = n;
        for (int k = 1; k < numberOfRanges; k++) {
            result[k] = (long) ((double) n * (double) k / (double) numberOfRanges);
        }
    }

    /**
     * Returns <code>context.{@link ArrayContext#getThreadPoolFactory() getThreadPoolFactory()}</code>
     * if <code>context!=null</code>
     * or {@link DefaultThreadPoolFactory#getDefaultThreadPoolFactory()}
     * if <code>context==null</code>.
     * It is the most typical way to get new thread pool factory for processing AlgART arrays.
     *
     * @param context some array context; can be {@code null}.
     * @return the thread pool factory, provided by this context, or the default thread pool factory
     * if the argument is {@code null}.
     */
    public static ThreadPoolFactory getThreadPoolFactory(ArrayContext context) {
        return context == null ?
                DefaultThreadPoolFactory.getDefaultThreadPoolFactory() :
                context.getThreadPoolFactory();
    }

    /**
     * Releases all resources, associated with any AlgART arrays created by this package.
     * Almost equivalent to calling {@link Array#freeResources(ArrayContext) freeResources(null)}
     * method for all arrays that were used in the application since its start
     * (including arrays that are in finalization stage).
     * The only difference is that this method, unlike {@link Array#freeResources(ArrayContext) freeResources(null)},
     * does not provide <i>flushing guarantees</i> 1 and 2, described in comments to
     * {@link Array#flushResources(ArrayContext, boolean) flushResources} method: in particular,
     * some array data may still be not stored in external resources, if it requires essential time.
     * The only purpose of this method is freeing occupied resources, not providing
     * guarantees concerning flushing data at this moment.
     *
     * <p>The same actions are automatically performed while built-in cleanup procedure
     * called while {@link #addShutdownTask(Runnable, TaskExecutionOrder)
     * shutdown hook installed by this package}.
     * However, these actions may require a long time, if
     * {@link Array#freeResources(ArrayContext) freeResources} method
     * was not accurately called after every usage of AlgART arrays.
     * It may extremely slow down JVM termination.
     * To avoid this effect, you may call this method after any large calculation stage,
     * in particular, in closing dialog of your application.
     *
     * <p>On the other hand, built-in cleanup procedure may work essentially faster than this method.
     * The reason is that all non-finalized arrays must stay alive after this method: the next access
     * to them must reload their data from external devices.
     * So, this method must save all data on external devices while releasing resources.
     * Unlike this, the built-in cleanup procedure has a right to lose any data in temporary created resources.
     * As an alternate, you may use {@link #gcAndAwaitFinalization(int)} method.
     *
     * <p>This method is thread-safe and can be executed in a parallel thread.
     */
    public static void freeAllResources() {
        DataStorage.freeAllResources();
    }

    /**
     * Performs a loop of <code>System.gc()</code>,
     * and waits for finishing all finalization tasks
     * that are scheduled by any classes of this package.
     * Does nothing if there are no scheduled finalization tasks.
     * Doesn't wait more than the specified number milliseconds.
     * Returns <code>true</code> if all finalization tasks were successfully completed
     * or <code>false</code> in a case of timeout.
     *
     * <p><i>Warning:</i> you must never call this method it there is at least one
     * <i>strongly</i> or <i>softly reachable</i> AlgART array instance,
     * that was not released by {@link Array#freeResources(ArrayContext) Array.freeResources} method.
     * If you call this method in such situation, it's possible that the method
     * will wait for the specified timeout and return <code>false</code>.
     *
     * <p>We don't recommend to call this method without necessity:
     * it can require essential time and clear all existing caches based on weak references.
     * Moreover, there is no guarantee that finalization tasks will be performed at all
     * (in particular, if there are some strongly or softly reachable AlgART arrays).
     * So, not too large <code>timeoutInMilliseconds</code> argument is absolutely necessary.
     * As an alternate, you may use {@link #freeAllResources()} method.
     *
     * <p>You may call this method at the end of the application
     * or at the end of large module
     * to avoid leaving extra temporary files and leak of disk space.
     * Though the same cleanup procedures are performed in the standard
     * {@link #addShutdownTask(Runnable, TaskExecutionOrder)
     * shutdown hook installed by this package}, but
     * this method increases the probability that all extra resources
     * will be really removed.
     *
     * <p>In current implementation, only creation arrays by
     * the {@link LargeMemoryModel large memory model} schedules some tasks:
     * namely, automatic releasing used resources and deletion of temporary files.
     *
     * @param timeoutInMilliseconds the maximal time to wait in milliseconds; please don't specify
     *                              large values here: several seconds is a good choice.
     * @return <code>true</code> if all finalization tasks were successfully completed.
     * @throws IllegalArgumentException if <code>timeoutInMilliseconds &lt;= 0</code>.
     * @throws InterruptedException     if another thread interrupted the current thread
     *                                  before or while the current thread was waiting for a notification.
     *                                  The <i>interrupted status</i> of the current thread is cleared when this
     *                                  exception is thrown.
     */
    public static boolean gcAndAwaitFinalization(int timeoutInMilliseconds) throws InterruptedException {
        if (timeoutInMilliseconds <= 0) {
            throw new IllegalArgumentException("timeoutInMilliseconds must be positive");
        }
        // Don't call freeAllResources() here! See comments "on the other hand..." in that method.
        long tFix = System.currentTimeMillis();
        long delay = 100;
        while (LargeMemoryModel.activeFinalizationTasksCount() > 0) {
            long t1 = System.currentTimeMillis();
            if (t1 - tFix > timeoutInMilliseconds) {
                return false;
            }
            // System.runFinalization(); - deprecated in new Java versions
            System.gc();
            long t2 = System.currentTimeMillis();
            if (LargeMemoryModel.activeFinalizationTasksCount() == 0) {
                break;
            }
            long sleepTime = Math.min(t2 - t1 + delay, 10000);
            sleepTime = Math.min(sleepTime, timeoutInMilliseconds - (t2 - tFix));
            //noinspection BusyWait
            Thread.sleep(Math.max(50, sleepTime));
            delay += 200;
            // Wait at least the same time, as gc() works, +delay, to avoid too high CPU usage.
            // Never wait more than 10 seconds or less than 50 ms.
        }
        for (int k = 0; k < 4; k++) {
            // To be on the safe side: finalize some additional objects
            // that could be referred from data storage finalized before.
            // Example of such objects: data file models stored in
            // LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion
            System.gc();
            Thread.sleep(50);
        }
        return true;
    }

    /**
     * Throws the specified exception, supposed to be unchecked or belonging to the specified class
     * &mdash; in other words, throws the argument,
     * if it is <code>RuntimeException</code>, <code>Error</code> or an instance of <code>exceptionClass</code>.
     * In other cases, this method throws <code>AssertionError</code>.
     *
     * <p>The implementation of this method is strictly following:
     * <pre>
     * if (exceptionClass.isInstance(exception)) {
     *     throw exceptionClass.cast(exception);
     * }
     * if (exception instanceof RuntimeException) {
     *     throw (RuntimeException) exception;
     * }
     * if (exception instanceof Error) {
     *     throw (Error) exception;
     * }
     * throw new AssertionError("Impossible exception: " + exception);
     * </pre>
     *
     * <p>This method is convenient when you need to catch all exceptions, belonging to the specified class,
     * and also all unchecked exceptions and errors,
     * then do something in connection with an exception/error,
     * and then re-throw the exception/error.
     * For example:
     *
     * <pre>
     * try {
     *     // creating some work files for storing large data
     *     // (IOException, IOError or some unexpected RuntimeException are possible)
     * } catch (Throwable e) {
     *     dispose();
     *     // - this method removes all previously created work files
     *     Arrays.{@link #throwException(Class, Throwable) throwException}(IOException.class, e);
     * }
     * </pre>
     *
     * <p>You must be sure that the <code>Throwable</code> argument of this method is really
     * <code>RuntimeException</code>, <code>Error</code> or an instance of <code>exceptionClass</code>
     * &mdash; all other exceptions are also caught by this method, but lead to throwing
     * <code>AssertionError</code>.
     *
     * @param <T>            the generic type of exception.
     * @param exceptionClass the class all checked exceptions, that can appear in the 2nd argument.
     * @param exception      some exception with the specified class or unchecked exception.
     * @throws T if the passed exception is an instance of <code>exceptionClass</code>.
     */
    public static <T extends Exception> void throwException(Class<? extends T> exceptionClass, Throwable exception)
            throws T {
        if (exceptionClass.isInstance(exception)) {
            throw exceptionClass.cast(exception);
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof Error) {
            throw (Error) exception;
        }
        throw new AssertionError("Impossible exception: " + exception);
    }

    /**
     * Throws the specified unchecked exception &mdash; in other words, throws the argument,
     * if it is <code>RuntimeException</code> or <code>Error</code>.
     * In other cases, this method throws <code>AssertionError</code>.
     *
     * <p>The implementation of this method is strictly following:
     * <pre>
     * if (exception instanceof RuntimeException) {
     *     throw (RuntimeException) exception;
     * }
     * if (exception instanceof Error) {
     *     throw (Error) exception;
     * }
     * throw new AssertionError("Impossible checked exception: " + exception);
     * </pre>
     *
     * <p>This method is convenient when you need to catch all unchecked exceptions and errors,
     * then do something in connection with an exception/error,
     * and then re-throw the exception/error.
     * For example:
     *
     * <pre>
     * try {
     *     // some your code
     * } catch (Throwable e) {
     *     exceptionOccurred = true;
     *     // - fix in some field "exceptionOccurred" the fact, that there were some exceptions
     *     Arrays.{@link #throwUncheckedException(Throwable) throwUncheckedException}(e);
     * }
     * </pre>
     *
     * <p>You must be sure that the <code>Throwable</code> argument
     * of this method is really <code>RuntimeException</code>
     * or <code>Error</code> &mdash; all other exceptions are also caught by this method, but lead to throwing
     * <code>AssertionError</code>.
     *
     * @param exception some unchecked exception.
     */
    public static void throwUncheckedException(Throwable exception) {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof Error) {
            throw (Error) exception;
        }
        throw new AssertionError("Impossible checked exception: " + exception);
    }

    /**
     * Describes when to execute the task passed to
     * {@link Arrays#addShutdownTask(Runnable, TaskExecutionOrder)} method.
     */
    public enum TaskExecutionOrder {
        BEFORE_STANDARD, AFTER_STANDARD
    }

    /**
     * Schedules the given task to be performed while system shutting down.
     *
     * <p>This package automatically registers one virtual-machine shutdown hook
     * by standard <code>Runtime.getRuntime().addShutdownHook(...)</code> call.
     * This hook performs all tasks, passed to this method, in strict order specified below
     * (unlike hooks registered by <code>addShutdownHook</code>).
     *
     * <ol>
     * <li>First of all, all tasks, passed to this method with the second argument equal to
     * {@link TaskExecutionOrder#BEFORE_STANDARD}, are performed, in the same order as calls of this method.</li>
     *
     * <li>Then the standard finalization tasks, provided by this package, are performed.
     * In particular, it includes deletion of all files returned by {@link DataFileModel#allTemporaryFiles()}
     * method for all data file models, used since the application start.
     * As a result, most of or (if there were no file blocking problems)
     * all these temporary files are removed.</li>
     *
     * <li>In fine, all tasks, passed to this method with the second argument equal to
     * {@link TaskExecutionOrder#AFTER_STANDARD}, are performed, in the same order as calls of this method.
     * </ol>
     *
     * <p>In the 3rd step, the {@link DataFileModel#allTemporaryFiles()} method
     * returns only data files that were not successfully deleted in the 2nd step
     * due to some problems (usually, because of impossibility of deleting mapped file
     * before the garbage collector will finalize all <code>MappedByteBuffer</code> instances).
     * So, it is a good idea to schedule here a task that will save paths to all these non-deleted files
     * in some text log. (You may use {@link LargeMemoryModel#allUsedDataFileModelsWithAutoDeletion()}
     * method to get a collection of all used {@link DataFileModel data file models}.)
     * While the next application start, it will be able to remove all these files.
     *
     * <p>The tasks scheduled by this method should not throw any exceptions.
     * If some exception is still thrown, the stack trace is printed to the system console
     * (by <code>printStackTrace()</code> method) and the further tasks are executed:
     * an exception does not break all shutdown hook.
     *
     * @param task          the scheduled task (its <code>run()</code> method will be called).
     * @param whenToExecute when to call the task: before or after standard ones.
     * @throws NullPointerException if <code>task</code> or <code>whenToExecute</code> is {@code null}.
     */
    public static void addShutdownTask(Runnable task, TaskExecutionOrder whenToExecute) {
        Objects.requireNonNull(task, "Null task argument");
        Objects.requireNonNull(whenToExecute, "Null whenToExecute argument");
        InternalUtils.addShutdownTask(task, whenToExecute);
    }

    /**
     * The information about the internal algorithm, which was used by copying methods of this package:
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)},
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)},
     * {@link Arrays#compareAndCopy(ArrayContext, UpdatableArray, Array)},
     * {@link Matrices#copy(ArrayContext, Matrix, Matrix)}, etc.
     * This information is returned by {@link Arrays.CopyStatus#algorithm()} method of the object,
     * returned by all these copying methods.
     * Can be useful for profiling and debugging needs.
     */
    public enum CopyAlgorithm {
        /**
         * Usual parallel copying algorithm, implemented by {@link Arrays.Copier} class or
         * by the analogous private class,
         * used by {@link Arrays#compareAndCopy(ArrayContext, UpdatableArray, Array)} method.
         */
        SIMPLE,

        /**
         * The optimized algorithm, used by
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * it preloads the underlying array of the source one into Java memory before copying.
         * May be used if the source array is a view of some underlying array,
         * created not by {@link SimpleMemoryModel}: some views of such arrays, for example,
         * geometrical matrix transformations, work much faster if the underlying array
         * is located in a usual Java memory ({@link SimpleMemoryModel}).
         */
        BUFFERING_WHOLE_ARRAY,

        /**
         * The optimized algorithm, used by
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * it splits the underlying array of the source one into "layers" (by the last coordinate,
         * if it is a {@link Matrix matrix}) and preloads every layer into Java memory before copying.
         * May be used if the source array is a view of some underlying array,
         * created not by {@link SimpleMemoryModel}: some views of such arrays, for example,
         * geometrical matrix transformations, work much faster if the underlying array
         * is located in a usual Java memory ({@link SimpleMemoryModel}).
         */
        BUFFERING_LAYERS,

        /**
         * The optimized algorithm, used by
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * it splits the underlying array of the source one into a regular grid of "tiles" (by all coordinates,
         * if it is a {@link Matrix matrix}) and preloads every tile into Java memory before copying.
         * May be used if the source array is a view of some underlying array,
         * created not by {@link SimpleMemoryModel}: some views of such arrays, for example,
         * geometrical matrix transformations, work much faster if the underlying array
         * is located in a usual Java memory ({@link SimpleMemoryModel}).
         */
        REGULAR_BUFFERING_TILES,

        /**
         * The optimized algorithm, used by
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} method in "non-strict" mode:
         * it recursively splits the underlying array of the source one into a tree of "tiles" (by all coordinates,
         * if it is a {@link Matrix matrix}) and preloads every tile, which is little enough,
         * into Java memory before copying.
         * May be used if the source array is a view of some underlying array,
         * created not by {@link SimpleMemoryModel}: some views of such arrays, for example,
         * geometrical matrix transformations, work much faster if the underlying array
         * is located in a usual Java memory ({@link SimpleMemoryModel}).
         */
        RECURSIVE_BUFFERING_TILES,

        TILING,
        UNTILING,
    }

    /**
     * The information about the copying, returned by copying methods of this package:
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)},
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)},
     * {@link Matrices#copy(ArrayContext, Matrix, Matrix)}, etc.
     */
    public static class CopyStatus {
        private final CopyAlgorithm algorithm;
        private final boolean strictMode;

        CopyStatus(CopyAlgorithm algorithm, boolean strictMode) {
            this.algorithm = algorithm;
            this.strictMode = strictMode;
        }

        /**
         * Returns the algorithm which was used for copying.
         *
         * @return the algorithm which was used for copying.
         */
        public CopyAlgorithm algorithm() {
            return algorithm;
        }

        /**
         * Returns <code>true</code> if and only if the array was copied in "strict" mode,
         * i&#46;e&#46; by
         * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} or
         * {@link Matrices#copy(ArrayContext, Matrix, Matrix, int, boolean)} method with
         * the argument <code>strictMode=true</code> or by any other method of copying arrays.
         * Note: if this value is <code>false</code>, it does not guarantee that some "non-strict" algorithm
         * was really used, it does only mean that the corresponding <code>strictMode</code> argument
         * of the copying method was <code>false</code>.
         *
         * @return <code>true</code> the array was copied strictly, <code>false</code> if it was copied
         * by {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} or
         * {@link Matrices#copy(ArrayContext, Matrix, Matrix, int, boolean)} method
         * with <code>strictMode=false</code>.
         */
        public boolean strictMode() {
            return strictMode;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation and usually contains
         * the indexes and values of the found minimum and maximum.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return "copied by " + algorithm + (strictMode ? "" : ", non-strict mode");
        }
    }

    /**
     * The information about the copying, returned by
     * {@link Arrays#compareAndCopy(ArrayContext, UpdatableArray, Array)} and
     * {@link Matrices#compareAndCopy(ArrayContext, Matrix, Matrix)} methods.
     */
    public static class ComparingCopyStatus extends CopyStatus {
        private final boolean changed;

        ComparingCopyStatus(CopyAlgorithm algorithm, boolean strictMode, boolean changed) {
            super(algorithm, strictMode);
            this.changed = changed;
        }

        /**
         * Returns <code>true</code> if and only if at least one element of the destination
         * array was changed as a result of copying from the source array,
         * in terms of the {@link Array#equals(Object)} method.
         *
         * @return whether the destination array was changed as a result of copying.
         */
        public boolean changed() {
            return changed;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation and usually contains
         * the indexes and values of the found minimum and maximum.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return super.toString() + ", " + (changed ? "changed" : "not changed");
        }
    }

    /**
     * <p>The helper class for {@link Arrays#rangeOf(PArray, MinMaxInfo)} method, containing information
     * about the minimum and maximum in some AlgART array.</p>
     *
     * <p>The instance of this class contains the following information:</p>
     *
     * <ul>
     * <li>{@link #indexOfMin() index of minimal} and {@link #indexOfMax() index of maximal}
     * elements in the AlgART array;</li>
     *
     * <li>the range {@link #range()} min..max}: the values of minimal and maximal elements;</li>
     *
     * <li>{@link #isInitialized() initialized} boolean flag, which is <code>false</code> after
     * creating the instance by the constructor
     * (when this object does not contain useful information) and
     * which is set to <code>true</code> before filling this object by real information at the end of executing
     * {@link Arrays#rangeOf(PArray, MinMaxInfo)} method;</li>
     * </ul>
     *
     * <p>The only way to create an instance of this class is the constructor without arguments,
     * that creates an uninitialized instance.
     * The only way to change the information stored in this instance is calling
     * {@link Arrays#rangeOf(PArray, MinMaxInfo)} method, that fills the instance by the actual information
     * and changes its state to initialized.</p>
     *
     * <p>This class is <b>thread-safe</b>: you may use the same instance of this class
     * in several threads. The state of the instance is always consistent:
     * all information, stored in it, including <code>initialized</code> flag,
     * is always changed simultaneously in a synchronized block.</p>
     */
    public static final class MinMaxInfo {
        private final Object lock = new Object();
        private boolean initialized = false;
        private long indexOfMin, indexOfMax;
        private Range range;
        private boolean allNaN = false;

        /**
         * Creates new {@link #isInitialized() uninitialized} instance of this class.
         * You must call {@link Arrays#rangeOf(PArray, MinMaxInfo)} method for this instance
         * before you will be able to use it.
         */
        public MinMaxInfo() {
        }

        /**
         * Returns <code>true</code> if and only this object is <i>initialized</i>,
         * that is if it was passed to {@link Arrays#rangeOf(PArray, MinMaxInfo)} method at least once
         * and this method was successfully finished.
         * If the object is not initialized, then all its methods, excepting
         * this one and methods of the basic <code>Object</code> class (<code>toString</code>, <code>equals</code>,
         * etc.)
         * throw <code>IllegalStateException</code>.
         *
         * @return whether this object is <i>initialized</i>.
         */
        public boolean isInitialized() {
            synchronized (lock) {
                return initialized;
            }
        }

        /**
         * Returns the index of the minimal array element stored in this object
         * or &minus;1 if an array is empty (<code>array.length()==0</code>).
         *
         * @return the index of the minimal array element stored in this object.
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public long indexOfMin() {
            synchronized (lock) {
                checkInitialized();
                return this.indexOfMin;
            }
        }

        /**
         * Returns the index of the maximal array element stored in this object
         * or &minus;1 if an array is empty (<code>array.length()==0</code>).
         *
         * @return the index of the maximal array element stored in this object.
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public long indexOfMax() {
            synchronized (lock) {
                checkInitialized();
                return this.indexOfMax;
            }
        }

        /**
         * Returns the value of the minimal array element stored in this object.
         *
         * @return the value of the minimal array element stored in this object.
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public double min() {
            synchronized (lock) {
                checkInitialized();
                return this.range.min();
            }
        }

        /**
         * Returns the value of the maximal array element stored in this object.
         *
         * @return the value of the maximal array element stored in this object.
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public double max() {
            synchronized (lock) {
                checkInitialized();
                return this.range.max();
            }
        }

        /**
         * Returns the values of both minimal and maximal array element stored in this object.
         *
         * <p>Note: for array of floating-points values, where all elements are <code>NaN</code>,
         * this method returns special value <code>Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY</code>
         * (because {@link Range} class cannot store <code>NaN</code> limits).
         * You can check this situation by {@link #allNaN()} method.
         *
         * @return the values of both minimal and maximal array element stored in this object.
         * @throws IllegalStateException if this instance is not {@link #isInitialized() initialized} yet.
         */
        public Range range() {
            synchronized (lock) {
                checkInitialized();
                return this.range;
            }
        }

        /**
         * Returns <code>true</code> if the array is {@link PFloatingArray} and all its elements are <code>NaN</code>.
         * In this case, {@link #range()} contains a special value
         * <code>Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY</code>.
         *
         * @return <code>true</code> if and only if the array is floating-point and is filled by <code>NaN</code>.
         */
        public boolean allNaN() {
            return allNaN;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation and usually contains
         * the indexes and values of the found minimum and maximum.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            synchronized (lock) {
                if (!initialized) {
                    return "not initialized MinMaxInfo";
                }
                return allNaN ? "NaN-filled" :
                        range + " (minimum at " + indexOfMin + ", maximum at " + indexOfMax + ")";
            }
        }

        /**
         * Returns the hash code of this range.
         *
         * @return the hash code of this range.
         */
        public int hashCode() {
            synchronized (lock) {
                int iMin = (int) indexOfMin * 37 + (int) (indexOfMin >>> 32);
                int iMax = (int) indexOfMax * 37 + (int) (indexOfMax >>> 32);
                return (iMin * 37 + iMax) * 37 + range.hashCode()
                        + (initialized ? 157 : 11) + (allNaN ? 73812 : 327);
            }
        }

        /**
         * Indicates whether some other instance of this class is equal to this instance.
         * Returns <code>true</code> if and only if <code>obj instanceof {@link MinMaxInfo MinMaxInfo}</code>
         * and the information, stored in both instances, is fully identical.
         *
         * @param obj the object to be compared for equality with this instance.
         * @return <code>true</code> if the specified object is equal to this one.
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof MinMaxInfo o)) {
                return false;
            }
            synchronized (lock) {
                synchronized (o.lock) {
                    return o.initialized = this.initialized
                            && o.indexOfMin == this.indexOfMin && o.indexOfMax == this.indexOfMax
                            && o.range.equals(this.range)
                            && o.allNaN == this.allNaN;
                }
            }
        }


        void setEmpty() {
            synchronized (lock) {
                this.indexOfMin = -1;
                this.indexOfMax = -1;
                this.range = Range.valueOf(0.0, 0.0);
                this.allNaN = false;
                this.initialized = true;
            }
        }

        void setAll(long indexOfMin, long indexOfMax, PArray src) {
            // It is called for non-empty array only. Possible cases.
            // 1) An array contains some "ordinary" values (the only possible case for PFixedArray).
            //    In this case, indexOfMin and indexOfMax are initialized properly, and min/max contain
            //    correct values ( < or > comparison worked at least once).
            // 2) An array contains only special values NEGATIVE_INFINITY / POSITIVE_INFINITY / NaN,
            //    and both NEGATIVE_INFINITY and POSITIVE_INFINITY appear. In this case,
            //    indexOfMin and indexOfMax are also initialized properly: indexOfMin at NEGATIVE_INFINITY,
            //    indexOfMax at NEGATIVE_INFINITY.
            //    Remember that NaN < min and NaN > max always return false.
            // 3) An array contains only NEGATIVE_INFINITY / NaN, and NEGATIVE_INFINITY appears.
            //    In this case, indexOfMax == -1, but indexOfMin is initialized properly at NEGATIVE_INFINITY.
            // 4) An array contains only POSITIVE_INFINITY / NaN, and POSITIVE_INFINITY appears.
            //    In this case, indexOfMin == -1, but indexOfMax is initialized properly at POSITIVE_INFINITY.
            // 5) An array contains only NaN. In this case, indexOfMin == indexOfMax == -1.
            // It is a really special case, when we cannot create any Range.
            synchronized (lock) {
                if (indexOfMin == -1 && indexOfMax == -1) {
                    // case 5): all NaN
                    this.indexOfMin = this.indexOfMax = 0;
                    this.range = Range.valueOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                    // - special "strange" range (Range class does not allow to use NaN)
                    this.allNaN = true;
                } else {
                    double min, max;
                    if (indexOfMin == -1) {
                        // case 4): float[]/double[] array filled by POSITIVE_INFINITY / NaN
                        indexOfMin = indexOfMax; // - position of POSITIVE_INFINITY
                        min = max = src.getDouble(indexOfMax);
                        assert min == Double.POSITIVE_INFINITY;
                    } else if (indexOfMax == -1) {
                        // case 3): float[]/double[] array filled by NEGATIVE_INFINITY / NaN
                        indexOfMax = indexOfMin; // - position of NEGATIVE_INFINITY
                        min = max = src.getDouble(indexOfMin);
                        assert max == Double.NEGATIVE_INFINITY;
                    } else {
                        min = src.getDouble(indexOfMin);
                        max = src.getDouble(indexOfMax);
                        if (min > max) {
                            throw new AssertionError("Illegal min and max");
                        }
                    }
                    this.indexOfMin = indexOfMin;
                    this.indexOfMax = indexOfMax;
                    this.range = Range.valueOf(min, max);
                    this.allNaN = false;
                }
                this.initialized = true;
            }
        }

        private void checkInitialized() {
            if (!initialized) {
                throw new IllegalStateException("This instance is not initialized by rangeOf method yet");
            }
        }
    }

    /**
     * <p>The class simplifying the parallel processing a large AlgART array in several threads,
     * where each thread process a set of ranges of the source array ({@link Array#subArray Array.subArray}).
     * Multi-thread processing can be very important on multiprocessor or multicore computers
     * for complex processing algorithm.
     * In addition, this class provides an ability to interrupt calculations and
     * show the executing progress via the {@link ArrayContext}.
     * This class lies in the base of such methods as
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)},
     * {@link Arrays#rangeOf(ArrayContext, PArray)},
     * {@link Arrays#sumOf(ArrayContext, PArray)}, etc.
     * and allows to easily create analogous multithreading context-based algorithms.</p>
     *
     * <p>The usage of this class is very simple. You just need to create an instance
     * by some of constructors and then call the only method {@link #process()}.
     * This method performs full processing the source AlgART array
     * in the necessary number of threads, according the information
     * from the context passed to the constructor.
     * See the comments to the constructor for detailed specification of using the context information.</p>
     *
     * <p>This class has the only abstract method {@link #processSubArr(long, int, int)},
     * that should perform actual processing of some range of the source array.
     * You must override this method to specify the processing algorithm.
     * You may also override some other methods to clarify the behavior of the basic {@link #process()}
     * method.</p>
     */
    public static abstract class ParallelExecutor {
        /**
         * The reference to the destination array. Equal to the corresponding argument of the constructor.
         * Can be {@code null} for algorithms that do not produce any destination arrays.
         */
        protected final UpdatableArray dest;

        /**
         * The reference to the source processed array. Equal to the corresponding argument of the constructor.
         */
        protected final Array src;

        /**
         * The maximal block size for processing array by
         * {@link #processSubArr(long, int, int)} method.
         * Equal to the corresponding argument of the constructor.
         */
        protected final int blockSize;

        /**
         * The number of parallel tasks that will be used by {@link #process()} method.
         * Equal to the corresponding argument of the constructor, if this argument is non-zero,
         * or calculated automatically if that argument is 0.
         */
        protected final int numberOfTasks;

        /**
         * The number of ranges that the source array is split into by {@link #process()} method.
         * Equal to the corresponding argument of the constructor, if this argument is non-zero,
         * or {@link #recommendedNumberOfRanges calculated automatically} if that argument is 0,
         * but this value is automatically increased to the nearest value multiple of {@link #numberOfTasks}.
         *
         * <p>The following conditions are always true:
         * <code>{@link #numberOfRanges}&gt;={@link #numberOfTasks}</code> and
         * <code>{@link #numberOfRanges}%{@link #numberOfTasks}==0</code>.
         *
         * @see #correctNumberOfRanges(long, int)
         */
        protected final long numberOfRanges;

        private final ArrayContext context;
        private final long length;
        private final double approximateRangeLength;
        private final ThreadPoolFactory threadPoolFactory;
        private final ReentrantLock lock = new ReentrantLock();
        private final long[] readyCountPerTask, lengthPerTask;
        private volatile boolean interruptionRequested;
        private RuntimeException interruptionReason = null;
        private long lastInterruptionTime = Long.MIN_VALUE;
        private long lastProgressTime = Long.MIN_VALUE;

        /**
         * Creates new instance of this class, intended for processing the passed <code>src</code> AlgART array.
         *
         * <p>The <code>context</code> argument, if it is not {@code null}, allows to clarify
         * the behavior of this class. If it is {@code null}, the default behavior is used.
         * Please see comments for {@link #process()} and {@link #processRange(long, long, int, long)}
         * method for precise specification of using the context.
         *
         * <p>The <code>dest</code> argument is not required and can be {@code null}: maybe, the algorithm
         * does not fill any resulting array (for example, {@link Arrays#sumOf(ArrayContext, PArray)} method).
         * But if it is specified, this class supposes that the implementation of its
         * {@link #processSubArr(long position, int count, int threadIndex)} abstract method
         * will save results in the corresponding fragment of <code>dest</code> array:
         * <code>dest.{@link UpdatableArray#subArr subArr}(position,count)</code>.
         * In this case, {@link #process} method will, maybe, perform some resource optimization
         * connected with the destination array, for example,
         * {@link Array#flushResources(ArrayContext) flush} the filled regions.
         * If this argument is not {@code null}, the lengths of the source and destination arrays
         * must be identical.
         *
         * @param context        the context of processing; can be {@code null}.
         * @param dest           the destination array or {@code null} if this algorithm does not write data
         *                       to any resulting AlgART array.
         * @param src            the source processed array.
         * @param blockSize      the maximal length of subregions that can be processed by
         *                       {@link #processSubArr(long, int, int)} method.
         * @param numberOfTasks  the desired number of parallel tasks or 0 if you want to determine
         *                       this number automatically, from the passed context or (if it is {@code null})
         *                       from {@link DefaultThreadPoolFactory} instance.
         * @param numberOfRanges the desired number of ranges for splitting the source array.
         *                       If this argument is positive, but
         *                       <code>numberOfRanges%{@link #numberOfTasks}!=0</code>,
         *                       it is replaced with the minimal integer, multiple of {@link #numberOfTasks},
         *                       greater than this argument.
         *                       If this argument is 0, then the number of ranges is chosen automatically as
         *                       the minimal integer, multiple of {@link #numberOfTasks}
         *                       and greater than or equal to
         *                       {@link #recommendedNumberOfRanges recommendedNumberOfRanges(src,&nbsp;true)}.
         * @throws NullPointerException     if the <code>src</code> argument is {@code null}.
         * @throws IllegalArgumentException if <code>blockSize &lt;= 0</code>,
         *                                  or if <code>numberOfTasks &lt; 0</code>,
         *                                  or if <code>numberOfRanges &lt; 0</code>.
         * @throws SizeMismatchException    if <code>dest&nbsp;!=&nbsp;null</code> and
         *                                  <code>dest.length()&nbsp;!=&nbsp;src.length()</code>.
         * @see #correctNumberOfRanges(long, int)
         */
        protected ParallelExecutor(
                ArrayContext context,
                UpdatableArray dest,
                Array src,
                int blockSize,
                int numberOfTasks,
                long numberOfRanges) {
            Objects.requireNonNull(src, "Null src argument");
            if (blockSize <= 0) {
                throw new IllegalArgumentException("Negative or zero blockSize=" + blockSize);
            }
            if (numberOfTasks < 0) {
                throw new IllegalArgumentException("Negative numberOfTasks=" + numberOfTasks);
            }
            if (numberOfRanges < 0) {
                throw new IllegalArgumentException("Negative numberOfRanges=" + numberOfRanges);
            }
            this.context = context;
            this.dest = dest;
            this.src = src;
            if (dest != null && src.length() != dest.length()) {
                throw new SizeMismatchException("dest.length() and src.length() mismatch");
            }
            this.length = src.length();
            this.blockSize = blockSize;
            this.threadPoolFactory = Arrays.getThreadPoolFactory(context);
            this.numberOfTasks = numberOfTasks > 0 ? numberOfTasks :
                    getClass() == Copier.class
                            && !src.isLazy() ? 1
                            // source array is probably file or memory: no reasons for multithreading copying
                            : Math.max(1, this.threadPoolFactory.recommendedNumberOfTasks(src));
            // assert this.numberOfTasks > 0;
            long m = numberOfRanges > 0 ? numberOfRanges : recommendedNumberOfRanges(src, true);
            assert m > 0 : "A bug in recommendedNumberOfRanges(Array): it returns non-positive value " + m;
            this.numberOfRanges = correctNumberOfRanges(m, this.numberOfTasks);
            this.approximateRangeLength = (double) length / (double) this.numberOfRanges;
            this.readyCountPerTask = new long[this.numberOfTasks]; // will be filled in process()
            this.lengthPerTask = new long[this.numberOfTasks]; // will be filled in process()
        }

        /**
         * The default (recommended) number of ranges for splitting the given AlgART array.
         * It is chosen as the minimal positive number <code>n</code> so, that:
         *
         * <ol>
         * <li>the subarray of <code>src</code> array, consisting of <code>Math.ceil((double)src.length()/n)</code>
         * elements, occupies not greater than {@link Arrays.SystemSettings#maxMultithreadingMemory()}
         * bytes (1&nbsp;MB by default).
         * (This value is stored while initializing {@link Arrays} class;
         * if some exception occurred while calling <code>Integer.getInteger</code>, default value 1048576 is used.)
         *
         * <li>if <code>recursive</code> argument is <code>true</code>,
         * the same condition is true for all underlying arrays,
         * returned by {@link Arrays#getUnderlyingArrays(Array) Arrays.getUnderlyingArrays(src)} call.</li>
         * </ol>
         *
         * <p>In other words, the ranges for splitting should not be too large (by default, 1 MB is a maximum).
         * It allows to guarantee that the ranges, which are simultaneously processed by several threads,
         * are located not too far from each other. It can be important for large arrays located in disk files.
         *
         * <p>The size estimation is performed via {@link Arrays#sizeOf(Array)} method.
         * If estimation is impossible (that method returns <code>-1</code>),
         * we suppose that every element requires 4 bytes.
         *
         * <p>The <code>recursive</code> argument allows better
         * estimation of the memory used by subarray of this array.
         * On the other hand, if this argument is <code>false</code>, the result is more obvious: it depends only
         * on <code>src.elementType()</code> (if it is not
         * a {@link CombinedMemoryModel#isCombinedArray combined array})
         * and does not depend on the internal nature of this array.
         *
         * @param src       the source processed array.
         * @param recursive whether this method should check the
         *                  {@link Arrays#getUnderlyingArrays(Array) underlying arrays}.
         * @return the recommended number of ranges for splitting this array by this class.
         * @throws NullPointerException if <code>src</code> argument is {@code null}.
         */
        public static long recommendedNumberOfRanges(Array src, boolean recursive) {
            Objects.requireNonNull(src, "Null src argument");
            long rangeLen = Math.max(1, lengthOf(src, SystemSettings.maxMultithreadingMemory()));
            long result = Math.max(1, (src.length() - 1) / rangeLen + 1);
            // =max(1, ceil(length/rangeLen)). Math.max here is necessary: if length=0, then we have -1/1+1=0.
            if (recursive) {
                for (Array a : Arrays.getUnderlyingArrays(src, true)) {
                    result = Math.max(result, recommendedNumberOfRanges(a, true));
                }
            }
            return result;
        }

        /**
         * Returns the nearest integer, greater or equal to <code>numberOfRanges</code> and
         * multiple of <code>numberOfTasks</code>.
         * Used for calculating {@link #numberOfRanges} field.
         *
         * @param numberOfRanges the desired number of ranges.
         * @param numberOfTasks  the desired number of tasks.
         * @return the number of ranges that will be really used by this class.
         * @throws IllegalArgumentException if <code>numberOfTasks &lt;= 0</code> or
         *                                  if <code>numberOfRanges &lt;= 0</code>.
         */
        public static long correctNumberOfRanges(long numberOfRanges, int numberOfTasks) {
            if (numberOfTasks <= 0) {
                throw new IllegalArgumentException("Zero or negative numberOfTasks=" + numberOfTasks);
            }
            if (numberOfRanges <= 0) {
                throw new IllegalArgumentException("Zero or negative numberOfRanges=" + numberOfRanges);
            }
            return numberOfRanges % numberOfTasks == 0 ? numberOfRanges :
                    numberOfRanges - numberOfRanges % numberOfTasks + numberOfTasks;
        }

        /**
         * Returns context, passed as the argument of the constructor.
         *
         * @return context.
         */
        public ArrayContext context() {
            return context;
        }

        /**
         * Performs full processing the source AlgART array, passed to the constructor.
         *
         * <p>This method uses a thread pool for performing calculations:
         * <code>java.util.concurrent.ExecutorService</code>.
         * The full processing task is split into <i>M</i>={@link #numberOfTasks} tasks,
         * and the source array is split into <i>n</i>={@link #numberOfRanges} ranges
         * (<i>n</i>&gt;=<i>M</i>, <i>n</i>%<i>M</i>=0).
         * It is possible to use <i>M</i>=1 (recommended settings for 1 CPU kernel),
         * but even in this case we recommend to choice <i>n</i> big enough to split the array
         * into not too large regions: it can help this method to optimize preloading and flushing
         * external disk resources.
         * The lengths of ranges are chosen equal or almost equal, with the only condition
         * that the first index of every range (splitting position) is multiple of {@link #granularity()}.
         * Each task #<i>k</i>, 0&lt;=<i>k</i>&lt;<i>M</i>,
         * processes a set of regions of the <code>src</code> array with step <i>M</i> regions,
         * i.e. the regions #<i>k</i>, #<i>k</i>+M, #<i>k</i>+2M, ...
         * Processing each region means just a call of {@link #processRange(long, long, int, long)} method
         * for the corresponding region of the array.
         *
         * <p>All tasks are submitted to the thread pool, and then this method
         * waits until all tasks will be completed.
         * If some task throws an exception, this exception is stored and the internal flag
         * "interruptionRequested" is set, that should lead to interruption of
         * all {@link #processRange(long, long, int, long)} methods (due to calling
         * {@link #checkInterruption()} by them).
         * This exception will be re-thrown by this method before finishing.
         *
         * <p>In addition to calling {@link #checkInterruption()},
         * this method also interrupts all running threads, if the current thread,
         * that calls this method,
         * is interrupted by the standard <code>Thread.interrupt()</code> call.
         * In this (and only this) case this method throws <code>java.io.IOError</code>.
         * Usually, you should avoid interrupting the threads, processing AlgART arrays,
         * via <code>Thread.interrupt()</code> technique: see the package description
         * about runtime exceptions issue.
         *
         * <p>The number of tasks and regions and the sizes of regions may be specified
         * by arguments of the constructor or may be chosen automatically (if that arguments are 0).
         *
         * <p>The thread pool, performing the multithreading processing,
         * is returned and (before finishing the processing) released by the methods of
         * <code>context.{@link ArrayContext#getThreadPoolFactory() getThreadPoolFactory()}</code> object,
         * where <code>context</code> is the argument of the constructor.
         * If <code>context</code> argument is {@code null},
         * the {@link DefaultThreadPoolFactory} instance is created and used instead
         * <code>context.{@link ArrayContext#getThreadPoolFactory() getThreadPoolFactory()}</code>.
         *
         * <p>Note: if the number of parallel tasks is 1,
         * this method <i>performs processing in the current thread</i>
         * and does not use the thread pool at all.
         *
         * <p>At the end, in <code>finally</code> section, this method calls
         * {@link #finish()} method. Please note: if some <code>RuntimeException</code> B is thrown
         * by {@link #finish()} <i>and</i> there were some other exception A while executing
         * the main body of this {@link #process()} method, than the finishing exception B is ignored,
         * but the main (usually more important) exception A is thrown.
         *
         * <p>This method does nothing if the length the source array is 0.
         */
        public void process() {
            if (this.length == 0) {
                return;
            }
            Runnable[] tasks = new Runnable[numberOfTasks];
            assert numberOfRanges >= numberOfTasks;
            assert numberOfRanges % numberOfTasks == 0;
            interruptionRequested = false;

            if (PROCESS_SYNCHRONIZATION_ALGORITHM == ProcessSynchronizationAlgorithm.NO_SYNCHRONIZATION) {
                for (int threadIndex = 0; threadIndex < numberOfTasks; threadIndex++) {
                    long summaryLen = 0;
                    if (numberOfRanges > MAX_NUMBER_OF_RANGES_FOR_PRECISE_LENGTH_PER_TASK_ESTIMATION) {
                        summaryLen = length / numberOfTasks;
                        // skipping the following loop: it can spend a lot of time, but useful only for progress bar
                    } else {
                        for (long rangeIndex = threadIndex; rangeIndex < numberOfRanges; rangeIndex += numberOfTasks) {
                            summaryLen += rangeLength(rangeIndex);
                        }
                    }
                    readyCountPerTask[threadIndex] = 0;
                    lengthPerTask[threadIndex] = summaryLen;
                }
                boolean noExceptions = false;
                try {
                    for (long rangeStart = 0; rangeStart < numberOfRanges; rangeStart += numberOfTasks) {
                        for (int threadIndex = 0; threadIndex < numberOfTasks; threadIndex++) {
                            final int ti = threadIndex;
                            final long ri = rangeStart + threadIndex;
                            final long rFrom = rangeFrom(ri);
                            final long rTo = rangeTo(ri);
                            src.subArray(rFrom, rTo).loadResources(null);
                            tasks[ti] = () -> {
                                Thread th = Thread.currentThread();
                                if (th instanceof ThreadForRanges) {
                                    // so, it's our thread, created by the custom ThreadFactory below
                                    th.setName("thread #" + (ti + 1) + "/" + numberOfTasks
                                            + ", range # " + (ri + 1) + "/" + numberOfRanges + " (" + src + ")");
                                }
                                try {
                                    processRange(rFrom, rTo, ti, ri);
                                } catch (Throwable ex) {
                                    interruptionRequested = true;
                                    throwUncheckedException(ex);
                                }
                            };
                        }
                        if (numberOfTasks == 1) {
                            tasks[0].run();
                            // to be on the safe side, we do not hope on the similar behavior for
                            // the implementation of performTasks method in threadPoolFactory object
                        } else {
                            ThreadFactory threadFactory = ThreadForRanges::new;
                            threadPoolFactory.performTasks(src, threadFactory, tasks);
                        }
                        if (interruptionReason != null) {
                            throw interruptionReason;
                        }
//                    if (dest != null) {
//                        final long rFrom = rangeFrom(rangeStartIndex);
//                        final long rTo = rangeTo(rangeStartIndex + numberOfTasks - 1);
//                        dest.subArray(rFrom, rTo).flushResources(null);
//                    }
                        noExceptions = true;
                    }
                } finally {
                    try {
                        finish();
                    } catch (RuntimeException ex) {
                        if (noExceptions) {
                            //noinspection ThrowFromFinallyBlock
                            throw ex;
                        }
                    }
                }
                // end of NO_SYNCHRONIZATION algorithm
            } else {

                // BAD SOLUTION: synchronization of long-working loadResources slows down the system
                final AtomicLong readyRanges = new AtomicLong(0);
                final Object synchronizer = new Object();
                for (int threadIndex = 0; threadIndex < numberOfTasks; threadIndex++) {
                    final int ti = threadIndex;
                    tasks[ti] = () -> {
                        Thread th = Thread.currentThread();
                        if (th instanceof ThreadForRanges) {
                            // so, it's our thread, created by the custom ThreadFactory below
                            th.setName("thread #" + (ti + 1) + "/" + numberOfTasks + " (" + src + ")");
                        }
                        for (long rangeIndex = ti; rangeIndex < numberOfRanges; rangeIndex += numberOfTasks) {
                            if (interruptionRequested) {
                                break;
                            }
                            if (PROCESS_SYNCHRONIZATION_ALGORITHM == ProcessSynchronizationAlgorithm.BEGINS) {
                                synchronized (synchronizer) {
                                    long startIndexInThisSeries = rangeIndex - ti;
                                    // we start new series per numberOfTasks ranges simultaneously
                                    while (readyRanges.get() < startIndexInThisSeries) {
                                        try {
                                            synchronizer.wait();
                                        } catch (InterruptedException ex) {
                                            interruptionRequested = true;
                                            throw new IOError(ex);
                                        }
                                    }
                                }
                            }
                            try {
                                final long rFrom = rangeFrom(rangeIndex);
                                final long rTo = rangeTo(rangeIndex);
                                src.subArray(rFrom, rTo).loadResources(null);
                                processRange(rFrom, rTo, ti, rangeIndex);
                            } catch (Throwable ex) {
                                interruptionRequested = true;
                                throwUncheckedException(ex);
                            }
                            synchronized (synchronizer) {
                                if (PROCESS_SYNCHRONIZATION_ALGORITHM == ProcessSynchronizationAlgorithm.BEGINS) {
                                    readyRanges.incrementAndGet();
                                } else {
                                    while (readyRanges.get() < rangeIndex) {
                                        try {
                                            synchronizer.wait();
                                        } catch (InterruptedException ex) {
                                            interruptionRequested = true;
                                            throw new IOError(ex);
                                        }
                                    }
                                    long ready = readyRanges.incrementAndGet();
                                    if (ready != rangeIndex + 1) {
                                        throw new AssertionError("Invalid synchronization (" + ready
                                                + " instead of " + (rangeIndex + 1) + " in "
                                                + ParallelExecutor.this.getClass());
                                    }
                                }
                                synchronizer.notifyAll();
                            }
                        }
                    };
                    long summaryLen = 0;
                    if (numberOfRanges > MAX_NUMBER_OF_RANGES_FOR_PRECISE_LENGTH_PER_TASK_ESTIMATION) {
                        summaryLen = length / numberOfTasks;
                        // skipping the following loop: it can spend a lot of time, but useful only for updating
                        // progress
                    } else {
                        for (long rangeIndex = ti; rangeIndex < numberOfRanges; rangeIndex += numberOfTasks) {
                            summaryLen += rangeLength(rangeIndex);
                        }
                    }
                    readyCountPerTask[ti] = 0;
                    lengthPerTask[ti] = summaryLen;
                }
                boolean noExceptions = false;
                try {
                    if (numberOfTasks == 1) { // NOT THE BEST IDEA: NO loadResources HERE
                        tasks[0].run();
                    } else {
                        ExecutorService pool = threadPoolFactory.getThreadPool(src, ThreadForRanges::new);
                        try {
                            Future<?>[] results = new Future<?>[numberOfTasks];
                            for (int k = 0; k < numberOfTasks; k++) {
                                results[k] = pool.submit(tasks[k]);
                            }
                            try {
                                for (int k = 0; k < numberOfTasks; k++) {
                                    results[k].get(); // waiting for finishing
                                }
                            } catch (ExecutionException ex) {
                                throwUncheckedException(ex);
                            } catch (InterruptedException ex) {
                                interruptionRequested = true;
                                throw new IOError(ex);
                            }
                        } finally {
                            threadPoolFactory.releaseThreadPool(pool);
                        }
                    }
                    if (interruptionReason != null) {
                        throw interruptionReason;
                    }
                    noExceptions = true;
                } finally {
                    try {
                        finish();
                    } catch (RuntimeException ex) {
                        if (noExceptions) {
                            //noinspection ThrowFromFinallyBlock
                            throw ex;
                        }
                    }
                }
            }
        }


        /**
         * Returns the value of {@link #numberOfTasks} field.
         *
         * @return the number of parallel tasks that will be used by {@link #process()} method.
         */
        public int numberOfTasks() {
            return numberOfTasks;
        }

        /**
         * Returns the value of {@link #numberOfRanges} field.
         *
         * @return the number of ranges that will be used by {@link #process()} method.
         */
        public long numberOfRanges() {
            return numberOfRanges;
        }

        /**
         * Returns the length of the specified region.
         * Equal to <code>{@link #rangeTo rangeTo}(rangeIndex)-{@link #rangeFrom rangeFrom}(rangeIndex)</code>.
         *
         * <p>The following condition is true for any <code>k</code> excepting <code>{@link #numberOfRanges}-1</code>
         * (the last range):
         * <code>{@link #rangeLength rangeLength(k)}%{@link #granularity()}==0</code>.
         *
         * @param rangeIndex the index of the region:
         *                   <code>0</code> for the first region, starting from the element <code>#0</code>,
         *                   <code>{@link #numberOfRanges}-1</code> for the last region,
         *                   ending with the element <code>#src.length()-1</code>
         * @return the size of the specified region.
         * @throws IndexOutOfBoundsException if <code>rangeIndex&lt;0</code> or
         *                                   <code>rangeIndex&gt;={@link #numberOfRanges}</code>.
         */
        public final long rangeLength(long rangeIndex) {
            return rangeTo(rangeIndex) - rangeFrom(rangeIndex);
        }

        /**
         * Returns the starting index in the source array (inclusive) of the specified region.
         * Calculated on the base of {@link #numberOfRanges} and
         * <code>src.length()</code>, where <code>src</code> is the source processed array.
         *
         * <p>The following condition is always true:
         * <code>{@link #rangeFrom rangeFrom(k)}%{@link #granularity()}==0</code>.
         *
         * @param rangeIndex the index of the region:
         *                   <code>0</code> for the first region, starting from the element <code>#0</code>,
         *                   <code>{@link #numberOfRanges}-1</code> for the last region,
         *                   ending with the element <code>#src.length()-1</code>
         * @return the starting index of the specified region (inclusive).
         */
        public final long rangeFrom(long rangeIndex) {
            if (rangeIndex < 0 || rangeIndex >= numberOfRanges) {
                throw new IndexOutOfBoundsException("rangeIndex (" + rangeIndex
                        + (rangeIndex < 0 ? ") < 0" : ") >= numberOfRanges (" + numberOfRanges + ")"));
            }
            if (rangeIndex == 0) {
                return 0;
            }
            long gr = granularity();
            if (gr <= 0) {
                throw new AssertionError("Negative or zero granularity() = " + gr);
            }
            long p = (long) (rangeIndex * approximateRangeLength);
            assert p <= length;
            // - because rangeIndex * approximateRangeLength < numberOfRanges * approximateRangeLength = length
            return gr == 1 ? p : p / gr * gr;
        }

        /**
         * Returns the ending index in the source array (exclusive) of the specified region.
         * Calculated on the base of {@link #numberOfRanges} and
         * <code>src.length()</code>, where <code>src</code> is the source processed array.
         *
         * <p>The following condition is true for any <code>k</code> excepting <code>{@link #numberOfRanges}-1</code>
         * (the last range):
         * <code>{@link #rangeTo rangeTo(k)}%{@link #granularity()}==0</code>.
         *
         * @param rangeIndex the index of the region:
         *                   <code>0</code> for the first region, starting from the element <code>#0</code>,
         *                   <code>{@link #numberOfRanges}-1</code> for the last region,
         *                   ending with the element <code>#src.length()-1</code>
         * @return the starting index of the specified region (inclusive).
         */
        public final long rangeTo(long rangeIndex) {
            if (rangeIndex < 0 || rangeIndex >= numberOfRanges) {
                throw new IndexOutOfBoundsException("rangeIndex (" + rangeIndex
                        + (rangeIndex < 0 ? ") < 0" : ") >= numberOfRanges (" + numberOfRanges + ")"));
            }
            if (rangeIndex == numberOfRanges - 1) {
                return length;
            }
            long gr = granularity();
            if (gr <= 0) {
                throw new AssertionError("Negative or zero granularity() = " + gr);
            }
            long p = (long) ((rangeIndex + 1) * approximateRangeLength);
            assert p <= length;
            // - because (rangeIndex+1) * approximateRangeLength <= numberOfRanges * approximateRangeLength = length
            return gr == 1 ? p : p / gr * gr;
        }

        /**
         * This method is automatically called before finishing the {@link #process()} method.
         * This implementation does nothing, but you may override it to release some resources
         * allocated by your constructor of the inheritor if this class.
         */
        protected void finish() {
        }

        /**
         * Processes the region of the source AlgART array, passed to the constructor.
         * Called by {@link #process()} method once by every thread.
         *
         * <p>This implementation splits the specified region into several subregions,
         * each not longer than the <code>blockSize</code> argument of the constructor.
         * For each subregion, this method calls {@link #processSubArr(long, int, int)}.
         * Then this method calls {@link #increaseReadyCount(int, long)}
         * with the first argument, equal to the passed <code>threadIndex</code>,
         * and the second argument, equal to the length of the processed subregion.
         * After this, it calls {@link #checkInterruption()} and {@link #updateProgress()}.
         * If {@link #checkInterruption()} has returned <code>true</code>, this method immediately finishes.
         *
         * <p>Before any operations, this method adds
         * {@link #startGap(long) startGap(rangeIndex)} to <code>fromIndex</code> argument
         * and subtracts {@link #endGap(long) endGap(rangeIndex)} from <code>endIndex</code> argument,
         * with calling {@link #increaseReadyCount(int, long)} for skipped elements.
         * If those methods return negative values, <code>AssertionError</code> is thrown.
         * If the sum <code>{@link #startGap(long) startGap(rangeIndex)}+{@link #endGap(long)
         * endGap(rangeIndex)}&gt;=toIndex-fromIndex</code>,
         * this method does not process any elements.
         *
         * <p>You may override this method, if this simple scheme is not appropriate for your algorithm.
         * In this case, please don't forget to periodically call {@link #increaseReadyCount(int, long)},
         * {@link #updateProgress()} and {@link #checkInterruption()} methods.
         *
         * <p>This method may not throw exceptions for incorrect arguments.
         * The {@link #process()} method always pass the correct ones.
         *
         * @param fromIndex   the start index of processed region, inclusive.
         * @param toIndex     the end index of processed region, exclusive.
         * @param threadIndex the index of the processed task (<code>0..{@link #numberOfTasks}-1</code>).
         *                    This argument is not used by this method, but just passed to
         *                    {@link #processSubArr(long, int, int)} method.
         * @param rangeIndex  the index of the processed range (<code>0..{@link #numberOfRanges}&minus;1</code>:
         *                    <code>0</code> for the first region, starting from the element <code>#0</code>,
         *                    <code>{@link #numberOfRanges}-1</code> for the last region,
         *                    ending with the element <code>#source_array.length()-1</code>).
         *                    This argument is not used by this method, but just passed to
         *                    {@link #startGap(long)} and {@link #endGap(long)} methods.
         * @throws IllegalArgumentException if <code>fromIndex&gt;endIndex</code>.
         */
        protected void processRange(long fromIndex, long toIndex, int threadIndex, long rangeIndex) {
//            System.out.println(threadIndex + "/" + rangeIndex + ": " + fromIndex + ".." + toIndex + "; " + length);
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex > toIndex");
            }
            long startGap = startGap(rangeIndex);
            long endGap = endGap(rangeIndex);
            if (startGap < 0) {
                throw new AssertionError("Negative startGap() = " + startGap);
            }
            if (endGap < 0) {
                throw new AssertionError("Negative endGap() = " + endGap);
            }
            startGap = Math.min(startGap, toIndex - fromIndex);
            endGap = Math.min(endGap, toIndex - fromIndex - startGap);
            if (startGap > 0) {
                increaseReadyCount(threadIndex, startGap);
            }
            fromIndex += startGap;
            toIndex -= endGap;
            for (long p = fromIndex; p < toIndex; ) {
                long len = Math.min(toIndex - p, blockSize);
                assert len == (int) len;
// ESTIMATION BELOW NOT TOO GOOD IDEA: SOME ALGORITHMS LIKE RANK MORPHOLOGY HAVE LONG-TIME PREPARATION IN EACH getData
//                if (p == fromIndex) {
//                    len = (int)Math.min(len, Math.max(5, (toIndex - fromIndex) / 16)); // WHY toIndex - fromIndex?
//                    // testing first 1/16 elements at the beginning
//                }
//                long t1 = p == fromIndex ? System.currentTimeMillis() : -1;
                processSubArr(p, (int) len, threadIndex);
//                System.out.println("Processing " + len + " in thread #" + threadIndex);
//                if (p == fromIndex) { // estimate speed
//                    long t2 = System.currentTimeMillis();
//                    if (t2 - t1 > 100 // to be on the safe side
//                        && (t2 - t1) * blockSize >
//                        SystemSettings.MAX_TIME_OF_NON_INTERRUPTIBLE_PROCESSING * len)
//                    {
//                        // too large blockSize: it will be very difficult to interrupt process via checkInterruption()
//                        actualBlockSize = Math.max(5,
//                            (int)(SystemSettings.MAX_TIME_OF_NON_INTERRUPTIBLE_PROCESSING
//                                * len / (t2 - t1)));
//                        System.out.println("Correction to " + actualBlockSize);
//                    }
//                }
                p += len;
                increaseReadyCount(threadIndex, len);
                boolean interrupted = checkInterruption();
                updateProgress();
                if (interrupted) {
                    break;
                }
            }
            if (endGap > 0) {
                increaseReadyCount(threadIndex, endGap);
            }
        }

        /**
         * Returns the number of elements that should be skipped by {@link #processRange(long, long, int, long)}
         * method at the beginning of each processed block.
         * This default implementation of this method returns 0.
         * The overridden implementation may return some positive value.
         * In this case, first {@link #startGap(long) startGap(rangeIndex)}
         * elements will not be processed
         * by {@link #processRange(long fromIndex, long toIndex, int threadIndex, long rangeIndex)} method:
         * they will be skipped and not passed to {@link #processSubArr(long, int, int)} method.
         * This can be useful for some algorithms that change the source AlgART arrays depending
         * on some set of elements with "near" indexes, alike in some image processing filters.
         *
         * @param rangeIndex the index of the region:
         *                   <code>0</code> for the first region, starting from the element <code>#0</code>,
         *                   <code>{@link #numberOfRanges}-1</code> for the last region,
         *                   ending with the element <code>#source_array.length()-1</code>
         * @return the number of elements that should be skipped by
         * {@link #processRange(long fromIndex, long toIndex, int threadIndex, long rangeIndex)}
         * at the beginning of the processed block.
         */
        public long startGap(long rangeIndex) {
            return 0;
        }

        /**
         * Returns the number of elements that should be skipped by {@link #processRange(long, long, int, long)}
         * method at the end of each processed block.
         * This default implementation of this method returns 0.
         * The overridden implementation may return some positive value.
         * In this case, last {@link #endGap(long) endGap(rangeIndex)}
         * elements will not be processed
         * by {@link #processRange(long fromIndex, long toIndex, int threadIndex, long rangeIndex)} method:
         * they will be skipped and not passed to {@link #processSubArr(long, int, int)} method.
         * This can be useful for some algorithms that change the source AlgART arrays depending
         * on some set of elements with "near" indexes, alike in some image processing filters.
         *
         * @param rangeIndex the index of the region:
         *                   <code>0</code> for the first region, starting from the element <code>#0</code>,
         *                   <code>{@link #numberOfRanges}-1</code> for the last region,
         *                   ending with the element <code>#source_array.length()-1</code>
         * @return the number of elements that should be skipped by
         * {@link #processRange(long fromIndex, long toIndex, int threadIndex, long rangeIndex)}
         * at the end of the processed block.
         */
        public long endGap(long rangeIndex) {
            return 0;
        }

        /**
         * Returns the granularity of splitting: an integer value so that the start index of any range is
         * multiple of {@link #granularity()}.
         * The default implementation of this method returns 64 if the source processed array
         * is {@link BitArray} (good splitting for processing packed bits)or 1 in another case.
         * You may override this method, for example, if you need every processed range to contain
         * an integer number of lines of some {@link Matrix AlgART matrix}.
         *
         * <p>This method is called by {@link #rangeLength}, {@link #rangeFrom}, {@link #rangeTo}
         * and {@link #process} methods. If this method returns zero or negative value,
         * <code>AssertionError</code> is thrown.
         *
         * @return the granularity of splitting (must be positive).
         */
        public long granularity() {
            return src instanceof BitArray ? 64 : 1;
        }

        /**
         * Should process the specified region of the source AlgART array.
         * This method is called by {@link #processRange(long, long, int, long)} method
         * with the <code>count</code> argument, not greater than
         * the <code>blockSize</code> argument of the constructor.
         *
         * @param position    the start index of processed region, inclusive.
         * @param count       the number of the processed elements.
         * @param threadIndex the index of the processed task (<code>0..{@link #numberOfTasks}-1</code>).
         *                    You can use this argument, for example, to operate with some data concerning
         *                    only one of the threads.
         */
        protected abstract void processSubArr(long position, int count, int threadIndex);

        /**
         * Return the total number of the processed elements of the source array.
         * This number is 0 after creating the instance and changed
         * (it's the only way) by the periodic calls of {@link #increaseReadyCount(int, long)} method.
         *
         * <p>This method is internally synchronized to provide correct value,
         * that may be updated by several threads.
         *
         * @return the number of successfully processed elements of the source array.
         */
        protected final long readyCount() {
            lock.lock();
            try {
                long sum = 0;
                for (long count : this.readyCountPerTask) {
                    sum += count;
                }
                return sum;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Adds the argument to the counter of the processed elements in the given region of the source array.
         * The sum of the counters for all regions is returned by {@link #readyCount()} method.
         *
         * <p>This method is internally synchronized to correctly update the counters
         * from several threads.
         *
         * @param threadIndex the index of the processed region (thread), from 0 to (number of tasks)-1.
         * @param increment   the desired increment of the number of processed elements.
         */
        protected final void increaseReadyCount(int threadIndex, long increment) {
            lock.lock();
            try {
                this.readyCountPerTask[threadIndex] += increment;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Checks, whether the user want to interrupt the calculations,
         * and returns <code>true</code> in this case.
         * Also returns <code>true</code> if some of another tasks, performed by {@link #process()} method
         * in a parallel threads, was finished with an exception.
         *
         * <p>More precisely, it checks the internal synchronized boolean flag
         * "interruptionRequested". If it's <code>true</code>, the method returns <code>true</code>.
         * If the context, passed to the constructor, was {@code null}, this method finishes
         * at this point (with <code>false</code> result).
         *
         * <p>Else, if some time was elapsed from the last call of this method,
         * it calls {@link ArrayContext#checkInterruption()} for the context, passed to the constructor.
         * If that method throws any exception, it is stored in an internal field,
         * the "interruptionRequested" flag is set and this method returns <code>true</code>.
         * This exception will be re-thrown by the basic {@link #process()} method
         * before its finishing.
         *
         * <p>In all other cases, this method returns <code>false</code>.
         *
         * <p>This method is internally synchronized to correctly check and update
         * "interruptionRequested" flag.
         *
         * @return <code>true</code> if the application requested the interruption of calculations.
         */
        protected final boolean checkInterruption() {
            if (interruptionRequested) {
                return true;
            }
            if (context == null) {
                return false;
            }
            if (!updateTime(false)) {
                return false;
            }
            RuntimeException re = null;
            try {
                context.checkInterruption();
            } catch (RuntimeException ex) {
                re = ex;
            }

            lock.lock(); // synchronization guarantees that interruptionReason will be set once only
            try {
                if (re != null) {
                    if (interruptionReason == null) {
                        interruptionReason = re;
                    }
                    interruptionRequested = true;
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * Calls {@link ArrayContext#updateProgress ArrayContext.updateProgress} method
         * with the context, passed to the constructor,
         * and correctly filled {@link ArrayContext.Event} instance.
         * Does nothing if the context, passed to the constructor, was {@code null},
         * or if this method was already called a little time ago.
         */
        protected final void updateProgress() {
            if (context == null) {
                return;
            }
            if (updateTime(true)) {
                long[] lengthPerTask, readyCountPerTask;
                lock.lock();
                try {
                    lengthPerTask = this.lengthPerTask.clone();
                    readyCountPerTask = this.readyCountPerTask.clone();
                } finally {
                    lock.unlock();
                }
                for (int k = 0; k < readyCountPerTask.length; k++) {
                    readyCountPerTask[k] = Math.min(readyCountPerTask[k], lengthPerTask[k]);
                    // necessary if numberOfRanges > MAX_NUMBER_OF_RANGES_FOR_PRECISE_LENGTH_PER_TASK_ESTIMATION
                }
                context.updateProgress(new ArrayContext.Event(src.elementType(), readyCountPerTask, lengthPerTask));
            }
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return "Parallel executor for " + numberOfTasks + " tasks and " + numberOfRanges
                    + " ranges per ~" + approximateRangeLength + " elements, block size " + blockSize
                    + ", source array " + src;
        }

        private boolean updateTime(boolean progressTime) {
            lock.lock(); // synchronization guarantees that the time will be increased always
            try {
                long t1 = progressTime ? lastProgressTime : lastInterruptionTime;
                long t2 = System.currentTimeMillis();
                if (t1 == Long.MIN_VALUE || t2 - t1 > SystemSettings.RECOMMENDED_TIME_OF_NON_INTERRUPTIBLE_PROCESSING) {
                    if (progressTime) {
                        lastProgressTime = t2;
                    } else {
                        lastInterruptionTime = t2;
                    }
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * The currently used synchronization algorithm: {@value}.
         */
        private static final ProcessSynchronizationAlgorithm PROCESS_SYNCHRONIZATION_ALGORITHM =
                ProcessSynchronizationAlgorithm.NO_SYNCHRONIZATION;

        /**
         * The algorithm of synchronizing preloading resources in {@link Arrays.ParallelExecutor#process} method.
         * Selected by the global constant {@link Arrays.ParallelExecutor#PROCESS_SYNCHRONIZATION_ALGORITHM}.
         * All currently implemented algorithms excepting {@link #NO_SYNCHRONIZATION} are not good enough:
         * they do not provide correct sequence of
         * {@link Array#loadResources(ArrayContext) loadResources(null)} calls and sometimes
         * do not allow calculations simultaneously with preloading another bank.
         * But these algorithms are not removed at all due to profiling and another possible future needs.
         */
        private enum ProcessSynchronizationAlgorithm {
            /**
             * The best and simplest algorithm: no synchronization, loadResources(null) is called outside the threads.
             */
            NO_SYNCHRONIZATION,

            /**
             * The same algorithm as in {@link GeneralizedBitProcessing}.
             * It allows preloading next packet of <code>numberOfTasks</code> blocks
             * while processing the previous packet.
             */
            TILING,

            /**
             * Synchronizes the beginning of every packet of <code>numberOfTasks</code> blocks.
             * However, does not provide correct order of
             * {@link Array#loadResources(ArrayContext) loadResources(null)}
             * calls after this and does not allow simultaneous preloading and processing.
             */
            BEGINS
        }

        private static class ThreadForRanges extends Thread {
            public ThreadForRanges(Runnable r) {
                super(r);
            }
        }
    }

    /**
     * <p>Implementation of {@link Arrays.ParallelExecutor} performing
     * simple copying of the source array.
     * This class is used by
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} and
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int)} methods.</p>
     *
     * <p>Please note: this class does not provide functionality of the methods
     * {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int, boolean)} and
     * {@link Arrays#compareAndCopy(ArrayContext, UpdatableArray, Array)}.</p>
     */
    public static class Copier extends Arrays.ParallelExecutor {
        private static final boolean VERY_SIMPLE_COPYING = false; // for profiling and debugging only

        /**
         * Creates new instance of this class, performing copying of <code>src</code> array
         * to <code>dest</code> array.
         * The {@link #process()} method of the created instance
         * is equivalent to {@link Arrays#copy(ArrayContext, UpdatableArray, Array, int)
         * Arrays.copy(context, dest, src, numberOfTasks)} call, excepting that
         * the last method sometimes uses more complex copying algorithms.
         *
         * @param context        the context of copying; can be {@code null}.
         * @param dest           the destination array.
         * @param src            the source array.
         * @param numberOfTasks  the desired number of parallel tasks or 0 if you want to determine
         *                       this number automatically, from the passed context or (if it is {@code null})
         *                       from {@link DefaultThreadPoolFactory} instance.
         * @param numberOfRanges the desired number of ranges for splitting the source array. If the
         *                       {@link #numberOfTasks} (calculated automatically or equal to the corresponding
         *                       argument) is 1, then this argument is ignored and 1 range is used (no splitting).
         *                       If this argument is positive and <code>&lt;{@link #numberOfTasks}</code>,
         *                       it is replaced with {@link #numberOfTasks}.
         *                       If this argument is 0 and <code>{@link #numberOfTasks}&gt;1</code>, then
         *                       the number of ranges is chosen automatically as
         *                       <code>max({@link #numberOfTasks},&nbsp;{@link #recommendedNumberOfRanges
         *                       recommendedNumberOfRanges(src,&nbsp;true)})</code>
         * @throws NullPointerException     if the <code>src</code> or <code>dest</code> argument is {@code null}.
         * @throws IllegalArgumentException if <code>blockSize &lt;= 0</code>,
         *                                  or if <code>numberOfTasks &lt; 0</code>,
         *                                  or if <code>numberOfRanges &lt; 0</code>.
         */
        public Copier(ArrayContext context, UpdatableArray dest, Array src, int numberOfTasks, long numberOfRanges) {
            super(context,
                    src.length() <= dest.length() ? dest.subArr(0, src.length()) : dest,
                    src.length() <= dest.length() ? src : src.subArr(0, dest.length()),
                    src instanceof BitArray ? 8 * 65536 : 2 * 65536,
                    numberOfTasks, numberOfRanges);
            if (!dest.elementType().isAssignableFrom(src.elementType()))
            // this check is necessary in a case of empty arrays
            {
                throw new IllegalArgumentException("Element types mismatch ("
                        + dest.elementType() + " and " + src.elementType() + ")");
            }
        }

        @Override
        public void process() {
            if (VERY_SIMPLE_COPYING) {
                dest.copy(src);
                return;
            }
            super.process();
        }

        /**
         * This implementation copies <code>src.{@link Array#subArr(long, long) subArr}(position,count)</code>
         * to <code>dest.{@link UpdatableArray#subArr(long, long) subArr}(position,count)</code>
         * via {@link UpdatableArray#copy(Array)} method.
         *
         * @param position    the start index of processed region, inclusive.
         * @param count       the number of the processed elements.
         * @param threadIndex the index of the processed region (thread), from 0 to (number of tasks)-1.
         */
        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            UpdatableArray subDest = dest.subArr(position, count);
            Array subSrc = src.subArr(position, count);
            subDest.copy(subSrc);
        }
    }

    /**
     * Estimates the number of elements in the specified array, corresponding to the specified memory size in bytes.
     * If <code>{@link #sizeOf(Array) sizeOf}(array)&lt;0</code> ("unknown size" of array elements),
     * this method uses the assumption that every element occupies 4 bytes.
     * Returns 0 if the length of the array is 0.
     *
     * @param array       the array.
     * @param sizeInBytes required memory size in bytes.
     * @return estimated number of elements of the given array, corresponding to this memory size.
     */
    static long lengthOf(Array array, long sizeInBytes) {
        if (sizeInBytes < 0) {
            throw new IllegalArgumentException("Negative sizeInBytes");
        }
        long len = array.length();
        long size = sizeOf(array);
        if (len == 0) {
            return 0;
        }
        if (size < 0) {
            return sizeInBytes / 4;
        }
        return (long) (sizeInBytes * ((double) len / (double) size));
    }

    /**
     * Provides package-private access to the backed array even for immutable arrays.
     * For copy-on-next-write subclasses of {@link AbstractArray}, does not clone data.
     * Returns {@code null} if the is no backed array (not throws an exception).
     * Also returns {@code null} for {@link BitArray}.
     *
     * @param array the source array.
     * @return the same result as {@link DirectAccessible#javaArray()} method, but also for immutable arrays.
     */
    static Object javaArrayInternal(Array array) {
        if (array instanceof AbstractArray) {
            return ((AbstractArray) array).javaArrayInternal();
        }
        if (array instanceof DirectAccessible && ((DirectAccessible) array).hasJavaArray()) {
            return ((DirectAccessible) array).javaArray();
        }
        return null;
    }

    /**
     * Provides package-private access to the offset in the backed array even for immutable arrays.
     *
     * @param array the source array.
     * @return the same result as {@link DirectAccessible#javaArrayOffset()} method,
     * but also for immutable arrays.
     */
    static int javaArrayOffsetInternal(Array array) {
        if (array instanceof AbstractArray) {
            return ((AbstractArray) array).javaArrayOffsetInternal();
        }
        if (array instanceof DirectAccessible && ((DirectAccessible) array).hasJavaArray()) {
            return ((DirectAccessible) array).javaArrayOffset();
        }
        return 0;
    }

    /**
     * Provides package-private access to the backed <code>long[]</code> array for bit arrays (even immutable).
     * For copy-on-next-write subclasses of {@link AbstractArray}, does not clone data.
     * Returns {@code null} if the is no backed array (not throws an exception).
     *
     * @param bitArray the source array.
     * @return the backed <code>long[]</code> array.
     */
    static long[] longJavaArrayInternal(BitArray bitArray) {
        if (bitArray instanceof SimpleArraysImpl.JABitArray) {
            return ((SimpleArraysImpl.JABitArray) bitArray).bitArray;
        }
        if (bitArray instanceof SimpleArraysImpl.JABitSubArray) {
            return ((SimpleArraysImpl.JABitSubArray) bitArray).bitArray;
        }
        return null;
    }

    /**
     * Provides package-private access to the offset in the backed packed bit array (even for immutable arrays).
     *
     * @param bitArray the source array.
     * @return the offset in the backed packed bit Java array.
     */
    static long longJavaArrayOffsetInternal(BitArray bitArray) {
        if (bitArray instanceof SimpleArraysImpl.JABitSubArray) {
            return ((SimpleArraysImpl.JABitSubArray) bitArray).offset;
        }
        return 0;
    }

    /**
     * Returns <code>true</code> if the passed array is a not-{@code null} built-in array of some AlgART matrix,
     * created by {@link Matrix#tile()} method.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array a build-in array of some tiled AlgART matrix.
     */
    static boolean isTiled(Array array) {
        return array instanceof ArraysTileMatrixImpl.TileMatrixArray;
    }

    /**
     * Returns <code>true</code> if the passed array is a not-{@code null} built-in array of some AlgART matrix,
     * created by {@link Matrix#tile()} method or by extracting submatrix (by one or several sequential
     * <code>subMatrix/subMatr</code> methods) from a matrix, created by {@link Matrix#tile()} method.
     *
     * @param array the checked AlgART array (can be {@code null}, than the method returns <code>false</code>).
     * @return <code>true</code> if the passed array a build-in array of some tiled AlgART matrix or
     * a submatrix of such a matrix.
     */
    static boolean isTiledOrSubMatrixOfTiled(Array array) {
        if (array instanceof ArraysSubMatrixImpl.SubMatrixArray) {
            return isTiledOrSubMatrixOfTiled(((ArraysSubMatrixImpl.SubMatrixArray) array).baseMatrix().array());
        }
        return array instanceof ArraysTileMatrixImpl.TileMatrixArray;
    }

    /**
     * Returns the {@link Matrix#tileDimensions() tile dimensions} for the passed array, if it is a built-in array
     * of some AlgART matrix, created by {@link Matrix#tile()} method.
     *
     * @param array the checked AlgART array: built-in array of some AlgART tiled matrix.
     * @return the tile dimensions, used for creating the tiled matrix.
     * @throws NullPointerException if the passed array is {@code null}.
     * @throws ClassCastException   if <code>!{@link #isTiled(Array) isTiled}(array)</code>.
     */
    static long[] tileDimensions(Array array) {
        return ((ArraysTileMatrixImpl.TileMatrixArray) array).tileDimensions();
    }

    /**
     * Returns the {@link Matrix#dimensions() base matrix dimensions} for the passed array, if it is a built-in array
     * of some AlgART matrix, created by {@link Matrix#tile()} method.
     *
     * @param array the checked AlgART array: built-in array of some AlgART tiled matrix.
     * @return the dimensions of that matrix.
     * @throws NullPointerException if the passed array is {@code null}.
     * @throws ClassCastException   if <code>!{@link #isTiled(Array) isTiled}(array)</code>.
     */
    static long[] tiledMatrixDimensions(Array array) {
        return ((ArraysTileMatrixImpl.TileMatrixArray) array).baseMatrix().dimensions();
    }

    /**
     * An analog of {@link #getUnderlyingArrays(Array)}
     * that, if <code>trusted=true</code>, provides package-private access to the internal Java array even
     * without any additional operation.
     *
     * @param array   the checked AlgART array.
     * @param trusted if <code>true</code>, returns a reference to the internal Java array without additional
     *                operations.
     * @return Java array of underlying arrays.
     * @throws NullPointerException if the passed argument is {@code null}.
     */
    static Array[] getUnderlyingArrays(Array array, boolean trusted) {
        Objects.requireNonNull(array, "Null array argument");
        if (!(array instanceof AbstractArray)) {
            return new Array[0];
        }
        Array[] underlyingArrays = ((AbstractArray) array).underlyingArrays;
        if (trusted) {
            return underlyingArrays;
        }
        Array[] result = new Array[underlyingArrays.length];
        boolean thisImmutable = array.isImmutable();
        for (int k = 0; k < result.length; k++) {
            result[k] = thisImmutable ?
                    underlyingArrays[k].asImmutable() :
                    underlyingArrays[k].shallowClone();
        }
        return result;
    }

    /**
     * An analog of {@link AbstractArray#defaultBuffer(Array, net.algart.arrays.DataBuffer.AccessMode, long)}
     * that provides package-private access to the backed array even for immutable and
     * copy-on-next-write arrays.
     * Never clones data for copy-on-next-write arrays.
     *
     * @param array    the array.
     * @param mode     the access mode for new buffer.
     * @param capacity the capacity of the buffer
     * @param trusted  if <code>true</code>, returns direct buffer even for immutable and copy-on-next-write arrays.
     * @return new data buffer for accessing this array.
     */
    static DataBuffer bufferInternal(Array array, DataBuffer.AccessMode mode, long capacity, boolean trusted) {
        if (array instanceof BitArray) {
            return new DataBuffersImpl.ArrayBitBuffer((BitArray) array, mode, capacity, trusted);
        } else if (array instanceof CharArray) {
            return new DataBuffersImpl.ArrayCharBuffer((CharArray) array, mode, capacity, trusted);
        } else if (array instanceof ByteArray) {
            return new DataBuffersImpl.ArrayByteBuffer((ByteArray) array, mode, capacity, trusted);
        } else if (array instanceof ShortArray) {
            return new DataBuffersImpl.ArrayShortBuffer((ShortArray) array, mode, capacity, trusted);
        } else if (array instanceof IntArray) {
            return new DataBuffersImpl.ArrayIntBuffer((IntArray) array, mode, capacity, trusted);
        } else if (array instanceof LongArray) {
            return new DataBuffersImpl.ArrayLongBuffer((LongArray) array, mode, capacity, trusted);
        } else if (array instanceof FloatArray) {
            return new DataBuffersImpl.ArrayFloatBuffer((FloatArray) array, mode, capacity, trusted);
        } else if (array instanceof DoubleArray) {
            return new DataBuffersImpl.ArrayDoubleBuffer((DoubleArray) array, mode, capacity, trusted);
        } else if (array instanceof ObjectArray<?>) {
            return new DataBuffersImpl.ArrayObjectBuffer<>(InternalUtils.cast(array), mode, capacity, trusted);
        } else {
            return new DataBuffersImpl.ArrayBuffer(array, mode, capacity, trusted);
        }
        // - to be on the safe side, for possible "strange" inheritors
    }

    /**
     * Equivalent to {@link #bufferInternal(Array, net.algart.arrays.DataBuffer.AccessMode, long, boolean)
     * bufferInternal(array, mode, AbstractArray.defaultBufferCapacity(array), true)}.
     *
     * @param array the array.
     * @param mode  the access mode for new buffer.
     * @return new data buffer for accessing this array.
     */
    static DataBuffer bufferInternal(Array array, DataBuffer.AccessMode mode) {
        return bufferInternal(array, mode, AbstractArray.defaultBufferCapacity(array), true);
    }

    static void enableCaching(DataBuffer dataBuffer) {
        if (dataBuffer instanceof DataBuffersImpl.ArrayBuffer) {
            ((DataBuffersImpl.ArrayBuffer) dataBuffer).caching = true;
        }
    }

    static void dispose(DataBuffer dataBuffer) {
        if (dataBuffer instanceof DataBuffersImpl.ArrayBuffer) {
            ((DataBuffersImpl.ArrayBuffer) dataBuffer).dispose();
        }
    }

    static int truncateLongToInt(long value) {
        return value == (int) value ? (int) value :
                value > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }

    static ObjectArray<?> nObjectCopies(long n, Object element, Class<?> nullType) {
        return element != null ? nObjectCopies(n, element) : nNullCopies(n, nullType);
    }

    static void checkElementTypeForNullAndVoid(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (elementType == void.class) {
            throw new IllegalArgumentException("Illegal elementType: it cannot be void.class");
        }
    }

    // Adapted procedure from java.util.Collections
    private static void rotate(long[] array, int distance) {
        if (array.length == 0) {
            return;
        }
        distance %= array.length;
        if (distance < 0) {
            distance += array.length;
        }
        if (distance == 0) {
            return;
        }

        for (int cycleStart = 0, nMoved = 0; nMoved != array.length; cycleStart++) {
            long displaced = array[cycleStart];
            int i = cycleStart;
            do {
                i += distance;
                if (i >= array.length) {
                    i -= array.length;
                }
                long temp = array[i];
                array[i] = displaced;
                displaced = temp;
                nMoved++;
            } while (i != cycleStart);
        }
    }

    private static class AlgARTArrayList<E> extends AbstractList<E> implements RandomAccess {
        private final Array array;

        AlgARTArrayList(Array array) {
            Objects.requireNonNull(array);
            if (array.length() > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Cannot view AlgART array as List, "
                        + "because it is too large: " + array);
            }
            this.array = array;
        }

        public int size() {
            long len = array.length();
            if (len > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Cannot access List based on AlgART array, "
                        + "because it is too large: " + array);
            }
            return (int) len;
        }

        public Object[] toArray() {
            if (!(array instanceof PArray)) {
                return (Object[]) array.toJavaArray();
            }
            long len = array.length();
            if (len != (int) len) {
                throw new TooLargeArrayException("Cannot convert List based on AlgART array to Java array, "
                        + "because it is too large: " + array);
            }
            Object[] result;
            Class<?> et = array.elementType();
            if (et == boolean.class) {
                result = new Boolean[(int) len];
            } else if (et == char.class) {
                result = new Character[(int) len];
            } else if (et == byte.class) {
                result = new Byte[(int) len];
            } else if (et == short.class) {
                result = new Short[(int) len];
            } else if (et == int.class) {
                result = new Integer[(int) len];
            } else if (et == long.class) {
                result = new Long[(int) len];
            } else if (et == float.class) {
                result = new Float[(int) len];
            } else if (et == double.class) {
                result = new Double[(int) len];
            } else {
                throw new AssertionError("Illegal array element type: " + array);
            }
            for (int k = 0; k < result.length; k++) {
                result[k] = array.getElement(k);
            }
            return result;
        }

        public E get(int index) {
            return InternalUtils.cast(array.getElement(index));
        }

        public E set(int index, E element) {
            if (array instanceof UpdatableArray) {
                E oldValue = InternalUtils.cast(array.getElement(index));
                ((UpdatableArray) array).setElement(index, element);
                return oldValue;
            } else {
                throw new UnsupportedOperationException("AlgART array is not updatable");
            }
        }

        public int indexOf(Object o) {
            long len = array.length();
            if (len != (int) len) {
                throw new TooLargeArrayException("Cannot run indexOf in List based on AlgART array, "
                        + "because it is too large: " + array);
            }
            if (o == null) {
                for (int k = 0; k < (int) len; k++) {
                    if (array.getElement(k) == null) {
                        return k;
                    }
                }
            } else {
                for (int k = 0; k < (int) len; k++) {
                    if (o.equals(array.getElement(k))) {
                        return k;
                    }
                }
            }
            return -1;
        }

        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }
    }

    private static class AlgARTArrayCharSequence implements CharSequence {
        private final CharArray charArray;

        private AlgARTArrayCharSequence(CharArray charArray) {
            Objects.requireNonNull(charArray, "Null charArray argument");
            if (charArray.length() > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Cannot view AlgART array as CharSequence, "
                        + "because it is too large: " + charArray);
            }
            this.charArray = charArray;
        }

        public int length() {
            return (int) charArray.length();
        }

        public char charAt(int index) {
            return charArray.getChar(index);
        }

        public CharSequence subSequence(int start, int end) {
            return new AlgARTArrayCharSequence((CharArray) charArray.subArray(start, end));
        }

        public String toString() {
            return Arrays.toString(charArray);
        }
    }
}
