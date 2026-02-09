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

package net.algart.matrices.filters3x3.demo;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.io.MatrixIO;
import net.algart.matrices.filters3x3.AverageBySquare3x3;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AverageBySquare3x3Demo {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s " +
                            "source_image.jpg/png/bmp result_image.bmp [numberOfIterations]%n",
                    AverageBySquare3x3Demo.class.getName());
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final Path resultFile = Paths.get(args[1]);
        final int numberOfIterations = args.length < 3 ? 1 : Integer.parseInt(args[2]);
        System.out.printf("Loading image %s...%n", sourceFile.toAbsolutePath().normalize());
        List<Matrix<UpdatablePArray>> matrices = MatrixIO.readImage(sourceFile);
        if (matrices.size() > 3) {
            matrices = matrices.subList(0, 3);
            // - AWT cannot write images with > 3 channels in some formats
        }
        System.out.printf("Averaging %d times...%n", numberOfIterations);
        for (int i = 0; i < numberOfIterations; i++) {
            matrices = Matrices.apply(AverageBySquare3x3::apply, matrices);
        }
        MatrixIO.writeImage(resultFile, matrices);
        System.out.printf("Result image is saved in %s%n", resultFile);
    }
}
