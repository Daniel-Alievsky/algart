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

package net.algart.matrices.scanning.demo;

import net.algart.arrays.*;
import net.algart.io.MatrixIO;
import net.algart.math.functions.RectangularFunc;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.ConnectivityType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FillHolesDemo {
    public static void main(String[] args) throws IOException {
        int startArgIndex = 0;
        boolean onlyThreshold = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-onlyThreshold")) {
            onlyThreshold = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.printf("Usage: %s " +
                            "[-onlyThreshold] source_image.jpg/png/bmp result_image.bmp threshold [numberOfTests]%n",
                    FillHolesDemo.class.getName());
            System.out.println("  threshold is a value in 0..1 range)");
            return;
        }
        final Path sourceFile = Paths.get(args[startArgIndex]);
        final Path resultFile = Paths.get(args[startArgIndex + 1]);
        final double threshold = Double.parseDouble(args[startArgIndex + 2]);
        final int numberOfTests = startArgIndex + 3 >= args.length ? 1 : Integer.parseInt(args[startArgIndex + 3]);
        System.out.printf("Loading image %s...%n", sourceFile.toAbsolutePath().normalize());
        List<Matrix<UpdatablePArray>> matrices = MatrixIO.readImage(sourceFile);
        Matrix<? extends PArray> intensity = ColorMatrices.toRGBIntensity(matrices);
        Matrix<UpdatableBitArray> binary = Matrix.newBitMatrix(intensity.dimensions());
        Matrix<UpdatableBitArray> result = Matrix.newBitMatrix(intensity.dimensions());
        Matrices.applyFunc(
                RectangularFunc.getInstance(0, threshold * intensity.maxPossibleValue(), 0, 1),
                binary, intensity);
        Matrices.copy(result, binary);
        if (!onlyThreshold) {
            System.out.printf("Scanning image %s...%n%n", binary);
            for (int test = 1; test <= numberOfTests; test++) {
                System.out.printf("Test #%d...%n", test);
                long t1 = System.nanoTime();
                Boundary2DScanner.fillHoles(result, binary, ConnectivityType.STRAIGHT_AND_DIAGONAL);
                long t2 = System.nanoTime();
                System.out.printf("Boundary scanned and filtered in %.3f ms%n%n", (t2 - t1) * 1e-6);
            }
        }
        MatrixIO.writeImage(resultFile, List.of(result));
        System.out.printf("Result binary image (with filled holes) is saved in %s%n", resultFile);
    }
}
