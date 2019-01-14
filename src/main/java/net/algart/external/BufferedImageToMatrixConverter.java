/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.awt.image.DataBuffer;

public abstract class BufferedImageToMatrixConverter {
    private static final JArrayPool INT_ARRAY_POOL = JArrayPool.getInstance(int.class, 32768);
    // - too high values decrease performance!

    private static final boolean USE_3_BANDS_FOR_NON_BANDED_GRAY = true;
    // - must be true to avoid a problem with reading gray images via Graphics2D ("simplest" algorithm)

    private final boolean addAlphaWhenExist;
    // if true and BufferedImage contains alpha-channel, the matrix 4xMxN will be returned by ToPacked3D class
    // if false, but the source has alpha, it may be interpreted, not ignored

    protected BufferedImageToMatrixConverter(boolean addAlphaWhenExist) {
        this.addAlphaWhenExist = addAlphaWhenExist;
    }

    public final boolean addAlphaWhenExist() {
        return addAlphaWhenExist;
    }

    public Matrix<? extends UpdatablePArray> toMatrix(BufferedImage bufferedImage) {
        return toMatrix(bufferedImage, null);
    }

    public Matrix<? extends UpdatablePArray> toMatrix(BufferedImage bufferedImage, UpdatablePArray resultArray) {
        if (bufferedImage == null)
            throw new NullPointerException("Null bufferedImage");
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int bandCount = getBandCount(bufferedImage);
        long[] dimensions = getResultMatrixDimensions(width, height, bandCount);
        Class<?> elementType = getResultElementType(bufferedImage);
        long size = Arrays.longMul(dimensions);
        if (size > Integer.MAX_VALUE || size == Long.MIN_VALUE)
            throw new AssertionError("Illegal getResultMatrixDimensions implementation: too large results");
        Object resultData = null;
        if (resultArray != null) {
            if (resultArray.elementType() != elementType)
                throw new IllegalArgumentException("Incompatible result array: element type should be "
                    + elementType);
            if (resultArray instanceof DirectAccessible && ((DirectAccessible) resultArray).hasJavaArray()
                && ((DirectAccessible) resultArray).javaArrayOffset() == 0
                && resultArray.length() >= size)
            {
                resultData = ((DirectAccessible) resultArray).javaArray();
            }
        }
        if (resultData == null) {
            resultData = java.lang.reflect.Array.newInstance(elementType, (int) size);
        }
        toJavaArray(bufferedImage, resultData);
        return Matrices.matrix((UpdatablePArray) SimpleMemoryModel.asUpdatableArray(resultData), dimensions);
    }

    public int getBandCount(BufferedImage bufferedImage) {
        if (bufferedImage == null)
            throw new NullPointerException("Null bufferedImage");
        ColorModel cm = bufferedImage.getColorModel();
        boolean gray = cm.getNumComponents() == 1;
        // ...SampleModel sm = bufferedImage.getSampleModel();...
        // || (sm.getNumBands() == 1 && sm.getNumDataElements() == 1) - not correct!
        // In some TIF we have only 1 band, but RGB color model, because it uses palette; so, bandCount must be 3.
        return cm.hasAlpha() && addAlphaWhenExist ? 4 : gray ? 1 : 3;
    }

    public abstract Class<?> getResultElementType(BufferedImage bufferedImage); // must be primitive and not boolean

    public final long[] getResultMatrixDimensions(BufferedImage bufferedImage) {
        if (bufferedImage == null)
            throw new NullPointerException("Null bufferedImage");
        return getResultMatrixDimensions(
            bufferedImage.getWidth(),
            bufferedImage.getHeight(),
            getBandCount(bufferedImage));
    }

    public abstract long[] getResultMatrixDimensions(int width, int height, int bandCount);

    protected abstract void toJavaArray(BufferedImage bufferedImage, Object resultJavaArray);

    public static final class ToPacked3D extends BufferedImageToMatrixConverter {
        public static final boolean DEFAULT_READ_PIXEL_VALUES_VIA_COLOR_MODEL = false;
        public static final boolean DEFAULT_READ_PIXEL_VALUES_VIA_GRAPHICS_2D = false;

        private boolean readPixelValuesViaColorModel = DEFAULT_READ_PIXEL_VALUES_VIA_COLOR_MODEL;
        private boolean readPixelValuesViaGraphics2D = DEFAULT_READ_PIXEL_VALUES_VIA_GRAPHICS_2D;

        public ToPacked3D(boolean addAlphaWhenExist) {
            super(addAlphaWhenExist);
        }

        public boolean isReadPixelValuesViaColorModel() {
            return readPixelValuesViaColorModel;
        }

        public ToPacked3D setReadPixelValuesViaColorModel(boolean readPixelValuesViaColorModel) {
            this.readPixelValuesViaColorModel = readPixelValuesViaColorModel;
            return this;
        }

        public boolean isReadPixelValuesViaGraphics2D() {
            return readPixelValuesViaGraphics2D;
        }

        public ToPacked3D setReadPixelValuesViaGraphics2D(boolean readPixelValuesViaGraphics2D) {
            this.readPixelValuesViaGraphics2D = readPixelValuesViaGraphics2D;
            return this;
        }

        @Override
        public Class<?> getResultElementType(BufferedImage bufferedImage) {
            if (bufferedImage == null)
                throw new NullPointerException("Null bufferedImage");
            if (readPixelValuesViaColorModel || readPixelValuesViaGraphics2D) {
                return byte.class;
            }
            Class<?> result = getResultElementTypeOrNullForUnsupported(bufferedImage);
            return result == null ? byte.class : result;
        }

        @Override
        public long[] getResultMatrixDimensions(int width, int height, int bandCount) {
            return new long[] {bandCount, width, height};
        }

        @Override
        protected void toJavaArray(BufferedImage bufferedImage, Object resultJavaArray) {
            final int dimX = bufferedImage.getWidth();
            final int dimY = bufferedImage.getHeight();
            final int bandCount = getBandCount(bufferedImage);
            assert bandCount <= 4;
            assert java.lang.reflect.Array.getLength(resultJavaArray) >= dimX * dimY * bandCount;
            final ColorModel colorModel = bufferedImage.getColorModel();
            final SampleModel sampleModel = bufferedImage.getSampleModel();
            final boolean gray = bandCount == 1;
            if (readPixelValuesViaColorModel) {
                assert resultJavaArray instanceof byte[];
                byte[] result = (byte[]) resultJavaArray;
                Raster r = bufferedImage.getRaster();
                Object outData = null;
                switch (bandCount) {
                    case 4:
                        for (int y = 0, disp = 0; y < dimY; y++) {
                            for (int x = 0; x < dimX; x++) {
                                outData = r.getDataElements(x, y, outData);
                                result[disp++] = (byte) colorModel.getRed(outData);
                                result[disp++] = (byte) colorModel.getGreen(outData);
                                result[disp++] = (byte) colorModel.getBlue(outData);
                                result[disp++] = (byte) colorModel.getAlpha(outData);
                            }
                        }
                        break;
                    case 3:
                        for (int y = 0, disp = 0; y < dimY; y++) {
                            for (int x = 0; x < dimX; x++) {
                                outData = r.getDataElements(x, y, outData);
                                result[disp++] = (byte) colorModel.getRed(outData);
                                result[disp++] = (byte) colorModel.getGreen(outData);
                                result[disp++] = (byte) colorModel.getBlue(outData);
                            }
                        }
                        break;
                    case 1:
                        for (int y = 0, disp = 0; y < dimY; y++) {
                            for (int x = 0; x < dimX; x++) {
                                outData = r.getDataElements(x, y, outData);
                                result[disp++] = (byte) colorModel.getGreen(outData);
                            }
                        }
                        break;
                    default:
                        throw new AssertionError("Illegal bandCount = " + bandCount);
                }
                return;
            }
            if (!readPixelValuesViaGraphics2D && supportedStructure(bufferedImage)) {
                // Default branch, used by this class without special settings.
                // But in a case of strange colorModel.getNumComponents() (like 2 or 1 with alpha),
                // or incompatibility of color components count and samples count (RGB, but indexed 1-band data),
                // or unsupported element type,
                // we don't use it and prefer more stable "simplest" algorithm below.
                final Raster r = bufferedImage.getRaster();
                final int dataBufferType = sampleModel.getDataType();
                final int[] buffer = dimX <= INT_ARRAY_POOL.arrayLength() ?
                    (int[]) INT_ARRAY_POOL.requestArray() :
                    new int[dimX];
                final int lineCount = buffer.length / dimX;
                assert lineCount >= 1;
                int numberOfPixels;
                for (int y = 0, disp = 0; y < dimY; y += lineCount, disp += numberOfPixels * bandCount) {
                    int m = Math.min(lineCount, dimY - y);
                    numberOfPixels = m * dimX;
                    for (int bandIndex = 0; bandIndex < bandCount; bandIndex++) {
                        r.getSamples(0, y, dimX, m, bandIndex, buffer);
                        switch (dataBufferType) {
                            case DataBuffer.TYPE_BYTE: {
                                assert resultJavaArray instanceof byte[];
                                byte[] result = (byte[]) resultJavaArray;
                                for (int i = 0, j = disp + bandIndex; i < numberOfPixels; i++, j += bandCount) {
                                    result[j] = (byte) (buffer[i] & 0xFF);
                                }
                                break;
                            }
                            case DataBuffer.TYPE_USHORT: {
                                assert resultJavaArray instanceof short[];
                                short[] result = (short[]) resultJavaArray;
                                for (int i = 0, j = disp + bandIndex; i < numberOfPixels; i++, j += bandCount) {
                                    result[j] = (short) (buffer[i] & 0xFFFF);
                                }
                                break;
                            }
                            case DataBuffer.TYPE_INT: {
                                assert resultJavaArray instanceof int[];
                                int[] result = (int[]) resultJavaArray;
                                for (int i = 0, j = disp + bandIndex; i < numberOfPixels; i++, j += bandCount) {
                                    result[j] = buffer[i];
                                }
                                break;
                            }
                            default:
                                throw new AssertionError("Unsupported type: illegal subclass implementation");
                        }
                    }
                }
                if (dimX <= INT_ARRAY_POOL.arrayLength()) {
                    INT_ARRAY_POOL.releaseArray(buffer);
                }
                //                System.out.println("Quick loading");
                return;
            }
            // Simplest algorithm: via BufferedImage.getGraphics
            assert resultJavaArray instanceof byte[];
            //TODO!! support in future ushort/int via getResultElementType
            //TODO!! and what if bandCount=3 and colorComponentsCount=4?
            final byte[] result = (byte[]) resultJavaArray;
            final boolean banded = USE_3_BANDS_FOR_NON_BANDED_GRAY ?
                bufferedImage.getSampleModel() instanceof BandedSampleModel :
                true;
            final byte[][] rgbAlpha = new byte[gray && !banded ? 3 : 1][];
            // even if gray, but not banded, we make full RGB banded image:
            // if no, ColorSpace.CS_GRAY produces invalid values (too dark)
            rgbAlpha[0] = result;
            for (int k = 1; k < rgbAlpha.length; k++) {
                rgbAlpha[k] = new byte[result.length]; // avoiding invalid values by creating extra bands
            }
            final ColorSpace cs = ColorSpace.getInstance(gray && banded ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
            final DataBufferByte db = new DataBufferByte(rgbAlpha, rgbAlpha[0].length);
            final WritableRaster wr;
            final ColorModel cm;
            if (gray) {
                int[] indexes = new int[rgbAlpha.length];
                int[] offsets = new int[rgbAlpha.length]; // zero-filled by Java
                for (int k = 0; k < indexes.length; k++) {
                    indexes[k] = k;
                    offsets[k] = 0;
                }
                wr = Raster.createBandedRaster(db, dimX, dimY, dimX, indexes, offsets, null);
                cm = new ComponentColorModel(cs, null, colorModel.hasAlpha(), false,
                    ColorModel.OPAQUE, db.getDataType());
            } else {
                int[] offsets = new int[bandCount];
                for (int k = 0; k < offsets.length; k++) {
                    offsets[k] = k;
                }
                wr = Raster.createInterleavedRaster(db, dimX, dimY, dimX * bandCount, bandCount, offsets, null);
                boolean hasAlpha = bandCount >= 4;
                cm = new ComponentColorModel(cs, null, hasAlpha, false,
                    hasAlpha ? ColorModel.TRANSLUCENT : ColorModel.OPAQUE, db.getDataType());
            }
            final BufferedImage resultImage = new BufferedImage(cm, wr, false, null);
            resultImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
        }

        private boolean supportedStructure(BufferedImage bufferedImage) {
            return getResultElementTypeOrNullForUnsupported(bufferedImage) != null;
        }

        private Class<?> getResultElementTypeOrNullForUnsupported(BufferedImage bufferedImage) {
            final int bandCount = getBandCount(bufferedImage);
            final ColorModel colorModel = bufferedImage.getColorModel();
            final SampleModel sampleModel = bufferedImage.getSampleModel();
            final int colorComponentsCount = colorModel.getNumComponents();
            if (bandCount <= colorComponentsCount // in particular, when bandCount=3 and colorComponentsCount=4
                && colorComponentsCount == sampleModel.getNumBands()
                && colorModel instanceof ComponentColorModel
                && sampleModel instanceof ComponentSampleModel)
            {
                switch (sampleModel.getDataType()) {
                    case DataBuffer.TYPE_BYTE :
                    case DataBuffer.TYPE_USHORT :
                    case DataBuffer.TYPE_INT :
                        return getResultElementType(sampleModel);
                }
            }
            return null;
        }

        private static Class<?> getResultElementType(SampleModel sampleModel) {
            switch (sampleModel.getDataType()) {
                case DataBuffer.TYPE_BYTE :
                    return byte.class;
                case DataBuffer.TYPE_SHORT :
                case DataBuffer.TYPE_USHORT :
                    return short.class;
                case DataBuffer.TYPE_INT :
                    return int.class;
                case DataBuffer.TYPE_FLOAT :
                    return float.class;
                case DataBuffer.TYPE_DOUBLE :
                    return double.class;
                default:
                    return byte.class;
            }
        }
    }
}
