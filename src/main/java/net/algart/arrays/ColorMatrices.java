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

package net.algart.arrays;

import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.LinearFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Utilities useful for working with lists of {@link Matrix AlgART matrices},
 * representing channels in a color image.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Daniel Alievsky
 */
public class ColorMatrices {
    private ColorMatrices() {
    }

    public static final double INTENSITY_R_WEIGHT = 0.299;
    public static final double INTENSITY_B_WEIGHT = 0.114;
    public static final double INTENSITY_G_WEIGHT = 1.0 - (INTENSITY_R_WEIGHT + INTENSITY_B_WEIGHT); // ~0.587

    public static Matrix<? extends PArray> asRGBIntensity(List<? extends Matrix<? extends PArray>> colorBands) {
        return colorBands.size() < 3 ?
                colorBands.get(0) :
                asRGBIntensity(colorBands.get(0), colorBands.get(1), colorBands.get(2));
    }

    public static Matrix<? extends PArray> asRGBIntensity(
            Matrix<? extends PArray> r,
            Matrix<? extends PArray> g,
            Matrix<? extends PArray> b)
    {
        if (g.type() == r.type() && g.type() == r.type()) {
            return asRGBIntensity(g.type(PArray.class), r, g, b);
        } else if (r.array().bitsPerElement() <= 8
                && g.array().bitsPerElement() <= 8
                && b.array().bitsPerElement() <= 8)
        {
            return asRGBIntensity(ByteArray.class, r, g, b);
        } else if (r.array().bitsPerElement() <= 16
                && g.array().bitsPerElement() <= 16
                && b.array().bitsPerElement() <= 16)
        {
            return asRGBIntensity(ShortArray.class, r, g, b);
        } else {
            return asRGBIntensity(FloatArray.class, r, g, b);
        }
    }

    public static <T extends PArray> Matrix<T> asRGBIntensity(
            Class<T> resultType,
            Matrix<? extends PArray> r,
            Matrix<? extends PArray> g,
            Matrix<? extends PArray> b)
    {
        Objects.requireNonNull(resultType, "Null resultType");
        if (r.type() == resultType && g.type() == resultType && g.type() == resultType) {
            final double increment = r.isFloatingPoint() ? 0.0 : 0.5;
            // - for integer matrices with the same type (most popular case) we prefer rounding (by adding 0.5)
            return Matrices.asFuncMatrix(
                    LinearFunc.getInstance(increment, INTENSITY_R_WEIGHT, INTENSITY_G_WEIGHT, INTENSITY_B_WEIGHT),
                    resultType, r, g, b);
        } else {
            final double scale = Arrays.maxPossibleValue(resultType, 1.0);
            return Matrices.asFuncMatrix(
                    LinearFunc.getInstance(0.0,
                            INTENSITY_R_WEIGHT * scale / Arrays.maxPossibleValue(r.type(), 1.0),
                            INTENSITY_G_WEIGHT * scale / Arrays.maxPossibleValue(g.type(), 1.0),
                            INTENSITY_B_WEIGHT * scale / Arrays.maxPossibleValue(b.type(), 1.0)),
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
        final double scale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double x0, double x1, double x2) {
                        return scale * rgbToHue(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
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
        final double scale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double x0, double x1, double x2) {
                        return scale * rgbToSaturationHsv(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
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
        final double scale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double x0, double x1, double x2) {
                        return scale * rgbToValue(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
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
        final double scale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double x0, double x1, double x2) {
                        return scale * rgbToSaturationHsl(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
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
        final double scale = Arrays.maxPossibleValue(resultType, 1.0);
        return Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double x0, double x1, double x2) {
                        return scale * rgbToLightness(x0 * rScaleInv, x1 * gScaleInv, x2 * bScaleInv);
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
        result.add(asRedFromHSV(resultType, hue, saturation, value));
        result.add(asGreenFromHSV(resultType, hue, saturation, value));
        result.add(asBlueFromHSV(resultType, hue, saturation, value));
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
        result.add(asRedFromHSL(resultType, hue, saturation, lightness));
        result.add(asGreenFromHSL(resultType, hue, saturation, lightness));
        result.add(asBlueFromHSL(resultType, hue, saturation, lightness));
        return result;
    }

    /*Repeat.AutoGeneratedEnd*/
    public static double rgbToHue(double r, double g, double b) {
        double cMax = Math.max(r, g);
        if (b > cMax) {
            cMax = b;
        }
        double cMin = Math.min(r, g);
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
        double cMax = Math.max(r, g);
        if (b > cMax) {
            cMax = b;
        }
        double cMin = Math.min(r, g);
        if (b < cMin) {
            cMin = b;
        }
        if (cMax == 0.0) {
            return 0.0;
        }
        return (cMax - cMin) / cMax;
    }

    public static double rgbToValue(double r, double g, double b) {
        double rgMax = Math.max(r, g);
        return Math.max(b, rgMax);
    }

    public static double rgbToSaturationHsl(double r, double g, double b) {
        double cMax = Math.max(r, g);
        if (b > cMax) {
            cMax = b;
        }
        double cMin = Math.min(r, g);
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
        double cMax = Math.max(r, g);
        if (b > cMax) {
            cMax = b;
        }
        double cMin = Math.min(r, g);
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
}
