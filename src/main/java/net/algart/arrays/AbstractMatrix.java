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

import net.algart.math.IRectangularArea;

import java.util.Objects;

/**
 * <p>A skeletal implementation of the {@link Matrix} interface to minimize
 * the effort required to implement this interface.</p>
 *
 * <p>All non-abstract methods are completely implemented here and may be not overridden in subclasses.</p>
 *
 * @param <T> the type of the built-in AlgART array.
 *
 * @author Daniel Alievsky
 */
public abstract class AbstractMatrix<T extends Array> implements Matrix<T> {

    public abstract T array();

    public Class<?> elementType() {
        return array().elementType();
    }

    public Class<? extends Array> type() {
        return array().type();
    }

    public Class<? extends UpdatableArray> updatableType() {
        return array().updatableType();
    }

    public <U extends Array> Class<? extends U> type(Class<U> arraySupertype) {
        Objects.requireNonNull(arraySupertype, "Null arraySupertype");
        if (UpdatableArray.class.isAssignableFrom(arraySupertype)) {
            throw new IllegalArgumentException("The passed arraySupertype, " + arraySupertype
                + ", is updatable, but the argument of Matrix.type method must define an immutable basic array "
                + "type (" + PArray.class.getName() + ", " + PIntegerArray.class.getName() + ", etc.)");
        }
        Class<? extends Array> result = array().type();
        if (!arraySupertype.isAssignableFrom(result)) {
            throw new ClassCastException("The type of built-in array of this matrix " + result.getName()
                + " is not the same type or a subtype of the specified supertype: " + arraySupertype
                + " (this matrix is " + this + ")");
        }
        return InternalUtils.cast(result);
    }

    public <U extends Array> Class<? extends U> updatableType(Class<U> arraySupertype) {
        Objects.requireNonNull(arraySupertype, "Null arraySupertype");
        Class<? extends Array> result = array().updatableType();
        if (!arraySupertype.isAssignableFrom(result)) {
            throw new ClassCastException("The type of built-in array of this matrix " + result.getName()
                + " is not the same type or a subtype of the specified supertype: " + arraySupertype
                + " (this matrix is " + this + ")");
        }
        return InternalUtils.cast(result);
    }

    public boolean isPrimitive() {
        return array() instanceof PArray;
    }

    public boolean isFloatingPoint() {
        return array() instanceof PFloatingArray;
    }

    public boolean isFixedPoint() {
        return array() instanceof PFixedArray;
    }

    public boolean isUnsigned() {
        return Arrays.isUnsignedElementType(elementType());
    }

    public long bitsPerElement() {
        final T array = array();
        return array instanceof PArray ? ((PArray) array).bitsPerElement() : -1;
    }

    public double maxPossibleValue(double valueForFloatingPoint) {
        final T array = array();
        return array instanceof PArray ?
                ((PArray) array).maxPossibleValue(valueForFloatingPoint) :
                Double.NaN;
    }

    public double maxPossibleValue() {
        return maxPossibleValue(1.0);
    }

    public abstract long[] dimensions();

    public abstract int dimCount();

    public abstract long dim(int n);

    /*Repeat.SectionStart dimEquals*/
    public boolean dimEquals(Matrix<?> m) {
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
    /*Repeat.SectionEnd dimEquals*/

    public boolean dimEquals(long... dimensions) {
        Objects.requireNonNull(dimensions, "Null dimensions argument");
        if (dimensions.length != dimCount()) {
            return false;
        }
        for (int k = 0; k < dimensions.length; k++) {
            if (dimensions[k] != dim(k)) {
                return false;
            }
        }
        return true;
    }

    public long index(long... coordinates) {
        int n = coordinates.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        --n;
        long result = coordinates[n];
        if (result < 0 || result >= dim(n)) {
            throw new IndexOutOfBoundsException("Coordinate #" + n + " (" + result
                + (result < 0 ? ") < 0" : ") >= dim(" + n + ") (" + dim(n) + ")") + " in " + this);
        }
        while (n > 0) {
            --n;
            long coord = coordinates[n];
            long limit = dim(n);
            if (coord < 0 || coord >= limit) {
                throw new IndexOutOfBoundsException("Coordinate #" + n + " (" + coord
                    + (coord < 0 ? ") < 0" : ") >= dim(" + n + ") (" + limit + ")") + " in " + this);
            }
            result = limit * result + coord;
        }
        return result;
    }

    public long index(long x, long y) {
        long dimX = dim(0), dimY = dim(1);
        if (x < 0 || x >= dimX) {
            throw new IndexOutOfBoundsException("X-coordinate (" + x
                + (x < 0 ? ") < 0" : ") >= dim(0) (" + dimX + ")") + " in " + this);
        }
        if (y < 0 || y >= dimY) {
            throw new IndexOutOfBoundsException("Y-coordinate (" + y
                + (y < 0 ? ") < 0" : ") >= dim(1) (" + dimY + ")") + " in " + this);
        }
        return y * dimX + x;
    }

    public long index(long x, long y, long z) {
        long dimX = dim(0), dimY = dim(1), dimZ = dim(2);
        if (x < 0 || x >= dimX) {
            throw new IndexOutOfBoundsException("X-coordinate (" + x
                + (x < 0 ? ") < 0" : ") >= dim(0) (" + dimX + ")") + " in " + this);
        }
        if (y < 0 || y >= dimY) {
            throw new IndexOutOfBoundsException("Y-coordinate (" + y
                + (y < 0 ? ") < 0" : ") >= dim(1) (" + dimY + ")") + " in " + this);
        }
        if (z < 0 || z >= dimZ) {
            throw new IndexOutOfBoundsException("Z-coordinate (" + z
                + (z < 0 ? ") < 0" : ") >= dim(2) (" + dimZ + ")") + " in " + this);
        }
        return (z * dimY + y) * dimX + x;
    }

    public long[] coordinates(long index, long[] result) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index argument");
        }
        int dimCount = dimCount();
        if (result == null) {
            result = new long[dimCount];
        } else if (result.length < dimCount) {
            throw new IllegalArgumentException("Too short result array: long[" + result.length
                + "]; " + dimCount + " elements required to store coordinates");
        }
        long a = index;
        for (int k = 0; k < dimCount - 1; k++) {
            long dim = dim(k);
            long b = a / dim;
            result[k] = a - b * dim; // here "*" is faster than "%"
            a = b;
        }
        long dim = dim(dimCount - 1);
        if (a >= dim) {
            throw new IndexOutOfBoundsException("Too large index argument: " + index
                + " >= matrix size " + JArrays.toString(dimensions(), "*", 10000));
        }
        result[dimCount - 1] = a;
        return result;
    }

    public long uncheckedIndex(long... coordinates) {
        int n = coordinates.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        --n;
        long result = coordinates[n];
        while (n > 0) {
            --n;
            result = dim(n) * result + coordinates[n];
        }
        return result;
    }

    public long cyclicIndex(long... coordinates) {
        int n = coordinates.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        --n;
        long result = coordinates[n];
        long limit = dim(n);
        result = limit == 0 ? 0 : result % limit;
        if (result < 0) {
            result += limit;
        }
        while (n > 0) {
            --n;
            long coord = coordinates[n];
            limit = dim(n);
            coord = limit == 0 ? 0 : coord % limit;
            if (coord < 0) {
                coord += limit;
            }
            result = limit * result + coord;
        }
        return result;
    }

    /*Repeat.SectionStart pseudoCyclicIndex*/
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
        if (isEmpty()) {
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
    /*Repeat.SectionEnd pseudoCyclicIndex*/

    public long mirrorCyclicIndex(long... coordinates) {
        int n = coordinates.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        --n;
        long result = coordinates[n];
        long limit = dim(n);
        result = limit == 0 ? 0 : normalizeMirrorCoord(result, limit);
        while (n > 0) {
            --n;
            long coord = coordinates[n];
            limit = dim(n);
            coord = limit == 0 ? 0 : normalizeMirrorCoord(coord, limit);
            result = limit * result + coord;
        }
        return result;
    }

    public boolean inside(long... coordinates) {
        if (coordinates.length == 0) {
            throw new IllegalArgumentException("Empty coordinates array");
        }
        for (int k = 0; k < coordinates.length; k++) {
            long coord = coordinates[k];
            if (coord < 0 || coord >= dim(k)) {
                return false;
            }
        }
        return true;
    }

    public boolean inside(long x, long y) {
        return x >= 0 && x < dim(0) && y >= 0 && y < dim(1);
    }

    public boolean inside(long x, long y, long z) {
        return x >= 0 && x < dim(0) && y >= 0 && y < dim(1) && z >= 0 && z < dim(2);
    }

    public abstract <U extends Array> Matrix<U> matrix(U anotherArray);

    public <U extends Array> Matrix<U> cast(Class<U> arrayClass) {
        if (!arrayClass.isInstance(array())) {
            throw new ClassCastException("Cannot cast " + array().getClass()
                + " (" + array() + ") to the type " + arrayClass.getName());
        }
        return InternalUtils.cast(this);
    }

    public Matrix<T> subMatrix(long[] from, long[] to) {
        return subMatrix(from, to, ContinuationMode.NONE, true);
    }

    public Matrix<T> subMatrix(IRectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        long[] from = area.min().coordinates();
        long[] to = area.max().coordinates();
        for (int k = 0; k < to.length; k++) {
            to[k]++;
        }
        return subMatrix(from, to, ContinuationMode.NONE, false);
    }

    public Matrix<T> subMatrix(long fromX, long fromY, long toX, long toY) {
        return subMatrix(new long[]{fromX, fromY}, new long[]{toX, toY}, ContinuationMode.NONE, false);
    }

    public Matrix<T> subMatrix(long fromX, long fromY, long fromZ, long toX, long toY, long toZ) {
        return subMatrix(new long[]{fromX, fromY, fromZ}, new long[]{toX, toY, toZ}, ContinuationMode.NONE, false);
    }

    public Matrix<T> subMatrix(long[] from, long[] to, ContinuationMode continuationMode) {
        return subMatrix(from, to, continuationMode, true);
    }

    public Matrix<T> subMatrix(IRectangularArea area, ContinuationMode continuationMode) {
        Objects.requireNonNull(area, "Null area argument");
        long[] from = area.min().coordinates();
        long[] to = area.max().coordinates();
        for (int k = 0; k < to.length; k++) {
            to[k]++;
        }
        return subMatrix(from, to, continuationMode, false);
    }

    public Matrix<T> subMatrix(long fromX, long fromY, long toX, long toY, ContinuationMode continuationMode) {
        return subMatrix(new long[]{fromX, fromY}, new long[]{toX, toY}, continuationMode, false);
    }

    public Matrix<T> subMatrix(
        long fromX, long fromY, long fromZ, long toX, long toY, long toZ,
        ContinuationMode continuationMode)
    {
        return subMatrix(new long[]{fromX, fromY, fromZ}, new long[]{toX, toY, toZ}, continuationMode, false);
    }

    public Matrix<T> subMatr(long[] position, long[] dimensions) {
        return subMatr(position, dimensions, ContinuationMode.NONE, true);
    }

    public Matrix<T> subMatr(long x, long y, long dimX, long dimY) {
        return subMatr(new long[]{x, y}, new long[]{dimX, dimY}, ContinuationMode.NONE, false);
    }

    public Matrix<T> subMatr(long x, long y, long z, long dimX, long dimY, long dimZ) {
        return subMatr(new long[]{x, y, z}, new long[]{dimX, dimY, dimZ}, ContinuationMode.NONE, false);
    }

    public Matrix<T> subMatr(long[] position, long[] dimensions, ContinuationMode continuationMode) {
        return subMatr(position, dimensions, continuationMode, true);
    }

    public Matrix<T> subMatr(long x, long y, long dimX, long dimY, ContinuationMode continuationMode) {
        return subMatr(new long[]{x, y}, new long[]{dimX, dimY}, continuationMode, false);
    }

    public Matrix<T> subMatr(
        long x, long y, long z, long dimX, long dimY, long dimZ,
        ContinuationMode continuationMode)
    {
        return subMatr(new long[]{x, y, z}, new long[]{dimX, dimY, dimZ}, continuationMode, false);
    }

    public boolean isSubMatrix() {
        return array() instanceof ArraysSubMatrixImpl.SubMatrixArray;
    }

    public Matrix<T> subMatrixParent() {
        T a = array();
        if (!(a instanceof ArraysSubMatrixImpl.SubMatrixArray)) {
            throw new NotSubMatrixException("subMatrixParent() method must not be called "
                + "for non-submatrix: " + this);
        }
        Matrix<?> result = ((ArraysSubMatrixImpl.SubMatrixArray) a).baseMatrix();
        return InternalUtils.cast(result);
    }

    public long[] subMatrixFrom() {
        T a = array();
        if (!(a instanceof ArraysSubMatrixImpl.SubMatrixArray)) {
            throw new NotSubMatrixException("subMatrixFrom() method must not be called "
                + "for non-submatrix: " + this);
        }
        return ((ArraysSubMatrixImpl.SubMatrixArray) a).from();
    }

    public long[] subMatrixTo() {
        T a = array();
        if (!(a instanceof ArraysSubMatrixImpl.SubMatrixArray)) {
            throw new NotSubMatrixException("subMatrixTo() method must not be called "
                + "for non-submatrix: " + this);
        }
        return ((ArraysSubMatrixImpl.SubMatrixArray) a).to();
    }

    public ContinuationMode subMatrixContinuationMode() {
        T a = array();
        if (!(a instanceof ArraysSubMatrixImpl.SubMatrixArray)) {
            throw new NotSubMatrixException("subMatrixContinuationMode() method must not be called "
                + "for non-submatrix: " + this);
        }
        return ((ArraysSubMatrixImpl.SubMatrixArray) a).continuationMode();
    }

    public Matrix<T> structureLike(Matrix<?> m) {
        return m.isTiled() ? this.tile(m.tileDimensions()) : this;
    }

    public boolean isStructuredLike(Matrix<?> m) {
        return m.isTiled() == this.isTiled();
    }

    public Matrix<T> tile(long... tileDim) {
        Objects.requireNonNull(tileDim, "Null tile dimensions Java array");
        T a = array();
        Array result;
        //[[Repeat() Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
        //           Boolean ==> Character,,Byte,,Short,,Integer,,Long,,Float,,Double;;
        //           if(?=\s+\(a\s+instanceof\s+Updatable) ==> } else if,,...]]
        if (a instanceof UpdatableBitArray) {
            Matrix<UpdatableBitArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableBitArray(m, tileDim);
        } else if (a instanceof BitArray) {
            Matrix<BitArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixBitArray(m, tileDim);
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        } else if (a instanceof UpdatableCharArray) {
            Matrix<UpdatableCharArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableCharArray(m, tileDim);
        } else if (a instanceof CharArray) {
            Matrix<CharArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixCharArray(m, tileDim);
        } else if (a instanceof UpdatableByteArray) {
            Matrix<UpdatableByteArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableByteArray(m, tileDim);
        } else if (a instanceof ByteArray) {
            Matrix<ByteArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixByteArray(m, tileDim);
        } else if (a instanceof UpdatableShortArray) {
            Matrix<UpdatableShortArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableShortArray(m, tileDim);
        } else if (a instanceof ShortArray) {
            Matrix<ShortArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixShortArray(m, tileDim);
        } else if (a instanceof UpdatableIntArray) {
            Matrix<UpdatableIntArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableIntArray(m, tileDim);
        } else if (a instanceof IntArray) {
            Matrix<IntArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixIntArray(m, tileDim);
        } else if (a instanceof UpdatableLongArray) {
            Matrix<UpdatableLongArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableLongArray(m, tileDim);
        } else if (a instanceof LongArray) {
            Matrix<LongArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixLongArray(m, tileDim);
        } else if (a instanceof UpdatableFloatArray) {
            Matrix<UpdatableFloatArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableFloatArray(m, tileDim);
        } else if (a instanceof FloatArray) {
            Matrix<FloatArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixFloatArray(m, tileDim);
        } else if (a instanceof UpdatableDoubleArray) {
            Matrix<UpdatableDoubleArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableDoubleArray(m, tileDim);
        } else if (a instanceof DoubleArray) {
            Matrix<DoubleArray> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixDoubleArray(m, tileDim);
            //[[Repeat.AutoGeneratedEnd]]
        } else if (a instanceof UpdatableObjectArray<?>) {
            Matrix<UpdatableObjectArray<Object>> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixUpdatableObjectArray<>(m, tileDim);
        } else if (a instanceof ObjectArray<?>) {
            Matrix<ObjectArray<Object>> m = InternalUtils.cast(this);
            result = new ArraysTileMatrixImpl.TileMatrixObjectArray<>(m, tileDim);
        } else {
            throw new AssertionError("Unallowed type of built-in array: " + a.getClass() + " in " + this);
        }
        return matrix(InternalUtils.<T>cast(result));
    }

    public Matrix<T> tile() {
        return tile(Matrices.defaultTileDimensions(dimCount()));
    }

    public Matrix<T> tileParent() {
        T a = array();
        if (!(a instanceof ArraysTileMatrixImpl.TileMatrixArray)) {
            throw new NotTiledMatrixException("tileParent() method must not be called "
                + "for non-tiled matrix: " + this);
        }
        Matrix<?> result = ((ArraysTileMatrixImpl.TileMatrixArray) a).baseMatrix();
        return InternalUtils.cast(result);
    }

    public long[] tileDimensions() {
        T a = array();
        if (!(a instanceof ArraysTileMatrixImpl.TileMatrixArray)) {
            throw new NotTiledMatrixException("tileDimensions() method must not be called "
                + "for non-tiled matrix: " + this);
        }
        return ((ArraysTileMatrixImpl.TileMatrixArray) a).tileDimensions();
    }

    public boolean isTiled() {
        return Arrays.isTiled(array());
    }

    public boolean isImmutable() {
        return array().isImmutable();
    }

    public boolean isCopyOnNextWrite() {
        return array().isCopyOnNextWrite();
    }

    public boolean isDirectAccessible() {
        Array a = array();
        return a instanceof DirectAccessible && ((DirectAccessible) a).hasJavaArray();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Matrix<T> clone() {
        final Matrix<UpdatableArray> result = Arrays.SMM.newMatrix(UpdatableArray.class, this);
        Matrices.copy(null, result, this);
        // - maximally fast multithreading copying
        return InternalUtils.cast(result);
    }

    public void flushResources(ArrayContext context) {
        array().flushResources(context);
    }

    public void freeResources(ArrayContext context) {
        array().freeResources(context);
    }

    public void freeResources() {
        array().freeResources(null);
    }

    @Override
    public String toString() {
        return "matrix " + Matrices.dimensionsToString(dimensions()) + " on " + array();
    }

    @Override
    public int hashCode() {
        return JArrays.arrayHashCode(dimensions(), 0, dimCount()) * 31 + array().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Matrix<?> m)) {
            return false;
        }
        return dimEquals(m) && m.array().equals(array());
    }

    static long normalizeMirrorCoord(long coord, long dim) {
        long repeatIndex = coord / dim;
        boolean mirror = (repeatIndex & 1) != 0;
        coord -= dim * repeatIndex; // = coord % dim
        if (coord < 0) { // mirror = !mirror in this case
            return mirror ? coord + dim : -coord - 1;
        } else {
            return mirror ? dim - 1 - coord : coord;
        }
    }

    static long checkDimensions(long[] dim) throws IllegalArgumentException {
        Objects.requireNonNull(dim, "Null dimensions Java array");
        if (dim.length == 0) {
            throw new IllegalArgumentException("Empty dimensions Java array");
        }
        for (int k = 0; k < dim.length; k++) {
            if (dim[k] < 0) {
                throw new IllegalArgumentException("Negative matrix dimension #" + k + ": " + dim[k]);
            }
        }
        long len = Arrays.longMul(dim);
        if (len == Long.MIN_VALUE) {
            throw new TooLargeArrayException("Too large dimensions: dim[0] * dim[1] * ... > Long.MAX_VALUE");
        }
        return len;
    }

    static void checkDimensions(long[] dim, long len) throws IllegalArgumentException {
        long correctLen = checkDimensions(dim);
        if (correctLen != len) {
            throw new SizeMismatchException("Dimensions / length mismatch: dim[0] * dim[1] * ... "
                + " = " + correctLen + ", but the array length = " + len);
        }
    }

    private Matrix<T> subMatrix(long[] from, long[] to, ContinuationMode continuationMode, boolean needCloning) {
        Objects.requireNonNull(from, "Null from[] Java array");
        Objects.requireNonNull(to, "Null to[] Java array");
        Objects.requireNonNull(continuationMode, "Null continuation mode");
        if (from.length != dimCount()) {
            throw new IllegalArgumentException("Illegal number of from[] elements: "
                + from.length + " instead " + dimCount());
        }
        if (to.length != from.length) {
            throw new IllegalArgumentException("Illegal number of to[] elements: "
                + to.length + " instead " + dimCount());
        }
        long[] dimensions = new long[from.length];
        if (needCloning) {
            from = from.clone(); // after this moment, "from" cannot be changed by parallel thread
        }
        for (int k = 0; k < from.length; k++) {
            long tok = to[k];  // unlike to[k], "tok" cannot be changed by parallel thread
            if (from[k] > tok) {
                throw new IndexOutOfBoundsException("Negative number of elements: from[" + k
                    + "] = " + from[k] + " > to[" + k + "] = " + tok
                    + " (start and end submatrix coordinate in the matrix " + this + ")");
            }
            dimensions[k] = tok - from[k];
            if (dimensions[k] < 0) {
                throw new IllegalArgumentException("Too large number of elements: to[" + k
                    + "] - from[" + k + "] > Long.MAX_VALUE "
                    + " (end and start submatrix coordinate in the matrix " + this + ")");
            }
        }
        return subMatr(from, dimensions, continuationMode, false);
    }

    private Matrix<T> subMatr(
        long[] position, long[] dimensions,
        ContinuationMode continuationMode, boolean needCloning)
    {
        Objects.requireNonNull(position, "Null position argument");
        Objects.requireNonNull(dimensions, "Null dimensions argument");
        Objects.requireNonNull(continuationMode, "Null continuation mode");
        T a = array();
        Object outsideValue = Matrices.castOutsideValue(continuationMode.isConstant() ?
            continuationMode.continuationConstant() : null, a);
        if (needCloning) { // necessary if some parallel thread is changing these arrays right now
            position = position.clone();
            dimensions = dimensions.clone();
        }
        if (continuationMode == ContinuationMode.NONE && Arrays.isNCopies(array())) {
            long len = ArraysSubMatrixImpl.checkBounds(this, position, dimensions, ContinuationMode.NONE);
            return InternalUtils.cast(Matrices.matrix(array().subArr(0, len), dimensions));
        }
        Array result;
        //[[Repeat() Bit     ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double;;
        //           Boolean ==> Character,,Byte,,Short,,Integer,,Long,,Float,,Double;;
        //           if(?=\s+\(a\s+instanceof\s+Updatable) ==> } else if,,...]]
        if (a instanceof UpdatableBitArray) {
            Matrix<UpdatableBitArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableBitArray(m, position, dimensions,
                (Boolean) outsideValue, continuationMode);
        } else if (a instanceof BitArray) {
            Matrix<BitArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixBitArray(m, position, dimensions,
                (Boolean) outsideValue, continuationMode);
            //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        } else if (a instanceof UpdatableCharArray) {
            Matrix<UpdatableCharArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableCharArray(m, position, dimensions,
                (Character) outsideValue, continuationMode);
        } else if (a instanceof CharArray) {
            Matrix<CharArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixCharArray(m, position, dimensions,
                (Character) outsideValue, continuationMode);
        } else if (a instanceof UpdatableByteArray) {
            Matrix<UpdatableByteArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableByteArray(m, position, dimensions,
                (Byte) outsideValue, continuationMode);
        } else if (a instanceof ByteArray) {
            Matrix<ByteArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixByteArray(m, position, dimensions,
                (Byte) outsideValue, continuationMode);
        } else if (a instanceof UpdatableShortArray) {
            Matrix<UpdatableShortArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableShortArray(m, position, dimensions,
                (Short) outsideValue, continuationMode);
        } else if (a instanceof ShortArray) {
            Matrix<ShortArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixShortArray(m, position, dimensions,
                (Short) outsideValue, continuationMode);
        } else if (a instanceof UpdatableIntArray) {
            Matrix<UpdatableIntArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableIntArray(m, position, dimensions,
                (Integer) outsideValue, continuationMode);
        } else if (a instanceof IntArray) {
            Matrix<IntArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixIntArray(m, position, dimensions,
                (Integer) outsideValue, continuationMode);
        } else if (a instanceof UpdatableLongArray) {
            Matrix<UpdatableLongArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableLongArray(m, position, dimensions,
                (Long) outsideValue, continuationMode);
        } else if (a instanceof LongArray) {
            Matrix<LongArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixLongArray(m, position, dimensions,
                (Long) outsideValue, continuationMode);
        } else if (a instanceof UpdatableFloatArray) {
            Matrix<UpdatableFloatArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableFloatArray(m, position, dimensions,
                (Float) outsideValue, continuationMode);
        } else if (a instanceof FloatArray) {
            Matrix<FloatArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixFloatArray(m, position, dimensions,
                (Float) outsideValue, continuationMode);
        } else if (a instanceof UpdatableDoubleArray) {
            Matrix<UpdatableDoubleArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableDoubleArray(m, position, dimensions,
                (Double) outsideValue, continuationMode);
        } else if (a instanceof DoubleArray) {
            Matrix<DoubleArray> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixDoubleArray(m, position, dimensions,
                (Double) outsideValue, continuationMode);
            //[[Repeat.AutoGeneratedEnd]]
        } else if (a instanceof UpdatableObjectArray<?>) {
            Matrix<UpdatableObjectArray<Object>> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixUpdatableObjectArray<>(m, position, dimensions,
                    outsideValue, continuationMode);
        } else if (a instanceof ObjectArray<?>) {
            Matrix<ObjectArray<Object>> m = InternalUtils.cast(this);
            result = new ArraysSubMatrixImpl.SubMatrixObjectArray<>(m, position, dimensions,
                    outsideValue, continuationMode);
        } else {
            throw new AssertionError("Unallowed type of built-in array: " + a.getClass() + " in " + this);
        }
        return Matrices.matrix(InternalUtils.<T>cast(result), dimensions);
    }
}
