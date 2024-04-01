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

package net.algart.math.patterns;

import net.algart.math.IPoint;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>A simple multidimensional bit matrix. Used for optimization of some methods processing patterns.
 * All dimensions of this matrix must be not greater than Integer.MAX_VALUE.</p>
 *
 * @author Daniel Alievsky
 */
class TinyBitMatrix {
    private final long[] array;
    private final int[] dimensions;
    private final long length;

    private TinyBitMatrix(long[] array, int[] dimensions) {
        this.length = checkDimensions(dimensions);
        long packedLength = TinyBitArrays.packedLength(this.length);
        if (packedLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large bit array: >=2^37 elements");
        }
        if (packedLength > array.length) {
            throw new IllegalArgumentException("Too large bit array: longer than the passed long[] array");
        }
        this.dimensions = dimensions.clone();
        this.array = array;
    }

    TinyBitMatrix(int[] dimensions) {
        this.length = checkDimensions(dimensions);
        long packedLength = TinyBitArrays.packedLength(this.length);
        if (packedLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large bit array: >=2^37 elements");
        }
        this.dimensions = dimensions.clone();
        this.array = new long[(int) packedLength];
    }

    public long[] array() {
        return this.array;
    }

    public int[] dimensions() {
        return this.dimensions.clone();
    }

    public int dimCount() {
        return this.dimensions.length;
    }

    public int dim(int n) {
        return n < this.dimensions.length ? this.dimensions[n] : 1;
    }

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/AbstractMatrix.java, dimEquals)
      @Override\s+(public) ==> $1 ;;
      \(Matrix\b.*?\s(\w+)\) ==> (TinyBitMatrix $1)
       !! Auto-generated: NOT EDIT !! */
    public boolean dimEquals(TinyBitMatrix m) {
        Objects.requireNonNull(m, "Null matrix");
        int dimCount = dimCount();
        if (m.dimCount() != dimCount) {
            return false;
        }
        for (int k = 0; k < dimCount; k++) {
            if (m.dim(k) != dim(k)) {
                return false;
            }
        }
        return true;
    }
    /*Repeat.IncludeEnd*/

    /*Repeat(INCLUDE_FROM_FILE, ../../arrays/AbstractMatrix.java, pseudoCyclicIndex)
      @Override\s+(public) ==> $1 ;;
      size\(\) ==> this.length
       !! Auto-generated: NOT EDIT !! */
    public long pseudoCyclicIndex(long... coordinates) {
        int n = coordinates.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        // 4D example:
        // index = (u*nz*ny*nx + z*ny*nz + y*nx + x) % N =
        //       = (u'*nz*ny*nx + z'*ny*nz + y'*nx + x') % N =
        //       = ((((u'*nz + z')*ny + y')*nx + x') % N
        // (N = nu*nz*ny*nx), where
        //       u' = u % nu
        //       z' = z % (nu*nz)
        //       y' = y % (nu*nz*ny)
        //       x' = x % (nu*nz*ny*nx)
        //
        --n;
        if (this.length == 0) {
            // we must check it here, in other case the further assertions can be false:
            // production of some dimensions in an empty matrix can be greater than Long.MAX_VALUE
            return 0;
        }
        long limit = dim(n);
        long result = coordinates[n] % limit;
        if (result < 0) {
            result += limit;
        }
        while (n > 0) {
            --n;
            long dim = dim(n);
            if (dim == 0) {
                return 0;
            }
            limit *= dim;
            long coord = coordinates[n] % limit;
            if (coord < 0) {
                coord += limit;
            }
            result *= dim;
            assert result >= 0 : "cyclic index becomes " + result + ", n=" + n + " in " + this; // overflow impossible
            result += coord;
            if (result < 0 || result >= limit) { // "< 0" means overflow
                result -= limit;
                assert result >= 0 : "cyclic index becomes " + result + ", n=" + n + " in " + this;
            }
        }
        return result;
    }
    /*Repeat.IncludeEnd*/

    public TinyBitMatrix reDim(int[] newDimensions) {
        return new TinyBitMatrix(array, newDimensions);
    }

    public void putPattern(Pattern pattern) {
        if (pattern.dimCount() != dimCount()) {
            throw new IllegalArgumentException("Number of dimensions of the pattern and the bit matrix mismatch");
        }
        for (IPoint ip : pattern.roundedPoints()) {
            TinyBitArrays.setBit(array, pseudoCyclicIndex(ip.coordinates()), true);
        }
    }

    public UniformGridPattern getIntegerPattern() {
        Set<IPoint> points = new HashSet<IPoint>();
        addPoints(points, new long[dimCount()], new boolean[dim(0)], 0, null);
        return Patterns.newIntegerPattern(points);
    }

    public UniformGridPattern getPattern(IPoint shift) {
        Set<IPoint> points = new HashSet<IPoint>();
        addPoints(points, new long[dimCount()], new boolean[dim(0)], 0, shift);
        return new BasicDirectPointSetUniformGridPattern(shift.coordCount(), points);
    }

    public void simpleDilation(TinyBitMatrix src, Pattern pattern) {
        Objects.requireNonNull(src, "Null src bit matrix");
        if (!dimEquals(src)) {
            throw new IllegalArgumentException("Bit matrix dimensions mismatch");
        }
        if (pattern.dimCount() != dimCount()) {
            throw new IllegalArgumentException("Number of dimensions mismatch of the pattern and the bit matrix");
        }
        Set<IPoint> points = pattern.roundedPoints();
        boolean first = true;
        for (IPoint ip : points) {
            long shift = pseudoCyclicIndex(ip.coordinates());
            if (first) {
                TinyBitArrays.copyBits(array, shift, src.array, 0, length - shift);
                TinyBitArrays.copyBits(array, 0, src.array, length - shift, shift);
            } else {
                TinyBitArrays.orBits(array, shift, src.array, 0, length - shift);
                TinyBitArrays.orBits(array, 0, src.array, length - shift, shift);
            }
            first = false;
        }
    }

    public long cardinality() {
        return TinyBitArrays.cardinality(array, 0, length);
    }

    private void addPoints(
        Set<IPoint> points,
        long[] coordinates, boolean[] temp, int lastCoordinatesCount, IPoint shift)
    {
        int currentCoordIndex = coordinates.length - 1 - lastCoordinatesCount;
        if (currentCoordIndex == 0) {
            coordinates[0] = 0;
            int n = dim(0);
            TinyBitArrays.unpackBits(temp, 0, array, pseudoCyclicIndex(coordinates), n);
            for (int i = 0; i < n; i++) {
                if (temp[i]) {
                    coordinates[0] = i;
                    IPoint p = IPoint.valueOf(coordinates);
                    if (shift != null) {
                        p = p.add(shift);
                    }
                    points.add(p);
                }
            }
        } else {
            for (int i = 0, n = dim(currentCoordIndex); i < n; i++) {
                coordinates[currentCoordIndex] = i;
                addPoints(points, coordinates, temp, lastCoordinatesCount + 1, shift);
            }
        }
    }

    private static long checkDimensions(int[] dim) throws IllegalArgumentException {
        Objects.requireNonNull(dim, "Null dimensions Java array");
        if (dim.length == 0) {
            throw new IllegalArgumentException("Empty dimensions Java array");
        }
        for (int n = 0; n < dim.length; n++) {
            if (dim[n] < 0) {
                throw new IllegalArgumentException("Negative matrix dimension #" + n + ": " + dim[n]);
            }
        }
        long[] lDim = new long[dim.length];
        for (int k = 0; k < dim.length; k++) {
            lDim[k] = dim[k];
        }
        long len = Patterns.longMul(lDim);
        if (len == Long.MIN_VALUE) {
            throw new TooManyPointsInPatternError("Illegal dimensions: dim[0] * dim[1] * ... > Long.MAX_VALUE");
        }
        return len;
    }
}
