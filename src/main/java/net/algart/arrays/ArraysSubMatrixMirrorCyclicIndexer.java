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
 * <p>Indexer for creating mirror-cyclically continued submatrices.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysSubMatrixMirrorCyclicIndexer implements ArraysSubMatrixIndexer {
    private static final boolean DEBUG_MODE = false; // enable 1 additional division, 1 multiplication and some asserts

    private static final int MC_INDEXER_BUFFER_LENGTH = DEBUG_MODE ? 64 : 8192;

    private static final JArrayPool BOOLEAN_BUFFERS = JArrayPool.getInstance(boolean.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool CHAR_BUFFERS = JArrayPool.getInstance(char.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, MC_INDEXER_BUFFER_LENGTH);
    private static final JArrayPool OBJECT_BUFFERS = JArrayPool.getInstance(Object.class, MC_INDEXER_BUFFER_LENGTH);

    private final Array baseArray;
    private final BitArray baseBitArray;
    private final UpdatableArray updatableBaseArray;
    private final UpdatableBitArray updatableBaseBitArray;
    private final UpdatableCharArray updatableBaseCharArray;
    private final UpdatableByteArray updatableBaseByteArray;
    private final UpdatableShortArray updatableBaseShortArray;
    private final UpdatableIntArray updatableBaseIntArray;
    private final UpdatableLongArray updatableBaseLongArray;
    private final UpdatableFloatArray updatableBaseFloatArray;
    private final UpdatableDoubleArray updatableBaseDoubleArray;
    private final UpdatableObjectArray<Object> updatableBaseObjectArray;
    private final long[] pos;
    private final long pos0;
    private final long[] dim;
    private final long dim0;
    private final long size;
    private final long[] baseDimMul;
    private final long[] baseDim;
    private final long baseDim0;
    private final long baseSize;
    private final ArrayReverser reverser;
    private final JArrayPool pool;

    ArraysSubMatrixMirrorCyclicIndexer(Matrix<? extends Array> baseMatrix, long[] pos, long[] dim) {
        this.baseArray = baseMatrix.array();
        this.baseBitArray = this.baseArray instanceof BitArray ? (BitArray) this.baseArray : null;
        this.updatableBaseArray = this.baseArray instanceof UpdatableArray ?
            (UpdatableArray) this.baseArray : null;
        this.updatableBaseBitArray = this.baseArray instanceof UpdatableBitArray ?
            (UpdatableBitArray) this.baseArray : null;
        this.updatableBaseCharArray = this.baseArray instanceof UpdatableCharArray ?
            (UpdatableCharArray) this.baseArray : null;
        this.updatableBaseByteArray = this.baseArray instanceof UpdatableByteArray ?
            (UpdatableByteArray) this.baseArray : null;
        this.updatableBaseShortArray = this.baseArray instanceof UpdatableShortArray ?
            (UpdatableShortArray) this.baseArray : null;
        this.updatableBaseIntArray = this.baseArray instanceof UpdatableIntArray ?
            (UpdatableIntArray) this.baseArray : null;
        this.updatableBaseLongArray = this.baseArray instanceof UpdatableLongArray ?
            (UpdatableLongArray) this.baseArray : null;
        this.updatableBaseFloatArray = this.baseArray instanceof UpdatableFloatArray ?
            (UpdatableFloatArray) this.baseArray : null;
        this.updatableBaseDoubleArray = this.baseArray instanceof UpdatableDoubleArray ?
            (UpdatableDoubleArray) this.baseArray : null;
        this.updatableBaseObjectArray = this.baseArray instanceof UpdatableObjectArray<?> ?
            ((UpdatableObjectArray<?>) this.baseArray).cast(Object.class) : null;

        //[[Repeat(IFF, ArraysSubMatrixConstantlyContinuedIndexer.java, init)
        //         (\/\/\s*collapsing.*?dimensions).*?(end\s+of\s+collapsing) ==>
        //         $1 is impossible for mirror continuation ;;
        //         k\s*\+\s*q ==> k ;;
        //         this\.\w+\[0\]\s*\*\=\s*collapsedDimensions;\s* ==> !! Auto-generated: NOT EDIT !! ]]
        assert dim.length == baseMatrix.dimCount();
        assert pos.length == dim.length;
        for (int k = 0; k < pos.length; k++) {
            assert dim[k] >= 0;
            assert pos[k] <= Long.MAX_VALUE - dim[k]; // i.e. dim[k]+pos[k] does not lead to overflow
        }
        int n = dim.length;
        // collapsing the first (lowest) dimensions is impossible for mirror continuation
        assert n > 0;
        this.baseDim = new long[n];
        this.pos = new long[n];
        this.dim = new long[n];
        for (int k = 0; k < n; k++) {
            this.baseDim[k] = baseMatrix.dim(k);
            this.pos[k] = pos[k];
            this.dim[k] = dim[k];
        }
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
        //[[Repeat.IncludeEnd]]
        this.baseSize = baseArray.length();

        this.reverser = baseArray instanceof BitArray ? new BooleanArrayReverser()
            : baseArray instanceof CharArray ? new CharArrayReverser()
            : baseArray instanceof ByteArray ? new ByteArrayReverser()
            : baseArray instanceof ShortArray ? new ShortArrayReverser()
            : baseArray instanceof IntArray ? new IntArrayReverser()
            : baseArray instanceof LongArray ? new LongArrayReverser()
            : baseArray instanceof FloatArray ? new FloatArrayReverser()
            : baseArray instanceof DoubleArray ? new DoubleArrayReverser()
            : baseArray instanceof ObjectArray<?> ? new ObjectArrayReverser()
            : null;
        if (this.reverser == null)
            throw new AssertionError("Illegal Array type: " + baseArray.getClass());
        this.pool = baseArray instanceof BitArray ? BOOLEAN_BUFFERS
            : baseArray instanceof CharArray ? CHAR_BUFFERS
            : baseArray instanceof ByteArray ? BYTE_BUFFERS
            : baseArray instanceof ShortArray ? SHORT_BUFFERS
            : baseArray instanceof IntArray ? INT_BUFFERS
            : baseArray instanceof LongArray ? LONG_BUFFERS
            : baseArray instanceof FloatArray ? FLOAT_BUFFERS
            : baseArray instanceof DoubleArray ? DOUBLE_BUFFERS
            : baseArray instanceof ObjectArray<?> ? OBJECT_BUFFERS :
            null;
    }

    public boolean bitsBlocksImplemented() {
        return true;
    }

    public boolean indexOfImplemented() {
        return false;
    }

    public long translate(long index) {
        assert index >= 0 && index < size; // must be checked in getXxx/setXxx methods; so, dimMul > 0
        if (dim.length == 1) {
            //[[Repeat.SectionStart translate_method_dim1_calculations]]
            long coord = pos0 + index; // index < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = AbstractMatrix.normalizeMirrorCoord(coord, baseDim0);
            //[[Repeat.SectionEnd translate_method_dim1_calculations]]
            return coord;
        }
        long a = index;
        long b = a / dim0;
        long subCoord0 = a - b * dim0; // faster equivalent of "a % dim0"
        //[[Repeat.SectionStart translate_method_main_calculations]]
        long baseCoord0 = pos0 + subCoord0;
        assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
        baseCoord0 = AbstractMatrix.normalizeMirrorCoord(baseCoord0, baseDim0);
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
            baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
            a = b;
            indexInBase += baseCoordK * baseDimMul[k];
        }
        assert k == dim.length - 1;
        long baseCoordLast = pos[k] + a;
        assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
        baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
        indexInBase += baseCoordLast * baseDimMul[k];
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
            long coord = pos0 + arrayPos; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            getDataInLine(0, coord, destArray, destArrayOffset, count);
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
            // (indexInBase\s*=)\s*baseCoord0 ==> $1 0;;
            // indexInBase ==> indexOfLineStartInBase;;
            // AbstractMatrix\.normalizeMirrorCoord(?=\(baseCoord0) ==> partiallyNormalizeMirrorCoord;;
            // (\r(?!\n)|\n|\r\n)(\s) ==> $1    $2 !! Auto-generated: NOT EDIT !! >>
            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];
            //<<Repeat.IncludeEnd>>
            getDataInLine(indexOfLineStartInBase, baseCoord0, destArray, destArrayOffset, len);
        }
    }//getData
    //[[Repeat.SectionEnd getData_method_impl]]

    private void getDataInLine(long indexOfLineStartInBase, long baseCoord0,
        Object destArray, int destArrayOffset, int len)
    {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        while (len > 0) {
            int m = (int) Math.min(baseDim0 - baseCoord0, len);
            if (inMirror) {
                baseCoord0 = baseDim0 - baseCoord0 - m;
            }
            baseArray.getData(indexOfLineStartInBase + baseCoord0, destArray, destArrayOffset, m);
            if (inMirror) {
                reverser.reverse(destArray, destArrayOffset, m);
            }
            baseCoord0 = 0;
            inMirror = !inMirror;
            destArrayOffset += m;
            len -= m;
        }
    }

    public void setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in setData/setData methods
        assert count >= 0; // must be checked in setData/setData methods
        if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
            return;
        }
        Object dataBuffer = pool.requestArray();
        try {
            if (dim.length == 1) {
                long coord = pos0 + arrayPos; // arrayPos < dim0 = size
                assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
                coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
                setDataInLine(0, coord, srcArray, srcArrayOffset, count, dataBuffer);
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
                //<<Repeat(INCLUDE_FROM_FILE, THIS_FILE, translate_method_main_calculations)
                // (indexInBase\s*=)\s*baseCoord0 ==> $1 0;;
                // indexInBase ==> indexOfLineStartInBase;;
                // AbstractMatrix\.normalizeMirrorCoord(?=\(baseCoord0) ==> partiallyNormalizeMirrorCoord;;
                // (\r(?!\n)|\n|\r\n)(\s) ==> $1        $2 !! Auto-generated: NOT EDIT !! >>
                long baseCoord0 = pos0 + subCoord0;
                assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
                baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
                a = b;
                long indexOfLineStartInBase = 0;
                int k = 1;
                for (; k < dim.length - 1; k++) {
                    b = a / dim[k];
                    long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                    if (DEBUG_MODE) {
                        assert subCoordK >= 0;
                        assert subCoordK < dim[k]; // it is a % dim[k]
                        assert indexOfLineStartInBase >= 0;
                        assert indexOfLineStartInBase < baseSize;
                    }
                    long baseCoordK = pos[k] + subCoordK;
                    assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                    baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                    a = b;
                    indexOfLineStartInBase += baseCoordK * baseDimMul[k];
                }
                assert k == dim.length - 1;
                long baseCoordLast = pos[k] + a;
                assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
                indexOfLineStartInBase += baseCoordLast * baseDimMul[k];
                //<<Repeat.IncludeEnd>>
                setDataInLine(indexOfLineStartInBase, baseCoord0, srcArray, srcArrayOffset, len, dataBuffer);
            }
        } finally {
            pool.releaseArray(dataBuffer);
        }
    }//setData

    private void setDataInLine(long indexOfLineStartInBase, long baseCoord0,
        Object srcArray, int srcArrayOffset, int len, Object dataBuffer)
    {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        while (len > 0) {
            int m = (int) Math.min(baseDim0 - baseCoord0, len);
            if (m > MC_INDEXER_BUFFER_LENGTH) {
                m = MC_INDEXER_BUFFER_LENGTH;
            }
            if (inMirror) {
                reverser.reverse(dataBuffer, srcArray, srcArrayOffset, m);
                updatableBaseArray.setData(indexOfLineStartInBase + baseDim0 - baseCoord0 - m, dataBuffer, 0, m);
            } else {
                updatableBaseArray.setData(indexOfLineStartInBase + baseCoord0, srcArray, srcArrayOffset, m);
            }
            baseCoord0 += m;
            if (baseCoord0 == baseDim0) {
                baseCoord0 = 0;
                inMirror = !inMirror;
            }
            srcArrayOffset += m;
            len -= m;
        }
    }

    //[[Repeat.SectionStart getBits_method_impl]]
    public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in getBits/setData methods
        assert count >= 0; // must be checked in getBits/setData methods
        if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
            return;
        }
        long[] dataBuffer = (long[]) LONG_BUFFERS.requestArray();
        try {
            if (dim.length == 1) {
                long coord = pos0 + arrayPos; // arrayPos < dim0 = size
                assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
                coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
                getBitsInLine(0, coord, destArray, destArrayOffset, count, dataBuffer);
                return;
            }

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
                baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
                a = b;
                long indexOfLineStartInBase = 0;
                int k = 1;
                for (; k < dim.length - 1; k++) {
                    b = a / dim[k];
                    long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                    if (DEBUG_MODE) {
                        assert subCoordK >= 0;
                        assert subCoordK < dim[k]; // it is a % dim[k]
                        assert indexOfLineStartInBase >= 0;
                        assert indexOfLineStartInBase < baseSize;
                    }
                    long baseCoordK = pos[k] + subCoordK;
                    assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                    baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                    a = b;
                    indexOfLineStartInBase += baseCoordK * baseDimMul[k];
                }
                assert k == dim.length - 1;
                long baseCoordLast = pos[k] + a;
                assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
                indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

                getBitsInLine(indexOfLineStartInBase, baseCoord0, destArray, destArrayOffset, len, dataBuffer);
            }
        } finally {
            LONG_BUFFERS.releaseArray(dataBuffer);
        }
    }//getBits
    //[[Repeat.SectionEnd getBits_method_impl]]

    private void getBitsInLine(long indexOfLineStartInBase, long baseCoord0,
        long[] destArray, long destArrayOffset, long len, long[] dataBuffer)
    {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        assert dataBuffer.length == MC_INDEXER_BUFFER_LENGTH;
        while (len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            if (m > MC_INDEXER_BUFFER_LENGTH * 64L) {
                m = MC_INDEXER_BUFFER_LENGTH * 64L;
            }
            if (inMirror) {
                baseBitArray.getBits(indexOfLineStartInBase + baseDim0 - baseCoord0 - m, dataBuffer, 0, m);
                PackedBitArrays.reverseBitsOrder(destArray, destArrayOffset, dataBuffer, 0, m);
            } else {
                baseBitArray.getBits(indexOfLineStartInBase + baseCoord0, destArray, destArrayOffset, m);
            }

            baseCoord0 += m;
            if (baseCoord0 == baseDim0) {
                baseCoord0 = 0;
                inMirror = !inMirror;
            }
            destArrayOffset += m;
            len -= m;
        }
    }

    //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getBits_method_impl)
    //  getBits ==> setBits;;
    //  \bbaseBitArray\b ==> updatableBaseBitArray ;;
    //  destArray ==> srcArray !! Auto-generated: NOT EDIT !! ]]
    public void setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
        assert arrayPos >= 0 && arrayPos <= size - count; // must be checked in setBits/setData methods
        assert count >= 0; // must be checked in setBits/setData methods
        if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
            return;
        }
        long[] dataBuffer = (long[]) LONG_BUFFERS.requestArray();
        try {
            if (dim.length == 1) {
                long coord = pos0 + arrayPos; // arrayPos < dim0 = size
                assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
                coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
                setBitsInLine(0, coord, srcArray, srcArrayOffset, count, dataBuffer);
                return;
            }

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
                baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
                a = b;
                long indexOfLineStartInBase = 0;
                int k = 1;
                for (; k < dim.length - 1; k++) {
                    b = a / dim[k];
                    long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                    if (DEBUG_MODE) {
                        assert subCoordK >= 0;
                        assert subCoordK < dim[k]; // it is a % dim[k]
                        assert indexOfLineStartInBase >= 0;
                        assert indexOfLineStartInBase < baseSize;
                    }
                    long baseCoordK = pos[k] + subCoordK;
                    assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                    baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                    a = b;
                    indexOfLineStartInBase += baseCoordK * baseDimMul[k];
                }
                assert k == dim.length - 1;
                long baseCoordLast = pos[k] + a;
                assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
                indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

                setBitsInLine(indexOfLineStartInBase, baseCoord0, srcArray, srcArrayOffset, len, dataBuffer);
            }
        } finally {
            LONG_BUFFERS.releaseArray(dataBuffer);
        }
    }//setBits
    //[[Repeat.IncludeEnd]]

    private void setBitsInLine(long indexOfLineStartInBase, long baseCoord0,
        long[] srcArray, long srcArrayOffset, long len, long[] dataBuffer)
    {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        assert dataBuffer.length == MC_INDEXER_BUFFER_LENGTH;
        while (len > 0) {
            long m = Math.min(baseDim0 - baseCoord0, len);
            if (m > MC_INDEXER_BUFFER_LENGTH * 64L) {
                m = MC_INDEXER_BUFFER_LENGTH * 64L;
            }
            if (inMirror) {
                PackedBitArrays.reverseBitsOrder(dataBuffer, 0, srcArray, srcArrayOffset, m);
                updatableBaseBitArray.setBits(indexOfLineStartInBase + baseDim0 - baseCoord0 - m, dataBuffer, 0, m);
            } else {
                updatableBaseBitArray.setBits(indexOfLineStartInBase + baseCoord0, srcArray, srcArrayOffset, m);
            }

            baseCoord0 += m;
            if (baseCoord0 == baseDim0) {
                baseCoord0 = 0;
                inMirror = !inMirror;
            }
            srcArrayOffset += m;
            len -= m;
        }
    }

    //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
    public long indexOfBit(long lowIndex, long highIndex, boolean value) {
        throw new AssertionError("indexOfBit method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
        throw new AssertionError("lastIndexOfBit method should not be called for mirror cyclic continuation");
    }

    public void fillBits(long position, long count, boolean value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillBitsInLine(0, coord, count, value);
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
            // (indexInBase\s*=)\s*baseCoord0 ==> $1 0;;
            // indexInBase ==> indexOfLineStartInBase;;
            // AbstractMatrix\.normalizeMirrorCoord(?=\(baseCoord0) ==> partiallyNormalizeMirrorCoord;;
            // (\r(?!\n)|\n|\r\n)(\s) ==> $1    $2 !! Auto-generated: NOT EDIT !! >>
            long baseCoord0 = pos0 + subCoord0;
            assert pos0 < 0 || baseCoord0 >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];
            //<<Repeat.IncludeEnd>>
            fillBitsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillBitsInLine(long indexOfLineStartInBase, long baseCoord0, long len, boolean value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseBitArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseBitArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    public long indexOfChar(long lowIndex, long highIndex, char value) {
        throw new AssertionError("indexOfChar method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
        throw new AssertionError("lastIndexOfChar method should not be called for mirror cyclic continuation");
    }

    public void fillChars(long position, long count, char value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillCharsInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillCharsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillCharsInLine(long indexOfLineStartInBase, long baseCoord0, long len, char value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseCharArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseCharArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfByte(long lowIndex, long highIndex, byte value) {
        throw new AssertionError("indexOfByte method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
        throw new AssertionError("lastIndexOfByte method should not be called for mirror cyclic continuation");
    }

    public void fillBytes(long position, long count, byte value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillBytesInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillBytesInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillBytesInLine(long indexOfLineStartInBase, long baseCoord0, long len, byte value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseByteArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseByteArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfShort(long lowIndex, long highIndex, short value) {
        throw new AssertionError("indexOfShort method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
        throw new AssertionError("lastIndexOfShort method should not be called for mirror cyclic continuation");
    }

    public void fillShorts(long position, long count, short value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillShortsInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillShortsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillShortsInLine(long indexOfLineStartInBase, long baseCoord0, long len, short value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseShortArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseShortArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfInt(long lowIndex, long highIndex, int value) {
        throw new AssertionError("indexOfInt method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
        throw new AssertionError("lastIndexOfInt method should not be called for mirror cyclic continuation");
    }

    public void fillInts(long position, long count, int value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillIntsInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillIntsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillIntsInLine(long indexOfLineStartInBase, long baseCoord0, long len, int value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseIntArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseIntArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfLong(long lowIndex, long highIndex, long value) {
        throw new AssertionError("indexOfLong method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
        throw new AssertionError("lastIndexOfLong method should not be called for mirror cyclic continuation");
    }

    public void fillLongs(long position, long count, long value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillLongsInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillLongsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillLongsInLine(long indexOfLineStartInBase, long baseCoord0, long len, long value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseLongArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseLongArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfFloat(long lowIndex, long highIndex, float value) {
        throw new AssertionError("indexOfFloat method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
        throw new AssertionError("lastIndexOfFloat method should not be called for mirror cyclic continuation");
    }

    public void fillFloats(long position, long count, float value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillFloatsInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillFloatsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillFloatsInLine(long indexOfLineStartInBase, long baseCoord0, long len, float value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseFloatArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseFloatArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfDouble(long lowIndex, long highIndex, double value) {
        throw new AssertionError("indexOfDouble method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
        throw new AssertionError("lastIndexOfDouble method should not be called for mirror cyclic continuation");
    }

    public void fillDoubles(long position, long count, double value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillDoublesInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillDoublesInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillDoublesInLine(long indexOfLineStartInBase, long baseCoord0, long len, double value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseDoubleArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseDoubleArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    public long indexOfObject(long lowIndex, long highIndex, Object value) {
        throw new AssertionError("indexOfObject method should not be called for mirror cyclic continuation");
    }

    public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
        throw new AssertionError("lastIndexOfObject method should not be called for mirror cyclic continuation");
    }

    public void fillObjects(long position, long count, Object value) {
        assert position >= 0 && position <= size - count; // must be checked in fill methods
        assert count >= 0; // must be checked in fill methods
        if (dim.length == 1) {
            if (count == 0) { // necessary to guarantee that baseDim0 != 0 below
                return;
            }
            long coord = pos0 + position; // arrayPos < dim0 = size
            assert pos0 < 0 || coord >= 0; // overflow impossible: pos0+dim0 <= Long.MAX_VALUE in any submatrix
            coord = partiallyNormalizeMirrorCoord(coord, baseDim0);
            fillObjectsInLine(0, coord, count, value);
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
            baseCoord0 = partiallyNormalizeMirrorCoord(baseCoord0, baseDim0);
            a = b;
            long indexOfLineStartInBase = 0;
            int k = 1;
            for (; k < dim.length - 1; k++) {
                b = a / dim[k];
                long subCoordK = a - b * dim[k]; // faster equivalent of "a % dim[k]"
                if (DEBUG_MODE) {
                    assert subCoordK >= 0;
                    assert subCoordK < dim[k]; // it is a % dim[k]
                    assert indexOfLineStartInBase >= 0;
                    assert indexOfLineStartInBase < baseSize;
                }
                long baseCoordK = pos[k] + subCoordK;
                assert pos[k] < 0 || baseCoordK >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
                baseCoordK = AbstractMatrix.normalizeMirrorCoord(baseCoordK, baseDim[k]);
                a = b;
                indexOfLineStartInBase += baseCoordK * baseDimMul[k];
            }
            assert k == dim.length - 1;
            long baseCoordLast = pos[k] + a;
            assert pos[k] < 0 || baseCoordLast >= 0; // overflow impossible: pos[k]+dim[k] <= Long.MAX_VALUE
            baseCoordLast = AbstractMatrix.normalizeMirrorCoord(baseCoordLast, baseDim[k]);
            indexOfLineStartInBase += baseCoordLast * baseDimMul[k];

            fillObjectsInLine(indexOfLineStartInBase, baseCoord0, len, value);
        }
    }

    private void fillObjectsInLine(long indexOfLineStartInBase, long baseCoord0, long len, Object value) {
        boolean inMirror = baseCoord0 < 0;
        baseCoord0 &= Long.MAX_VALUE;
        assert baseCoord0 < baseDim0;
        long m = Math.min(baseDim0 - baseCoord0, len);
        long position = indexOfLineStartInBase + (inMirror ? baseDim0 - baseCoord0 - m : baseCoord0);
        updatableBaseObjectArray.fill(position, m, value);
        len -= m;
        if (len == 0) {
            return;
        }
        inMirror = !inMirror;
        m = Math.min(baseDim0, len);
        position = inMirror ? indexOfLineStartInBase + baseDim0 - m : indexOfLineStartInBase;
        updatableBaseObjectArray.fill(position, m, value);
        // no sense to fill farther: the following elements will repeat
    }

    //[[Repeat.AutoGeneratedEnd]]

    private static interface ArrayReverser {
        public void reverse(Object javaArray, int position, int count);

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count);
    }

    //[[Repeat() Boolean ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //           boolean ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
    private static class BooleanArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            boolean[] a = (boolean[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                boolean temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            boolean[] da = (boolean[]) destArray;
            boolean[] sa = (boolean[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    private static class CharArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            char[] a = (char[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                char temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            char[] da = (char[]) destArray;
            char[] sa = (char[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class ByteArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            byte[] a = (byte[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                byte temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            byte[] da = (byte[]) destArray;
            byte[] sa = (byte[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class ShortArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            short[] a = (short[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                short temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            short[] da = (short[]) destArray;
            short[] sa = (short[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class IntArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            int[] a = (int[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                int temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            int[] da = (int[]) destArray;
            int[] sa = (int[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class LongArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            long[] a = (long[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                long temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            long[] da = (long[]) destArray;
            long[] sa = (long[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class FloatArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            float[] a = (float[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                float temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            float[] da = (float[]) destArray;
            float[] sa = (float[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class DoubleArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            double[] a = (double[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                double temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            double[] da = (double[]) destArray;
            double[] sa = (double[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    private static class ObjectArrayReverser implements ArrayReverser {
        public void reverse(Object javaArray, int position, int count) {
            Object[] a = (Object[]) javaArray;
            // all count elements were previously read into this array,
            // so we can be sure that position+count is calculated without overflow:
            // in other case, we would already throw IndexOutOfBoundException
            for (int i = position, mid = position + (count >> 1), j = position + count - 1; i < mid; i++, j--) {
                Object temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        public void reverse(Object destArray, Object srcArray, int srcPosition, int count) {
            Object[] da = (Object[]) destArray;
            Object[] sa = (Object[]) srcArray;
            for (int i = 0, j = srcPosition + count - 1; i < count; i++, j--) {
                da[i] = sa[j];
            }
        }
    }

    //[[Repeat.AutoGeneratedEnd]]

    private static long partiallyNormalizeMirrorCoord(long coord, long dim) {
        long repeatIndex = coord / dim;
        boolean mirror = (repeatIndex & 1) != 0;
        coord -= dim * repeatIndex;
        if (coord < 0) {
            coord += dim;
            mirror = !mirror;
        }
        return mirror ? coord | Long.MIN_VALUE : coord;
    }
}
