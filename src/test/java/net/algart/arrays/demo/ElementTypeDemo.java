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

import net.algart.arrays.*;
import net.algart.math.functions.ConstantFunc;

import java.util.Locale;

/**
 * <p>Shows the minimal and maximal possible array elements for all element types.</p>
 *
 * @author Daniel Alievsky
 */
public class ElementTypeDemo {
    public static void main(String[] args) {
        Class<?>[] types = {boolean.class, char.class, byte.class, short.class,
                int.class, long.class, float.class, double.class,
                String.class
        };
        for (Class<?> t : types) {
            Class<? extends Array> type = Arrays.type(Array.class, t);
            PArray[] arrays = new PArray[5];
            double min = Arrays.minPossibleValue(type, t.isPrimitive() ? Double.NEGATIVE_INFINITY : Double.NaN);
            double max = Arrays.maxPossibleValue(type, t.isPrimitive() ? Double.POSITIVE_INFINITY : Double.NaN);
            if (t.isPrimitive() != Arrays.isPrimitiveElementType(t)) {
                throw new AssertionError();
            }
            final long bitsPerElement =Arrays.bitsPerElement(t);
            final boolean floatingPoint = Arrays.isFloatingPointElementType(t);
            System.out.printf(Locale.US,
                    "%-16s - minimal possible element is %.1f, maximal possible element is %.1f%n" +
                            "    primitive: %s, numbers: %s, floating: %s, unsigned: %s, bits: %d, sizeOf: %s%n",
                    t.getCanonicalName(),
                    min,
                    max,
                    Arrays.isPrimitiveElementType(t),
                    Arrays.isNumberElementType(t),
                    floatingPoint,
                    Arrays.isUnsignedElementType(t),
                    bitsPerElement,
                    Arrays.sizeOf(t));
            if (t.isPrimitive()) {
                if (t != char.class && Arrays.primitiveType(bitsPerElement, floatingPoint) != t) {
                    throw new AssertionError("Invalid Arrays.primitiveType() for " + t);
                }
                arrays[0] = (PArray) SimpleMemoryModel.getInstance().newEmptyArray(t);
                arrays[1] = (PArray) BufferMemoryModel.getInstance().newEmptyArray(t);
                arrays[2] = (PArray) LargeMemoryModel.getInstance().newEmptyArray(t);
                arrays[3] = Arrays.nPCopies(1, t, 1);
                assert arrays[3] != null;
                arrays[4] = Arrays.asIndexFuncArray(ConstantFunc.getInstance(0), Arrays.type(PArray.class, t), 1);
                for (PArray a : arrays) {
                    if (a.minPossibleValue(Double.NEGATIVE_INFINITY) != min)
                        throw new AssertionError("Illegal minValue(double) in " + a);
                    if (a.maxPossibleValue(Double.POSITIVE_INFINITY) != max)
                        throw new AssertionError("Illegal maxValue(double) in " + a);
                    if (a instanceof PFixedArray) {
                        if (((PFixedArray) a).minPossibleValue() != min)
                            throw new AssertionError("Illegal minValue() in " + a);
                        if (((PFixedArray) a).maxPossibleValue() != max)
                            throw new AssertionError("Illegal maxValue() in " + a);
                    }
                    final Matrix<PArray> m = Matrices.matrix(a.asImmutable(), a.length());
                    if (!m.isPrimitive()) {
                        throw new AssertionError("Illegal isPrimitive() in " + m);
                    }
                    if (m.bitsPerElement() != bitsPerElement) {
                        throw new AssertionError("Illegal bitsPerElement() in " + m);
                    }
                    if (m.maxPossibleValue() != a.maxPossibleValue(1.0)) {
                        throw new AssertionError("Illegal maxValue() in " + m);
                    }
                    if (m.maxPossibleValue(Double.POSITIVE_INFINITY) != max) {
                        throw new AssertionError("Illegal maxValue() in " + m);
                    }
                }
            }
        }
        if (Arrays.primitiveType(1, false) != boolean.class) throw new AssertionError();
        if (Arrays.primitiveType(32, true) != float.class) throw new AssertionError();
    }
}
