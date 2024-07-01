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

package net.algart.io.demo;

import net.algart.arrays.ColorMatrices;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.io.MatrixIO;
import net.algart.io.awt.BufferedImageToMatrix;
import net.algart.io.awt.MatrixToBufferedImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReadWriteImageTest {
    public static void main(String[] args) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        int startArgIndex = 0;
        boolean monochrome = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-mono")) {
            monochrome = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.printf("Usage: %s [-mono] source_image.jpg/png/bmp copy_1.jpg/png/bmp copy_2.jpg/png/bmp" +
                            "[RGBToPacked|BGRToPacked|RGBToInterleaved|BGRToInterleaved]%n",
                    ReadWriteImageTest.class.getName());
            return;
        }
        final Path sourceFile = Paths.get(args[startArgIndex]);
        final Path targetFile1 = Paths.get(args[startArgIndex + 1]);
        final Path targetFile2 = Paths.get(args[startArgIndex + 2]);
        Class<? extends MatrixToBufferedImage> matrixToBufferedImageClass =
                MatrixToBufferedImage.InterleavedRGBToPacked.class;
        if (args.length > startArgIndex + 3) {
            matrixToBufferedImageClass = switch (args[startArgIndex + 3]) {
                case "RGBToPacked" -> MatrixToBufferedImage.InterleavedRGBToPacked.class;
                case "BGRToPacked" -> MatrixToBufferedImage.InterleavedBGRToPacked.class;
                case "RGBToInterleaved" -> MatrixToBufferedImage.InterleavedRGBToInterleaved.class;
                case "BGRToInterleaved" -> MatrixToBufferedImage.InterleavedBGRToInterleaved.class;
                case "RGBToBanded" -> MatrixToBufferedImage.InterleavedRGBToBanded.class;
                case "BGRToBanded" -> MatrixToBufferedImage.InterleavedBGRToBanded.class;
                default -> throw new IllegalArgumentException("Unknown mode: " + args[startArgIndex + 3]);
            };
        }

        for (int test = 1; test <= 10; test++) {
            System.out.printf("%nTest #%d%n", test);
            var toMatrix = new BufferedImageToMatrix.ToInterleavedRGB();
            var toBufferedImage = matrixToBufferedImageClass.getConstructor().newInstance();
            toMatrix.setEnableAlpha(true);

            System.out.println("Reading " + sourceFile + "...");
            BufferedImage bi = MatrixIO.readBufferedImage(sourceFile);
            if (monochrome) {
                System.out.println("Conversion to monochrome...");
                var separate = Matrices.separate(toMatrix.toMatrix(bi));
                var intensity = ColorMatrices.asRGBIntensity(separate).clone();
                bi = new MatrixToBufferedImage.InterleavedRGBToPacked().toBufferedImage(intensity);
            }

            long t1 = System.nanoTime();
            final List<Matrix<UpdatablePArray>> image = Matrices.separate(toMatrix.toMatrix(bi));
            long t2 = System.nanoTime();
            final BufferedImage bi1 = toBufferedImage.toBufferedImage(Matrices.interleave(image));
            long t3 = System.nanoTime();

            AWT2MatrixTest.drawTextOnImage(bi1);
            System.out.println("Writing " + targetFile1 + "...");
            MatrixIO.writeBufferedImage(targetFile1, bi1);
            long t4 = System.nanoTime();
            Matrix<UpdatablePArray> matrix1 = toMatrix.toMatrix(bi);
            long t5 = System.nanoTime();
            Matrix<UpdatablePArray> matrix2 = toMatrix.setReadPixelValuesViaGraphics2D(true).toMatrix(bi);
            long t6 = System.nanoTime();
            final BufferedImage bi2 = toBufferedImage.toBufferedImage(matrix1);
            long t7 = System.nanoTime();

            AWT2MatrixTest.drawTextOnImage(bi2);
            System.out.println("Writing " + targetFile2 + "...");
            MatrixIO.writeBufferedImage(targetFile2, bi2);

            System.out.println("Source: " + WriteDemoImageTest.toString(bi));
            System.out.println("Converted 1: " + WriteDemoImageTest.toString(bi1));
            System.out.println("Converted 2: " + WriteDemoImageTest.toString(bi2));
            System.out.println("Matrix: " + matrix1);
            if (!matrix1.equals(matrix2)) {
                System.out.println("Different behaviour of BufferedImageToMatrix while using Graphics2D!");
                Path altFile = Paths.get(targetFile2 + ".alt.png");
                System.out.println("        " + matrix2);
                MatrixIO.writeBufferedImage(altFile, toBufferedImage.toBufferedImage(matrix2));
                System.out.println("        saved in " + altFile);
            }

            System.out.printf("BufferedImageToMatrix + separate: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t2 - t1) * 1e-9));
            System.out.printf("interleave + MatrixToBufferedImage: %.3f ms, %.3f MB/sec%n",
                    (t3 - t2) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t3 - t2) * 1e-9));
            System.out.printf("BufferedImageToMatrix: %.3f ms, %.3f MB/sec%n",
                    (t5 - t4) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t5 - t4) * 1e-9));
            System.out.printf("BufferedImageToMatrix, Graphics2D: %.3f ms, %.3f MB/sec%n",
                    (t6 - t5) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t6 - t5) * 1e-9));
            System.out.printf("MatrixToBufferedImage: %.3f ms, %.3f MB/sec%n",
                    (t7 - t6) * 1e-6, Matrices.sizeOf(matrix1) / 1048576.0 / ((t7 - t6) * 1e-9));
        }
    }
}
