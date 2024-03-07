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
import net.algart.external.awt.MatrixToBufferedImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WriteDemoImageTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.out.printf("Usage: %s target_image.jpg/png/bmp byte|short|int|long|float|double " +
                            "channels dimX dimY%n",
                    WriteDemoImageTest.class);
            return;
        }
        final Path targetFile = Paths.get(args[0]);
        final String elementTypeName = args[1];
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
        final int channels = Integer.parseInt(args[2]);
        final int dimX = Integer.parseInt(args[3]);
        final int dimY = Integer.parseInt(args[4]);
        Object array = makeSamples(elementType, channels, dimX, dimY);
        List<Matrix<UpdatablePArray>> image = SimpleMemoryModel.asMatrix(array, dimX, dimY, channels).asLayers();

        System.out.println("Writing " + targetFile + "...");
        final Matrix<PArray> matrix = Matrices.interleave(null, image);
        final BufferedImage bi = new MatrixToBufferedImage.InterleavedRGBToInterleaved()
                .setUnsignedInt32(true)
                .toBufferedImage(matrix);
        MatrixIO.writeBufferedImage(targetFile, bi);
    }

    private static Object makeSamples(Class<?> elementType, int bandCount, int dimX, int dimY) {
        final int matrixSize = dimX * dimY;
        if (elementType == byte.class) {
            byte[] channels = new byte[matrixSize * bandCount];
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
            short[] channels = new short[matrixSize * bandCount];
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
            int[] channels = new int[matrixSize * bandCount];
            for (int y = 0, disp = 0; y < dimY; y++) {
                final int c = (y / 32) % bandCount;
                for (int x = 0; x < dimX; x++, disp++) {
                    channels[disp + c * matrixSize] = 157 * 65536 * (x + y);
                }
            }
            return channels;
        } else if (elementType == float.class) {
            float[] channels = new float[matrixSize * bandCount];
            for (int y = 0, disp = 0; y < dimY; y++) {
                final int c = (y / 32) % bandCount;
                for (int x = 0; x < dimX; x++, disp++) {
                    int v = (x + y) & 0xFF;
                    channels[disp + c * matrixSize] = (float) (0.5 + 1.5 * (v / 256.0 - 0.5));
                }
            }
            return channels;
        } else if (elementType == double.class) {
            double[] channels = new double[matrixSize * bandCount];
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
