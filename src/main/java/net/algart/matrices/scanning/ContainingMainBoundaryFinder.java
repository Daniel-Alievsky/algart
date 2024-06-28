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

package net.algart.matrices.scanning;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.matrices.Abstract2DProcessor;

import java.util.Arrays;
import java.util.Objects;

public class ContainingMainBoundaryFinder extends Abstract2DProcessor {
    private static final byte LEFT = 1;
    private static final byte RIGHT = 2;

    private final IRectangularArea matrixRectangle;
    private final byte[] brackets;
    private final long matrixSize;

    private Matrix<? extends BitArray> matrix = null;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private boolean clearBoundary = false;

    private Boundary2DScanner scanner = null;
    private long[] offsets = JArrays.EMPTY_LONGS;
    private int offsetsLength = 0;
    private boolean ready = false;

    private ContainingMainBoundaryFinder(long[] dimensions) {
        super(boolean.class, dimensions);
        this.matrixRectangle = IRectangularArea.valueOf(0, 0, dimX() - 1, dimY() - 1);
        this.brackets = new byte[this.dimX() + 1];
        this.matrixSize = matrixSize();
    }

    public static ContainingMainBoundaryFinder newInstance(long[] dimensions) {
        return new ContainingMainBoundaryFinder(dimensions);
    }

    public static ContainingMainBoundaryFinder newInstance(long dimX, long dimY) {
        return newInstance(new long[]{dimX, dimY});
    }

    public static ContainingMainBoundaryFinder newInstance(Matrix<? extends BitArray> matrix) {
        Objects.requireNonNull(matrix, "Null matrix");
        final ContainingMainBoundaryFinder result = new ContainingMainBoundaryFinder(matrix.dimensions());
        result.matrix = matrix;
        return result;
    }

    public Matrix<? extends BitArray> getMatrix() {
        if (matrix == null) {
            throw new IllegalStateException("Scanned matrix is not specified yet");
        }
        return matrix;
    }

    public ContainingMainBoundaryFinder setMatrix(Matrix<? extends BitArray> matrix) {
        checkCompatibility(matrix);
        this.matrix = matrix;
        return this;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public ContainingMainBoundaryFinder setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = Objects.requireNonNull(connectivityType, "Null connectivityType");
        return this;
    }

    public boolean isClearBoundary() {
        return clearBoundary;
    }

    // Note: this mode usually not necessary, it is supported only to simplify compatibility with old algorithms
    public ContainingMainBoundaryFinder setClearBoundary(boolean clearBoundary) {
        this.clearBoundary = clearBoundary;
        return this;
    }

    public IPoint fillAtRectangle(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            IRectangularArea rectangle) {
        return drawRectangleAndFind(result, source, rectangle, true, false);
    }

    // Mostly useful variant of usage
    public IPoint clearOutsideRectangle(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            IRectangularArea rectangle) {
        return drawRectangleAndFind(result, source, rectangle, false, true);
    }

    public IPoint buildAroundRectangle(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            IRectangularArea rectangle) {
        return drawRectangleAndFind(result, source, rectangle, true, true);
    }


    /**
     * Finds main boundary in the source binary matrix, passed to {@link #newInstance(Matrix)}
     * or {@link #setMatrix(Matrix)} method,
     * containing the specified position (x,y), and stores it inside this object.
     *
     * <p>Returns the x-coordinate x' of the pixel of this main boundary, so that x'&lt;=x
     * and (x',y) belongs to this boundary. You can trace this boundary again starting from
     * position (x',y), X_MINUS pixel side. If the given position is outside any main boundary,
     * returns -1.
     *
     * @param x x-coordinate. Note that it must be actually <code>int</code> value in current implementation
     *          (this class does not support matrices with a width &ge;Integer.MAX_VALUE).
     * @param y y-coordinate.
     * @return <code>-1</code> if this point is outside all main boundaries;
     * x-coordinate of the left pixel of some main boundary, where this point is inside
     * (its y-coordinate is the same as the argument).
     * @throws IndexOutOfBoundsException if these coordinates are out of the source matrix.
     */
    public long find(long x, long y) {
        getMatrix();
        final long result = findUnsortedVerticalSegmentsAndReturnLeftX(x, y);
//        long t1 = System.nanoTime();
        Arrays.sort(offsets, 0, offsetsLength);
//        long t2 = System.nanoTime();
//        System.out.printf(Locale.US, "Sorting: %.3f ms%n", (t2 - t1) * 1e-6);
        ready = true;
        return result;
    }

    public void fillBitsInside(Matrix<? extends UpdatableBitArray> result) {
        fillInside(result, 1.0);
    }

    public void clearBitsOutside(Matrix<? extends UpdatableBitArray> result) {
        fillOutside(result, 0.0);
    }

    /**
     * Fills all pixels inside the main boundary, found by the last call of {@link #find(long x, long y)} method,
     * with the specified filler.
     * Does nothing if the point (x,y), passed to that method, lies outside any main boundaries.
     *
     * @param result matrix to fill.
     * @param filler value to fill.
     * @throws IllegalStateException if {@link #find(long, long)} was not called yet.
     */
    public void fillInside(Matrix<? extends UpdatablePArray> result, double filler) {
        Objects.requireNonNull(result, "Null result matrix");
        checkReady();
        if (!result.dimEquals(matrix)) {
            throw new IllegalArgumentException("Matrix to fill " + result
                    + " has another dimensions than the scanned matrix " + matrix);
        }
        assert (offsetsLength & 1) == 0;
        final UpdatablePArray array = result.array();
        final long matrixSize = array.length();
        for (int k = 0; k < offsetsLength; k += 2) {
            final long from = offsets[k];
            final long to = offsets[k + 1];
            assert from >= 0 && to > from && to <= matrixSize : "Invalid from, to = " + from + ", " + to
                    + " (full length = " + matrixSize + ")";
            array.fill(from, to - from, filler);
        }
    }

    /**
     * Fills all pixels outside the main boundary, found by the last call of {@link #find(long x, long y)} method,
     * with the specified filler.
     * Fills all matrix if the point (x,y), passed to that method, lies outside any main boundaries.
     *
     * @param result matrix to fill.
     * @param filler value to fill.
     * @throws IllegalStateException if {@link #find(long, long)} was not called yet.
     */
    public void fillOutside(Matrix<? extends UpdatablePArray> result, double filler) {
        Objects.requireNonNull(result, "Null result matrix");
        checkReady();
        if (!result.dimEquals(matrix)) {
            throw new IllegalArgumentException("Matrix to fill " + result
                    + " has another dimensions than the scanned matrix " + matrix);
        }
        assert (offsetsLength & 1) == 0;
        final UpdatablePArray array = result.array();
        final long matrixSize = array.length();
        long to = 0;
        for (int k = 0; k < offsetsLength; k += 2) {
            final long from = offsets[k];
            array.fill(to, from - to, filler);
            to = offsets[k + 1];
            assert from >= 0 && to > from && to <= matrixSize : "Invalid from, to = " + from + ", " + to
                    + " (full length = " + matrixSize + ")";
        }
        array.fill(to, matrixSize - to, filler);
    }

    /**
     * Finds minimal rectangle, containing the main boundary, found by the last call of {@link #find(long x, long y)}
     * method.
     * Returns {@code null} if the point (x,y), passed to that method, lies outside any main boundaries.
     *
     * @return Minimal rectangle, containing the main boundary, or {@code null} if there is no containing boundary.
     * @throws IllegalStateException if {@link #find(long, long)} was not called yet.
     */
    public IRectangularArea findContainingRectangle() {
        checkReady();
        assert (offsetsLength & 1) == 0;
        if (offsetsLength == 0) {
            return null;
        }
        long minX = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE;
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        final int dimX = dimX();
        for (int k = 0; k < offsetsLength; k += 2) {
            final long from = offsets[k];
            final long to = offsets[k + 1];
            assert from >= 0 && to > from && to <= matrixSize : "Invalid from, to = " + from + ", " + to
                    + " (full length = " + matrixSize + ")";
            final long y = from / dimX;
            final long lineOffset = y * dimX;
            final long x1 = from - lineOffset;
            final long x2 = (to - 1) - lineOffset;
            assert x2 < lineOffset + dimX : "Invalid from, to = " + from + ", " + to
                    + ": different rows " + y + "!=" + (to - 1) / dimX;
            assert x1 <= x2 : "Invalid from, to = " + from + ", " + to
                    + ": left = " + x1 + " > right = " + x2;
            minX = Math.min(minX, x1);
            maxX = Math.max(maxX, x2);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return IRectangularArea.valueOf(minX, minY, maxX, maxY);
    }

    public void clear() {
        this.offsetsLength = 0;
    }

    public Boundary2DScanner scanner() {
        checkReady();
        assert scanner != null;
        return scanner;
    }

    public void freeResources() {
        clear();
        this.offsets = JArrays.EMPTY_LONGS;
        // - no sense to remove scanner: it contains external data (matrix)
    }

    private IPoint drawRectangleAndFind(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            IRectangularArea rectangle,
            boolean fill,
            boolean clear) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(source, "Null source matrix");
        Objects.requireNonNull(rectangle, "Null rectangle");
        if (rectangle.coordCount() != 2) {
            throw new IllegalArgumentException("Rectangular area is not 2-dimensional: " + rectangle);
        }
        if (!result.dimEquals(source)) {
            throw new IllegalArgumentException("Matrix to fill " + result
                    + " has another dimensions than the scanned source " + source);
        }
        setMatrix(result);
        clear();
        final IRectangularArea r = rectangle.intersection(matrixRectangle);
        if (r == null) {
            if (clear) {
                result.array().fill(0);
            } else {
                result.array().copy(source.array());
                if (clearBoundary) {
                    Matrices.clearBoundary(result, 1);
                }
            }
            return null;
        }
        result.array().copy(source.array());
        final Matrix<? extends UpdatableBitArray> subResult = result.subMatrix(r);
        subResult.array().fill(1);
        if (clearBoundary) {
            Matrices.clearBoundary(result, 1);
        }
        final long found = find(r.minX(), r.minY());
        if (found < 0) {
            throw new AssertionError("Didn't find unit pixel at " + r.min()
                    + ", which set to 1 before; result is " + found);
        }
        if (fill) {
            if (clear) {
                result.array().fill(0);
            }
            fillBitsInside(result);
        } else {
            clearBitsOutside(result);
            subResult.array().copy(source.subMatrix(r).array());
        }
        return IPoint.valueOf(found, r.minY());
    }

    private long findUnsortedVerticalSegmentsAndReturnLeftX(long x, long y) {
        if (x < 0 || x >= dimX() || y < 0 || y >= dimY()) {
            throw new IndexOutOfBoundsException("Position (" + x + ", " + y + ") is out of the matrix " + matrix);
        }
        Arrays.fill(brackets, (byte) 0);
        clear();
        final BitArray bitArray = matrix.array();
        final long from = y * dimX();
        final long to = from + x + 1;
        // - including the point (x,y) itself!
        this.scanner = Boundary2DScanner.getSingleBoundaryScanner(matrix, connectivityType);
        // - note: this class works with brackets itself, so getSingleBoundaryScanner
        // (the simplest kind of scanner) is quite enough
        final Boundary2DScanner wrapper = new VerticalSegmentsFinder(scanner, y);
        long p = bitArray.indexOf(from, to, true);
        while (p != -1) {
            final int leftX = (int) (p - from);
            clear();
            if (leftX > x) {
                return -1;
            }
            wrapper.goTo(leftX, y, Boundary2DScanner.Side.X_MINUS);
            wrapper.scanBoundary();
            // - stores contour in this.offsets
            if ((offsetsLength & 1) != 0) {
                throw new AssertionError("Unbalanced brackets at (" + leftX + ", " + y + ")");
            }
            assert brackets[leftX] == 1;
            final int rightX = findRightBracket(leftX);
            if (rightX == -1) {
                throw new AssertionError("Right bracket not found after the left bracket at ("
                        + leftX + ", " + y + ")");
            }
            if (rightX > x) {
                // - last contour stays in this.offsets
                return leftX;
            }
            p = bitArray.indexOf(p + (rightX - leftX), to, true);
        }
        clear();
        return -1;
    }

    private int findRightBracket(int left) {
        for (int i = left + 1, n = dimX() + 1; i < n; i++) {
            if (brackets[i] != 0) {
                assert brackets[i] == RIGHT :
                        "invalid bracket " + brackets[i] + " at " + i + " after left bracket at " + left
                                + "; but the right bracket expected!";
                return i;
            }
        }
        return -1;
    }

    private void addOffset(long offset) {
        if (offsetsLength == offsets.length) {
            ensureCapacity((long) offsetsLength + 1);
        }
        offsets[offsetsLength++] = offset;
    }

    private void ensureCapacity(long newLength) {
        if (newLength > offsets.length) {
            if (newLength > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too long boundary: >=2^31 vertical segments");
            }
            offsets = java.util.Arrays.copyOf(offsets, Math.max(16,
                    Math.max((int) newLength,
                            (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * offsets.length)))));
        }
    }

    private void checkReady() {
        if (!ready) {
            throw new IllegalStateException("Data are not ready: finding has not been performed yet");
        }
    }

    private class VerticalSegmentsFinder extends Boundary2DWrapper {
        final long y;

        VerticalSegmentsFinder(Boundary2DScanner parent, long y) {
            super(parent);
            this.y = y;
        }

        @Override
        public void next() {
            parent.next();
            switch (parent.side()) {
                case X_MINUS: {
                    leftBracket();
                    break;
                }
                case X_PLUS: {
                    rightBracket();
                    break;
                }
            }
        }

        private void leftBracket() {
            if (parent.y() == y) {
                brackets[(int) parent.x()] = LEFT;
            }
            addOffset(parent.currentIndexInArray());
        }

        private void rightBracket() {
            if (parent.y() == y) {
                brackets[(int) parent.x() + 1] = RIGHT;
            }
            addOffset(parent.currentIndexInArray() + 1);
        }
    }
}
