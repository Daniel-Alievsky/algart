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

import net.algart.arrays.*;
import net.algart.io.MatrixIO;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class InterleaveSeparateMatricesTest {
    private static final boolean MULTIHREADING = false;
    private static final MemoryModel MEMORY_MODEL = SimpleMemoryModel.getInstance();

    private static void checkIdentity(Matrix<?> a, Matrix<?> b, String what) {
        if (!a.dimEquals(b)) {
            throw new AssertionError(what + " dimensions mismatch!");
        }
        if (!a.equals(b)) {
            throw new AssertionError(what + " mismatch!");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s some_image.png matrix_folder%n", InterleaveSeparateMatricesTest.class);
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final Path matrixFolder = Paths.get(args[1]);
        final ArrayContext context = ArrayContext.getSimpleContext(MEMORY_MODEL, MULTIHREADING);
        List<? extends Matrix<? extends PArray>> source = MatrixIO.readImage(sourceFile);
        List<Matrix<? extends PNumberArray>> matrices = Matrices.several(
                PNumberArray.class,
                source.toArray(new Matrix<?>[0]));
        // - matrices, loaded by MatrixIO, are not binary
        System.out.printf("List: %s%n", matrices);
        Matrix<PArray> merged = Matrices.mergeLayers(matrices);
        System.out.printf("Merged: %s%n", merged);
        MatrixIO.writeImageFolder(matrixFolder, List.of(merged));
        List<Matrix<PArray>> unpacked = Matrices.asLayers(merged);
        for (int k = 0; k < unpacked.size(); k++) {
            checkIdentity(unpacked.get(k), matrices.get(k), "Channel #" + k);
        }
        for (int test = 1; test <= 10; test++) {
            System.out.printf("%nTest #%d%n", test);
            long t1 = System.nanoTime();
            Matrix<PNumberArray> interleaved = Matrices.interleave(context, matrices);
            long t2 = System.nanoTime();
            List<Matrix<PArray>> separated = Matrices.separate(context, interleaved);
            long t3 = System.nanoTime();
            for (int k = 0; k < unpacked.size(); k++) {
                checkIdentity(separated.get(k), matrices.get(k), "Channel #" + k);
            }
            System.out.printf("interleave: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOf(interleaved) / 1048576.0 / ((t2 - t1) * 1e-9));
            System.out.printf("separate: %.3f ms, %.3f MB/sec%n",
                    (t3 - t2) * 1e-6, Matrices.sizeOf(interleaved) / 1048576.0 / ((t3 - t2) * 1e-9));
            List<Matrix<UpdatablePArray>> separatedResult = separated.stream().map(
                    m -> MEMORY_MODEL.newMatrix(UpdatablePArray.class, m)).toList();
            Matrix<UpdatablePArray> interleavedResult = MEMORY_MODEL.newMatrix(UpdatablePArray.class, interleaved);
            t1 = System.nanoTime();
            Matrices.separate(context, separatedResult, interleaved);
            t2 = System.nanoTime();
            Matrices.interleave(context, interleavedResult, separatedResult);
            t3 = System.nanoTime();
            for (int k = 0; k < unpacked.size(); k++) {
                checkIdentity(separatedResult.get(k), matrices.get(k), "Channel #" + k);
            }
            checkIdentity(interleavedResult, interleaved, "Interleaved");
            System.out.printf("interleave in-place: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOf(interleaved) / 1048576.0 / ((t2 - t1) * 1e-9));
            System.out.printf("separate in-place: %.3f ms, %.3f MB/sec%n",
                    (t3 - t2) * 1e-6, Matrices.sizeOf(interleaved) / 1048576.0 / ((t3 - t2) * 1e-9));

            final Matrix<UpdatablePArray> separatedSingle = MEMORY_MODEL.newMatrix(UpdatablePArray.class,
                    interleaved.elementType(),
                    interleaved.dim(1), interleaved.dim(2), interleaved.dim(0));
            t1 = System.nanoTime();
            Matrices.separate(context, Matrices.asLayers(separatedSingle), interleaved);
            t2 = System.nanoTime();
            Matrices.interleave(context, interleavedResult, Matrices.asLayers(separatedSingle));
            t3 = System.nanoTime();
            checkIdentity(separatedSingle, merged, "Separated-in-single");
            checkIdentity(interleavedResult, interleaved, "Interleaved-in-single");
            System.out.printf("interleave in single matrix: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOf(interleaved) / 1048576.0 / ((t2 - t1) * 1e-9));
            System.out.printf("separate in single matrix: %.3f ms, %.3f MB/sec%n",
                    (t3 - t2) * 1e-6, Matrices.sizeOf(interleaved) / 1048576.0 / ((t3 - t2) * 1e-9));
        }
    }
}
