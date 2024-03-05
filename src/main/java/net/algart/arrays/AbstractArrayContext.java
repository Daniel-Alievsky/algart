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
 * <p>A skeletal implementation of the {@link ArrayContext} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>All non-abstract methods of this class are completely implemented
 * and usually should not be overridden.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractArrayContext implements ArrayContext {
    public ArrayContext part(final double fromPart, final double toPart) {
        if (fromPart < 0.0)
            throw new IllegalArgumentException("Illegal fromPart=" + fromPart + " (must be in range 0.0..1.0)");
        if (toPart > 1.0)
            throw new IllegalArgumentException("Illegal toPart=" + toPart + " (must be in range 0.0..1.0)");
        if (fromPart > toPart)
            throw new IllegalArgumentException("Illegal fromPart=" + fromPart + " or toPart=" + toPart
                + " (fromPart must not be greater than toPart)");
        if (this instanceof NoProgressVersion || this instanceof Default) { // in particular, DefaultSingleThread
            return this;
        }
        return new Subtask(fromPart, toPart);
    }

    public ArrayContext part(long from, long to, long total) {
        if (total < 0)
            throw new IllegalArgumentException("Negative total=" + total);
        if (from < 0)
            throw new IllegalArgumentException("Illegal from=" + from + " (must be in range 0.." + total + ")");
        if (to > total)
            throw new IllegalArgumentException("Illegal to=" + to + " (must be in range 0.." + total + ")");
        if (from > to)
            throw new IllegalArgumentException("Illegal from=" + from + " or to=" + to
                + " (\"from\" must not be greater than \"to\")");
        if (total == 0) {
            assert from == 0;
            assert to == 0;
            return part(0.0, 1.0);
        }
        return part((double)from / (double)total, to == total ? 1.0 : (double)to / (double)total);
    }

    public ArrayContext noProgressVersion() {
        return new NoProgressVersion();
    }

    public ArrayContext singleThreadVersion() {
        return new SingleThreadVersion();
    }

    public ArrayContext multithreadingVersion(int currentThreadIndex, int numberOfThreads) {
        if (numberOfThreads <= 0)
            throw new IllegalArgumentException("Zero or negative number of threads " + numberOfThreads
                + " while creating multithreading array context");
        if (currentThreadIndex < 0)
            throw new IllegalArgumentException("Negative index of the current thread " + currentThreadIndex
                + " while creating multithreading array context");
        if (currentThreadIndex >= numberOfThreads)
            throw new IllegalArgumentException("Index of the current thread " + currentThreadIndex
                + " is out of range 0..(numberOfThreads-1)=" + (numberOfThreads - 1)
                + " while creating multithreading array context");
        if (currentThreadIndex == currentThreadIndex() && numberOfThreads == numberOfThreads()) {
            return this;
        }
        return new MultithreadingVersion(currentThreadIndex, numberOfThreads);
    }

    public ArrayContext customDataVersion(Object customData) {
        if (customData == customData()) {
            return this;
        }
        return new CustomDataVersion(customData);
    }

    public abstract MemoryModel getMemoryModel();

    public abstract ThreadPoolFactory getThreadPoolFactory();

    public abstract void checkInterruption() throws RuntimeException;

    public abstract void updateProgress(Event event);

    public final void checkInterruptionAndUpdateProgress(Class<?> elementType, long readyCount, long length) {
        checkInterruption();
        updateProgress(new ArrayContext.Event(elementType, readyCount, length));
    }

    public int currentThreadIndex() {
        return 0;
    }

    public int numberOfThreads() {
        return 1;
    }

    public Object customData() {
        return null;
    }

    static class Default extends AbstractArrayContext implements ArrayContext {
        @Override
        public ArrayContext noProgressVersion() {
            return this;
        }

        @Override
        public ArrayContext singleThreadVersion() {
            return DEFAULT_SINGLE_THREAD;
        }

        @Override
        public MemoryModel getMemoryModel() {
            return SimpleMemoryModel.getInstance();
        }

        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return DefaultThreadPoolFactory.getDefaultThreadPoolFactory();
        }

        @Override
        public void checkInterruption() throws RuntimeException {
        }

        @Override
        public void updateProgress(Event event) {
        }

        @Override
        public String toString() {
            return "default array context";
        }
    }

    static class DefaultSingleThread extends Default implements ArrayContext {
        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return DefaultThreadPoolFactory.getDefaultThreadPoolFactory(1);
        }

        @Override
        public String toString() {
            return "default single-thread array context";
        }
    }

    private class Subtask extends AbstractArrayContext implements ArrayContext {
        private final double fromPart;
        private final double toPart;

        private Subtask(double fromPart, double toPart) {
            this.fromPart = fromPart;
            this.toPart = toPart;
        }

        @Override
        public MemoryModel getMemoryModel() {
            return AbstractArrayContext.this.getMemoryModel();
        }

        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return AbstractArrayContext.this.getThreadPoolFactory();
        }

        @Override
        public void checkInterruption() throws RuntimeException {
            AbstractArrayContext.this.checkInterruption();
        }

        @Override
        public void updateProgress(final ArrayContext.Event event) {
            AbstractArrayContext.this.updateProgress(new ArrayContext.Event(event.elementType(),
                event.readyCountPerTask(), event.lengthPerTask())
            {
                public double readyPart() {
                    return fromPart + event.readyPart() * (toPart - fromPart);
                }
            });
        }

        @Override
        public int currentThreadIndex() {
            return AbstractArrayContext.this.currentThreadIndex();
        }

        @Override
        public int numberOfThreads() {
            return AbstractArrayContext.this.numberOfThreads();
        }

        @Override
        public Object customData() {
            return AbstractArrayContext.this.customData();
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "sub-task context %.3f..%.3f of %s",
                fromPart, toPart, AbstractArrayContext.this);
        }
    }

    private class NoProgressVersion extends AbstractArrayContext implements ArrayContext {
        @Override
        public ArrayContext noProgressVersion() {
            return this;
        }

        @Override
        public MemoryModel getMemoryModel() {
            return AbstractArrayContext.this.getMemoryModel();
        }

        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return AbstractArrayContext.this.getThreadPoolFactory().singleThreadVersion();
        }

        @Override
        public void checkInterruption() throws RuntimeException {
            AbstractArrayContext.this.checkInterruption();
        }

        @Override
        public void updateProgress(ArrayContext.Event event) {
        }

        @Override
        public int currentThreadIndex() {
            return AbstractArrayContext.this.currentThreadIndex();
        }

        @Override
        public int numberOfThreads() {
            return AbstractArrayContext.this.numberOfThreads();
        }

        @Override
        public Object customData() {
            return AbstractArrayContext.this.customData();
        }

        @Override
        public String toString() {
            return "no-progress version of " + AbstractArrayContext.this;
        }
    }

    private class SingleThreadVersion extends AbstractArrayContext implements ArrayContext {
        @Override
        public ArrayContext singleThreadVersion() {
            return this;
        }

        @Override
        public MemoryModel getMemoryModel() {
            return AbstractArrayContext.this.getMemoryModel();
        }

        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return AbstractArrayContext.this.getThreadPoolFactory().singleThreadVersion();
        }

        @Override
        public void checkInterruption() throws RuntimeException {
            AbstractArrayContext.this.checkInterruption();
        }

        @Override
        public void updateProgress(ArrayContext.Event event) {
            AbstractArrayContext.this.updateProgress(event);
        }

        @Override
        public int currentThreadIndex() {
            return AbstractArrayContext.this.currentThreadIndex();
        }

        @Override
        public int numberOfThreads() {
            return AbstractArrayContext.this.numberOfThreads();
        }

        @Override
        public Object customData() {
            return AbstractArrayContext.this.customData();
        }

        @Override
        public String toString() {
            return "single-thread version of " + AbstractArrayContext.this;
        }
    }

    private class MultithreadingVersion extends AbstractArrayContext implements ArrayContext {
        private final int currentThreadIndex;
        private final int numberOfThreads;

        private MultithreadingVersion(int currentThreadIndex, int numberOfThreads) {
            assert numberOfThreads > 0 && currentThreadIndex >= 0 && currentThreadIndex < numberOfThreads;
            this.currentThreadIndex = currentThreadIndex;
            this.numberOfThreads = numberOfThreads;
        }

        @Override
        public MemoryModel getMemoryModel() {
            return AbstractArrayContext.this.getMemoryModel();
        }

        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return AbstractArrayContext.this.getThreadPoolFactory();
        }

        @Override
        public void checkInterruption() throws RuntimeException {
            AbstractArrayContext.this.checkInterruption();
        }

        @Override
        public void updateProgress(ArrayContext.Event event) {
            AbstractArrayContext.this.updateProgress(event);
        }

        @Override
        public int currentThreadIndex() {
            return this.currentThreadIndex;
        }

        @Override
        public int numberOfThreads() {
            return this.numberOfThreads;
        }

        @Override
        public Object customData() {
            return AbstractArrayContext.this.customData();
        }
    }

    private class CustomDataVersion extends AbstractArrayContext implements ArrayContext {
        private Object customData;
        private CustomDataVersion(Object customData) {
            this.customData = customData;
        }

        @Override
        public MemoryModel getMemoryModel() {
            return AbstractArrayContext.this.getMemoryModel();
        }

        @Override
        public ThreadPoolFactory getThreadPoolFactory() {
            return AbstractArrayContext.this.getThreadPoolFactory();
        }

        @Override
        public void checkInterruption() throws RuntimeException {
            AbstractArrayContext.this.checkInterruption();
        }

        @Override
        public void updateProgress(ArrayContext.Event event) {
            AbstractArrayContext.this.updateProgress(event);
        }

        @Override
        public int currentThreadIndex() {
            return AbstractArrayContext.this.currentThreadIndex();
        }

        @Override
        public int numberOfThreads() {
            return AbstractArrayContext.this.numberOfThreads();
        }

        @Override
        public Object customData() {
            return this.customData;
        }
    }
}
