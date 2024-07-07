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

import net.algart.arrays.ColorChannelOrder;
import net.algart.io.awt.BufferedImageToMatrix;
import net.algart.io.awt.MatrixToBufferedImage;

import java.awt.*;

public class ConverterNames {
    private static void show(BufferedImageToMatrix converter, ColorChannelOrder order) {
        System.out.println(converter);
        if (converter.channelOrder() != order) {
            throw new AssertionError();
        }
    }

    private static void show(MatrixToBufferedImage converter, ColorChannelOrder order) {
        System.out.println(converter);
        if (converter.channelOrder() != order) {
            throw new AssertionError();
        }
    }

    public static void main(String[] args) {
        show(new BufferedImageToMatrix.ToInterleavedRGB(), ColorChannelOrder.RGB);
        show(new BufferedImageToMatrix.ToInterleavedBGR(), ColorChannelOrder.BGR);
        show(new BufferedImageToMatrix.ToInterleavedBGR().setReadingViaColorModel(true), ColorChannelOrder.BGR);
        show(new BufferedImageToMatrix.ToInterleavedBGR().setReadingViaGraphics2D(true), ColorChannelOrder.BGR);
        show(new BufferedImageToMatrix.ToInterleavedBGR()
                .setReadingViaColorModel(true).setReadingViaGraphics2D(true), ColorChannelOrder.BGR);
        show(new MatrixToBufferedImage.InterleavedRGBToPacked(), ColorChannelOrder.RGB);
        show(new MatrixToBufferedImage.InterleavedBGRToPacked(), ColorChannelOrder.BGR);
        show(new MatrixToBufferedImage.InterleavedBGRToPacked().setAlwaysAddAlpha(true), ColorChannelOrder.BGR);
        show(new MatrixToBufferedImage.InterleavedRGBToInterleaved(), ColorChannelOrder.RGB);
        show(new MatrixToBufferedImage.InterleavedBGRToInterleaved(), ColorChannelOrder.BGR);
        show(new MatrixToBufferedImage.InterleavedRGBToBanded(), ColorChannelOrder.RGB);
        show(new MatrixToBufferedImage.InterleavedBGRToBanded(), ColorChannelOrder.BGR);
        show(new MatrixToBufferedImage.InterleavedBGRToBanded().setAlwaysAddAlpha(true), ColorChannelOrder.BGR);
        show(new MatrixToBufferedImage.MonochromeToIndexed(Color.BLUE, Color.WHITE), ColorChannelOrder.RGB);
    }
}
