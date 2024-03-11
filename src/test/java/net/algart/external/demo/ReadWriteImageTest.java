/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.external.demo;

import net.algart.arrays.*;
import net.algart.external.MatrixIO;
import net.algart.external.awt.BufferedImageToMatrix;
import net.algart.external.awt.MatrixToBufferedImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReadWriteImageTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: %s source_image.jpg/png/bmp copy_1.jpg/png/bmp copy_2.jpg/png/bmp%n",
                    ReadWriteImageTest.class);
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final Path targetFile1 = Paths.get(args[1]);
        final Path targetFile2 = Paths.get(args[2]);

        for (int test = 1; test <= 10; test++) {
            System.out.printf("%nTest #%d%n", test);
            var toMatrix = new BufferedImageToMatrix.ToInterleavedRGB();
            var toBufferedImage = new MatrixToBufferedImage.InterleavedRGBToInterleaved();
            toMatrix.setEnableAlpha(true);
            toBufferedImage.setAlwaysAddAlpha(false);

            final BufferedImage bi = MatrixIO.readBufferedImage(sourceFile);
            long t1 = System.nanoTime();
            final List<Matrix<UpdatablePArray>> image = MatrixIO.readImage(sourceFile);
            long t2 = System.nanoTime();
            MatrixIO.writeImage(targetFile1, image);
            long t3 = System.nanoTime();
            Matrix<UpdatablePArray> matrix1 = toMatrix.toMatrix(bi);
            long t4 = System.nanoTime();
            Matrix<UpdatablePArray> matrix2 = toMatrix.setReadPixelValuesViaGraphics2D(true).toMatrix(bi);
            long t5 = System.nanoTime();
            final BufferedImage bufferedImage = toBufferedImage.toBufferedImage(matrix1);
            long t6 = System.nanoTime();
            MatrixIO.writeBufferedImage(targetFile2, bufferedImage);
            System.out.println("Matrix: " + matrix1);
            if (!matrix1.equals(matrix2)) {
                System.out.println("Different behaviour of BufferedImageToMatrix while using Graphics2D!");
                Path altFile = Paths.get(targetFile2 + ".alt.png");
                System.out.println("        " + matrix2);
                MatrixIO.writeBufferedImage(altFile, toBufferedImage.toBufferedImage(matrix2));
                System.out.println("        saved in " + altFile);
            }

            System.out.printf("readImage: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t2 - t1) * 1e-9));
            System.out.printf("writeImage: %.3f ms, %.3f MB/sec%n",
                    (t3 - t2) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t3 - t2) * 1e-9));
            System.out.printf("BufferedImageToMatrix: %.3f ms, %.3f MB/sec%n",
                    (t4 - t3) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t4 - t3) * 1e-9));
            System.out.printf("BufferedImageToMatrix, Graphics2D: %.3f ms, %.3f MB/sec%n",
                    (t5 - t4) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t5 - t4) * 1e-9));
            System.out.printf("MatrixToBufferedImage: %.3f ms, %.3f MB/sec%n",
                    (t6 - t5) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t6 - t5) * 1e-9));
        }
    }
}
