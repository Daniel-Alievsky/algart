/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableBitArray;
import net.algart.arrays.UpdatablePArray;

public class MatrixNewDemo {
    public static void main(String[] args) {
        Matrix<UpdatablePArray> m1 = Matrix.newMatrix(Arrays.BMM, short.class, 100, 100);
        System.out.println(m1);
        // Matrix.newMatrix(String.class, 100, 100);
        Matrix<UpdatablePArray> m2 = Matrix.newMatrix(float.class, 100);
        System.out.println(m2);
        Matrix<UpdatableBitArray> m3 = Matrix.newBitMatrix(100, 100, 100);
        System.out.println(m3);
        for (int k = 0; k < 10; k++) {
            if (m3.dim(k) != m3.dim32(k)) {
                throw new AssertionError();
            }
        }
        if (m3.dimX() != m3.dimX32() || m3.dimY() != m3.dimY32() || m3.dimZ() != m3.dimZ32()) {
            throw new AssertionError();
        }
        Matrix<UpdatableBitArray> m4 = Matrix.newBitMatrix(4000_000_000L, 2);
        // The following calls lead to TooLargeArrayException
//        if (m4.dimX() != m4.dimX32() || m4.dimY() != m4.dimY32() || m4.dimZ() != m4.dimZ32()) {
//            throw new AssertionError();
//        }
        if (m4.isEmpty()) throw new AssertionError();
        System.out.println(m4);

        Matrix<? extends UpdatablePArray> m5 = Matrix.newCharMatrix(0, 100);
        if (!m5.isEmpty()) throw new AssertionError();
        System.out.print(m5);
    }
}
