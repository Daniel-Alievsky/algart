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
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.RectangularFunc;

import java.util.ArrayList;
import java.util.List;

public class ImageConversions {
    private ImageConversions() {
    }

    public static Matrix<? extends UpdatablePArray> packBandsIntoSequentialSamples(
        final ArrayContext context,
        final List<? extends Matrix<? extends PArray>> colorBands)
    {
        return packBandsIntoSequentialSamples(context, colorBands, null);
    }

    public static Matrix<? extends UpdatablePArray> packBandsIntoSequentialSamples(
        final ArrayContext context,
        List<? extends Matrix<? extends PArray>> colorBands,
        Matrix<? extends UpdatablePArray> result)
    {
        colorBands = new ArrayList<Matrix<? extends PArray>>(colorBands);
        if (colorBands.size() == 0)
            throw new IllegalArgumentException("Empty list of matrices");
        final Matrix<? extends PArray> m0 = colorBands.get(0);
        PArray[] bands = Matrices.arraysOfParallelMatrices(PArray.class, colorBands);
        final int nBands = bands.length;
        if (m0.dimCount() != 2)
            throw new IllegalArgumentException("Source matrices are not 2-dimensional");
        if (result != null && (result.dimCount() != 3 || result.dim(0) != nBands
            || result.dim(1) != m0.dimX() || result.dim(2) != m0.dimY()))
            throw new SizeMismatchException("The color bands and the resulting matrix dimensions mismatch: "
                + "the number of bands is " + nBands + ", the color band #0 is " + m0 + ", the result is " + result);
        for (int k = 1; k < nBands; k++) {
            if (bands[k].elementType() != m0.elementType())
                throw new IllegalArgumentException("Source matrices have different element types");
        }
        if (result != null && result.elementType() != m0.elementType())
            throw new IllegalArgumentException("Source matrices and the result has different element types");

        boolean identicalTiling = false;
        long[] tileDim = null;
        if (m0.isTiled()) {
            tileDim = m0.tileDimensions();
            identicalTiling = true;
            for (Matrix<? extends PArray> m : colorBands) {
                identicalTiling &= m.isTiled() && java.util.Arrays.equals(m.tileDimensions(), tileDim);
            }
            if (result != null) {
                long[] resultTileDim = result.tileDimensions();
                identicalTiling &= result.isTiled() && resultTileDim[0] == nBands
                    && resultTileDim[1] == tileDim[0] && resultTileDim[2] == tileDim[1];
            }
        }
        if (result == null) {
            double memory = 0.0;
            for (Matrix<? extends PArray> m : colorBands) {
                memory += Matrices.sizeOf(m);
            }
            MemoryModel mm = context == null || memory <= Arrays.SystemSettings.maxTempJavaMemory() ?
                Arrays.SMM : context.getMemoryModel();
            result = mm.newMatrix(UpdatablePArray.class, m0.elementType(), nBands, m0.dimX(), m0.dimY());
            if (identicalTiling) {
                assert tileDim != null;
                result = result.tile(nBands, tileDim[0], tileDim[1]);
            }
        }
        UpdatablePArray resultArray = result.array();
        if (identicalTiling) {
            for (int k = 0; k < nBands; k++) {
                bands[k] = colorBands.get(k).tileParent().array();
            }
            resultArray = result.tileParent().array();
        }
        if (nBands == 1) {
            Arrays.copy(context, resultArray, bands[0]); // the simplest solution is enough in this case
            return  result;
        }
        final int nPixels = (AbstractArray.defaultBufferCapacity(resultArray) + nBands - 1) / nBands;
        final DataBuffer buf = resultArray.buffer(DataBuffer.AccessMode.READ_WRITE, nPixels * nBands);
        final Object workArray = resultArray.newJavaArray(nPixels);
        long bandPos = 0;
        for (buf.map(0, false); buf.hasData(); buf.mapNext(false), bandPos += nPixels) {
            int len = (int) Math.min(nPixels, bands[0].length() - bandPos);
            for (int k = 0; k < nBands; k++) {
                bands[k].getData(bandPos, workArray, 0, len);
                if (resultArray instanceof BitArray) {
                    // improbable case of RGB bit matrix
                    long[] data = (long[]) buf.data();
                    boolean[] w = (boolean[]) workArray;
                    int j = 0;
                    for (long disp = buf.fromIndex() + k; j < len; j++, disp += nBands) {
                        PackedBitArrays.setBit(data, disp, w[j]);
                    }
                //[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
                //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double]]
                } else if (resultArray instanceof CharArray) {
                    char[] data = (char[]) buf.data();
                    char[] w = (char[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                } else if (resultArray instanceof ByteArray) {
                    byte[] data = (byte[]) buf.data();
                    byte[] w = (byte[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                } else if (resultArray instanceof ShortArray) {
                    short[] data = (short[]) buf.data();
                    short[] w = (short[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                } else if (resultArray instanceof IntArray) {
                    int[] data = (int[]) buf.data();
                    int[] w = (int[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                } else if (resultArray instanceof LongArray) {
                    long[] data = (long[]) buf.data();
                    long[] w = (long[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                } else if (resultArray instanceof FloatArray) {
                    float[] data = (float[]) buf.data();
                    float[] w = (float[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                } else if (resultArray instanceof DoubleArray) {
                    double[] data = (double[]) buf.data();
                    double[] w = (double[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        data[disp] = w[j];
                    }
                //[[Repeat.AutoGeneratedEnd]]
                } else
                    throw new AssertionError("Must not occur");
            }
            buf.force();
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(resultArray.elementType(),
                    buf.position() + buf.count(), result.size());
            }
        }
        return result;
    }

    public static List<Matrix<? extends PArray>> unpackBandsFromSequentialSamples(
        ArrayContext context,
        Matrix<? extends PArray> packedBands)
    {
        if (packedBands == null)
            throw new NullPointerException("Null packed 3D-matrix");
        if (packedBands.dim(0) > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Too large 1st matrix dimension: it must be 31-bit value "
                    + "(usually 1, 3 or 4)");
        if (packedBands.dim(0) > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Too large 1st matrix dimension: it must be 31-bit value "
                + "(usually 1, 3 or 4)");
        final int nBands = (int) packedBands.dim(0);
        final PArray packed3DArray = packedBands.isTiled() ? packedBands.tileParent().array() : packedBands.array();
        MemoryModel mm = context == null || Matrices.sizeOf(packedBands) <= Arrays.SystemSettings.maxTempJavaMemory() ?
            Arrays.SMM : context.getMemoryModel();
        final UpdatablePArray[] bandsArray = new UpdatablePArray[nBands];
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        for (int k = 0; k < nBands; k++) {
            Matrix<UpdatablePArray> m = mm.newMatrix(UpdatablePArray.class,
                packedBands.elementType(), packedBands.dim(1), packedBands.dim(2));
            bandsArray[k] = m.array(); // not tiled!
            if (packedBands.isTiled()) {
                long[] tileDim = packedBands.tileDimensions();
                m = m.tile(tileDim[1], tileDim[2]);
            }
            result.add(m);
        }
        if (nBands == 1) {
            Arrays.copy(context, bandsArray[0], packed3DArray); // the simplest solution is enough in this case
            return result;
        }
        final int nPixels = (AbstractArray.defaultBufferCapacity(packed3DArray) + nBands - 1) / nBands;
        final DataBuffer buf = packed3DArray.buffer(DataBuffer.AccessMode.READ, nPixels * nBands);
        final Object workArray = packed3DArray.newJavaArray(nPixels);
        long bandPos = 0;
        for (buf.map(0); buf.hasData(); buf.mapNext(), bandPos += nPixels) {
            int len = (int) Math.min(nPixels, bandsArray[0].length() - bandPos);
            for (int k = 0; k < nBands; k++) {
                if (packed3DArray instanceof BitArray) {
                    // improbable case of RGB bit matrix
                    long[] data = (long[]) buf.data();
                    boolean[] w = (boolean[]) workArray;
                    int j = 0;
                    for (long disp = buf.fromIndex() + k; j < len; j++, disp += nBands) {
                        w[j] = PackedBitArrays.getBit(data, disp);
                    }

                //[[Repeat() char ==> byte,,short,,int,,long,,float,,double;;
                //           Char ==> Byte,,Short,,Int,,Long,,Float,,Double]]
                } else if (packed3DArray instanceof CharArray) {
                    char[] data = (char[]) buf.data();
                    char[] w = (char[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                } else if (packed3DArray instanceof ByteArray) {
                    byte[] data = (byte[]) buf.data();
                    byte[] w = (byte[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                } else if (packed3DArray instanceof ShortArray) {
                    short[] data = (short[]) buf.data();
                    short[] w = (short[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                } else if (packed3DArray instanceof IntArray) {
                    int[] data = (int[]) buf.data();
                    int[] w = (int[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                } else if (packed3DArray instanceof LongArray) {
                    long[] data = (long[]) buf.data();
                    long[] w = (long[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                } else if (packed3DArray instanceof FloatArray) {
                    float[] data = (float[]) buf.data();
                    float[] w = (float[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                } else if (packed3DArray instanceof DoubleArray) {
                    double[] data = (double[]) buf.data();
                    double[] w = (double[]) workArray;
                    for (int j = 0, disp = buf.from() + k; j < len; j++, disp += nBands) {
                        w[j] = data[disp];
                    }

                //[[Repeat.AutoGeneratedEnd]]
                } else
                    throw new AssertionError("Must not occur");
                bandsArray[k].setData(bandPos, workArray, 0, len);
            }
            if (context != null) {
                context.checkInterruptionAndUpdateProgress(packed3DArray.elementType(),
                    buf.position() + buf.count(), packedBands.size());
            }
        }
        return result;
    }

    public static Matrix<? extends PArray> asIntensity(List<? extends Matrix<? extends PArray>> colorBands) {
        return colorBands.size() < 3 ?
            colorBands.get(0) :
            asIntensity(colorBands.get(0), colorBands.get(1), colorBands.get(2));
    }

    public static Matrix<? extends PArray> asIntensity(
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        if (g.type() == r.type() && g.type() == r.type()) {
            return asIntensity(g.type(PArray.class), r, g, b);
        } else if (r.array().bitsPerElement() <= 8
            && g.array().bitsPerElement() <= 8
            && b.array().bitsPerElement() <= 8)
        {
            return asIntensity(ByteArray.class, r, g, b);
        } else if (r.array().bitsPerElement() <= 16
            && g.array().bitsPerElement() <= 16
            && b.array().bitsPerElement() <= 16)
        {
            return asIntensity(ShortArray.class, r, g, b);
        } else {
            return asIntensity(FloatArray.class, r, g, b);
        }
    }

    public static <T extends PArray> Matrix<T> asIntensity(
        Class<T> resultType,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        if (resultType == null)
            throw new NullPointerException("Null resultType");
        if (r.type() == resultType && g.type() == resultType && g.type() == resultType) {
            return Matrices.asFuncMatrix(
                LinearFunc.getInstance(0.0, 0.299, 0.587, 0.114),
                resultType, r, g, b);
        } else {
            return Matrices.asFuncMatrix(
                LinearFunc.getInstance(0.0,
                    0.299 * Arrays.maxPossibleValue(resultType, 1.0) / Arrays.maxPossibleValue(r.type(), 1.0),
                    0.587 * Arrays.maxPossibleValue(resultType, 1.0) / Arrays.maxPossibleValue(g.type(), 1.0),
                    0.114 * Arrays.maxPossibleValue(resultType, 1.0) / Arrays.maxPossibleValue(b.type(), 1.0)),
                resultType, r, g, b);
        }
    }

    public static <T extends PArray> Matrix<T> asHue(
        Class<T> resultType,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        final double rScaleInv = 1.0 / r.array().maxPossibleValue(1.0);
        final double gScaleInv = 1.0 / g.array().maxPossibleValue(1.0);
        final double bScaleInv = 1.0 / b.array().maxPossibleValue(1.0);
        final double destScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return destScale * rgbToHue(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
                }
            },
            resultType, r, g, b);
    }

    public static <T extends PArray> Matrix<T> asHSVSaturation(
        Class<T> resultType,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        final double rScaleInv = 1.0 / r.array().maxPossibleValue(1.0);
        final double gScaleInv = 1.0 / g.array().maxPossibleValue(1.0);
        final double bScaleInv = 1.0 / b.array().maxPossibleValue(1.0);
        final double destScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return destScale * rgbToSaturationHsv(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
                }
            },
            resultType, r, g, b);
    }

    public static <T extends PArray> Matrix<T> asHSVValue(
        Class<T> resultType,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        final double rScaleInv = 1.0 / r.array().maxPossibleValue(1.0);
        final double gScaleInv = 1.0 / g.array().maxPossibleValue(1.0);
        final double bScaleInv = 1.0 / b.array().maxPossibleValue(1.0);
        final double destScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return destScale * rgbToValue(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
                }
            },
            resultType, r, g, b);
    }

    public static <T extends PArray> Matrix<T> asHSLSaturation(
        Class<T> resultType,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        final double rScaleInv = 1.0 / r.array().maxPossibleValue(1.0);
        final double gScaleInv = 1.0 / g.array().maxPossibleValue(1.0);
        final double bScaleInv = 1.0 / b.array().maxPossibleValue(1.0);
        final double destScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return destScale * rgbToSaturationHsl(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
                }
            },
            resultType, r, g, b);
    }

    public static <T extends PArray> Matrix<T> asHSLLightness(
        Class<T> resultType,
        Matrix<? extends PArray> r,
        Matrix<? extends PArray> g,
        Matrix<? extends PArray> b)
    {
        final double rScaleInv = 1.0 / r.array().maxPossibleValue(1.0);
        final double gScaleInv = 1.0 / g.array().maxPossibleValue(1.0);
        final double bScaleInv = 1.0 / b.array().maxPossibleValue(1.0);
        final double destScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return destScale * rgbToLightness(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
                }
            },
            resultType, r, g, b);
    }

    /*Repeat() Red ==> Green,,Blue */
    public static <T extends PArray> Matrix<T> asRedFromHSV(
        Class<T> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> value)
    {
        final double hScaleInv = 1.0 / hue.array().maxPossibleValue(1.0);
        final double sScaleInv = 1.0 / saturation.array().maxPossibleValue(1.0);
        final double vScaleInv = 1.0 / value.array().maxPossibleValue(1.0);
        final double resultScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return resultScale * hsvToRed(x0 * hScaleInv, x1 * sScaleInv, x2 * vScaleInv);
                }
            },
            resultType, hue, saturation, value);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public static <T extends PArray> Matrix<T> asGreenFromHSV(
        Class<T> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> value)
    {
        final double hScaleInv = 1.0 / hue.array().maxPossibleValue(1.0);
        final double sScaleInv = 1.0 / saturation.array().maxPossibleValue(1.0);
        final double vScaleInv = 1.0 / value.array().maxPossibleValue(1.0);
        final double resultScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return resultScale * hsvToGreen(x0 * hScaleInv, x1 * sScaleInv, x2 * vScaleInv);
                }
            },
            resultType, hue, saturation, value);
    }

    public static <T extends PArray> Matrix<T> asBlueFromHSV(
        Class<T> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> value)
    {
        final double hScaleInv = 1.0 / hue.array().maxPossibleValue(1.0);
        final double sScaleInv = 1.0 / saturation.array().maxPossibleValue(1.0);
        final double vScaleInv = 1.0 / value.array().maxPossibleValue(1.0);
        final double resultScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return resultScale * hsvToBlue(x0 * hScaleInv, x1 * sScaleInv, x2 * vScaleInv);
                }
            },
            resultType, hue, saturation, value);
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() Red ==> Green,,Blue */
    public static <T extends PArray> Matrix<T> asRedFromHSL(
        Class<T> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> lightness)
    {
        final double hScaleInv = 1.0 / hue.array().maxPossibleValue(1.0);
        final double sScaleInv = 1.0 / saturation.array().maxPossibleValue(1.0);
        final double lScaleInv = 1.0 / lightness.array().maxPossibleValue(1.0);
        final double resultScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return resultScale * hslToRed(x0 * hScaleInv, x1 * sScaleInv, x2 * lScaleInv);
                }
            },
            resultType, hue, saturation, lightness);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public static <T extends PArray> Matrix<T> asGreenFromHSL(
        Class<T> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> lightness)
    {
        final double hScaleInv = 1.0 / hue.array().maxPossibleValue(1.0);
        final double sScaleInv = 1.0 / saturation.array().maxPossibleValue(1.0);
        final double lScaleInv = 1.0 / lightness.array().maxPossibleValue(1.0);
        final double resultScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return resultScale * hslToGreen(x0 * hScaleInv, x1 * sScaleInv, x2 * lScaleInv);
                }
            },
            resultType, hue, saturation, lightness);
    }

    public static <T extends PArray> Matrix<T> asBlueFromHSL(
        Class<T> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> lightness)
    {
        final double hScaleInv = 1.0 / hue.array().maxPossibleValue(1.0);
        final double sScaleInv = 1.0 / saturation.array().maxPossibleValue(1.0);
        final double lScaleInv = 1.0 / lightness.array().maxPossibleValue(1.0);
        final double resultScale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
            new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0], x[1], x[2]);
                }

                @Override
                public double get(double x0, double x1, double x2) {
                    return resultScale * hslToBlue(x0 * hScaleInv, x1 * sScaleInv, x2 * lScaleInv);
                }
            },
            resultType, hue, saturation, lightness);
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() HSV ==> HSL;; value ==> lightness */
    public static List<Matrix<? extends PArray>> asRGBFromHSV(
        Class<? extends PArray> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> value)
    {
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        result.add(ImageConversions.asRedFromHSV(resultType, hue, saturation, value));
        result.add(ImageConversions.asGreenFromHSV(resultType, hue, saturation, value));
        result.add(ImageConversions.asBlueFromHSV(resultType, hue, saturation, value));
        return result;
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public static List<Matrix<? extends PArray>> asRGBFromHSL(
        Class<? extends PArray> resultType,
        Matrix<? extends PArray> hue,
        Matrix<? extends PArray> saturation,
        Matrix<? extends PArray> lightness)
    {
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        result.add(ImageConversions.asRedFromHSL(resultType, hue, saturation, lightness));
        result.add(ImageConversions.asGreenFromHSL(resultType, hue, saturation, lightness));
        result.add(ImageConversions.asBlueFromHSL(resultType, hue, saturation, lightness));
        return result;
    }
    /*Repeat.AutoGeneratedEnd*/

    public static Matrix<BitArray> asBitInsideRange(
        Matrix<? extends PArray> m,
        double min,
        double max,
        boolean normalize)
    {
        double scale = normalize ? m.array().maxPossibleValue(1.0) : 1.0;
        return Matrices.asFuncMatrix(
            RectangularFunc.getInstance(
                min * scale,
                max * scale, 1.0, 0.0),
            BitArray.class, m);
    }

    public static Matrix<BitArray> asBitInsideOrOutsideRange(
        Matrix<? extends PArray> m,
        double min,
        double max,
        boolean normalize)
    {
        double scale = normalize ? m.array().maxPossibleValue(1.0) : 1.0;
        return Matrices.asFuncMatrix(
            min <= max ?
                RectangularFunc.getInstance(
                    min * scale,
                    max * scale, 1.0, 0.0) :
                RectangularFunc.getInstance(
                    max * scale,
                    min * scale, 0.0, 1.0),
            BitArray.class, m);
    }

    public static Matrix<BitArray> asBitLess(
        Matrix<? extends PArray> m,
        double max,
        boolean normalize)
    {
        return asBitInsideRange(m, Double.NEGATIVE_INFINITY, max, normalize);
    }

    public static Matrix<BitArray> asBitGreater(
        Matrix<? extends PArray> m,
        double min,
        boolean normalize)
    {
        return asBitInsideRange(m, min, Double.POSITIVE_INFINITY, normalize);
    }

    public static double rgbToHue(double r, double g, double b) {
        double cMax = r > g ? r : g;
        if (b > cMax) {
            cMax = b;
        }
        double cMin = r < g ? r : g;
        if (b < cMin) {
            cMin = b;
        }
        if (cMin == cMax) {
            return 0.0;
        }
        double hue;
        if (r == cMax) {
            hue = (g - b) / (cMax - cMin);
        } else if (g == cMax) {
            hue = 2.0 + (b - r) / (cMax - cMin);
        } else {
            hue = 4.0 + (r - g) / (cMax - cMin);
        }
        hue *= 1.0 / 6.0;
        if (hue < 0.0) {
            hue += 1.0;
        }
        return hue;
    }

    public static double rgbToSaturationHsv(double r, double g, double b) {
        double cMax = r > g ? r : g;
        if (b > cMax) {
            cMax = b;
        }
        double cMin = r < g ? r : g;
        if (b < cMin) {
            cMin = b;
        }
        if (cMax == 0.0) {
            return 0.0;
        }
        return (cMax - cMin) / cMax;
    }

    public static double rgbToValue(double r, double g, double b) {
        double rgMax = r > g ? r : g;
        return b > rgMax ? b : rgMax;
    }

    public static double rgbToSaturationHsl(double r, double g, double b) {
        double cMax = r > g ? r : g;
        if (b > cMax) {
            cMax = b;
        }
        double cMin = r < g ? r : g;
        if (b < cMin) {
            cMin = b;
        }
        double sum = cMax + cMin;
        double diff = cMax - cMin;
        if (sum == 0.0 || diff == 0) {
            return 0.0;
        }
        if (sum == 2.0) {
            return 1.0;
        }
        if (sum <= 1.0) {
            return diff / sum;
        } else {
            return diff / (2.0 - sum);
        }
    }

    public static double rgbToLightness(double r, double g, double b) {
        double cMax = r > g ? r : g;
        if (b > cMax) {
            cMax = b;
        }
        double cMin = r < g ? r : g;
        if (b < cMin) {
            cMin = b;
        }
        return 0.5 * (cMax + cMin);
    }

    public static double hsvToRed(double h, double s, double v) {
        if (s == 0.0) {
            return v;
        }
        h = (h - StrictMath.floor(h)) * 6.0;
        switch ((int) h) {
            case 0:
            case 5:
                return v;
            case 1:
                return v * (1.0 - s * (h - StrictMath.floor(h)));
            case 2:
            case 3:
                return v * (1.0 - s);
            case 4:
                return v * (1.0 - (s * (1.0 - (h - StrictMath.floor(h)))));
            default:
                return 0.0; // impossible
        }
    }

    public static double hsvToGreen(double h, double s, double v) {
        if (s == 0.0) {
            return v;
        }
        h = (h - StrictMath.floor(h)) * 6.0;
        switch ((int) h) {
            case 0:
                return v * (1.0 - (s * (1.0 - (h - StrictMath.floor(h)))));
            case 1:
            case 2:
                return v;
            case 3:
                return v * (1.0 - s * (h - StrictMath.floor(h)));
            case 4:
            case 5:
                return v * (1.0 - s);
            default:
                return 0.0; // impossible
        }
    }

    public static double hsvToBlue(double h, double s, double v) {
        if (v == 0.0) {
            return v;
        }
        h = (h - StrictMath.floor(h)) * 6.0;
        switch ((int) h) {
            case 0:
            case 1:
                return v * (1.0 - s);
            case 2:
                return v * (1.0 - (s * (1.0 - (h - StrictMath.floor(h)))));
            case 3:
            case 4:
                return v;
            case 5:
                return v * (1.0 - s * (h - StrictMath.floor(h)));
            default:
                return 0.0; // impossible
        }
    }

    /*Repeat() Red ==> Green,,Blue;; (h\s*)\+=(\s*1\.0\s*\/\s*3\.0;) ==> \/\/ h is not corrected,,$1-=$2 */
    public static double hslToRed(double h, double s, double l) {
        if (s == 0) {
            return l;
        }
        double q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
        double p = 2.0 * l - q;
        h += 1.0 / 3.0;
        h = (h - StrictMath.floor(h)) * 6.0;
        switch ((int) h) {
            case 0:
                return p + (q - p) * h;
            case 1:
            case 2:
                return q;
            case 3:
                return p + (q - p) * (4.0 - h);
            case 4:
            case 5:
                return p;
            default:
                return 0.0; // impossible
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public static double hslToGreen(double h, double s, double l) {
        if (s == 0) {
            return l;
        }
        double q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
        double p = 2.0 * l - q;
        // h is not corrected
        h = (h - StrictMath.floor(h)) * 6.0;
        switch ((int) h) {
            case 0:
                return p + (q - p) * h;
            case 1:
            case 2:
                return q;
            case 3:
                return p + (q - p) * (4.0 - h);
            case 4:
            case 5:
                return p;
            default:
                return 0.0; // impossible
        }
    }

    public static double hslToBlue(double h, double s, double l) {
        if (s == 0) {
            return l;
        }
        double q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
        double p = 2.0 * l - q;
        h -= 1.0 / 3.0;
        h = (h - StrictMath.floor(h)) * 6.0;
        switch ((int) h) {
            case 0:
                return p + (q - p) * h;
            case 1:
            case 2:
                return q;
            case 3:
                return p + (q - p) * (4.0 - h);
            case 4:
            case 5:
                return p;
            default:
                return 0.0; // impossible
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    public static double[] HSL_to_RGB(double h, double s, double l)

    {

        double r = 0, g = 0, b = 0;

        double temp1, temp2;


        if (l == 0)
        {
            r = g = b = 0;
        } else
        {
            if (s == 0)
            {
                r = g = b = l;
            } else {
                temp2 = ((l <= 0.5) ? l * (1.0 + s) : l + s - (l * s));
                temp1 = 2.0 * l - temp2;
                double[] t3 = new double[]{h + 1.0 / 3.0, h, h - 1.0 / 3.0};
                double[] clr = new double[]{0, 0, 0};
                for (int i = 0; i < 3; i++)
                {
                    if (t3[i] < 0)
                        t3[i] += 1.0;
                    if (t3[i] > 1)
                        t3[i] -= 1.0;
                    if (6.0 * t3[i] < 1.0)
                        clr[i] = temp1 + (temp2 - temp1) * t3[i] * 6.0;
                    else if (2.0 * t3[i] < 1.0)
                        clr[i] = temp2;
                    else if (3.0 * t3[i] < 2.0)
                        clr[i] = (temp1 + (temp2 - temp1) * ((2.0 / 3.0) - t3[i]) * 6.0);
                    else
                        clr[i] = temp1;
                }
                r = clr[0];
                g = clr[1];
                b = clr[2];
            }

        }


        return new double[] {r, g, b};
    }

}
