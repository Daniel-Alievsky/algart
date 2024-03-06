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

package net.algart.external.awt;

import net.algart.arrays.*;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Locale;
import java.util.Objects;

public abstract class MatrixToBufferedImageConverter {
    /**
     * Equivalent to <tt>{@link #toBufferedImage(net.algart.arrays.Matrix, java.awt.image.DataBuffer)
     * toBufferedImage}(packedMatrix, null)</tt>.
     *
     * @param packedMatrix the packed matrix.
     * @return the <tt>BufferedImage</tt> with the same data.
     */
    public final BufferedImage toBufferedImage(Matrix<? extends PArray> packedMatrix) {
        return toBufferedImage(packedMatrix, null);
    }

    /**
     * Converts the given packed matrix (2- or 3-dimensional) into <tt>BufferedImage</tt>.
     * Note: <tt>dataBuffer!=null</tt>, then the elements of the given matrix ignored, but the data
     * of the given buffer are used instead. (It is supposed, that this buffer was created
     * by {@link #toDataBuffer(net.algart.arrays.Matrix)} method, maybe with some post-processing.)
     *
     * @param packedMatrix the packed matrix.
     * @param dataBuffer   the data for <tt>BufferedImage</tt>; may be <tt>null</tt>, then it is automatically
     *                     created as {@link #toDataBuffer(net.algart.arrays.Matrix)
     *                     toDataBuffer(packedMatrix)}.
     * @return the <tt>BufferedImage</tt> with the same data.
     */
    public final BufferedImage toBufferedImage(
            Matrix<? extends PArray> packedMatrix,
            java.awt.image.DataBuffer dataBuffer) {
        checkMatrix(packedMatrix);
        if (dataBuffer == null) {
            dataBuffer = toDataBuffer(packedMatrix);
        }
        final int dimX = getWidth(packedMatrix);
        final int dimY = getHeight(packedMatrix);
        final int bandCount = getBandCount(packedMatrix);
        int[] bandMasks = rgbAlphaMasks(bandCount);
        if (bandMasks != null) {
            WritableRaster wr = Raster.createPackedRaster(dataBuffer, dimX, dimY, dimX, bandMasks, null);
            DirectColorModel cm = bandMasks.length > 3 ?
                    new DirectColorModel(32, bandMasks[0], bandMasks[1], bandMasks[2], bandMasks[3]) :
                    new DirectColorModel(24, bandMasks[0], bandMasks[1], bandMasks[2], 0);
            return new BufferedImage(cm, wr, false, null);
        }
        byte[][] palette = palette();
        if (palette != null) {
            if (palette.length < 3)
                throw new AssertionError("palette() method must return palette with 3 or 4 bands");
            IndexColorModel cm = palette.length == 3 ?
                    new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2]) :
                    new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2], palette[3]);
            WritableRaster wr = Raster.createInterleavedRaster(
                    dataBuffer, dimX, dimY, dimX, 1, new int[]{0}, null);
            return new BufferedImage(cm, wr, false, null);
        }
        int[] indexes = new int[dataBuffer.getNumBanks()];
        int[] offsets = new int[dataBuffer.getNumBanks()];
        for (int k = 0; k < indexes.length; k++) {
            indexes[k] = k;
            offsets[k] = 0;
        }
        WritableRaster wr = indexes.length == 1 ?
                Raster.createInterleavedRaster(dataBuffer, dimX, dimY, dimX, 1, new int[]{0}, null) :
                // - important! in other case,
                // createGraphics().drawXxx method will use incorrect (translated) intensity
                Raster.createBandedRaster(dataBuffer, dimX, dimY, dimX, indexes, offsets, null);
        ColorSpace cs = ColorSpace.getInstance(indexes.length == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        boolean hasAlpha = indexes.length > 3;
        ComponentColorModel cm = new ComponentColorModel(cs, null,
                hasAlpha, false, hasAlpha ? ColorModel.TRANSLUCENT : ColorModel.OPAQUE, dataBuffer.getDataType());
        return new BufferedImage(cm, wr, false, null);
    }

    /**
     * Returns the x-dimension of the image, corresponding to the given packed matrix.
     * Note that it is <tt>int</tt>, not <tt>long</tt> (AWT images have 31-bit dimensions).
     *
     * @param packedMatrix the packed matrix.
     * @return the width of the corresponding image.
     */
    public int getWidth(Matrix<? extends PArray> packedMatrix) {
        checkMatrix(packedMatrix);
        return (int) packedMatrix.dim(packedMatrix.dimCount() - 2);
    }

    /**
     * Returns the y-dimension of the image, corresponding to the given packed matrix.
     * Note that it is <tt>int</tt>, not <tt>long</tt> (AWT images have 31-bit dimensions).
     *
     * @param packedMatrix the packed matrix.
     * @return the height of the corresponding image.
     */
    public int getHeight(Matrix<? extends PArray> packedMatrix) {
        checkMatrix(packedMatrix);
        return (int) packedMatrix.dim(packedMatrix.dimCount() - 1);
    }

    /**
     * Returns the number of color bands of the image, corresponding to the given packed matrix.
     * For example, it is 3 for RGB image or 4 for RGB-Alpha.
     *
     * @param packedMatrix the packed matrix.
     * @return the corresponding number of color bands.
     */
    public int getBandCount(Matrix<? extends PArray> packedMatrix) {
        checkMatrix(packedMatrix);
        return packedMatrix.dimCount() == 2 ? 1 : (int) packedMatrix.dim(0);
    }

    /**
     * Converts the given packed matrix (2- or 3-dimensional) into <tt>java.awt.image.DataBuffer</tt>.
     * This method is useful in addition to {@link #toBufferedImage(Matrix, java.awt.image.DataBuffer)},
     * if you want to do something with the created DataBuffer, for example, to correct some its pixels.
     *
     * @param packedMatrix the packed data.
     * @return the newly allocated <tt>DataBuffer</tt> with the same data.
     */
    public final java.awt.image.DataBuffer toDataBuffer(Matrix<? extends PArray> packedMatrix) {
        checkMatrix(packedMatrix);
        long bandCount = packedMatrix.dimCount() == 2 ? 1 : packedMatrix.dim(0);
        PArray array = packedMatrix.array();
        if (!SimpleMemoryModel.isSimpleArray(array)) {
            array = array.updatableClone(Arrays.SMM);
        }
        return toDataBuffer(array, (int) bandCount);
    }

    /**
     * Converts the color into a form, stored in the data buffer, returned by {@link #toDataBuffer(Matrix)}.
     *
     * <p>The default implementation is suitable for monochrome, indexed and multi-bank data buffers.
     *
     * <p>Note: if the packed matrix is monochrome or indexed, i.e.
     * <tt>{@link #getBandCount(Matrix) getBandCount}(packedMatrix)==1</tt>,
     * this method returns
     * <pre>
     * Math.round(0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue())
     * </pre>
     * <p>The good idea is to provide identical R, G, B color components in such cases
     * (if this method is not overridden).
     *
     * @param packedMatrix the packed data.
     * @param color        some color.
     * @param bankIndex    index of the bank in terms of <tt>java.awt.image.DataBuffer</tt>.
     * @return the corresponded component of this color or packed RGB-Alpha value,
     * depending on the structure of the data buffer.
     */
    public long colorValue(Matrix<? extends PArray> packedMatrix, java.awt.Color color, int bankIndex) {
        final int bandCount = getBandCount(packedMatrix);
        if (bandCount == 1) {
            return Math.round(0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
        }
        if (color == null)
            throw new NullPointerException("Null color argument");
        return switch (bankIndex) {
            case 0 -> color.getRed();
            case 1 -> color.getGreen();
            case 2 -> color.getBlue();
            case 3 -> color.getAlpha();
            default -> 0;
        };
    }

    /**
     * Returns <tt>true</tt> if the AlgART array or matrix, passed to the methods of this class,
     * must contain <tt>byte</tt> elements. In this case, the client of this class must convert
     * all other element types into <tt>byte</tt> by some way, usually via
     * {@link Matrices#asFuncMatrix(net.algart.math.functions.Func, Class, Matrix)} method.
     *
     * <p>The default implementation returns <tt>true</tt>.
     * Please override this method if your implementation forms specific versions of
     * <tt>java.awt.image.DataBuffer</tt> for non-byte element types.
     */
    public boolean byteArrayRequired() {
        return true;
    }

    /**
     * Returns the palette (<tt></tt>byte[4][256]</tt>) if the indexed image is supposed.
     *
     * <p>The default implementation returns <tt>null</tt>, that means non-indexed image.
     *
     * @return the palette or <tt>null</tt> if the image should be not indexed.
     */
    public byte[][] palette() {
        return null;
    }

    /**
     * Actual method, on which {@link #toDataBuffer(Matrix)} is based.
     *
     * <p>The passed AlgART array must be {@link DirectAccessible direct accessible}.
     *
     * @param packedArray the packed data.
     * @param bandCount   the number of bands: if called from {@link #toDataBuffer(Matrix)},
     *                    it is 1 for 2-dimensional matrix and {@link Matrix#dim(int) dim(0)}
     *                    for 3-dimensional matrix.
     * @return the newly allocated <tt>DataBuffer</tt> with the same data.
     */
    protected abstract java.awt.image.DataBuffer toDataBuffer(PArray packedArray, int bandCount);

    /**
     * Returns the band masks, which will be passed to <tt>Raster.createPackedRaster</tt> method,
     * if you want to convert data into a packed <tt>BufferedImage</tt>.
     * The resulting array may be <tt>null</tt>, that means an unpacked form of the raster
     * (<tt>Raster.createBandedRaster</tt>), or an array containing <tt>bandCount</tt> elements:
     * red, green, blue and (if necessary) alpha masks.
     *
     * @param bandCount the number of masks (3 or 4, in other cases <tt>null</tt> is returned).
     * @return the bit masks for storing bands in the packed <tt>int</tt> values.
     */

    protected int[] rgbAlphaMasks(int bandCount) {
        return null;
    }

    /**
     * Equivalent to <tt>dataBuffer.getData(bankIndex)</tt> for the corresponding specific element type.
     * It can be used, for example, together with {@link SimpleMemoryModel#asUpdatableArray(Object)} method.
     *
     * @param dataBuffer the data buffer.
     * @param bankIndex  the band index.
     * @return the data array for the specified bank.
     */
    public static Object getDataArray(java.awt.image.DataBuffer dataBuffer, int bankIndex) {
        if (dataBuffer instanceof java.awt.image.DataBufferByte) {
            return ((java.awt.image.DataBufferByte) dataBuffer).getData(bankIndex);
        } else if (dataBuffer instanceof java.awt.image.DataBufferShort) {
            return ((java.awt.image.DataBufferShort) dataBuffer).getData(bankIndex);
        } else if (dataBuffer instanceof java.awt.image.DataBufferUShort) {
            return ((java.awt.image.DataBufferUShort) dataBuffer).getData(bankIndex);
        } else if (dataBuffer instanceof java.awt.image.DataBufferInt) {
            return ((java.awt.image.DataBufferInt) dataBuffer).getData(bankIndex);
        } else if (dataBuffer instanceof java.awt.image.DataBufferFloat) {
            return ((java.awt.image.DataBufferFloat) dataBuffer).getData(bankIndex);
        } else if (dataBuffer instanceof java.awt.image.DataBufferDouble) {
            return ((java.awt.image.DataBufferDouble) dataBuffer).getData(bankIndex);
        } else
            throw new UnsupportedOperationException("Unknown DataBuffer type");
    }

    private void checkMatrix(Matrix<? extends PArray> packedMatrix) {
        if (packedMatrix == null)
            throw new NullPointerException("Null packed matrix");
        if (packedMatrix.dimCount() != 3 && packedMatrix.dimCount() != 2)
            throw new IllegalArgumentException("Packed matrix must be 2- or 3-dimensional");
        long bandCount = packedMatrix.dimCount() == 2 ? 1 : packedMatrix.dim(0);
        if (bandCount < 1 || bandCount > 4)
            throw new IllegalArgumentException("The number of color channels(RGBA) must be in 1..4 range");
        if (packedMatrix.dim(1) > Integer.MAX_VALUE || packedMatrix.dim(2) > Integer.MAX_VALUE)
            throw new TooLargeArrayException("Too large packed " + packedMatrix
                    + ": dim(1)/dim(2) must be in <=Integer.MAX_VALUE");
        PArray array = packedMatrix.array();
        if (!(array instanceof ByteArray) && byteArrayRequired())
            throw new IllegalArgumentException("ByteArray required");
    }

    protected void toDataBufferBand0Filter(byte[] src, int srcPos, byte[] dest) {
        System.arraycopy(src, srcPos, dest, 0, dest.length);
    }

    public static class Packed3DToPackedRGB extends MatrixToBufferedImageConverter {
        private final boolean addAlpha;

        public Packed3DToPackedRGB(boolean addAlpha) {
            this.addAlpha = addAlpha;
        }

        public boolean addAlpha() {
            return addAlpha;
        }

        @Override
        public long colorValue(Matrix<? extends PArray> packedMatrix, java.awt.Color color, int bankIndex) {
            return color.getRGB();
        }

        @Override
        public String toString() {
            return "Packed3DToPackedRGB (addAlpha=" + addAlpha + ")";
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray packedArray, int bandCount) {
            if (!(packedArray instanceof ByteArray)) {
                throw new IllegalArgumentException("ByteArray required");
            }
            if (!(packedArray instanceof DirectAccessible)) {
                throw new IllegalArgumentException("DirectAccessible packedArray required");
            }
            int len = (int) (packedArray.length() / bandCount);
            if ((long) len * (long) bandCount != packedArray.length()) {
                throw new IllegalArgumentException("Unaligned ByteArray: its length " + packedArray.length() +
                        " is not divided by band count = " + bandCount);
            }
            byte[] ja = (byte[]) ((DirectAccessible) packedArray).javaArray();
            int offset = ((DirectAccessible) packedArray).javaArrayOffset();
            switch (bandCount) {
                case 1: {
                    if (addAlpha) {
                        int[] result = new int[len];
                        for (int j = 0, disp = offset; j < len; j++, disp++) {
                            result[j] = (ja[disp] & 0xFF) << 16
                                    | (ja[disp] & 0xFF) << 8
                                    | (ja[disp] & 0xFF)
                                    | 0xFF000000;
                        }
                        return new java.awt.image.DataBufferInt(result, len);
                    } else {
                        byte[] result = new byte[len];
                        toDataBufferBand0Filter(ja, offset, result);
                        return new java.awt.image.DataBufferByte(result, len);
                    }
                }
                case 2: {
                    int[] result = new int[len];
                    for (int j = 0, disp = offset; j < len; j++, disp += 2) {
                        result[j] = (ja[disp] & 0xFF) << 16
                                | (ja[disp] & 0xFF) << 8
                                | (ja[disp] & 0xFF)
                                | (ja[disp + 1] & 0xFF) << 24;
                    }
                    return new java.awt.image.DataBufferInt(result, len);
                }
                case 3: {
                    int[] result = new int[len];
                    for (int j = 0, disp = offset; j < len; j++, disp += 3) {
                        result[j] = (ja[disp] & 0xFF) << 16
                                | (ja[disp + 1] & 0xFF) << 8
                                | (ja[disp + 2] & 0xFF)
                                | 0xFF000000;
                    }
                    return new java.awt.image.DataBufferInt(result, len);
                }
                case 4: {
                    int[] result = new int[len];
                    for (int j = 0, disp = offset; j < len; j++, disp += 4) {
                        result[j] = (ja[disp] & 0xFF) << 16
                                | (ja[disp + 1] & 0xFF) << 8
                                | (ja[disp + 2] & 0xFF)
                                | (ja[disp + 3] & 0xFF) << 24;
                    }
                    return new java.awt.image.DataBufferInt(result, len);
                }
                default:
                    throw new IllegalArgumentException("Illegal bandCount = " + bandCount);
            }
        }

        @Override
        protected int[] rgbAlphaMasks(int bandCount) {
            if (addAlpha || bandCount == 4 || bandCount == 2) {
                return new int[]{0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000};
            } else if (bandCount == 3) {
                return new int[]{0x00FF0000, 0x0000FF00, 0x000000FF};
            } else {
                return null;
            }
        }
    }

    public static class Packed3DToPackedBGR extends Packed3DToPackedRGB {
        static final Packed3DToPackedBGR INSTANCE = new Packed3DToPackedBGR();

        private Packed3DToPackedBGR() {
            super(false);
        }

        @Override
        protected int[] rgbAlphaMasks(int bandCount) {
            if (bandCount == 4 || bandCount == 2) {
                return new int[]{0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000};
            } else if (bandCount == 3) {
                return new int[]{0x000000ff, 0x0000ff00, 0x00ff0000};
            } else {
                return null;
            }
        }
    }

    public static class Packed3DToBandedRGB extends MatrixToBufferedImageConverter {
        private final boolean addAlpha;

        public Packed3DToBandedRGB(boolean addAlpha) {
            this.addAlpha = addAlpha;
        }

        public boolean addAlpha() {
            return addAlpha;
        }

        @Override
        public String toString() {
            return "Packed3DToBandedRGB (addAlpha=" + addAlpha + ')';
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray packedArray, int bandCount) {
            if (!(packedArray instanceof ByteArray)) {
                throw new IllegalArgumentException("ByteArray required");
            }
            if (!(packedArray instanceof DirectAccessible)) {
                throw new IllegalArgumentException("DirectAccessible packedArray required");
            }
            int len = (int) (packedArray.length() / bandCount);
            if ((long) len * (long) bandCount != packedArray.length()) {
                throw new IllegalArgumentException("Unaligned ByteArray: its length " + packedArray.length() +
                        " is not divided by band count = " + bandCount);
            }
            byte[] ja = (byte[]) ((DirectAccessible) packedArray).javaArray();
            int offset = ((DirectAccessible) packedArray).javaArrayOffset();
            switch (bandCount) {
                case 1: {
                    if (addAlpha) {
                        byte[][] result = new byte[4][len];
                        System.arraycopy(ja, offset, result[0], 0, len);
                        System.arraycopy(ja, offset, result[1], 0, len);
                        System.arraycopy(ja, offset, result[2], 0, len);
                        JArrays.fillByteArray(result[3], (byte) 0xFF);
                        return new java.awt.image.DataBufferByte(result, len);
                    } else {
                        byte[] result = new byte[len];
                        System.arraycopy(ja, offset, result, 0, len);
                        return new java.awt.image.DataBufferByte(result, len);
                    }
                }
                case 2: {
                    byte[][] result = new byte[4][len];
                    for (int j = 0, disp = offset; j < len; j++, disp += 2) {
                        result[0][j] = ja[disp];
                        result[1][j] = ja[disp];
                        result[2][j] = ja[disp];
                        result[3][j] = ja[disp + 1];
                    }
                    return new java.awt.image.DataBufferByte(result, len);
                }
                case 3: {
                    byte[][] result;
                    if (addAlpha) {
                        result = new byte[4][len];
                        for (int j = 0, disp = offset; j < len; j++, disp += 3) {
                            result[0][j] = ja[disp];
                            result[1][j] = ja[disp + 1];
                            result[2][j] = ja[disp + 2];
                            result[3][j] = (byte) 0xFF;
                        }
                    } else {
                        result = new byte[3][len];
                        for (int j = 0, disp = offset; j < len; j++, disp += 3) {
                            result[0][j] = ja[disp];
                            result[1][j] = ja[disp + 1];
                            result[2][j] = ja[disp + 2];
                        }
                    }
                    return new java.awt.image.DataBufferByte(result, len);
                }
                case 4: {
                    byte[][] result = new byte[4][len];
                    for (int j = 0, disp = offset; j < len; j++, disp += 4) {
                        result[0][j] = ja[disp];
                        result[1][j] = ja[disp + 1];
                        result[2][j] = ja[disp + 2];
                        result[3][j] = ja[disp + 3];
                    }
                    return new java.awt.image.DataBufferByte(result, len);
                }
                default:
                    throw new IllegalArgumentException("Illegal bandCount = " + bandCount);
            }
        }
    }

    public static class MonochromeToIndexed extends Packed3DToPackedRGB {
        private final byte[] baseColor0, baseColor255;

        public MonochromeToIndexed(java.awt.Color baseColor0, java.awt.Color baseColor255) {
            super(false);
            if (baseColor0 == null)
                throw new NullPointerException("Null baseColor0");
            if (baseColor255 == null)
                throw new NullPointerException("Null baseColor255");
            this.baseColor0 = new byte[]{
                    (byte) baseColor0.getRed(),
                    (byte) baseColor0.getGreen(),
                    (byte) baseColor0.getBlue(),
                    (byte) baseColor0.getAlpha()
            };
            this.baseColor255 = new byte[]{
                    (byte) baseColor255.getRed(),
                    (byte) baseColor255.getGreen(),
                    (byte) baseColor255.getBlue(),
                    (byte) baseColor255.getAlpha()
            };
        }

        // Arguments must be in 0.0..1.0 range!
        public MonochromeToIndexed(double[] baseColor0, double[] baseColor255) {
            super(false);
            Objects.requireNonNull(baseColor0, "Null baseColor0");
            Objects.requireNonNull(baseColor255, "Null baseColor255");
            this.baseColor0 = new byte[4];
            this.baseColor255 = new byte[4];
            for (int k = 0; k < 4; k++) {
                double bc0 = baseColor0[k];
                this.baseColor0[k] = (byte) (bc0 < 0.0 ? 0 : bc0 > 1.0 ? 255 : Math.round(bc0 * 255.0));
                double bc255 = baseColor255[k];
                this.baseColor255[k] = (byte) (bc255 < 0.0 ? 0 : bc255 > 1.0 ? 255 : Math.round(bc255 * 255.0));
            }
        }

        @Override
        public long colorValue(Matrix<? extends PArray> packedMatrix, Color color, int bankIndex) {
            return Math.round(0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
        }

        @Override
        public byte[][] palette() {
            byte[][] result = new byte[4][256];
            for (int k = 0; k < 256; k++) {
                for (int bandIndex = 0; bandIndex < 4; bandIndex++) {
                    double bc0 = baseColor0[bandIndex] & 0xFF;
                    double bc255 = baseColor255[bandIndex] & 0xFF;
                    result[bandIndex][k] = (byte) Math.round(bc0 + (bc255 - bc0) * k / 255.0);
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return "MonochromeToIndexed ("
                    + "baseColor0=(" + JArrays.toString(baseColor0, Locale.US, "0x%X", ",", 100)
                    + "(, baseColor255=(" + JArrays.toString(baseColor255, Locale.US, "0x%X", ",", 100)
                    + "))";
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray packedArray, int bandCount) {
            if (bandCount != 1)
                throw new IllegalArgumentException("Illegal bandCount = " + bandCount + " (must be 1)");
            return super.toDataBuffer(packedArray, bandCount);
        }
    }
}