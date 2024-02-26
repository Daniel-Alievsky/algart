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

import net.algart.math.functions.Func;

/**
 * <p>Implementations of {@link Matrices} methods returning trivial interpolations of the matrix.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysInterpolationsImpl {
    static abstract class AbstractInterpolation implements Func {
        final Matrix<? extends PArray> m;
        final PArray array;
        final long[] dim;
        final long dimX, dimY, dimZ;
        final int idimX, idimY, idimZ;

        AbstractInterpolation(Matrix<? extends PArray> m) {
            if (m == null)
                throw new NullPointerException("Null m argument");
            this.m = m;
            this.array = m.array();
            this.dim = m.dimensions();
            this.dimX = m.dimX();
            this.dimY = m.dimY();
            this.dimZ = m.dimZ();
            this.idimX = (int)dimX;
            this.idimY = (int)dimY;
            this.idimZ = (int)dimZ;
        }

        void checkIndex(int coordIndex, long index) {
            if (index < 0 || index >= m.dim(coordIndex))
                throw new IndexOutOfBoundsException("Coordinate #" + coordIndex + " (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(" + coordIndex + ") = " + m.dim(coordIndex)));
        }

        void checkIndex0(long index) {
            if (index < 0 || index >= dimX)
                throw new IndexOutOfBoundsException("X-coordinate (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(0) = " + dimX));
        }

        void checkIndex1(long index) {
            if (index < 0 || index >= dimY)
                throw new IndexOutOfBoundsException("Y-coordinate (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(1) = " + dimY));
        }

        void checkIndex2(long index) {
            if (index < 0 || index >= dimZ)
                throw new IndexOutOfBoundsException("Z-coordinate (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(2) = " + dimZ));
        }

        void checkIntIndex0(int index) {
            if (index < 0 || index >= idimX)
                throw new IndexOutOfBoundsException("X-coordinate (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(0) = " + idimX));
        }

        void checkIntIndex1(int index) {
            if (index < 0 || index >= idimY)
                throw new IndexOutOfBoundsException("Y-coordinate (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(1) = " + idimY));
        }

        void checkIntIndex2(int index) {
            if (index < 0 || index >= idimZ)
                throw new IndexOutOfBoundsException("Z-coordinate (" + index
                    + (index < 0 ? ") < 0" : ") >= dim(2) = " + idimZ));
        }

        public final double get() {
            throw new IndexOutOfBoundsException("At least 1 argument required");
        }

        public Matrices.InterpolationMethod getInterpolationMethod() {
            return Matrices.InterpolationMethod.STEP_FUNCTION;
        }

        public abstract Boolean isChecked();

        public abstract Double outsideValue();

        public abstract String toString();
    }

    static class UncheckedTrivialInterpolation extends AbstractInterpolation {
        UncheckedTrivialInterpolation(Matrix<? extends PArray> m) {
            super(m);
        }

        public double get(double... x) {
            if (x.length == 0)
                throw new IndexOutOfBoundsException("At least 1 argument required");
            int n = dim.length < x.length ? dim.length - 1 : x.length - 1;
            long index = (long)x[n];
            while (--n >= 0) {
                long coord = (long)x[n];
                index = dim[n] * index + coord;
            }
            return array.getDouble(index);
        }

        public double get(double x) {
            long ix = (long)x;
            return array.getDouble(ix);
        }

        public double get(double x, double y) {
            long ix = (long)x;
            long iy = (long)y;
            return array.getDouble(iy * dimX + ix);
        }

        public double get(double x, double y, double z) {
            long ix = (long)x;
            long iy = (long)y;
            long iz = (long)z;
            return array.getDouble((iz * dimY + iy) * dimX + ix);
        }

        public double get(double x, double y, double z, double t) {
            long ix = (long)x;
            long iy = (long)y;
            long iz = (long)z;
            long it = (long)t;
            return array.getDouble(((it * dimZ + iz) * dimY + iy) * dimX + ix);
        }

        public Boolean isChecked() {
            return false;
        }

        public Double outsideValue() {
            return null;
        }

        public String toString() {
            return "step function (unchecked ranges) based on " + m;
        }
    }

    static class CheckedTrivialInterpolation extends AbstractInterpolation {
        CheckedTrivialInterpolation(Matrix<? extends PArray> m) {
            super(m);
        }

        public double get(double... x) {
            if (x.length == 0)
                throw new IndexOutOfBoundsException("At least 1 argument required");
            int n = dim.length < x.length ? dim.length - 1 : x.length - 1;
            long index = (long)x[n];
            checkIndex(n, index);
            while (--n >= 0) {
                long coord = (long)x[n];
                checkIndex(n, coord);
                index = dim[n] * index + coord;
            }
            return array.getDouble(index);
        }

        public double get(double x) {
            long ix = (long)x;
            return array.getDouble(ix);
        }

        public double get(double x, double y) {
            long ix = (long)x;
            checkIndex0(ix);
            long iy = (long)y;
            checkIndex1(iy);
            return array.getDouble(iy * dimX + ix);
        }

        public double get(double x, double y, double z) {
            long ix = (long)x;
            checkIndex0(ix);
            long iy = (long)y;
            checkIndex1(iy);
            long iz = (long)z;
            checkIndex2(iz);
            return array.getDouble((iz * dimY + iy) * dimX + ix);
        }

        public double get(double x, double y, double z, double t) {
            long ix = (long)x;
            checkIndex0(ix);
            long iy = (long)y;
            checkIndex1(iy);
            long iz = (long)z;
            checkIndex2(iz);
            long it = (long)t;
            checkIndex(3, iz);
            return array.getDouble(((it * dimZ + iz) * dimY + iy) * dimX + ix);
        }

        public Boolean isChecked() {
            return true;
        }

        public Double outsideValue() {
            return null;
        }

        public String toString() {
            return "step function based on " + m;
        }
    }

    static class ContinuedTrivialInterpolation extends AbstractInterpolation {
        final double outsideValue;
        ContinuedTrivialInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m);
            this.outsideValue = outsideValue;
        }

        public double get(double... x) {
            if (x.length == 0)
                throw new IndexOutOfBoundsException("At least 1 argument required");
            int n = dim.length < x.length ? dim.length - 1 : x.length - 1;
            long index = (long)x[n];
            if (index < 0 || index >= dim[n])
                return outsideValue;
            while (--n >= 0) {
                long coord = (long)x[n];
                if (coord < 0 || coord >= dim[n])
                    return outsideValue;
                index = dim[n] * index + coord;
            }
            return array.getDouble(index);
        }

        public double get(double x) {
            long ix = (long)x;
            if (ix < 0 || ix >= dimX)
                return outsideValue;
            return array.getDouble(ix);
        }

        public double get(double x, double y) {
            long ix = (long)x;
            if (ix < 0 || ix >= dimX)
                return outsideValue;
            long iy = (long)y;
            if (iy < 0 || iy >= dimY)
                return outsideValue;
            return array.getDouble(iy * dimX + ix);
        }

        public double get(double x, double y, double z) {
            long ix = (long)x;
            if (ix < 0 || ix >= dimX)
                return outsideValue;
            long iy = (long)y;
            if (iy < 0 || iy >= dimY)
                return outsideValue;
            long iz = (long)z;
            if (iz < 0 || iz >= dimZ)
                return outsideValue;
            return array.getDouble((iz * dimY + iy) * dimX + ix);
        }

        public double get(double x, double y, double z, double t) {
            long ix = (long)x;
            if (ix < 0 || ix >= dimX)
                return outsideValue;
            long iy = (long)y;
            if (iy < 0 || iy >= dimY)
                return outsideValue;
            long iz = (long)z;
            if (iz < 0 || iz >= dimZ)
                return outsideValue;
            long it = (long)t;
            if (it < 0 || it >= m.dim(3))
                return outsideValue;
            return array.getDouble(((it * dimZ + iz) * dimY + iy) * dimX + ix);
        }

        public Boolean isChecked() {
            return null;
        }

        public Double outsideValue() {
            return outsideValue;
        }

        public String toString() {
            return "continued (by " + outsideValue + ") step function based on " + m;
        }
    }

    /*Repeat() byte ==> char,,short,,int,,long,,float,,double;;
               Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
               (\s*&\s*0xFF) ==> ,,$1FF,, ,, ...
     */
    static class UncheckedTrivialByteInterpolation extends UncheckedTrivialInterpolation {
        private final byte[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialByteInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (byte[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix] & 0xFF;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix] & 0xFF;
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix] & 0xFF;
        }
    }

    static class CheckedTrivialByteInterpolation extends CheckedTrivialInterpolation {
        private final byte[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialByteInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (byte[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix] & 0xFF;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix] & 0xFF;
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix] & 0xFF;
        }
    }

    static class ContinuedTrivialByteInterpolation extends ContinuedTrivialInterpolation {
        private final byte[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialByteInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (byte[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix] & 0xFF;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix] & 0xFF;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static class UncheckedTrivialCharInterpolation extends UncheckedTrivialInterpolation {
        private final char[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialCharInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (char[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class CheckedTrivialCharInterpolation extends CheckedTrivialInterpolation {
        private final char[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialCharInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (char[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class ContinuedTrivialCharInterpolation extends ContinuedTrivialInterpolation {
        private final char[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialCharInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (char[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class UncheckedTrivialShortInterpolation extends UncheckedTrivialInterpolation {
        private final short[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialShortInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (short[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix] & 0xFFFF;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix] & 0xFFFF;
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix] & 0xFFFF;
        }
    }

    static class CheckedTrivialShortInterpolation extends CheckedTrivialInterpolation {
        private final short[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialShortInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (short[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix] & 0xFFFF;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix] & 0xFFFF;
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix] & 0xFFFF;
        }
    }

    static class ContinuedTrivialShortInterpolation extends ContinuedTrivialInterpolation {
        private final short[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialShortInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (short[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix] & 0xFFFF;
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix] & 0xFFFF;
        }
    }

    static class UncheckedTrivialIntInterpolation extends UncheckedTrivialInterpolation {
        private final int[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialIntInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (int[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class CheckedTrivialIntInterpolation extends CheckedTrivialInterpolation {
        private final int[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialIntInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (int[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class ContinuedTrivialIntInterpolation extends ContinuedTrivialInterpolation {
        private final int[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialIntInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (int[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class UncheckedTrivialLongInterpolation extends UncheckedTrivialInterpolation {
        private final long[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialLongInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (long[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class CheckedTrivialLongInterpolation extends CheckedTrivialInterpolation {
        private final long[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialLongInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (long[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class ContinuedTrivialLongInterpolation extends ContinuedTrivialInterpolation {
        private final long[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialLongInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (long[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class UncheckedTrivialFloatInterpolation extends UncheckedTrivialInterpolation {
        private final float[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialFloatInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (float[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class CheckedTrivialFloatInterpolation extends CheckedTrivialInterpolation {
        private final float[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialFloatInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (float[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class ContinuedTrivialFloatInterpolation extends ContinuedTrivialInterpolation {
        private final float[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialFloatInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (float[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class UncheckedTrivialDoubleInterpolation extends UncheckedTrivialInterpolation {
        private final double[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        UncheckedTrivialDoubleInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (double[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            int iy = (int)y;
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            int iy = (int)y;
            int iz = (int)z;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class CheckedTrivialDoubleInterpolation extends CheckedTrivialInterpolation {
        private final double[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        CheckedTrivialDoubleInterpolation(Matrix<? extends PArray> m) {
            super(m);
            this.arr = (double[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            checkIntIndex0(ix); // necessary, unlike a case of using array.getDouble
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            return arr[ofs + iy * idimX + ix];
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            checkIntIndex0(ix);
            int iy = (int)y;
            checkIntIndex1(iy);
            int iz = (int)z;
            checkIntIndex2(iz);
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }

    static class ContinuedTrivialDoubleInterpolation extends ContinuedTrivialInterpolation {
        private final double[] arr;
        private final int ofs;
        // must be array.length()<Integer.MAX_VALUE!
        ContinuedTrivialDoubleInterpolation(Matrix<? extends PArray> m, double outsideValue) {
            super(m, outsideValue);
            this.arr = (double[])Arrays.javaArrayInternal(array);
            this.ofs = Arrays.javaArrayOffsetInternal(array);
        }

        public double get(double x) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            return arr[ofs + ix];
        }

        public double get(double x, double y) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            return array.getDouble(iy * idimX + ix);
        }

        public double get(double x, double y, double z) {
            int ix = (int)x;
            if (ix < 0 || ix >= idimX)
                return outsideValue;
            int iy = (int)y;
            if (iy < 0 || iy >= idimY)
                return outsideValue;
            int iz = (int)z;
            if (iz < 0 || iz >= idimZ)
                return outsideValue;
            return arr[ofs + (iz * idimY + iy) * idimX + ix];
        }
    }
    /*Repeat.AutoGeneratedEnd*/
}
