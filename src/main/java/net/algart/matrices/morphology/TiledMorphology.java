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

package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.ApertureProcessor;
import net.algart.matrices.DependenceApertureBuilder;
import net.algart.matrices.TiledApertureProcessorFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static net.algart.matrices.DependenceApertureBuilder.*;

/**
 * <p>The filter allowing to transform any {@link Morphology} object to another instance of that interface,
 * which uses some given {@link TiledApertureProcessorFactory tiler} for processing the source matrix
 * (an argument of {@link Morphology} methods).</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * implementing {@link Morphology}, and some tiler (an instance of
 * {@link TiledApertureProcessorFactory} class).
 * This object works almost identically to the parent object with the only difference,
 * that it uses the specified tiler for performing all operations.</p>
 *
 * <p>More precisely, each method of this object creates an implementation <code>p</code> of {@link ApertureProcessor}
 * interface. The only thing, performed by
 * {@link ApertureProcessor#process(Map, Map) process} method of
 * that object <code>p</code>, is calling the same method of <i>parent</i> object with the arguments
 * of <code>p.{@link ApertureProcessor#process(Map, Map) process(dest,src)}</code> method
 * (the source matrix is retrieved from <code>src</code>, the result is saved into <code>dest</code>).
 * The dependence aperture <code>p.{@link ApertureProcessor#dependenceAperture(Object) dependenceAperture(...)}</code>
 * is calculated automatically on the base of the patterns and the performed operation.
 * Then, the method of this object executes the required operation with help of
 * <code>{@link #tiler() tiler()}.{@link TiledApertureProcessorFactory#tile(ApertureProcessor)
 * tile}(p).{@link ApertureProcessor#process(Map, Map) process(dest,src)}</code> call
 * &mdash; the source matrix is passed via <code>src</code>, the result is retrieved from <code>dest</code>.
 * As a result, the same operation is performed tile-by-tile.</p>
 *
 * <p>The methods {@link #asDilation(Matrix, Pattern)} and {@link #asErosion(Matrix, Pattern)} are an exception
 * from this rule. These methods of this class works in the same way, as in
 * {@link ContinuedMorphology} class, the continuation mode of which is equal to
 * {@link #tiler() tiler()}.{@link TiledApertureProcessorFactory#continuationMode() continuationMode()}.</p>
 *
 * <p>Note: in improbable cases, when the dimensions of the source matrix and/or
 * the sizes of the pattern are extremely large (about 2<sup>63</sup>),
 * so that the necessary appended matrices should have dimensions or total number of elements,
 * greater than <code>Long.MAX_VALUE</code>,
 * the methods of this class throw <code>IndexOutOfBoundsException</code> and do nothing.
 * See comments to {@link TiledApertureProcessorFactory} class, "Restriction" section for precise details.
 * Of course, these are very improbable cases.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class TiledMorphology implements Morphology {
    private final Morphology parent;
    final ArrayContext context;
    final TiledApertureProcessorFactory tiler;
    final int dimCount;

    TiledMorphology(Morphology parent, TiledApertureProcessorFactory tiler) {
        Objects.requireNonNull(parent, "Null parent morphology");
        Objects.requireNonNull(tiler, "Null tiler");
        this.parent = parent;
        this.context = parent.context() == null ? ArrayContext.DEFAULT : parent.context();
        this.tiler = tiler.context(this.context);
        this.dimCount = tiler.dimCount();
    }

    /**
     * Returns new instance of this class with the passed parent {@link Morphology} object
     * and the specified processing tiler.
     *
     * <p>Note: the {@link #context() context} of the created object is retrieved from
     * <code>parent.{@link Morphology#context() context()}</code>, and
     * the {@link TiledApertureProcessorFactory#context() context} of the passed tiler
     * is automatically replaced with the same one &mdash; the current {@link #tiler() tiler}
     * of the created object is <code>tiler.{@link TiledApertureProcessorFactory#context(ArrayContext)
     * context}(newInstance.{@link #context() context()})</code>.
     * It means that the {@link TiledApertureProcessorFactory#context() context} of the passed tiler is not important
     * and can be {@code null}.</p>
     *
     * @param parent <i>parent</i> object: the instance of {@link Morphology} interface
     *               that will perform all operations.
     * @param tiler  the tiler, which will be used for processing matrices by this class.
     * @return new instance of this class.
     * @throws NullPointerException if <code>parent</code> or <code>tiler</code> argument is {@code null}.
     */
    public static TiledMorphology getInstance(Morphology parent, TiledApertureProcessorFactory tiler) {
        return new TiledMorphology(parent, tiler);
    }

    /**
     * Returns the parent {@link Morphology} object, passed to
     * {@link #getInstance(Morphology, TiledApertureProcessorFactory)} method.
     *
     * @return the parent {@link Morphology} object.
     */
    public Morphology parent() {
        return this.parent;
    }

    /**
     * Returns the processing tiler that will be used for tiled processing the source matrices.
     *
     * @return the tiler, which is used for tiled processing the source matrices.
     * @see #getInstance(Morphology, TiledApertureProcessorFactory)
     * @see TiledRankMorphology#getInstance(RankMorphology, TiledApertureProcessorFactory)
     */
    public TiledApertureProcessorFactory tiler() {
        return this.tiler;
    }

    public ArrayContext context() {
        return this.context;
    }

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <code>newContext</code> for all operations.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * <p>More precisely, this method is equivalent to
     * <code>{@link #getInstance(Morphology, TiledApertureProcessorFactory)
     * getInstance}({@link #parent()}.{@link Morphology#context(ArrayContext)
     * context}(newContext), {@link #tiler() tiler()}).</code>
     *
     * @param newContext another context, used by the returned instance; can be {@code null}.
     * @return new instance with another context.
     */
    public Morphology context(ArrayContext newContext) {
        return new TiledMorphology(parent.context(newContext), tiler);
    }

    /**
     * Returns <code>true</code>, if this class works in the default
     * {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC
     * pseudo-cyclic continuation mode}.
     * More precisely, returns <code>true</code> if and only if
     * <code>{@link #tiler()}.{@link TiledApertureProcessorFactory#continuationMode()
     * continuationMode()}</code> is {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC PSEUDO_CYCLIC}.
     *
     * @return whether this class works in the pseudo-cyclic continuation mode.
     */
    public boolean isPseudoCyclic() {
        return tiler.continuationMode() == Matrix.ContinuationMode.PSEUDO_CYCLIC;
    }

    public Matrix<? extends PArray> asDilation(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = (parent.isPseudoCyclic() ? SUM : SUM_MAX_0).getAperture(src.dimCount(), pattern, false);
        src = DependenceApertureBuilder.extend(src, a, tiler.continuationMode());
        return DependenceApertureBuilder.reduce(parent.asDilation(src, pattern), a);
    }

    public Matrix<? extends PArray> asErosion(Matrix<? extends PArray> src, Pattern pattern) {
        IRectangularArea a = (parent.isPseudoCyclic() ? SUM : SUM_MAX_0).getAperture(src.dimCount(), pattern, true);
        src = DependenceApertureBuilder.extend(src, a, tiler.continuationMode());
        return DependenceApertureBuilder.reduce(parent.asErosion(src, pattern), a);
    }

    /*Repeat() dilation ==> erosion,,weakDilation,,weakErosion,,beucherGradient;;
               (pattern,\s+)false ==> $1true,,$1false, $1true,,$1true, $1false,,$1false, $1true;;
               SUM      ==> SUM,,SUM_MAX_0,,SUM_MAX_0,,MAX */

    public Matrix<? extends UpdatablePArray> dilation(Matrix<? extends PArray> src, final Pattern pattern) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, pattern, false)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().dilation(src, pattern);
            }
        }, src);
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public Matrix<? extends UpdatablePArray> erosion(Matrix<? extends PArray> src, final Pattern pattern) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, pattern, true)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().erosion(src, pattern);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> weakDilation(Matrix<? extends PArray> src, final Pattern pattern) {
        return tilingProcess(new MorphologyProcessor(
                SUM_MAX_0.getAperture(dimCount, pattern, false, pattern, true)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().weakDilation(src, pattern);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> weakErosion(Matrix<? extends PArray> src, final Pattern pattern) {
        return tilingProcess(new MorphologyProcessor(
                SUM_MAX_0.getAperture(dimCount, pattern, true, pattern, false)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().weakErosion(src, pattern);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> beucherGradient(Matrix<? extends PArray> src, final Pattern pattern) {
        return tilingProcess(new MorphologyProcessor(
                MAX.getAperture(dimCount, pattern, false, pattern, true)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().beucherGradient(src, pattern);
            }
        }, src);
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() dilation ==> erosion,,closing,,opening;;
               (pattern,\s+)false ==> $1true,,$1false, $1true,,$1true, $1false */

    public Matrix<? extends UpdatablePArray> dilation(
            Matrix<? extends PArray> src, final Pattern pattern,
            final SubtractionMode subtractionMode) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, pattern, false)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().dilation(src, pattern, subtractionMode);
            }
        }, src);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public Matrix<? extends UpdatablePArray> erosion(
            Matrix<? extends PArray> src, final Pattern pattern,
            final SubtractionMode subtractionMode) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, pattern, true)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().erosion(src, pattern, subtractionMode);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> closing(
            Matrix<? extends PArray> src, final Pattern pattern,
            final SubtractionMode subtractionMode) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, pattern, false, pattern, true)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().closing(src, pattern, subtractionMode);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> opening(
            Matrix<? extends PArray> src, final Pattern pattern,
            final SubtractionMode subtractionMode) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, pattern, true, pattern, false)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().opening(src, pattern, subtractionMode);
            }
        }, src);
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() dilationErosion ==> erosionDilation,,maskedDilationErosion,,maskedErosionDilation;;
               (ptn1,\s+)false,\s+(ptn2,\s+)true ==> $1true, $2false,,$1false, $2true,,$1true, $2false;;
               (,\s*(?:final\s+SubtractionMode\s+)?subtractionMode) ==> $1,, ,, ;;
               SUM             ==> SUM,,SUM_MAX_0,,SUM_MAX_0 */

    public Matrix<? extends UpdatablePArray> dilationErosion(
            Matrix<? extends PArray> src,
            final Pattern ptn1, final Pattern ptn2, final SubtractionMode subtractionMode) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, ptn1, false, ptn2, true)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().dilationErosion(src, ptn1, ptn2, subtractionMode);
            }
        }, src);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public Matrix<? extends UpdatablePArray> erosionDilation(
            Matrix<? extends PArray> src,
            final Pattern ptn1, final Pattern ptn2, final SubtractionMode subtractionMode) {
        return tilingProcess(new MorphologyProcessor(
                SUM.getAperture(dimCount, ptn1, true, ptn2, false)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().erosionDilation(src, ptn1, ptn2, subtractionMode);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> maskedDilationErosion(
            Matrix<? extends PArray> src,
            final Pattern ptn1, final Pattern ptn2) {
        return tilingProcess(new MorphologyProcessor(
                SUM_MAX_0.getAperture(dimCount, ptn1, false, ptn2, true)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().maskedDilationErosion(src, ptn1, ptn2);
            }
        }, src);
    }

    public Matrix<? extends UpdatablePArray> maskedErosionDilation(
            Matrix<? extends PArray> src,
            final Pattern ptn1, final Pattern ptn2) {
        return tilingProcess(new MorphologyProcessor(
                SUM_MAX_0.getAperture(dimCount, ptn1, true, ptn2, false)) {
            @Override
            public Matrix<? extends UpdatablePArray> process(Matrix<? extends PArray> src) {
                return parent().maskedErosionDilation(src, ptn1, ptn2);
            }
        }, src);
    }

    /*Repeat.AutoGeneratedEnd*/


    /*Repeat() dilation ==> erosion;; (dimCount,\s+pattern,\s+)false ==> $1true */
    public void dilation(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, final Pattern pattern,
            boolean disableMemoryAllocation) {
        Matrices.checkDimensionEquality(dest, src);
        if (disableMemoryAllocation) { // in this case we should not allocate anything, already having 1-pass algorithm
            parent.dilation(dest, src, pattern, true);
        } else {
            Map<Integer, Matrix<?>> destMatrices = new LinkedHashMap<>();
            Map<Integer, Matrix<?>> srcMatrices = new LinkedHashMap<>();
            destMatrices.put(0, dest);
            srcMatrices.put(0, src);
            tiler.tile(new MorphologyInPlaceProcessor(SUM.getAperture(dimCount, pattern, false)) {
                @Override
                public void process(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
                    parent().dilation(dest, src, pattern, false);
                }
            }).process(destMatrices, srcMatrices);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public void erosion(
            Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, final Pattern pattern,
            boolean disableMemoryAllocation) {
        Matrices.checkDimensionEquality(dest, src);
        if (disableMemoryAllocation) { // in this case we should not allocate anything, already having 1-pass algorithm
            parent.erosion(dest, src, pattern, true);
        } else {
            Map<Integer, Matrix<?>> destMatrices = new LinkedHashMap<>();
            Map<Integer, Matrix<?>> srcMatrices = new LinkedHashMap<>();
            destMatrices.put(0, dest);
            srcMatrices.put(0, src);
            tiler.tile(new MorphologyInPlaceProcessor(SUM.getAperture(dimCount, pattern, true)) {
                @Override
                public void process(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
                    parent().erosion(dest, src, pattern, false);
                }
            }).process(destMatrices, srcMatrices);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    public void dilation(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        dilation(dest, src, pattern, false);
    }

    public void erosion(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern) {
        erosion(dest, src, pattern, false);
    }

    private Matrix<? extends UpdatablePArray> tilingProcess(
            ApertureProcessor<Integer> processor,
            Matrix<? extends PArray> src) {
        Map<Integer, Matrix<?>> destMatrices = new LinkedHashMap<>();
        Map<Integer, Matrix<?>> srcMatrices = new LinkedHashMap<>();
        destMatrices.put(0, null); // reserving space for 1 result
        srcMatrices.put(0, src);
        tiler.tile(processor).process(destMatrices, srcMatrices);
        return destMatrices.get(0).cast(UpdatablePArray.class);
    }

    private abstract class MorphologyProcessor
            extends AbstractArrayProcessorWithContextSwitching
            implements ApertureProcessor<Integer>, ArrayProcessorWithContextSwitching {
        private final IRectangularArea dependenceAperture;

        private MorphologyProcessor(IRectangularArea dependenceAperture) {
            super(null); // will be replaced by the tiler
            this.dependenceAperture = dependenceAperture;
        }

        public Morphology parent() { // parent with the correct sub-task context
            return parent.context(this.context());
        }

        public void process(Map<Integer, Matrix<?>> dest, Map<Integer, Matrix<?>> src) {
            assert src.size() == 1;
            assert dest.size() == 1;
            dest.put(0, process(src.get(0).cast(PArray.class)));
        }

        public abstract Matrix<? extends PArray> process(Matrix<? extends PArray> src);

        public IRectangularArea dependenceAperture(Integer srcMatrixKey) {
            assert srcMatrixKey == 0;
            return dependenceAperture;
        }
    }

    private abstract class MorphologyInPlaceProcessor
            extends AbstractArrayProcessorWithContextSwitching
            implements ApertureProcessor<Integer>, ArrayProcessorWithContextSwitching {
        private final IRectangularArea dependenceAperture;

        private MorphologyInPlaceProcessor(IRectangularArea dependenceAperture) {
            super(null); // will be replaced by the tiler
            this.dependenceAperture = dependenceAperture;
        }

        public Morphology parent() { // parent with the correct sub-task context
            return parent.context(this.context());
        }

        public void process(Map<Integer, Matrix<?>> dest, Map<Integer, Matrix<?>> src) {
            assert src.size() == 1;
            assert dest.size() == 1;
            process(dest.get(0).cast(UpdatablePArray.class), src.get(0).cast(PArray.class));
        }

        public abstract void process(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src);

        public IRectangularArea dependenceAperture(Integer srcMatrixKey) {
            assert srcMatrixKey == 0;
            return dependenceAperture;
        }
    }
}
