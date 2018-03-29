/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.math.functions.Func;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.DependenceApertureBuilder;
import net.algart.matrices.ApertureProcessor;
import net.algart.matrices.TiledApertureProcessorFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>The filter allowing to transform any {@link RankMorphology} object to another instance of that interface,
 * which uses some given {@link TiledApertureProcessorFactory tiler} for processing the source matrices
 * (arguments of {@link RankMorphology} methods).</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * implementing {@link RankMorphology}, and some tiler (an instance of
 * {@link TiledApertureProcessorFactory} class).
 * This object works almost identically to the parent object with the only difference,
 * that it uses the specified tiler for performing all operations.</p>
 *
 * <p>More precisely, each method of this object creates an implementation <tt>p</tt> of {@link ApertureProcessor}
 * interface. The only thing, performed by
 * {@link ApertureProcessor#process(Map, Map) process} method of
 * that object <tt>p</tt>, is calling the same method of <i>parent</i> object with the arguments
 * of <tt>p.{@link ApertureProcessor#process(Map, Map) process(dest,src)}</tt> method
 * (the source matrix or matrices are retrieved from <tt>src</tt>, the result is saved into <tt>dest</tt>).
 * The dependence aperture <tt>p.{@link ApertureProcessor#dependenceAperture(Object) dependenceAperture(...)}</tt>
 * is calculated automatically on the base of the patterns and the performed operation.
 * Then, the method of this object executes the required operation with help of
 * <nobr><tt>{@link #tiler() tiler()}.{@link TiledApertureProcessorFactory#tile(ApertureProcessor)
 * tile}(p).{@link ApertureProcessor#process(Map, Map) process(dest,src)}</tt></nobr> call
 * &mdash; the source matrix or matrices are passed via <tt>src</tt>, the result is retrieved from <tt>dest</tt>.
 * As a result, the same operation is performed tile-by-tile.</p>
 *
 * <p>The methods "<tt>as<i>Operation</i></tt>", returning a view of the passed sources matrices
 * (like {@link #asDilation(Matrix, Pattern)}, {@link #asPercentile(Matrix, double, Pattern)},
 * {@link #asRank(Class, Matrix, Matrix, Pattern)}, etc.) are an exception
 * from this rule. These methods of this class works in the same way, as in
 * {@link ContinuedRankMorphology} class, the continuation mode of which is equal to
 * {@link #tiler() tiler()}.{@link TiledApertureProcessorFactory#continuationMode() continuationMode()}.</p>
 *
 * <p>Note: in improbable cases, when the dimensions of the source matrix and/or
 * the sizes of the pattern are extremely large (about 2<sup>63</sup>),
 * so that the necessary appended matrices should have dimensions or total number of elements,
 * greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>,
 * the methods of this class throw <tt>IndexOutOfBoundsException</tt> and do nothing.
 * See comments to {@link TiledApertureProcessorFactory} class, "Restriction" section for precise details.
 * Of course, these are very improbable cases.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class TiledRankMorphology extends TiledMorphology implements RankMorphology {
    private final RankMorphology parent;
    private final Matrix.ContinuationMode continuationMode;

    TiledRankMorphology(RankMorphology parent, TiledApertureProcessorFactory tiler) {
        super(parent, tiler);
        this.parent = parent;
        this.continuationMode = tiler.continuationMode();
    }

    /**
     * Returns new instance of this class with the passed parent {@link RankMorphology} object
     * and the specified processing tiler.
     *
     * <p>Note: the {@link #context() context} of the created object is retrieved from
     * <tt>parent.{@link Morphology#context() context()}</tt>, and
     * the {@link TiledApertureProcessorFactory#context() context} of the passed tiler
     * is automatically replaced with the same one &mdash; the current {@link #tiler() tiler}
     * of the created object is <nobr><tt>tiler.{@link TiledApertureProcessorFactory#context(ArrayContext)
     * context}(newInstance.{@link #context() context()})</tt></nobr>.
     * It means that the {@link TiledApertureProcessorFactory#context() context} of the passed tiler is not important
     * and can be <tt>null</tt>.</p>
     *
     * @param parent <i>parent</i> object: the instance of {@link Morphology} interface
     *               that will perform all operations.
     * @param tiler  the tiler, which will be used for processing matrices by this class.
     * @return       new instance of this class.
     * @throws NullPointerException if <tt>parent</tt> or <tt>tiler</tt> argument is <tt>null</tt>.
     */
    public static TiledRankMorphology getInstance(RankMorphology parent, TiledApertureProcessorFactory tiler) {
        return new TiledRankMorphology(parent, tiler);
    }

    /**
     * Returns the parent {@link RankMorphology} object,
     * passed to {@link #getInstance(RankMorphology, TiledApertureProcessorFactory)} method.
     *
     * @return the parent {@link RankMorphology} object.
     */
    @Override
    public RankMorphology parent() {
        return this.parent;
    }

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <tt>newContext</tt> for all operations.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * <p>More precisely, this method is equivalent to
     * <tt>{@link #getInstance(RankMorphology, TiledApertureProcessorFactory)
     * getInstance}({@link #parent()}.{@link RankMorphology#context(ArrayContext)
     * context}(newContext), {@link #tiler() tiler()}).</tt>
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another context.
     */
    @Override
    public RankMorphology context(ArrayContext newContext) {
        return new TiledRankMorphology(parent.context(newContext), tiler);
    }

    /*Repeat(INCLUDE_FROM_FILE, ContinuedRankMorphology.java, lazy) !! Auto-generated: NOT EDIT !! */
    // **** LAZY FUNCTIONS ****
    public Matrix<? extends PArray> asPercentile(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes, Pattern pattern)
    {
        Continuer c = new Continuer(null, src, percentileIndexes, pattern, parent, continuationMode);
        return c.reduce(parent.asPercentile(c.get(0), c.get(1), pattern));
    }

    public Matrix<? extends PArray> asPercentile(Matrix<? extends PArray> src,
        double percentileIndex, Pattern pattern)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asPercentile(c.get(0), percentileIndex, pattern));
    }

    public <T extends PArray> Matrix<T> asRank(Class<? extends T> requiredType,
        Matrix<? extends PArray> baseMatrix, Matrix<? extends PArray> rankedMatrix, Pattern pattern)
    {
        Continuer c = new Continuer(null, baseMatrix, rankedMatrix, pattern, parent, continuationMode);
        return c.reduce(parent.asRank(requiredType, c.get(0), c.get(1), pattern));
    }

    public Matrix<? extends PArray> asMeanBetweenPercentiles(Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(null, src, fromPercentileIndexes, toPercentileIndexes, pattern,
            parent, continuationMode);
        return c.reduce(parent.asMeanBetweenPercentiles(c.get(0), c.get(1), c.get(2), pattern, filler));
    }

    public Matrix<? extends PArray> asMeanBetweenPercentiles(Matrix<? extends PArray> src,
        double fromPercentileIndex,
        double toPercentileIndex,
        Pattern pattern, double filler)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asMeanBetweenPercentiles(c.get(0), fromPercentileIndex, toPercentileIndex,
            pattern, filler));
    }

    public Matrix<? extends PArray> asMeanBetweenValues(Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        Pattern pattern, double filler)
    {
        Continuer c = new Continuer(null, src, minValues, maxValues, pattern, parent, continuationMode);
        return c.reduce(parent.asMeanBetweenValues(c.get(0), c.get(1), c.get(2), pattern, filler));
    }

    public Matrix<? extends PArray> asMean(Matrix<? extends PArray> src, Pattern pattern) {
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asMean(c.get(0), pattern));
    }

    public Matrix<? extends PArray> asFunctionOfSum(Matrix<? extends PArray> src,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asFunctionOfSum(c.get(0), pattern, processingFunc));
    }

    public Matrix<? extends PArray> asFunctionOfPercentilePair(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        Pattern pattern, Func processingFunc)
    {
        Continuer c = new Continuer(null, src, percentileIndexes1, percentileIndexes2, pattern,
            parent, continuationMode);
        return c.reduce(parent.asFunctionOfPercentilePair(c.get(0), c.get(1), c.get(2), pattern, processingFunc));
    }

    public Matrix<? extends PArray> asFunctionOfPercentilePair(Matrix<? extends PArray> src,
        double percentileIndex1,
        double percentileIndex2,
        Pattern pattern, Func processingFunc)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        Continuer c = new Continuer(null, src, pattern, parent, continuationMode);
        return c.reduce(parent.asFunctionOfPercentilePair(c.get(0), percentileIndex1, percentileIndex2,
            pattern, processingFunc));
    }
    /*Repeat.IncludeEnd*/

    // **** ACTUALIZING FUNCTIONS ****
    /*Repeat.SectionStart actual*/
    public Matrix<? extends UpdatablePArray> percentile(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes, final Pattern pattern)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().percentile(src.get(0), src.get(1), pattern);
            }
        }, null, src, percentileIndexes);
    }

    public Matrix<? extends UpdatablePArray> percentile(Matrix<? extends PArray> src,
        final double percentileIndex, final Pattern pattern)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().percentile(src.get(0), percentileIndex, pattern);
            }
        }, null, src);
    }

    public <T extends PArray> Matrix<? extends T> rank(final Class<? extends T> requiredType,
        Matrix<? extends PArray> baseMatrix,
        Matrix<? extends PArray> rankedMatrix, final Pattern pattern)
    {
        return tilingProcess(new RankMorphologyProcessor(baseMatrix.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().rank(requiredType, src.get(0), src.get(1), pattern);
            }
        }, null, baseMatrix, rankedMatrix).cast(requiredType);
    }

    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        final Pattern pattern, final double filler)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().meanBetweenPercentiles(src.get(0), src.get(1), src.get(2), pattern, filler);
            }
        }, null, src, fromPercentileIndexes, toPercentileIndexes);
    }

    public Matrix<? extends UpdatablePArray> meanBetweenPercentiles(Matrix<? extends PArray> src,
        final double fromPercentileIndex,
        final double toPercentileIndex,
        final Pattern pattern, final double filler)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().meanBetweenPercentiles(src.get(0), fromPercentileIndex, toPercentileIndex,
                    pattern, filler);
            }
        }, null, src);
    }

    public Matrix<? extends UpdatablePArray> meanBetweenValues(Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        final Pattern pattern, final double filler)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().meanBetweenValues(src.get(0), src.get(1), src.get(2), pattern, filler);
            }
        }, null, src, minValues, maxValues);
    }

    public Matrix<? extends UpdatablePArray> mean(Matrix<? extends PArray> src, final Pattern pattern) {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().mean(src.get(0), pattern);
            }
        }, null, src);
    }

    public Matrix<? extends UpdatablePArray> functionOfSum(Matrix<? extends PArray> src,
        final Pattern pattern, final Func processingFunc)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().functionOfSum(src.get(0), pattern, processingFunc);
            }
        }, null, src);
    }

    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        final Pattern pattern, final Func processingFunc)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().functionOfPercentilePair(src.get(0), src.get(1), src.get(2), pattern, processingFunc);
            }
        }, null, src, percentileIndexes1, percentileIndexes2);
    }

    public Matrix<? extends UpdatablePArray> functionOfPercentilePair(Matrix<? extends PArray> src,
        final double percentileIndex1,
        final double percentileIndex2,
        final Pattern pattern, final Func processingFunc)
    {
        return tilingProcess(new RankMorphologyProcessor(src.dimCount(), pattern) {
            @Override
            public Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src) {
                return parent().functionOfPercentilePair(src.get(0), percentileIndex1, percentileIndex2,
                    pattern, processingFunc);
            }
        }, null, src);
    }
    /*Repeat.SectionEnd actual*/

    // **** IN-PLACE FUNCTIONS ****
    /*Repeat(INCLUDE_FROM_FILE, THIS_FILE, actual)
        (final)?(\s+Class<[^>]*>\s+)?requiredType,\s* ==> ;;
        \.cast\((requiredType|UpdatablePArray\.class)\) ==> ;;
        public\s+(?:<[^>]*>\s*)?Matrix<[^>]*> ==> public void;;
        (Matrix\s*<[^>]*>\s+(?:src|baseMatrix)) ==> Matrix<? extends UpdatablePArray> dest, $1;;
        return\s+(tilingProcess) ==> $1;;
        RankMorphologyProcessor ==> RankMorphologyInPlaceProcessor;;
        process\(List ==> process(Matrix<? extends UpdatablePArray> dest, List;;
        return\s+parent ==> parent;;
        (src\.get\(0\)) ==> dest, $1;;
        null\,\s+(src|baseMatrix) ==> dest, $1 !! Auto-generated: NOT EDIT !! */
    public void percentile(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes, final Pattern pattern)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().percentile(dest, src.get(0), src.get(1), pattern);
            }
        }, dest, src, percentileIndexes);
    }

    public void percentile(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        final double percentileIndex, final Pattern pattern)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().percentile(dest, src.get(0), percentileIndex, pattern);
            }
        }, dest, src);
    }

    public void rank(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> baseMatrix,
        Matrix<? extends PArray> rankedMatrix, final Pattern pattern)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(baseMatrix.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().rank(dest, src.get(0), src.get(1), pattern);
            }
        }, dest, baseMatrix, rankedMatrix);
    }

    public void meanBetweenPercentiles(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> fromPercentileIndexes,
        Matrix<? extends PArray> toPercentileIndexes,
        final Pattern pattern, final double filler)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().meanBetweenPercentiles(dest, src.get(0), src.get(1), src.get(2), pattern, filler);
            }
        }, dest, src, fromPercentileIndexes, toPercentileIndexes);
    }

    public void meanBetweenPercentiles(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        final double fromPercentileIndex,
        final double toPercentileIndex,
        final Pattern pattern, final double filler)
    { // important to call the same method of the parent, which will create a constant matrix with increased sizes
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().meanBetweenPercentiles(dest, src.get(0), fromPercentileIndex, toPercentileIndex,
                    pattern, filler);
            }
        }, dest, src);
    }

    public void meanBetweenValues(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> minValues,
        Matrix<? extends PArray> maxValues,
        final Pattern pattern, final double filler)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().meanBetweenValues(dest, src.get(0), src.get(1), src.get(2), pattern, filler);
            }
        }, dest, src, minValues, maxValues);
    }

    public void mean(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, final Pattern pattern) {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().mean(dest, src.get(0), pattern);
            }
        }, dest, src);
    }

    public void functionOfSum(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        final Pattern pattern, final Func processingFunc)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().functionOfSum(dest, src.get(0), pattern, processingFunc);
            }
        }, dest, src);
    }

    public void functionOfPercentilePair(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        Matrix<? extends PArray> percentileIndexes1,
        Matrix<? extends PArray> percentileIndexes2,
        final Pattern pattern, final Func processingFunc)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().functionOfPercentilePair(dest, src.get(0), src.get(1), src.get(2), pattern, processingFunc);
            }
        }, dest, src, percentileIndexes1, percentileIndexes2);
    }

    public void functionOfPercentilePair(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        final double percentileIndex1,
        final double percentileIndex2,
        final Pattern pattern, final Func processingFunc)
    {
        tilingProcess(new RankMorphologyInPlaceProcessor(src.dimCount(), pattern) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src) {
                parent().functionOfPercentilePair(dest, src.get(0), percentileIndex1, percentileIndex2,
                    pattern, processingFunc);
            }
        }, dest, src);
    }
    /*Repeat.IncludeEnd*/

    private Matrix<? extends UpdatablePArray> tilingProcess(ApertureProcessor<Integer> processor,
        Matrix<? extends UpdatablePArray> dest, // maybe null
        Matrix<? extends PArray> src)
    {
        return tilingProcess(processor, dest, Matrices.several(PArray.class, src));
    }

    private Matrix<? extends UpdatablePArray> tilingProcess(ApertureProcessor<Integer> processor,
        Matrix<? extends UpdatablePArray> dest, // maybe null
        Matrix<? extends PArray> src,
        Matrix<? extends PArray> additional)
    {
        return tilingProcess(processor, dest, Matrices.several(PArray.class, src, additional));
    }

    private Matrix<? extends UpdatablePArray> tilingProcess(ApertureProcessor<Integer> processor,
        Matrix<? extends UpdatablePArray> dest, // maybe null
        Matrix<? extends PArray> src,
        Matrix<? extends PArray> additional1,
        Matrix<? extends PArray> additional2)
    {
        return tilingProcess(processor, dest, Matrices.several(PArray.class, src, additional1, additional2));
    }

    private Matrix<? extends UpdatablePArray> tilingProcess(ApertureProcessor<Integer> processor,
        Matrix<? extends UpdatablePArray> dest, // maybe null
        List<Matrix<? extends PArray>> src)
    {
        Map<Integer, Matrix<?>> destMatrices = new LinkedHashMap<Integer, Matrix<?>>();
        Map<Integer, Matrix<?>> srcMatrices = new LinkedHashMap<Integer, Matrix<?>>();
        destMatrices.put(0, dest);
        for (int i = 0, n = src.size(); i < n; i++) {
            srcMatrices.put(i, src.get(i));
        }
        tiler.tile(processor).process(destMatrices, srcMatrices);
        return destMatrices.get(0).cast(UpdatablePArray.class);
    }

    private abstract class RankMorphologyProcessor
        extends AbstractArrayProcessorWithContextSwitching
        implements ApertureProcessor<Integer>, ArrayProcessorWithContextSwitching
    {
        private final IRectangularArea dependenceAperture;
        private final IRectangularArea emptyAperture;

        private RankMorphologyProcessor(int dimCount, Pattern pattern) {
            super(null); // will be replaced by the tiler
            this.dependenceAperture = DependenceApertureBuilder.SUM_MAX_0.getAperture(dimCount, pattern, false);
            this.emptyAperture = IRectangularArea.valueOf(IPoint.origin(dimCount), IPoint.origin(dimCount));
        }

        public RankMorphology parent() { // parent with the correct sub-task context
            return parent.context(this.context());
        }

        public void process(Map<Integer, Matrix<?>> dest, Map<Integer, Matrix<?>> src) {
            assert src.size() <= 3;
            assert dest.size() == 1;
            List<Matrix<? extends PArray>> srcMatrices = new ArrayList<Matrix<? extends PArray>>();
            for (int i = 0; i < 3; i++) {
                Matrix<?> m = src.get(i);
                if (m != null) {
                    srcMatrices.add(src.get(i).cast(PArray.class));
                }
            }
            assert srcMatrices.size() == src.size() : "not all arguments specified";
            dest.put(0, process(srcMatrices));
        }

        public abstract Matrix<? extends PArray> process(List<Matrix<? extends PArray>> src);

        public IRectangularArea dependenceAperture(Integer srcMatrixKey) {
            return srcMatrixKey == 0 ? dependenceAperture : emptyAperture;
        }
    }

    private abstract class RankMorphologyInPlaceProcessor
        extends AbstractArrayProcessorWithContextSwitching
        implements ApertureProcessor<Integer>, ArrayProcessorWithContextSwitching
    {
        private final IRectangularArea dependenceAperture;
        private final IRectangularArea emptyAperture;

        private RankMorphologyInPlaceProcessor(int dimCount, Pattern pattern) {
            super(null); // will be replaced by the tiler
            this.dependenceAperture = DependenceApertureBuilder.SUM_MAX_0.getAperture(dimCount, pattern, false);
            this.emptyAperture = IRectangularArea.valueOf(IPoint.origin(dimCount), IPoint.origin(dimCount));
        }

        public RankMorphology parent() { // parent with the correct sub-task context
            return parent.context(this.context());
        }

        public void process(Map<Integer, Matrix<?>> dest, Map<Integer, Matrix<?>> src) {
            assert src.size() <= 3;
            assert dest.size() == 1;
            List<Matrix<? extends PArray>> srcMatrices = new ArrayList<Matrix<? extends PArray>>();
            for (int i = 0; i < 3; i++) {
                Matrix<?> m = src.get(i);
                if (m != null) {
                    srcMatrices.add(src.get(i).cast(PArray.class));
                }
            }
            assert srcMatrices.size() == src.size() : "not all arguments specified";
            process(dest.get(0).cast(UpdatablePArray.class), srcMatrices);
        }

        public abstract void process(Matrix<? extends UpdatablePArray> dest, List<Matrix<? extends PArray>> src);

        public IRectangularArea dependenceAperture(Integer srcMatrixKey) {
            return srcMatrixKey == 0 ? dependenceAperture : emptyAperture;
        }
    }
}
