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

package net.algart.matrices.linearfiltering;

import net.algart.arrays.*;
import net.algart.math.IRectangularArea;
import net.algart.math.patterns.WeightedPattern;
import net.algart.matrices.ApertureProcessor;
import net.algart.matrices.DependenceApertureBuilder;
import net.algart.matrices.TiledApertureProcessorFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static net.algart.matrices.DependenceApertureBuilder.SUM;
import static net.algart.matrices.DependenceApertureBuilder.SUM_MAX_0;

/**
 * <p>The filter allowing to transform any {@link Convolution} object to another instance of that interface,
 * which uses some given {@link TiledApertureProcessorFactory tiler} for processing the source matrix
 * (an argument of {@link Convolution} methods).</p>
 *
 * <p>This object is built on the base of some <i>parent</i> object,
 * implementing {@link Convolution}, and some tiler (an instance of
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
 * <p>The method {@link #asConvolution(Matrix, WeightedPattern)} is an exception
 * from this rule. These methods of this class works in the same way, as in
 * {@link ContinuedConvolution} class, the continuation mode of which is equal to
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
public class TiledConvolution implements Convolution {
    private final Convolution parent;
    private final ArrayContext context;
    private final TiledApertureProcessorFactory tiler;
    private final int dimCount;

    TiledConvolution(Convolution parent, TiledApertureProcessorFactory tiler) {
        Objects.requireNonNull(parent, "Null parent convolution");
        Objects.requireNonNull(tiler, "Null tiler");
        this.parent = parent;
        this.context = parent.context() == null ? ArrayContext.DEFAULT : parent.context();
        this.tiler = tiler.context(this.context);
        this.dimCount = tiler.dimCount();
    }
    /**
     * Returns new instance of this class with the passed parent {@link Convolution} object
     * and the specified processing tiler.
     *
     * <p>Note: the {@link #context() context} of the created object is retrieved from
     * <code>parent.{@link Convolution#context() context()}</code>, and
     * the {@link TiledApertureProcessorFactory#context() context} of the passed tiler
     * is automatically replaced with the same one &mdash; the current {@link #tiler() tiler}
     * of the created object is <code>tiler.{@link TiledApertureProcessorFactory#context(ArrayContext)
     * context}(newInstance.{@link #context() context()})</code>.
     * It means that the {@link TiledApertureProcessorFactory#context() context} of the passed tiler is not important
     * and can be {@code null}.</p>
     *
     * @param parent <i>parent</i> object: the instance of {@link Convolution} interface
     *               that will perform all operations.
     * @param tiler  the tiler, which will be used for processing matrices by this class.
     * @return       new instance of this class.
     * @throws NullPointerException if <code>parent</code> or <code>tiler</code> argument is {@code null}.
     */
    public static TiledConvolution getInstance(Convolution parent, TiledApertureProcessorFactory tiler) {
        return new TiledConvolution(parent, tiler);
    }

    /**
     * Returns the parent {@link Convolution} object, passed to
     * {@link #getInstance(Convolution, TiledApertureProcessorFactory)} method.
     *
     * @return the parent {@link Convolution} object.
     */
    public Convolution parent() {
        return this.parent;
    }

    /**
     * Returns the processing tiler that will be used for tiled processing the source matrices.
     *
     * @return the tiler, which is used for tiled processing the source matrices.
     * @see #getInstance(Convolution, TiledApertureProcessorFactory)
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
     * <code>{@link #getInstance(Convolution, TiledApertureProcessorFactory)
     * getInstance}({@link #parent()}.{@link Convolution#context(ArrayContext)
     * context}(newContext), {@link #tiler() tiler()}).</code>
     *
     * @param newContext another context, used by the returned instance; can be {@code null}.
     * @return           new instance with another context.
     */
    public Convolution context(ArrayContext newContext) {
        return new TiledConvolution(parent.context(newContext), tiler);
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

    public double increment(Class<?> elementType) {
        return parent.increment(elementType);
    }

    public Matrix<? extends PArray> asConvolution(Matrix<? extends PArray> src, WeightedPattern pattern) {
        IRectangularArea a = (parent.isPseudoCyclic() ? SUM : SUM_MAX_0).getAperture(src.dimCount(), pattern, false);
        src = DependenceApertureBuilder.extend(src, a, tiler.continuationMode());
        return DependenceApertureBuilder.reduce(parent.asConvolution(src, pattern), a);
    }

    public <T extends PArray> Matrix<T> asConvolution(Class<? extends T> requiredType,
        Matrix<? extends PArray> src, WeightedPattern pattern)
    {
        IRectangularArea a = (parent.isPseudoCyclic() ? SUM : SUM_MAX_0).getAperture(src.dimCount(), pattern, false);
        src = DependenceApertureBuilder.extend(src, a, tiler.continuationMode());
        return DependenceApertureBuilder.reduce(parent.asConvolution(requiredType, src, pattern), a);
    }

    public Matrix<? extends UpdatablePArray> convolution(Matrix<? extends PArray> src,
        final WeightedPattern pattern)
    {
        return tilingProcess(new ConvolutionProcessor(SUM.getAperture(dimCount, pattern, false)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().convolution(src, pattern);
            }
        }, null, src);
    }

    public <T extends PArray> Matrix<? extends T> convolution(final Class<? extends T> requiredType,
        Matrix<? extends PArray> src, final WeightedPattern pattern)
    {
        return tilingProcess(new ConvolutionProcessor(SUM.getAperture(dimCount, pattern, false)) {
            @Override
            public Matrix<? extends PArray> process(Matrix<? extends PArray> src) {
                return parent().convolution(requiredType, src, pattern);
            }
        }, null, src).cast(requiredType);
    }

    public void convolution(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src,
        final WeightedPattern pattern)
    {
        tilingProcess(new ConvolutionInPlaceProcessor(SUM.getAperture(dimCount, pattern, false)) {
            @Override
            public void process(Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src) {
                parent().convolution(dest, src, pattern);
            }
        }, dest, src);
    }

    private Matrix<? extends UpdatablePArray> tilingProcess(ApertureProcessor<Integer> processor,
        Matrix<? extends UpdatablePArray> dest, // maybe null
        Matrix<? extends PArray> src)
    {
        Map<Integer, Matrix<?>> destMatrices = new LinkedHashMap<>();
        Map<Integer, Matrix<?>> srcMatrices = new LinkedHashMap<>();
        destMatrices.put(0, dest);
        srcMatrices.put(0, src);
        tiler.tile(processor).process(destMatrices, srcMatrices);
        return destMatrices.get(0).cast(UpdatablePArray.class);
    }

    private abstract class ConvolutionProcessor
        extends AbstractArrayProcessorWithContextSwitching
        implements ApertureProcessor<Integer>, ArrayProcessorWithContextSwitching
    {
        private final IRectangularArea dependenceAperture;

        private ConvolutionProcessor(IRectangularArea dependenceAperture) {
            super(null); // will be replaced by the tiler
            this.dependenceAperture = dependenceAperture;
        }

        public Convolution parent() { // parent with the correct sub-task context
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

    private abstract class ConvolutionInPlaceProcessor
        extends AbstractArrayProcessorWithContextSwitching
        implements ApertureProcessor<Integer>, ArrayProcessorWithContextSwitching
    {
        private final IRectangularArea dependenceAperture;

        private ConvolutionInPlaceProcessor(IRectangularArea dependenceAperture) {
            super(null); // will be replaced by the tiler
            this.dependenceAperture = dependenceAperture;
        }

        public Convolution parent() { // parent with the correct sub-task context
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
