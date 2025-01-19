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

package net.algart.matrices.filters3x3;

import net.algart.arrays.*;
import net.algart.matrices.Abstract2DProcessor;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.stream.IntStream;

public abstract class AbstractQuickFilter3x3 extends Abstract2DProcessor {
    final int rem1ForDimX;
    final int dimX;
    final int dimXm1;
    private final int dimXMul2;
    private final Object buffer3Lines;
    private final Object resultLine;
    private final int numberOfRanges;
    private final int[] splitters;

    private boolean multithreading = Arrays.SystemSettings.cpuCount() > 1;

    protected AbstractQuickFilter3x3(Class<?> elementType, long[] dimensions) {
        super(elementType, dimensions, Integer.MAX_VALUE / 3);
        this.dimX = dimX();
        this.dimXm1 = this.dimX - 1;
        this.rem1ForDimX = rem(1, dimX);
        this.dimXMul2 = 2 * dimX;
        this.buffer3Lines = Array.newInstance(elementType, 3 * dimX);
        this.resultLine = Array.newInstance(elementType, dimX);
        final int reducedDimY = (int) Math.min(dimY(), Integer.MAX_VALUE);
        // - never used if dimY is not 32-bit integer
        final int cpuCount = Arrays.SystemSettings.cpuCount();
        // - splitting into more than cpuCount ranges provides better performance
        this.numberOfRanges = cpuCount == 1 ? 1 : Math.min(reducedDimY, 4 * cpuCount);
        this.splitters = new int[numberOfRanges + 1];
        Arrays.splitToRanges(this.splitters, reducedDimY);
    }

    public boolean isMultithreading() {
        return multithreading;
    }

    public void setMultithreading(boolean multithreading) {
        this.multithreading = multithreading;
    }

    public int numberOfRanges() {
        return numberOfRanges;
    }

    public Matrix<? extends UpdatablePArray> filter(Matrix<? extends PArray> source) {
        final Matrix<? extends UpdatablePArray> result = Arrays.SMM.newMatrix(UpdatablePArray.class, source);
        filter(result, source);
        return result;
    }

    public void filter(Matrix<? extends UpdatablePArray> result, Matrix<? extends PArray> source) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(source, "Null source");
        checkCompatibility(result);
        checkCompatibility(source);
        if (source.isEmpty()) {
            // nothing to do;
            // now we can be sure that dimX > 0 and dimY > 0
            return;
        }
        Object sourceArray = null;
        int sourceOffset = -1;
        DirectAccessible da;
        if (source.array() instanceof DirectAccessible && (da = (DirectAccessible) source.array()).hasJavaArray()) {
            sourceArray = da.javaArray();
            sourceOffset = da.javaArrayOffset();
        }
        Object resultArray = null;
        int resultOffset = -1;
        if (result.array() instanceof DirectAccessible && (da = (DirectAccessible) result.array()).hasJavaArray()) {
            resultArray = da.javaArray();
            resultOffset = da.javaArrayOffset();
        }
        initialize();
        if (sourceArray == null || resultArray == null) {
            doFilter(result.array(), source.array());
        } else {
            doFilterForDirectAccessible(resultArray, resultOffset, sourceArray, sourceOffset);
        }
    }

    protected void initialize() {
        // nothing to do in default implementation
    }

    protected abstract void process3Lines(
            Object resultJavaArray,
            int resultLineOffset,
            Object sourceJavaArray,
            int firstLineOffset,
            int middleLineOffset,
            int lastLineOffset,
            int multithreadingRangeIndex);

    private void doFilter(UpdatablePArray result, PArray source) {
        final int dimX = dimX();
        final long dimY = dimY();
        source.getData(matrixSize() - dimX, buffer3Lines, 0, dimX);
        source.getData(0, buffer3Lines, dimX, dimX);
        final long matrixSizeMinusDimX = matrixSize() - dimX;
        for (long y = 0, lineOffset = 0; y < dimY; y++, lineOffset += dimX) {
            final long nextLineOffset = nextLineOffset(lineOffset);
            source.getData(nextLineOffset, buffer3Lines, dimXMul2, dimX);
            process3Lines(
                    resultLine, 0,
                    buffer3Lines, 0, dimX, dimXMul2,
                    0);
            result.setData(lineOffset, resultLine);
            if (lineOffset < matrixSizeMinusDimX) {
                System.arraycopy(buffer3Lines, dimX, buffer3Lines, 0, dimXMul2);
            }
        }
    }

    private void doFilterForDirectAccessible(
            Object resultArray,
            int resultOffset,
            Object sourceArray,
            int sourceOffset) {
        final int dimX = dimX();
        final long dimY = dimY();
        assert matrixSize() == (int) matrixSize();
        assert dimY == (int) dimY;
        if (isMultithreading()) {
//            if (needMemoryForMultithreading()) { // - DOESN'T OPTIMIZE
            IntStream.range(0, numberOfRanges).parallel().forEach(rangeIndex -> {
                final int from = splitters[rangeIndex];
                final int to = splitters[rangeIndex + 1];
                for (int y = from, lineOffset = y * dimX; y < to; y++, lineOffset += dimX) {
                    processLine(
                            resultArray, resultOffset,
                            sourceArray, sourceOffset,
                            lineOffset, rangeIndex);
                }
            });
//            } else {
//                IntStream.range(0, (int) dimY).parallel().forEach(y -> {
//                    processLine(
//                            resultArray, resultOffset,
//                            sourceArray, sourceOffset,
//                            y * dimX, -1);
//                });
//            }
        } else {
            for (int y = 0, lineOffset = 0; y < dimY; y++, lineOffset += dimX) {
                processLine(resultArray, resultOffset, sourceArray, sourceOffset, lineOffset, 0);
            }
        }
    }

    private void processLine(
            Object resultArray,
            int resultOffset,
            Object sourceArray,
            int sourceOffset,
            int lineOffset,
            int multithreadingRangeIndex) {
        final int previousLineOffset = (int) previousLineOffset(lineOffset);
        final int nextLineOffset = (int) nextLineOffset(lineOffset);
        process3Lines(
                resultArray,
                resultOffset + lineOffset,
                sourceArray,
                sourceOffset + previousLineOffset,
                sourceOffset + lineOffset,
                sourceOffset + nextLineOffset,
                multithreadingRangeIndex);
    }
}
