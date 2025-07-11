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
import net.algart.math.functions.LinearFunc;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.*;
import java.util.Locale;
import java.util.Objects;

/**
 * Converter from AlgART 3D interleaved matrices into {@link BufferedImage}.
 * For monochrome images, 2D matrices are also allowed.
 *
 * @author Daniel Alievsky
 */
public abstract class MatrixToImage {
    private boolean unsignedInt32 = false;
    private boolean bytesRequired = false;

    /**
     * Equivalent to <code>{@link #ofChannels(java.util.List, boolean)
     * ofChannels}(channels, true)</code>.
     *
     * @param channels some multichannel image.
     * @return the equivalent <code>BufferedImage</code>.
     */
    public static BufferedImage ofChannels(java.util.List<? extends Matrix<? extends PArray>> channels) {
        return ofChannels(channels, true);

    }

    /**
     * Converts multichannel AlgART image (list of matrices) to <code>BufferedImage</code>.
     * Equivalent to
     * <pre>
     *      {@link #ofInterleaved(Matrix, boolean) ofInterleaved}({@link Matrices#interleave
     *      Matrices.interleave}(channels), convertAllElementTypesToByte)
     * </pre>
     * This is the simplest way to convert the list of channels to a form of AlgART matrices to
     * <code>BufferedImage</code>.
     * Note that sometimes this is not the best solution: for example, you may prefer to
     * call {@link #setUnsignedInt32(boolean) setUnsignedInt32(true)} before conversion
     * if you process 32-bit matrices built with other libraries (like 32-bit grayscale TIFF images).
     *
     * @param channels                     some multichannel image.
     * @param convertAllElementTypesToByte whether we need to convert non-byte data to <code>byte</code> values.
     * @return the equivalent <code>BufferedImage</code>.
     */
    public static BufferedImage ofChannels(
            java.util.List<? extends Matrix<? extends PArray>> channels,
            boolean convertAllElementTypesToByte) {
        Objects.requireNonNull(channels, "Null channels");
        return ofInterleaved(Matrices.interleave(channels), convertAllElementTypesToByte);
    }

    /**
     * Equivalent to <code>{@link #ofInterleaved(Matrix, boolean) ofInterleaved}(interleavedMatrix, true)</code>.
     *
     * @param interleavedMatrix the interleaved matrix (monochrome, or RGB, or RGBA).
     * @return the equivalent <code>BufferedImage</code>.
     */
    public static BufferedImage ofInterleaved(Matrix<? extends PArray> interleavedMatrix) {
        return ofInterleaved(interleavedMatrix, true);
    }

    /**
     * Converts the interleaved matrix to <code>BufferedImage</code>;
     * for multichannel matrices, the standard RGB/RGBA sample order is assumed.
     * Equivalent to
     * <pre>
     *     new {@link InterleavedRGBToInterleaved MatrixToImage.InterleavedRGBToInterleaved}()
     *                 .{@link #setBytesRequired}(convertAllElementTypesToByte)
     *                 .{@link #toBufferedImage(Matrix) toBufferedImage}(interleavedMatrix)
     * </pre>
     *
     * @param interleavedMatrix            the interleaved matrix (monochrome, or RGB, or RGBA).
     * @param convertAllElementTypesToByte whether we need to convert non-byte data to <code>byte</code> values.
     * @return the equivalent <code>BufferedImage</code>.
     */
    public static BufferedImage ofInterleaved(
            Matrix<? extends PArray> interleavedMatrix,
            boolean convertAllElementTypesToByte) {
        return new InterleavedRGBToInterleaved()
                .setBytesRequired(convertAllElementTypesToByte)
                .toBufferedImage(interleavedMatrix);
    }

    /**
     * Equivalent to <code>{@link #ofInterleavedBGR(Matrix, boolean)
     * ofInterleavedBGR}(interleavedMatrix, true)</code>.
     *
     * @param interleavedMatrix the interleaved matrix (monochrome, or BGR, or BGRA).
     * @return the equivalent <code>BufferedImage</code>.
     */
    public static BufferedImage ofInterleavedBGR(Matrix<? extends PArray> interleavedMatrix) {
        return ofInterleavedBGR(interleavedMatrix, true);
    }

    /**
     * Converts the interleaved matrix to <code>BufferedImage</code>;
     * for multichannel matrices, BGR/BGRA sample order is assumed.
     * Equivalent to
     * <pre>
     *     new {@link InterleavedBGRToInterleaved MatrixToImage.InterleavedBGRToInterleaved}()
     *                 .{@link #setBytesRequired}(convertAllElementTypesToByte)
     *                 .{@link #toBufferedImage(Matrix) toBufferedImage}(interleavedMatrix)
     * </pre>
     *
     * @param interleavedMatrix the interleaved matrix (monochrome, or BGR, or BGRA).
     * @param convertAllElementTypesToByte whether we need to convert non-byte data to <code>byte</code> values.
     * @return the equivalent <code>BufferedImage</code>.
     */
    public static BufferedImage ofInterleavedBGR(
            Matrix<? extends PArray> interleavedMatrix,
            boolean convertAllElementTypesToByte) {
        return new InterleavedBGRToInterleaved()
                .setBytesRequired(convertAllElementTypesToByte)
                .toBufferedImage(interleavedMatrix);
    }

    public boolean isUnsignedInt32() {
        return unsignedInt32;
    }

    /**
     * Specifies how to convert 32-bit <code>int</code> values to bytes: by simple unsigned shift ">>>24"
     * (<code>true</code> value) or via usual AlgART scaling: 0 to 0, <code>Integer.MAX_VALUE</code>
     * to 255 (<code>false</code> value).
     * This flag is used in {@link #toDataBuffer(Matrix)} method.
     *
     * @param unsignedInt32 whether <code>int</code> values are considered to be unsigned 32-bit;
     *                      default is <code>false</code>.
     * @return a reference to this object.
     */
    public MatrixToImage setUnsignedInt32(boolean unsignedInt32) {
        this.unsignedInt32 = unsignedInt32;
        return this;
    }

    public boolean isBytesRequired() {
        return bytesRequired;
    }

    /**
     * Sets a flag that ensures that the element types of AlgART arrays or matrices
     * passed to methods of this class are automatically converted to <code>byte</code> values
     * (if they are not bytes).
     * If the argument is <code>true</code>, you can be sure that
     * {@link #elementTypeSupported(Class)} method will return <code>true</code> for <code>byte.class</code> only.
     * This can be useful before writing the result <code>BufferedImage</code> into a file,
     * because some formats like JPEG support only <code>byte</code> (8-bit) precision.
     *
     * <p>This flag is used by {@link #elementTypeSupported(Class)} method.
     *
     * @param bytesRequired whether we need to convert the passed data to <code>byte</code> values.
     * @return a reference to this object.
     */
    public MatrixToImage setBytesRequired(boolean bytesRequired) {
        this.bytesRequired = bytesRequired;
        return this;
    }

    /**
     * Equivalent to <code>{@link #toBufferedImage(net.algart.arrays.Matrix, java.awt.image.DataBuffer)
     * toBufferedImage}(interleavedMatrix, null)</code>.
     *
     * @param interleavedMatrix the interleaved matrix.
     * @return the <code>BufferedImage</code> with the same data.
     */
    public final BufferedImage toBufferedImage(Matrix<? extends PArray> interleavedMatrix) {
        return toBufferedImage(interleavedMatrix, null);
    }

    /**
     * Converts the given interleaved matrix (2- or 3-dimensional) into <code>BufferedImage</code>.
     * Note: <code>dataBuffer!=null</code>, then the elements of the given matrix ignored, but the data
     * of the given buffer are used instead. (It is supposed that this buffer was created
     * by {@link #toDataBuffer(net.algart.arrays.Matrix)} method, maybe with some post-processing.)
     *
     * @param interleavedMatrix the interleaved matrix.
     * @param dataBuffer        the data for <code>BufferedImage</code>; can be {@code null}, then it is automatically
     *                          created as {@link #toDataBuffer(net.algart.arrays.Matrix)
     *                          toDataBuffer(interleavedMatrix)}.
     * @return the <code>BufferedImage</code> with the same data.
     */
    public final BufferedImage toBufferedImage(
            Matrix<? extends PArray> interleavedMatrix,
            java.awt.image.DataBuffer dataBuffer) {
        checkMatrix(interleavedMatrix);
        if (dataBuffer == null) {
            dataBuffer = toDataBuffer(interleavedMatrix);
        }
        final int dimX = dimX(interleavedMatrix);
        final int dimY = dimY(interleavedMatrix);
        final int bandCount = numberOfChannels(interleavedMatrix);
        final int resultNumberOfChannels = resultNumberOfChannels(bandCount);
        final byte[][] palette = palette();
        if (palette != null) {
            if (bandCount != 1) {
                throw new AssertionError(bandCount + " > 1 bands must not be allowed when " +
                        "palette() method returns non-null value");
            }
            if (palette.length < 3) {
                throw new AssertionError("palette() method must return palette with 3 or 4 bands");
            }
            IndexColorModel cm = palette.length == 3 ?
                    new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2]) :
                    new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2], palette[3]);
            WritableRaster wr = Raster.createInterleavedRaster(
                    dataBuffer, dimX, dimY, dimX, 1, new int[]{0}, null);
            return new BufferedImage(cm, wr, false, null);
        }
        final int[] bandMasks = packedSamplesRGBAMasks(bandCount);
        if (bandMasks != null) {
            if (resultNumberOfChannels != bandMasks.length) {
                throw new AssertionError("packedSamplesRGBAMasks() array must contain " +
                        resultNumberOfChannels + " elements");
            }
            WritableRaster wr = Raster.createPackedRaster(dataBuffer, dimX, dimY, dimX, bandMasks, null);
            DirectColorModel cm = bandMasks.length > 3 ?
                    new DirectColorModel(32, bandMasks[0], bandMasks[1], bandMasks[2], bandMasks[3]) :
                    new DirectColorModel(24, bandMasks[0], bandMasks[1], bandMasks[2], 0);
            return new BufferedImage(cm, wr, false, null);
        }
        final int[] sampleOffsets = interleavedSamplesRGBAOffsets(resultNumberOfChannels);
        if (sampleOffsets == null || sampleOffsets.length != resultNumberOfChannels) {
            throw new AssertionError("interleavedSamplesRGBAOffsets() must return non-empty array " +
                    "with " + resultNumberOfChannels + " elements, but it returns " +
                    java.util.Arrays.toString(sampleOffsets));
        }
        final int numberOfBanks = dataBuffer.getNumBanks();
        final WritableRaster wr;
        if (numberOfBanks == 1) {
            wr = Raster.createInterleavedRaster(
                    dataBuffer, dimX, dimY, dimX * resultNumberOfChannels,
                    resultNumberOfChannels, sampleOffsets, null);
        } else {
            final int[] indexes = bandedSamplesRGBABankIndexes(numberOfBanks);
            final int[] offsets = new int[numberOfBanks];
            // - zero-filled by Java
            wr = Raster.createBandedRaster(
                    dataBuffer, dimX, dimY, dimX,
                    indexes, offsets, null);
        }
        // - Important! Even for monochrome case (numberOfChannels == 1), we must use createInterleavedRaster!
        // If we try to call createBandedRaster in this case,
        // createGraphics().drawXxx method will use incorrect (translated) intensity
        final ComponentColorModel cm = componentColorModel(dataBuffer, resultNumberOfChannels);
        return new BufferedImage(cm, wr, false, null);
    }

    /**
     * Returns the x-dimension of the image, corresponding to the given interleaved matrix.
     * Note that it is <code>int</code>, not <code>long</code> (AWT images have 31-bit dimensions).
     *
     * @param interleavedMatrix the interleaved matrix.
     * @return the width of the corresponding image.
     */
    public int dimX(Matrix<? extends PArray> interleavedMatrix) {
        checkMatrix(interleavedMatrix);
        return (int) interleavedMatrix.dim(interleavedMatrix.dimCount() - 2);
    }

    /**
     * Returns the y-dimension of the image, corresponding to the given interleaved matrix.
     * Note that it is <code>int</code>, not <code>long</code> (AWT images have 31-bit dimensions).
     *
     * @param interleavedMatrix the interleaved matrix.
     * @return the height of the corresponding image.
     */
    public int dimY(Matrix<? extends PArray> interleavedMatrix) {
        checkMatrix(interleavedMatrix);
        return (int) interleavedMatrix.dim(interleavedMatrix.dimCount() - 1);
    }

    /**
     * Returns the number of color channels in the image, corresponding to the given interleaved matrix.
     * For example, it is 3 for RGB image or 4 for RGB-Alpha.
     *
     * @param interleavedMatrix the interleaved matrix.
     * @return the corresponding number of color bands.
     */
    public int numberOfChannels(Matrix<? extends PArray> interleavedMatrix) {
        checkMatrix(interleavedMatrix);
        return interleavedMatrix.dimCount() == 2 ? 1 : (int) interleavedMatrix.dim(0);
    }

    /**
     * Converts the given interleaved matrix (2- or 3-dimensional) into <code>java.awt.image.DataBuffer</code>.
     * This method is useful in addition to {@link #toBufferedImage(Matrix, java.awt.image.DataBuffer)},
     * if you want to do something with the created DataBuffer, for example, to correct some its pixels.
     *
     * <p>This method automatically converts the source data to a byte (8-bit) array,
     * if {@link #elementTypeSupported(Class)} returns <code>false</code> for the element type.
     *
     * @param interleavedMatrix the interleaved data.
     * @return the newly allocated <code>DataBuffer</code> with the same data.
     */
    public final java.awt.image.DataBuffer toDataBuffer(Matrix<? extends PArray> interleavedMatrix) {
        checkMatrix(interleavedMatrix);
        long bandCount = interleavedMatrix.dimCount() == 2 ? 1 : interleavedMatrix.dim(0);
        PArray array = interleavedMatrix.array();
        if (!elementTypeSupported(array.elementType())) {
            if (array instanceof IntArray ia && unsignedInt32) {
                int[] ints = ia.ja();
                byte[] bytes = new byte[ints.length];
                for (int k = 0; k < bytes.length; k++) {
                    bytes[k] = (byte) (ints[k] >>> 24);
                }
                array = SimpleMemoryModel.asUpdatableByteArray(bytes);
            } else {
                array = Arrays.asFuncArray(
                        LinearFunc.getInstance(0.0, 255.0 / array.maxPossibleValue(1.0)),
                        ByteArray.class,
                        array);
            }
        }
        if (!SimpleMemoryModel.isSimpleArray(array)) {
            array = array.updatableClone(Arrays.SMM);
        }
        return toDataBuffer(array, (int) bandCount);
    }

    /**
     * Returns the color channels order, in which they are written in the source color matrix.
     * The result is {@link ColorChannelOrder#RGB}
     * in {@link InterleavedRGBToPacked}, {@link InterleavedRGBToInterleaved},
     * {@link InterleavedRGBToBanded} classes, and {@link ColorChannelOrder#BGR}
     * in {@link InterleavedBGRToPacked}, {@link InterleavedBGRToInterleaved}, {@link InterleavedBGRToBanded}
     * classes.
     *
     * @return channel order: RGB or BGR.
     */
    public abstract ColorChannelOrder channelOrder();

    /**
     * Returns <code>true</code> if the specified element type of AlgART arrays or matrices,
     * passed to the methods of this class, is supported.
     * If this method returns <code>false</code>, this class converts
     * all other element types into <code>byte</code> (but the client may do this itself).
     *
     * <p>The default implementation returns <code>elementType == byte.class</code>.
     * Implementation in {@link InterleavedRGBToInterleaved} and {@link InterleavedBGRToInterleaved} classes
     * returns <code>true</code> also for <code>short.class</code> (which is interpreted
     * as <code>DataBuffer.TYPE_USHORT</code>), unless you disabled this by calling
     * {@link InterleavedRGBToInterleaved#setBytesRequired(boolean) setBytesRequired(true)}.
     * Other classes in this package preserve the default implementation.
     *
     * @param elementType some primitive type.
     * @return whether this element type of AlgART arrays or matrix is supported.
     */
    public boolean elementTypeSupported(Class<?> elementType) {
        return elementType == byte.class;
    }

    /**
     * Converts the color to the form stored in the data buffer returned by {@link #toDataBuffer(Matrix)}.
     *
     * <p>The default implementation is suitable for monochrome, indexed and multi-bank data buffers.</p>
     *
     * <p>Note: if the interleaved matrix is monochrome or indexed, i.e.
     * <code>{@link #numberOfChannels(Matrix) getBandCount}(interleavedMatrix)==1</code>,
     * this method returns
     * <pre>
     * Math.round(0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue())
     * </pre>
     * <p>In such cases, it is a  good idea to provide identical R, G, B color components
     * (if this method is not overridden).</p>
     *
     * <p>This method can be useful if you want to update the data buffer built by {@link #toDataBuffer(Matrix)}
     * method, for example, to fill some areas with a "background" color.</p>
     *
     * @param interleavedMatrix the interleaved data.
     * @param color             some color.
     * @param bankIndex         index of the bank in terms of {@link java.awt.image.DataBuffer}.
     * @return the corresponded component of this color or packed RGB-Alpha value,
     * depending on the structure of the data buffer.
     */
    public long colorValue(Matrix<? extends PArray> interleavedMatrix, Color color, int bankIndex) {
        final int bandCount = numberOfChannels(interleavedMatrix);
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
     * Returns the palette (<code>byte[4][256]</code>) if the indexed image is supposed.
     *
     * <p>The default implementation returns {@code null}, that means non-indexed image.
     *
     * @return the palette or {@code null} if the image should be not indexed.
     */
    public byte[][] palette() {
        return null;
    }

    /**
     * Actual method, on which {@link #toDataBuffer(Matrix)} is based.
     *
     * <p>The passed AlgART array must be {@link DirectAccessible direct accessible}.
     *
     * @param interleavedArray the interleaved data.
     * @param bandCount        the number of bands: if called from {@link #toDataBuffer(Matrix)},
     *                         it is 1 for 2-dimensional matrix and {@link Matrix#dim(int) dim(0)}
     *                         for 3-dimensional matrix.
     * @return the newly allocated <code>DataBuffer</code> with the same data.
     */
    protected abstract java.awt.image.DataBuffer toDataBuffer(PArray interleavedArray, int bandCount);

    /**
     * Returns the number of channels in the resulting buffered image if the number of source channels is specified
     * in the argument.
     *
     * <p>Usually this method returns <code>sourceNumberOfChannels</code> without change,
     * but may differ if {@link #toDataBuffer(PArray, int)} method performs some correction of the image structure,
     * for example, adds some channels. This is true for the classes
     * {@link InterleavedRGBToBanded}, {@link InterleavedBGRToBanded},
     * {@link InterleavedRGBToPacked}, {@link InterleavedBGRToPacked},
     * when <code>sourceNumberOfChannels==2</code> and also
     * when automatic adding the alpha-channel is turned on
     * using their method <code>setAlwaysAddAlpha(true)</code>:
     * in both cases this method returns 4 (RGBA or BGRA image).
     *
     * @param sourceNumberOfChannels the number of channels in the source image.
     * @return the actual number of channels in the created <code>BufferedImage</code>.
     */
    public int resultNumberOfChannels(int sourceNumberOfChannels) {
        return sourceNumberOfChannels;
    }

    /**
     * Returns the band masks, which will be passed to {@link
     * Raster#createPackedRaster(java.awt.image.DataBuffer, int, int, int, int[], Point)} method,
     * if you want to convert data into a packed <code>BufferedImage</code>.
     * The resulting array can be {@code null}, that means an interleaved or banded raster,
     * or an array containing <code>bandCount</code> elements:
     * red, green, blue and (if necessary) alpha masks.
     *
     * <p>Note that actual number of color channels will be equal to the length of the returned array,
     * not to the original <code>bandCount</code>; for example, this allows to automatically add
     * an alpha-channel to a monochrome image (with automatic conversion into 4-channel RGBA image).
     *
     * @param bandCount the number of bands (color channels) in the source matrix
     * @return the bit masks for storing bands in the packed <code>int</code> values;
     * should be {@code null} if {@code bandCount <= 1}.
     */
    protected int[] packedSamplesRGBAMasks(int bandCount) {
        return null;
    }

    /**
     * Returns the band offsets inside the single pixel, which will be passed to {@link
     * Raster#createInterleavedRaster(java.awt.image.DataBuffer, int, int, int, int, int[], Point)} method,
     * if you want to convert data into an interleaved <code>BufferedImage</code>.
     * The length of resulting array must be equal to <code>bandCount</code> argument.
     *
     * @param bandCount the number of bands (color channels) in the source matrix; always &ge;1.
     * @return the band offsets for storing bands of the single pixel.
     * @throws IllegalArgumentException if the argument is zero or negative.
     */
    protected int[] interleavedSamplesRGBAOffsets(int bandCount) {
        return increasedIndexes(bandCount);
    }

    /**
     * Returns the bank indexes, which will be passed to {@link
     * Raster#createBandedRaster(DataBuffer, int, int, int, int[], int[], Point)} method,
     * if you want to convert data into a banded <code>BufferedImage</code>.
     * The length of resulting array must be equal to <code>bandCount</code> argument.
     *
     * @param bankCount the number of banks in the data buffer.
     * @return the bank indexes for each band.
     * @throws IllegalArgumentException if the argument is zero or negative.
     */
    protected int[] bandedSamplesRGBABankIndexes(int bankCount) {
        return increasedIndexes(bankCount);
    }

    /**
     * Equivalent to <code>dataBuffer.getData(bankIndex)</code> for the corresponding specific element type.
     * It can be used, for example, together with {@link SimpleMemoryModel#asUpdatableArray(Object)} method.
     *
     * @param dataBuffer the data buffer.
     * @param bankIndex  the band index.
     * @return the data array for the specified bank.
     */
    public static Object dataArray(java.awt.image.DataBuffer dataBuffer, int bankIndex) {
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
        } else {
            throw new UnsupportedOperationException("Unknown DataBuffer type");
        }
    }

    int checkArray(PArray interleavedArray, int bandCount) {
        Objects.requireNonNull(interleavedArray, "Null source array");
        if (!(elementTypeSupported(interleavedArray.elementType()))) {
            throw new IllegalArgumentException("Unsupported element type: " + interleavedArray.elementType());
        }
        int len = interleavedArray.length32() / bandCount;
        if ((long) len * (long) bandCount != interleavedArray.length()) {
            throw new IllegalArgumentException("Unaligned ByteArray: its length " + interleavedArray.length() +
                    " is not divided by band count = " + bandCount);
        }
        return len;
    }

    private static ComponentColorModel componentColorModel(DataBuffer dataBuffer, int numberOfChannels) {
        ColorSpace cs = ColorSpace.getInstance(numberOfChannels < 3 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        boolean hasAlpha = numberOfChannels == 2 || numberOfChannels > 3;
        // - note: 2-channel image is a possible case, but we must interpret this as CS_GRAY with alpha,
        // because we need to create a 2-component color model, compatible with 2-channel raster
        return new ComponentColorModel(cs, null,
                hasAlpha,
                false,
                hasAlpha ? ColorModel.TRANSLUCENT : ColorModel.OPAQUE,
                dataBuffer.getDataType());
    }

    private static int increaseNumberOfChannelsTo4(int sourceNumberOfChannels, boolean alwaysAddAlpha) {
        return switch (sourceNumberOfChannels) {
            case 1, 3 -> alwaysAddAlpha ? 4 : sourceNumberOfChannels;
            case 2, 4 -> 4;
            default -> throw new IllegalArgumentException("Illegal sourceNumberOfChannels = " +
                    sourceNumberOfChannels);
        };
    }

    private static int[] increasedIndexes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Zero or negative number of channels: " + length);
        }
        int[] result = new int[length];
        for (int k = 0; k < result.length; k++) {
            result[k] = k;
        }
        return result;
    }

    private static int[] increasedIndexesFlipRB(int length) {
        int[] result = increasedIndexes(length);
        if (length == 3 || length == 4) {
            result[0] = 2;
            result[2] = 0;
        }
        return result;
    }

    private void checkMatrix(Matrix<? extends PArray> interleavedMatrix) {
        Objects.requireNonNull(interleavedMatrix, "Null interleaved matrix");
        if (interleavedMatrix.dimCount() != 3 && interleavedMatrix.dimCount() != 2) {
            throw new IllegalArgumentException("Interleaved matrix must be 2- or 3-dimensional");
        }
        final long numberOfChannels = interleavedMatrix.dimCount() == 2 ? 1 : interleavedMatrix.dim(0);
        if (numberOfChannels < 1 || numberOfChannels > 4) {
            throw new IllegalArgumentException("Unsupported number " + numberOfChannels +
                    " of color channels: it must be in range from 1 (monochrome)" +
                    " to 4 (usually red, green, blue, alpha)");
        }
        if (interleavedMatrix.size() > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large interleaved matrix " + interleavedMatrix
                    + ": number of elements must be <= Integer.MAX_VALUE");
        }
    }

    public static class InterleavedRGBToPacked extends MatrixToImage {
        private boolean alwaysAddAlpha = false;

        public boolean isAlwaysAddAlpha() {
            return alwaysAddAlpha;
        }

        public InterleavedRGBToPacked setAlwaysAddAlpha(boolean alwaysAddAlpha) {
            this.alwaysAddAlpha = alwaysAddAlpha;
            return this;
        }

        @Override
        public ColorChannelOrder channelOrder() {
            return ColorChannelOrder.RGB;
        }

        @Override
        public long colorValue(Matrix<? extends PArray> interleavedMatrix, Color color, int bankIndex) {
            return color.getRGB();
            // Note: this behavior is correct both for InterleavedRGBToPacked and InterleavedBGRToPacked.
            // These classes use the same format of data buffer, the difference is only in packedSamplesRGBAMasks()
        }

        @Override
        public String toString() {
            return "InterleavedRGBToPacked" + (isAlwaysAddAlpha() ? " (always adding alpha)" : "");
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray interleavedArray, int bandCount) {
            final int len = checkArray(interleavedArray, bandCount);
            final byte[] ja = interleavedArray.jaByte();
            switch (bandCount) {
                case 1 -> {
                    if (alwaysAddAlpha) {
                        int[] result = new int[len];
                        for (int j = 0, disp = 0; j < len; j++, disp++) {
                            result[j] = (ja[disp] & 0xFF) << 16
                                    | (ja[disp] & 0xFF) << 8
                                    | (ja[disp] & 0xFF)
                                    | 0xFF000000;
                        }
                        return new DataBufferInt(result, len);
                    } else {
                        byte[] result = new byte[len];
                        System.arraycopy(ja, 0, result, 0, result.length);
                        return new DataBufferByte(result, len);
                    }
                }
                case 2 -> {
                    int[] result = new int[len];
                    for (int j = 0, disp = 0; j < len; j++, disp += 2) {
                        result[j] = (ja[disp] & 0xFF) << 16
                                | (ja[disp] & 0xFF) << 8
                                | (ja[disp] & 0xFF)
                                | (ja[disp + 1] & 0xFF) << 24;
                    }
                    return new DataBufferInt(result, len);
                }
                case 3 -> {
                    int[] result = new int[len];
                    for (int j = 0, disp = 0; j < len; j++, disp += 3) {
                        result[j] = (ja[disp] & 0xFF) << 16
                                | (ja[disp + 1] & 0xFF) << 8
                                | (ja[disp + 2] & 0xFF)
                                | 0xFF000000;
                    }
                    return new DataBufferInt(result, len);
                }
                case 4 -> {
                    int[] result = new int[len];
                    for (int j = 0, disp = 0; j < len; j++, disp += 4) {
                        result[j] = (ja[disp] & 0xFF) << 16
                                | (ja[disp + 1] & 0xFF) << 8
                                | (ja[disp + 2] & 0xFF)
                                | (ja[disp + 3] & 0xFF) << 24;
                    }
                    return new DataBufferInt(result, len);
                }
                default -> throw new IllegalArgumentException("Illegal bandCount = " + bandCount);
            }
        }

        @Override
        public int resultNumberOfChannels(int sourceNumberOfChannels) {
            return increaseNumberOfChannelsTo4(sourceNumberOfChannels, alwaysAddAlpha);
        }

        @Override
        protected int[] packedSamplesRGBAMasks(int bandCount) {
            if (bandCount == 4 || bandCount == 2 || isAlwaysAddAlpha()) {
                return new int[]{0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000};
            } else if (bandCount == 3) {
                return new int[]{0x00ff0000, 0x0000ff00, 0x000000ff};
            } else {
                return null;
            }
        }
    }

    public static class InterleavedBGRToPacked extends InterleavedRGBToPacked {
        @Override
        public InterleavedBGRToPacked setAlwaysAddAlpha(boolean alwaysAddAlpha) {
            super.setAlwaysAddAlpha(alwaysAddAlpha);
            return this;
        }

        @Override
        public ColorChannelOrder channelOrder() {
            return ColorChannelOrder.BGR;
        }

        @Override
        protected int[] packedSamplesRGBAMasks(int bandCount) {
            if (bandCount == 4 || bandCount == 2 || isAlwaysAddAlpha()) {
                return new int[]{0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000};
            } else if (bandCount == 3) {
                return new int[]{0x000000ff, 0x0000ff00, 0x00ff0000};
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return "InterleavedBGRToPacked" + (isAlwaysAddAlpha() ? " (always adding alpha)" : "");
        }
    }

    public static class InterleavedRGBToInterleaved extends MatrixToImage {
        @Override
        public boolean elementTypeSupported(Class<?> elementType) {
            return isBytesRequired() ?
                    elementType == byte.class :
                    elementType == byte.class || elementType == short.class;
        }

        @Override
        public ColorChannelOrder channelOrder() {
            return ColorChannelOrder.RGB;
        }

        @Override
        public String toString() {
            return "InterleavedRGBToInterleaved";
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray interleavedArray, int bandCount) {
            checkArray(interleavedArray, bandCount);
            if (interleavedArray instanceof ByteArray a) {
                final byte[] result = a.toJavaArray();
                // - not ja(): we must be sure that "result" is a newly created Java array
                return new java.awt.image.DataBufferByte(result, result.length);
            } else if (interleavedArray instanceof ShortArray a) {
                final short[] result = a.toJavaArray();
                return new java.awt.image.DataBufferUShort(result, result.length);
            } else {
                throw new AssertionError("Unsupported element type: " + interleavedArray.elementType());
                // can occur only in incorrect subclasses: it is checked in checkArray()

            }
        }
    }

    public static class InterleavedBGRToInterleaved extends InterleavedRGBToInterleaved {
        @Override
        public ColorChannelOrder channelOrder() {
            return ColorChannelOrder.BGR;
        }

        @Override
        protected int[] interleavedSamplesRGBAOffsets(int bandCount) {
            return increasedIndexesFlipRB(bandCount);
        }
    }

    public static class InterleavedRGBToBanded extends MatrixToImage {
        private boolean alwaysAddAlpha = false;

        public boolean isAlwaysAddAlpha() {
            return alwaysAddAlpha;
        }

        public InterleavedRGBToBanded setAlwaysAddAlpha(boolean alwaysAddAlpha) {
            this.alwaysAddAlpha = alwaysAddAlpha;
            return this;
        }

        @Override
        public ColorChannelOrder channelOrder() {
            return ColorChannelOrder.RGB;
        }

        @Override
        public String toString() {
            return "InterleavedToBandedRGB" + (isAlwaysAddAlpha() ? " (always adding alpha)" : "");
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray interleavedArray, int bandCount) {
            final int len = checkArray(interleavedArray, bandCount);
            final byte[] ja = interleavedArray.jaByte();
            switch (bandCount) {
                case 1 -> {
                    if (alwaysAddAlpha) {
                        byte[][] result = new byte[4][len];
                        System.arraycopy(ja, 0, result[0], 0, len);
                        System.arraycopy(ja, 0, result[1], 0, len);
                        System.arraycopy(ja, 0, result[2], 0, len);
                        JArrays.fill(result[3], (byte) 0xFF);
                        return new DataBufferByte(result, len);
                    } else {
                        byte[] result = new byte[len];
                        System.arraycopy(ja, 0, result, 0, len);
                        return new DataBufferByte(result, len);
                    }
                }
                case 2 -> {
                    byte[][] result = new byte[4][len];
                    for (int j = 0, disp = 0; j < len; j++, disp += 2) {
                        result[0][j] = ja[disp];
                        result[1][j] = ja[disp];
                        result[2][j] = ja[disp];
                        result[3][j] = ja[disp + 1];
                    }
                    return new DataBufferByte(result, len);
                }
                case 3 -> {
                    byte[][] result;
                    if (alwaysAddAlpha) {
                        result = new byte[4][len];
                        for (int j = 0, disp = 0; j < len; j++, disp += 3) {
                            result[0][j] = ja[disp];
                            result[1][j] = ja[disp + 1];
                            result[2][j] = ja[disp + 2];
                            result[3][j] = (byte) 0xFF;
                        }
                    } else {
                        result = new byte[3][len];
                        for (int j = 0, disp = 0; j < len; j++, disp += 3) {
                            result[0][j] = ja[disp];
                            result[1][j] = ja[disp + 1];
                            result[2][j] = ja[disp + 2];
                        }
                    }
                    return new DataBufferByte(result, len);
                }
                case 4 -> {
                    byte[][] result = new byte[4][len];
                    for (int j = 0, disp = 0; j < len; j++, disp += 4) {
                        result[0][j] = ja[disp];
                        result[1][j] = ja[disp + 1];
                        result[2][j] = ja[disp + 2];
                        result[3][j] = ja[disp + 3];
                    }
                    return new DataBufferByte(result, len);
                }
                default -> throw new IllegalArgumentException("Illegal bandCount = " + bandCount);
            }
        }

        @Override
        public int resultNumberOfChannels(int sourceNumberOfChannels) {
            return increaseNumberOfChannelsTo4(sourceNumberOfChannels, alwaysAddAlpha);
        }
    }

    public static class InterleavedBGRToBanded extends InterleavedRGBToBanded {
        @Override
        public InterleavedBGRToBanded setAlwaysAddAlpha(boolean alwaysAddAlpha) {
            super.setAlwaysAddAlpha(alwaysAddAlpha);
            return this;
        }

        @Override
        public ColorChannelOrder channelOrder() {
            return ColorChannelOrder.BGR;
        }

        @Override
        public String toString() {
            return "InterleavedBGRToBanded" + (isAlwaysAddAlpha() ? " (always adding alpha)" : "");
        }

        @Override
        protected int[] bandedSamplesRGBABankIndexes(int bankCount) {
            return increasedIndexesFlipRB(bankCount);
        }
    }

    public static class MonochromeToIndexed extends InterleavedRGBToPacked {
        private final byte[] baseColor0, baseColor255;

        public MonochromeToIndexed(Color baseColor0, Color baseColor255) {
            Objects.requireNonNull(baseColor0, "Null baseColor0");
            Objects.requireNonNull(baseColor255, "Null baseColor255");
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
        public long colorValue(Matrix<? extends PArray> interleavedMatrix, Color color, int bankIndex) {
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
            return "MonochromeToIndexed: "
                    + "baseColor0=(" + JArrays.toString(
                    baseColor0, Locale.US, "0x%X", ",", 100)
                    + "), baseColor255=(" + JArrays.toString(
                    baseColor255, Locale.US, "0x%X", ",", 100)
                    + ")";
        }

        @Override
        protected java.awt.image.DataBuffer toDataBuffer(PArray interleavedArray, int bandCount) {
            if (bandCount != 1) {
                throw new IllegalArgumentException("Illegal bandCount = " + bandCount + ": must be 1 for " + this);
            }
            return super.toDataBuffer(interleavedArray, bandCount);
        }
    }
}