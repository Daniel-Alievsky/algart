package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.functions.*;
import net.algart.math.patterns.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>Iterative erosion of the matrix.</p>
 *
 * <p>This class is an implementation of {@link IterativeArrayProcessor} interface,
 * performing {@link Morphology#erosion(Matrix, Pattern) erosion}
 * of some source matrix (<tt>{@link Matrix}({@link UpdatablePArray})</tt>)
 * by sequential {@link Patterns#newMinkowskiMultiplePattern(Pattern, int) Minkowski multiples}
 * of some pattern: <i>k</i>&otimes;<i>P</i>, <i>k</i>=1,2,....
 * Every erosion is added to some accumulator matrix, named <tt>result</tt> and returned by
 * {@link #result()} method.
 * If the source matrix is binary, then each element of the <tt>result</tt> matrix will be an integer,
 * which is non-zero only for non-zero elements of the source one and which shows the distance from this point
 * to the nearest zero element, "measured" in the "sizes" of the pattern.</p>
 *
 * <p>More precisely, in this implementation:</p>
 *
 * <ul>
 * <li>the {@link #context() current context} in the returned instance is equal
 * to the current context of the {@link Morphology} object, passed to {@link #getInstance getInstance} method.</li>
 *
 * <li>{@link #performIteration(ArrayContext)} method calculates<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<tt>{@link Morphology#erosion(Matrix, Pattern) erosion}(source_matrix,
 * {@link Patterns#newMinkowskiSum(Pattern[])
 * Patterns.newMinkowskiSum}(ptn<sub>0</sub>,ptn<sub>1</sub>,...,ptn<sub>k</sub>))</tt>
 * (<tt>k+1</tt> summands),<br>
 * where <tt>k</tt> is the index of the current iteration (0, 1, 2, ...)
 * and <tt>ptn<sub>i</sub>=patterns[i%patterns.length]</tt>, <tt>patterns</tt> is the argument of
 * {@link #getInstance getInstance} method.
 * If only one pattern <i>P</i> was passed to {@link #getInstance getInstance} method,
 * it means the erosion by {@link Patterns#newMinkowskiMultiplePattern(Pattern, int) Minkowski multiple pattern}
 * <tt>(k+1)</tt>&otimes;<i>P</i>.
 * This erosion is arithmetically added, with truncation of overflows, to the <tt>result</tt> matrix,
 * by {@link Matrices#applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)} with
 * {@link Func#X_PLUS_Y} function.
 * (Really, this method does not actually perform these operation, but performs equivalent
 * calculations with some optimization.)
 * </li>
 *
 * <li>{@link #done()} method returns <tt>true</tt> if the last erosion was the same as the previous erosion:
 * for bit matrices, it usually means that both erosions are zero-filled (all "objects" were removed).</li>
 *
 * <li>{@link #result()} method always returns the reference to the accumulator matrix <tt>result</tt>.</li>
 * </ul>
 *
 * <p>The <tt>result</tt> matrix is created and zero-filled by {@link #getInstance getInstance} method,
 * and before the first iteration the source matrix is added to it (that means just copying).</p>
 *
 * <p>Please note: there is no guarantees that <tt>result</tt> matrix, returned by {@link #result()} method,
 * is updated after each iteration. This class contains some optimization, that can lead to updating <tt>result</tt>
 * only after some iteration, for example, after iteration #8, #16, etc.
 * There is only the guarantee that {@link #result()} returns valid matrix
 * when {@link #done()} returns <tt>true</tt>.</p>
 *
 * <p>This class may be applied to a matrix with any number of dimensions.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class IterativeErosion extends AbstractIterativeArrayProcessor<Matrix<? extends UpdatablePArray>>
    implements IterativeArrayProcessor<Matrix<? extends UpdatablePArray>>
{
    private static final int TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION = 5;
    private static final int MAX_TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION = 5000;
    // here 5 * 5000 < DefaultThreadPoolFactory.MIN_MULTITHREADING_LENGTH;
    // it's important: multithreading is an antioptimization here
    private static final int NUMBER_OF_TESTS_FOR_COMPLEXITY_ESTIMATION = 5;
    private static final int MAX_NUMBER_OF_ITERATIONS_FOR_COMPLEXITY_ESTIMATION = 5000;
    private static final int MAX_TESTING_TIME_PER_COORD_FOR_COMPLEXITY_ESTIMATION = 250; // ms

    private static final int NUMBER_OF_STORED_EROSIONS = 8;

    private final Morphology morphology;
    private final Pattern[] patterns;
    private final Matrix<? extends UpdatablePArray> result, temp1, temp2;
    private final List<Matrix<? extends UpdatablePArray>> store = new ArrayList<Matrix<? extends UpdatablePArray>>();
    private int patternIndex = 0;
    private int storeSize = 0;
    private boolean firstIteration = true;
    private boolean useCarcasses = false;
    private boolean done = false;

    private IterativeErosion(
        Morphology morphology,
        Class<? extends UpdatablePArray> requiredType,
        Matrix<? extends PArray> matrix, Pattern... patterns)
    {
        super(morphology.context());
        if (requiredType == null)
            throw new NullPointerException("Null requiredType argument");
        if (matrix == null)
            throw new NullPointerException("Null matrix argument");
        if (patterns.length == 0)
            throw new IllegalArgumentException("Empty patterns[] argument");
        for (int k = 0; k < patterns.length; k++) {
            if (patterns[k] == null)
                throw new NullPointerException("Null patterns[" + k + "]");
        }
        final Class<?> reType = Arrays.elementType(requiredType);
        final MemoryModel mmRes = mm(memoryModel, matrix, Arrays.sizeOf(reType) / Arrays.sizeOf(matrix.elementType()));
        final MemoryModel mmTemp = mm(memoryModel, matrix, 2 + NUMBER_OF_STORED_EROSIONS);
        this.morphology = morphology;
        this.patterns = patterns.clone();
        this.result = mmRes.newMatrix(requiredType, reType, matrix.dimensions());
        this.temp1 = mmTemp.newLazyCopy(UpdatablePArray.class, matrix);
        this.temp2 = mmTemp.newMatrix(UpdatablePArray.class, matrix);
        store.add(result);
        for (int k = 0; k < NUMBER_OF_STORED_EROSIONS; k++) {
            store.add(mmTemp.newMatrix(UpdatablePArray.class, matrix));
        }
        store.add(temp1);
    }

    /**
     * Creates new instance of this class.
     *
     * @param morphology        the {@link Morphology} object that will be used for performing erosions.
     * @param requiredType      the type of built-in AlgART array for {@link #result()} matrix.
     *                          Should be enough for storing elementwise sums of hundreds of eroded matrices;
     *                          in other case, overflows will lead to trunctation of the sums.
     * @param matrix            the source matrix, that will be eroded by Minkowski sums of the passed patterns
     *                          (or just by Minkowski muptiples <i>k</i>&otimes;<i>P</i>, if <tt>patterns</tt>
     *                          argument contains only 1 pattern).
     * @param patterns          one or several patterns for performing erosion. For little pattern sizes, you may
     *                          specify several patterns with near form to increase the precision
     *                          of the resulting matrix.
     * @return                  new instance of this class.
     * @throws NullPointerException     if one of arguments or one of passed patterns is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>patterns.length==0</tt> (no patterns passed).
     */
    public static IterativeErosion getInstance(
        Morphology morphology,
        Class<? extends UpdatablePArray> requiredType,
        Matrix<? extends PArray> matrix, Pattern... patterns)
    {
        return new IterativeErosion(morphology, requiredType, matrix, patterns);
    }

    @Override
    public void performIteration(ArrayContext context) {
        Pattern ptn = patterns[patternIndex];
        Pattern ptnOrCarc = useCarcasses ? ptn.carcass() : ptn;
        // this may be optimized a little more if there are equal patterns, but I think it's not important
        if (firstIteration) {
            Matrices.applyFunc(part(context, 0.0, 0.05), Func.IDENTITY, result, temp1);
        }
        morphology.context(part(context, firstIteration ? 0.05 : 0.0, 0.75)).erosion(temp2, temp1, ptnOrCarc);
        firstIteration = false;
        patternIndex++;
        if (patternIndex == patterns.length) {
            useCarcasses = true;
            patternIndex = 0;
        }
        done = !Matrices.compareAndCopy(part(context, 0.75, 0.8), temp1, temp2).changed();
        if (done || storeSize == NUMBER_OF_STORED_EROSIONS) {
            int n = done ? 1 + storeSize : 2 + NUMBER_OF_STORED_EROSIONS;
            // if done, then the current temp1 is extra: it is equal to the previous one
            if (n > 1) { // may be 1 if done and NUMBER_OF_STORED_EROSIONS == 0
                Func f = LinearFunc.getInstance(0.0, Arrays.toJavaArray(Arrays.nDoubleCopies(n, 1.0)));
                Matrices.applyFunc(part(context, 0.8, 1.0), f, result, store.subList(0, n));
            }
            storeSize = 0;
            // temp1 is already accumulated in the result: no reasons to save it in the store
        } else if (NUMBER_OF_STORED_EROSIONS > 0) {
            Matrices.copy(part(context, 0.8, 1.0), store.get(1 + storeSize), temp1);
            storeSize++;
            // accumulating several erosions increases speed of further averaging
        }
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public long estimatedNumberOfIterations() {
        return estimatedNumberOfIterations(temp1, patterns[0]);
    }

    @Override
    public Matrix<? extends UpdatablePArray> result() {
        return result;
    }

    @Override
    public void freeResources(ArrayContext context) {
        temp1.freeResources(context == null ? null : context.part(0.0, 1.0 / 3.0));
        temp2.freeResources(context == null ? null : context.part(1.0 / 3.0, 2.0 / 3.0));
        result.freeResources(context == null ? null : context.part(2.0 / 3.0, 1.0));
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "iterative erosion by patterns: " + JArrays.toString(patterns, ", ", 1000);
    }

    public static long estimatedNumberOfIterations(Matrix<? extends PArray> matrix, Pattern pattern) {
        int dimCount = matrix.dimCount();
        long minDim = matrix.dim(0);
        for (int coord = 1; coord < dimCount; coord++) {
            minDim = Math.min(minDim, matrix.dim(coord));
        }
        if (dimCount > 3 || // too complex case: avoid very long calculations here
            minDim < 2 * TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION)
        {
            return minDim;
        }
        pattern = Patterns.newRectangularIntegerPattern(pattern.roundedCoordArea().ranges());
        int maxIterCount = 0;
        long[] position = new long[dimCount];
        long[] dimensions = new long[dimCount];
        JArrays.fillLongArray(dimensions, TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION);
        final Random rnd = new Random(157);
        final MemoryModel smm = SimpleMemoryModel.getInstance();
        final Morphology morphology = BasicMorphology.getInstance(null);
        for (int coord = 0; coord < dimCount; coord++) {
//            long t1 = System.nanoTime();
            dimensions[coord] = Math.min(matrix.dim(coord), MAX_TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION);
            Matrix<UpdatablePArray> w1 = smm.newMatrix(UpdatablePArray.class, matrix.elementType(), dimensions);
            Matrix<UpdatablePArray> w2 = smm.newMatrix(UpdatablePArray.class, matrix.elementType(), dimensions);
//            long t2 = System.nanoTime();
            for (int testIndex = 0; testIndex < NUMBER_OF_TESTS_FOR_COMPLEXITY_ESTIMATION; testIndex++) {
                for (int k = 0; k < dimCount; k++) {
                    int dim = (int)Math.min(Integer.MAX_VALUE, matrix.dim(k)); // int type is required for rnd.nextInt
                    assert dim >= TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION;
                    position[k] = k == coord ? 0 : rnd.nextInt(dim - TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION);
                }
                Matrix<? extends PArray> m = matrix.subMatr(position, dimensions);
                Matrices.copy(null, w1, m);
                long patternWidth = Math.min(Integer.MAX_VALUE, pattern.roundedCoordRange(coord).size());
                // Integer.MAX_VALUE guarantees impossibility of overflow
                int count = 1;
                long tStart = System.currentTimeMillis();
                for (; count < MAX_NUMBER_OF_ITERATIONS_FOR_COMPLEXITY_ESTIMATION; count++) {
                    if (patternWidth * count >=
                        dimensions[coord]) { // this check is usefult if full submatrix is constant
                        break;
                    }
                    morphology.erosion(w2, w1, pattern, true);
                    if (!Matrices.compareAndCopy(null, w1, w2).changed()) {
                        break;
                    }
                    if (System.currentTimeMillis() - tStart >
                        MAX_TESTING_TIME_PER_COORD_FOR_COMPLEXITY_ESTIMATION /
                            NUMBER_OF_TESTS_FOR_COMPLEXITY_ESTIMATION)
                        break;
                }
                maxIterCount = Math.max(maxIterCount, count);
                if (count == MAX_NUMBER_OF_ITERATIONS_FOR_COMPLEXITY_ESTIMATION) {
                    return maxIterCount;
                }
            }
            dimensions[coord] = TEST_MATRIX_DIM_FOR_COMPLEXITY_ESTIMATION;
//            long t3 = System.nanoTime();
//            System.out.println("Time: " + (t2 - t1)*1e-6 + "+" + (t3-t2)*1e-6 + ": " + maxIterCount);
        }
        return maxIterCount;
    }

    static MemoryModel mm(MemoryModel memoryModel, Matrix<? extends PArray> matrix, double numberOfMatrices) {
        if (Matrices.sizeOf(matrix) > Arrays.SystemSettings.maxTempJavaMemory() / numberOfMatrices) {
            return memoryModel;
        } else {
            return SimpleMemoryModel.getInstance();
        }
    }

    static boolean isSmall(Pattern pattern) {
        if (pattern instanceof QuickPointCountPattern) {
            return pattern.pointCount() <= 5;
        } else {
            // don't call pointCount() for such pattern: it can be slow
            return false;
        }
    }
}
