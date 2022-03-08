/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Indexer for creating cyclically and pseudo-cyclically continued submatrices.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysSubMatrixCyclicIndexer implements ArraysSubMatrixIndexer {
    private static final boolean DEBUG_MODE = false; // enable 1 additional division, 1 multiplication and some asserts

    private final Array baseArray;
    private final UpdatableArray updatableBaseArray;
    private final BitArray baseBitArray;
    private final UpdatableBitArray updatableBaseBitArray;
    private final CharArray baseCharArray;
    private final UpdatableCharArray updatableBaseCharArray;
    private final ByteArray baseByteArray;
    private final UpdatableByteArray updatableBaseByteArray;
    private final ShortArray baseShortArray;
    private final UpdatableShortArray updatableBaseShortArray;
    private final IntArray baseIntArray;
    private final UpdatableIntArray updatableBaseIntArray;
    private final LongArray baseLongArray;
    private final UpdatableLongArray updatableBaseLongArray;
    private final FloatArray baseFloatArray;
    private final UpdatableFloatArray updatableBaseFloatArray;
    private final DoubleArray baseDoubleArray;
    private final UpdatableDoubleArray updatableBaseDoubleArray;
    private final ObjectArray<Object> baseObjectArray;
    private final UpdatableObjectArray<Object> updatableBaseObjectArray;
    private final long[] pos;
    private final long pos0;
    private final long[] dim;
    private final long dim0;
    private final long size;
    private final long[] baseDimMul;
    private final long[] dividers; // dividers[k] = pseudoCyclic ? baseDim[k]*baseDim[k+1]*... : baseDim[k]
    private final long baseDim0;
    private final long repeatingStep; // = dividers[0]
    private final long baseSize;
    private final boolean pseudoCyclic;

    ArraysSubMatrixCyclicIndexer(Matrix<? extends Array> baseMatrix, long[] pos, long[] dim, boolean pseudoCyclic) {
        this.baseArray = baseMatrix.array();
        this.updatableBaseArray = this.baseArray instanceof UpdatableArray ?
            (UpdatableArray) this.baseArray : null;
        this.baseBitArray = this.baseArray instanceof BitArray ? (BitArray) this.baseArray : null;
        this.updatableBaseBitArray = this.baseArray instanceof UpdatableBitArray ?
            (UpdatableBitArray) this.baseArray : null;
        this.baseCharArray = this.baseArray instanceof CharArray ? (CharArray) this.baseArray : null;
        this.updatableBaseCharArray = this.baseArray instanceof UpdatableCharArray ?
            (UpdatableCharArray) this.baseArray : null;
        this.baseByteArray = this.baseArray instanceof ByteArray ? (ByteArray) this.baseArray : null;
        this.updatableBaseByteArray = this.baseArray instanceof UpdatableByteArray ?
            (UpdatableByteArray) this.baseArray : null;
        this.baseShortArray = this.baseArray instanceof ShortArray ? (ShortArray) this.baseArray : null;
        this.updatableBaseShortArray = this.baseArray instanceof UpdatableShortArray ?
            (UpdatableShortArray) this.baseArray : null;
        this.baseIntArray = this.baseArray instanceof IntArray ? (IntArray) this.baseArray : null;
        this.updatableBaseIntArray = this.baseArray instanceof UpdatableIntArray ?
            (UpdatableIntArray) this.baseArray : null;
        this.baseLongArray = this.baseArray instanceof LongArray ? (LongArray) this.baseArray : null;
        this.updatableBaseLongArray = this.baseArray instanceof UpdatableLongArray ?
            (UpdatableLongArray) this.baseArray : null;
        this.baseFloatArray = this.baseArray instanceof FloatArray ? (FloatArray) this.baseArray : null;
        this.updatableBaseFloatArray = this.baseArray instanceof UpdatableFloatArray ?
            (UpdatableFloatArray) this.baseArray : null;
        this.baseDoubleArray = this.baseArray instanceof DoubleArray ? (DoubleArray) this.baseArray : null;
        this.updatableBaseDoubleArray = this.baseArray instanceof UpdatableDoubleArray ?
            (UpdatableDoubleArray) this.baseArray : null;
        this.baseObjectArray = this.baseArray instanceof ObjectArray<?> ?
            ((ObjectArray<?>) this.baseArray).cast(Object.class) : null;
        this.updatableBaseObjectArray = this.baseArray instanceof UpdatableObjectArray<?> ?
            ((UpdatableObjectArray<?>) this.baseArray).cast(Object.class) : null;

        //[[Repeat(IFF, ArraysSubMatrixConstantlyContinuedIndexer.java, init)
        //         this\.(baseDim)\[ ==> $1[ ;;
        //         this\.(baseDim)(?=\s*=) ==> long[] $1 !! Auto-generated: NOT EDIT !! ]]
        assert dim.length == baseMatrix.dimCount();
        assert pos.length == dim.length;
        for (int k = 0; k < pos.length; k++) {
            assert dim[k] >= 0;
            assert pos[k] <= Long.MAX_VALUE - dim[k]; // i.e. dim[k]+pos[k] does not lead to overflow
        }
        int n = dim.length;
        // collapsing the first (lowest) dimensions if the submatrix is trivial for them
        int q = 0;
        long collapsedDimensions = 1;
        while (q < dim.length - 1 && pos[q] == 0 && dim[q] == baseMatrix.dim(q)) {
            // q < dim.length - 1: we cannot collapse all dimensions into "0-dimensional" matrix
            collapsedDimensions *= dim[q];
            q++;
            n--;
        } // end of collapsing
        assert n > 0;
        long[] baseDim = new long[n];
        this.pos = new long[n];
        this.dim = new long[n];
        for (int k = 0; k < n; k++) {
            baseDim[k] = baseMatrix.dim(k + q);
            this.pos[k] = pos[k + q];
            this.dim[k] = dim[k + q];
        }
        baseDim[0] *= collapsedDimensions;
        this.pos[0] *= collapsedDimensions;
        this.dim[0] *= collapsedDimensions;
        this.baseDim0 = baseDim[0];
        this.pos0 = this.pos[0]; // important! this.pos[0], not just pos[0]
        this.dim0 = this.dim[0]; // important! this.dim[0], not just dim[0]
        this.size = Arrays.longMul(dim);
        assert this.size >= 0;
        assert this.size == Arrays.longMul(this.dim);
        this.baseDimMul = new long[n];
        for (int k = 0; k < n; k++) {
            this.baseDimMul[k] = k == 0 ? 1 : this.baseDimMul[k - 1] * baseDim[k - 1];
        }
        //[[Repeat.IncludeEnd]]
        this.dividers = new long[this.pos.length];
        for (int k = this.pos.length - 1; k >= 0; k--) {
            this.dividers[k] = k == this.pos.length - 1 || !pseudoCyclic ?
                baseDim[k] :
                this.dividers[k + 1] * baseDim[k];
        }
        this.repeatingStep = this.dividers[0];
        this.baseSize = baseArray.length();
        this.pseudoCyclic = pseudoCyclic;
        assert this.dividers[0] == (pseudoCyclic ? this.baseSize : this.baseDim0);
    }

    public boolean bitsBlocksImplemented() {
        return true;
    }

    public boolean indexOfImplemented() {
        return true;
    }

    public long translate(long index) {
        assert index >= 0 && index < size; // must be checked in getXxx/setXxx methods; so, dimMul > 0
        if (dim.length == 1) {
            //[[Repeat.SectionStart translate_method_dim1_calculations]]
            long coord = pos0 + index; // index < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }
            //[[Repeat.SectionEnd translate_method_dim1_calculations]]
            return coord;
        }
        long a = index;
        long b = a / dim0;
        long subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
        //[[Repeat.SectionStart translate_method_main_calculations]]
        long baseCoord0 = pos0 + subCoord0;
        assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
        baseCoord0 %= repeatingStep;
        if (baseCoord0 < 0) {
            baseCoord0 += repeatingStep;
        }
        // baseCoord0 = x % nx or x % (nx*ny*nz*...)
        a = b;
        long indexInBase = baseCoord0;
        int k = 1;
        for (; k < dim.length - 1; k++) {
            b = a / dim[k];
            long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
            if (DEBUG_MODE) {
                assert subCoordK >= 0;
                assert subCoordK < dim[k]; // it is a % dim[k]
                assert indexInBase >= 0;
                assert indexInBase < baseSize;
            }
            long baseCoordK = pos[k] + subCoordK;
            assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordK %= dividers[k];
            if (baseCoordK < 0) {
                baseCoordK += dividers[k];
            }
            // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
            a = b;
            indexInBase += baseCoordK * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }
        }
        assert k == dim.length - 1;
        long baseCoordLast = pos[k] + a;
        assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
        baseCoordLast %= dividers[k];
        if (baseCoordLast < 0) {
            baseCoordLast += dividers[k];
        }
        indexInBase += baseCoordLast * baseDimMul[k];
        if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
            assert pseudoCyclic; // it is impossible in the standard cyclic mode
            indexInBase -= baseSize;
            assert indexInBase >= 0;
        }
        //[[Repeat.SectionEnd translate_method_main_calculations]]
        return indexInBase;
    }//translate

    //[[Repeat.SectionStart getData_method_impl]]
    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in getData/setData methods
        assert count >= 0; // must be checked in getData/setData methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_dim1_calculations)
            //  index ==> arrayPos !! Auto-generated: NOT EDIT !! >>
            long coord = pos0 + arrayPos; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }
            //<<Repeat.IncludeEnd>>
            getDataInLine(coord, 0, destArray, destArrayOffset, count);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = arrayPos; count > 0; destArrayOffset++, index++, count--) {
        //     long indexInBase = translate(index);
        //     destArray[destArrayOffset] = getXxx(indexInBase);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        int len;
        long bLast = -157;
        for (long index = arrayPos; count > 0; index += len, destArrayOffset += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != arrayPos) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == arrayPos) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = (int) Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_main_calculations)
            // (\r(?!\n)|\n|\r\n)(\s) ==> $1    $2 !! Auto-generated: NOT EDIT !! >>
            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }
            //<<Repeat.IncludeEnd>>
            getDataInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, destArray, destArrayOffset, len);
        }
    }//getData

    private void getDataInLine(long indexInBase, long repeatingStart,
        Object destArray, int destArrayOffset, int len)
    {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        while (len > 0) {
            int m = (int) Math.min(repeatingLimit - indexInBase, len);
            baseArray.getData(indexInBase, destArray, destArrayOffset, m);
            indexInBase = repeatingStart;
            destArrayOffset += m;
            len -= m;
        }
    }
    //[[Repeat.SectionEnd getData_method_impl]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
    //  getData ==> setData;;
    //  \bbaseArray\b ==> updatableBaseArray ;;
    //  destArray ==> srcArray ;;
    //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==>
    //  setXxx(index, $1)   !! Auto-generated: NOT EDIT !! ]]
    public void setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in setData/setData methods
        assert count >= 0; // must be checked in setData/setData methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + arrayPos; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            setDataInLine(coord, 0, srcArray, srcArrayOffset, count);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = arrayPos; count > 0; srcArrayOffset++, index++, count--) {
        //     long indexInBase = translate(index);
        //     srcArray[srcArrayOffset] = getXxx(indexInBase);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        int len;
        long bLast = -157;
        for (long index = arrayPos; count > 0; index += len, srcArrayOffset += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != arrayPos) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == arrayPos) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = (int) Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            setDataInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, srcArray, srcArrayOffset, len);
        }
    }//setData

    private void setDataInLine(long indexInBase, long repeatingStart,
        Object srcArray, int srcArrayOffset, int len)
    {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        while (len > 0) {
            int m = (int) Math.min(repeatingLimit - indexInBase, len);
            updatableBaseArray.setData(indexInBase, srcArray, srcArrayOffset, m);
            indexInBase = repeatingStart;
            srcArrayOffset += m;
            len -= m;
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
    //  \bint\s+(destArrayOffset|count|len|m)\b ==> long $1 ;;
    //  \bObject\s+destArray\b ==> long[] destArray ;;
    //  \bbaseArray\b ==> baseBitArray ;;
    //  \(int\)\s* ==> ;;
    //  getData ==> getBits !! Auto-generated: NOT EDIT !! ]]
    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in getBits/setData methods
        assert count >= 0; // must be checked in getBits/setData methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + arrayPos; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            getBitsInLine(coord, 0, destArray, destArrayOffset, count);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = arrayPos; count > 0; destArrayOffset++, index++, count--) {
        //     long indexInBase = translate(index);
        //     destArray[destArrayOffset] = getXxx(indexInBase);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = arrayPos; count > 0; index += len, destArrayOffset += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != arrayPos) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == arrayPos) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            getBitsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, destArray, destArrayOffset, len);
        }
    }//getBits

    private void getBitsInLine(long indexInBase, long repeatingStart,
        long[] destArray, long destArrayOffset, long len)
    {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        while (len > 0) {
            long m = Math.min(repeatingLimit - indexInBase, len);
            baseBitArray.getBits(indexInBase, destArray, destArrayOffset, m);
            indexInBase = repeatingStart;
            destArrayOffset += m;
            len -= m;
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
    //  getData ==> setBits;;
    //  \bbaseArray\b ==> updatableBaseBitArray ;;
    //  destArray ==> srcArray ;;
    //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==> setXxx(index, $1) ;;
    //  \bint\s+(srcArrayOffset|count|len|m)\b ==> long $1 ;;
    //  \bObject\s+srcArray\b ==> long[] srcArray ;;
    //  \(int\)\s* ==>   !! Auto-generated: NOT EDIT !! ]]
    public void setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in setBits/setData methods
        assert count >= 0; // must be checked in setBits/setData methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + arrayPos; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            setBitsInLine(coord, 0, srcArray, srcArrayOffset, count);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = arrayPos; count > 0; srcArrayOffset++, index++, count--) {
        //     long indexInBase = translate(index);
        //     srcArray[srcArrayOffset] = getXxx(indexInBase);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = arrayPos; count > 0; index += len, srcArrayOffset += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != arrayPos) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == arrayPos) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            setBitsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, srcArray, srcArrayOffset, len);
        }
    }//setBits

    private void setBitsInLine(long indexInBase, long repeatingStart,
        long[] srcArray, long srcArrayOffset, long len)
    {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        while (len > 0) {
            long m = Math.min(repeatingLimit - indexInBase, len);
            updatableBaseBitArray.setBits(indexInBase, srcArray, srcArrayOffset, m);
            indexInBase = repeatingStart;
            srcArrayOffset += m;
            len -= m;
        }
    }
    //[[Repeat.IncludeEnd]]


    //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
    public long indexOfBit(long lowIndex, long highIndex, boolean value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_dim1_calculations)
            //  index ==> lowIndex !! Auto-generated: NOT EDIT !! >>
            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }
            //<<Repeat.IncludeEnd>>
            long result = indexOfBitInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_main_calculations)
            // (\r(?!\n)|\n|\r\n)(\s) ==> $1    $2 !! Auto-generated: NOT EDIT !! >>
            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }
            //<<Repeat.IncludeEnd>>
            long result = indexOfBitInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfBitInLine(long indexInBase, long repeatingStart, long len, boolean value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseBitArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseBitArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseBitArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_dim1_calculations)
            //  index ==> highIndex !! Auto-generated: NOT EDIT !! >>
            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }
            //<<Repeat.IncludeEnd>>
            long result = lastIndexOfBitInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_main_calculations)
            // (\r(?!\n)|\n|\r\n)(\s) ==> $1    $2 !! Auto-generated: NOT EDIT !! >>
            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }
            //<<Repeat.IncludeEnd>>
            long result = lastIndexOfBitInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfBitInLine(long indexInBase, long repeatingStart, long len, boolean value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseBitArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseBitArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseBitArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillBits(long position, long count, boolean value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_dim1_calculations)
            //  index ==> position !! Auto-generated: NOT EDIT !! >>
            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }
            //<<Repeat.IncludeEnd>>
            fillBitsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;
            //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_main_calculations)
            // (\r(?!\n)|\n|\r\n)(\s) ==> $1    $2 !! Auto-generated: NOT EDIT !! >>
            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }
            //<<Repeat.IncludeEnd>>
            fillBitsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillBitsInLine(long indexInBase, long repeatingStart, long len, boolean value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseBitArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseBitArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseBitArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    public long indexOfChar(long lowIndex, long highIndex, char value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfCharInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfCharInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfCharInLine(long indexInBase, long repeatingStart, long len, char value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseCharArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseCharArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseCharArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfCharInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfCharInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfCharInLine(long indexInBase, long repeatingStart, long len, char value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseCharArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseCharArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseCharArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillChars(long position, long count, char value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillCharsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillCharsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillCharsInLine(long indexInBase, long repeatingStart, long len, char value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseCharArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseCharArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseCharArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfByte(long lowIndex, long highIndex, byte value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfByteInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfByteInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfByteInLine(long indexInBase, long repeatingStart, long len, byte value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseByteArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseByteArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseByteArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfByteInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfByteInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfByteInLine(long indexInBase, long repeatingStart, long len, byte value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseByteArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseByteArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseByteArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillBytes(long position, long count, byte value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillBytesInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillBytesInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillBytesInLine(long indexInBase, long repeatingStart, long len, byte value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseByteArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseByteArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseByteArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfShort(long lowIndex, long highIndex, short value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfShortInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfShortInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfShortInLine(long indexInBase, long repeatingStart, long len, short value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseShortArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseShortArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseShortArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfShortInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfShortInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfShortInLine(long indexInBase, long repeatingStart, long len, short value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseShortArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseShortArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseShortArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillShorts(long position, long count, short value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillShortsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillShortsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillShortsInLine(long indexInBase, long repeatingStart, long len, short value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseShortArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseShortArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseShortArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfInt(long lowIndex, long highIndex, int value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfIntInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfIntInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfIntInLine(long indexInBase, long repeatingStart, long len, int value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseIntArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseIntArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseIntArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfIntInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfIntInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfIntInLine(long indexInBase, long repeatingStart, long len, int value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseIntArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseIntArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseIntArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillInts(long position, long count, int value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillIntsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillIntsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillIntsInLine(long indexInBase, long repeatingStart, long len, int value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseIntArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseIntArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseIntArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfLong(long lowIndex, long highIndex, long value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfLongInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfLongInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfLongInLine(long indexInBase, long repeatingStart, long len, long value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseLongArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseLongArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseLongArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfLongInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfLongInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfLongInLine(long indexInBase, long repeatingStart, long len, long value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseLongArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseLongArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseLongArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillLongs(long position, long count, long value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillLongsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillLongsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillLongsInLine(long indexInBase, long repeatingStart, long len, long value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseLongArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseLongArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseLongArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfFloat(long lowIndex, long highIndex, float value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfFloatInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfFloatInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfFloatInLine(long indexInBase, long repeatingStart, long len, float value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseFloatArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseFloatArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseFloatArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfFloatInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfFloatInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfFloatInLine(long indexInBase, long repeatingStart, long len, float value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseFloatArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseFloatArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseFloatArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillFloats(long position, long count, float value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillFloatsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillFloatsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillFloatsInLine(long indexInBase, long repeatingStart, long len, float value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseFloatArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseFloatArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseFloatArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfDouble(long lowIndex, long highIndex, double value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfDoubleInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfDoubleInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfDoubleInLine(long indexInBase, long repeatingStart, long len, double value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseDoubleArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseDoubleArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseDoubleArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfDoubleInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfDoubleInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfDoubleInLine(long indexInBase, long repeatingStart, long len, double value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseDoubleArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseDoubleArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseDoubleArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillDoubles(long position, long count, double value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillDoublesInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillDoublesInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillDoublesInLine(long indexInBase, long repeatingStart, long len, double value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseDoubleArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseDoubleArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseDoubleArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfObject(long lowIndex, long highIndex, Object value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        if (dim.length == 1) {

            long coord = pos0 + lowIndex; // lowIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = indexOfObjectInLine(coord, 0, count, value);
            return result == -1 ? -1 : lowIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = lowIndex; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != lowIndex) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == lowIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = indexOfObjectInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != -1) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    private long indexOfObjectInLine(long indexInBase, long repeatingStart, long len, Object value) {
        if (len <= 0) {
            return -1;
        }
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            return baseObjectArray.indexOf(indexInBase, indexInBase + len, value);
        }
        long result = baseObjectArray.indexOf(indexInBase, repeatingLimit, value);
        if (result != -1) {
            return result;
        }
        result = baseObjectArray.indexOf(repeatingStart,
            len >= repeatingStep ? indexInBase : indexInBase + len - repeatingStep, value);
        if (result != -1) {
            return repeatingStep + result;
        }
        // no sense to search farther: the following elements will repeat
        return -1;
    }

    public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
        if (lowIndex < 0) {
            lowIndex = 0;
        }
        if (highIndex > size) {
            highIndex = size;
        }
        if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
            return -1;
        }
        if (baseSize == 0) { // necessary to guarantee that dividers[...] != 0 below
            return -1;
        }
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {

            long coord = pos0 + highIndex; // highIndex < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            long result = lastIndexOfObjectInLine(coord, 0, count, value);
            return result == Long.MAX_VALUE ? -1 : highIndex + result - coord;
        }
        long len;
        long bLast = -157;
        for (long index = highIndex; count > 0; index -= len, count -= len) {
            // Here we at the end of some line in submatrix, excepting, maybe,
            // the last line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != highIndex) {
                    assert subCoord0 == dim0 - 1 :
                        "indexing bug: subCoord0 = " + subCoord0 + " != dim[0]-1 = " + (dim0 - 1);
                    assert b == bLast - 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == highIndex) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast - 1;
                    subCoord0 = dim0 - 1;
                }
            }
            bLast = b;
            len = Math.min(subCoord0 + 1, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            long result = lastIndexOfObjectInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
            if (result != Long.MAX_VALUE) {
                return index + result - indexInBase;
            }
        }
        return -1;
    }

    // Here indexInBase refers to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfObjectInLine(long indexInBase, long repeatingStart, long len, Object value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        assert indexInBase >= repeatingStart && indexInBase < repeatingStart + repeatingStep;
        if (len <= indexInBase - repeatingStart + 1) {
            long result = baseObjectArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
            return result == -1 ? Long.MAX_VALUE : result;
        }
        long result = baseObjectArray.lastIndexOf(repeatingStart, indexInBase + 1, value);
        if (result != -1) {
            return result;
        }
        result = baseObjectArray.lastIndexOf(
            len >= repeatingStep ? indexInBase + 1 : indexInBase + 1 + repeatingStep - len,
            repeatingStart + repeatingStep, value);
        if (result != -1) {
            return result - repeatingStep;
        }
        return Long.MAX_VALUE;
    }

    public void fillObjects(long position, long count, Object value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }

            long coord = pos0 + position; // position < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord %= baseDim0;
            if (coord < 0) {
                coord += baseDim0;
            }

            fillObjectsInLine(coord, 0, count, value);
            return;
        }
        // We need to perform the following loop:
        //
        // for (long index = position; count > 0; index++, count--) {
        //     long indexInBase = translate(index);
        //     setXxx(indexInBase, value);
        // }
        // Below is an optimization of such a loop, processing contiguous data blocks

        long len;
        long bLast = -157;
        for (long index = position; count > 0; index += len, count -= len) {
            // Here we at the beginning of some line in submatrix, excepting, maybe,
            // the first line, where we can be at its middle point
            long a = index;
            long b, subCoord0;
            if (DEBUG_MODE) {
                b = a / dim0;
                subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                if (index != position) {
                    assert subCoord0 == 0 : "indexing bug: non-zero subCoord0 = " + subCoord0;
                    assert b == bLast + 1 : "indexing bug: strange change of index / dim[0]";
                }
            } else {
                if (index == position) {
                    b = a / dim0;
                    subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
                } else {
                    b = bLast + 1;
                    subCoord0 = 0;
                }
            }
            bLast = b;
            len = Math.min(dim0 - subCoord0, count);
            // len is the maximal length of the nearest contiguous data block since this index
            assert len > 0 : "zero len = " + len;

            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 %= repeatingStep;
            if (baseCoord0 < 0) {
                baseCoord0 += repeatingStep;
            }
            // baseCoord0 = x % nx or x % (nx*ny*nz*...)
            a = b;
            long indexInBase = baseCoord0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexInBase >= 0;
                    assert indexInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK %= dividers[k];
                if (baseCoordK < 0) {
                    baseCoordK += dividers[k];
                }
                // baseCoord1 = y % ny or y % (ny*nz*...), baseCoord2 = z % nz or z % (nz*...)
                a = b;
                indexInBase += baseCoordK * baseDimMul[k];
                if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                    assert pseudoCyclic; // it is impossible in the standard cyclic mode
                    indexInBase -= baseSize;
                    assert indexInBase >= 0;
                }
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast %= dividers[k];
            if (baseCoordLast < 0) {
                baseCoordLast += dividers[k];
            }
            indexInBase += baseCoordLast * baseDimMul[k];
            if (indexInBase < 0 || indexInBase >= baseSize) { // out of 0..baseSize-1, maybe due to overflow
                assert pseudoCyclic; // it is impossible in the standard cyclic mode
                indexInBase -= baseSize;
                assert indexInBase >= 0;
            }

            fillObjectsInLine(indexInBase, pseudoCyclic ? 0 : indexInBase - baseCoord0, len, value);
        }
    }

    private void fillObjectsInLine(long indexInBase, long repeatingStart, long len, Object value) {
        long repeatingLimit = repeatingStart + repeatingStep;
        assert indexInBase >= repeatingStart && indexInBase < repeatingLimit;
        if (len <= repeatingLimit - indexInBase) {
            updatableBaseObjectArray.fill(indexInBase, len, value);
            return;
        }
        updatableBaseObjectArray.fill(indexInBase, repeatingLimit - indexInBase, value);
        updatableBaseObjectArray.fill(repeatingStart,
            len >= repeatingStep ? indexInBase - repeatingStart : len - (repeatingLimit - indexInBase), value);
        // no sense to fill farther: the following elements will repeat
    }

    //[[Repeat.AutoGeneratedEnd]]
}
