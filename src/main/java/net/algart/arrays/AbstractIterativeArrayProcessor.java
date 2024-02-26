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

import java.util.Locale;

/**
 * <p>A skeletal implementation of the {@link IterativeArrayProcessor} interface.
 * Usually, you need to extend this class to implement that interface.</p>
 *
 * <p>All non-abstract methods are completely implemented here and usually
 * should not be overridden in subclasses.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractIterativeArrayProcessor<T>
    implements IterativeArrayProcessor<T>
{
    /**
     * Current execution context. It is returned by {@link #context()} method.
     */
    protected final ArrayContext context;

    /**
     * The memory model used by this instance for all operations.
     * Equal to {@link #context}.{@link ArrayContext#getMemoryModel() getMemoryModel()} if
     * <tt>{@link #context}!=null</tt>, in other case equal to
     * {@link SimpleMemoryModel#getInstance()}.
     */
    protected final MemoryModel memoryModel;

    /**
     * Creates an instance of this class with the given context.
     *
     * @param context the context used by this instance for all operations (will be saved in {@link #context} field).
     */
    protected AbstractIterativeArrayProcessor(ArrayContext context) {
        this.context = context;
        this.memoryModel = context == null ? SimpleMemoryModel.getInstance() : context.getMemoryModel();
    }

    public abstract void performIteration(ArrayContext context);

    public abstract boolean done();

    public abstract long estimatedNumberOfIterations();

    public abstract T result();

    public abstract void freeResources(ArrayContext context);

    public ArrayContext context() {
        return context;
    }

    /**
     * <p>This method returns <tt><nobr>context == null ? null :
     * context.{@link ArrayContext#part(double, double) part}(fromPart, toPart))</nobr></tt>.
     * This operation is needful very often while implementing {@link #performIteration(ArrayContext)} method.
     *
     * @param context  some array context.
     * @param fromPart the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the start of the subtask:
     *                 see {@link ArrayContext#updateProgress(net.algart.arrays.ArrayContext.Event)
     *                 ArrayContext.updateProgress} method
     * @param toPart   the estimated ready part, from 0.0 to 1.0,
     *                 of the total algorithm at the finish of the subtask:
     *                 see {@link ArrayContext#updateProgress(net.algart.arrays.ArrayContext.Event)
     *                 ArrayContext.updateProgress} method;
     *                 must be not less than <tt>fromPart</tt> range.
     * @return         new context, describing the execution of the subtask of the current task.
     * @throws IllegalArgumentException if <tt>fromPart</tt> or <tt>toPart</tt> is not in
     *                                  <tt>0.0..1.0</tt> range or if <tt>fromPart&gt;toPart</tt>.
     */
    public static ArrayContext part(ArrayContext context, double fromPart, double toPart) {
        return context == null ? null : context.part(fromPart, toPart);
    }

    public T process() {
        long t1 = Arrays.CONFIG_LOGGABLE ? System.nanoTime() : 0;
        long totalCount = -1;
        long remainingCount = -1;
        double unknownRemains = this instanceof LimitedIterations<?> ? 0.0 : 0.2;
        // LimitedIterations has precise and quick estimatedNumberOfIterations() method
        double unknownStep = 0.01;
        ArrayContext estimatedContext = part(context, 0.0, 1.0 - unknownRemains);
        // We start estimation from 0..80%; then estimate full task as 80..81%, ..., 98..99%,
        // then 99.00..99.05%, 99.05%..99.10%, ..., etc.
        // If future estimations of number of iterations will be bad, the progress bar will quickly become ~100%
        boolean done = done();
        T result = done ? result() : null;
        long count = 0;
        for (; !done; count++) {
            if (totalCount <= 0) {
                assert remainingCount <= 0;
                totalCount = estimatedNumberOfIterations();
                if (totalCount > 0) {
                    remainingCount = totalCount; // initializing counter
                }
            }
            ArrayContext subContext = estimatedContext;
            // subContext is this context before totalCount becomes positive
            if (remainingCount > 0) {
                assert totalCount > 0;
                assert remainingCount <= totalCount;
                double fromPart = (double)(totalCount - remainingCount) / (double)totalCount;
                remainingCount--;
                double toPart = Math.min(1.0, (double)(totalCount - remainingCount) / (double)totalCount);
                // Math.min - to be on the safe side
                subContext = part(subContext, fromPart, toPart);
            }
            performIteration(subContext);
            done = done();
            if (remainingCount == 0 && !done) {
                if (unknownRemains <= unknownStep * 1.05) {
                    unknownStep *= 0.05;
                }
                estimatedContext = part(context, 1.0 - unknownRemains, 1.0 - (unknownRemains - unknownStep));
                unknownRemains -= unknownStep;
                totalCount = 0; // invoke reinitializing estimatedContext
            }
            result = result();
            if (context != null)
                context.checkInterruption();
            Array resultArray = resultArray(result);
            if (resultArray != null) {
                long len = resultArray.length();
                ArrayContext c = done ? context : subContext;
                if (c != null)
                    c.updateProgress(new ArrayContext.Event(resultArray.elementType(), len, len));
            }
//            System.out.print(count + "  ");
        }
        if (Arrays.CONFIG_LOGGABLE && Arrays.SystemSettings.profilingMode()) {
            long t2 = System.nanoTime();
            if (t2 - t1 > (long)Arrays.SystemSettings.RECOMMENDED_ELAPSED_TIME_FOR_ADDITIONAL_LOGGING * 1000000L) {
                // there is no sense to spend time for logging very quick algorithms
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                StringBuilder stackInfo = new StringBuilder();
                for (int k = 1; k < stack.length; k++) {
                    String methodName = stack[k].getMethodName();
                    String className = stack[k].getClassName();
                    if (stackInfo.length() == 0) {
                        if ("process".equals(methodName) &&
                            AbstractIterativeArrayProcessor.class.getName().equals(className)) {
                            continue;
                        }
                    } else {
                        stackInfo.append("/");
                    }
                    stackInfo.append(stack[k].getMethodName());
                }
                Array resultArray = resultArray(result);
                Arrays.LOGGER.config(String.format(Locale.US,
                    "Array / matrix is processed in %d iterations in %.3f ms "
                    + "(%.4f ms/iteration" + (result == null ? "" : ", %.3f ns/element") + ") by " + this
                    + (resultArray == null ? " " : " [" + result + "] ") + "in " + stackInfo,
                    count, 1e-6 * (t2 - t1),
                    1e-6 * (t2 - t1) / Math.max(1.0, count),
                    resultArray == null ? null :
                    (t2 - t1) / Math.max(1.0, count) / resultArray.length()));
            }
        }
        return result;
    }

    public IterativeArrayProcessor<T> limitIterations(long maxNumberOfIterations) {
        if (maxNumberOfIterations < 0)
            return this;
        return new LimitedIterations<T>(this, maxNumberOfIterations);
    }

    public IterativeArrayProcessor<T> chain(IterativeArrayProcessor<T> followingProcessor, double weight) {
        if (followingProcessor == null)
            throw new NullPointerException("Null followingProcessor argument");
        if (weight < 0.0)
            throw new IllegalArgumentException("Negative weight");
        return new Chain<T>(this, followingProcessor, weight);
    }

    private static Array resultArray(Object result) {
        if (result instanceof Array) {
            return (Array)result;
        } else if (result instanceof Matrix<?>) {
            return ((Matrix<?>)result).array();
        } else {
            return null;
        }
    }

    private static class LimitedIterations<T> extends AbstractIterativeArrayProcessor<T> {
        private final IterativeArrayProcessor<T> proc;
        private final long maxNumberOfIterations;
        private long numberOfIterations;

        private LimitedIterations(IterativeArrayProcessor<T> proc, long maxNumberOfIterations) {
            super(proc.context());
            assert maxNumberOfIterations >= 0 : "Negative maxNumberOfIterations";
            this.proc = proc;
            this.maxNumberOfIterations = maxNumberOfIterations;
            this.numberOfIterations = maxNumberOfIterations;
        }

        @Override
        public void performIteration(ArrayContext context) {
            proc.performIteration(context);
            if (numberOfIterations > 0) // for a case when a client calls this method after done()
                numberOfIterations--;
        }

        @Override
        public boolean done() {
            assert numberOfIterations >= 0 : "negative numberOfIterations = " + numberOfIterations;
            return numberOfIterations == 0 || proc.done();
        }

        @Override
        public long estimatedNumberOfIterations() {
            return numberOfIterations;
        }

        @Override
        public T result() {
            return proc.result();
        }

        @Override
        public void freeResources(ArrayContext context) {
            proc.freeResources(context);
        }

        @Override
        public String toString() {
            return proc.toString() + ", limited by " + maxNumberOfIterations + " iterations";
        }
    }

    private static class Chain<T> extends AbstractIterativeArrayProcessor<T> {
        private final IterativeArrayProcessor<T> proc1, proc2;
        private final double weight2;

        private Chain(IterativeArrayProcessor<T> proc1, IterativeArrayProcessor<T> proc2, double weight2) {
            super(proc1.context());
            assert proc2 != null : "Null proc2";
            assert weight2 >= 0 : "Negative weight2";
            this.proc1 = proc1;
            this.proc2 = proc2;
            this.weight2 = weight2;
        }

        @Override
        public void performIteration(ArrayContext context) {
            if (!proc1.done()) {
                proc1.performIteration(context);
            } else {
                proc2.performIteration(context);
            }
        }

        @Override
        public boolean done() {
            return proc1.done() && proc2.done();
        }

        @Override
        public long estimatedNumberOfIterations() {
            if (!proc1.done()) {
                long n1 = proc1.estimatedNumberOfIterations();
                long n2 = proc2.estimatedNumberOfIterations();
                if (n1 <= 0 || n2 <= 0)
                    return 0;
                return n1 + Math.round(weight2 * n2);
            } else {
                return proc2.estimatedNumberOfIterations();
            }
        }

        @Override
        public T result() {
            return !proc1.done() ? proc1.result() : proc2.result();
        }

        @Override
        public void freeResources(ArrayContext context) {
            proc1.freeResources(context == null ? null : context.part(0.0, 0.5));
            proc2.freeResources(context == null ? null : context.part(0.5, 1.0));
        }

        @Override
        public String toString() {
            return "chain " + proc1.toString() + " | [weight " + weight2 + "] " + proc2.toString();
        }
    }
}
