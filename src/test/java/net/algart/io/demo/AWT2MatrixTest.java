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
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("    " + AWT2MatrixTest.class.getName()
                    + " some_image.jpeg/png/bmp/... result_1.jpeg/png/bmp/... " +
                    "result_2.jpeg/png/bmp/... result_3.jpeg/png/bmp/... " +
                    "result_4.jpeg/png/bmp/... result_5BGR.jpeg/png/bmp/... " +
                    "result_6.jpeg/png/bmp/...");
            return;
        }

        final Path srcFile = Path.of(args[0]);
        final Path resultFile1 = Path.of(args[1]);
        final Path resultFile2 = args.length <= 2 ? null : Path.of(args[2]);
        final Path resultFile3 = args.length <= 3 ? null : Path.of(args[3]);
        final Path resultFile4 = args.length <= 4 ? null : Path.of(args[4]);
        final Path resultFile5BGR = args.length <= 5 ? null : Path.of(args[5]);
        final Path resultFile6 = args.length <= 6 ? null : Path.of(args[6]);

        System.out.printf("Reading %s...%n", srcFile);
        BufferedImage bi1 = MatrixIO.readBufferedImage(srcFile);
        System.out.printf("BufferedImage: %s%n", bi1);
        System.out.printf("Writing BufferedImage to %s...%n", resultFile1);
        MatrixIO.writeBufferedImage(resultFile1, bi1);

        final Matrix<UpdatablePArray> interleaved1 = ImageToMatrix.toInterleaved(bi1);
        List<Matrix<UpdatablePArray>> channels = ImageToMatrix.toChannels(bi1);
        final int dimX = channels.getFirst().dimX32();
        final int dimY = channels.getFirst().dimY32();
        final Matrix<UpdatablePArray> interleaved2 = Matrices.interleave(channels);
        if (!interleaved1.equals(interleaved2)) {
            throw new AssertionError("separate/interleave mismatch");
        }

        BufferedImage bi2 = MatrixToImage.ofInterleaved(interleaved2, false);
        System.out.printf("BufferedImage: %s%n", bi2);
        if (resultFile2 != null) {
            System.out.printf("Writing AlgART MatrixToImage.ofInterleaved to %s...%n", resultFile2);
            MatrixIO.writeBufferedImage(resultFile2, bi2);
        }

        BufferedImage bi3 = new MatrixToImage.InterleavedRGBToPacked().toBufferedImage(interleaved2);
        System.out.printf("BufferedImage: %s%n", bi3);
        if (resultFile3 != null) {
            System.out.printf("Writing AlgART InterleavedRGBToPacked to %s...%n", resultFile3);
            MatrixIO.writeBufferedImage(resultFile3, bi3);
        }

        BufferedImage bi4 = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_BGR);
        drawTextOnImage(bi4);
        System.out.printf("BufferedImage: %s%n", bi4);
        if (resultFile4 != null) {
            System.out.printf("Writing new test image to %s...%n", resultFile4);
            MatrixIO.writeBufferedImage(resultFile4, bi4);
            // Note: JPEG2000 will be written incorrectly in jai-imageio-jpeg2000 1.4.0!
        }

        final Matrix<UpdatablePArray> interleavedBGR = ImageToMatrix.toInterleavedBGR(bi1);
        final List<Matrix<UpdatablePArray>> channelsBGR = Matrices.separate(interleavedBGR);
        if (resultFile5BGR != null) {
            System.out.printf("Writing AlgART inverted R-B channels to %s...%n", resultFile5BGR);
            MatrixIO.writeImage(resultFile5BGR, channelsBGR);
        }
        BufferedImage bi6 = MatrixToImage.ofInterleavedBGR(interleavedBGR);
        if (resultFile6 != null) {
            System.out.printf("Writing AlgART MatrixToImage.ofInterleavedBGR to %s...%n", resultFile6);
            MatrixIO.writeBufferedImage(resultFile6, bi6);
        }

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
