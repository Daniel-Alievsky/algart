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

/**
 * <p>Implementations of {@link Matrices} methods returning polylinear interpolations of the matrix.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysPolylinearInterpolationsImpl {
    static abstract class AbstractPolylinearInterpolation extends ArraysInterpolationsImpl.AbstractInterpolation {
        AbstractPolylinearInterpolation(Matrix<? extends PArray> m) {
            super(m);
        }

        public final double get(double x, double y, double z) { // not optimized 3D branch
            return get(new double[] {x, y, z});
        }

        public final double get(double x, double y, double z, double t) { // not optimized 4D branch
            return get(new double[] {x, y, z, t});
        }

        @Override
        public Matrices.InterpolationMethod getInterpolationMethod() {
            return Matrices.InterpolationMethod.POLYLINEAR_FUNCTION;
        }

    }

    static strictfp class UncheckedPolylinearInterpolation extends AbstractPolylinearInterpolation {
        UncheckedPolylinearInterpolation(Matrix<? extends PArray> m) {
            super(m);
        }

        public double get(double... x) {
            if (x.length == 0)
                throw new IndexOutOfBoundsException("At least 1 argument required");
            return get(x, dim.length < x.length ? dim.length  : x.length, 0);
        }

        public double get(double x) {
            long ix = (long)x;
            double a = array.getDouble(ix);
            if (ix == dimX - 1)
                return a;
            double b = array.getDouble(ix + 1);
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            long ix = (long)x;
            long iy = (long)y;
            long ofs1 = iy * dimX + ix, ofs2 = ofs1 + dimX;
            double a = array.getDouble(ofs1);
            double b = ix == dimX - 1 ? a : array.getDouble(ofs1 + 1);
            double v1 = (a - b) * (ix - x) + a;
            if (iy == dimY - 1)
                return v1;
            double c = array.getDouble(ofs2);
            double d = ix == dimX - 1 ? c : array.getDouble(ofs2 + 1);
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }

        double get(double[] x, int xLength, long layerIndex) {
            if (xLength == 1) {
                long coord = (long)x[0];
                long index =  layerIndex * dimX + coord;
                double a = array.getDouble(index);
                if (coord == dimX - 1)
                    return a;
                double b = array.getDouble(index + 1);
                return (a - b) * (coord - x[0]) + a;
            } else {
                int n = xLength - 1;
                long coord = (long)x[n];
                long index =  layerIndex * dim[n] + coord;
                double a = get(x, n, index);
                if (coord == dim[n] - 1)
                    return a;
                double b = get(x, n, index + 1);
                return (a - b) * (coord - x[n]) + a;
            }
        }

        public Boolean isChecked() {
            return false;
        }

        public Double outsideValue() {
            return null;
        }

        public String toString() {
            return "polylinear interpolation (unchecked ranges) of " + m;
        }
    }

    static strictfp class CheckedPolylinearInterpolation extends AbstractPolylinearInterpolation {
        CheckedPolylinearInterpolation(Matrix<? extends PArray> m) {
            super(m);
        }

        public double get(double... x) {
            if (x.length == 0)
                throw new IndexOutOfBoundsException("At least 1 argument required");
            return get(x, dim.length < x.length ? dim.length  : x.length, 0);
        }

        public double get(double x) {
            long ix = (long)x;
            checkIndex0(ix);
            double a = array.getDouble(ix);
            if (ix == dimX - 1)
                return a;
            double b = array.getDouble(ix + 1);
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            long ix = (long)x;
            checkIndex0(ix);
            long iy = (long)y;
            checkIndex1(iy);
            long ofs1 = iy * dimX + ix, ofs2 = ofs1 + dimX;
            double a = array.getDouble(ofs1);
            double b = ix == dimX - 1 ? a : array.getDouble(ofs1 + 1);
            double v1 = (a - b) * (ix - x) + a;
            if (iy == dimY - 1)
                return v1;
            double c = array.getDouble(ofs2);
            double d = ix == dimX - 1 ? c : array.getDouble(ofs2 + 1);
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }

        double get(double[] x, int xLength, long layerIndex) {
            if (xLength == 1) {
                long coord = (long)x[0];
                checkIndex0(coord);
                long index =  layerIndex * dimX + coord;
                double a = array.getDouble(index);
                if (coord == dimX - 1)
                    return a;
                double b = array.getDouble(index + 1);
                return (a - b) * (coord - x[0]) + a;
            } else {
                int n = xLength - 1;
                long coord = (long)x[n];
                checkIndex(n, coord);
                long index =  layerIndex * dim[n] + coord;
                double a = get(x, n, index);
                if (coord == dim[n] - 1)
                    return a;
                double b = get(x, n, index + 1);
                return (a - b) * (coord - x[n]) + a;
            }
        }

        public Boolean isChecked() {
            return true;
        }

        public Double outsideValue() {
            return null;
        }

        public String toString() {
            return "polylinear interpolation of " + m;
        }
    }

    static strictfp class ContinuedPolylinearInterpolation extends AbstractPolylinearInterpolation {
        final double outsideValue;
        ContinuedPolylinearInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m);
            this.outsideValue = outsideValue;
        }

        public double get(double... x) {
            if (x.length == 0)
                throw new IndexOutOfBoundsException("At least 1 argument required");
            return get(x, dim.length < x.length ? dim.length  : x.length, 0);
        }

        public double get(double x) {
            long ix = (long)x;
            if (ix < 0 || ix >= dimX - 1)
                return ix == dimX - 1 ? array.getDouble(ix) : outsideValue;
            double a = array.getDouble(ix);
            double b = array.getDouble(ix + 1);
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            long ix = (long)x;
            long iy = (long)y;
            if (ix < 0 || ix >= dimX - 1 || iy < 0 || iy >= dimY - 1) {
                if (ix == dimX - 1) {
                    if (iy < 0 || iy >= dimY)
                        return outsideValue;
                    long ofs1 = iy * dimX + ix, ofs2 = ofs1 + dimX;
                    if (iy == dimY - 1)
                        return array.getDouble(ofs1);
                    double a = array.getDouble(ofs1);
                    double c = array.getDouble(ofs2);
                    return (a - c) * (iy - y) + a;
                } else if (iy == dimY - 1) {
                    assert ix != dimX - 1;
                    if (ix < 0 || ix >= dimX)
                        return outsideValue;
                    long ofs1 = iy * dimX + ix;
                    double a = array.getDouble(ofs1);
                    double b = array.getDouble(ofs1 + 1);
                    return (a - b) * (ix - x) + a;
                }
                return outsideValue;
            }
            long ofs1 = iy * dimX + ix, ofs2 = ofs1 + dimX;
            double a = array.getDouble(ofs1);
            double b = array.getDouble(ofs1 + 1);
            double c = array.getDouble(ofs2);
            double d = array.getDouble(ofs2 + 1);
            double v1 = (a - b) * (ix - x) + a;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }

        double get(double[] x, int xLength, long layerIndex) {
            if (xLength == 1) {
                long coord = (long)x[0];
                long index =  layerIndex * dimX + coord;
                if (coord < 0 || coord >= dimX - 1)
                    return coord == dimX - 1 ? array.getDouble(index) : outsideValue;
                double a = array.getDouble(index);
                double b = array.getDouble(index + 1);
                return (a - b) * (coord - x[0]) + a;
            } else {
                int n = xLength - 1;
                long coord = (long)x[n];
                long index =  layerIndex * dim[n] + coord;
                if (coord < 0 || coord >= dim[n] - 1)
                    return coord == dim[n] - 1 ? get(x, n, index) : outsideValue;
                double a = get(x, n, index);
                double b = get(x, n, index + 1);
                return (a - b) * (coord - x[n]) + a;
            }
        }

        public Boolean isChecked() {
            return null;
        }

        public Double outsideValue() {
            return outsideValue;
        }

        public String toString() {
            return "continued (by " + outsideValue + ") polylinear interpolation of " + m;
        }
    }

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
               (\s*&\s*0xFF) ==> ,,$1FF,, ,, ...
     */
    static strictfp class UncheckedPolylinearByteInterpolation extends UncheckedPolylinearInterpolation {
        private final byte[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearByteInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (byte[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix] & 0xFF;
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1] & 0xFF;
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1] & 0xFF;
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1] & 0xFF;
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2] & 0xFF;
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1] & 0xFF;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearByteInterpolation extends CheckedPolylinearInterpolation {
        private final byte[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearByteInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (byte[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix] & 0xFF;
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1] & 0xFF;
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1] & 0xFF;
            double b = arr[ofs + ofs1 + 1] & 0xFF;
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2] & 0xFF;
            double d = arr[ofs + ofs2 + 1] & 0xFF;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearByteInterpolation extends ContinuedPolylinearInterpolation {
        private final byte[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearByteInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (byte[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] & 0xFF : outsideValue;
            double a = arr[ofs + ix] & 0xFF;
            double b = arr[ofs + ix + 1] & 0xFF;
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1] & 0xFF;
            double b = arr[ofs + ofs1 + 1] & 0xFF;
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2] & 0xFF;
            double d = arr[ofs + ofs2 + 1] & 0xFF;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static strictfp class UncheckedPolylinearCharInterpolation extends UncheckedPolylinearInterpolation {
        private final char[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearCharInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (char[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2];
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearCharInterpolation extends CheckedPolylinearInterpolation {
        private final char[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearCharInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (char[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearCharInterpolation extends ContinuedPolylinearInterpolation {
        private final char[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearCharInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (char[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] : outsideValue;
            double a = arr[ofs + ix];
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class UncheckedPolylinearShortInterpolation extends UncheckedPolylinearInterpolation {
        private final short[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearShortInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (short[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix] & 0xFFFF;
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1] & 0xFFFF;
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1] & 0xFFFF;
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1] & 0xFFFF;
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2] & 0xFFFF;
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1] & 0xFFFF;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearShortInterpolation extends CheckedPolylinearInterpolation {
        private final short[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearShortInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (short[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix] & 0xFFFF;
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1] & 0xFFFF;
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1] & 0xFFFF;
            double b = arr[ofs + ofs1 + 1] & 0xFFFF;
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2] & 0xFFFF;
            double d = arr[ofs + ofs2 + 1] & 0xFFFF;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearShortInterpolation extends ContinuedPolylinearInterpolation {
        private final short[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearShortInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (short[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] & 0xFFFF : outsideValue;
            double a = arr[ofs + ix] & 0xFFFF;
            double b = arr[ofs + ix + 1] & 0xFFFF;
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1] & 0xFFFF;
            double b = arr[ofs + ofs1 + 1] & 0xFFFF;
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2] & 0xFFFF;
            double d = arr[ofs + ofs2 + 1] & 0xFFFF;
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class UncheckedPolylinearIntInterpolation extends UncheckedPolylinearInterpolation {
        private final int[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearIntInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (int[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2];
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearIntInterpolation extends CheckedPolylinearInterpolation {
        private final int[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearIntInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (int[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearIntInterpolation extends ContinuedPolylinearInterpolation {
        private final int[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearIntInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (int[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] : outsideValue;
            double a = arr[ofs + ix];
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class UncheckedPolylinearLongInterpolation extends UncheckedPolylinearInterpolation {
        private final long[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearLongInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (long[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2];
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearLongInterpolation extends CheckedPolylinearInterpolation {
        private final long[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearLongInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (long[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearLongInterpolation extends ContinuedPolylinearInterpolation {
        private final long[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearLongInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (long[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] : outsideValue;
            double a = arr[ofs + ix];
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class UncheckedPolylinearFloatInterpolation extends UncheckedPolylinearInterpolation {
        private final float[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearFloatInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (float[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2];
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearFloatInterpolation extends CheckedPolylinearInterpolation {
        private final float[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearFloatInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (float[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearFloatInterpolation extends ContinuedPolylinearInterpolation {
        private final float[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearFloatInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (float[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] : outsideValue;
            double a = arr[ofs + ix];
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class UncheckedPolylinearDoubleInterpolation extends UncheckedPolylinearInterpolation {
        private final double[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedPolylinearDoubleInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (double[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = ix == idimX - 1 ? a : arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            if (iy == idimY - 1)
                return v1;
            double c = arr[ofs + ofs2];
            double d = ix == idimX - 1 ? c : arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class CheckedPolylinearDoubleInterpolation extends CheckedPolylinearInterpolation {
        private final double[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedPolylinearDoubleInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (double[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix);
            double a = arr[ofs + ix];
            if (ix == idimX - 1)
                return a;
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                checkIntIndex0(ix);
                checkIntIndex1(iy);
                throw new AssertionError();
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }

    static strictfp class ContinuedPolylinearDoubleInterpolation extends ContinuedPolylinearInterpolation {
        private final double[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedPolylinearDoubleInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (double[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX - 1)
                return ix == idimX - 1 ? arr[ofs + ix] : outsideValue;
            double a = arr[ofs + ix];
            double b = arr[ofs + ix + 1];
            return (a - b) * (ix - x) + a;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            if (ix < 0 || ix >= idimX - 1 || iy < 0 || iy >= idimY - 1) {
                if (ix == idimX - 1 || iy == idimY - 1) {
                    return super.get(x, y);
                }
                return outsideValue;
            }
            int ofs1 = iy * idimX + ix, ofs2 = ofs1 + idimX;
            double a = arr[ofs + ofs1];
            double b = arr[ofs + ofs1 + 1];
            double v1 = (a - b) * (ix - x) + a;
            double c = arr[ofs + ofs2];
            double d = arr[ofs + ofs2 + 1];
            double v2 = (c - d) * (ix - x) + c;
            return (v1 - v2) * (iy - y) + v1;
        }
    }
    /*Repeat.AutoGeneratedEnd*/
}
