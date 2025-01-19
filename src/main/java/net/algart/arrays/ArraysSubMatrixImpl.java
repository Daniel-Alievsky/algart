/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Implementation of {@link Matrix#subMatrix} methods.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysSubMatrixImpl {

    static final long OUTSIDE_INDEX = -1;

    interface SubMatrixArray {
        Matrix<? extends Array> baseMatrix();

        Matrix.ContinuationMode continuationMode();

        long[] from();

        long[] to();

        long[] dimensions();
    }

    /*Repeat() boolean(?!\s+(isLazy|mustBeInside)) ==> char,,byte,,short,,int,,long,,float,,double,,E;;
               @Override\s+public\s+\w+\s+(getBits|setBits|nextQuickPosition).*?}(?=\s*(?:public|@Override)) ==>
                   ,, ...;;
               Bit ==> Char,,Byte,,Short,,Int,,Long,,Float,,Double,,Object;;
               public\s+byte\s+(getByte)|public\s+short\s+(getShort) ==> public int $1$2,, ...;;
               (index\s*==\s*OUTSIDE_INDEX\s*\?\s*outsideValue) ==> $1,,$1 & 0xFF,,$1 & 0xFFFF,,$1,, ...;;
               (super\(checkBounds) ==> $1,,$1,,$1,,$1,,$1,,$1,,$1,,super(baseMatrix.array().elementType(),
                checkBounds;;
               (ObjectArray)(?!\() ==> $1<E>,, ...;;
               (get|set)Object ==> $1,, ...;;
               E\[\" ==> \" + elementType().getName() + \"[\",, ...*/

    static class SubMatrixBitArray extends AbstractBitArray implements SubMatrixArray {
        private final Matrix<? extends BitArray> baseMatrix;
        private final BitArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final boolean outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixBitArray(Matrix<? extends BitArray> baseMatrix,
            long[] position, long[] dimensions, boolean outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nBitCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public boolean getBit(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getBit(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (!indexer.bitsBlocksImplemented()) {
                super.getBits(arrayPos, destArray, destArrayOffset, count);
                return;
            }
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos, length, getClass());
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1, length, getClass());
            }
            indexer.getBits(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public long nextQuickPosition(long position) {
            // Usually getBits works much faster for bit submatrices
            return !indexer.bitsBlocksImplemented() || position >= length ? -1 : position < 0 ? 0 : position;
        }

        @Override
        public long indexOf(long lowIndex, long highIndex, boolean value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfBit(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfBit(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray boolean["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableBitArray extends AbstractUpdatableBitArray implements SubMatrixArray {
        private final Matrix<? extends BitArray> baseMatrix;
        private final UpdatableBitArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final boolean outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableBitArray(Matrix<? extends UpdatableBitArray> baseMatrix,
            long[] position, long[] dimensions, boolean outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nBitCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean getBit(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getBit(index);
        }

        @Override
        public void setBit(long index, boolean value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setBit(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public void getBits(long arrayPos, long[] destArray, long destArrayOffset, long count) {
            if (!indexer.bitsBlocksImplemented()) {
                super.getBits(arrayPos, destArray, destArrayOffset, count);
                return;
            }
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos, length, getClass());
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1, length, getClass());
            }
            indexer.getBits(arrayPos, destArray, destArrayOffset, count);
        }

        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        @Override
        public UpdatableBitArray setBits(long arrayPos, long[] srcArray, long srcArrayOffset, long count) {
            if (!indexer.bitsBlocksImplemented()) {
                return super.setBits(arrayPos, srcArray, srcArrayOffset, count);
            }
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos, length, getClass());
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1, length, getClass());
            }
            indexer.setBits(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        @Override
        public long nextQuickPosition(long position) {
            // Usually getBits works much faster to bit submatrices
            return !indexer.bitsBlocksImplemented() || position >= length ? -1 : position < 0 ? 0 : position;
        }

        @Override
        public long indexOf(long lowIndex, long highIndex, boolean value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfBit(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, boolean value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfBit(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableBitArray fill(long position, long count, boolean value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nBitCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillBits(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray boolean["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !!*/

    static class SubMatrixCharArray extends AbstractCharArray implements SubMatrixArray {
        private final Matrix<? extends CharArray> baseMatrix;
        private final CharArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final char outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixCharArray(Matrix<? extends CharArray> baseMatrix,
            long[] position, long[] dimensions, char outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nCharCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public char getChar(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getChar(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, char value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfChar(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, char value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfChar(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray char["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableCharArray extends AbstractUpdatableCharArray implements SubMatrixArray {
        private final Matrix<? extends CharArray> baseMatrix;
        private final UpdatableCharArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final char outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableCharArray(Matrix<? extends UpdatableCharArray> baseMatrix,
            long[] position, long[] dimensions, char outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nCharCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public char getChar(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getChar(index);
        }

        @Override
        public void setChar(long index, char value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setChar(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, char value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfChar(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, char value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfChar(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableCharArray fill(long position, long count, char value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nCharCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillChars(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray char["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixByteArray extends AbstractByteArray implements SubMatrixArray {
        private final Matrix<? extends ByteArray> baseMatrix;
        private final ByteArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final byte outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixByteArray(Matrix<? extends ByteArray> baseMatrix,
            long[] position, long[] dimensions, byte outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nByteCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public int getByte(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue & 0xFF : baseArray.getByte(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, byte value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfByte(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, byte value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfByte(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray byte["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableByteArray extends AbstractUpdatableByteArray implements SubMatrixArray {
        private final Matrix<? extends ByteArray> baseMatrix;
        private final UpdatableByteArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final byte outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableByteArray(Matrix<? extends UpdatableByteArray> baseMatrix,
            long[] position, long[] dimensions, byte outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nByteCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public int getByte(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue & 0xFF : baseArray.getByte(index);
        }

        @Override
        public void setByte(long index, byte value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setByte(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, byte value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfByte(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, byte value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfByte(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableByteArray fill(long position, long count, byte value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nByteCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillBytes(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray byte["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixShortArray extends AbstractShortArray implements SubMatrixArray {
        private final Matrix<? extends ShortArray> baseMatrix;
        private final ShortArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final short outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixShortArray(Matrix<? extends ShortArray> baseMatrix,
            long[] position, long[] dimensions, short outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nShortCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public int getShort(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue & 0xFFFF : baseArray.getShort(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, short value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfShort(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, short value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfShort(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray short["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableShortArray extends AbstractUpdatableShortArray implements SubMatrixArray {
        private final Matrix<? extends ShortArray> baseMatrix;
        private final UpdatableShortArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final short outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableShortArray(Matrix<? extends UpdatableShortArray> baseMatrix,
            long[] position, long[] dimensions, short outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nShortCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public int getShort(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue & 0xFFFF : baseArray.getShort(index);
        }

        @Override
        public void setShort(long index, short value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setShort(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, short value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfShort(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, short value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfShort(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableShortArray fill(long position, long count, short value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nShortCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillShorts(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray short["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixIntArray extends AbstractIntArray implements SubMatrixArray {
        private final Matrix<? extends IntArray> baseMatrix;
        private final IntArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final int outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixIntArray(Matrix<? extends IntArray> baseMatrix,
            long[] position, long[] dimensions, int outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nIntCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public int getInt(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getInt(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, int value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfInt(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, int value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfInt(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray int["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableIntArray extends AbstractUpdatableIntArray implements SubMatrixArray {
        private final Matrix<? extends IntArray> baseMatrix;
        private final UpdatableIntArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final int outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableIntArray(Matrix<? extends UpdatableIntArray> baseMatrix,
            long[] position, long[] dimensions, int outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nIntCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public int getInt(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getInt(index);
        }

        @Override
        public void setInt(long index, int value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setInt(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, int value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfInt(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, int value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfInt(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableIntArray fill(long position, long count, int value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nIntCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillInts(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray int["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixLongArray extends AbstractLongArray implements SubMatrixArray {
        private final Matrix<? extends LongArray> baseMatrix;
        private final LongArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final long outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixLongArray(Matrix<? extends LongArray> baseMatrix,
            long[] position, long[] dimensions, long outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nLongCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public long getLong(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getLong(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, long value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfLong(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfLong(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray long["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableLongArray extends AbstractUpdatableLongArray implements SubMatrixArray {
        private final Matrix<? extends LongArray> baseMatrix;
        private final UpdatableLongArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final long outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableLongArray(Matrix<? extends UpdatableLongArray> baseMatrix,
            long[] position, long[] dimensions, long outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nLongCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public long getLong(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getLong(index);
        }

        @Override
        public void setLong(long index, long value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setLong(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, long value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfLong(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, long value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfLong(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableLongArray fill(long position, long count, long value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nLongCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillLongs(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray long["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixFloatArray extends AbstractFloatArray implements SubMatrixArray {
        private final Matrix<? extends FloatArray> baseMatrix;
        private final FloatArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final float outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixFloatArray(Matrix<? extends FloatArray> baseMatrix,
            long[] position, long[] dimensions, float outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nFloatCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public float getFloat(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getFloat(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, float value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfFloat(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, float value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfFloat(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray float["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableFloatArray extends AbstractUpdatableFloatArray implements SubMatrixArray {
        private final Matrix<? extends FloatArray> baseMatrix;
        private final UpdatableFloatArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final float outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableFloatArray(Matrix<? extends UpdatableFloatArray> baseMatrix,
            long[] position, long[] dimensions, float outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nFloatCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public float getFloat(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getFloat(index);
        }

        @Override
        public void setFloat(long index, float value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setFloat(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, float value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfFloat(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, float value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfFloat(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableFloatArray fill(long position, long count, float value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nFloatCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillFloats(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray float["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixDoubleArray extends AbstractDoubleArray implements SubMatrixArray {
        private final Matrix<? extends DoubleArray> baseMatrix;
        private final DoubleArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final double outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixDoubleArray(Matrix<? extends DoubleArray> baseMatrix,
            long[] position, long[] dimensions, double outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nDoubleCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public double getDouble(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getDouble(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, double value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfDouble(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfDouble(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray double["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableDoubleArray extends AbstractUpdatableDoubleArray implements SubMatrixArray {
        private final Matrix<? extends DoubleArray> baseMatrix;
        private final UpdatableDoubleArray baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final double outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableDoubleArray(Matrix<? extends UpdatableDoubleArray> baseMatrix,
            long[] position, long[] dimensions, double outsideValue, Matrix.ContinuationMode mode)
        {
            super(checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nDoubleCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public double getDouble(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.getDouble(index);
        }

        @Override
        public void setDouble(long index, double value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.setDouble(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, double value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfDouble(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, double value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfDouble(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableDoubleArray fill(long position, long count, double value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nDoubleCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillDoubles(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray double["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixObjectArray<E> extends AbstractObjectArray<E> implements SubMatrixArray {
        private final Matrix<? extends ObjectArray<E>> baseMatrix;
        private final ObjectArray<E> baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final E outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixObjectArray(Matrix<? extends ObjectArray<E>> baseMatrix,
            long[] position, long[] dimensions, E outsideValue, Matrix.ContinuationMode mode)
        {
            super(baseMatrix.array().elementType(),
                checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nObjectCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public boolean isLazy() {
            return baseArray.isLazy();
        }

        @Override
        public E get(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.get(index);
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, E value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfObject(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, E value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfObject(lowIndex, highIndex, value);
        }

        public String toString() {
            return "immutable AlgART baseArray " + elementType().getName() + "["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    static class SubMatrixUpdatableObjectArray<E> extends AbstractUpdatableObjectArray<E> implements SubMatrixArray {
        private final Matrix<? extends ObjectArray<E>> baseMatrix;
        private final UpdatableObjectArray<E> baseArray;
        private final long[] position;
        private final long[] dimensions;
        private final ArraysSubMatrixIndexer indexer;
        private final E outsideValue;
        private final Matrix.ContinuationMode continuationMode;

        SubMatrixUpdatableObjectArray(Matrix<? extends UpdatableObjectArray<E>> baseMatrix,
            long[] position, long[] dimensions, E outsideValue, Matrix.ContinuationMode mode)
        {
            super(baseMatrix.array().elementType(),
                checkBounds(baseMatrix, position, dimensions, mode), false, baseMatrix.array());
            this.baseMatrix = baseMatrix;
            this.baseArray = baseMatrix.array();
            this.position = position.clone();
            this.dimensions = dimensions.clone();
            this.outsideValue = outsideValue;
            this.continuationMode = mode;
            if (mode == Matrix.ContinuationMode.NONE || mode.isConstant()
                || isInside(baseMatrix, position, dimensions))
            {
                this.indexer = new ArraysSubMatrixConstantlyContinuedIndexer(baseMatrix, position, dimensions,
                    Arrays.nObjectCopies(this.length, this.outsideValue));
            } else if (mode == Matrix.ContinuationMode.CYCLIC || mode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                this.indexer = new ArraysSubMatrixCyclicIndexer(baseMatrix, position, dimensions,
                    mode == Matrix.ContinuationMode.PSEUDO_CYCLIC);
            } else if (mode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                this.indexer = new ArraysSubMatrixMirrorCyclicIndexer(baseMatrix, position, dimensions);
            } else {
                throw new AssertionError("Unsupported continuation mode: " + mode);
            }
        }

        public Matrix<? extends Array> baseMatrix() {
            return baseMatrix;
        }

        public Matrix.ContinuationMode continuationMode() {
            return continuationMode;
        }

        public long[] from() {
            return position.clone();
        }

        public long[] to() {
            long[] result = new long[position.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = position[k] + dimensions[k];
            }
            return result;
        }

        public long[] dimensions() {
            return dimensions.clone();
        }

        @Override
        public E get(long index) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            return index == OUTSIDE_INDEX ? outsideValue : baseArray.get(index);
        }

        @Override
        public void set(long index, E value) {
            if (index < 0 || index >= length) {
                throw rangeException(index);
            }
            index = indexer.translate(index);
            if (index != OUTSIDE_INDEX) {
                baseArray.set(index, value);
            }
        }

        @Override
        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.getData(arrayPos, destArray, destArrayOffset, count);
        }



        @Override
        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            indexer.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }





        @Override
        public long indexOf(long lowIndex, long highIndex, E value) {
            return !indexer.indexOfImplemented() ?
                super.indexOf(lowIndex, highIndex, value) :
                indexer.indexOfObject(lowIndex, highIndex, value);
        }

        @Override
        public long lastIndexOf(long lowIndex, long highIndex, E value) {
            return !indexer.indexOfImplemented() ?
                super.lastIndexOf(lowIndex, highIndex, value) :
                indexer.lastIndexOfObject(lowIndex, highIndex, value);
        }

        @Override
        public UpdatableObjectArray<E> fill(long position, long count, E value) {
            checkSubArrArguments(position, count);
            if (baseMatrix.isTiled()) {
                UpdatableArray a = position == 0 && count == length() ? this : subArr(position, count);
                if (ArraysSubMatrixCopier.copySubMatrixArray(null, a, Arrays.nObjectCopies(count, value))) {
                    return this;
                }
            }
            indexer.fillObjects(position, count, value);
            return this;
        }

        public String toString() {
            return "unresizable AlgART baseArray " + elementType().getName() + "["
                + length + "] containing submatrix of " + baseMatrix;
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    static long checkBounds(Matrix<?> baseMatrix, long[] position, long[] dimensions,
        Matrix.ContinuationMode continuationMode)
        throws IllegalArgumentException, IndexOutOfBoundsException
    {
        Objects.requireNonNull(baseMatrix, "Null baseMatrix");
        Objects.requireNonNull(position, "Null position[] Java baseArray");
        Objects.requireNonNull(dimensions, "Null dimensions[] Java baseArray");
        if (position.length != baseMatrix.dimCount()) {
            throw new IllegalArgumentException("Illegal number of position[] elements: "
                + position.length + " instead of " + baseMatrix.dimCount());
        }
        assert continuationMode != null;
        if (dimensions.length != position.length) {
            throw new IllegalArgumentException("Illegal number of dimensions[] elements: "
                + dimensions.length + " instead of " + baseMatrix.dimCount());
        }
        for (int k = 0; k < position.length; k++) {
            if (dimensions[k] < 0) {
                throw new IndexOutOfBoundsException("Negative submatrix dimension: dimensions["
                    + k + "] = " + dimensions[k]);
            }
            long d = baseMatrix.dim(k);
            if (continuationMode == Matrix.ContinuationMode.NONE || (d == 0 && !continuationMode.isConstant())) {
                if (position[k] < 0) {
                    throw new IndexOutOfBoundsException("Negative position[" + k + "] = " + position[k]
                        + (continuationMode != Matrix.ContinuationMode.NONE ?
                        ", and this matrix dimension is zero - it is not allowed for " + continuationMode : "")
                        + " (start submatrix coordinate in " + baseMatrix + ")");
                }
                if (position[k] > d - dimensions[k]) {
                    throw new IndexOutOfBoundsException("Too large position[" + k + "] + dimensions[" + k
                        + "] = " + position[k] + " + " + dimensions[k] + " > " + d
                        + (continuationMode != Matrix.ContinuationMode.NONE ?
                        ", and this matrix dimension is zero - it is not allowed for " + continuationMode : "")
                        + " (start submatrix coordinate + dimension in " + baseMatrix + ")");
                }
            } else {
                if (position[k] > Long.MAX_VALUE - dimensions[k]) {
                    throw new IndexOutOfBoundsException("Too large position[" + k + "] + dimensions[" + k
                        + "] = " + position[k] + " + " + dimensions[k] + " > Long.MAX_VALUE");
                }
            }
        }
        long len = Arrays.longMul(dimensions);
        if (len == Long.MIN_VALUE) {
            throw new IndexOutOfBoundsException("Too large submatrix dimensions "
                + "dim[0] * dim[1] * ... = " + JArrays.toString(dimensions, " * ", 100) + " > Long.MAX_VALUE");
        }
        return len;
    }

    static boolean isInside(Matrix<?> baseMatrix, long[] position, long[] dimensions) {
        for (int k = 0; k < position.length; k++) {
            long d = baseMatrix.dim(k);
            if (position[k] < 0 || position[k] > d - dimensions[k]) {
                return false;
            }
        }
        return true;
    }

}
