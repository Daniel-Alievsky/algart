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

import java.util.Objects;

/**
 * <p>Indexer for creating usual and constantly continued submatrices.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysSubMatrixConstantlyContinuedIndexer implements ArraysSubMatrixIndexer {
    private static final boolean DEBUG_MODE = false; // enable 1 additional division, 1 multiplication and some asserts

    private static final long OUTSIDE_INDEX = ArraysSubMatrixImpl.OUTSIDE_INDEX;

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
    private final long[] baseDim;
    private final long baseDim0;
    private final long[] baseDimMul;
    private final Array outsideConst;
    private final boolean outsideBitConst;
    private final char outsideCharConst;
    private final byte outsideByteConst;
    private final short outsideShortConst;
    private final int outsideIntConst;
    private final long outsideLongConst;
    private final float outsideFloatConst;
    private final double outsideDoubleConst;
    private final Object outsideObjectConst;

    ArraysSubMatrixConstantlyContinuedIndexer(Matrix<? extends Array> baseMatrix,
        long[] pos, long[] dim, Array outsideConst)
    {
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

        //[[Repeat.SectionStart init]]
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
        this.baseDim = new long[n];
        this.pos = new long[n];
        this.dim = new long[n];
        for (int k = 0; k < n; k++) {
            this.baseDim[k] = baseMatrix.dim(k + q);
            this.pos[k] = pos[k + q];
            this.dim[k] = dim[k + q];
        }
        this.baseDim[0] *= collapsedDimensions;
        this.pos[0] *= collapsedDimensions;
        this.dim[0] *= collapsedDimensions;
        this.baseDim0 = this.baseDim[0];
        this.pos0 = this.pos[0]; // important! this.pos[0], not just pos[0]
        this.dim0 = this.dim[0]; // important! this.dim[0], not just dim[0]
        this.size = Arrays.longMul(dim);
        assert this.size >= 0;
        assert this.size == Arrays.longMul(this.dim);
        this.baseDimMul = new long[n];
        for (int k = 0; k < n; k++) {
            this.baseDimMul[k] = k == 0 ? 1 : this.baseDimMul[k - 1] * this.baseDim[k - 1];
        }
        //[[Repeat.SectionEnd init]]
        this.outsideConst = outsideConst;
        long outsideLength = outsideConst.length();
        this.outsideBitConst = outsideConst instanceof BitArray && outsideLength > 0
            && ((BitArray)outsideConst).getBit(0);
        this.outsideCharConst = outsideConst instanceof CharArray && outsideLength > 0 ?
            ((CharArray)outsideConst).getChar(0) : 0;
        this.outsideByteConst = outsideConst instanceof ByteArray && outsideLength > 0 ?
            (byte)((ByteArray)outsideConst).getByte(0) : 0;
        this.outsideShortConst = outsideConst instanceof ShortArray && outsideLength > 0 ?
            (short)((ShortArray)outsideConst).getShort(0) : 0;
        this.outsideIntConst = outsideConst instanceof IntArray && outsideLength > 0 ?
            ((IntArray)outsideConst).getInt(0) : 0;
        this.outsideLongConst = outsideConst instanceof LongArray && outsideLength > 0 ?
            ((LongArray)outsideConst).getLong(0) : 0;
        this.outsideFloatConst = outsideConst instanceof FloatArray && outsideLength > 0 ?
            ((FloatArray)outsideConst).getFloat(0) : 0;
        this.outsideDoubleConst = outsideConst instanceof DoubleArray && outsideLength > 0 ?
            ((DoubleArray)outsideConst).getDouble(0) : 0;
        this.outsideObjectConst = outsideConst instanceof ObjectArray<?> && outsideLength > 0 ?
            ((ObjectArray<?>)outsideConst).get(0) : 0;
    }

    public boolean bitsBlocksImplemented() {
        return true;
    }

    public boolean indexOfImplemented() {
        return true;
    }

    public long translate(long index) {
        assert index >= 0 && index < size; // must be checked in getXxx/setXxx methods
        if (dim.length == 1) {
            long coord = pos0 + index;
            return coord < 0 || coord >= baseDim0 ? OUTSIDE_INDEX : coord;
        }
        long a = index;
        long b = a / dim0;
        long baseCoord0 = pos0 + a - b * dim0;  // faster equivalent of "pos0 + a % dim0"
        if (baseCoord0 < 0 || baseCoord0 >= baseDim0) {
            // About overflows here and below. We check pos[k] + x, where x >= 0, pos[k] is any long.
            // Overflow possible only if pos[k] >= 0 and pos[k] + x > Long.MAX_VALUE.
            // This case leads to the negative result and is processed by the check "< 0" here.
            return OUTSIDE_INDEX;
        }
        a = b;
        long indexInBase = baseCoord0;
        int k = 1;
        for (; k < dim.length - 1; k++) {
            b = a / dim[k];
            long baseCoordK = pos[k] + a - b * dim[k]; // faster equivalent of "pos[k] + a % dim[k]"
            if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                return OUTSIDE_INDEX;
            }
            a = b;
            indexInBase += baseCoordK * baseDimMul[k];
        }
        assert k == dim.length - 1;
        long baseCoordLast = pos[k] + a;
        if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
            return OUTSIDE_INDEX;
        }
        indexInBase += baseCoordLast * baseDimMul[k];
        return indexInBase;
    }//translate

    //[[Repeat.SectionStart getData_method_impl]]
    public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in getData/setData methods
        assert count >= 0; // must be checked in getData/setData methods
        if (dim.length == 1) {
            // overflow impossible, because arrayPos+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            getDataInLine(arrayPos + pos0, arrayPos + pos0, destArray, destArrayOffset, count);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                outsideConst.getData(0, destArray, destArrayOffset, len);
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                getDataInLine(indexInBase, baseCoord0, destArray, destArrayOffset, len);
            }
        }
    }//getData

    private void getDataInLine(long indexInBase, long baseCoord0, Object destArray, int destArrayOffset, int len) {
        if (baseCoord0 < 0 && len > 0) {
            int m = (int) Math.min(-baseCoord0, len);
            outsideConst.getData(0, destArray, destArrayOffset, m);
            baseCoord0 += m;
            indexInBase += m;
            destArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0 && len > 0) {
            int m = (int) Math.min(baseDim0 - baseCoord0, len);
            baseArray.getData(indexInBase, destArray, destArrayOffset, m);
            baseCoord0 += m;
            destArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            outsideConst.getData(0, destArray, destArrayOffset, len);
        }
    }
    //[[Repeat.SectionEnd getData_method_impl]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
    //  getData ==> setData;;
    //  \bbaseArray\b ==> updatableBaseArray ;;
    //  destArray ==> srcArray ;;
    //  outsideConst.setData\(.*?\)\; ==> \/\/ nothing to do! ;;
    //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==>
    //  setXxx(index, $1)   !! Auto-generated: NOT EDIT !! ]]
    public void setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in setData/setData methods
        assert count >= 0; // must be checked in setData/setData methods
        if (dim.length == 1) {
            // overflow impossible, because arrayPos+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            setDataInLine(arrayPos + pos0, arrayPos + pos0, srcArray, srcArrayOffset, count);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                // nothing to do!
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                setDataInLine(indexInBase, baseCoord0, srcArray, srcArrayOffset, len);
            }
        }
    }//setData

    private void setDataInLine(long indexInBase, long baseCoord0, Object srcArray, int srcArrayOffset, int len) {
        if (baseCoord0 < 0 && len > 0) {
            int m = (int) Math.min(-baseCoord0, len);
            // nothing to do!
            baseCoord0 += m;
            indexInBase += m;
            srcArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0 && len > 0) {
            int m = (int) Math.min(baseDim0 - baseCoord0, len);
            updatableBaseArray.setData(indexInBase, srcArray, srcArrayOffset, m);
            baseCoord0 += m;
            srcArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            // nothing to do!
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
    //  \bint\s+(destArrayOffset|count|len|m)\b ==> long $1 ;;
    //  \bObject\s+destArray\b ==> long[] destArray ;;
    //  \bbaseArray\b ==> baseBitArray ;;
    //  \(int\)\s* ==> ;;
    //  getData ==> getBits;;
    //  outsideConst\.getBits\(\w+,\s*(\w+),(\s*\w+),(\s*\w+)\) ==>
    //  PackedBitArrays.fillBits($1, $2, $3, outsideBitConst)   !! Auto-generated: NOT EDIT !! ]]
    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in getBits/setData methods
        assert count >= 0; // must be checked in getBits/setData methods
        if (dim.length == 1) {
            // overflow impossible, because arrayPos+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            getBitsInLine(arrayPos + pos0, arrayPos + pos0, destArray, destArrayOffset, count);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                PackedBitArrays.fillBits(destArray,  destArrayOffset,  len, outsideBitConst);
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                getBitsInLine(indexInBase, baseCoord0, destArray, destArrayOffset, len);
            }
        }
    }//getBits

    private void getBitsInLine(long indexInBase, long baseCoord0, long[] destArray, long destArrayOffset, long len) {
        if (baseCoord0 < 0 && len > 0) {
            long m = Math.min(-baseCoord0, len);
            PackedBitArrays.fillBits(destArray,  destArrayOffset,  m, outsideBitConst);
            baseCoord0 += m;
            indexInBase += m;
            destArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            baseBitArray.getBits(indexInBase, destArray, destArrayOffset, m);
            baseCoord0 += m;
            destArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            PackedBitArrays.fillBits(destArray,  destArrayOffset,  len, outsideBitConst);
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
    //  getData ==> setBits;;
    //  \bbaseArray\b ==> updatableBaseBitArray ;;
    //  destArray ==> srcArray ;;
    //  outsideConst.setBits\(.*?\)\; ==> \/\/ nothing to do! ;;
    //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==> setXxx(index, $1) ;;
    //  \bint\s+(srcArrayOffset|count|len|m)\b ==> long $1 ;;
    //  \bObject\s+srcArray\b ==> long[] srcArray ;;
    //  \(int\)\s* ==>   !! Auto-generated: NOT EDIT !! ]]
    public void setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in setBits/setData methods
        assert count >= 0; // must be checked in setBits/setData methods
        if (dim.length == 1) {
            // overflow impossible, because arrayPos+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            setBitsInLine(arrayPos + pos0, arrayPos + pos0, srcArray, srcArrayOffset, count);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                // nothing to do!
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                setBitsInLine(indexInBase, baseCoord0, srcArray, srcArrayOffset, len);
            }
        }
    }//setBits

    private void setBitsInLine(long indexInBase, long baseCoord0, long[] srcArray, long srcArrayOffset, long len) {
        if (baseCoord0 < 0 && len > 0) {
            long m = Math.min(-baseCoord0, len);
            // nothing to do!
            baseCoord0 += m;
            indexInBase += m;
            srcArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseBitArray.setBits(indexInBase, srcArray, srcArrayOffset, m);
            baseCoord0 += m;
            srcArrayOffset += m;
            len -= m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            // nothing to do!
        }
    }
    //[[Repeat.IncludeEnd]]

    //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object;;
    //           (value)\s*==\s*(outsideObjectConst) ==> Objects.equals($1, $2),,...]]
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfBitInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideBitConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfBitInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfBitInLine(long indexInBase, long baseCoord0, long len, boolean value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideBitConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseBitArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideBitConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfBitInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideBitConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfBitInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfBitInLine(long indexInBase, long baseCoord0, long len, boolean value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideBitConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseBitArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideBitConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillBits(long position, long count, boolean value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillBitsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillBitsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillBits

    private void fillBitsInLine(long indexInBase, long baseCoord0, long len, boolean value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseBitArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfCharInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideCharConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfCharInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfCharInLine(long indexInBase, long baseCoord0, long len, char value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideCharConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseCharArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideCharConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfCharInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideCharConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfCharInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfCharInLine(long indexInBase, long baseCoord0, long len, char value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideCharConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseCharArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideCharConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillChars(long position, long count, char value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillCharsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillCharsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillChars

    private void fillCharsInLine(long indexInBase, long baseCoord0, long len, char value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseCharArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfByteInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideByteConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfByteInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfByteInLine(long indexInBase, long baseCoord0, long len, byte value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideByteConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseByteArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideByteConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfByteInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideByteConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfByteInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfByteInLine(long indexInBase, long baseCoord0, long len, byte value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideByteConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseByteArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideByteConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillBytes(long position, long count, byte value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillBytesInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillBytesInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillBytes

    private void fillBytesInLine(long indexInBase, long baseCoord0, long len, byte value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseByteArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfShortInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideShortConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfShortInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfShortInLine(long indexInBase, long baseCoord0, long len, short value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideShortConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseShortArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideShortConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfShortInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideShortConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfShortInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfShortInLine(long indexInBase, long baseCoord0, long len, short value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideShortConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseShortArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideShortConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillShorts(long position, long count, short value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillShortsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillShortsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillShorts

    private void fillShortsInLine(long indexInBase, long baseCoord0, long len, short value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseShortArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfIntInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideIntConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfIntInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfIntInLine(long indexInBase, long baseCoord0, long len, int value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideIntConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseIntArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideIntConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfIntInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideIntConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfIntInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfIntInLine(long indexInBase, long baseCoord0, long len, int value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideIntConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseIntArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideIntConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillInts(long position, long count, int value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillIntsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillIntsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillInts

    private void fillIntsInLine(long indexInBase, long baseCoord0, long len, int value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseIntArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfLongInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideLongConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfLongInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfLongInLine(long indexInBase, long baseCoord0, long len, long value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideLongConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseLongArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideLongConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfLongInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideLongConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfLongInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfLongInLine(long indexInBase, long baseCoord0, long len, long value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideLongConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseLongArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideLongConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillLongs(long position, long count, long value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillLongsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillLongsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillLongs

    private void fillLongsInLine(long indexInBase, long baseCoord0, long len, long value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseLongArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfFloatInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideFloatConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfFloatInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfFloatInLine(long indexInBase, long baseCoord0, long len, float value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideFloatConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseFloatArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideFloatConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfFloatInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideFloatConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfFloatInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfFloatInLine(long indexInBase, long baseCoord0, long len, float value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideFloatConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseFloatArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideFloatConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillFloats(long position, long count, float value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillFloatsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillFloatsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillFloats

    private void fillFloatsInLine(long indexInBase, long baseCoord0, long len, float value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseFloatArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfDoubleInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideDoubleConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfDoubleInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfDoubleInLine(long indexInBase, long baseCoord0, long len, double value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (value == outsideDoubleConst) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseDoubleArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (value == outsideDoubleConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfDoubleInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (value == outsideDoubleConst) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfDoubleInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfDoubleInLine(long indexInBase, long baseCoord0, long len, double value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (value == outsideDoubleConst) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseDoubleArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (value == outsideDoubleConst) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillDoubles(long position, long count, double value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillDoublesInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillDoublesInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillDoubles

    private void fillDoublesInLine(long indexInBase, long baseCoord0, long len, double value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseDoubleArray.fill(indexInBase, m, value);
        }
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
        long count = highIndex - lowIndex;
        if (dim.length == 1) {
            long result = indexOfObjectInLine(lowIndex + pos0, lowIndex + pos0, count, value);
            // overflow impossible, because lowIndex<=highIndex, and
            // highIndex+pos0 < pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (Objects.equals(value, outsideObjectConst)) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = indexOfObjectInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+len<=Long.MAX_VALUE
    private long indexOfObjectInLine(long indexInBase, long baseCoord0, long len, Object value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 < 0) {
            long m = -baseCoord0;
            if (Objects.equals(value, outsideObjectConst)) {
                return indexInBase;
            }
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= 0 && baseCoord0 < baseDim0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            long result = baseObjectArray.indexOf(indexInBase, indexInBase + m, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 += m;
            indexInBase += m;
        }
        if (baseCoord0 >= baseDim0 && len > 0) {
            if (Objects.equals(value, outsideObjectConst)) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
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
        long count = highIndex - lowIndex;
        highIndex--;
        if (dim.length == 1) {
            long result = lastIndexOfObjectInLine(highIndex + pos0, highIndex + pos0, count, value);
            // overflow impossible: highIndex+pos0 < pos0+size = pos0+dim0 <= Long.MAX_VALUE in any submatrix
            return result == Long.MAX_VALUE ? -1 : result - pos0;
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 < 0 || baseCoord0 - len >= baseDim0) {
                // overflow impossible in "baseCoord0 - len", because it is checked only when baseCoord0 >= 0
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (fullLineOutside) { // then indexInBase has no sense
                if (Objects.equals(value, outsideObjectConst)) {
                    return index;
                }
            } else {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                long result = lastIndexOfObjectInLine(indexInBase, baseCoord0, len, value);
                if (result != Long.MAX_VALUE) {
                    return index + result - indexInBase;
                }
            }
        }
        return -1;
    }

    // Here indexInBase and baseCoord0 refer to the LAST element in the line, and indexInBase<=Long.MAX_VALUE-1.
    // Returns Long.MAX_VALUE instead of -1, because results can be negative.
    // Long.MAX_VALUE cannot be a successful result, because indexInBase+1<=Long.MAX_VALUE
    private long lastIndexOfObjectInLine(long indexInBase, long baseCoord0, long len, Object value) {
        if (len <= 0) {
            return Long.MAX_VALUE;
        }
        if (baseCoord0 >= baseDim0) {
            if (Objects.equals(value, outsideObjectConst)) {
                return indexInBase;
            }
            long m = baseCoord0 - baseDim0 + 1;
            len -= m;
            if (len <= 0) {
                return Long.MAX_VALUE;
            }
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < baseDim0 && baseCoord0 >= 0) {
            long m = Math.min(baseCoord0 + 1, len);
            long result = baseObjectArray.lastIndexOf(indexInBase + 1 - m, indexInBase + 1, value);
            if (result != -1) {
                return result;
            }
            len -= m;
            baseCoord0 -= m;
            indexInBase -= m;
        }
        if (baseCoord0 < 0 && len > 0) {
            if (Objects.equals(value, outsideObjectConst)) {
                return indexInBase;
            }
        }
        return Long.MAX_VALUE;
    }

    public void fillObjects(long position, long count, Object value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            fillObjectsInLine(position + pos0, position + pos0, count, value);
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
            long baseCoord0 = pos0 + subCoord0; // overflow impossible here, because subCoord0 = a % dim0 < dim0
            long indexInBase = baseCoord0;
            boolean fullLineOutside;
            if (baseCoord0 <= -len || baseCoord0 >= baseDim0) {
                fullLineOutside = true;
            } else {
                a = b;
                BaseIndexCalculation:
                {
                    fullLineOutside = false;
                    int k = 1;
                    for (; k < dim.length - 1; k++) {
                        b = a / dim[k];
                        long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                        long baseCoordK = pos[k] + subCoordK;
                        if (baseCoordK < 0 || baseCoordK >= baseDim[k]) {
                            fullLineOutside = true;
                            break BaseIndexCalculation;
                        }
                        a = b;
                        indexInBase += baseCoordK * baseDimMul[k];
                    }
                    assert k == dim.length - 1;
                    long baseCoordLast = pos[k] + a;
                    if (baseCoordLast < 0 || baseCoordLast >= baseDim[k]) {
                        fullLineOutside = true;
                        break BaseIndexCalculation;
                    }
                    indexInBase += baseCoordLast * baseDimMul[k];
                }
            }
            if (!fullLineOutside) {
                // indexInBase-baseCoord0 is an index of 1st element in an existing line of the base matrix;
                // indexInBase is this index + baseCoord0, which is in range -Long.MAX_VALUE <= baseCoord0 < baseDim0,
                // so it is also represented correctly (without overflow)
                fillObjectsInLine(indexInBase, baseCoord0, len, value);
            }
        }
    }//fillObjects

    private void fillObjectsInLine(long indexInBase, long baseCoord0, long len, Object value) {
        if (baseCoord0 < 0 && len > 0) {
            len += baseCoord0;
            if (len <= 0) {
                return;
            }
            indexInBase -= baseCoord0;
            baseCoord0 = 0;
        }
        if (baseCoord0 < baseDim0 && len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            updatableBaseObjectArray.fill(indexInBase, m, value);
        }
    }

    //[[Repeat.AutoGeneratedEnd]]
}
