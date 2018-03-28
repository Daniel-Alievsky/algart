/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Arrays;
import net.algart.arrays.JArrays;
import net.algart.arrays.PArray;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.math.Range;

public class SpecialRangeOfTest {
    private static final double[] FILLERS = {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, 0.0};

    public static void main(String[] args) {
        for (double filler : FILLERS) {
            System.out.println("Testing " + filler + "...");

            float[] floats= new float[100];
            JArrays.fillFloatArray(floats, (float) filler);
            Range range = Arrays.rangeOf((PArray) SimpleMemoryModel.asUpdatableArray(floats));
            System.out.println("float[] rangeOf: " + range);

            double[] doubles = new double[100];
            JArrays.fillDoubleArray(doubles, filler);
            range = Arrays.rangeOf((PArray) SimpleMemoryModel.asUpdatableArray(doubles));
            System.out.println("double[] rangeOf: " + range);
        }
    }
}
