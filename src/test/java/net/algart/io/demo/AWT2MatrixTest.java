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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.io.MatrixIO;
import net.algart.io.awt.ImageToMatrix;
import net.algart.io.awt.MatrixToImage;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AWT2MatrixTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.out.println("Usage:");
            System.out.println("    " + AWT2MatrixTest.class.getName()
                    + " some_image.jpeg/png/bmp/... result_1.jpeg/png/bmp/... " +
                    "result_2.jpeg/png/bmp/... result_3.jpeg/png/bmp/... result_4.jpeg/png/bmp/... ");
            return;
        }

        final Path srcFile = Path.of(args[0]);
        final Path resultFile1 = Path.of(args[1]);
        final Path resultFile2 = Path.of(args[2]);
        final Path resultFile3 = Path.of(args[3]);
        final Path resultFile4 = Path.of(args[4]);

        System.out.printf("Reading %s...%n", srcFile);
        BufferedImage bi1 = MatrixIO.readBufferedImage(srcFile);
        System.out.printf("BufferedImage: %s%n", bi1);
        System.out.printf("Writing BufferedImage to %s...%n", resultFile1);
        MatrixIO.writeBufferedImage(resultFile1, bi1);

        final Matrix<UpdatablePArray> matrix = new ImageToMatrix.ToInterleavedRGB().toMatrix(bi1);
        List<Matrix<UpdatablePArray>> matrices = Matrices.separate(matrix);
        final int dimX = matrices.get(0).dimX32();
        final int dimY = matrices.get(0).dimY32();

        BufferedImage bi2 = new MatrixToImage.InterleavedRGBToInterleaved().toBufferedImage(
                Matrices.interleave(matrices));
        System.out.printf("BufferedImage: %s%n", bi2);
        System.out.printf("Writing AlgART InterleavedRGBToInterleavedSamples to %s...%n", resultFile2);
        MatrixIO.writeBufferedImage(resultFile2, bi2);

        BufferedImage bi3 = new MatrixToImage.InterleavedRGBToPacked().toBufferedImage(
                Matrices.interleave(matrices));
        System.out.printf("BufferedImage: %s%n", bi3);
        System.out.printf("Writing AlgART InterleavedRGBToPackedSamples to %s...%n", resultFile3);
        MatrixIO.writeBufferedImage(resultFile3, bi3);

        BufferedImage bi4 = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_BGR);
        drawTextOnImage(bi4);
        System.out.printf("BufferedImage: %s%n", bi4);
        System.out.printf("Writing new test image to %s...%n", resultFile4);
        MatrixIO.writeBufferedImage(resultFile4, bi4);
        // Note: JPEG2000 will be written incorrectly in jai-imageio-jpeg2000 1.4.0!
    }

    static void drawTextOnImage(BufferedImage bufferedImage) {
        Graphics graphics = bufferedImage.getGraphics();
        graphics.setFont(new Font("Monospaced", Font.BOLD, 60));
        graphics.setColor(new Color(0xFFFF80));
        graphics.drawString("Hello", 100, 100);
    }

    static String toString(BufferedImage bi) {
        final SampleModel sm = bi.getSampleModel();
        final ColorSpace cs = bi.getColorModel().getColorSpace();
        final DataBuffer db = bi.getData().getDataBuffer();
        return bi +
                "; sample model: " + sm + " " +
                sm.getWidth() + "x" + sm.getHeight() + "x" + sm.getNumBands() +
                " type " + sm.getDataType() + " (" + ImageToMatrix.tryToDetectElementType(sm) + ")" +
                "; data buffer: " + db + " type " + db.getDataType() + ", " + db.getNumBanks() + " banks" +
                "; color space: numComponents " + cs.getNumComponents() + ", type " + cs.getType();
    }
}
