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

import net.algart.arrays.*;
import net.algart.external.MatrixIO;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JoinSliceMatricesTest {
    private static ArrayContext CONTEXT = ArrayContext.DEFAULT_SINGLE_THREAD;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s some_image.png matrix_folder%n", JoinSliceMatricesTest.class);
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final Path matrixFolder = Paths.get(args[1]);
        List<Matrix<? extends PArray>> source = MatrixIO.readImage(sourceFile);
        List<Matrix<? extends PNumberArray>> matrices = Matrices.several(
                PNumberArray.class, source.toArray(new Matrix<?>[0]));
        // - matrices, loaded by MatrixIO, are not binary
        System.out.printf("List: %s%n", matrices);
        Matrix<PArray> merged = Matrices.mergeLayers(Arrays.SMM, matrices);
        System.out.printf("Joined: %s%n", merged);
        MatrixIO.writeAlgARTImage(matrixFolder, List.of(merged));
        List<Matrix<PArray>> unpacked = Matrices.asLayers(merged);
        for (int k = 0; k < unpacked.size(); k++) {
            if (!unpacked.get(k).equals(matrices.get(k))) {
                throw new AssertionError("Channels #" + k + " mismatch!");
            }
        }
        Matrix<PNumberArray> interleave = Matrices.interleave(CONTEXT, matrices);
        List<Matrix<PArray>> separate = Matrices.separate(CONTEXT, interleave);
        for (int k = 0; k < unpacked.size(); k++) {
            if (!separate.get(k).equals(matrices.get(k))) {
                throw new AssertionError("Channels #" + k + " mismatch!");
            }
        }
    }
}
