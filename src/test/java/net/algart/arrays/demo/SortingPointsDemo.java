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

package net.algart.arrays.demo;

import net.algart.arrays.ArraySorter;

/**
 * <p>Sorting points: an example from comments to {@link net.algart.arrays.ArraySorter} class</p>
 *
 * @author Daniel Alievsky
 */
public class SortingPointsDemo {
    public static void main(String[] args) {
        int n = 100;
        final float[] xy = new float[2 * n];
        final int[] indexes = new int[n];
        for (int k = 0; k < n; k++) {
            xy[2 * k] = (float) Math.random();     // x-coordinate of the point #k
            xy[2 * k + 1] = (float) Math.random(); // y-coordinate of the point #k
            indexes[k] = k;
        }
        System.out.println(n + " points before sorting:");
        for (int k = 0; k < n; k++) {
            System.out.println("x = " + xy[2 * k] + ", y = " + xy[2 * k + 1]);
        }
        final float[] xyClone = xy.clone();

        ArraySorter.getQuickSorter().sortIndexes(indexes, 0, n, (i, j) -> xy[2 * i] < xy[2 * j]);
        ArraySorter.getQuickSorter().sort(0, n,
                (i, j) -> xy[2 * i] < xy[2 * j],
                (i, j) -> {
                    float temp = xy[2 * i];
                    xy[2 * i] = xy[2 * j];
                    xy[2 * j] = temp;
                    temp = xy[2 * i + 1];
                    xy[2 * i + 1] = xy[2 * j + 1];
                    xy[2 * j + 1] = temp;
                });

        System.out.println(n + " points after sorting:");
        for (int k = 0; k < n; k++) {
            System.out.println("x = " + xy[2 * k] + ", y = " + xy[2 * k + 1]);
            if (xy[2 * k] != xyClone[2 * indexes[k]] || xy[2 * k + 1] != xyClone[2 * indexes[k] + 1]) {
                throw new AssertionError("Bug in sorting indexes for index " + k);
            }
        }
    }
}
