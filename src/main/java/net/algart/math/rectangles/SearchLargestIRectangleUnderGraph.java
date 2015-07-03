/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

class SearchLargestIRectangleUnderGraph {
    final long[] x;
    final long[] y;
    long currentFromY;

    double maxRectangleArea = Double.NEGATIVE_INFINITY;
    long maxRectangleFromX = Long.MAX_VALUE;
    long maxRectangleToX = Long.MIN_VALUE;
    long maxRectangleFromY = Long.MAX_VALUE;
    long maxRectangleToY = Long.MIN_VALUE;
    boolean maxRectangleCorrected = false;

    SearchLargestIRectangleUnderGraph(long[] x, long[] y, long currentFromY) {
        this.x = x;
        this.y = y;
        assert x.length == y.length + 1 : "number of function values must be 1 less than number of x values";
        this.currentFromY = currentFromY;
    }

    void correctMaximalRectangle(int fromIndex, int toIndex) {
        assert fromIndex >= 0 && toIndex <= x.length && fromIndex <= toIndex;
        if (fromIndex == toIndex) {
            return;
        }
        for (int k = fromIndex; k < toIndex; k++) {
            if (y[k] < currentFromY) {
                throw new AssertionError("All y must be >= currentFromY");
            }
        }
        // Unfortunately Java still does not support optimization of tail recursion (1.8),
        // so we should implement the stack manually
        int[] stack = new int[128];
        // - more than enough
        int stackTop = 2;
        stack[0] = fromIndex;
        stack[1] = toIndex;
        for (;;) {
            assert stackTop % 2 == 0;
            long maxY = currentFromY;
            for (int k = fromIndex; k < toIndex; k++) {
                if (y[k] > maxY) {
                    maxY = y[k];
                }
            }
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
                    toIndex = minIndex;
                } else {
                    stack[stackTop++] = minIndex + 1;
                    stack[stackTop++] = toIndex;
                    fromIndex = minIndex + 1;
                }
            }
        }
    }

    private void correctMaxRectangle(long fromX, long fromY, long toX, long toY) {
        final double area = area(fromX, fromY, toX, toY);
        if (area > maxRectangleArea) {
            maxRectangleFromX = fromX;
            maxRectangleFromY = fromY;
            maxRectangleToX = toX;
            maxRectangleToY = toY;
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
