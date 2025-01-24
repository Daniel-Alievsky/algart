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

import net.algart.arrays.ByteArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.io.MatrixIO;
import net.algart.io.awt.ImageToMatrix;
import net.algart.io.awt.MatrixToImage;
import net.algart.math.functions.Func2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WritePaletteImageTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: %s target_image.png/bmp.gif dimX dimY%n",
                    WritePaletteImageTest.class);
            return;
        }
        final Path targetFile = Paths.get(args[0]);
        final int dimX = Integer.parseInt(args[1]);
        final int dimY = Integer.parseInt(args[2]);
        Matrix<? extends PArray> m = Matrices.asCoordFuncMatrix(
                (Func2) (x, y) -> (x + y) / 2, ByteArray.class, dimX, dimY).clone();
        final BufferedImage bi = new MatrixToImage.MonochromeToIndexed(Color.BLUE, Color.RED)
                .toBufferedImage(m);
        System.out.println(AWT2MatrixTest.toString(bi));
        System.out.println(ImageToMatrix.defaultNumberOfChannels(bi) + " channels, " +
                ImageToMatrix.defaultElementType(bi) + " elements");

        System.out.println("Writing " + targetFile + "...");
        MatrixIO.writeBufferedImage(targetFile, bi);
    }
}
