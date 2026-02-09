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

import net.algart.arrays.ColorChannelOrder;
import net.algart.io.awt.ImageToMatrix;
import net.algart.io.awt.MatrixToImage;

import java.awt.*;

public class ConverterNames {
    private static void show(ImageToMatrix converter, ColorChannelOrder order) {
        System.out.println(converter);
        if (converter.channelOrder() != order) {
            throw new AssertionError();
        }
    }

    private static void show(MatrixToImage converter, ColorChannelOrder order) {
        System.out.println(converter);
        if (converter.channelOrder() != order) {
            throw new AssertionError();
        }
    }

    public static void main(String[] args) {
        show(new ImageToMatrix.ToInterleavedRGB(), ColorChannelOrder.RGB);
        show(new ImageToMatrix.ToInterleavedBGR(), ColorChannelOrder.BGR);
        show(new ImageToMatrix.ToInterleavedBGR().setReadingViaColorModel(true), ColorChannelOrder.BGR);
        show(new ImageToMatrix.ToInterleavedBGR().setReadingViaGraphics(true), ColorChannelOrder.BGR);
        show(new ImageToMatrix.ToInterleavedBGR()
                .setReadingViaColorModel(true).setReadingViaGraphics(true), ColorChannelOrder.BGR);
        show(new MatrixToImage.InterleavedRGBToPacked(), ColorChannelOrder.RGB);
        show(new MatrixToImage.InterleavedBGRToPacked(), ColorChannelOrder.BGR);
        show(new MatrixToImage.InterleavedBGRToPacked().setAlwaysAddAlpha(true), ColorChannelOrder.BGR);
        show(new MatrixToImage.InterleavedRGBToInterleaved(), ColorChannelOrder.RGB);
        show(new MatrixToImage.InterleavedBGRToInterleaved(), ColorChannelOrder.BGR);
        show(new MatrixToImage.InterleavedRGBToBanded(), ColorChannelOrder.RGB);
        show(new MatrixToImage.InterleavedBGRToBanded(), ColorChannelOrder.BGR);
        show(new MatrixToImage.InterleavedBGRToBanded().setAlwaysAddAlpha(true), ColorChannelOrder.BGR);
        show(new MatrixToImage.MonochromeToIndexed(Color.BLUE, Color.WHITE), ColorChannelOrder.RGB);
    }
}
