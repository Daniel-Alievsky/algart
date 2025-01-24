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

package net.algart.io.demo;

import net.algart.arrays.*;
import net.algart.io.MatrixIO;
import net.algart.io.awt.ImageToMatrix;
import net.algart.io.awt.MatrixToImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReadWriteImageTest {
    public static void main(String[] args) throws IOException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        int startArgIndex = 0;
        boolean monochrome = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-mono")) {
            monochrome = true;
            startArgIndex++;
        }
        boolean reduce4ChannelsTo2 = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-reduce4ChannelsTo2")) {
            reduce4ChannelsTo2 = true;
            startArgIndex++;
        }
        boolean addAlpha = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-addAlpha")) {
            addAlpha = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.printf("Usage: %s [-mono] [-reduce4ChannelsTo2] source_image.jpg/png/bmp " +
                            "copy_1.jpg/png/bmp copy_2.jpg/png/bmp copy_3.jpg/png/bmp " +
                            "[ALL|RGBToPacked|BGRToPacked|RGBToInterleaved|BGRToInterleaved]%n",
                    ReadWriteImageTest.class.getName());
            return;
        }
        final Path sourceFile = Paths.get(args[startArgIndex]);
        final Path targetFile1 = Paths.get(args[startArgIndex + 1]);
        final Path targetFile2 = Paths.get(args[startArgIndex + 2]);
        final Path targetFile3 = Paths.get(args[startArgIndex + 3]);
        final String mode = args.length <= startArgIndex + 4 ? "ALL" : args[startArgIndex + 4];

        final Map<Object, Class<? extends MatrixToImage>> matrixToBufferedImageClassMap = new LinkedHashMap<>();
        matrixToBufferedImageClassMap.put("RGBToPacked", MatrixToImage.InterleavedRGBToPacked.class);
        matrixToBufferedImageClassMap.put("BGRToPacked", MatrixToImage.InterleavedBGRToPacked.class);
        matrixToBufferedImageClassMap.put("RGBToInterleaved", MatrixToImage.InterleavedRGBToInterleaved.class);
        matrixToBufferedImageClassMap.put("BGRToInterleaved", MatrixToImage.InterleavedBGRToInterleaved.class);
        matrixToBufferedImageClassMap.put("RGBToBanded", MatrixToImage.InterleavedRGBToBanded.class);
        matrixToBufferedImageClassMap.put("BGRToBanded", MatrixToImage.InterleavedBGRToBanded.class);
        final var matrixToBufferedImageClasses = new ArrayList<>(matrixToBufferedImageClassMap.values());

        for (int test = 0; test < 12; test++) {
            System.out.printf("%nTest #%d%n", test + 1);
            final Class<? extends MatrixToImage> matrixToBufferedImageClass =
                    mode.equalsIgnoreCase("ALL") ?
                            matrixToBufferedImageClasses.get(test % matrixToBufferedImageClasses.size()) :
                            matrixToBufferedImageClassMap.get(mode);
            if (matrixToBufferedImageClass == null) {
                throw new IllegalArgumentException("Unknown mode: " + mode);
            }
            final MatrixToImage toBufferedImage = matrixToBufferedImageClass.getConstructor().newInstance();
            if (addAlpha) {
                if (toBufferedImage instanceof MatrixToImage.InterleavedRGBToPacked c) {
                    c.setAlwaysAddAlpha(true);
                } else if (toBufferedImage instanceof MatrixToImage.InterleavedRGBToBanded c) {
                    c.setAlwaysAddAlpha(true);
                } else {
                    addAlpha = false;
                }
            }
            System.out.printf("Channel order: %s%n", toBufferedImage.channelOrder());
            var toMatrix = toBufferedImage.channelOrder() == ColorChannelOrder.RGB ?
                    new ImageToMatrix.ToInterleavedRGB() :
                    new ImageToMatrix.ToInterleavedBGR();
            toMatrix.setEnableAlpha(true);
            System.out.println("toBufferedImage converter: " + toBufferedImage);
            System.out.println("toMatrix converter: " + toMatrix);

            System.out.println("Reading " + sourceFile + "...");
            long t1 = System.nanoTime();
            BufferedImage bi1 = MatrixIO.readBufferedImage(sourceFile);
            long t2 = System.nanoTime();
            System.out.printf("Call readBufferedImage() method: %.3f ms%n", (t2 - t1) * 1e-6);
            if (monochrome) {
                t1 = System.nanoTime();
                var separate = Matrices.separate(toMatrix.toMatrix(bi1));
                var intensity = ColorMatrices.toRGBIntensity(separate);
                bi1 = new MatrixToImage.InterleavedRGBToPacked().toBufferedImage(intensity);
                t2 = System.nanoTime();
                System.out.printf("Converted to monochrome in %.3f ms%n", (t2 - t1) * 1e-6);
            }
            System.out.println("Source buffered image (1): " + AWT2MatrixTest.toString(bi1));

            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix1 = toMatrix.toMatrix(bi1);
            t2 = System.nanoTime();
            System.out.printf("Call ImageToMatrix.toMatrix() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix1) / ((t2 - t1) * 1e-9));
            System.out.printf("Result matrix (1): %s%n", matrix1);

            t1 = System.nanoTime();
            final List<Matrix<UpdatablePArray>> channels = Matrices.separate(matrix1);
            t2 = System.nanoTime();
            System.out.printf("Call separate() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix1) / ((t2 - t1) * 1e-9));
            if (channels.size() != toMatrix.resultNumberOfChannels(bi1)) {
                throw new AssertionError(channels.size() + "!=" + toMatrix.resultNumberOfChannels(bi1));
            }

            if (reduce4ChannelsTo2 && channels.size() >= 4) {
                channels.subList(1, 3).clear();
                System.out.println("Reducing 4-channel image to 2-channel image");
            }
            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> interleave = Matrices.interleave(channels);
            t2 = System.nanoTime();
            System.out.printf("Call interleave() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix1) / ((t2 - t1) * 1e-9));
            if (!reduce4ChannelsTo2 && !matrix1.equals(interleave)) {
                throw new AssertionError("separate/interleave mismatch");
            }

            t1 = System.nanoTime();
            final BufferedImage bi2 = toBufferedImage.toBufferedImage(interleave);
            t2 = System.nanoTime();
            System.out.printf("Call MatrixToImage.toBufferedImage() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(interleave) / ((t2 - t1) * 1e-9));
            System.out.println("Result of this conversion (1): " + AWT2MatrixTest.toString(bi2));
            if (addAlpha && toBufferedImage.resultNumberOfChannels(channels.size()) != 4) {
                throw new AssertionError("Invalid resultNumberOfChannels(" + channels.size() + ")");
            }
            if (bi2.getColorModel().getNumComponents() != toBufferedImage.resultNumberOfChannels(channels.size())) {
                throw new AssertionError("Invalid " + bi2.getColorModel().getNumComponents());
            }

            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix2 = toMatrix.toMatrix(bi2);
            t2 = System.nanoTime();
            System.out.printf("Call ImageToMatrix.toMatrix() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix2) / ((t2 - t1) * 1e-9));
            if (!matrix2.equals(interleave)) {
                System.out.println("        Result matrix (2): " + matrix2);
                System.out.println("        Matrix has been changed after conversion to BufferedImage and back!");
            }

            t1 = System.nanoTime();
            AWT2MatrixTest.drawTextOnImage(bi2);
            t2 = System.nanoTime();
            System.out.printf("Drawing text: %.3f ms%n", (t2 - t1) * 1e-6);

            System.out.println("Writing result of the conversion (1) into " + targetFile1 + "...");
            t1 = System.nanoTime();
            MatrixIO.writeBufferedImage(targetFile1, bi2);
            t2 = System.nanoTime();
            System.out.printf("Call writeBufferedImage() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(interleave) / ((t2 - t1) * 1e-9));

            // Testing toMatrix via ColorModel
            toMatrix.setReadingViaColorModel(true);
            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix3 = toMatrix.toMatrix(bi1);
            t2 = System.nanoTime();
            System.out.printf("Call ImageToMatrix.toMatrix() method via ColorModel: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix3) / ((t2 - t1) * 1e-9));

            t1 = System.nanoTime();
            final BufferedImage bi3 = toBufferedImage.toBufferedImage(matrix3);
            t2 = System.nanoTime();
            System.out.printf("Call MatrixToImage.toBufferedImage() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix3) / ((t2 - t1) * 1e-9));
            System.out.println("Result of this conversion (2): " + AWT2MatrixTest.toString(bi3));

            AWT2MatrixTest.drawTextOnImage(bi3);
            System.out.println("Writing result of this conversion (2) into " + targetFile2 + "...");
            MatrixIO.writeBufferedImage(targetFile2, bi3);

            if (!matrix1.equals(matrix3)) {
                System.out.println("Different behavior of ImageToMatrix while using ColorModel!");
                System.out.println("        " + matrix3);
            }

            // Testing toMatrix via Graphics
            toMatrix.setReadingViaColorModel(false);
            toMatrix.setReadingViaGraphics(true);
            t1 = System.nanoTime();
            final Matrix<UpdatablePArray> matrix4 = toMatrix.toMatrix(bi1);
            t2 = System.nanoTime();
            System.out.printf("Call ImageToMatrix.toMatrix() method via getGraphics: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix4) / ((t2 - t1) * 1e-9));

            t1 = System.nanoTime();
            final BufferedImage bi4 = toBufferedImage.toBufferedImage(matrix4);
            t2 = System.nanoTime();
            System.out.printf("Call MatrixToImage.toBufferedImage() method: %.3f ms, %.3f MB/sec%n",
                    (t2 - t1) * 1e-6, Matrices.sizeOfMB(matrix4) / ((t2 - t1) * 1e-9));
            System.out.println("Result of this conversion (3): " + AWT2MatrixTest.toString(bi4));

            AWT2MatrixTest.drawTextOnImage(bi4);
            System.out.println("Writing result of the conversion (3) into " + targetFile3 + "...");
            MatrixIO.writeBufferedImage(targetFile3, bi4);

            if (!matrix1.equals(matrix4)) {
                System.out.println("Different behavior of ImageToMatrix while using Graphics!");
                System.out.println("        " + matrix4);
            }
        }
    }
}
