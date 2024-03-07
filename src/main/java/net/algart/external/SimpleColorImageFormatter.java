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

package net.algart.external;

import net.algart.arrays.*;
import net.algart.math.functions.LinearFunc;

import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.awt.image.DataBuffer;
import java.util.ArrayList;
import java.util.List;

// Deprecated - should be replaced with new methods in Converters
class SimpleColorImageFormatter implements ColorImageFormatter {
    public static interface ColorBandsCorrector {
        public byte[][] getPalette(); // ignored if null

        public byte[][] correct(byte[][] rgbAlpha);
    }

    /**
     * Translates 2-band image (gray + alpha) to 4-band (RGB + alpha).
     */
    public static class SimpleCorrectorForMonochrome implements ColorBandsCorrector {
        public byte[][] getPalette() {
            return null;
        }

        public byte[][] correct(byte[][] rgbAlpha) {
            if (rgbAlpha.length != 2) {
                return rgbAlpha;
            }
            byte[][] result = new byte[rgbAlpha.length + 2][];
            for (int k = 0; k < 3; k++) {
                result[k] = rgbAlpha[0];
            }
            assert result.length <= 4;
            if (result.length == 4) {
                result[3] = rgbAlpha[1];
            }
            return result;
        }
    }

    /**
     * Translates 1-band image (monochrome) to 1-band indexed one (fixed RGB color + alpha) or to 4-band RGB+Alpha.
     */
    public static class MonochromeToAlphaCorrector implements ColorBandsCorrector {
        private final byte[] baseColor;
        private boolean indexedResult = true;

        public MonochromeToAlphaCorrector(java.awt.Color baseColor) {
            if (baseColor == null)
                throw new NullPointerException("Null base color");
            this.baseColor = new byte[] {
                (byte) baseColor.getRed(), (byte) baseColor.getGreen(), (byte) baseColor.getBlue()
            };
        }

        public MonochromeToAlphaCorrector(double[] baseColor) {
            if (baseColor == null)
                throw new NullPointerException("Null base color");
            if (baseColor.length < 3)
                throw new IllegalArgumentException("3 RGB base color components required");
            this.baseColor = new byte[3];
            for (int k = 0; k < 3; k++) {
                double bc = baseColor[k];
                this.baseColor[k] = (byte)(bc < 0.0 ? 0 : bc > 1.0 ? 255 : Math.round(bc / 255.0));
            }
        }

        public boolean isIndexedResult() {
            return indexedResult;
        }

        public MonochromeToAlphaCorrector setIndexedResult(boolean indexedResult) {
            this.indexedResult = indexedResult;
            return this;
        }

        public byte[][] getPalette() {
            byte[][] result = new byte[4][256];
            for (int k = 0; k < 256; k++) {
                for (int bandIndex = 0; bandIndex < 3; bandIndex++) {
                    result[bandIndex][k] = baseColor[bandIndex];
                }
                result[3][k] = (byte) k;
            }
            return result;
        }

        public byte[][] correct(byte[][] rgbAlpha) {
            if (indexedResult || rgbAlpha.length > 1) {
                return rgbAlpha;
            }
            byte[][] result = new byte[4][];
            result[3] = rgbAlpha[0];
            for (int k = 0; k < 3; k++) {
                result[k] = new byte[rgbAlpha[0].length];
                JArrays.fillByteArray(result[k], baseColor[k]);
            }
            return result;
        }
    }

    private boolean packedBandsInSingleInt = true;
    private boolean readPixelValuesViaColorModel = false;
    private ColorBandsCorrector corrector = new SimpleCorrectorForMonochrome();

    public boolean isPackedBandsInSingleInt() {
        return packedBandsInSingleInt;
    }

    public SimpleColorImageFormatter setPackedBandsInSingleInt(boolean packedBandsInSingleInt) {
        this.packedBandsInSingleInt = packedBandsInSingleInt;
        return this;
    }

    public boolean isReadPixelValuesViaColorModel() {
        return readPixelValuesViaColorModel;
    }

    public SimpleColorImageFormatter setReadPixelValuesViaColorModel(boolean readPixelValuesViaColorModel) {
        this.readPixelValuesViaColorModel = readPixelValuesViaColorModel;
        return this;
    }

    public ColorBandsCorrector getCorrector() {
        return corrector;
    }

    public SimpleColorImageFormatter setCorrector(ColorBandsCorrector corrector) {
        if (corrector == null)
            throw new NullPointerException("Null corrector");
        this.corrector = corrector;
        return this;
    }

    public BufferedImage toBufferedImage(List<? extends Matrix<? extends PArray>> image) {
        if (image == null)
            throw new NullPointerException("Null image");
        image = new ArrayList<Matrix<? extends PArray>>(image);
        // cloning before checking guarantees correct check while multithreading
        if (image.isEmpty())
            throw new IllegalArgumentException("Empty list of image bands");
        if (image.get(0).size() > Integer.MAX_VALUE)
            throw new TooLargeArrayException("The image is too large "
                + "and cannot be converted to BufferedImage: it contains >Integer.MAX_VALUE pixels");
        int numberOfBands = image.size();
        if (numberOfBands > 4)
            throw new IllegalArgumentException("The image contains " + numberOfBands + ">4 bands "
                + "and cannot be converted to BufferedImage: don't know what to do with extra bands");
        final int dimX = (int) image.get(0).dimX();
        final int dimY = (int) image.get(0).dimY();
        final int size = (int) image.get(0).size();
        byte[][] rgbAlpha = new byte[numberOfBands][];
        for (int k = 0; k < rgbAlpha.length; k++) {
            Matrix<? extends PArray> m = image.get(k);
            if (!m.dimEquals(image.get(0)))
                throw new SizeMismatchException("Dimensions mismatch in the matrices of the image");
            assert m.size() == size;
            rgbAlpha[k] = new byte[size];
            UpdatableByteArray dest = SimpleMemoryModel.asUpdatableByteArray(rgbAlpha[k]);
            if (m.array().bitsPerElement() == 8) {
                dest.copy(m.array());
            } else {
                double max = m.array().maxPossibleValue(1.0);
                Arrays.applyFunc(null, LinearFunc.getInstance(0.0, 255.0 / max), dest, m.array());
            }
        }
        rgbAlpha = corrector.correct(rgbAlpha);
        boolean hasAlpha = rgbAlpha.length == 4;
        if (packedBandsInSingleInt && rgbAlpha.length >= 3) {
            int[] packedRGB = new int[size];
            if (hasAlpha) {
                for (int j = 0; j < size; j++) {
                    packedRGB[j] = (rgbAlpha[0][j] & 0xFF) << 16
                        | (rgbAlpha[1][j] & 0xFF) << 8
                        | (rgbAlpha[2][j] & 0xFF)
                        | (rgbAlpha[3][j] & 0xFF) << 24;
                }
            } else {
                for (int j = 0; j < size; j++) {
                    packedRGB[j] = (rgbAlpha[0][j] & 0xFF) << 16
                        | (rgbAlpha[1][j] & 0xFF) << 8
                        | (rgbAlpha[2][j] & 0xFF)
                        | 0xFF000000;
                }
            }
            int[] bandMasks = hasAlpha ?
                new int[]{0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000} :
                new int[]{0x00FF0000, 0x0000FF00, 0x000000FF};
            WritableRaster wr = Raster.createPackedRaster(new DataBufferInt(packedRGB, packedRGB.length),
                dimX, dimY, dimX, bandMasks, null);
            DirectColorModel cm = hasAlpha ?
                new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000) :
                new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x0);
            return new BufferedImage(cm, wr, false, null);

        }
        byte[][] palette = rgbAlpha.length == 1 ? corrector.getPalette() : null;
        DataBufferByte dataBuffer = new DataBufferByte(rgbAlpha, rgbAlpha[0].length);
        if (palette != null) {
            if (palette.length < 3)
                throw new AssertionError("getPalette() method must return palette with 3 or 4 bands");
//            int[] paletteRGBA = new int[256]; // no difference with the following simpler code
//            for (int k = 0; k < 256; k++) {
//                paletteRGBA[k] = (palette[0][k] & 0xFF) << 16
//                    | (palette[1][k] & 0xFF) << 8
//                    | (palette[2][k] & 0xFF)
//                    | (palette.length == 3 ? 0xFF000000 : (palette[3][k] & 0xFF) << 24);
//            }
//            IndexColorModel cm = new IndexColorModel(Byte.SIZE, 256, paletteRGBA, 0,
//                palette.length >= 4, -1, DataBuffer.TYPE_BYTE);
            IndexColorModel cm = palette.length == 3 ?
                new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2]) :
                new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2], palette[3]);
            WritableRaster wr = Raster.createInterleavedRaster(dataBuffer, dimX, dimY, dimX, 1, new int[]{0}, null);
            return new BufferedImage(cm, wr, false, null);
        }
        int[] indexes = new int[rgbAlpha.length];
        int[] offsets = new int[rgbAlpha.length];
        int[] bits = new int[rgbAlpha.length];
        for (int k = 0; k < rgbAlpha.length; k++) {
            indexes[k] = k;
            offsets[k] = 0;
            bits[k] = 8;
        }
        WritableRaster wr = Raster.createBandedRaster(dataBuffer, dimX, dimY, dimX, indexes, offsets, null);
        ColorSpace cs = ColorSpace.getInstance(rgbAlpha.length == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        ComponentColorModel cm = new ComponentColorModel(cs, bits,
            hasAlpha, false, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        return new BufferedImage(cm, wr, false, null);
    }

    public List<Matrix<UpdatablePArray>> toImage(BufferedImage bufferedImage) {
        if (bufferedImage == null)
            throw new NullPointerException("Null buffered image");
        int dimX = bufferedImage.getWidth();
        int dimY = bufferedImage.getHeight();
        ColorModel cm = bufferedImage.getColorModel();
        byte[][] rgbAlpha;
        if (readPixelValuesViaColorModel) {
            rgbAlpha = new byte[cm.hasAlpha() ? 4 : 3][dimX * dimY];
            // note: for gray image, the results will be invalid (too light)
            WritableRaster wr = bufferedImage.getRaster();
            Object outData = null;
            for (int y = 0, disp = 0; y < dimY; y++) {
                for (int x = 0; x < dimX; x++, disp++) {
                    outData = wr.getDataElements(x, y, outData);
                    rgbAlpha[0][disp] = (byte) cm.getRed(outData);
                    rgbAlpha[1][disp] = (byte) cm.getGreen(outData);
                    rgbAlpha[2][disp] = (byte) cm.getBlue(outData);
                    if (rgbAlpha.length == 4) {
                        rgbAlpha[3][disp] = (byte) cm.getAlpha(outData);
                    }
                }
            }
        } else {
            boolean gray = cm.getNumComponents() == 1 && cm instanceof ComponentColorModel;
            boolean banded = bufferedImage.getSampleModel() instanceof BandedSampleModel;
            // even if gray, but not banded, we make full RGB image:
            // if no, ColorSpace.CS_GRAY produces invalid values (too dark)
            rgbAlpha = new byte[cm.hasAlpha() ? 4 : gray && banded ? 1 : 3][dimX * dimY];
            DataBufferByte dataBuffer = new DataBufferByte(rgbAlpha, rgbAlpha[0].length);
            int[] indexes = new int[rgbAlpha.length];
            int[] offsets = new int[rgbAlpha.length];
            int[] bits = new int[rgbAlpha.length];
            for (int k = 0; k < rgbAlpha.length; k++) {
                indexes[k] = k;
                offsets[k] = 0;
                bits[k] = 8;
            }
            WritableRaster wr = Raster.createBandedRaster(dataBuffer, dimX, dimY, dimX, indexes, offsets, null);
            ColorSpace cs = ColorSpace.getInstance(rgbAlpha.length == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
            cm = new ComponentColorModel(cs, bits, cm.hasAlpha(), false, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            BufferedImage result = new BufferedImage(cm, wr, false, null);
            result.getGraphics().drawImage(bufferedImage, 0, 0, null);
            if (gray) {
                rgbAlpha = new byte[][] {rgbAlpha[0]}; // only 1 band is interesting
            }
        }
        List<Matrix<UpdatablePArray>> data = new ArrayList<>();
        for (byte[] band : rgbAlpha) {
            data.add(Matrices.matrix(SimpleMemoryModel.asUpdatableByteArray(band), dimX, dimY));
        }
        return data;
    }
}
