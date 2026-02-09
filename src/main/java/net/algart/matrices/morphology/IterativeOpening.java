/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Iterative opening of the matrix.</p>
 *
 * <p>This class is an implementation of {@link IterativeArrayProcessor} interface,
 * performing {@link Morphology#opening(Matrix, Pattern, Morphology.SubtractionMode) opening}
 * of some source matrix (<code>{@link Matrix}({@link UpdatablePArray})</code>)
 * by sequential {@link Patterns#newMinkowskiMultiplePattern(Pattern, int) Minkowski multiples}
 * of some pattern: <i>k</i>&otimes;<i>P</i>, <i>k</i>=1,2,....
 * <p>(In this class, the {@link Morphology#opening(Matrix, Pattern, Morphology.SubtractionMode)
 * Morphology.opening} method is supposed
 * to be performed with the last argument {@link Morphology.SubtractionMode#NONE}.)
 *
 * <p>If the object was created by {@link #getInstance getInstance} method,
 * then every opening is added to some accumulator matrix, named <code>result</code> and returned by
 * {@link #result()} method.
 * If the object was created by {@link #getGranulometryInstance getGranulometryInstance} method,
 * then the {@link Arrays#sumOf arithmethic sum} of all elements of every opening
 * will be stored in some {@link PNumberArray}, returned by {@link #sumsOfOpenings()} method.
 * These sums contain information about typical sizes of objects, "drawn" in the matrix.</p>
 *
 * <p>More precisely, in this implementation:</p>
 *
 * <ul>
 * <li>the {@link #context() current context} in the returned instance is equal
 * to the current context of the {@link Morphology} object, passed to {@link #getInstance getInstance} methods.</li>
 *
 * <li>{@link #performIteration(ArrayContext)} method calculates<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<code>{@link Morphology#opening(Matrix, Pattern, Morphology.SubtractionMode)
 * opening}(source_matrix,
 * {@link Patterns#newMinkowskiSum(Pattern[])
 * Patterns.newMinkowskiSum}(ptn<sub>0</sub>,ptn<sub>1</sub>,...,ptn<sub>k</sub>))</code>
 * (<code>k+1</code> summands),<br>
 * where <code>k</code> is the index of the current iteration (0, 1, 2, ...)
 * and <code>ptn<sub>i</sub>=patterns[i%patterns.length]</code>, <code>patterns</code> is the argument of
 * {@link #getInstance getInstance} method.
 * If only one pattern <i>P</i> was passed to {@link #getInstance getInstance} method,
 * it means the opening by {@link Patterns#newMinkowskiMultiplePattern(Pattern, int) Minkowski multiple pattern}
 * <code>(k+1)</code>&otimes;<i>P</i>.
 * If the object was created by {@link #getInstance getInstance} method,
 * this opening is arithmetically added, with truncation of overflows, to the <code>result</code> matrix,
 * by {@link Matrices#applyFunc(ArrayContext, Func, Matrix, Matrix, Matrix)} with
 * {@link Func#X_PLUS_Y} function.
 * If the object was created by {@link #getGranulometryInstance getGranulometryInstance} method,
 * the {@link Arrays#sumOf arithmethic sum} of all elements of this opening is
 * {@link MutableDoubleArray#pushDouble added to the end} of the AlgART array,
 * returned by {@link #sumsOfOpenings()} method.
 * (Really, this method does not actually perform these operation, but performs equivalent
 * calculations with some optimization.)
 * </li>
 *
 * <li>Besides opening, this class always calculates the
 * {@link Morphology#erosion(Matrix, Pattern) erosion} by the same pattern.
 * The {@link #done()} method returns <code>true</code> if the last erosion was the same as the previous erosion.
 * In this case, all further possible iteration will always generate the same opening.
 * For bit matrices, it usually means that the opening is zero-filled (all "objects" were removed).</li>
 *
 * <li>{@link #result()} method always returns the reference to the accumulator matrix <code>result</code>,
 * if the object was created by {@link #getInstance getInstance} method,
 * or {@code null},
 * if the object was created by {@link #getGranulometryInstance getGranulometryInstance} method.</li>
 * </ul>
 *
 * <p>Note: this method is based on {@link Morphology#dilation(Matrix, Pattern) dilation} method,
 * called after erosion. It can be <b>important</b> for some continuation models of
 * {@link ContinuedMorphology} class, if you use it as an argument of {@link #getInstance getInstance} methods.
 * For example, continuation by positive infinity constant work correctly for original
 * {@link Morphology#opening(Matrix, Pattern, Morphology.SubtractionMode) opening} method,
 * but here it will lead to side effects as a result of dilation.
 * We recommend not to use continuation by positive infinity or other high constants (greater
 * than minimum of the source matrix).</p>
 *
 * <p>The <code>result</code> matrix is created and zero-filled by {@link #getInstance getInstance} method,
 * and before the first iteration the source matrix is added to it (that means just copying).
 * The AlgART array, returned by {@link #sumsOfOpenings()} method, is created
 * as empty {@link MutablePNumberArray} by {@link #getGranulometryInstance getGranulometryInstance} method,
 * and before the first iteration the sum of elements of the source matrix is stored there as the first element.</p>
 *
 * <p>Also note: there is no guarantees that <code>result</code> matrix, returned by {@link #result()} method,
 * and the array returned by {@link #sumsOfOpenings()} method,
 * are updated after each iteration. This class contains some optimization, that can lead to updating results
 * only after some iteration, for example, after iteration #8, #16, etc.
 * There is only the guarantee that {@link #result()} and {@link #sumsOfOpenings()} return valid results
 * when {@link #done()} returns <code>true</code>.</p>
 *
 * <p>This class may be applied to a matrix with any number of dimensions.</p>
 *
 * @author Daniel Alievsky
 */
public class IterativeOpening extends AbstractIterativeArrayProcessor<Matrix<? extends UpdatablePArray>>
    implements IterativeArrayProcessor<Matrix<? extends UpdatablePArray>>
{
    private static final int NUMBER_OF_STORED_OPENINGS = 8;

    private final Morphology morphology;
    private final Pattern[] patterns;
    private final Matrix<? extends UpdatablePArray> result, temp1, temp2, temp3;
    private final MutableDoubleArray sumsOfOpenings;
    private final List<Matrix<? extends UpdatablePArray>> store;
    private final boolean onlyGranulometry;
    private final int numberOfStoredOpenings;
    private int numberOfPerformedErosions = 0;
    private int patternIndex = 0;
    private int storeSize = 0;
    private boolean firstIteration = true;
    private boolean useCarcasses = false;
    private boolean done = false;

    private IterativeOpening(
        Morphology morphology,
        Class<? extends UpdatablePArray> requiredType,
        Matrix<? extends PArray> matrix, Pattern[] patterns, boolean onlyGranulometry)
    {
        super(morphology.context());
        Objects.requireNonNull(matrix, "Null matrix argument");
        if (patterns.length == 0) {
            throw new IllegalArgumentException("Empty patterns[] argument");
        }
        for (int k = 0; k < patterns.length; k++) {
            Objects.requireNonNull(patterns[k], "Null patterns[" + k + "]");
        }
        this.onlyGranulometry = onlyGranulometry;
        this.numberOfStoredOpenings = onlyGranulometry ? 0 : NUMBER_OF_STORED_OPENINGS;
        final MemoryModel mmTemp = IterativeErosion.mm(memoryModel, matrix, 2 + numberOfStoredOpenings);
        this.morphology = morphology;
        this.patterns = patterns.clone();
        if (onlyGranulometry) {
            long minDim = matrix.dim(0);
            for (int coord = 1, dimCount = matrix.dimCount(); coord < dimCount; coord++) {
                minDim = Math.min(minDim, matrix.dim(coord));
            }
            MemoryModel mmGran = minDim <= Arrays.SystemSettings.maxTempJavaMemory() / 8 ?
                SimpleMemoryModel.getInstance() : memoryModel; // very simple estimation
            this.result = null;
            this.sumsOfOpenings = mmGran.newEmptyDoubleArray();
        } else {
            Class<?> reType = Arrays.elementType(requiredType);
            MemoryModel mmRes = IterativeErosion.mm(memoryModel, matrix,
                Arrays.sizeOf(reType) / Arrays.sizeOf(matrix.elementType()));
            this.result = mmRes.newMatrix(requiredType, reType, matrix.dimensions());
            this.sumsOfOpenings = null;
        }
        this.temp1 = mmTemp.newLazyCopy(UpdatablePArray.class, matrix);
        this.temp2 = mmTemp.newMatrix(UpdatablePArray.class, matrix);
        this.temp3 = mmTemp.newMatrix(UpdatablePArray.class, matrix);
        if (!onlyGranulometry) {
            store = new ArrayList<>();
            store.add(result);
            for (int k = 0; k < numberOfStoredOpenings; k++) {
                store.add(mmTemp.newMatrix(UpdatablePArray.class, matrix));
            }
            store.add(temp3);
        } else {
            store = null;
        }
    }

    /**
     * Creates new instance of this class, that adds every opening to the accumulator matrix,
     * returned by {@link #result()} method.
     *
     * @param morphology        the {@link Morphology} object that will be used for performing openings.
     * @param requiredType      the type of built-in AlgART array for {@link #result()} matrix.
     *                          Should be enough for storing elementwise sums of hundreds of opened matrices;
     *                          in another case, overflows will lead to truncation of the sums.
     * @param matrix            the source matrix, that will be opened by Minkowski sums of the passed patterns
     *                          (or just by Minkowski multiples <i>k</i>&otimes;<i>P</i>, if <code>patterns</code>
     *                          argument contains only 1 pattern).
     * @param patterns          one or several patterns for performing erosion. For little pattern sizes, you may
     *                          specify several patterns with near form to increase the precision
     *                          of the resulting matrix.
     * @return                  new instance of this class.
     * @throws NullPointerException     if one of arguments or one of passed patterns is {@code null}.
     * @throws IllegalArgumentException if <code>patterns.length==0</code> (no patterns passed).
     */
    public static IterativeOpening getInstance(
        Morphology morphology,
        Class<? extends UpdatablePArray> requiredType,
        Matrix<? extends PArray> matrix, Pattern... patterns)
    {
        Objects.requireNonNull(requiredType, "Null requiredType argument");
        return new IterativeOpening(morphology, requiredType, matrix, patterns, false);
    }

    /**
     * Creates new instance of this class, that calculates the sum of all elements of every opening
     * and stores them in some built-in AlgART array, returned by {@link #sumsOfOpenings()} method.
     *
     * @param morphology        the {@link Morphology} object that will be used for performing openings.
     * @param matrix            the source matrix, that will be opened by Minkowski sums of the passed patterns
     *                          (or just by Minkowski multiples <i>k</i>&otimes;<i>P</i>, if <code>patterns</code>
     *                          argument contains only 1 pattern).
     * @param patterns          one or several patterns for performing erosion. For little pattern sizes, you may
     *                          specify several patterns with near form to increase the precision
     *                          of the resulting matrix.
     * @return                  new instance of this class.
     * @throws NullPointerException     if one of arguments or one of passed patterns is {@code null}.
     * @throws IllegalArgumentException if <code>patterns.length==0</code> (no patterns passed).
     */
    public static IterativeOpening getGranulometryInstance(
        Morphology morphology,
        Matrix<? extends PArray> matrix, Pattern... patterns)
    {
        return new IterativeOpening(morphology, null, matrix, patterns, true);
    }

    /**
     * Returns an {@link Array#asImmutable() immutable view} of internal AlgART array, where the sums
     * of all elements of the source matrix and every its openings are stored.
     * The difference between elements <code>#k</code> and <code>#k+1</code> estimates the summary area
     * (volume, in 3D or <i>n</i>-dimensional cases) of all objects, which "sizes" are not "less"
     * than the Minkowski sum of <code>k</code> patterns, but "less" than
     * the Minkowski sum of <code>k+1</code> patterns.
     *
     * @return an immutable view of the built-in AlgART array storing the sums of all elements of all openings.
     */
    public PNumberArray sumsOfOpenings() {
        return sumsOfOpenings == null ? null : sumsOfOpenings.asImmutable();
    }

    @Override
    public void performIteration(ArrayContext context) {
        Pattern ptn = patterns[patternIndex];
        Pattern ptnOrCarcass = useCarcasses ? ptn.carcass() : ptn;
        // this may be optimized a little more if there are equal patterns, but I think it's not important
        if (firstIteration) {
            if (onlyGranulometry) {
                sumsOfOpenings.pushDouble(Arrays.sumOf(part(context, 0.0, 0.05), temp1.array()));
            } else {
                Matrices.applyFunc(part(context, 0.0, 0.05), Func.IDENTITY, result, temp1);
            }
        }
        morphology.context(part(context, firstIteration ? 0.05 : 0.0, 0.2)).erosion(temp2, temp1, ptnOrCarcass);
        done = numberOfPerformedErosions == Integer.MAX_VALUE
            || !Matrices.compareAndCopy(part(context, 0.2, 0.25), temp1, temp2).changed();
        if (!done) {
            numberOfPerformedErosions++;
            morphology.context(part(context, 0.25, 0.9)).dilation(temp3, temp2,
                Patterns.newMinkowskiMultiplePattern(ptn, numberOfPerformedErosions));
            if (onlyGranulometry) {
                sumsOfOpenings.pushDouble(Arrays.sumOf(part(context, 0.9, 1.0), temp3.array()));
            }
        }
        firstIteration = false;
        patternIndex++;
        if (patternIndex == patterns.length) {
            useCarcasses = true;
            patternIndex = 0;
        }
        if (!onlyGranulometry) {
            if (done || storeSize == numberOfStoredOpenings) {
                int n = done ? 1 + storeSize : 2 + numberOfStoredOpenings;
                // if done, then the current temp3 is extra: it is equal to the previous one
                if (n > 1) { // may be 1 if done and NUMBER_OF_STORED_EROSIONS == 0
                    final Func f = LinearFunc.getInstance(0.0, Arrays.nDoubleCopies(n, 1.0).toJavaArray());
                    Matrices.applyFunc(part(context, 0.9, 1.0), f, result, store.subList(0, n));
                }
                storeSize = 0;
                // temp3 is already accumulated in the result: no reasons to save it in the store
            } else if (numberOfStoredOpenings > 0) {
                Matrices.copy(part(context, 0.9, 1.0), store.get(1 + storeSize), temp3);
                storeSize++;
                // accumulating several erosions increases speed of further averaging
            }
        }
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public long estimatedNumberOfIterations() {
        return IterativeErosion.estimatedNumberOfIterations(temp1, patterns[0]);
    }

    @Override
    public Matrix<? extends UpdatablePArray> result() {
        return result;
    }

    @Override
    public void freeResources(ArrayContext context) {
        if (result != null) {
            temp1.freeResources(context == null ? null : context.part(0.0, 0.2));
            temp2.freeResources(context == null ? null : context.part(0.2, 0.4));
            temp3.freeResources(context == null ? null : context.part(0.4, 0.6));
            result.freeResources(context == null ? null : context.part(0.6, 0.8));
            sumsOfOpenings.freeResources(context == null ? null : context.part(0.8, 1.0));
        } else {
            temp1.freeResources(context == null ? null : context.part(0.0, 0.25));
            temp2.freeResources(context == null ? null : context.part(0.25, 0.5));
            temp3.freeResources(context == null ? null : context.part(0.5, 0.75));
            sumsOfOpenings.freeResources(context == null ? null : context.part(0.75, 1.0));
        }
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "iterative opening by patterns: " + JArrays.toString(patterns, ", ", 1000);
    }

}
