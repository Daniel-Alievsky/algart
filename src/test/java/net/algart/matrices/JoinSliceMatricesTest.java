/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.external.MatrixIO;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JoinSliceMatricesTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s some_image.png matrix_folder%n", JoinSliceMatricesTest.class);
            return;
        }
        final File sourceFile = new File(args[0]);
        final File matrixFolder = new File(args[1]);
        List<Matrix<? extends PArray>> matrices = MatrixIO.readImage(sourceFile);
        System.out.printf("List: %s%n", matrices);
        Matrix<PArray> joined = Matrices.mergeAlongLastDimension(PArray.class, matrices);
        System.out.printf("Joined: %s%n", joined);
        MatrixIO.writeAlgARTImage(matrixFolder, List.of(joined));
        List<Matrix<PArray>> unpacked = Matrices.splitAlongLastDimension(joined);
        for (int k = 0; k < unpacked.size(); k++) {
            if (!unpacked.get(k).equals(matrices.get(k))) {
                throw new AssertionError("Channels #" + k + " mismatch!");
            }
        }
    }
}
