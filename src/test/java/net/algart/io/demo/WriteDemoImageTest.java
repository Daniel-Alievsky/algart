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

import net.algart.arrays.*;
import net.algart.io.MatrixIO;
import net.algart.io.awt.MatrixToBufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class WriteDemoImageTest {
    public static void main(String[] args) throws IOException {
        int startArgIndex = 0;
        boolean imageIOWrite = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-imageIOWrite")) {
            imageIOWrite = true;
            startArgIndex++;
        }
        String formatName = null;
        if (startArgIndex < args.length && args[startArgIndex].toLowerCase().startsWith("-formatname=")) {
            formatName = args[startArgIndex].substring("-formatname=".length());
            startArgIndex++;
        }
        if (args.length < startArgIndex + 5) {
            System.out.printf("Usage: [-imageIOWrite] [-formatName=XXX] " +
                            "%s target_image.jpg/png/bmp byte|short|int|long|float|double " +
                            "channels dimX dimY [quality]%n",
                    WriteDemoImageTest.class);
            return;
        }
        final Path targetFile = Paths.get(args[startArgIndex]);
        final String elementTypeName = args[startArgIndex + 1];
        final Class<?> elementType  = elementTypeName.equals("boolean") ? boolean.class
                : elementTypeName.equals("char") ? char.class
                : elementTypeName.equals("byte") ? byte.class
                : elementTypeName.equals("short") ? short.class
                : elementTypeName.equals("int") ? int.class
                : elementTypeName.equals("long") ? long.class
                : elementTypeName.equals("float") ? float.class
                : elementTypeName.equals("double") ? double.class : null;
        if (elementType == null) {
            throw new IllegalArgumentException("Invalid element type name " + elementTypeName);
        }
        final int channels = Integer.parseInt(args[startArgIndex + 2]);
        final int dimX = Integer.parseInt(args[startArgIndex + 3]);
        final int dimY = Integer.parseInt(args[startArgIndex + 4]);
        final Double quality = args.length < startArgIndex + 6 ? null : Double.parseDouble(args[startArgIndex + 5]);
        Object array = makeSamples(elementType, channels, dimX, dimY);
        List<Matrix<UpdatablePArray>> image = Matrix.as(array, dimX, dimY, channels).asLayers();

        final Matrix<PArray> matrix = Matrices.interleave(image);
        final BufferedImage bi = new MatrixToBufferedImage.InterleavedRGBToInterleaved()
                .setUnsignedInt32(true)
                .toBufferedImage(matrix);
        System.out.println(AWT2MatrixTest.toString(bi));
        final String fileSuffix = MatrixIO.extension(targetFile);
        if (imageIOWrite) {
            System.out.println("Writing " + targetFile + " via ImageIO.write...");
            if (!ImageIO.write(bi, fileSuffix, targetFile.toFile())) {
                throw new IOException("Failed to write image " + targetFile);
            }
        } else if (formatName != null) {
            System.out.println("Writing " + targetFile +  " by format name \"" + formatName + "\"...");
            MatrixIO.writeBufferedImageByFormatName(targetFile, bi, formatName, param -> setQuality(param, quality));
        } else {
            System.out.println("Writing " + targetFile + " by file suffix...");
            MatrixIO.writeBufferedImage(targetFile, bi, param -> setQuality(param, quality));
        }
    }

    private static void setQuality(ImageWriteParam param, Double quality) {
        if (quality != null) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            if (param.getCompressionType() != null) {
                param.setCompressionQuality(quality.floatValue());
            } else {
                System.out.println("No default compression");
            }
        }
    }

    static Object makeSamples(Class<?> elementType, int bandCount, int dimX, int dimY) {
        final int matrixSize = dimX * dimY;
        if (elementType == byte.class) {
            byte[] channels = new byte[bandCount * matrixSize];
            if (bandCount == 4) {
                Arrays.fill(channels, 3 * matrixSize, 4 * matrixSize, (byte) 128);
            }
            for (int y = 0; y < dimY; y++) {
                int c1 = (y / 32) % (bandCount + 1) - 1;
                int c2 = c1;
                if (c1 == -1) {
                    c1 = 0;
                    c2 = bandCount - 1;
                }
                for (int c = c1; c <= c2; c++) {
                    for (int x = 0, disp = y * dimX; x < dimX; x++, disp++) {
                        channels[disp + c * matrixSize] = (byte) (x + y);
                    }
                }
            }
            return channels;
        } else if (elementType == short.class) {
            short[] channels = new short[bandCount * matrixSize];
            if (bandCount == 4) {
                Arrays.fill(channels, 3 * matrixSize, 4 * matrixSize, (short) 32768);
            }
            for (int y = 0; y < dimY; y++) {
                int c1 = (y / 32) % (bandCount + 1) - 1;
                int c2 = c1;
                if (c1 == -1) {
                    c1 = 0;
                    c2 = bandCount - 1;
                }
                for (int c = c1; c <= c2; c++) {
                    for (int x = 0, disp = y * dimX; x < dimX; x++, disp++) {
                        channels[disp + c * matrixSize] = (short) (157 * (x + y));
                    }
                }
            }
            return channels;
        } else if (elementType == int.class) {
            int[] channels = new int[bandCount * matrixSize];
            if (bandCount == 4) {
                Arrays.fill(channels, 3 * matrixSize, 4 * matrixSize, Integer.MAX_VALUE);
            }
            for (int y = 0, disp = 0; y < dimY; y++) {
                final int c = (y / 32) % bandCount;
                for (int x = 0; x < dimX; x++, disp++) {
                    channels[disp + c * matrixSize] = 157 * 65536 * (x + y);
                }
            }
            return channels;
        } else if (elementType == float.class) {
            float[] channels = new float[bandCount * matrixSize];
            if (bandCount == 4) {
                Arrays.fill(channels, 3 * matrixSize, 4 * matrixSize, 0.5f);
            }
            for (int y = 0, disp = 0; y < dimY; y++) {
                final int c = (y / 32) % bandCount;
                for (int x = 0; x < dimX; x++, disp++) {
                    int v = (x + y) & 0xFF;
                    channels[disp + c * matrixSize] = (float) (0.5 + 1.5 * (v / 256.0 - 0.5));
                }
            }
            return channels;
        } else if (elementType == double.class) {
            double[] channels = new double[bandCount * matrixSize];
            if (bandCount == 4) {
                Arrays.fill(channels, 3 * matrixSize, 4 * matrixSize, 0.5);
            }
            for (int y = 0, disp = 0; y < dimY; y++) {
                final int c = (y / 32) % bandCount;
                for (int x = 0; x < dimX; x++, disp++) {
                    int v = (x + y) & 0xFF;
                    channels[disp + c * matrixSize] = (float) (0.5 + 1.5 * (v / 256.0 - 0.5));
                }
            }
            return channels;
        }
        throw new UnsupportedOperationException("Unsupported elementType = " + elementType);
    }

}
