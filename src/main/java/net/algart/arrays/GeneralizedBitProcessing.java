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

import net.algart.math.functions.ConstantFunc;
import net.algart.math.Range;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Universal converter of bitwise operation (an algorithm processing {@link BitArray})
 * to operation over any primitive type (an algorithm processing {@link PArray}).</p>
 *
 * <p>This class implements the following common idea. Let we have some algorithm,
 * that transforms the source bit array ({@link BitArray}) <b>b</b> to another bit array &fnof;(<b>b</b>).
 * (Here is an interface {@link GeneralizedBitProcessing.SliceOperation} representing such algorithm.)
 * Let we want to generalize this algorithm for a case of any element types &mdash; <tt>byte</tt>, <tt>int</tt>,
 * <tt>double</tt>, etc.; in other words, for the common case of {@link PArray}.
 * This class allows to do this.</p>
 *
 * <p><a name="bitslice"></a>Namely, let we have the source array ({@link PArray}) <b>a</b>,
 * and let <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i> be some numeric range,
 * <i>a<sub>min</sub></i>&le;<i>a<sub>max</sub></i>
 * usually (but not necessarily) from the minimal to the maximal value, stored in the source array
 * ({@link Arrays#rangeOf(PArray) Arrays.rangeOf(<b>a</b>)}).
 * This class can work in two modes, called <i>rounding modes</i>
 * (and represented by {@link GeneralizedBitProcessing.RoundingMode} enum):
 * the first mode is called <i>round-down mode</i>, and the second is called <i>round-up mode</i>.
 * Below is the specification of the behaviour in both modes.</p>
 *
 * <div align="center">
 * <table width="90%" border="1" cellpadding="8" cellspacing="0">
 *   <tr>
 *     <td width="50%"><b>Round-down mode</b></td>
 *     <td width="50%"><b>Round-up mode</b></td>
 *   </tr>
 *   <tr>
 *     <td colspan="2">
 *     <p>In both modes, we consider <i>n</i>+1 (<i>n</i>&ge;0) numeric <i>thresholds</i>
 *     <nobr><i>t</i><sub>0</sub>, <i>t</i><sub>1</sub>, ..., <i>t</i><sub><i>n</i></sub> in
 *     <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i> range:</p>
 *     <blockquote>
 *     <i>t</i><sub>0</sub> = <i>a<sub>min</sub></i>,<br>
 *     <i>t</i><sub>1</sub> =
 *     <i>a<sub>min</sub></i> + (<i>a<sub>max</sub></i>&minus;<i>a<sub>min</sub></i>)/<i>n</i>,<br>
 *     . . .<br>
 *     <i>t</i><sub><i>i</i></sub> =
 *     <i>a<sub>min</sub></i> + <i>i</i> * (<i>a<sub>max</sub></i>&minus;<i>a<sub>min</sub></i>)/<i>n</i>,<br>
 *     . . .<br>
 *     <i>t</i><sub><i>n</i></sub> = <i>a<sub>max</sub></i>
 *     </blockquote>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>(In the degenerated case <i>n</i>=0, we consider
 *     <i>t</i><sub>0</sub> = <i>t</i><sub><i>n</i></sub> = <i>a<sub>min</sub></i>.)
 *     </td>
 *     <td>(In the degenerated case <i>n</i>=0, we consider
 *     <i>t</i><sub>0</sub> = <i>t</i><sub><i>n</i></sub> = <i>a<sub>max</sub></i>.)
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *     <p>Then we define the <i>bit slice</i> <b>b</b><sub><i>i</i></sub>, <i>i</i>=0,1,...,<i>n</i>,
 *     as a bit array <b>a</b>&ge;<i>t</i><sub><i>i</i></sub>, i.e. {@link BitArray} with the same length as <b>a</b>,
 *     where each element</p>
 *     <blockquote>
 *     <b>b</b><sub><i>i</i></sub>[k] = <b>a</b>[k]&ge;<i>t</i><sub><i>i</i></sub> ? 1 : 0
 *     </blockquote>
 *     </td>
 *     <td>
 *     <p>Then we define the <i>bit slice</i> <b>b</b><sub><i>i</i></sub>, <i>i</i>=0,1,...,<i>n</i>,
 *     as a bit array <b>a</b>&gt;<i>t</i><sub><i>i</i></sub>, i.e. {@link BitArray} with the same length as <b>a</b>,
 *     where each element</p>
 *     <blockquote>
 *     <b>b</b><sub><i>i</i></sub>[k] = <b>a</b>[k]&gt;<i>t</i><sub><i>i</i></sub> ? 1 : 0
 *     </blockquote>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td colspan="2">
 *     The described transformation of the numeric array <b>a</b> to a set of <i>n</i>+1 "slices"
 *     (bit arrays) <b>b</b><sub><i>i</i></sub> is called <i>splitting to slices</i>.
 *     Then we consider the backward conversion of the set of bit slices
 *     <b>b</b><sub><i>i</i></sub> to a numeric array <b>a'</b>, called <i>joining the slices</i>:
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *     <blockquote>
 *     <b>a'</b>[k] = <i>t</i><sub>max <i>i</i>: <b>b</b><sub><i>i</i></sub>[k] = 1</sub>,
 *     or <b>a'</b>[k] = <i>a<sub>min</sub></i> if all <b>b</b><sub><i>i</i></sub>[k] = 0
 *     </blockquote>
 *     </td>
 *     <td>
 *     <blockquote>
 *     <b>a'</b>[k] = <i>t</i><sub>min <i>i</i>: <b>b</b><sub><i>i</i></sub>[k] = 0</sub>,
 *     or <b>a'</b>[k] = <i>a<sub>max</sub></i> if all <b>b</b><sub><i>i</i></sub>[k] = 1
 *     </blockquote>
 *     </td>
 *   </tr>
 * </table>
 * </div>
 *
 * <p>It's obvious that if all <b>a</b> elements are inside <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i>
 * range and if <i>n</i> is large enough, then <b>a'</b> is almost equal to <b>a</b>.
 * In particular, if <b>a</b> is a byte array ({@link ByteArray}), <i>a<sub>min</sub></i>=0,
 * <i>a<sub>max</sub></i>=255 and <i>n</i>=255, then <b>a'</b> is strictly equal to&nbsp;<b>a</b>
 * (in both rounding modes).</p>
 *
 * <p>The main task, solved by this class, is converting any bitwise operation &fnof;(<b>b</b>)
 * to a new operation <i>g</i>(<b>a</b>), defining for any primitive-type array <b>a</b>,
 * according to the following rule:</p>
 *
 * <div align="center">
 * <table width="90%" border="1" cellpadding="8" cellspacing="0">
 *   <tr>
 *     <td width="50%"><b>Round-down mode</b></td>
 *     <td width="50%"><b>Round-up mode</b></td>
 *   </tr>
 *   <tr>
 *     <td>
 *     <blockquote>
 *     <i>g</i>(<b>a</b>)[k] = <i>t</i><sub>max <i>i</i>: &fnof;(<b>b</b><sub><i>i</i></sub>)[k] = 1</sub>,
 *     or <i>g</i>(<b>a</b>)[k] = <i>a<sub>min</sub></i> if all &fnof;(<b>b</b><sub><i>i</i></sub>)[k] = 0
 *     </blockquote>
 *     </td>
 *     <td>
 *     <blockquote>
 *     <i>g</i>(<b>a</b>)[k] = <i>t</i><sub>min <i>i</i>: &fnof;(<b>b</b><sub><i>i</i></sub>)[k] = 0</sub>,
 *     or <i>g</i>(<b>a</b>)[k] = <i>a<sub>max</sub></i> if all &fnof;(<b>b</b><sub><i>i</i></sub>)[k] = 0
 *     </blockquote>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td colspan="2">
 *     In other words, the source array is split into bit slices, the bitwise operation is applied to all slices,
 *     and then we perform the backward conversion (joining) of slices set to a numeric array <i>g</i>(<b>a</b>).
 *     </td>
 *   </tr>
 * </table>
 * </div>
 *
 * <p>The conversion of the source array <b>a</b> to the target array <b>c</b>=<i>g</i>(<b>a</b>)
 * is performed by the main
 * method of this class: {@link #process(UpdatablePArray c, PArray a, Range range, long numberOfSlices)}.
 * The <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i> range and the number of slices
 * <tt>numberOfSlices</tt>=<i>n</i>+1 are specified as arguments of this method.
 * The original bitwise algorithm should be specified while creating an instance of this class by
 * {@link #getInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation, GeneralizedBitProcessing.RoundingMode)
 * getInstance} method.</p>
 *
 * <p>Note that the described operation does not require to calculate &fnof;(<b>b</b><sub>0</sub>)
 * in the round-down mode or &fnof;(<b>b</b><sub><i>n</i></sub>) in the round-up mode:
 * the result does not depend on it. Also note that the case <i>n</i>=0 is degenerated:
 * in this case always
 * <nobr><i>g</i>(<b>a</b>)[k] = <i>a<sub>min</sub></i></nobr> (round-down mode)
 * or <i>a<sub>max</sub></i> (round-up mode).</p>
 *
 * <p>Additional useful note: for some kinds of bitwise algorithms, you can improve the precision of the results
 * by replacing (after calling {@link #process(UpdatablePArray, PArray, Range, long) process} method)
 * the resulting array <b>c</b>=<i>g</i>(<b>a</b>)
 * with elementwise maximum, in case of round-down mode, or elementwise minimum, in case of round-up mode,
 * of <b>c</b> and the source array <b>a</b>: <nobr><b>c</b>=max(<b>c</b>,<b>a</b>)</nobr> or
 * <nobr><b>c</b>=min(<b>c</b>,<b>a</b>)</nobr> correspondingly.
 * You can do it easily by
 * {@link Arrays#applyFunc(ArrayContext, net.algart.math.functions.Func, UpdatablePArray, PArray...)} method
 * with {@link net.algart.math.functions.Func#MAX Func.MAX} or {@link net.algart.math.functions.Func#MIN Func.MIN}
 * argument.</p>
 *
 * <p>This class allocates, in {@link #process(UpdatablePArray, PArray, Range, long) process} method, some temporary
 * {@link UpdatableBitArray bit arrays}. Arrays are allocated with help of the memory model,
 * returned by <tt>context.{@link ArrayContext#getMemoryModel() getMemoryModel()}</tt> method
 * of the <tt>context</tt>, specified while creating an instance of this class.
 * If the context is <tt>null</tt>, or if necessary amount of memory is less than
 * {@link Arrays.SystemSettings#maxTempJavaMemory()}, then {@link SimpleMemoryModel} is used
 * for allocating temporary arrays. There is a special case when
 * {@link #process(UpdatablePArray, PArray, Range, long) process} method
 * is called for bit arrays (element type is <tt>boolean</tt>); in this case, no AlgART arrays
 * are allocated.
 *
 * <p>When this class allocates temporary AlgART arrays, it also checks whether the passed (source and destination)
 * arrays are <i>tiled</i>, i.e. they are underlying arrays of some matrices, created by
 * {@link Matrix#tile(long...)} or {@link Matrix#tile()} method.
 * Only the case, when these methods are implemented in this package, is recognized.
 * In this case, if the <tt>src</tt> array, passed to {@link #process(UpdatablePArray, PArray, Range, long) process}
 * method, is tiled, then the temporary AlgART arrays are tiled with the same tile structure.
 *
 * <p>This class uses multithreading (alike {@link Arrays#copy(ArrayContext, UpdatableArray, Array)}
 * and similar methods) to optimize calculations on multiprocessor or multi-core computers.
 * Namely, the {@link #process(UpdatablePArray, PArray, Range, long) process} method
 * processes different bit slices in several parallel threads.
 * However, it is not useful if the bitwise processing method {@link
 * GeneralizedBitProcessing.SliceOperation#processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int)}
 * already use multithreading optimization. In this case, please create an instance of this class via
 * {@link #getSingleThreadInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation,
 * GeneralizedBitProcessing.RoundingMode) getSingleThreadInstance} method.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithread access is necessary.
 * However, usually there are no reasons to use the same instance of this class in different threads:
 * usually there is much better idea to create a separate instance for every thread.</p>
 *
 * @author Daniel Alievsky
 */
public class GeneralizedBitProcessing extends AbstractArrayProcessorWithContextSwitching {

    /**
     * Rounding mode, in which {@link GeneralizedBitProcessing} class works: see comments to that class.
     */
    public enum RoundingMode {
        /**
         * Round-down mode.
         */
        ROUND_DOWN,
        /**
         * Round-up mode.
         */
        ROUND_UP
    }

    /**
     * Algorithm of processing bit arrays, that should be generalized for another element types via
     * {@link GeneralizedBitProcessing} class.
     */
    public static interface SliceOperation {
        /**
         * Processes the source bit array <tt>srcBits</tt> and saves the results in <tt>destBits</tt> bit array.
         * This method is called by
         * {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long)} method
         * for every {@link GeneralizedBitProcessing bit slice}</a> of the source non-bit array.
         * It is the main method, which you should implement to generalize some bit algorithm for non-bit case.
         *
         * <p>The <tt>destBits</tt> and <tt>srcBits</tt> arrays will be different bit arrays, allocated by
         * {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long) process} method
         * (according to the memory model, recommended by the {@link GeneralizedBitProcessing#context() context}),
         * if the {@link #isInPlaceProcessingAllowed()} method returns <tt>false</tt>.
         * If that method returns <tt>true</tt>, the <tt>destBits</tt> and <tt>srcBits</tt> arguments
         * will be references to the same bit array.
         *
         * <p>The index <i>i</i> of the slice, processed by this method, is passed via <tt>sliceIndex</tt> argument.
         * In other words, the threshold, corresponding to this slice, is
         *
         * <blockquote>
         * <i>t</i><sub><i>i</i></sub> =
         * <i>a<sub>min</sub></i> + <i>i</i> * (<i>a<sub>max</sub></i>&minus;<i>a<sub>min</sub></i>)/<i>n</i>,
         * &nbsp;<i>i</i> = <tt>sliceIndex</tt>
         * </blockquote>
         *
         * <p>(here <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i> is the range of processed values and
         * <i>n</i>+1 is the desired number of slices, passed via <tt>range</tt> and <tt>numberOfSlices</tt>
         * arguments of
         * {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long) process} method).
         * In the round-down mode <tt>sliceIndex</tt> is always in range <tt>1..</tt><i>n</i>,
         * and in the round-up mode it is always in range <tt>0..</tt><i>n</i><tt>-1</tt>.
         * The slice with index <i>i</i>=0 (round-down mode) or <i>i</i>=<i>n</i> (round-up mode)
         * is never processed, because the end result does not depend on it.
         * See comments to {@link GeneralizedBitProcessing} class for more details.
         *
         * <p>Please note that this method can be called simultaneously in different threads,
         * when {@link GeneralizedBitProcessing} class uses multithreading optimization.
         * In this case, <tt>threadIndex</tt> argument will be the index of the thread,
         * in which this method is called, i.e. an integer in
         *
         * <blockquote>
         * <tt>0..min(</tt><i>n</i><tt>-1,{@link GeneralizedBitProcessing#numberOfTasks()}&minus;1)</tt>
         * </blockquote>
         *
         * <p>range, where <i>n</i>+1 is the desired number of slices.
         * The high bound of this range, increased by 1, is also passed via <tt>numberOfThreads</tt> argument.
         * The <tt>threadIndex</tt> can be very useful if your algorithm requires some work memory or other objects:
         * in this case, your should allocate different work memory for different threads.
         *
         * <p>If multithreading optimization is not used, in particular, if the arrays, processed by
         * {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long) process} method,
         * are {@link BitArray bit arrays}, then <tt>threadIndex=0</tt> and <tt>numberOfThreads=1</tt>.
         *
         * @param context         the context of execution. It will be <tt>null</tt>, if (and only if)
         *                        the same argument of
         *                        {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long)
         *                        process} method is <tt>null</tt>; in this case, the context should be ignored.
         *                        The main purpose of the context is to allow interruption of this method via
         *                        {@link ArrayContext#checkInterruption()} and to allocate
         *                        work memory via {@link ArrayContext#getMemoryModel()}.
         * @param destBits        the destination bit array, where results of processing should be stored.
         * @param srcBits         the source bit array for processing.
         * @param sliceIndex      the index of the currently processed slice,
         *                        from <tt>1</tt> to <i>n</i> in the round-down mode or
         *                        from <tt>0</tt> to <i>n</i><tt>-1</tt> the round-up mode
         *                        (<i>n</i> is the desired number of slices minus 1).
         * @param threadIndex     the index of the current thread (different for different threads in a case of
         *                        multithreading optimization).
         * @param numberOfThreads the maximal possible value of <tt>threadIndex+1</tt>: equal to
         *                        <nobr><tt>min(numberOfSlices&minus;1,{@link
         *                        GeneralizedBitProcessing#numberOfTasks()})</tt></nobr>,
         *                        where <tt>numberOfSlices</tt>=<i>n</i>+1 is the argument of
         *                        {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long)
         *                        process} method.
         * @throws NullPointerException if <tt>srcBits</tt> or <tt>destBits</tt> argument is <tt>null</tt>.
         */
        public void processBits(ArrayContext context, UpdatableBitArray destBits, BitArray srcBits,
            long sliceIndex, int threadIndex, int numberOfThreads);

        /**
         * Indicates whether this algorithm can work in place.
         *
         * <p>Some algorithms, processing bit arrays, can work in place, when the results are stored in the source
         * array. In this case, this method should return <tt>true</tt>. If it returns <tt>true</tt>,
         * {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long)} method
         * saves memory and time by passing the same bit array as <tt>destBits</tt> and <tt>srcBits</tt> arguments
         * of {@link #processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int) processBits} method.
         * If it returns <tt>false</tt>,
         * {@link GeneralizedBitProcessing#process(UpdatablePArray, PArray, Range, long)} method
         * allocates 2 different bit arrays for source and resulting bit arrays and passes
         * them as <tt>destBits</tt> and <tt>srcBits</tt> arguments
         * of {@link #processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int) processBits} method.
         *
         * @return <tt>true</tt> if {@link #processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int)
         *         processBits} method can work correctly when <tt>destBits==srcBits</tt> or <tt>false</tt>
         *         if that method requires different source and destination arrays.
         */
        public boolean isInPlaceProcessingAllowed();
    }

    private final ThreadPoolFactory threadPoolFactory;
    private final int numberOfTasks;
    private final SliceOperation sliceOperation;
    private final RoundingMode roundingMode;
    private final boolean inPlaceProcessingAllowed;

    private GeneralizedBitProcessing(ArrayContext context,
        SliceOperation sliceOperation, RoundingMode roundingMode, int numberOfTasks)
    {
        super(context);
        Objects.requireNonNull(sliceOperation, "Null sliceOperation argument");
        Objects.requireNonNull(roundingMode, "Null roundingMode argument");
        this.threadPoolFactory = Arrays.getThreadPoolFactory(context);
        this.numberOfTasks = numberOfTasks > 0 ? numberOfTasks :
            Math.max(1, this.threadPoolFactory.recommendedNumberOfTasks());
        this.sliceOperation = sliceOperation;
        this.roundingMode = roundingMode;
        this.inPlaceProcessingAllowed = sliceOperation.isInPlaceProcessingAllowed();
    }

    /**
     * Returns new instance of this class.
     *
     * @param context        the {@link #context() context} that will be used by this object;
     *                       may be <tt>null</tt>, then it will be ignored, and all temporary arrays
     *                       will be created by {@link SimpleMemoryModel}.
     * @param sliceOperation the bit processing operation that will be generalized by this instance.
     * @param roundingMode   the rounding mode, used by the created instance.
     * @return               new instance of this class.
     * @throws NullPointerException if <tt>sliceOperation</tt> or <tt>roundingMode</tt> argument is <tt>null</tt>.
     * @see #getSingleThreadInstance(ArrayContext, SliceOperation, net.algart.arrays.GeneralizedBitProcessing.RoundingMode)
     */
    public static GeneralizedBitProcessing getInstance(ArrayContext context,
        SliceOperation sliceOperation, RoundingMode roundingMode)
    {
        return new GeneralizedBitProcessing(context, sliceOperation, roundingMode, 0);
    }

    /**
     * Returns new instance of this class, that does not use multithreading optimization.
     * Usually this class performs calculations in many threads
     * (different slides in different threads), according to information from the passed context,
     * but an instance, created by this method, does not perform this.
     * It is the best choice if the operation, implemented by passed <tt>sliceOperation</tt> object,
     * already uses multithreading.
     *
     * @param context        the {@link #context() context} that will be used by this object;
     *                       may be <tt>null</tt>, then it will be ignored, and all temporary arrays
     *                       will be created by {@link SimpleMemoryModel}.
     * @param sliceOperation the bit processing operation that will be generalized by this instance.
     * @param roundingMode   the rounding mode, used by the created instance.
     * @return               new instance of this class.
     * @throws NullPointerException if <tt>sliceOperation</tt> or <tt>roundingMode</tt> argument is <tt>null</tt>.
     * @see #getInstance(ArrayContext, SliceOperation, net.algart.arrays.GeneralizedBitProcessing.RoundingMode)
     */
    public static GeneralizedBitProcessing getSingleThreadInstance(ArrayContext context,
        SliceOperation sliceOperation, RoundingMode roundingMode)
    {
        return new GeneralizedBitProcessing(context, sliceOperation, roundingMode, 1);
    }

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <tt>newContext</tt> for all operations.
     * The returned instance is a clone of this one, but there is no guarantees
     * that it is a deep clone.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another context.
     */
    @Override
    public GeneralizedBitProcessing context(ArrayContext newContext) {
        return newContext == this.context() ? this :
            new GeneralizedBitProcessing(newContext, this.sliceOperation, this.roundingMode, this.numberOfTasks);
    }

    /**
     * Returns the bit processing algorithm, used by this instance.
     * More precisely, this method just returns a reference to the <tt>sliceOperation</tt> object,
     * passed to {@link
     * #getInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation, GeneralizedBitProcessing.RoundingMode)
     * getInstance} or {@link #getSingleThreadInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation,
     * GeneralizedBitProcessing.RoundingMode)  getSingleThreadInstance} method.
     *
     * @return the bit processing algorithm, used by this instance.
     */
    public SliceOperation sliceOperation() {
        return this.sliceOperation;
    }

    /**
     * Returns the rounding mode, used by this instance.
     * More precisely, this method just returns a reference to the <tt>roundingMode</tt> object,
     * passed to {@link
     * #getInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation, GeneralizedBitProcessing.RoundingMode)
     * getInstance} or {@link #getSingleThreadInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation,
     * GeneralizedBitProcessing.RoundingMode)  getSingleThreadInstance} method.
     *
     * @return the roundingMode, used by this instance.
     */
    public RoundingMode roundingMode() {
        return this.roundingMode;
    }

    /**
     * Returns the number of threads, that this class uses for multithreading optimization.
     * It is equal to:
     * <ul>
     * <li>1 if this instance was created by
     * {@link #getSingleThreadInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation,
     * GeneralizedBitProcessing.RoundingMode) getSingleThreadInstance} method or</li>
     * <li><tt>context.{@link ArrayContext#getThreadPoolFactory()
     * getThreadPoolFactory()}.{@link ThreadPoolFactory#recommendedNumberOfTasks() recommendedNumberOfTasks()}</tt>
     * if it was created by {@link #getInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation,
     * GeneralizedBitProcessing.RoundingMode) getInstance} method.</li>
     * </ul>
     *
     * <p>(As in {@link Arrays#copy(ArrayContext, UpdatableArray, Array)}, if the <tt>context</tt> argument of
     * <nobr>{@link
     * #getInstance(ArrayContext, GeneralizedBitProcessing.SliceOperation, GeneralizedBitProcessing.RoundingMode)
     * getInstance}</nobr> method
     * is <tt>null</tt>, then {@link DefaultThreadPoolFactory} is used.)
     *
     * <p>Note that the real number of parallel threads will be a minimum from this value and
     * <i>n</i>, where <i>n</i>+1 is the desired number of slices (the last argument of
     * {@link #process(UpdatablePArray, PArray, Range, long)} method).
     *
     * @return the number of threads, that this class uses for multithreading optimization.
     */
    public int numberOfTasks() {
        return this.numberOfTasks;
    }

    /**
     * Performs processing of the source array <tt>src</tt>, with saving results in <tt>dest</tt> array,
     * on the base of the bit processing algorithm, specified for this instance.
     * Namely, the source array <tt>src</tt> is splitted to bit slices, each slice is processed by
     * {@link
     * GeneralizedBitProcessing.SliceOperation#processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int)}
     * method of the {@link #sliceOperation()} object (representing the used bit processing algorithm),
     * and the processed slices are joined to the resulting array <tt>dest</tt>.
     * See the precise description of this generalization of a bit processing algorithm
     * in the {@link GeneralizedBitProcessing comments to this class}.
     *
     * <p>The <tt>src</tt> and <tt>dest</tt> arrays must not be the same array or views of the same array;
     * in other case, the results will be incorrect.
     * These arrays must have the same element types and the same lengths; in other case,
     * an exception will occur.
     *
     * <p>The <tt>range</tt> argument specifies the <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i> range,
     * used for splitting to bit slices.
     * If you do not want to lose too little or too big values in the processed array, this range should
     * contain all possible values of <tt>src</tt> array.
     * The simplest way to provide this is using the result of {@link Arrays#rangeOf(PArray) Arrays.rangeOf(src)}.
     *
     * <p>The <tt>numberOfSlices</tt> argument is the number of bit slices, used for splitting the <tt>src</tt> array,
     * which is equal to <i>n</i>+1 in the {@link GeneralizedBitProcessing comments to this class}.
     * Less values of this argument increases speed, larger values increases precision of the result
     * (only <tt>numberOfSlices</tt> different values are possible for <tt>dest</tt> elements).
     * If the element type is a fixed-point type (<tt>src</tt> and <tt>dest</tt> are {@link PFixedArray} instance),
     * this argument is automatically truncated to
     * <nobr><tt>min(numberOfSlices, (long)(range.{@link Range#size() size()}+1.0))</tt></nobr>,
     * because larger values cannot increase the precision.
     *
     * <p>Please remember that <tt>numberOfSlices=1</tt> (<i>n</i>=0) is a degenerated case: in this case,
     * the <tt>dest</tt> array is just filled by <tt>range.min()</tt>
     * (round-down mode) or <tt>range.min()</tt> (round-up mode),
     * alike in {@link UpdatablePArray#fill(double)} method.
     *
     * <p>If the element type is <tt>boolean</tt> ({@link BitArray}), then a generalization of the bitwise algorithm
     * is not necessary. In this case, if <tt>numberOfSlices&gt;1</tt>, this method just calls
     * <tt>{@link #sliceOperation()}.{@link
     * GeneralizedBitProcessing.SliceOperation#processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int)
     * processBits}</tt> method for the passed <tt>dest</tt> and <tt>src</tt> arrays.
     * (However, according to the specification of {@link GeneralizedBitProcessing.SliceOperation}, if its
     * {@link GeneralizedBitProcessing.SliceOperation#isInPlaceProcessingAllowed() isInPlaceProcessingAllowed()}
     * method returns <tt>true</tt>, then <tt>src</tt> arrays is firstly copied into <tt>dest</tt>,
     * and then the <tt>dest</tt> arrays is passed as both <tt>srcBits</tt> and <tt>destBits</tt> arguments.)
     *
     * <p>Note: if the element type is <tt>boolean</tt>, then multithreading is never used:
     * {@link
     * GeneralizedBitProcessing.SliceOperation#processBits(ArrayContext, UpdatableBitArray, BitArray, long, int, int)
     * processBits} method is called
     * in the current thread, and its <tt>threadIndex</tt> and <tt>numberOfThreads</tt> arguments are
     * <tt>0</tt> and <tt>1</tt> correspondingly.
     *
     * @param dest           the result of processing.
     * @param src            the source AlgART array.
     * @param range          the <i>a<sub>min</sub></i>..<i>a<sub>max</sub></i> range,
     *                       used for splitting to bit slices.
     * @param numberOfSlices the number of bit slices (i.e. <i>n</i>+1); must be positive.
     * @throws NullPointerException     if one of the arguments is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>dest</tt> and <tt>src</tt> arrays have different lengths
     *                                  or element types, or if <tt>numberOfSlices&lt;=0</tt>.
     */
    public void process(UpdatablePArray dest, PArray src, Range range, long numberOfSlices) {
        Objects.requireNonNull(dest, "Null dest argument");
        Objects.requireNonNull(src, "Null src argument");
        Objects.requireNonNull(range, "Null range argument");
        if (src.elementType() != dest.elementType()) {
            throw new IllegalArgumentException("Different element types of src and dest arrays ("
                + src.elementType().getCanonicalName() + " and " + dest.elementType().getCanonicalName() + ")");
        }
        if (dest.length() != src.length()) {
            throw new SizeMismatchException("Different lengths of src and dest arrays ("
                + src.length() + " and " + dest.length() + ")");
        }
        if (numberOfSlices <= 0) {
            throw new IllegalArgumentException("numberOfSlices must be positive");
        }
        if (src instanceof PFixedArray) {
            numberOfSlices = Math.min(numberOfSlices, (long)(range.size() + 1.0));
        }
        if (numberOfSlices >= 2 && src instanceof BitArray) {
            if (inPlaceProcessingAllowed) {
                Arrays.copy(contextPart(0.0, 0.05), dest, src);
                this.sliceOperation.processBits(contextPart(0.05, 1.0),
                    (UpdatableBitArray)dest, (BitArray)dest, 1, 0, 1);
            } else {
                this.sliceOperation.processBits(context(), (UpdatableBitArray)dest, (BitArray)src, 1, 0, 1);
            }
            return;
        }
        long numberOfRanges = numberOfSlices - 1;
        // numberOfSlices thresholds split the range into numberOfRanges ranges:
        // range.min(), range.min() + range.size() / numberOfRanges, ..., range.max()
        ArrayContext acFill = numberOfRanges == 0 ? context() : contextPart(0.0, 0.1 / numberOfRanges);
        ConstantFunc filler;
        switch (roundingMode) {
            case ROUND_DOWN:
                filler = ConstantFunc.getInstance(range.min());
                break;
            case ROUND_UP:
                filler = ConstantFunc.getInstance(range.max());
                break;
            default:
                throw new AssertionError();
        }
        Arrays.copy(acFill, dest, Arrays.asIndexFuncArray(filler, dest.type(), dest.length()));
        // equivalent to dest.fill(range.min()), but supports context (important for a case of very large disk array)
        if (numberOfRanges == 0) { // degenerated case: numberOfSlices == 1
            return;
        }
        assert numberOfSlices >= 2;
        Runnable[] tasks = createTasks(contextPart(0.1 / numberOfRanges, 1.0), dest, src, range, numberOfRanges);
        threadPoolFactory.performTasks(tasks);
    }

    private Runnable[] createTasks(ArrayContext context,
        final UpdatablePArray dest, final PArray src, final Range range, final long numberOfRanges)
    {
        long len = src.length();
        assert dest.length() == len;
        assert dest.elementType() == src.elementType();
        assert numberOfRanges >= 1 : "1 slice must be processed by trivial filling out of this method";
        int nt = (int)Math.min(this.numberOfTasks, numberOfRanges);
        UpdatableBitArray[] srcBits = allocateMemory(nt, src);
        UpdatableBitArray[] destBits = this.inPlaceProcessingAllowed ? srcBits : allocateMemory(nt, src);
        Runnable[] tasks = new Runnable[nt];
        AtomicLong readyLayers = new AtomicLong(0);
        AtomicBoolean interruptionRequest = new AtomicBoolean(false);
        if (context != null && nt > 1) {
            context = context.singleThreadVersion();
        }
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        for (int threadIndex = 0; threadIndex < tasks.length; threadIndex++) {
            SliceProcessingTask task = new SliceProcessingTask(context, threadIndex, nt, lock, condition);
            task.setProcessedData(dest, src, destBits, srcBits);
            task.setRanges(range, numberOfRanges);
            task.setSynchronizationVariables(readyLayers, interruptionRequest);
            tasks[threadIndex] = task;
        }
        return tasks;
    }

    private UpdatableBitArray[] allocateMemory(int numberOfTasks, PArray src) {
        long nba = this.sliceOperation.isInPlaceProcessingAllowed() ?
            numberOfTasks :
            2 * (long)numberOfTasks;
        long arrayLength = src.length();
        MemoryModel mm = Arrays.sizeOf(boolean.class, arrayLength) < Arrays.SystemSettings.maxTempJavaMemory() / nba ?
            SimpleMemoryModel.getInstance() :
            memoryModel();
        UpdatableBitArray[] result = new UpdatableBitArray[numberOfTasks];
        Matrix<? extends PArray> srcBaseMatrix = !Arrays.isTiled(src) ? null :
            ((ArraysTileMatrixImpl.TileMatrixArray)src).baseMatrix().cast(PArray.class);
        for (int k = 0; k < result.length; k++) {
            result[k] = mm.newUnresizableBitArray(arrayLength);
            if (srcBaseMatrix != null) {
                result[k] = srcBaseMatrix.matrix(result[k]).tile(Arrays.tileDimensions(src)).array();
            }
        }
        return result;
    }

    private class SliceProcessingTask implements Runnable {
        private final ArrayContext context;
        private final int threadIndex;
        private final int numberOfThreads;
        private final Lock lock;
        private final Condition condition;

        private Range range;
        private long numberOfRanges;

        private AtomicLong readyLayers;
        private AtomicBoolean interruptionRequest;

        private UpdatablePArray dest;
        private PArray src;
        private UpdatableBitArray[] srcBits;
        private UpdatableBitArray[] destBits;

        private SliceProcessingTask(ArrayContext context,
            int threadIndex, int numberOfThreads, Lock lock, Condition condition)
        {
            this.context = context;
            this.threadIndex = threadIndex;
            this.numberOfThreads = numberOfThreads;
            this.lock = lock;
            this.condition = condition;
        }

        public void setRanges(Range range, long numberOfRanges) {
            this.range = range;
            this.numberOfRanges = numberOfRanges;
        }

        private void setProcessedData(UpdatablePArray dest, PArray src,
            UpdatableBitArray[] destBits, UpdatableBitArray[] srcBits)
        {
            this.dest = dest;
            this.src = src;
            this.destBits = destBits;
            this.srcBits = srcBits;
        }

        private void setSynchronizationVariables(AtomicLong readyLayers, AtomicBoolean interruptionRequest) {
            this.readyLayers = readyLayers;
            this.interruptionRequest = interruptionRequest;
        }

        public void run() {
            if (lock == null || range == null || dest == null || src == null) {
                throw new AssertionError(this + " is not initialized correctly");
            }
            for (long sliceIndex = threadIndex + 1; sliceIndex <= numberOfRanges; sliceIndex += numberOfThreads) {
                // threadIndex+1 because processing the layer #0 is trivial and should be skipped
                double value;
                switch (roundingMode) {
                    case ROUND_DOWN:
                        value = sliceIndex == numberOfRanges ? range.max() : // to be on the safe side
                            range.min() + range.size() * (double)sliceIndex / numberOfRanges;
                        break;
                    case ROUND_UP:
                        value = sliceIndex == numberOfRanges ? range.min() : // to be on the safe side
                            range.max() - range.size() * (double)sliceIndex / numberOfRanges;
                        break;
                    default:
                        throw new AssertionError();
                }
                ArrayContext acToBit, acOp, acFromBit;
                if (context != null) {
                    ArrayContext ac = context.part(sliceIndex - 1, sliceIndex, numberOfRanges);
                    acToBit = numberOfThreads == 1 ? ac.part(0.0, 0.05) : context.noProgressVersion();
                    acOp = numberOfThreads == 1 ? ac.part(0.05, 0.95) : context.noProgressVersion();
                    acFromBit = ac.part(0.95, 1.0);
                } else {
                    acToBit = acOp = acFromBit = ArrayContext.DEFAULT_SINGLE_THREAD;
                    // if context==null, then numberOfThreads is the default system recommended number of threads;
                    // if it is >1, then we need provide single-thread processing in every slice,
                    // and if it is 1, then "null" context works like DEFAULT_SINGLE_THREAD
                }
                if (interruptionRequest.get()) {
                    return;
                }
//                long t1 = System.nanoTime();
                switch (roundingMode) {
                    case ROUND_DOWN:
                        Arrays.packBitsGreaterOrEqual(acToBit, srcBits[threadIndex], src, value);
                        break;
                    case ROUND_UP:
                        Arrays.packBitsGreater(acToBit, srcBits[threadIndex], src, value);
                        break;
                }

                // Below is a slower code (commented out), equivalent to this "packBitsGreaterOrEqual"
                // (but not to "packBitsLessOrEqual"):
                //
                // Arrays.applyFunc(acToBit,
                //     net.algart.math.functions.RectangularFunc.getInstance(value,
                //         Double.POSITIVE_INFINITY, 1.0, 0.0), srcBits[threadIndex], src);

//                long t2 = System.nanoTime();
                sliceOperation.processBits(acOp, destBits[threadIndex], srcBits[threadIndex],
                    roundingMode == RoundingMode.ROUND_DOWN ?
                        sliceIndex :
                        numberOfRanges - sliceIndex,
                    threadIndex, numberOfThreads);
//                long t3 = System.nanoTime();
                lock.lock();
                try {
                    if (interruptionRequest.get()) {
                        return; // important to avoid writing anywhere if interruption is necessary
                    }
                    while (readyLayers.get() < sliceIndex - 1) {
                        // This loop cannot be infinite.
                        // Proof.
                        // Note that sliceIndex takes all values m=1,2,...,numberOfRanges in some order.
                        // Let's prove by induction with m, that this loop is not infinite when sliceIndex=m.
                        // 1. If sliceIndex=m=1, it is obvious: readyLayers.get() is never < 0.
                        // 2. Let's suppose, that this loop was successfully performed and finished in some thread,
                        // when sliceIndex was equal to m-1. And let now sliceIndex be equal to m.
                        // Because this loop was successfully performed with sliceIndex = m-1,
                        // so, readyLayers.get() had become >= m-2. Some time after this,
                        // readyLayers.incrementAndGet() operator below was executed,
                        // and readyLayers.get() must become >= m-1.
                        // At this moment, this loop must finish with sliceIndex = m.
                        try {
                            condition.await(100, TimeUnit.MILLISECONDS);
                            // 100 ms - to be on the safe side, if "signalAll" has not help
                        } catch (InterruptedException e) {
                            interruptionRequest.set(true);
                            return;
                        }
                        if (context != null) {
                            context.checkInterruption();
                        }
                        // to be on the safe side, allow interruption even in a case of a bug here
                    }
                    switch (roundingMode) {
                        case ROUND_DOWN:
                            Arrays.unpackUnitBits(acFromBit, dest, destBits[threadIndex], value);
                            break;
                        case ROUND_UP:
                            Arrays.unpackZeroBits(acFromBit, dest, destBits[threadIndex], value);
                            break;
                    }

                    // Below is a slower code (commented out), equivalent to this "unpackUnitBits"
                    // (but not to "unpackZeroBits"), which, however,
                    // does not need waiting for readyLayers.get() == sliceIndex-1:
                    //
                    // PArray castBits = Arrays.asFuncArray(
                    //     net.algart.math.functions.LinearFunc.getInstance(0.0, value),
                    //     dest.type(), destBits[threadIndex]);
                    // Arrays.applyFunc(acFromBit, net.algart.math.functions.Func.MAX,
                    //     dest, dest, castBits);

                    long ready = readyLayers.incrementAndGet();
                    if (ready != sliceIndex) {
                        throw new AssertionError("Invalid synchronization in " + GeneralizedBitProcessing.class);
                    }
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
//                System.out.printf("%d: %.5f + %.5f + %.5f, %d tasks (%s <-- %s)%n",
//                    sliceIndex, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (System.nanoTime() - t3) * 1e-6,
//                    numberOfThreads, destBits[threadIndex], srcBits[threadIndex]);
            }
        }
    }

}
