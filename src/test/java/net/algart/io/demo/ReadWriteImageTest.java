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
import net.algart.arrays.ColorMatrices.ChannelOrder;
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

        assert new BufferedImageToMatrix.ToInterleavedRGB().channelOrder() == ChannelOrder.RGB;
        assert new BufferedImageToMatrix.ToInterleavedBGR().channelOrder() == ChannelOrder.BGR;
        assert new MatrixToBufferedImage.InterleavedRGBToPacked().channelOrder() == ChannelOrder.RGB;
        assert new MatrixToBufferedImage.InterleavedBGRToPacked().channelOrder() == ChannelOrder.BGR;
        assert new MatrixToBufferedImage.InterleavedRGBToInterleaved().channelOrder() == ChannelOrder.RGB;
        assert new MatrixToBufferedImage.InterleavedBGRToInterleaved().channelOrder() == ChannelOrder.BGR;
        assert new MatrixToBufferedImage.InterleavedRGBToBanded().channelOrder() == ChannelOrder.RGB;
        assert new MatrixToBufferedImage.InterleavedBGRToBanded().channelOrder() == ChannelOrder.BGR;

        for (int test = 1; test <= 10; test++) {
            System.out.printf("%nTest #%d%n", test);
            var toBufferedImage = matrixToBufferedImageClass.getConstructor().newInstance();
            System.out.printf("Channel order: %s%n", toBufferedImage.channelOrder());
            var toMatrix = toBufferedImage.channelOrder() == ChannelOrder.RGB ?
                    new BufferedImageToMatrix.ToInterleavedRGB() :
                    new BufferedImageToMatrix.ToInterleavedBGR();
            toMatrix.setEnableAlpha(true);

            System.out.println("Reading " + sourceFile + "...");
            long t1 = System.nanoTime();
            BufferedImage bi = MatrixIO.readBufferedImage(sourceFile);
            long t2 = System.nanoTime();
            System.out.printf("readBufferedImage: %.3f ms%n", (t2 - t1) * 1e-6);
            if (monochrome) {
                t1 = System.nanoTime();
                var separate = Matrices.separate(toMatrix.toMatrix(bi));
                var intensity = ColorMatrices.asRGBIntensity(separate).clone();
                bi = new MatrixToBufferedImage.InterleavedRGBToPacked().toBufferedImage(intensity);
                t2 = System.nanoTime();
                System.out.printf("Converted to monochrome in %.3f ms%n", (t2 - t1) * 1e-6);
            }
            System.out.println("Source image: " + AWT2MatrixTest.toString(bi));

            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix1 = toMatrix.toMatrix(bi);
            t2 = System.nanoTime();
            System.out.printf("BufferedImageToMatrix: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix1) / ((t2 - t1) * 1e-9));

            t1 = System.nanoTime();
            final List<Matrix<UpdatablePArray>> image = Matrices.separate(matrix1);
            t2 = System.nanoTime();
            System.out.printf("separate: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix1) / ((t2 - t1) * 1e-9));

            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> interleave = Matrices.interleave(image);
            t2 = System.nanoTime();
            System.out.printf("interleave: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix1) / ((t2 - t1) * 1e-9));
            if (!matrix1.equals(interleave)) {
                throw new AssertionError("separate/interleave mismatch");
            }
            System.out.println("Matrix 1: " + matrix1);

            t1 = System.nanoTime();
            final BufferedImage bi1 = toBufferedImage.toBufferedImage(interleave);
            t2 = System.nanoTime();
            System.out.printf("MatrixToBufferedImage: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(interleave) / ((t2 - t1) * 1e-9));
            System.out.println("Converted 1: " + AWT2MatrixTest.toString(bi1));

            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix2 = toMatrix.toMatrix(bi1);
            t2 = System.nanoTime();
            System.out.printf("BufferedImageToMatrix: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix2) / ((t2 - t1) * 1e-9));
            if (!matrix1.equals(matrix2)) {
                System.out.println("        Matrix 2: " + matrix2);
                System.out.println("        Matrix has been changed after conversion to BufferedImage and back!");
            }

            t1 = System.nanoTime();
            AWT2MatrixTest.drawTextOnImage(bi1);
            t2 = System.nanoTime();
            System.out.printf("drawing text: %.3f ms%n", (t2 - t1) * 1e-6);

            System.out.println("Writing " + targetFile1 + "...");
            t1 = System.nanoTime();
            MatrixIO.writeBufferedImage(targetFile1, bi1);
            t2 = System.nanoTime();
            System.out.printf("writeBufferedImage: %.3f ms, %.3f MB/sec%n",
                (t2 - t1) * 1e-6, Matrices.sizeOfMB(interleave) / ((t2 - t1) * 1e-9));

            toMatrix.setReadPixelValuesViaColorModel(true);
            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix3 = toMatrix.toMatrix(bi);
            t2 = System.nanoTime();
            System.out.printf("BufferedImageToMatrix, Graphics2D: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix3) / ((t2 - t1) * 1e-9));

            t1 = System.nanoTime();
            final BufferedImage bi2 = toBufferedImage.toBufferedImage(matrix3);
            t2 = System.nanoTime();
            System.out.printf("MatrixToBufferedImage: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix3) / ((t2 - t1) * 1e-9));
            System.out.println("Converted 2: " + AWT2MatrixTest.toString(bi2));

            AWT2MatrixTest.drawTextOnImage(bi2);
            System.out.println("Writing " + targetFile2 + "...");
            MatrixIO.writeBufferedImage(targetFile2, bi2);

            if (!matrix1.equals(matrix3)) {
                System.out.println("Different behaviour of BufferedImageToMatrix while using Graphics2D!");
                Path altFile = Paths.get(targetFile2 + ".alt.png");
                System.out.println("        " + matrix3);
                BufferedImage biAlt = toBufferedImage.toBufferedImage(matrix3);
                AWT2MatrixTest.drawTextOnImage(biAlt);
                MatrixIO.writeBufferedImage(altFile, biAlt);
                System.out.println("        saved in " + altFile);
            }
        }
    }
}
