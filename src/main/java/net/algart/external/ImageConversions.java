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
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.RectangularFunc;

import java.util.ArrayList;
import java.util.List;

//TODO!! Remove; replace with Matrices.merge/splitAlongFirstDimension
@Deprecated
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
