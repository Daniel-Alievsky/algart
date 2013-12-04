/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

class ArraysTileMatrixImpl {

    private static final boolean DEBUG_MODE = false; // enable comparison with translate() method in each data block

    public static boolean isPowerOfTwo(long[] tileDim) {
        for (long d : tileDim) {
            if ((d & (d - 1)) != 0)
                return false;
        }
        return true;
    }

    interface TileMatrixArray {
        public Matrix<? extends Array> baseMatrix();

        public long[] tileDimensions();

        public Indexer indexer();
    }

    //[[Repeat() boolean(?!\s+isLazy) ==> char,,byte,,short,,int,,long,,float,,double,,E;;
    //           @Override\s+public\s+\w+\s+(getBits|setBits|nextQuickPosition).*?} ==> ,, ...;;
    //           Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
    //           public\s+byte\s+(getByte)|public\s+short\s+(getShort) ==> public int $1$2,, ...;;
    //           (super\(baseMatrix\.size\(\)) ==>
    //               $1,,$1,,$1,,$1,,$1,,$1,,$1,,super(baseMatrix.array().elementType(), baseMatrix.size();;
    //           (ObjectArray)(\s(?!\?)|>) ==> $1<E>$2,, ...;;
    //           (get|set)Object ==> $1,, ...;;
    //           E\[\" ==> \" + elementType().getName() + \"[\",, ...]]
    static class TileMatrixBitArray extends AbstractBitArray implements TileMatrixArray {
        private final Matrix<? extends BitArray> baseMatrix;
        private final BitArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixBitArray(Matrix<? extends BitArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public boolean getBit(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getBit(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1, length, getClass());
            indexer.getBits(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public long nextQuickPosition(long position) {
            return position >= length ? -1 : position < 0 ? 0 : position;
        }

        @Override
        public long indexOf(long lowIndex, long highIndex, boolean value) {
            return indexer.indexOfBit(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
            return indexer.lastIndexOfBit(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray boolean["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableBitArray extends AbstractUpdatableBitArray implements TileMatrixArray {
        private final Matrix<? extends BitArray> baseMatrix;
        private final UpdatableBitArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableBitArray(Matrix<? extends UpdatableBitArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean getBit(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getBit(index);
        }

        @Override
        public void setBit(long index, boolean value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setBit(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1, length, getClass());
            indexer.getBits(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        @Override
        public UpdatableBitArray setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos, length, getClass());
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1, length, getClass());
            indexer.setBits(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        @Override
        public long nextQuickPosition(long position) {
            return position >= length ? -1 : position < 0 ? 0 : position;
        }

        @Override
        public long indexOf(long lowIndex, long highIndex, boolean value) {
            return indexer.indexOfBit(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
            return indexer.lastIndexOfBit(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableBitArray fill(long position, long count, boolean value) {
            checkSubArrArguments(position, count);
            indexer.fillBits(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray boolean["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class TileMatrixCharArray extends AbstractCharArray implements TileMatrixArray {
        private final Matrix<? extends CharArray> baseMatrix;
        private final CharArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixCharArray(Matrix<? extends CharArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public char getChar(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getChar(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, char value) {
            return indexer.indexOfChar(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, char value) {
            return indexer.lastIndexOfChar(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray char["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableCharArray extends AbstractUpdatableCharArray implements TileMatrixArray {
        private final Matrix<? extends CharArray> baseMatrix;
        private final UpdatableCharArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableCharArray(Matrix<? extends UpdatableCharArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public char getChar(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getChar(index);
        }

        @Override
        public void setChar(long index, char value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setChar(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, char value) {
            return indexer.indexOfChar(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, char value) {
            return indexer.lastIndexOfChar(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableCharArray fill(long position, long count, char value) {
            checkSubArrArguments(position, count);
            indexer.fillChars(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray char["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixByteArray extends AbstractByteArray implements TileMatrixArray {
        private final Matrix<? extends ByteArray> baseMatrix;
        private final ByteArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixByteArray(Matrix<? extends ByteArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public int getByte(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getByte(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, byte value) {
            return indexer.indexOfByte(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, byte value) {
            return indexer.lastIndexOfByte(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray byte["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableByteArray extends AbstractUpdatableByteArray implements TileMatrixArray {
        private final Matrix<? extends ByteArray> baseMatrix;
        private final UpdatableByteArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableByteArray(Matrix<? extends UpdatableByteArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public int getByte(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getByte(index);
        }

        @Override
        public void setByte(long index, byte value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setByte(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, byte value) {
            return indexer.indexOfByte(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, byte value) {
            return indexer.lastIndexOfByte(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableByteArray fill(long position, long count, byte value) {
            checkSubArrArguments(position, count);
            indexer.fillBytes(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray byte["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixShortArray extends AbstractShortArray implements TileMatrixArray {
        private final Matrix<? extends ShortArray> baseMatrix;
        private final ShortArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixShortArray(Matrix<? extends ShortArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public int getShort(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getShort(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, short value) {
            return indexer.indexOfShort(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, short value) {
            return indexer.lastIndexOfShort(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray short["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableShortArray extends AbstractUpdatableShortArray implements TileMatrixArray {
        private final Matrix<? extends ShortArray> baseMatrix;
        private final UpdatableShortArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableShortArray(Matrix<? extends UpdatableShortArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public int getShort(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getShort(index);
        }

        @Override
        public void setShort(long index, short value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setShort(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, short value) {
            return indexer.indexOfShort(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, short value) {
            return indexer.lastIndexOfShort(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableShortArray fill(long position, long count, short value) {
            checkSubArrArguments(position, count);
            indexer.fillShorts(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray short["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixIntArray extends AbstractIntArray implements TileMatrixArray {
        private final Matrix<? extends IntArray> baseMatrix;
        private final IntArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixIntArray(Matrix<? extends IntArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getInt(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, int value) {
            return indexer.indexOfInt(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, int value) {
            return indexer.lastIndexOfInt(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray int["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableIntArray extends AbstractUpdatableIntArray implements TileMatrixArray {
        private final Matrix<? extends IntArray> baseMatrix;
        private final UpdatableIntArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableIntArray(Matrix<? extends UpdatableIntArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public int getInt(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getInt(index);
        }

        @Override
        public void setInt(long index, int value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setInt(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, int value) {
            return indexer.indexOfInt(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, int value) {
            return indexer.lastIndexOfInt(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableIntArray fill(long position, long count, int value) {
            checkSubArrArguments(position, count);
            indexer.fillInts(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray int["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixLongArray extends AbstractLongArray implements TileMatrixArray {
        private final Matrix<? extends LongArray> baseMatrix;
        private final LongArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixLongArray(Matrix<? extends LongArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getLong(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, long value) {
            return indexer.indexOfLong(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return indexer.lastIndexOfLong(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray long["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableLongArray extends AbstractUpdatableLongArray implements TileMatrixArray {
        private final Matrix<? extends LongArray> baseMatrix;
        private final UpdatableLongArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableLongArray(Matrix<? extends UpdatableLongArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public long getLong(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getLong(index);
        }

        @Override
        public void setLong(long index, long value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setLong(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, long value) {
            return indexer.indexOfLong(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return indexer.lastIndexOfLong(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableLongArray fill(long position, long count, long value) {
            checkSubArrArguments(position, count);
            indexer.fillLongs(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray long["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixFloatArray extends AbstractFloatArray implements TileMatrixArray {
        private final Matrix<? extends FloatArray> baseMatrix;
        private final FloatArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixFloatArray(Matrix<? extends FloatArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public float getFloat(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getFloat(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, float value) {
            return indexer.indexOfFloat(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, float value) {
            return indexer.lastIndexOfFloat(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray float["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableFloatArray extends AbstractUpdatableFloatArray implements TileMatrixArray {
        private final Matrix<? extends FloatArray> baseMatrix;
        private final UpdatableFloatArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableFloatArray(Matrix<? extends UpdatableFloatArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public float getFloat(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getFloat(index);
        }

        @Override
        public void setFloat(long index, float value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setFloat(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, float value) {
            return indexer.indexOfFloat(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, float value) {
            return indexer.lastIndexOfFloat(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableFloatArray fill(long position, long count, float value) {
            checkSubArrArguments(position, count);
            indexer.fillFloats(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray float["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixDoubleArray extends AbstractDoubleArray implements TileMatrixArray {
        private final Matrix<? extends DoubleArray> baseMatrix;
        private final DoubleArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixDoubleArray(Matrix<? extends DoubleArray> baseMatrix, long[] tileDim) {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getDouble(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, double value) {
            return indexer.indexOfDouble(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return indexer.lastIndexOfDouble(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray double["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableDoubleArray extends AbstractUpdatableDoubleArray implements TileMatrixArray {
        private final Matrix<? extends DoubleArray> baseMatrix;
        private final UpdatableDoubleArray baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableDoubleArray(Matrix<? extends UpdatableDoubleArray> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public double getDouble(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.getDouble(index);
        }

        @Override
        public void setDouble(long index, double value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.setDouble(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, double value) {
            return indexer.indexOfDouble(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return indexer.lastIndexOfDouble(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableDoubleArray fill(long position, long count, double value) {
            checkSubArrArguments(position, count);
            indexer.fillDoubles(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray double["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    static class TileMatrixObjectArray<E> extends AbstractObjectArray<E> implements TileMatrixArray {
        private final Matrix<? extends ObjectArray<E>> baseMatrix;
        private final ObjectArray<E> baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixObjectArray(Matrix<? extends ObjectArray<E>> baseMatrix, long[] tileDim) {
            super(baseMatrix.array().elementType(), baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public E get(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.get(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, E value) {
            return indexer.indexOfObject(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, E value) {
            return indexer.lastIndexOfObject(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray " + elementType().getName() + "["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }

    static class TileMatrixUpdatableObjectArray<E> extends AbstractUpdatableObjectArray<E> implements TileMatrixArray {
        private final Matrix<? extends ObjectArray<E>> baseMatrix;
        private final UpdatableObjectArray<E> baseArray;
        private final long[] tileDim;
        private final Indexer indexer;

        TileMatrixUpdatableObjectArray(Matrix<? extends UpdatableObjectArray<E>> baseMatrix,
            long[] tileDim)
        {
            super(baseMatrix.array().elementType(), baseMatrix.size(), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.tileDim = tileDim.clone(); // important: cloning before checking in the Indexer
            this.indexer = Indexer.getInstance(baseMatrix, this.tileDim);
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public long[] tileDimensions() {
            return tileDim.clone();
        }

        public Indexer indexer() {
            return indexer;
        }

        @Override
        public E get(long index) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            return baseArray.get(index);
        }

        @Override
        public void set(long index, E value) {
            if (index < 0 || index >= length)
                throw rangeException(index);
            index = indexer.translate(index);
            baseArray.set(index, value);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            if (destArray == null)
                throw new NullPointerException("Null destArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            if (srcArray == null)
                throw new NullPointerException("Null srcArray argument");
            if (count < 0)
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            if (arrayPos < 0)
                throw rangeException(arrayPos);
            if (arrayPos > length - count)
                throw rangeException(arrayPos + count - 1);
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, E value) {
            return indexer.indexOfObject(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, E value) {
            return indexer.lastIndexOfObject(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableObjectArray<E> fill(long position, long count, E value) {
            checkSubArrArguments(position, count);
            indexer.fillObjects(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray " + elementType().getName() + "["
                + length + "] tiled by " + JArrays.toString(tileDim, "x", 1000)
                + " from " + baseMatrix;
        }
    }
    //[[Repeat.AutoGeneratedEnd]]

    static class Indexer {
        /**
         * <tt>baseMatrix.array()</tt>.
         */
        final Array baseArray;

        /**
         * <tt>(UpdatableArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not an {@link UpdatableArray}.
         */
        final UpdatableArray updatableBaseArray;

        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double]]
        /**
         * <tt>(BitArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link BitArray}.
         */
        final BitArray baseBitArray;

        /**
         * <tt>(UpdatableBitArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableBitArray}.
         */
        final UpdatableBitArray updatableBaseBitArray;

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        /**
         * <tt>(CharArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link CharArray}.
         */
        final CharArray baseCharArray;

        /**
         * <tt>(UpdatableCharArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableCharArray}.
         */
        final UpdatableCharArray updatableBaseCharArray;

        /**
         * <tt>(ByteArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link ByteArray}.
         */
        final ByteArray baseByteArray;

        /**
         * <tt>(UpdatableByteArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableByteArray}.
         */
        final UpdatableByteArray updatableBaseByteArray;

        /**
         * <tt>(ShortArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link ShortArray}.
         */
        final ShortArray baseShortArray;

        /**
         * <tt>(UpdatableShortArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableShortArray}.
         */
        final UpdatableShortArray updatableBaseShortArray;

        /**
         * <tt>(IntArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link IntArray}.
         */
        final IntArray baseIntArray;

        /**
         * <tt>(UpdatableIntArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableIntArray}.
         */
        final UpdatableIntArray updatableBaseIntArray;

        /**
         * <tt>(LongArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link LongArray}.
         */
        final LongArray baseLongArray;

        /**
         * <tt>(UpdatableLongArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableLongArray}.
         */
        final UpdatableLongArray updatableBaseLongArray;

        /**
         * <tt>(FloatArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link FloatArray}.
         */
        final FloatArray baseFloatArray;

        /**
         * <tt>(UpdatableFloatArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableFloatArray}.
         */
        final UpdatableFloatArray updatableBaseFloatArray;

        /**
         * <tt>(DoubleArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link DoubleArray}.
         */
        final DoubleArray baseDoubleArray;

        /**
         * <tt>(UpdatableDoubleArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableDoubleArray}.
         */
        final UpdatableDoubleArray updatableBaseDoubleArray;

        //[[Repeat.AutoGeneratedEnd]]

        /**
         * <tt>(ObjectArray)baseMatrix.array()</tt> or <tt>null</tt> if it is not a {@link ObjectArray}.
         */
        final ObjectArray<Object> baseObjectArray;

        /**
         * <tt>(UpdatableObjectArray)baseMatrix.array()</tt>
         * or <tt>null</tt> if it is not an {@link UpdatableObjectArray}.
         */
        final UpdatableObjectArray<Object> updatableBaseObjectArray;

        /**
         * <tt>baseMatrix.size()</tt>.
         */
        final long baseLength;

        /**
         * Dimensions of <tt>baseMatrix</tt>.
         */
        final long[] baseDim;

        /**
         * <tt>{@link #baseDim}[0]</tt>
         */
        final long baseDim0;

        /**
         * <tt>{@link #baseDim}[{@link #n}-1]</tt>
         */
        final long baseDimLast;

        /**
         * <pre>baseDimMul[k] = 1 * baseDim[0] * ... * baseDim[k-1]</pre>
         */
        final long[] baseDimMul;

        /**
         * The argument of the constructor.
         */
        final long[] tileDim;

        /**
         * <tt>{@link #tileDim}[0]</tt>
         */
        final long tileDim0;

        /**
         * <tt>{@link #tileDim}[{@link #n}-1]</tt>
         */
        final long tileDimLast;

        /**
         * Number of dimensions.
         */
        final int n;

        Indexer(Array baseArray, long[] baseDim, long[] tileDim) {
            this.baseArray = baseArray;
            this.updatableBaseArray = this.baseArray instanceof UpdatableArray ?
                (UpdatableArray)this.baseArray : null;
            this.baseBitArray = this.baseArray instanceof BitArray ? (BitArray)this.baseArray : null;
            this.updatableBaseBitArray = this.baseArray instanceof UpdatableBitArray ?
                (UpdatableBitArray)this.baseArray : null;
            this.baseCharArray = this.baseArray instanceof CharArray ? (CharArray)this.baseArray : null;
            this.updatableBaseCharArray = this.baseArray instanceof UpdatableCharArray ?
                (UpdatableCharArray)this.baseArray : null;
            this.baseByteArray = this.baseArray instanceof ByteArray ? (ByteArray)this.baseArray : null;
            this.updatableBaseByteArray = this.baseArray instanceof UpdatableByteArray ?
                (UpdatableByteArray)this.baseArray : null;
            this.baseShortArray = this.baseArray instanceof ShortArray ? (ShortArray)this.baseArray : null;
            this.updatableBaseShortArray = this.baseArray instanceof UpdatableShortArray ?
                (UpdatableShortArray)this.baseArray : null;
            this.baseIntArray = this.baseArray instanceof IntArray ? (IntArray)this.baseArray : null;
            this.updatableBaseIntArray = this.baseArray instanceof UpdatableIntArray ?
                (UpdatableIntArray)this.baseArray : null;
            this.baseLongArray = this.baseArray instanceof LongArray ? (LongArray)this.baseArray : null;
            this.updatableBaseLongArray = this.baseArray instanceof UpdatableLongArray ?
                (UpdatableLongArray)this.baseArray : null;
            this.baseFloatArray = this.baseArray instanceof FloatArray ? (FloatArray)this.baseArray : null;
            this.updatableBaseFloatArray = this.baseArray instanceof UpdatableFloatArray ?
                (UpdatableFloatArray)this.baseArray : null;
            this.baseDoubleArray = this.baseArray instanceof DoubleArray ? (DoubleArray)this.baseArray : null;
            this.updatableBaseDoubleArray = this.baseArray instanceof UpdatableDoubleArray ?
                (UpdatableDoubleArray)this.baseArray : null;
            this.baseObjectArray = this.baseArray instanceof ObjectArray<?> ?
                ((ObjectArray<?>)this.baseArray).cast(Object.class) : null;
            this.updatableBaseObjectArray = this.baseArray instanceof UpdatableObjectArray<?> ?
                ((UpdatableObjectArray<?>)this.baseArray).cast(Object.class) : null;

            this.baseLength = this.baseArray.length();
            this.n = baseDim.length;
            assert tileDim.length == n;
            this.baseDim = baseDim;
            this.tileDim = tileDim;
            this.baseDim0 = baseDim[0];
            this.baseDimLast = baseDim[n - 1];
            this.tileDim0 = tileDim[0];
            this.tileDimLast = tileDim[n - 1];
            this.baseDimMul = new long[n];
            for (int k = 0; k < n; k++) {
                this.baseDimMul[k] = k == 0 ? 1 : this.baseDimMul[k - 1] * this.baseDim[k - 1];
            }
        }

        public static Indexer getInstance(Matrix<? extends Array> baseMatrix, long[] tileDim) {
            if (tileDim == null)
                throw new NullPointerException("Null tile dimensions Java array");
            int n = baseMatrix.dimCount();
            if (tileDim.length != n)
                throw new IllegalArgumentException("Number of tile dimensions is not equal "
                    + "to number of matrix dimensions");
            for (int k = 0; k < n; k++) {
                if (tileDim[k] <= 0)
                    throw new IllegalArgumentException("Negative or zero tile dimension #" + k + ": " + tileDim[k]);
            }
            long tileSize = Arrays.longMul(tileDim);
            if (tileSize == Long.MIN_VALUE)
                throw new IllegalArgumentException("Too large tile dimensions: "
                    + "dim[0] * dim[1] * ... = " + JArrays.toString(tileDim, " * ", 100) + " > Long.MAX_VALUE");
            // collapsing the first (lowest) dimensions if the tiling is trivial for them
            int q = 0;
            long collapsedDimensions = 1;
            while (q < tileDim.length - 1 && tileDim[q] >= baseMatrix.dim(q)) {
                // q < tileDim.length - 1: we cannot collapse all dimensions into "0-dimensional" matrix
                collapsedDimensions *= baseMatrix.dim(q);
                q++;
                n--;
            } // end of collapsing
            assert n > 0;
            long[] actualBaseDim = new long[n];
            long[] actualTileDim = new long[n];
            for (int k = 0; k < n; k++) {
                actualBaseDim[k] = baseMatrix.dim(k + q);
                actualTileDim[k] = tileDim[k + q];
            }
            actualBaseDim[0] *= collapsedDimensions;
            actualTileDim[0] *= collapsedDimensions; // overflow impossible, because tileDim[q] >= baseMatrix.dim(q)
            if (isPowerOfTwo(actualTileDim) && n > 1) {
                switch (n) {
                    case 2:
                        return new IndexerForPowerOfTwo2D(baseMatrix.array(), actualBaseDim, actualTileDim);
                    case 3:
                        return new IndexerForPowerOfTwo3D(baseMatrix.array(), actualBaseDim, actualTileDim);
                    default:
                        return new IndexerForPowerOfTwo(baseMatrix.array(), actualBaseDim, actualTileDim);
                }
            } else {
                switch (n) {
                    case 1:
                        return new Indexer1D(baseMatrix.array(), actualBaseDim, actualTileDim);
                    case 2:
                        return new Indexer2D(baseMatrix.array(), actualBaseDim, actualTileDim);
                    case 3:
                        return new Indexer3D(baseMatrix.array(), actualBaseDim, actualTileDim);
                    default:
                        return new Indexer(baseMatrix.array(), actualBaseDim, actualTileDim);
                }
            }
        }

        long translate(final long index) {
            assert index >= 0; // must be checked in getXxx/setXxx methods
            if (n == 1) {
                return index;
            }
            long previousVolume = 0;
            // Summary size of all tiles before this: previousVolume =
            //       baseDim[0] * baseDim[1] * ... * baseDim[n-3] * baseDim[n-2] * cStart[n-1]
            //     + baseDim[0] * baseDim[1] * ... * baseDim[n-3] * cStart[n-2] * currentTileDim[n-1]
            //     + . . .
            //     + cStart[0] * currentTileDim[1] * ... * currentTileDim[n-1]
            // where
            //     cStart[k] = coord[k] - coord[k] % tileDim[k]
            // is the start element of the current tile and
            //     currentTileDim[k] = min(tileDim[k], baseDim[k] - cStart[k])
            // are dimensions of the current tile, truncated to the matrix sizes.
            // If all baseDim[k] % tileDim[k] == 0, this formula is identical to tileIndex * tileSize,
            // where tileIndex is calculated on the base of tileCoordinates[k] = coord[k] / tileDim[k]
            // For example, in 3D case: previousVolume =
            //       baseDimX * baseDimY * cStartZ
            //     + baseDimX * cStartY * currentTileDimZ
            //     + cStartX * currentTileDimY * currentTileDimZ
            // in 2D case: previousVolume =
            //       baseDimX * cStartY
            //     + cStartX * currentTileDimY
            long a = index;
            long currentTileDimMul = 1;
            for (int k = n - 1; k > 0; k--) { // 1st loop for finding previousVolume
                long coordinate = a / baseDimMul[k]; // coordinate #k, coord[k] in terms above
                a -= coordinate * baseDimMul[k];
                long cStart = coordinate - coordinate % tileDim[k]; // cStart[k]: starting coordinate in this tile
                previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                long rest = baseDim[k] - cStart;
                if (tileDim[k] <= rest) {
                    currentTileDimMul *= tileDim[k];
                } else {
                    currentTileDimMul *= rest;
                }
            }
            // now "a" is coordinate #0
            previousVolume += (a - a % tileDim0) * currentTileDimMul;
            long indexInTile = 0;
            // Index of the current tile:
            //     coordInTile[0]
            //     + currentTileDim[0] * coordInTile[1]
            //     + currentTileDim[0] * currentTileDim[1] * coordInTile[2]
            //     + . . .
            //     + currentTileDim[0] * currentTileDim[1] * ... * currentTileDim[n-2] * coordInTile[n-1]
            // where
            //     coordInTile[k] = coord[k] % tileDim[k]
            a = index;
            currentTileDimMul = 1;
            for (int k = 0; k < n - 1; k++) { // 2nd loop for finding indexInTile
                long dim = baseDim[k];
                long b = a / dim;
                long coordinate = a - b * dim; // coordinate #k, coord[k] in terms above; here "*" is faster than "%"
                long coordInTile = coordinate % tileDim[k];
                indexInTile += coordInTile * currentTileDimMul;
                long cStart = coordinate - coordInTile; // cStart[k]: starting coordinate in this tile
                long rest = dim - cStart;
                if (tileDim[k] <= rest) {
                    currentTileDimMul *= tileDim[k];
                } else {
                    currentTileDimMul *= rest;
                }
                a = b;
            }
            // now "a" is coordinate #n-1, coord[n-1] in terms above
            indexInTile += (a % tileDimLast) * currentTileDimMul;
            long indexInBase = previousVolume + indexInTile;
            assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase < baseLength :
                "A bug in translate() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                    + index + " translated to " + indexInBase + ", baseAndTileDimMul = "
                    + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//            System.out.println(index + " translated to " + indexInBase);
            return indexInBase;
        }//translate

        //[[Repeat.SectionStart getData_method_impl]]
        void getData(final long arrayPos, Object destArray, int destArrayOffset, int count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in getData/setData methods
            assert count >= 0; // must be checked in getData/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; destArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     destArray[destArrayOffset] = getXxx(index);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            int len;
            for (long index = arrayPos; count > 0; index += len, destArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? (int)restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in getData/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in getData() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + baseArray;
                baseArray.getData(indexInBase, destArray, destArrayOffset, len);
//                System.out.println("getData: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//getData
        //[[Repeat.SectionEnd getData_method_impl]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
        //  getData ==> setData;;
        //  \bbaseArray\b ==> updatableBaseArray ;;
        //  destArray ==> srcArray ;;
        //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==>
        //  setXxx(index, $1)   !! Auto-generated: NOT EDIT !! ]]
        void setData(final long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in setData/setData methods
            assert count >= 0; // must be checked in setData/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; srcArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     setXxx(index, srcArray[srcArrayOffset]);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            int len;
            for (long index = arrayPos; count > 0; index += len, srcArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? (int)restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in setData/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + updatableBaseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in setData() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + updatableBaseArray;
                updatableBaseArray.setData(indexInBase, srcArray, srcArrayOffset, len);
//                System.out.println("setData: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//setData
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
        //  \bint\s+(destArrayOffset|count|len)\b ==> long $1 ;;
        //  \bObject\s+destArray\b ==> long[] destArray ;;
        //  \bbaseArray\b ==> baseBitArray ;;
        //  \(int\) ==> ;;
        //  getData ==> getBits    !! Auto-generated: NOT EDIT !! ]]
        void getBits(final long arrayPos, long[] destArray, long destArrayOffset, long count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in getBits/setData methods
            assert count >= 0; // must be checked in getBits/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; destArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     destArray[destArrayOffset] = getXxx(index);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            long len;
            for (long index = arrayPos; count > 0; index += len, destArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in getBits/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + baseBitArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in getBits() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + baseBitArray;
                baseBitArray.getBits(indexInBase, destArray, destArrayOffset, len);
//                System.out.println("getBits: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//getBits
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getData_method_impl)
        //  getData ==> setBits;;
        //  \bbaseArray\b ==> updatableBaseBitArray ;;
        //  destArray ==> srcArray ;;
        //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==> setXxx(index, $1) ;;
        //  \bint\s+(srcArrayOffset|count|len)\b ==> long $1 ;;
        //  \bObject\s+srcArray\b ==> long[] srcArray ;;
        //  \(int\) ==>   !! Auto-generated: NOT EDIT !! ]]
        void setBits(final long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in setBits/setData methods
            assert count >= 0; // must be checked in setBits/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; srcArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     setXxx(index, srcArray[srcArrayOffset]);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            long len;
            for (long index = arrayPos; count > 0; index += len, srcArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in setBits/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + updatableBaseBitArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in setBits() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + updatableBaseBitArray;
                updatableBaseBitArray.setBits(indexInBase, srcArray, srcArrayOffset, len);
//                System.out.println("setBits: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//setBits
        //[[Repeat.IncludeEnd]]

        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
        //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
        public long indexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfBit/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseBitArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfBit/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseBitArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillBits(long position, long count, boolean value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfBit/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseBitArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        public long indexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfChar/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseCharArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfChar/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseCharArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillChars(long position, long count, char value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfChar/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseCharArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfByte/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseByteArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfByte/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseByteArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillBytes(long position, long count, byte value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfByte/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseByteArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfShort/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseShortArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfShort/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseShortArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillShorts(long position, long count, short value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfShort/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseShortArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfInt/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseIntArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfInt/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseIntArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillInts(long position, long count, int value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfInt/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseIntArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfLong/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseLongArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfLong/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseLongArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillLongs(long position, long count, long value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfLong/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseLongArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfFloat/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseFloatArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfFloat/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseFloatArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillFloats(long position, long count, float value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfFloat/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseFloatArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfDouble/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfDouble/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillDoubles(long position, long count, double value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfDouble/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseDoubleArray.fill(indexInBase, len, value);
            }
        }

        public long indexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfObject/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseObjectArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfObject/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseObjectArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        public void fillObjects(long position, long count, Object value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord - coord % tileDim[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                final long coord0InTile = a % tileDim0;
                previousVolume += (a - coord0InTile) * currentTileDimMul;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord % tileDim[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul *= tileDim[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a % tileDimLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfObject/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseObjectArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedEnd]]
    }

    private static class Indexer1D extends Indexer {
        Indexer1D(Array baseArray, long[] baseDim, long[] tileDim) {
            super(baseArray, baseDim, tileDim);
            assert this.n == 1;
        }

        @Override
        long translate(final long index) {
            return index;
        }

        @Override
        void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            baseArray.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        void setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            updatableBaseArray.setData(arrayPos, srcArray, srcArrayOffset, count);
        }

        @Override
        void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            baseBitArray.getBits(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        void setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
            updatableBaseBitArray.setBits(arrayPos, srcArray, srcArrayOffset, count);
        }

        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
        //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
        @Override
        public long indexOfBit(long lowIndex, long highIndex, boolean value) {
            return baseBitArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            return baseBitArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillBits(long position, long count, boolean value) {
            updatableBaseBitArray.fill(position, count, value);
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        @Override
        public long indexOfChar(long lowIndex, long highIndex, char value) {
            return baseCharArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            return baseCharArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillChars(long position, long count, char value) {
            updatableBaseCharArray.fill(position, count, value);
        }

        @Override
        public long indexOfByte(long lowIndex, long highIndex, byte value) {
            return baseByteArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            return baseByteArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillBytes(long position, long count, byte value) {
            updatableBaseByteArray.fill(position, count, value);
        }

        @Override
        public long indexOfShort(long lowIndex, long highIndex, short value) {
            return baseShortArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            return baseShortArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillShorts(long position, long count, short value) {
            updatableBaseShortArray.fill(position, count, value);
        }

        @Override
        public long indexOfInt(long lowIndex, long highIndex, int value) {
            return baseIntArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            return baseIntArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillInts(long position, long count, int value) {
            updatableBaseIntArray.fill(position, count, value);
        }

        @Override
        public long indexOfLong(long lowIndex, long highIndex, long value) {
            return baseLongArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            return baseLongArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillLongs(long position, long count, long value) {
            updatableBaseLongArray.fill(position, count, value);
        }

        @Override
        public long indexOfFloat(long lowIndex, long highIndex, float value) {
            return baseFloatArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            return baseFloatArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillFloats(long position, long count, float value) {
            updatableBaseFloatArray.fill(position, count, value);
        }

        @Override
        public long indexOfDouble(long lowIndex, long highIndex, double value) {
            return baseDoubleArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            return baseDoubleArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillDoubles(long position, long count, double value) {
            updatableBaseDoubleArray.fill(position, count, value);
        }

        @Override
        public long indexOfObject(long lowIndex, long highIndex, Object value) {
            return baseObjectArray.indexOf(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
            return baseObjectArray.lastIndexOf(lowIndex, highIndex, value);
        }

        @Override
        public void fillObjects(long position, long count, Object value) {
            updatableBaseObjectArray.fill(position, count, value);
        }

        //[[Repeat.AutoGeneratedEnd]]
    }

    private static class Indexer2D extends Indexer {
        Indexer2D(Array baseArray, long[] baseDim, long[] tileDim) {
            super(baseArray, baseDim, tileDim);
            assert this.n == 2;
        }

        @Override
        long translate(final long index) {
            assert index >= 0; // must be checked in getXxx/setXxx methods
            long coordY = index / baseDim0;
            long coordX = index - coordY * baseDim0;
            long cStartX = coordX - coordX % tileDim0;
            long cStartY = coordY - coordY % tileDimLast;
            long restX = baseDim0 - cStartX;
            long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
            long restY = baseDimLast - cStartY;
            long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
            long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;
            long indexInTile = (coordY - cStartY) * currentTileDimX + (coordX - cStartX);
            long indexInBase = previousVolume + indexInTile;
            assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase < baseLength :
                "A bug in " + getClass() + ": previousVolume = " + previousVolume + ", "
                    + index + " translated to " + indexInBase + ", baseAndTileDimMul = "
                    + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//            System.out.println(index + " translated to " + indexInBase);
            return indexInBase;
        }

        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
        //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
        @Override
        public long indexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseBitArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseBitArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillBits(long position, long count, boolean value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseBitArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        @Override
        public long indexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseCharArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseCharArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillChars(long position, long count, char value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseCharArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseByteArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseByteArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillBytes(long position, long count, byte value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseByteArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseShortArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseShortArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillShorts(long position, long count, short value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseShortArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseIntArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseIntArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillInts(long position, long count, int value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseIntArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseLongArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseLongArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillLongs(long position, long count, long value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseLongArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseFloatArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseFloatArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillFloats(long position, long count, float value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseFloatArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillDoubles(long position, long count, double value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseDoubleArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseObjectArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseObjectArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillObjects(long position, long count, Object value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX - coordX % tileDim0;
                long cStartY = coordY - coordY % tileDimLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseObjectArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedEnd]]
    }

    private static class Indexer3D extends Indexer {
        final long baseDimXY;
        Indexer3D(Array baseArray, long[] baseDim, long[] tileDim) {
            super(baseArray, baseDim, tileDim);
            assert this.n == 3;
            this.baseDimXY = baseDimMul[2];
        }

        @Override
        long translate(final long index) {
            assert index >= 0; // must be checked in getXxx/setXxx methods
            long a = index;
            long coordZ = a / baseDimXY;
            a -= coordZ * baseDimXY;
            long coordY = a / baseDim[0];
            long coordX = a - coordY * baseDim[0];
            long cStartX = coordX - coordX % tileDim[0];
            long cStartY = coordY - coordY % tileDim[1];
            long cStartZ = coordZ - coordZ % tileDim[2];
            long restX = baseDim[0] - cStartX;
            long currentTileDimX = tileDim[0] <= restX ? tileDim[0] : restX;
            long restY = baseDim[1] - cStartY;
            long currentTileDimY = tileDim[1] <= restY ? tileDim[1] : restY;
            long restZ = baseDim[2] - cStartZ;
            long currentTileDimZ = tileDim[2] <= restZ ? tileDim[2] : restZ;
            long previousVolume = baseDimXY * cStartZ
                + baseDim[0] * cStartY * currentTileDimZ
                + cStartX * currentTileDimY * currentTileDimZ;
            long indexInTile = ((coordZ - cStartZ) * currentTileDimY + (coordY - cStartY))
                * currentTileDimX + (coordX - cStartX);
            long indexInBase = previousVolume + indexInTile;
            assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase < baseLength :
                "A bug in " + getClass() + ": previousVolume = " + previousVolume + ", "
                    + index + " translated to " + indexInBase + ", baseAndTileDimMul = "
                    + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//            System.out.println(index + " translated to " + indexInBase);
            return indexInBase;
        }
    }

    private static class IndexerForPowerOfTwo extends Indexer {
        /**
         * <tt>tileDimMask[k] = {@link #tileDim}[k]-1</tt> (2<sup><i>l</i></sup>&minus;1).
         */
        final long[] tileDimMask;

        /**
         * <tt>{@link #tileDimMask}[0]</tt>
         */
        final long tileDimMask0;

        /**
         * <tt>{@link #tileDimMask}[{@link #n}-1]</tt>
         */
        final long tileDimMaskLast;

        /**
         * <tt>tileDim[k] = 2<sup>tileDimLog[k]</sup></tt>.
         */
        final int[] tileDimLog;

        IndexerForPowerOfTwo(Array baseArray, long[] baseDim, long[] tileDim) {
            super(baseArray, baseDim, tileDim);
            if (!isPowerOfTwo(tileDim))
                throw new AssertionError("Some tile dimension is not 2^k: " + JArrays.toString(tileDim, "x", 1000));
            this.tileDimMask = new long[n];
            for (int k = 0; k < n; k++) {
                this.tileDimMask[k] = tileDim[k] - 1;
            }
            this.tileDimMask0 = this.tileDimMask[0];
            this.tileDimMaskLast = this.tileDimMask[n - 1];
            this.tileDimLog = new int[n];
            for (int k = 0; k < n; k++) {
                this.tileDimLog[k] = 63 - Long.numberOfLeadingZeros(tileDim[k]);
            }
        }

        @Override
        long translate(final long index) {
            assert index >= 0; // must be checked in getXxx/setXxx methods
            if (n == 1) {
                return index;
            }
            long previousVolume = 0;
            // Summary size of all tiles before this: previousVolume =
            //       baseDim[0] * baseDim[1] * ... * baseDim[n-3] * baseDim[n-2] * cStart[n-1]
            //     + baseDim[0] * baseDim[1] * ... * baseDim[n-3] * cStart[n-2] * currentTileDim[n-1]
            //     + . . .
            //     + cStart[0] * currentTileDim[1] * ... * currentTileDim[n-1]
            // where
            //     cStart[k] = coord[k] - coord[k] % tileDim[k]
            // is the start element of the current tile and
            //     currentTileDim[k] = min(tileDim[k], baseDim[k] - cStart[k])
            // are dimensions of the current tile, truncated to the matrix sizes.
            // If all baseDim[k] % tileDim[k] == 0, this formula is identical to tileIndex * tileSize,
            // where tileIndex is calculated on the base of tileCoordinates[k] = coord[k] / tileDim[k]
            // For example, in 3D case: previousVolume =
            //       baseDimX * baseDimY * cStartZ
            //     + baseDimX * cStartY * currentTileDimZ
            //     + cStartX * currentTileDimY * currentTileDimZ
            // in 2D case: previousVolume =
            //       baseDimX * cStartY
            //     + cStartX * currentTileDimY
            long a = index;
            long currentTileDimMul = 1;
            for (int k = n - 1; k > 0; k--) { // 1st loop for finding previousVolume
                long coordinate = a / baseDimMul[k]; // coordinate #k, coord[k] in terms above
                a -= coordinate * baseDimMul[k];
                long cStart = coordinate & ~tileDimMask[k]; // cStart[k]: starting coordinate in this tile
                previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                long rest = baseDim[k] - cStart;
                if (tileDim[k] <= rest) {
                    currentTileDimMul <<= tileDimLog[k];
                } else {
                    currentTileDimMul *= rest;
                }
            }
            // now "a" is coordinate #0
            previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
            long indexInTile = 0;
            // Index of the current tile:
            //     coordInTile[0]
            //     + currentTileDim[0] * coordInTile[1]
            //     + currentTileDim[0] * currentTileDim[1] * coordInTile[2]
            //     + . . .
            //     + currentTileDim[0] * currentTileDim[1] * ... * currentTileDim[n-2] * coordInTile[n-1]
            // where
            //     coordInTile[k] = coord[k] % tileDim[k]
            a = index;
            currentTileDimMul = 1;
            for (int k = 0; k < n - 1; k++) { // 2nd loop for finding indexInTile
                long dim = baseDim[k];
                long b = a / dim;
                long coord = a - b * dim; // coordinate #k, coord[k] in terms above; here "*" is faster than "%"
                long coordInTile = coord & tileDimMask[k];
                indexInTile += coordInTile * currentTileDimMul;
                long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                long rest = dim - cStart;
                if (tileDim[k] <= rest) {
                    currentTileDimMul <<= tileDimLog[k];
                } else {
                    currentTileDimMul *= rest;
                }
                a = b;
            }
            // now "a" is coordinate #n-1, coord[n-1] in terms above
            indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
            long indexInBase = previousVolume + indexInTile;
            assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase < baseLength :
                "A bug in translate() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                    + index + " translated to " + indexInBase + ", baseAndTileDimMul = "
                    + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//            System.out.println(index + " translated to " + indexInBase);
            return indexInBase;
        }//translate

        //[[Repeat.SectionStart getDataForPowerOfTwo_method_impl]]
        @Override
        void getData(final long arrayPos, Object destArray, int destArrayOffset, int count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in getData/setData methods
            assert count >= 0; // must be checked in getData/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; destArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     destArray[destArrayOffset] = getXxx(index);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            int len;
            for (long index = arrayPos; count > 0; index += len, destArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? (int)restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in getData/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in getData() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + baseArray;
                baseArray.getData(indexInBase, destArray, destArrayOffset, len);
//                System.out.println("getData: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//getData
        //[[Repeat.SectionEnd getDataForPowerOfTwo_method_impl]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getDataForPowerOfTwo_method_impl)
        //  getData ==> setData;;
        //  \bbaseArray\b ==> updatableBaseArray ;;
        //  destArray ==> srcArray ;;
        //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==>
        //  setXxx(index, $1)   !! Auto-generated: NOT EDIT !! ]]
        @Override
        void setData(final long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in setData/setData methods
            assert count >= 0; // must be checked in setData/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; srcArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     setXxx(index, srcArray[srcArrayOffset]);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            int len;
            for (long index = arrayPos; count > 0; index += len, srcArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? (int)restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in setData/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + updatableBaseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in setData() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + updatableBaseArray;
                updatableBaseArray.setData(indexInBase, srcArray, srcArrayOffset, len);
//                System.out.println("setData: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//setData
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getDataForPowerOfTwo_method_impl)
        //  \bint\s+(destArrayOffset|count|len)\b ==> long $1 ;;
        //  \bObject\s+destArray\b ==> long[] destArray ;;
        //  \bbaseArray\b ==> baseBitArray ;;
        //  \(int\) ==> ;;
        //  getData ==> getBits    !! Auto-generated: NOT EDIT !! ]]
        @Override
        void getBits(final long arrayPos, long[] destArray, long destArrayOffset, long count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in getBits/setData methods
            assert count >= 0; // must be checked in getBits/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; destArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     destArray[destArrayOffset] = getXxx(index);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            long len;
            for (long index = arrayPos; count > 0; index += len, destArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in getBits/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + baseBitArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in getBits() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + baseBitArray;
                baseBitArray.getBits(indexInBase, destArray, destArrayOffset, len);
//                System.out.println("getBits: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//getBits
        //[[Repeat.IncludeEnd]]

        //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, getDataForPowerOfTwo_method_impl)
        //  getData ==> setBits;;
        //  \bbaseArray\b ==> updatableBaseBitArray ;;
        //  destArray ==> srcArray ;;
        //  (srcArray\[srcArrayOffset\])\s*=\s*getXxx\(index\) ==> setXxx(index, $1) ;;
        //  \bint\s+(srcArrayOffset|count|len)\b ==> long $1 ;;
        //  \bObject\s+srcArray\b ==> long[] srcArray ;;
        //  \(int\) ==>   !! Auto-generated: NOT EDIT !! ]]
        @Override
        void setBits(final long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
            assert arrayPos >= 0 && arrayPos <= baseLength - count; // must be checked in setBits/setData methods
            assert count >= 0; // must be checked in setBits/setData methods
            // We do not check count==0, though it is possible for empty matrices with zero dimensions
            // (danger of division by zero): all operations below are performed only inside the loop while count>0

            // We need to perform the following loop:
            //
            // for (long index = arrayPos; count > 0; srcArrayOffset++, index++, count--) {
            //     long baseIndex = translate(index);
            //     setXxx(index, srcArray[srcArrayOffset]);
            // }
            //
            // Below is an optimization of such a loop, processing contiguous data blocks
            long len;
            for (long index = arrayPos; count > 0; index += len, srcArrayOffset += len, count -= len) {
                long previousVolume = 0;
                long a = index;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k]; // coordinate #k, coord[k]
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k]; // cStart[k]: starting coordinate in this tile
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                // now "a" is coordinate #0
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                assert index == arrayPos || coord0InTile == 0 : "Non-aligned index for non-first tile";
                final long c0Start = a - coord0InTile; // cStart[0]: starting coordinate in this tile
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0; // now it is the tile x-dimension
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = index / baseDim0;
                for (int k = 1; k < n - 1; k++) { // finding indexInTile
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim; // coordinate #k; here "*" is faster than "%"
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile; // cStart[k]: starting coordinate in this tile
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                // now "a" is coordinate #n-1
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(index) :
                        "A bug in setBits/translate in " + getClass() + ": previousVolume = " + previousVolume + ", "
                            + index + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", "
                            + updatableBaseBitArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase + len <= baseLength :
                    "A bug in setBits() in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + index + " translated to " + indexInBase + "(+" + len + "), arrayPos = " + arrayPos
                        + ", count = " + count + ", original count = " + (count + index - arrayPos)
                        + ", baseAndTileDimMul = " + JArrays.toString(baseDimMul, ",", 1000) + ", "
                        + updatableBaseBitArray;
                updatableBaseBitArray.setBits(indexInBase, srcArray, srcArrayOffset, len);
//                System.out.println("setBits: " + arrayPos + ": " + index + " translated to " + indexInBase);
            }
        }//setBits
        //[[Repeat.IncludeEnd]]

        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
        //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
        @Override
        public long indexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfBit/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseBitArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfBit/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseBitArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillBits(long position, long count, boolean value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfBit/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseBitArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        @Override
        public long indexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfChar/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseCharArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfChar/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseCharArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillChars(long position, long count, char value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfChar/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseCharArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfByte/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseByteArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfByte/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseByteArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillBytes(long position, long count, byte value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfByte/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseByteArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfShort/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseShortArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfShort/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseShortArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillShorts(long position, long count, short value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfShort/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseShortArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfInt/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseIntArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfInt/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseIntArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillInts(long position, long count, int value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfInt/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseIntArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfLong/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseLongArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfLong/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseLongArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillLongs(long position, long count, long value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfLong/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseLongArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfFloat/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseFloatArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfFloat/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseFloatArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillFloats(long position, long count, float value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfFloat/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseFloatArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfDouble/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfDouble/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillDoubles(long position, long count, double value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfDouble/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseDoubleArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long previousVolume = 0;
                long a = lowIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = lowIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(lowIndex) :
                        "A bug in indexOfObject/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + lowIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseObjectArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long previousVolume = 0;
                long a = highIndex;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                len = coord0InTile + 1 < count ? coord0InTile + 1 : count;
                assert len > 0 : "Zero len";
                a = highIndex / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(highIndex) :
                        "A bug in indexOfObject/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + highIndex + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseObjectArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillObjects(long position, long count, Object value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long previousVolume = 0;
                long a = position;
                long currentTileDimMul = 1;
                for (int k = n - 1; k > 0; k--) {
                    long coord = a / baseDimMul[k];
                    a -= coord * baseDimMul[k];
                    long cStart = coord & ~tileDimMask[k];
                    previousVolume += cStart * baseDimMul[k] * currentTileDimMul;
                    long rest = baseDim[k] - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                }
                previousVolume += (a & ~tileDimMask0) * currentTileDimMul;
                final long coord0InTile = a & tileDimMask0;
                final long c0Start = a - coord0InTile;
                long indexInTile = coord0InTile;
                final long rest0 = baseDim0 - c0Start;
                currentTileDimMul = tileDim0 < rest0 ? tileDim0 : rest0;
                final long restInTile0 = currentTileDimMul - coord0InTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                a = position / baseDim0;
                for (int k = 1; k < n - 1; k++) {
                    long dim = baseDim[k];
                    long b = a / dim;
                    long coord = a - b * dim;
                    long coordInTile = coord & tileDimMask[k];
                    indexInTile += coordInTile * currentTileDimMul;
                    long cStart = coord - coordInTile;
                    long rest = dim - cStart;
                    if (tileDim[k] <= rest) {
                        currentTileDimMul <<= tileDimLog[k];
                    } else {
                        currentTileDimMul *= rest;
                    }
                    a = b;
                }
                indexInTile += (a & tileDimMaskLast) * currentTileDimMul;
                long indexInBase = previousVolume + indexInTile;
                if (DEBUG_MODE) {
                    assert indexInBase == translate(position) :
                        "A bug in indexOfObject/translate in " + getClass()
                            + ": previousVolume = " + previousVolume + ", "
                            + position + " translated to " + indexInBase + "(+" + len + "), baseAndTileDimMul = "
                            + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
                }
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseObjectArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedEnd]]
    }

    private static class IndexerForPowerOfTwo2D extends IndexerForPowerOfTwo {
        IndexerForPowerOfTwo2D(Array baseArray, long[] baseDim, long[] tileDim) {
            super(baseArray, baseDim, tileDim);
            assert this.n == 2;
        }

        @Override
        long translate(final long index) {
            assert index >= 0; // must be checked in getXxx/setXxx methods
            long coordY = index / baseDim0;
            long coordX = index - coordY * baseDim0;
            long cStartX = coordX & ~tileDimMask0;
            long cStartY = coordY & ~tileDimMaskLast;
            long restX = baseDim0 - cStartX;
            long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
            long restY = baseDimLast - cStartY;
            long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
            long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;
            long indexInTile = (coordY - cStartY) * currentTileDimX + (coordX - cStartX);
            long indexInBase = previousVolume + indexInTile;
            assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase < baseLength :
                "A bug in " + getClass() + ": previousVolume = " + previousVolume + ", "
                    + index + " translated to " + indexInBase + ", baseAndTileDimMul = "
                    + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//            System.out.println(index + " translated to " + indexInBase);
            return indexInBase;
        }

        //[[Repeat() Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
        //           boolean(?=\s+value) ==> char,,byte,,short,,int,,long,,float,,double,,Object]]
        @Override
        public long indexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseBitArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfBit(long lowIndex, long highIndex, boolean value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseBitArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillBits(long position, long count, boolean value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfBit in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseBitArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        @Override
        public long indexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseCharArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfChar(long lowIndex, long highIndex, char value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseCharArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillChars(long position, long count, char value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfChar in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseCharArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseByteArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfByte(long lowIndex, long highIndex, byte value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseByteArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillBytes(long position, long count, byte value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfByte in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseByteArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseShortArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfShort(long lowIndex, long highIndex, short value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseShortArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillShorts(long position, long count, short value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfShort in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseShortArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseIntArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfInt(long lowIndex, long highIndex, int value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseIntArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillInts(long position, long count, int value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfInt in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseIntArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseLongArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfLong(long lowIndex, long highIndex, long value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseLongArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillLongs(long position, long count, long value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfLong in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseLongArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseFloatArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfFloat(long lowIndex, long highIndex, float value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseFloatArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillFloats(long position, long count, float value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfFloat in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseFloatArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfDouble(long lowIndex, long highIndex, double value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseDoubleArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillDoubles(long position, long count, double value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfDouble in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseDoubleArray.fill(indexInBase, len, value);
            }
        }

        @Override
        public long indexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            long len;
            for (; count > 0; lowIndex += len, count -= len) {
                long coordY = lowIndex / baseDim0;
                long coordX = lowIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + lowIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + lowIndex + " translated to " + indexInBase);
                long result = baseObjectArray.indexOf(indexInBase, indexInBase + len, value);
                if (result != -1) {
                    return lowIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public long lastIndexOfObject(long lowIndex, long highIndex, Object value) {
            if (lowIndex < 0) {
                lowIndex = 0;
            }
            if (highIndex > baseLength) {
                highIndex = baseLength;
            }
            if (highIndex <= lowIndex) { // necessary to provide correct calculation of count (without overflow)
                return -1;
            }
            long count =  highIndex - lowIndex;
            // See comments to the following algorithm in getData method
            highIndex--;
            long len;
            for (; count > 0; highIndex -= len, count -= len) {
                long coordY = highIndex / baseDim0;
                long coordX = highIndex - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                len = coordXInTile + 1 < count ? coordXInTile + 1 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= len - 1
                    && indexInBase <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + highIndex + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + highIndex + " translated to " + indexInBase);
                long result = baseObjectArray.lastIndexOf(indexInBase + 1 - len, indexInBase + 1, value);
                if (result != -1) {
                    return highIndex + result - indexInBase;
                }
            }
            return -1;
        }

        @Override
        public void fillObjects(long position, long count, Object value) {
            long len;
            for (; count > 0; position += len, count -= len) {
                long coordY = position / baseDim0;
                long coordX = position - coordY * baseDim0;
                long cStartX = coordX & ~tileDimMask0;
                long cStartY = coordY & ~tileDimMaskLast;
                long restX = baseDim0 - cStartX;
                long currentTileDimX = tileDim0 <= restX ? tileDim0 : restX;
                long restY = baseDimLast - cStartY;
                long currentTileDimY = tileDimLast <= restY ? tileDimLast : restY;
                long previousVolume = baseDim0 * cStartY + cStartX * currentTileDimY;

                long coordXInTile = coordX - cStartX;
                long coordYInTile = coordY - cStartY;
                final long restInTile0 = currentTileDimX - coordXInTile;
                len = restInTile0 < count ? restInTile0 : count;
                assert len > 0 : "Zero len";
                long indexInTile = coordYInTile * currentTileDimX + coordXInTile;
                long indexInBase = previousVolume + indexInTile;
                assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0
                    && indexInBase + len <= baseLength :
                    "A bug in indexOfObject in " + getClass() + ": previousVolume = " + previousVolume + ", "
                        + position + " translated to " + indexInBase + "(+" + len
                        + "), count = " + count + ", baseAndTileDimMul = "
                        + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//                System.out.println("index: " + position + " translated to " + indexInBase);
                updatableBaseObjectArray.fill(indexInBase, len, value);
            }
        }

        //[[Repeat.AutoGeneratedEnd]]
    }

    private static class IndexerForPowerOfTwo3D extends IndexerForPowerOfTwo {
        final long baseDimXY;
        IndexerForPowerOfTwo3D(Array baseArray, long[] baseDim, long[] tileDim) {
            super(baseArray, baseDim, tileDim);
            assert this.n == 3;
            this.baseDimXY = baseDimMul[2];
        }

        @Override
        long translate(final long index) {
            assert index >= 0; // must be checked in getXxx/setXxx methods
            long a = index;
            long coordZ = a / baseDimXY;
            a -= coordZ * baseDimXY;
            long coordY = a / baseDim[0];
            long coordX = a - coordY * baseDim[0];
            long cStartX = coordX & ~tileDimMask[0];
            long cStartY = coordY & ~tileDimMask[1];
            long cStartZ = coordZ & ~tileDimMask[2];
            long restX = baseDim[0] - cStartX;
            long currentTileDimX = tileDim[0] <= restX ? tileDim[0] : restX;
            long restY = baseDim[1] - cStartY;
            long currentTileDimY = tileDim[1] <= restY ? tileDim[1] : restY;
            long restZ = baseDim[2] - cStartZ;
            long currentTileDimZ = tileDim[2] <= restZ ? tileDim[2] : restZ;
            long previousVolume = baseDimXY * cStartZ
                + baseDim[0] * cStartY * currentTileDimZ
                + cStartX * currentTileDimY * currentTileDimZ;
            long indexInTile = ((coordZ - cStartZ) * currentTileDimY + (coordY - cStartY))
                * currentTileDimX + (coordX - cStartX);
            long indexInBase = previousVolume + indexInTile;
            assert previousVolume >= 0 && indexInTile >= 0 && indexInBase >= 0 && indexInBase < baseLength :
                "A bug in " + getClass() + ": previousVolume = " + previousVolume + ", "
                    + index + " translated to " + indexInBase + ", baseAndTileDimMul = "
                    + JArrays.toString(baseDimMul, ",", 1000) + ", " + baseArray;
//            System.out.println(index + " translated to " + indexInBase);
            return indexInBase;
        }
    }
}
