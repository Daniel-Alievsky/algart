/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.IRectangularArea;
import net.algart.matrices.Abstract2DProcessor;

import java.util.Objects;

public abstract class Quick2DAverager extends Abstract2DProcessor {
    final boolean twoStage;
    final int dimX;
    final long dimY;

    boolean strictDivision = false;
    boolean rounding = true;

    private Quick2DAverager(Class<?> elementType, long[] dimensions, boolean twoStage) {
        super(elementType, dimensions);
        this.twoStage = twoStage;
        this.dimX = dimX();
        this.dimY = dimY();
    }

    public static Quick2DAverager newInstance(Class<?> elementType, long dimX, long dimY) {
        return newInstance(elementType, new long[]{dimX, dimY}, false);
    }

    public static Quick2DAverager newTwoStageInstance(Class<?> elementType, long dimX, long dimY) {
        return newInstance(elementType, new long[]{dimX, dimY}, true);
    }

    public static Quick2DAverager newInstance(Class<?> elementType, long[] dimensions, boolean twoStage) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (elementType == char.class) {
            return new ForChar(elementType, dimensions, twoStage);
        } else if (elementType == boolean.class) {
            return new ForBit(elementType, dimensions, twoStage);
        } else if (elementType == byte.class) {
            return new ForByte(elementType, dimensions, twoStage);
        } else if (elementType == short.class) {
            return new ForShort(elementType, dimensions, twoStage);
        } else if (elementType == int.class) {
            return new ForInt(elementType, dimensions, twoStage);
        } else if (elementType == long.class) {
            return new ForLong(elementType, dimensions, twoStage);
        } else if (elementType == float.class) {
            return new ForFloat(elementType, dimensions, twoStage);
        } else if (elementType == double.class) {
            return new ForDouble(elementType, dimensions, twoStage);
        } else {
            throw new UnsupportedOperationException("Non-primitive element type " + elementType + " is not supported");
        }
    }

    public static boolean isSupportedRectangle(IRectangularArea rectangle) {
        return unsupportedRectangleCode(rectangle) == 0;
    }

    public abstract boolean isInteger();

    public boolean isStrictDivision() {
        return strictDivision;
    }

    public Quick2DAverager setStrictDivision(boolean strictDivision) {
        this.strictDivision = strictDivision;
        return this;
    }

    public boolean isRounding() {
        return rounding;
    }

    public Quick2DAverager setRounding(boolean rounding) {
        this.rounding = rounding;
        return this;
    }

    public boolean isTwoStage() {
        return twoStage;
    }

    public Matrix<? extends UpdatablePArray> filter(Matrix<? extends PArray> source, IRectangularArea rectangle) {
        final Matrix<? extends UpdatablePArray> result = Arrays.SMM.newMatrix(UpdatablePArray.class, source);
        filter(result, source, rectangle);
        return result;
    }

    public void filter(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            long minX,
            long minY,
            long sizeX,
            long sizeY) {
        if (sizeX < 0 || sizeY < 0) {
            throw new IllegalArgumentException("Negative sizeX=" + sizeX + " or sizeY=" + sizeY);
        }
        filter(result, source, IRectangularArea.valueOf(minX, minY, minX + sizeX - 1, minY + sizeY - 1));
    }

    public void filter(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            IRectangularArea rectangle) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(source, "Null source");
        Objects.requireNonNull(rectangle, "Null rectangle");
        checkCompatibility(result);
        checkCompatibility(source);
        if (rectangle.coordCount() != 2) {
            throw new IllegalArgumentException("Rectangular area " + rectangle + " is not 2-dimensional");
        }
        switch (unsupportedRectangleCode(rectangle)) {
            case 1:
                throw new IllegalArgumentException("Rectangular area " + rectangle + " is not 2-dimensional");
            case 2:
                throw new IllegalArgumentException("Too large rectangle " + rectangle + " for averaging: its area "
                        + ((double) rectangle.sizeX() * (double) rectangle.sizeY()) + " > 2^31-1");
            case 3:
                throw new IllegalArgumentException("Too large rectangle boundaries " + rectangle
                        + " : they are outside 32-bit range -2^31..2^31-1");
        }
        if (source.size() == 0) {
            // nothing to do;
            // now we can be sure that dimX > 0 and dimY > 0
            return;
        }
        final int x1 = (int) rectangle.minX();
        final int y1 = (int) rectangle.minY();
        final int x2 = (int) rectangle.maxX();
        final int y2 = (int) rectangle.maxY();
        Object sourceArray = null;
        int sourceOffset = -1;
        if (source.array() instanceof DirectAccessible) {
            final DirectAccessible da = (DirectAccessible) source.array();
            if (da.hasJavaArray()) {
                sourceArray = da.javaArray();
                sourceOffset = da.javaArrayOffset();
            }
        }
        if (y1 == y2) {
            doAverageByHorizontalLine(result.array(), source.array(), x1, y1, x2);
        } else {
            if (sourceArray == null) {
                doAverageByRectangle(result.array(), source.array(), x1, y1, x2, y2);
            } else {
                doAverageByRectangleForDirectAccessible(result.array(), sourceArray, sourceOffset, x1, y1, x2, y2);
            }
        }
    }

    @Override
    public String toString() {
        return "quick averager ("
                + (strictDivision ? "strict division, " : "")
                + (rounding ? "rounding" : "truncating")
                + (twoStage ? ", two-stage mode)" : ")");
    }

    void writeShiftedLineFromArray(UpdatablePArray result, int x2, long lineOffset, Object lineArray) {
        x2 = rem(x2, dimX);
        result.setData(lineOffset + x2, lineArray, 0, dimX - x2);
        result.setData(lineOffset, lineArray, dimX - x2, x2);
    }

    void averageResultLine(long averagedSize) {
        assert averagedSize > 0;
        if (averagedSize == 1) {
            copyFromSummedLine();
            return;
        }
        if (strictDivision) {
            divideSummedLine(averagedSize);
        } else {
            multiplySummedLine(1.0 / averagedSize);
        }
    }

    void averageAccumulatorForStage2(long averagedSize) {
        assert averagedSize > 0;
        if (strictDivision) {
            divideAccumulator(averagedSize);
        } else {
            multiplyAccumulator(1.0 / averagedSize);
        }
    }

    abstract void clearAccumulator();

    abstract void readLine(PArray source, long lineOffset);

    abstract void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset);

    abstract void sumLineFromAccumulator(int sizeX);

    abstract void sumLineFromAccumulatorForStage2(int sizeX);

    abstract void copyToAccumulator();

    abstract void copyFromSummedLine();

    abstract void addLine();

    abstract void subtractLine();

    abstract void addLineForArray(Object javaArray, int lineOffset);

    abstract void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd);

    abstract void divideSummedLine(long divider);

    abstract void multiplySummedLine(double multiplier);

    abstract void divideAccumulator(long divider);

    abstract void multiplyAccumulator(double multiplier);

    private void doAverageByRectangle(
            UpdatablePArray result,
            PArray source,
            int x1,
            int y1,
            int x2,
            int y2) {
        final int sizeX = x2 - x1 + 1;
        final int sizeY = y2 - y1 + 1;
        assert sizeX > 0;
        assert sizeY > 0;
        clearAccumulator();
        long lineOffsetL = rem(-y2, dimY) * (long) dimX;
        long lineOffsetR = lineOffsetL;
        for (long dy = y2; dy >= y1; dy--) {
            readLine(source, lineOffsetR);
            addLine();
            lineOffsetR = nextLineOffset(lineOffsetR);
        }
        completeLine(result, 0, x2, sizeX, sizeY);
        for (long y = 1, lineOffset = dimX; y < dimY; y++, lineOffset += dimX) {
            readLine(source, lineOffsetL);
            subtractLine();
            lineOffsetL = nextLineOffset(lineOffsetL);
            readLine(source, lineOffsetR);
            addLine();
            lineOffsetR = nextLineOffset(lineOffsetR);
            completeLine(result, lineOffset, x2, sizeX, sizeY);
        }
    }

    private void doAverageByHorizontalLine(
            UpdatablePArray result,
            PArray source,
            int x1,
            int y1,
            int x2) {
        final int sizeX = x2 - x1 + 1;
        assert sizeX > 0;
        long lineOffsetShift = rem(-y1, dimY) * (long) dimX;
        for (long y = 0, lineOffset = 0; y < dimY; y++, lineOffset += dimX) {
            readLine(source, lineOffsetShift);
            copyToAccumulator();
            lineOffsetShift = nextLineOffset(lineOffsetShift);
            sumLineFromAccumulator(sizeX);
            averageResultLine(sizeX);
            writeShiftedLine(result, x2, lineOffset);
        }
    }

    private void doAverageByRectangleForDirectAccessible(
            UpdatablePArray result,
            Object sourceArray,
            int sourceOffset,
            int x1,
            int y1,
            int x2,
            int y2) {
        final int sizeX = x2 - x1 + 1;
        final int sizeY = y2 - y1 + 1;
        assert sizeX > 0;
        assert sizeY > 0;
        clearAccumulator();
        long lineOffsetL = rem(-y2, dimY) * (long) dimX;
        long lineOffsetR = lineOffsetL;
        for (long dy = y2; dy >= y1; dy--) {
            addLineForArray(sourceArray, (int) (sourceOffset + lineOffsetR));
            lineOffsetR = nextLineOffset(lineOffsetR);
        }
        completeLine(result, 0, x2, sizeX, sizeY);
        for (long y = 1, lineOffset = dimX; y < dimY; y++, lineOffset += dimX) {
//            readLine(source, lineOffsetL);
            addAndSubtractLineForArray(
                    sourceArray,
                    (int) (sourceOffset + lineOffsetL),
                    (int) (sourceOffset + lineOffsetR));
            lineOffsetL = nextLineOffset(lineOffsetL);
            lineOffsetR = nextLineOffset(lineOffsetR);
            completeLine(result, lineOffset, x2, sizeX, sizeY);
        }
    }

    private void completeLine(UpdatablePArray result, long lineOffset, int x2, int sizeX, int sizeY) {
        final long finalAveragedSize = twoStage ? sizeX : (long) sizeX * (long) sizeY;
        if (twoStage) {
            averageAccumulatorForStage2(sizeY);
            sumLineFromAccumulatorForStage2(sizeX);
        } else {
            sumLineFromAccumulator(sizeX);
        }
        averageResultLine(finalAveragedSize);
        writeShiftedLine(result, x2, lineOffset);
    }

    private static int unsupportedRectangleCode(IRectangularArea rectangle) {
        Objects.requireNonNull(rectangle, "Null rectangle");
        if (rectangle.coordCount() != 2) {
            return 1;
        }
        if (rectangle.sizeX() > Integer.MAX_VALUE || rectangle.sizeY() > Integer.MAX_VALUE
                || rectangle.sizeX() * rectangle.sizeY() > Integer.MAX_VALUE) {
            return 2;
        }
        if (rectangle.minX() < -Integer.MAX_VALUE || rectangle.maxX() > Integer.MAX_VALUE
                || rectangle.minY() < -Integer.MAX_VALUE || rectangle.maxY() > Integer.MAX_VALUE) {
            // - note: -MAX_VALUE, not MIN_VALUE! We MUST have ability to negate this coordinates (use -x for any x)
            return 3;
        }
        return 0;
    }

    private abstract static class ForInteger32 extends Quick2DAverager {
        final long[] accumulator;
        final long[] accumulatorForStage2;
        final long[] summedLine;

        private ForInteger32(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.accumulator = new long[dimX];
            this.accumulatorForStage2 = twoStage ? new long[dimX] : null;
            this.summedLine = new long[dimX];
        }

        @Override
        void clearAccumulator() {
            java.util.Arrays.fill(accumulator, 0);
        }

        @Override
        void sumLineFromAccumulator(int sizeX) {
            sumLineFromArray(sizeX, accumulator);
        }

        @Override
        void sumLineFromAccumulatorForStage2(int sizeX) {
            sumLineFromArray(sizeX, accumulatorForStage2);
        }

        @Override
        void divideSummedLine(long divider) {
            if (rounding) {
                divideSummedLineWithRounding(divider);
            } else {
                divideSummedLineWithoutRounding(divider);
            }
        }

        @Override
        void multiplySummedLine(double multiplier) {
            if (rounding) {
                multiplySummedLineWithRounding(multiplier);
            } else {
                multiplySummedLineWithoutRounding(multiplier);
            }
        }

        @Override
        void divideAccumulator(long divider) {
            if (rounding) {
                divideAccumulatorWithRounding(divider);
            } else {
                divideAccumulatorWithoutRounding(divider);
            }
        }

        @Override
        void multiplyAccumulator(double multiplier) {
            if (rounding) {
                multiplyAccumulatorWithRounding(multiplier);
            } else {
                multiplyAccumulatorWithoutRounding(multiplier);
            }
        }

        abstract void divideSummedLineWithoutRounding(long divider);

        abstract void multiplySummedLineWithoutRounding(double multiplier);

        abstract void divideSummedLineWithRounding(long divider);

        abstract void multiplySummedLineWithRounding(double multiplier);

        private void divideAccumulatorWithRounding(long divider) {
            final long half = divider >> 1;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulatorForStage2[i] = (accumulator[i] + half) / divider;
            }
        }

        private void multiplyAccumulatorWithRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulatorForStage2[i] = (long) (accumulator[i] * multiplier + 0.5);
            }
        }

        private void divideAccumulatorWithoutRounding(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulatorForStage2[i] = accumulator[i] / divider;
            }
        }

        private void multiplyAccumulatorWithoutRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulatorForStage2[i] = (long) (accumulator[i] * multiplier);
            }
        }

        private void sumLineFromArray(int sizeX, long[] array) {
            assert sizeX > 0;
            if (sizeX == 1) {
                System.arraycopy(array, 0, summedLine, 0, dimX);
                return;
            }
            long sum = sumZeroPosition(sizeX, array);
            summedLine[0] = sum;
            int xL = 0;
            int xR = rem(sizeX, dimX);
            for (int to = dimX; xR < to; xR++) {
                sum += array[xR] - array[xL];
                xL++;
                summedLine[xL] = sum;
            }
            assert xL < dimX;
            // but if xL==xR==dimX-1, nothing to do more
            xR = 0;
            for (int to = dimX - 1; xL < to; xR++) {
                sum += array[xR] - array[xL];
                xL++;
                summedLine[xL] = sum;
            }
        }

        private static long sumZeroPosition(int sizeX, long[] array) {
            long sum = 0;
            for (int i = 0; i < sizeX; ) {
                final int length = Math.min(sizeX - i, array.length);
                for (int x = 0; x < length; x++) {
                    sum += array[x];
                }
                i += length;
            }
            return sum;
        }
    }

    private abstract static class ForLongAndFloatingPoint extends Quick2DAverager {
        final double[] accumulator;
        final double[] accumulatorForStage2;
        final double[] summedLine;

        private ForLongAndFloatingPoint(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.accumulator = new double[dimX];
            this.accumulatorForStage2 = twoStage ? new double[dimX] : null;
            this.summedLine = new double[dimX];
        }

        @Override
        void clearAccumulator() {
            java.util.Arrays.fill(accumulator, 0);
        }

        @Override
        void sumLineFromAccumulator(int sizeX) {
            sumLineFromArray(sizeX, accumulator);
        }

        @Override
        void sumLineFromAccumulatorForStage2(int sizeX) {
            sumLineFromArray(sizeX, accumulatorForStage2);
        }

        @Override
        abstract void divideSummedLine(long divider);

        @Override
        abstract void multiplySummedLine(double multiplier);

        @Override
        void divideAccumulator(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulatorForStage2[i] = accumulator[i] / divider;
            }
        }

        @Override
        void multiplyAccumulator(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulatorForStage2[i] = accumulator[i] * multiplier;
            }
        }

        void sumLineFromArray(int sizeX, double[] array) {
            assert sizeX > 0;
            if (sizeX == 1) {
                System.arraycopy(array, 0, summedLine, 0, dimX);
                return;
            }
            double sum = sumZeroPosition(sizeX, array);
            summedLine[0] = sum;
            int xL = 0;
            int xR = rem(sizeX, dimX);
            for (int to = dimX; xR < to; xR++) {
                sum += array[xR] - array[xL];
                xL++;
                summedLine[xL] = sum;
            }
            assert xL < dimX;
            // but if xL==xR==dimX-1, nothing to do more
            xR = 0;
            for (int to = dimX - 1; xL < to; xR++) {
                sum += array[xR] - array[xL];
                xL++;
                summedLine[xL] = sum;
            }
        }

        private static double sumZeroPosition(int sizeX, double[] array) {
            double sum = 0;
            for (int i = 0; i < sizeX; ) {
                final int length = Math.min(sizeX - i, array.length);
                for (int x = 0; x < length; x++) {
                    sum += array[x];
                }
                i += length;
            }
            return sum;
        }
    }

    /*Repeat() Char                 ==> Bit,,Byte,,Short,,Int,,Long,,Float,,Double;;
               char                 ==> boolean,,byte,,short,,int,,long,,float,,double;;
               (ForInteger32)       ==> $1,,$1,,$1,,$1,,ForLongAndFloatingPoint,,...;;
               (return\s)(true)     ==> $1true,,$1true,,$1true,,$1true,,$1true,,$1false,,...;;
               (=\s*|-\s*)((?:line|array)\[[^\]]+\]) ==> $1($2? 1 : 0),,
                                        $1($2 & 0xFF),,
                                        $1($2 & 0xFFFF),,
                                        $1$2,,...;;
               \(boolean\)\s+\((.*?)\); ==> (int) ($1) != 0;,,...;;
               \(double\)\s+ ==> ,,...;;
               (void\s+divideSummedLineWithRounding)(.*?)(//end-of-method\s*void) ==>
                                        $1$2$3,,$1$2$3,,$1$2$3,,$1$2$3,, void,,...;;
               (void\s+multiplySummedLineWithRounding)(.*?)(//end-of-method\s*void) ==>
                                        $1$2$3,,$1$2$3,,$1$2$3,,$1$2$3,, void,,...;;
               (divideSummedLineWithoutRounding) ==> $1,,$1,,$1,,$1,,divideSummedLine,,...;;
               (multiplySummedLineWithoutRounding) ==> $1,,$1,,$1,,$1,,multiplySummedLine,,...
     */
    private static class ForChar extends ForInteger32 {
        final char[] line;

        private ForChar(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new char[dimX];
        }

        @Override
        public boolean isInteger() {
            return true;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = line[i];
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (char) (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += line[i];
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= line[i];
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final char[] array = (char[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffset + i];
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final char[] array = (char[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffsetToAdd + i] - array[lineOffsetToSubtract + i];
            }
        }

        void divideSummedLineWithRounding(long divider) {
            final long half = divider >> 1;
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (char) ((summedLine[i] + half) / divider);
            }
        }//end-of-method

        void multiplySummedLineWithRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (char) (summedLine[i] * multiplier + 0.5);
            }
        }//end-of-method

        void divideSummedLineWithoutRounding(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (char) (summedLine[i] / divider);
            }
        }

        void multiplySummedLineWithoutRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (char) (summedLine[i] * multiplier);
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static class ForBit extends ForInteger32 {
        final boolean[] line;

        private ForBit(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new boolean[dimX];
        }

        @Override
        public boolean isInteger() {
            return true;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = (line[i]? 1 : 0);
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i]) != 0;
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (line[i]? 1 : 0);
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= (line[i]? 1 : 0);
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final boolean[] array = (boolean[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (array[lineOffset + i]? 1 : 0);
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final boolean[] array = (boolean[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (array[lineOffsetToAdd + i]? 1 : 0) - (array[lineOffsetToSubtract + i]? 1 : 0);
            }
        }

        void divideSummedLineWithRounding(long divider) {
            final long half = divider >> 1;
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) ((summedLine[i] + half) / divider) != 0;
            }
        }//end-of-method

        void multiplySummedLineWithRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i] * multiplier + 0.5) != 0;
            }
        }//end-of-method

        void divideSummedLineWithoutRounding(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i] / divider) != 0;
            }
        }

        void multiplySummedLineWithoutRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i] * multiplier) != 0;
            }
        }
    }


    private static class ForByte extends ForInteger32 {
        final byte[] line;

        private ForByte(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new byte[dimX];
        }

        @Override
        public boolean isInteger() {
            return true;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = (line[i] & 0xFF);
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (byte) (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (line[i] & 0xFF);
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= (line[i] & 0xFF);
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final byte[] array = (byte[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (array[lineOffset + i] & 0xFF);
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final byte[] array = (byte[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (array[lineOffsetToAdd + i] & 0xFF) - (array[lineOffsetToSubtract + i] & 0xFF);
            }
        }

        void divideSummedLineWithRounding(long divider) {
            final long half = divider >> 1;
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (byte) ((summedLine[i] + half) / divider);
            }
        }//end-of-method

        void multiplySummedLineWithRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (byte) (summedLine[i] * multiplier + 0.5);
            }
        }//end-of-method

        void divideSummedLineWithoutRounding(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (byte) (summedLine[i] / divider);
            }
        }

        void multiplySummedLineWithoutRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (byte) (summedLine[i] * multiplier);
            }
        }
    }


    private static class ForShort extends ForInteger32 {
        final short[] line;

        private ForShort(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new short[dimX];
        }

        @Override
        public boolean isInteger() {
            return true;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = (line[i] & 0xFFFF);
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (short) (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (line[i] & 0xFFFF);
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= (line[i] & 0xFFFF);
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final short[] array = (short[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (array[lineOffset + i] & 0xFFFF);
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final short[] array = (short[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += (array[lineOffsetToAdd + i] & 0xFFFF) - (array[lineOffsetToSubtract + i] & 0xFFFF);
            }
        }

        void divideSummedLineWithRounding(long divider) {
            final long half = divider >> 1;
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (short) ((summedLine[i] + half) / divider);
            }
        }//end-of-method

        void multiplySummedLineWithRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (short) (summedLine[i] * multiplier + 0.5);
            }
        }//end-of-method

        void divideSummedLineWithoutRounding(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (short) (summedLine[i] / divider);
            }
        }

        void multiplySummedLineWithoutRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (short) (summedLine[i] * multiplier);
            }
        }
    }


    private static class ForInt extends ForInteger32 {
        final int[] line;

        private ForInt(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new int[dimX];
        }

        @Override
        public boolean isInteger() {
            return true;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = line[i];
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += line[i];
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= line[i];
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final int[] array = (int[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffset + i];
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final int[] array = (int[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffsetToAdd + i] - array[lineOffsetToSubtract + i];
            }
        }

        void divideSummedLineWithRounding(long divider) {
            final long half = divider >> 1;
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) ((summedLine[i] + half) / divider);
            }
        }//end-of-method

        void multiplySummedLineWithRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i] * multiplier + 0.5);
            }
        }//end-of-method

        void divideSummedLineWithoutRounding(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i] / divider);
            }
        }

        void multiplySummedLineWithoutRounding(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (int) (summedLine[i] * multiplier);
            }
        }
    }


    private static class ForLong extends ForLongAndFloatingPoint {
        final long[] line;

        private ForLong(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new long[dimX];
        }

        @Override
        public boolean isInteger() {
            return true;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = line[i];
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (long) (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += line[i];
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= line[i];
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final long[] array = (long[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffset + i];
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final long[] array = (long[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffsetToAdd + i] - array[lineOffsetToSubtract + i];
            }
        }

        void divideSummedLine(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (long) (summedLine[i] / divider);
            }
        }

        void multiplySummedLine(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (long) (summedLine[i] * multiplier);
            }
        }
    }


    private static class ForFloat extends ForLongAndFloatingPoint {
        final float[] line;

        private ForFloat(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new float[dimX];
        }

        @Override
        public boolean isInteger() {
            return false;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = line[i];
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (float) (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += line[i];
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= line[i];
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final float[] array = (float[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffset + i];
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final float[] array = (float[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffsetToAdd + i] - array[lineOffsetToSubtract + i];
            }
        }

        void divideSummedLine(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (float) (summedLine[i] / divider);
            }
        }

        void multiplySummedLine(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (float) (summedLine[i] * multiplier);
            }
        }
    }


    private static class ForDouble extends ForLongAndFloatingPoint {
        final double[] line;

        private ForDouble(Class<?> elementType, long[] dimensions, boolean twoStage) {
            super(elementType, dimensions, twoStage);
            this.line = new double[dimX];
        }

        @Override
        public boolean isInteger() {
            return false;
        }

        @Override
        void readLine(PArray source, long lineOffset) {
            source.getData(lineOffset, line, 0, dimX);
        }

        @Override
        void writeShiftedLine(UpdatablePArray result, int x2, long lineOffset) {
            writeShiftedLineFromArray(result, x2, lineOffset, line);
        }

        @Override
        void copyToAccumulator() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] = line[i];
            }
        }

        @Override
        void copyFromSummedLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (summedLine[i]);
            }
        }

        @Override
        void addLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += line[i];
            }
        }

        @Override
        void subtractLine() {
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] -= line[i];
            }
        }

        @Override
        void addLineForArray(Object javaArray, int lineOffset) {
            final double[] array = (double[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffset + i];
            }
        }

        @Override
        void addAndSubtractLineForArray(Object javaArray, int lineOffsetToSubtract, int lineOffsetToAdd) {
            final double[] array = (double[]) javaArray;
            for (int i = 0, n = dimX; i < n; i++) {
                accumulator[i] += array[lineOffsetToAdd + i] - array[lineOffsetToSubtract + i];
            }
        }

        void divideSummedLine(long divider) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (summedLine[i] / divider);
            }
        }

        void multiplySummedLine(double multiplier) {
            for (int i = 0, n = dimX; i < n; i++) {
                line[i] = (summedLine[i] * multiplier);
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

//    public static strictfp void main(String[] args) {
//        final QuickAverager averager = QuickAverager.newInstance(byte.class, 210, 200);
//        final Matrix<? extends PArray> m = Arrays.SMM.newByteMatrix(200, 200);
//        averager.filter(m, IRectangularArea.valueOf(0, 0, 10, 10));
//    }
}
