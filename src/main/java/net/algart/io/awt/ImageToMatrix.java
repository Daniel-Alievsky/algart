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

package net.algart.io.awt;

import net.algart.arrays.*;

import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.*;
import java.util.List;
import java.util.Objects;

/**
 * Converter from {@link BufferedImage} into AlgART 3D interleaved matrices.
 *
 * @author Daniel Alievsky
 */
public abstract class ImageToMatrix {
    private boolean enableAlpha = true;
    // if true and if BufferedImage contains an alpha-channel, the matrix 4xMxN will be returned
    // if false, but the source has alpha, it may be interpreted, not ignored

    /**
     * Converts <code>BufferedImage</code> to multichannel AlgART image (list of matrices).
     * Equivalent to
     * <pre>
     *     {@link Matrices#separate Matrices.separate}({@link #toInterleavedRGB
     *     toInterleavedMatrix}(bufferedImage))
     * </pre>
     *
     * @param bufferedImage some buffered image.
     * @return the list of color channels if this image.
     */
    public static List<Matrix<UpdatablePArray>> toChannels(BufferedImage bufferedImage) {
        return Matrices.separate(toInterleavedRGB(bufferedImage));
    }

    /**
     * Converts <code>BufferedImage</code> to interleaved RGB AlgART matrix.
     * Equivalent to
     * <pre>
     *     new {@link ToInterleavedRGB
     *     ImageToMatrix.ToInterleavedRGB}().{@link #toMatrix toMatrix}(bufferedImage)
     * </pre>
     *
     * @param bufferedImage some buffered image.
     * @return the interleaved RGB matrix.
     */
    public static Matrix<UpdatablePArray> toInterleavedRGB(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage, "Null bufferedImage");
        return new ToInterleavedRGB().toMatrix(bufferedImage);
    }

    /**
     * Converts <code>BufferedImage</code> to interleaved BGR AlgART matrix.
     * Equivalent to
     * <pre>
     *     new {@link ToInterleavedRGB
     *     ImageToMatrix.ToInterleavedBGR}().{@link #toMatrix toMatrix}(bufferedImage)
     * </pre>
     *
     * @param bufferedImage some buffered image.
     * @return the interleaved BGR matrix.
     */
    public static Matrix<UpdatablePArray> toInterleavedBGR(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage, "Null bufferedImage");
        return new ToInterleavedBGR().toMatrix(bufferedImage);
    }

    public static int defaultNumberOfChannels(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage, "Null bufferedImage");
        ColorModel cm = bufferedImage.getColorModel();
        boolean gray = cm.getNumComponents() == 1;
        // ...SampleModel sm = bufferedImage.getSampleModel();...
        // || (sm.getNumBands() == 1 && sm.getNumDataElements() == 1) - not correct!
        // In some TIFF we have only 1 band, but RGB color model, because it uses palette; so, the result must be 3.
        return cm.hasAlpha() ? 4 : gray ? 1 : 3;
    }

    public static Class<?> defaultElementType(BufferedImage bufferedImage) {
        final Class<?> result = resultElementTypeOrNullForUnsupported(bufferedImage);
        return result == null ? byte.class : result;
    }

    public boolean isEnableAlpha() {
        return enableAlpha;
    }

    public ImageToMatrix setEnableAlpha(boolean enableAlpha) {
        this.enableAlpha = enableAlpha;
        return this;
    }

    public Matrix<UpdatablePArray> toMatrix(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage, "Null bufferedImage");
        final int dimX = bufferedImage.getWidth();
        final int dimY = bufferedImage.getHeight();
        final int bandCount = resultNumberOfChannels(bufferedImage);
        final long[] dimensions = resultMatrixDimensions(dimX, dimY, bandCount);
        final Class<?> elementType = resultElementType(bufferedImage);
        final long size = Arrays.longMul(dimensions);
        if (size > Integer.MAX_VALUE || size == Long.MIN_VALUE) {
            throw new AssertionError("Illegal getResultMatrixDimensions implementation: too large results");
        }
        Object resultData = java.lang.reflect.Array.newInstance(elementType, (int) size);
        toJavaArray(resultData, bufferedImage);
        return SimpleMemoryModel.asMatrix(resultData, dimensions);
    }

    public final int resultNumberOfChannels(BufferedImage bufferedImage) {
        final int result = defaultNumberOfChannels(bufferedImage);
        return !enableAlpha && result > 3 ? 3 : result;
    }

    /**
     * Returns the color channels order, in which they are written in the result color matrix.
     * The result is {@link ColorChannelOrder#RGB} in {@link ToInterleavedRGB} class,
     * and {@link ColorChannelOrder#BGR} in {@link ToInterleavedBGR} class.
     *
     * @return channel order: RGB or BGR.
     */
    public abstract ColorChannelOrder channelOrder();

    // must be primitive and not boolean
    public abstract Class<?> resultElementType(BufferedImage bufferedImage);

    public final long[] resultMatrixDimensions(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage, "Null bufferedImage");
        return resultMatrixDimensions(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                resultNumberOfChannels(bufferedImage));
    }

    public abstract long[] resultMatrixDimensions(int width, int height, int bandCount);

    public static Class<?> tryToDetectElementType(SampleModel sampleModel) {
        return switch (sampleModel.getDataType()) {
            case DataBuffer.TYPE_BYTE -> byte.class;
            case DataBuffer.TYPE_SHORT, DataBuffer.TYPE_USHORT -> short.class;
            case DataBuffer.TYPE_INT -> int.class;
            case DataBuffer.TYPE_FLOAT -> float.class;
            case DataBuffer.TYPE_DOUBLE -> double.class;
            default -> null;
        };
    }

    protected abstract void toJavaArray(Object resultJavaArray, BufferedImage bufferedImage);

    private static Class<?> resultElementTypeOrNullForUnsupported(BufferedImage bufferedImage) {
        final int numberOfChannels = defaultNumberOfChannels(bufferedImage);
        final ColorModel colorModel = bufferedImage.getColorModel();
        final SampleModel sampleModel = bufferedImage.getSampleModel();
        final int colorComponentsCount = colorModel.getNumComponents();
        if (numberOfChannels > colorComponentsCount || colorComponentsCount != sampleModel.getNumBands()) {
            return null;
            // - excluding images with palette (defaultNumberOfChannels will be 3, but only 1 band in getNumBands())
            // but numberOfChannels=3 and colorComponentsCount=4 case is allowed
        }
        if (!(colorModel instanceof ComponentColorModel && sampleModel instanceof ComponentSampleModel)) {
            // - in this case, for example, for the result of
            //      new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB)
            // or
            //      new BufferedImage(1000, 1000, BufferedImage.TYPE_USHORT_565_RGB)
            // there is no simple way to detect a correct element type:
            // for example, DataBuffer.TYPE_INT can really mean packed 8-bit RGB values
            return null;
        }
        return switch (sampleModel.getDataType()) {
            case DataBuffer.TYPE_BYTE,
                 DataBuffer.TYPE_USHORT,
                 DataBuffer.TYPE_INT,
                 DataBuffer.TYPE_FLOAT,
                 DataBuffer.TYPE_DOUBLE -> tryToDetectElementType(sampleModel);
            default -> null;
        };
    }

    public static class ToInterleavedRGB extends ImageToMatrix {
        public static final boolean DEFAULT_READING_VIA_COLOR_MODEL = false;
        public static final boolean DEFAULT_READING_VIA_GRAPHICS = false;

        private final boolean bgrOrder;

        private boolean readingViaColorModel = DEFAULT_READING_VIA_COLOR_MODEL;
        private boolean readingViaGraphics = DEFAULT_READING_VIA_GRAPHICS;

        public ToInterleavedRGB() {
            this(false);
        }

        ToInterleavedRGB(boolean bgrOrder) {
            this.bgrOrder = bgrOrder;
        }

        public boolean isReadingViaColorModel() {
            return readingViaColorModel;
        }

        /**
         * Sets the alternative way for reading pixels: using <code>ColorModel.getRed, getGreen, getBlue</code>
         * methods.
         * Usually should be <code>false</code> (default value).
         *
         * @param readingViaColorModel whether the sample values should
         *                             be read via <code>ColorModel</code> methods.
         * @return a reference to this object.
         */
        public ToInterleavedRGB setReadingViaColorModel(boolean readingViaColorModel) {
            this.readingViaColorModel = readingViaColorModel;
            return this;
        }

        public boolean isReadingViaGraphics() {
            return readingViaGraphics;
        }

        /**
         * Sets the alternative way for reading pixels: drawing the source image on a <code>Graphics2D</code> object
         * returned by <code>getGraphics()</code> method of <code>BufferedImage</code> created on the base
         * of an underlying Java array with the resulting pixel samples.
         * Usually should be <code>false</code> (default value).
         *
         * <p>Note that this mode is enabled automatically for unusual images,
         * in particular for images with palette or for non-standard precisions
         * such as {@link BufferedImage#TYPE_USHORT_565_RGB}.
         *
         * <p>Note that {@link #setReadingViaColorModel(boolean)} method has a higher priority:
         * if reading via a color model is selected, the mode set by this method is ignored.
         *
         * @param readingViaGraphics whether the sample values should
         *                           be read via drawing on <code>Graphics</code>.
         * @return a reference to this object.
         */
        public ToInterleavedRGB setReadingViaGraphics(boolean readingViaGraphics) {
            this.readingViaGraphics = readingViaGraphics;
            return this;
        }

        @Override
        public ColorChannelOrder channelOrder() {
            return bgrOrder ? ColorChannelOrder.BGR : ColorChannelOrder.RGB;
        }

        @Override
        public Class<?> resultElementType(BufferedImage bufferedImage) {
            Objects.requireNonNull(bufferedImage, "Null bufferedImage");
            if (readingViaColorModel || readingViaGraphics) {
                return byte.class;
            }
            return defaultElementType(bufferedImage);
        }

        @Override
        public long[] resultMatrixDimensions(int width, int height, int bandCount) {
            return new long[]{bandCount, width, height};
        }

        @Override
        public String toString() {
            return "ToInterleaved" + (bgrOrder ? "BGR" : "RGB") +
                    (readingViaColorModel ? " (reading via color model)" :
                            readingViaGraphics ? " (reading via graphics)" : "");

        }

        @Override
        protected void toJavaArray(Object resultJavaArray, BufferedImage bufferedImage) {
            Objects.requireNonNull(resultJavaArray, "Null resultJavaArray");
            Objects.requireNonNull(bufferedImage, "Null bufferedImage");
            if (!resultJavaArray.getClass().isArray()) {
                throw new IllegalArgumentException("resultJavaArray is not an array: " + resultJavaArray.getClass());
            }
            final int dimX = bufferedImage.getWidth();
            final int dimY = bufferedImage.getHeight();
            final int bandCount = resultNumberOfChannels(bufferedImage);
            assert bandCount <= 4;
            if (java.lang.reflect.Array.getLength(resultJavaArray) < dimX * dimY * bandCount) {
                throw new IllegalArgumentException("resultJavaArray too small for " +
                        dimX + "x" + dimY + "x" + bandCount);
            }
            final boolean invertBandOrder = bgrOrder && (bandCount == 3 || bandCount == 4);
            if (readingViaColorModel) {
                toJavaArrayViaColorModel(resultJavaArray, bufferedImage);
                return;
            }
            if (readingViaGraphics || !isSupportedStructure(bufferedImage)) {
                toJavaArrayViaGraphics(resultJavaArray, bufferedImage);
                return;
            }
            // Default branch, used by this class without special settings.
            // But in the case of strange colorModel.getNumComponents() (like 2 or 1 with alpha),
            // or incompatibility of color components count and samples count (RGB, but indexed 1-band data),
            // or unsupported element type,
            // we don't use it and prefer more stable "simplest" algorithm below.
            final Raster r = bufferedImage.getRaster();
            final int dataBufferType = r.getSampleModel().getDataType();
            int[] iBuffer = null;
            float[] fBuffer = null;
            switch (dataBufferType) {
                case DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, DataBuffer.TYPE_INT -> iBuffer = new int[dimX];
                case DataBuffer.TYPE_FLOAT, DataBuffer.TYPE_DOUBLE -> fBuffer = new float[dimX];
            }
            for (int y = 0, disp = 0; y < dimY; y++, disp += dimX * bandCount) {
                for (int bandIndex = 0; bandIndex < bandCount; bandIndex++) {
                    final int correctedBandIndex = invertBandOrder && bandIndex < 3 ? 2 - bandIndex : bandIndex;
                    switch (dataBufferType) {
                        case DataBuffer.TYPE_BYTE -> {
                            if (!(resultJavaArray instanceof byte[] result)) {
                                throw new IllegalArgumentException("resultJavaArray must be byte[], but it is " +
                                        resultJavaArray.getClass().getSimpleName());
                            }
                            r.getSamples(0, y, dimX, 1, correctedBandIndex, iBuffer);
                            for (int i = 0, j = disp + bandIndex; i < dimX; i++, j += bandCount) {
                                result[j] = (byte) (iBuffer[i] & 0xFF);
                            }
                        }
                        case DataBuffer.TYPE_USHORT -> {
                            if (!(resultJavaArray instanceof short[] result)) {
                                throw new IllegalArgumentException("resultJavaArray must be short[], but it is " +
                                        resultJavaArray.getClass().getSimpleName());
                            }
                            r.getSamples(0, y, dimX, 1, correctedBandIndex, iBuffer);
                            for (int i = 0, j = disp + bandIndex; i < dimX; i++, j += bandCount) {
                                result[j] = (short) (iBuffer[i] & 0xFFFF);
                            }
                        }
                        case DataBuffer.TYPE_INT -> {
                            if (!(resultJavaArray instanceof int[] result)) {
                                throw new IllegalArgumentException("resultJavaArray must be int[], but it is " +
                                        resultJavaArray.getClass().getSimpleName());
                            }
                            r.getSamples(0, y, dimX, 1, correctedBandIndex, iBuffer);
                            for (int i = 0, j = disp + bandIndex; i < dimX; i++, j += bandCount) {
                                result[j] = iBuffer[i];
                            }
                        }
                        case DataBuffer.TYPE_FLOAT -> {
                            if (!(resultJavaArray instanceof float[] result)) {
                                throw new IllegalArgumentException("resultJavaArray must be float[], but it is " +
                                        resultJavaArray.getClass().getSimpleName());
                            }
                            r.getSamples(0, y, dimX, 1, correctedBandIndex, fBuffer);
                            for (int i = 0, j = disp + bandIndex; i < dimX; i++, j += bandCount) {
                                result[j] = fBuffer[i];
                            }
                        }
                        case DataBuffer.TYPE_DOUBLE -> {
                            if (!(resultJavaArray instanceof double[] result)) {
                                throw new IllegalArgumentException("resultJavaArray must be double[], but it is " +
                                        resultJavaArray.getClass().getSimpleName());
                            }
                            r.getSamples(0, y, dimX, 1, correctedBandIndex, fBuffer);
                            for (int i = 0, j = disp + bandIndex; i < dimX; i++, j += bandCount) {
                                result[j] = fBuffer[i];
                            }
                        }
                        default -> throw new AssertionError(
                                "Unsupported data buffer type: " + dataBufferType);
                        // can occur only in incorrect subclasses: it is checked in isSupportedStructure()
                    }
                }
            }

        }

        private void toJavaArrayViaGraphics(Object resultJavaArray, BufferedImage bufferedImage) {
            // Simplest algorithm: via BufferedImage.getGraphics
            // Note: sometimes, due to some internal optimizations, this branch works even faster
            // than the previous one. But usually, the previous branch works in several times faster.
            if (!(resultJavaArray instanceof byte[] result)) {
                // - also checked in getResultElementType
                throw new IllegalArgumentException("resultJavaArray must be byte[] for conversion via Graphics");
            }
            final int dimX = bufferedImage.getWidth();
            final int dimY = bufferedImage.getHeight();
            final int bandCount = resultNumberOfChannels(bufferedImage);
            final boolean gray = bandCount == 1;
            final boolean invertBandOrder = bgrOrder && (bandCount == 3 || bandCount == 4);
            final boolean banded = bufferedImage.getSampleModel() instanceof BandedSampleModel;
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
                int[] offsets = new int[rgbAlpha.length];
                // zero-filled by Java
                for (int k = 0; k < indexes.length; k++) {
                    indexes[k] = k;
                    offsets[k] = 0;
                }
                wr = Raster.createBandedRaster(db, dimX, dimY, dimX, indexes, offsets, null);
                cm = new ComponentColorModel(cs, null,
                        bufferedImage.getColorModel().hasAlpha(), false,
                        ColorModel.OPAQUE, db.getDataType());
            } else {
                int[] offsets = new int[bandCount];
                for (int k = 0; k < offsets.length; k++) {
                    if (invertBandOrder) {
                        offsets[k] = k == 0 ? 2 : k == 2 ? 0 : k;
                    } else {
                        offsets[k] = k;
                    }
                }
                wr = Raster.createInterleavedRaster(
                        db, dimX, dimY, dimX * bandCount, bandCount, offsets, null);
                boolean hasAlpha = bandCount >= 4;
                cm = new ComponentColorModel(cs, null, hasAlpha, false,
                        hasAlpha ? ColorModel.TRANSLUCENT : ColorModel.OPAQUE, db.getDataType());
            }
            final BufferedImage resultImage = new BufferedImage(cm, wr, false, null);
            resultImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
        }

        private void toJavaArrayViaColorModel(Object resultJavaArray, BufferedImage bufferedImage) {
            if (!(resultJavaArray instanceof byte[] result)) {
                // - also checked in getResultElementType
                throw new IllegalArgumentException("resultJavaArray must be byte[] for conversion via color model");
            }
            final int dimX = bufferedImage.getWidth();
            final int dimY = bufferedImage.getHeight();
            final int bandCount = resultNumberOfChannels(bufferedImage);
            final ColorModel colorModel = bufferedImage.getColorModel();
            final Raster r = bufferedImage.getRaster();
            Object outData = null;
            final int rIndex = bgrOrder ? 2 : 0;
            final int gIndex = 1;
            final int bIndex = bgrOrder ? 0 : 2;
            switch (bandCount) {
                case 4 -> {
                    for (int y = 0, disp = 0; y < dimY; y++) {
                        for (int x = 0; x < dimX; x++) {
                            outData = r.getDataElements(x, y, outData);
                            result[disp + rIndex] = (byte) colorModel.getRed(outData);
                            result[disp + gIndex] = (byte) colorModel.getGreen(outData);
                            result[disp + bIndex] = (byte) colorModel.getBlue(outData);
                            result[disp + 3] = (byte) colorModel.getAlpha(outData);
                            disp += 4;
                        }
                    }
                }
                case 3 -> {
                    for (int y = 0, disp = 0; y < dimY; y++) {
                        for (int x = 0; x < dimX; x++) {
                            outData = r.getDataElements(x, y, outData);
                            result[disp + rIndex] = (byte) colorModel.getRed(outData);
                            result[disp + gIndex] = (byte) colorModel.getGreen(outData);
                            result[disp + bIndex] = (byte) colorModel.getBlue(outData);
                            disp += 3;
                        }
                    }
                }
                case 1 -> {
                    for (int y = 0, disp = 0; y < dimY; y++) {
                        for (int x = 0; x < dimX; x++) {
                            outData = r.getDataElements(x, y, outData);
                            result[disp++] = (byte) colorModel.getGreen(outData);
                            // - note: little other results for grayscale image
                        }
                    }
                }
                default -> throw new AssertionError("Illegal bandCount = " + bandCount);
            }
        }

        private boolean isSupportedStructure(BufferedImage bufferedImage) {
            return resultElementTypeOrNullForUnsupported(bufferedImage) != null;
        }
    }

    public static final class ToInterleavedBGR extends ToInterleavedRGB {
        public ToInterleavedBGR() {
            super(true);
        }
    }
}
