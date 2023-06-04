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

package net.algart.arrays.demo;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.functions.LinearFunc;

import java.math.BigInteger;
import java.util.Random;

/**
 * <p>Testing extra-large index calculation in the {@link Matrix matrices} (<tt>&gt;Long.MAX_VALUE</tt>).</p>
 *
 * @author Daniel Alievsky
 */
public class HugeMatrixIndexesTest {
    static final Matrix<DoubleArray> matrix2A = Matrices.asCoordFuncMatrix(
        LinearFunc.getAveragingInstance(2), DoubleArray.class, Long.MAX_VALUE / 2, 2);
    static final Matrix<DoubleArray> matrix2B = Matrices.asCoordFuncMatrix(
        LinearFunc.getAveragingInstance(2), DoubleArray.class, 2, Long.MAX_VALUE / 2);
    static final Matrix<DoubleArray> matrix3A = Matrices.asCoordFuncMatrix(
        LinearFunc.getAveragingInstance(3), DoubleArray.class, Long.MAX_VALUE / 4, 2, 2);
    static final Matrix<DoubleArray> matrix3B = Matrices.asCoordFuncMatrix(
        LinearFunc.getAveragingInstance(3), DoubleArray.class, 2, 2, Long.MAX_VALUE / 4);
    static final long[] SUBMATRIX_DIMENSIONS_2A = {3, Long.MAX_VALUE / 3};
    static final long[] SUBMATRIX_DIMENSIONS_2B = {Long.MAX_VALUE / 3, 3};
    static final long[] SUBMATRIX_DIMENSIONS_3A = {2, 3, Long.MAX_VALUE / 6};
    static final long[] SUBMATRIX_DIMENSIONS_3B = {Long.MAX_VALUE / 6, 2, 3};

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: " + HugeMatrixIndexesTest.class.getName()
                + " numberOfTests [>nul]");
            return;
        }
        int numberOfTests = Integer.parseInt(args[0]);
        Random rnd = new Random();
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            System.err.print(testCount + "\r");
            Matrix<DoubleArray> matrix;
            if (testCount < numberOfTests / 2) {
                Matrix<DoubleArray> matrix2 = (testCount >> 1) % 2 == 1 ? matrix2A : matrix2B;
                Matrix<DoubleArray> matrix3 = (testCount >> 1) % 2 == 1 ? matrix3A : matrix3B;
                matrix = testCount % 2 == 1 ? matrix3 : matrix2;
            } else {
                int dimCount = rnd.nextInt(5) + 1;
                matrix = Matrices.asCoordFuncMatrix(
                    LinearFunc.getAveragingInstance(dimCount), DoubleArray.class,
                    randomDimensions(rnd, dimCount, Long.MAX_VALUE));
            }
            long[] submatrixDim;
            if (testCount < numberOfTests / 4) {
                long[] submatrixDim2 = (testCount >> 1) % 2 == 1 ? SUBMATRIX_DIMENSIONS_2A : SUBMATRIX_DIMENSIONS_2B;
                long[] submatrixDim3 = (testCount >> 1) % 2 == 1 ? SUBMATRIX_DIMENSIONS_3A : SUBMATRIX_DIMENSIONS_3B;
                submatrixDim = testCount % 2 == 1 ? submatrixDim3 : submatrixDim2;
            } else {
                submatrixDim = randomDimensions(rnd, matrix.dimCount(), Long.MAX_VALUE);
            }
            long[] submatrixPos = new long[submatrixDim.length];
            for (int k = 0; k < submatrixDim.length; k++) {
                submatrixPos[k] = testCount < numberOfTests / 10 ?
                    rnd.nextInt(5) :
                    Math.min(rnd.nextLong(), Long.MAX_VALUE - submatrixDim[k]);
                if (matrix.dim(k) == 0) {
                    submatrixPos[k] = submatrixDim[k] = 0; // only such cyclic/pseudo-cyclic submatrices are allowed
                }
            }
            Matrix<DoubleArray> cyclic = matrix.subMatr(submatrixPos, submatrixDim,
                Matrix.ContinuationMode.CYCLIC);

            IPoint from = IPoint.valueOf(cyclic.subMatrixFrom());
            IPoint to = IPoint.valueOf(cyclic.subMatrixTo());
            Matrix<DoubleArray> pseudoCyclic = matrix.subMatrix(from.coordinates(), to.coordinates(),
                Matrix.ContinuationMode.PSEUDO_CYCLIC);
            System.out.println(testCount + ": testing " + from + ".." + to
                + " inside " + JArrays.toString(matrix.dimensions(), "x", 100));
            for (int shiftIndex = 0; shiftIndex < 1000; shiftIndex++) {
                boolean showResults = shiftIndex < 10;
                long[] shift = new long[cyclic.dimCount()];
                for (int k = 0; k < shift.length; k++) {
                    shift[k] = cyclic.size() == 0 ? rnd.nextInt(10) :
                        showResults && k == shift.length - 1 ?
                            (cyclic.dim(k) + shiftIndex - 1) % cyclic.dim(k) :
                            Math.round(rnd.nextDouble() * (cyclic.dim(k) - 1));
                }
                IPoint p = IPoint.valueOf(shift);
                IPoint shifted = from.add(p);
                long cyclicIndex = matrix.cyclicIndex(shifted.coordinates());
                long pseudoCyclicIndex = matrix.pseudoCyclicIndex(shifted.coordinates());
                long oneDimensional = shifted.toOneDimensional(matrix.dimensions(), true);
                if (pseudoCyclicIndex != oneDimensional)
                    throw new AssertionError("Bug A detected: pseudo-cyclic index calculated differently "
                        + "in Matrix and IPoint: " + pseudoCyclicIndex + " and " + oneDimensional);
                long rPseudoCyclicIndex = hugePseudoCyclicIndex(matrix.dimensions(), shifted.coordinates());
                if (pseudoCyclicIndex != rPseudoCyclicIndex)
                    throw new AssertionError("Bug B detected: pseudo-cyclic index calculated incorrectly: "
                        + pseudoCyclicIndex + " instead of " + rPseudoCyclicIndex);
                if (cyclic.size() == 0) {
                    continue; // cannot test internal indexes
                }
                long index = cyclic.index(p.coordinates());
                double cv = cyclic.array().getDouble(index);
                double pv = pseudoCyclic.array().getDouble(index);
                double rcv = matrix.array().getDouble(cyclicIndex);
                double rpv = matrix.array().getDouble(pseudoCyclicIndex);
                if (showResults || cv != rcv || pv != rpv) {
                    System.out.println("    " + p
                        + ": cyclic/pseudo-cyclic index = " + cyclicIndex + " / " + pseudoCyclicIndex
                        + ", cyclic/pseudo-cyclic value = " + cv + "/" + pv);
                }
                if (cv != rcv)
                    throw new AssertionError("Bug C detected: incorrect cyclic value (" + rcv + " expected)");
                if (pv != rpv)
                    throw new AssertionError("Bug D detected: incorrect pseudo-cyclic value (" + rpv + " expected)");
            }
        }
        System.err.println("All O'k             ");
    }

    private static long[] randomDimensions(Random rnd, int dimCount, long totalSize) {
        assert totalSize >= 0;
        long[] result = new long[dimCount];
        long product = 1;
        boolean inverseOrder = rnd.nextBoolean();
        for (int k = 0; k < dimCount; k++) {
            long limit = product == 0 ? totalSize  : totalSize / product;
            // - at least one zero dimension should lead to trivial results
            limit = Math.min(limit, rnd.nextBoolean() ? 5 :
                rnd.nextBoolean() ? (long) Math.cbrt(totalSize) : totalSize);
            int dimIndex = inverseOrder ? dimCount - 1 - k : k;
            result[dimIndex] = Math.round(rnd.nextDouble() * limit);
            product *= result[dimIndex];
            assert product <= totalSize : "product=" + product + ", totalSize=" + totalSize;
        }
        return result;
    }

    private static long hugePseudoCyclicIndex(long dim[], long[] coordinates) {
        assert coordinates.length > 0;
        assert coordinates.length == dim.length;
        long size = Arrays.longMul(dim);
        if (size == 0) {
            return 0;
        }
        assert size > 0;
        BigInteger result = BigInteger.valueOf(coordinates[0]);
        for (int k = 1; k < dim.length; k++) {
            BigInteger dimMul = BigInteger.valueOf(Arrays.longMul(dim, 0, k));
            BigInteger mul = BigInteger.valueOf(coordinates[k]).multiply(dimMul);
            result = result.add(mul);
        }
//        if (!result.equals(BigInteger.valueOf(result.longValue()))) {
//            System.out.println("Really huge index: " + result);
//        }
        long rem = result.remainder(BigInteger.valueOf(size)).longValue();
        return rem < 0 ? rem + size : rem;
    }
}
