/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.rectangles;

import net.algart.math.IRectangularArea;

import java.util.Arrays;

class SearchIRectangleInHypograph {
    private final long[] x;
    private final long[] y;
    private long currentFromY;

    private double maxRectangleArea = Double.NEGATIVE_INFINITY;
    private long largestRectangleFromX = Long.MAX_VALUE;
    private long largestRectangleToX = Long.MIN_VALUE;
    private long largestRectangleFromY = Long.MAX_VALUE;
    private long largestRectangleToY = Long.MIN_VALUE;
    private boolean maxRectangleCorrected = false;

    public SearchIRectangleInHypograph(long[] x) {
        this.x = x;
        this.y = new long[x.length - 1];
        assert x.length == y.length + 1 : "number of function values must be 1 less than number of x values";
        this.currentFromY = Long.MAX_VALUE;
        Arrays.fill(y, Long.MIN_VALUE);
    }

    public void setY(int index, long value) {
        // In future version, here can be more optimal operations, for example, correction of the pyramid of minimums.
        this.y[index] = value;
    }

    public void resetAlreadyFoundRectangle() {
        this.maxRectangleArea = Double.NEGATIVE_INFINITY;
    }

    public boolean isMaxRectangleCorrected() {
        return maxRectangleCorrected;
    }

    public void resetMaxRectangleCorrected() {
        this.maxRectangleCorrected = false;
    }

    public void setCurrentFromY(long currentFromY) {
        this.currentFromY = currentFromY;
    }

    public IRectangularArea largestRectangle() {
        assert largestRectangleFromX < largestRectangleToX;
        assert largestRectangleFromY < largestRectangleToY;
        return IRectangularArea.valueOf(
            largestRectangleFromX, largestRectangleFromY,
            largestRectangleToX - 1, largestRectangleToY - 1);
    }

    public void correctMaximalRectangle(int fromIndex, int toIndex) {
        assert fromIndex >= 0 && toIndex <= y.length && fromIndex <= toIndex :
            "index out of ranges 0.." + y.length + ": " + fromIndex + ".." + toIndex;
        if (fromIndex == toIndex) {
            return;
        }
        // Unfortunately Java still does not support optimization of tail recursion (1.8),
        // so we should implement the stack manually
        int[] stack = new int[128];
        // - more than enough
        int stackTop = 2;
        stack[0] = fromIndex;
        stack[1] = toIndex;
        for (long iterationCount = 0; ; iterationCount++) {
            if (iterationCount > 16 * (long) y.length + 16) {
                // Really we can loop here ~ 2 * y.length times, including "empty" visits when fromIndex==toIndex
                throw new AssertionError("Infinite loop detected");
            }
            assert stackTop % 2 == 0;
            long maxY = currentFromY;
            for (int k = fromIndex; k < toIndex; k++) {
                if (y[k] > maxY) {
                    maxY = y[k];
                }
            }
            // If fromIndex == toIndex, then maxY == currentFromY
            if (maxY == currentFromY || area(x[fromIndex], currentFromY, x[toIndex], maxY) <= maxRectangleArea) {
                // we cannot find here a greater rectangle
                if (stackTop == 0) {
                    break;
                }
                stackTop -= 2;
                fromIndex = stack[stackTop];
                toIndex = stack[stackTop + 1];

            } else {
                assert fromIndex < toIndex;
                // - because maxY > currentFromY
                long minY = y[fromIndex];
                int minIndex = fromIndex;
                for (int k = fromIndex + 1; k < toIndex; k++) {
                    if (y[k] < minY) {
                        minY = y[k];
                        minIndex = k;
                    }
                }
                correctMaxRectangle(x[fromIndex], currentFromY, x[toIndex], minY);
                // Save in stack the greater subrange and go to processing the smaller subramge:
                if (minIndex - fromIndex >= toIndex - (minIndex + 1)) {
                    stack[stackTop++] = fromIndex;
                    stack[stackTop++] = minIndex;
                    fromIndex = minIndex + 1;
                } else {
                    stack[stackTop++] = minIndex + 1;
                    stack[stackTop++] = toIndex;
                    toIndex = minIndex;
                }
            }
        }
    }


    private void correctMaxRectangle(long fromX, long fromY, long toX, long toY) {
        final double area = area(fromX, fromY, toX, toY);
        if (area > maxRectangleArea) {
            largestRectangleFromX = fromX;
            largestRectangleFromY = fromY;
            largestRectangleToX = toX;
            largestRectangleToY = toY;
            maxRectangleArea = area;
            maxRectangleCorrected = true;
        }
    }

    static double area(long fromX, long fromY, long toX, long toY) {
        assert fromX <= toX;
        assert fromY <= toY;
        return (double) (toX - fromX) * (double) (toY - fromY);
    }
}
