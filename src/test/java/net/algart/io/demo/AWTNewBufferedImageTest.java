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

import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.io.MatrixIO;
import net.algart.io.awt.ImageToMatrix;
import net.algart.io.awt.MatrixToImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class AWTNewBufferedImageTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: " +
                            "%s result_1.jpeg/png/bmp/... " +
                    "result_2.jpeg/png/bmp/... result_3.jpeg/png/bmp/... ",
                    AWTNewBufferedImageTest.class);
            return;
        }
        Path resultFile1 = Path.of(args[0]);
        Path resultFile2 = Path.of(args[1]);
        Path resultFile3 = Path.of(args[2]);

        BufferedImage bi1 = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        checkBufferedImage(bi1, resultFile1);

        BufferedImage bi2 = new BufferedImage(1000, 1000, BufferedImage.TYPE_4BYTE_ABGR);
        checkBufferedImage(bi2, resultFile2);

        BufferedImage bi3 = new BufferedImage(1000, 1000, BufferedImage.TYPE_USHORT_565_RGB);
        checkBufferedImage(bi3, resultFile3);
    }

    private static void checkBufferedImage(BufferedImage bi1, Path file) throws IOException {
        AWT2MatrixTest.drawTextOnImage(bi1);
        System.out.printf("Written to %s%n", file);
        MatrixIO.writeBufferedImage(file, bi1);
        System.out.printf("BufferedImage: %s%n", AWT2MatrixTest.toString(bi1));
        Matrix<UpdatablePArray> m = new ImageToMatrix.ToInterleavedRGB().toMatrix(bi1);
        System.out.printf("Converted to matrix: %s%n", m);
        file = Path.of(file + ".converted.png");
        MatrixIO.writeBufferedImage(file, new MatrixToImage.InterleavedRGBToInterleaved().toBufferedImage(m));
        System.out.printf("Written after conversion to %s%n%n", file);
    }
}
