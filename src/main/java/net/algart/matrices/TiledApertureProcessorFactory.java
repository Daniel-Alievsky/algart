/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices;

import net.algart.arrays.*;
import net.algart.arrays.Arrays;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;

import java.util.*;

/**
 * <p><i>Tiler</i>: generator of <i>tiled</i> {@link ApertureProcessor aperture matrix processors}.
 * The tiler works with some algorithm, represented by {@link ApertureProcessor} interface
 * and called <i>one-tile processor</i>: it can be any algorithm, which
 * processes one or several {@link Matrix <i>n</i>-dimensional matrices} (with identical sets of dimensions)
 * and returns one or several other matrices as a result (with the same sets of dimensions).
 * The only requirement is that the value of every element of the resulting matrices depends only on
 * the elements of the source matrices in a fixed rectangular aperture "around" the same position,
 * as described in {@link ApertureProcessor} interface.
 * This class allows to convert the given one-tile processor into another aperture processor,
 * called a <i>tiled</i> processor, created on the base of the original one-tile processor
 * and functionally equivalent to it. (The equivalence can be violated on the bounds
 * of the matrices, where the tiled processor provides several models of continuations &mdash;
 * see below the section "Continuation model outside the bounds of the large matrices".)
 * This new processor splits all the matrices into relatively little <i>tiles</i> (rectangular areas,
 * i.e. submatrices), performs the processing of every tile with the one-tile processor and places the results
 * into the corresponding submatrices of the resulting matrices.
 * Such conversion of one algorithm to another is called <i>tiling an algorithm</i>
 * and is performed by {@link #tile(ApertureProcessor)} method &mdash; the main method of this class.</p>
 *
 *
 * <h4>Why to tile aperture processors</h4>
 *
 * <p>The goal of tiling some algorithms is optimization of processing very large matrices,
 * usually located on external storage devices (for example, with help of {@link LargeMemoryModel}).</p>
 *
 * <p>First, very large matrices are usually {@link Matrix#tile() tiled}, but many algorithms process matrices
 * in a simple "streaming" manner, i.e. load elements in the order, corresponding to the order of elements
 * in the {@link Matrix#array() built-in AlgART array}. This order of downloading is inefficient for tiled matrices.
 * The same algorithms, tiled with help of this class, process large tiled matrices in more efficient order:
 * they download a rectangular block from all source matrices into newly created
 * (relatively little) matrices, process them and store the results
 * into the corresponding submatrices of the destination matrices.
 * For maximal efficiency, the tiler tries to use {@link SimpleMemoryModel} for storing and processing every
 * rectangular block (a tile); you can control this with <tt>maxTempJavaMemory</tt> argument of all
 * instantiation methods <tt>getInstance</tt>. In addition, the matrices, allocated by the tiled processor
 * (if it creates them), are automatically tiled by {@link Matrix#tile(long...)} method (see more details below in
 * the specification of <tt>process</tt> method in the tiled aperture processors, stage <b>4.d</b>).</p>
 *
 * <p>Second, many algorithms (for example, the basic implementation of
 * {@link net.algart.matrices.morphology.Morphology mathematical morphology} from
 * {@link net.algart.matrices.morphology} package) are multipass, i.e. process source matrices in many passes
 * though all the matrix. It can be very important for high performance, when all data are located in RAM,
 * especially in a form of Java memory (via {@link SimpleMemoryModel}), but it can extremely slow down
 * the calculations, when the source matrices are very large and located on a disk or another storage,
 * because an algorithm downloads all data from external devices again during each pass.
 * Unlike this, a matrix processor, generated by the tiler (by {@link #tile(ApertureProcessor)} method),
 * is always single-pass: each tile is downloaded and saved only 1 time, and multipass processing is applied
 * to relatively little tile matrices, usually allocated in {@link SimpleMemoryModel}.</p>
 *
 * <p>Third, tiling by this class is the simplest way to optimize an algorithm for multiprocessor or multi-core
 * computers, if the algorithm does not provide multithreading optimization itself:
 * several tiles can be processed simultaneously in parallel threads. See below about multithreading.</p>
 *
 *
 * <h4>Specification of {@link ApertureProcessor#process(Map, Map)
 * process} and other methods in the tiled aperture processors</h4>
 *
 * <p>Here is the precise specification of the behaviour of the {@link ApertureProcessor},
 * tiled by this tiler, i.e. of the result of {@link #tile(ApertureProcessor oneTileProcessor)}.
 * We call the argument of this method the <i>one-tile processor</i>, and the result of this method
 * the <i>tiled processor</i>.</p>
 *
 * <ul>
 *     <li>The generic type <tt>K</tt> of indexes of the source and resulting matrices in the tiled
 *     processor is the same as in the one-tile processor. (It is obvious from the declaration
 *     of {@link #tile(ApertureProcessor) tile} method.)
 *     <br>&nbsp;</li>
 *
 *     <li>{@link ApertureProcessor#process(Map dest, Map src)} method of the tiled processor
 *     does the following.<br>&nbsp;
 *     <ol>
 *         <li>It checks, whether the arguments are correct. If they violate one of common requirement,
 *         described in "Throws" section in
 *         {@link ApertureProcessor#process(Map, Map) comments to "process" method},
 *         a corresponding exception is thrown. In particular, this implementation checks,
 *         that all matrices in the <tt>dest</tt> map are either <tt>null</tt>
 *         or updatable, i.e. their {@link Matrix#array() built-in arrays} implement
 *         {@link UpdatableArray} interface &mdash; if at least one non-null matrix in the <tt>dest</tt> map
 *         is not updatable, <tt>IllegalArgumentException</tt> is thrown.<br>
 *         In addition, the given implementation throws
 *         <tt>IllegalArgumentException</tt> if one of the passed matrices has
 *         {@link Matrix#dimCount() number of dimensions}, other than the number of dimensions of this tiler,
 *         returned by its {@link #dimCount()} method.<br>
 *         If no source matrices and no non-null resulting matrices were passed to this method,
 *         i.e. if <tt>src.isEmpty()</tt> and either <tt>dest.isEmpty()</tt>, or all matrices
 *         <tt>dest.get(key)==null</tt>, then {@link ApertureProcessor#process(Map, Map) process}
 *         method does nothing and immediately returns.
 *         If at least one of dimensions of the passed matrices is 0, then
 *         {@link ApertureProcessor#process(Map, Map) process}
 *         method also does nothing and immediately returns (there are no elements to process).
 *         <br>&nbsp;</li>
 *
 *         <li>It calculates the <i>maximal dependence aperture</i> <b>A</b><sup>m</sup>: a minimal integer
 *         rectangular area (an instance of {@link IRectangularArea}), containing all dependence apertures
 *         <b>A</b><sub><i>i</i></sub> of the one-tile processor (returned by its
 *         {@link ApertureProcessor#dependenceAperture(Object) dependenceAperture(<i>i</i>)} method)
 *         for all indexes <i>i</i>&isin;<i>Q</i>=<tt>src.keySet()</tt>, and also containing the origin
 *         of coordinates. While this calculation, <tt>IndexOutOfBoundsException</tt> will be thrown,
 *         if the number of dimensions for one of results of {@link ApertureProcessor#dependenceAperture(Object)
 *         dependenceAperture(<i>i</i>)} calls is less than {@link #dimCount()}, but if some
 *         of them has more than {@link #dimCount()} dimensions, the extra dimensions of such aperture
 *         are just ignored (here and in the further algorithm).
 *         <br>&nbsp;</li>
 *
 *         <li>It splits all source matrices <b>M</b><sub><i>i</i></sub> (<tt>src.get(<i>i</i>)</tt>)
 *         and all resulting matrices <b>M'</b><sub><i>j</i></sub> (<tt>dest.get(<i>j</i>)</tt>) into
 *         a set of rectangular non-overlapping <i>tiles</i>, i.e. submatrices, the dimensions of which are chosen
 *         to be equal to the desired tile dimensions of this tiler ({@link #tileDim()}) or, maybe, less.
 *         (This stage does not suppose any actual calculations: we consider this stage
 *         for the sake of simplicity.)<br>
 *         For every tile, we shall designate
 *         <nobr><b>f</b> = (<i>f</i><sub>0</sub>, <i>f</i><sub>1</sub>, ...,
 *         <i>f</i><sub><i>n</i>&minus;1</sub>)</nobr> the <i>n</i>-dimensional starting point of the tile (inclusive)
 *         and <nobr><b>t</b> = (<i>t</i><sub>0</sub>, <i>t</i><sub>1</sub>, ...,
 *         <i>t</i><sub><i>n</i>&minus;1</sub>)</nobr> the <i>n</i>-dimensional ending point of the tile (exclusive).
 *         (Here "f" is the starting letter of "from" word, "t" is the starting letter of "to" word.)
 *         More precisely, this tile consists of all elements of the source and target matrices with such indexes
 *         <nobr>(<i>i</i><sub>0</sub>, <i>i</i><sub>1</sub>, ..., <i>i</i><sub><i>n</i>&minus;1</sub>)</nobr>,
 *         that<br>
 *             &nbsp;&nbsp;&nbsp;&nbsp;<i>f<sub>k</sub></i> &le; <i>i<sub>k</sub></i> &lt; <i>t<sub>k</sub></i>,
 *         <nobr><i>k</i>=0,1,...,<i>n</i>&minus;1</nobr>.<br>
 *         Besides this tile (<b>f</b>,&nbsp;<b>t</b>), we also consider the <i>extended tile</i>
 *         (<b>fe</b>,&nbsp;<b>te</b>), consisting of all elements of the source and target matrices with such indexes
 *         <nobr>(<i>i</i><sub>0</sub>, <i>i</i><sub>1</sub>, ..., <i>i</i><sub><i>n</i>&minus;1</sub>)</nobr>,
 *         that<br>
 *             &nbsp;&nbsp;&nbsp;&nbsp;<i>fe<sub>k</sub></i>
 *             = <i>f<sub>k</sub></i> + <b>A</b><sup>m</sup>.{@link IRectangularArea#min(int) min(<i>k</i>)}
 *             &le; <i>i<sub>k</sub></i>
 *             &lt; <i>te<sub>k</sub></i>
 *             =  <i>t<sub>k</sub></i> + <b>A</b><sup>m</sup>.{@link IRectangularArea#min(int) max(<i>k</i>)}.
 *         <br>Note that all tiles (<b>f</b>,&nbsp;<b>t</b>) lie fully inside the dimensions
 *         of the source and target matrices, but it is not always so for extended tiles (<b>fe</b>,&nbsp;<b>te</b>).
 *         Also note that each tile (<b>f</b>,&nbsp;<b>t</b>) is a subset of the corresponding extended tile
 *         (<b>fe</b>,&nbsp;<b>te</b>), because the maximal dependence aperture <b>A</b><sup>m</sup>
 *         always contains the origin of coordinates (as written in the item 2).
 *         <br>&nbsp;</li>
 *
 *         <li>Then the {@link ApertureProcessor#process(Map dest, Map src) process}
 *         method of the tiled processor does the following, for every tile (<b>f</b>,&nbsp;<b>t</b>)
 *         and the corresponding extended tile (<b>fe</b>,&nbsp;<b>te</b>):
 *         <ol type="a">
 *             <li>For each index of a source matrices <i>i</i>&isin;<i>Q</i>=<tt>src.keySet()</tt> and
 *             for each index of a resulting non-null matrix <i>j</i>&isin;<i>R</i>=<tt>dest.keySet()</tt>,
 *             passed to this method, it allocates new matrices
 *             <b>m</b><sub><i>i</i></sub> and <b>m'</b><sub><i>j</i></sub>
 *             with the same element type as <b>M</b><sub><i>i</i></sub> and <b>M'</b><sub><i>j</i></sub>
 *             and with dimensions, equal to the sizes of the extended tile
 *             <i>te<sub>k</sub></i>&minus;<i>fe<sub>k</sub></i>.
 *             For each index <i>j</i> of a null resulting matrix <b>M'</b><sub><i>j</i></sub><tt>=null</tt>,
 *             passed to this method, it's assumed <b>m'</b><sub><i>j</i></sub><tt>=null</tt>.
 *             The newly created (relatively small) matrices (or <tt>null</tt> references) <b>m</b><sub><i>i</i></sub>
 *             and <b>m'</b><sub><i>j</i></sub> are stored in two <tt>Map&lt;K,&nbsp;Matrix&lt;?&gt;&gt;</tt> objects
 *             <tt>srcTile</tt> (<b>m</b><sub><i>i</i></sub>) and <tt>destTile</tt> (<b>m'</b><sub><i>j</i></sub>),
 *             in the same manner as the original <tt>dest</tt> and <tt>src</tt> arguments.<br>
 *             Note that the tiled processor tries to use the created matrices <b>m</b><sub><i>i</i></sub>
 *             and <b>m'</b><sub><i>j</i></sub> many times for different tiles, because there is no sense
 *             to create them again for every tile.<br>
 *             This algorithm tries to create all matrices in {@link SimpleMemoryModel}. But, it the total
 *             amount of memory, necessary simultaneously for all these matrices, is greater than
 *             {@link #maxTempJavaMemory()} bytes (this parameter is passed to all instantiation methods
 *             <tt>getInstance</tt> of the tiler), then the memory model from the {@link #context() current context}
 *             is used instead. Note that the total amount of memory depends not only on the number of arguments and
 *             results and the tile dimensions, but also on the desired
 *             {@link #numberOfTasks() number of parallel tasks}.<br>
 *             Here is a guarantee that all non-null matrices <b>m'</b><sub><i>j</i></sub>
 *             (stored in <tt>destTile</tt>) are updatable, i.e. their {@link Matrix#array() built-in arrays}
 *             implement {@link UpdatableArray} interface.
 *             </li>
 *
 *             <li>For each source matrix <b>M</b><sub><i>i</i></sub>, its submatrix, corresponding to the
 *             extended tile and extracted with <nobr><b>M</b><sub><i>i</i></sub>.{@link
 *             Matrix#subMatrix(long[], long[], Matrix.ContinuationMode)
 *             subMatrix}(<b>fe</b>, <b>te</b>, <tt>continuationMode</tt>)</nobr> call, is copied into
 *             the corresponding small matrix <b>m</b><sub><i>i</i></sub> (<tt>srcTile.get(<i>i</i>)</tt>).
 *             Here the <tt>continuationMode</tt> is equal to the continuation mode of this tiler, returned by
 *             {@link #continuationMode()} method.<br>
 *             At this step, the method may really copy less data from some source matrices (with getting
 *             the same results), if the corresponding dependence apertures <b>A</b><sub><i>i</i></sub> are less
 *             than the maximal aperture <b>A</b><sup>m</sup>.
 *             </li>
 *
 *             <li><b>Main part of the algorithm:</b>
 *             {@link ApertureProcessor#process(Map, Map) process} method of
 *             the one-tile processor is called with the arguments <b>m'</b><sub><i>j</i></sub>
 *             and <b>m</b><sub><i>i</i></sub> &mdash;
 *             <nobr><tt>oneTileProcessor.{@link ApertureProcessor#process(Map, Map)
 *             process}(destTile, srcTile)</tt></nobr>.<br>
 *             Note that, as a result, all null small matrices <b>m'</b><sub><i>j</i></sub>
 *             (<tt>destTile.get(<i>j</i>)</tt>) will become not null &mdash; it is a requirement,
 *             described in {@link ApertureProcessor#process(Map, Map)
 *             comments to "process" method}. If some matrix <nobr><tt>destTile.get(<i>j</i>)</tt></nobr>
 *             is <tt>null</tt> after calling the one-tile processor, it means an invalid implementation
 *             of the one-tile processor: <tt>AssertionError</tt> is thrown in this case.
 *             </li>
 *
 *             <li>If it is the first processed tile, this algorithm scans the whole map <tt>destTile</tt>.
 *             If it contains some matrix <b>m'</b><sub><i>j</i></sub>, for which the index <i>j</i> is not
 *             present in the <tt>dest</tt> map (<nobr><tt>!dest.containsKey(<i>j</i>)</tt></nobr>)
 *             or the value <nobr><tt>dest.get(<i>j</i>)==null</tt></nobr>,
 *             the corresponding resulting matrices <b>M'</b><sub><i>j</i></sub>
 *             is created with the element type, equal to corresponding
 *             <b>m'</b><sub><i>j</i></sub>.{@link Matrix#elementType() elementType()}, and dimensions,
 *             equal to dimensions of other source and resulting matrices <b>M</b><sub><i>i</i></sub>
 *             and <b>M'</b><sub><i>j</i></sub>. Each created matrix <b>M'</b><sub><i>j</i></sub> is saved
 *             back into <tt>dest</tt> argument:
 *             <nobr><tt>dest.put(</tt><i>j</i>,&nbsp;<b>M'</b><sub><i>j</i></sub><tt>)</tt></nobr>.<br>
 *             The resulting matrices <b>M'</b><sub><i>j</i></sub> are created in the memory model
 *             from the current context of the tiler: {@link #context()}.{@link ArrayContext#getMemoryModel()
 *             getMemoryModel()}. Moreover, every newly created matrix is automatically tiled, i.e. replaced
 *             with <tt>newMatrix.{@link Matrix#tile(long...) tile}(allocationTileDim)</tt>, where
 *             <tt>allocationTileDim</tt> is the corresponding argument of the <tt>getInstance</tt>
 *             instantiation method &mdash; with the only exception, when you explicitly specify <tt>null</tt>
 *             for this argument (in this case the new matrices are not tiled). In most cases,
 *             the tiler is used with very large matrices, and automatic tiling the resulting matrices
 *             improves performance.
 *             </li>
 *
 *             <li>The central part (submatrix) of all matrices <b>m'</b><sub><i>j</i></sub>,
 *             corresponding to the original (non-extended) tile (<b>f</b>,&nbsp;<b>t</b>),
 *             is copied into the corresponding tile of the resulting matrices <b>M'</b><sub><i>j</i></sub>,
 *             i.e. into their submatrices <nobr><b>M'</b><sub><i>j</i></sub>.{@link
 *             Matrix#subMatrix(long[], long[]) subMatrix}(<b>f</b>, <b>t</b>)</nobr>.
 *             <br>&nbsp;</li>
 *         </ol></li>
 *     </ol></li>
 *
 *     <li>{@link ApertureProcessor#dependenceAperture(Object srcMatrixKey)} method of the tiled processor
 *     just calls the same method of the one-tile processor with the same <tt>srcMatrixKey</tt> argument
 *     and returns its result.
 *     <br>&nbsp;</li>
 * </ul>
 *
 * <p>Note: there is a guarantee, that each resulting matrix <b>M'</b><sub><i>j</i></sub>,
 * created by {@link ApertureProcessor#process(Map dest, Map src) process} method of the tiled processor
 * at the stage <b>4.d</b> is <i>updatable</i>: its {@link Matrix#array() built-in array} is {@link UpdatableArray}
 * and, thus, the matrix can be cast to <nobr><tt>{@link Matrix}&lt;UpdatableArray&gt;</tt></nobr> with help of
 * <nobr><tt>{@link Matrix#cast(Class) Matrix.cast}(UpdatableArray.class)</tt></nobr> call.</p>
 *
 * <p>Note: {@link ApertureProcessor#process(Map dest, Map src) process}
 * method of the tiled processor can process several tiles simultaneously in parallel threads
 * to optimize calculations on multiprocessor or multi-core computers.
 * It depends on the <tt>numberOfTasks</tt> argument of the instantiation methods <tt>getInstance</tt>.
 * If it is 0 (or absent), the desired number of parallel tasks
 * is detected automatically on the base of the {@link ArrayContext} argument
 * of the instantiation methods.
 * Many algorithms (one-tile processors) provide multithreading optimization themselves,
 * so there is no sense to use this feature: in this case you may specify <tt>numberOfTasks=1</tt>.</p>
 *
 *
 * <h4>Continuation model outside the bounds of the large matrices</h4>
 *
 * <p>The behaviour of the aperture processor, tiled by {@link #tile(ApertureProcessor oneTileProcessor)} method,
 * can little differ from the behaviour of the original one-tile processor near the bounds of the matrices,
 * namely for the resulting elements, for which the dependence aperture
 * <nobr><b>A</b><sub><i>i</i></sub>={@link ApertureProcessor#dependenceAperture(Object)
 * dependenceAperture(<i>i</i>)}</nobr> (at least for one index <i>i</i> of a source matrix)
 * does not fully lie inside the corresponding source matrix <b>M</b><sub><i>i</i></sub>.</p>
 *
 * <p>In such situation the behaviour of the original one-tile processor depends on implementation &mdash;
 * for example, many algorithms suppose so-called pseudo-cyclic continuation mode, described in comments
 * to {@link net.algart.arrays.Matrix.ContinuationMode#PSEUDO_CYCLIC} constant.
 * But the behaviour of the resulting processor,
 * tiled by {@link #tile(ApertureProcessor)} method, is strictly defined always and corresponds to
 * the {@link net.algart.arrays.Matrix.ContinuationMode continuation mode},
 * passed as <tt>continuationMode</tt> argument to an instantiation method <tt>getInstance</tt>
 * of the tiler and returned by {@link #continuationMode()} method.
 * You can see it from the specification of the behaviour of
 * {@link ApertureProcessor#process(Map dest, Map src) process} method above,
 * stage <b>4.b</b>.</p>
 *
 * <p>If the one-tile processor works according one of continuation models, provided by
 * {@link net.algart.arrays.Matrix.ContinuationMode} class, you can guarantee the identical behaviour of
 * the tiled processor by passing the same continuation mode into a tiler instantiation method <tt>getInstance</tt>;
 * if no, the tiled processor will be impossible to provide identical results.</p>
 *
 * <p>Note that {@link net.algart.arrays.Matrix.ContinuationMode#NONE} continuation mode cannot be used in the tiler:
 * such value of <tt>continuationMode</tt> argument of instantiation methods <tt>getInstance</tt> leads
 * to <tt>IllegalArgumentException</tt>.</p>
 *
 *
 * <h4>Contexts for the one-tile processor</h4>
 *
 * <p>First of all, we note that every tiled processor &mdash; a result of {@link #tile(ApertureProcessor)} method
 * &mdash; always implements not only {@link ApertureProcessor}, but also
 * {@link ArrayProcessorWithContextSwitching} interface. So, you can use its
 * {@link ArrayProcessor#context()} and {@link ArrayProcessorWithContextSwitching#context(ArrayContext)} methods
 * after corresponding type cast. The current context of the tiled processor
 * (returned by {@link ArrayProcessor#context() context()} method) is initially equal to the
 * {@link #context() current context} of the tiler, and you can change it with help of
 * {@link ArrayProcessorWithContextSwitching#context(ArrayContext) context(ArrayContext)} method.
 * This context (if it is not <tt>null</tt>) is used for determining memory model,
 * which should be used for allocating matrices, for showing execution progress
 * and allowing to stop execution after processing every tile (even if the one-tile processor
 * does not support these features) and for multithreading simultaneous processing several tiles,
 * if <tt>{@link #numberOfTasks()}&gt;1</tt>. And it will be initially <tt>null</tt>, if the
 * {@link #context() current context} of the tiler is <tt>null</tt> &mdash; then it will be ignored.</p>
 *
 * <p>Many algorithms, which can be tiled by this class, also works with some {@link ArrayContext}
 * to provide abilities to stop calculations, show progress, determine desired memory model for allocating
 * AlgART arrays, etc. Such algorithms <i>should</i> implement not only {@link ApertureProcessor} interface,
 * but also {@link ArrayProcessorWithContextSwitching} interface, and <i>should</i> get the current context
 * via their {@link #context()} method. This requirement if not absolute, but if your algorithm retrieves
 * the context with some other way, then the behaviour of its {@link ArrayContext#updateProgress(ArrayContext.Event)}
 * method can be incorrect &mdash; your processor, processing one tile, will not "know" that it is only a part
 * of the full task (processing all tiles).</p>
 *
 * <p>If a one-tile processor, tiled by {@link #tile(ApertureProcessor)} method, really implements
 * {@link ArrayProcessorWithContextSwitching}, then
 * {@link ApertureProcessor#process(Map dest, Map src) process} method of the tiled processor
 * creates special <i>tile context</i> before processing every tile and
 * {@link ArrayProcessorWithContextSwitching#context(ArrayContext) switches} the one-tile processor
 * to this context before calling its
 * {@link ApertureProcessor#process(Map dest, Map src) process} method.
 * In other words, at the stage <b>4.c</b> the tiled processor calls not<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<nobr><tt>oneTileProcessor.{@link
 * ApertureProcessor#process(Map, Map)
 * process}(destTile, srcTile)</tt></nobr>,<br>
 * but<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<nobr><tt>((ApertureProcessor&lt;K&gt;)(oneTileProcessor.{@link
 * ArrayProcessorWithContextSwitching#context(ArrayContext)
 * context}(tileContext))).{@link ApertureProcessor#process(Map, Map)
 * process}(destTile, srcTile)</tt>.</nobr><br>
 * (By the way, it means that you are able not to think about the initial value of the
 * {@link ArrayProcessor#context() current context} in the constructor of your one-tile processor:
 * it will be surely replaced with <tt>tileContext</tt> before usage of your processor.
 * For example, you may initialize it by <tt>null</tt>.)
 * Of course, it is supposed that the switching method
 * <nobr><tt>oneTileProcessor.{@link ArrayProcessorWithContextSwitching#context(ArrayContext)
 * context}(tileContext)</tt></nobr> returns an object that also implements {@link ApertureProcessor} &mdash;
 * if it is not so, it means an invalid implementation of that method, and <tt>AssertionError</tt>
 * or <tt>ClassCastException</tt> can be thrown in this case.</p>
 *
 * <p>The <tt>tileContext</tt> here <b>is never <tt>null</tt></b>: you can freely use this fact
 * in your implementation of the one-tile processor.
 * This context is formed automatically as a {@link ArrayContext#part(double, double) part}
 * of the current context of the <i>tiled</i> processor, returned by <i>its</i> {@link ArrayProcessor#context()
 * context()} method &mdash; a part, corresponding to processing only one from a lot of tiles.
 * (As written above, by default the current context of the tiled processor is equal to the
 * {@link #context() current context} of the tiler.)
 * Thus, the tiler provides correct behaviour of
 * <nobr><tt>oneTileProcessor.{@link ArrayProcessor#context()
 * context()}.{@link ArrayContext#updateProgress(ArrayContext.Event) updateProgress(...)}</tt></nobr>
 * inside {@link ApertureProcessor#process(Map, Map) process} method
 * of your one-tile processor.
 * If the current context of the tiled processor is <tt>null</tt>,
 * <tt>tileContext</tt> is formed from {@link ArrayContext#DEFAULT}.</p>
 *
 * <p>The <tt>tileContext</tt> also provides additional information about the position and sizes
 * of the currently processed tile. Namely, it is created with help of {@link ArrayContext#customDataVersion(Object)}
 * method in such a way, that its {@link ArrayContext#customData() customData()} method always returns
 * a correctly filled instance of {@link TileInformation} class, describing the currently processed tile.</p>
 *
 * <p>If the current {@link #numberOfTasks() number of tasks}, desired for this tiler,
 * is greater than 1, and the tiled processor uses multithreading for parallel processing several tiles,
 * then the <tt>tileContext</tt> is formed in a more complex way.
 * Namely, in this case it is also a {@link ArrayContext#part(double, double) part} of the full context
 * with correctly filled {@link ArrayContext#customData() customData()} (an instance of {@link TileInformation}),
 * and in addition:</p>
 * <ul>
 *     <li>{@link ArrayContext#multithreadedVersion(int k, int n)} method is called &mdash; so,
 *     the one-tile processor can determine, in which of several parallel threads it is called
 *     (the index <tt>k</tt>) and what is the total number of parallel threads
 *     (the value <tt>n&le;{@link #numberOfTasks()}</tt> &mdash; it can be less than {@link #numberOfTasks()},
 *     for example, when the total number of tiles is less than it).
 *     This is helpful if the implementation of the one-tile processor needs some work memory
 *     or another objects, which should be created before all calculations
 *     and must be separate for different threads;</li>
 *     <li>{@link ArrayContext#singleThreadVersion()} method is called &mdash; in other words,
 *     the tiler tries to suppress multithreading in the one-tile processor, when it uses multithreading
 *     itself for parallel processing several tiles;</li>
 *     <li>{@link ArrayContext#noProgressVersion()} method is called &mdash; because a progress bar cannot be updated
 *     correctly while parallel processing several tiles (it will be updated after finishing processing
 *     this group of tiles).</li>
 * </ul>
 *
 *
 * <h4>Restrictions</h4>
 *
 * <p>Every instance of this class can work only with some fixed number <i>n</i> of matrix dimensions,
 * returned by {@link #dimCount()} method and equal to the length of <tt>tileDim</tt> array,
 * passed as an argument of the instantiation methods <tt>getInstance</tt>. It means that
 * {@link ApertureProcessor#process(Map, Map) process} method of an aperture processor,
 * returned by {@link #tile(ApertureProcessor)} method, can process only <i>n</i>-dimensional matrices
 * with <i>n</i>={@link #dimCount()} and throws <tt>IllegalArgumentException</tt> if some of the passed matrices
 * has another number of dimensions.</p>
 *
 * <p>The tiler has no restrictions for the types of matrix elements: it can work with any element types,
 * including non-primitive types. But usually the types of matrix elements are primitive.</p>
 *
 * <p>Note: in improbable cases, when the dimensions of the source and resulting matrices and/or
 * the sizes of the {@link ApertureProcessor#dependenceAperture(Object) dependence apertures}
 * are extremely large (about 2<sup>63</sup>),
 * so that the sum of some matrix dimension and the corresponding size of the aperture
 * ({@link IRectangularArea#width(int)}) or the product of all such sums (i.e. the number of elements
 * in a source/resulting matrix, {@link DependenceApertureBuilder#extendDimensions(long[], IRectangularArea)
 * extended} by such aperture) is greater than <nobr><tt>Long.MAX_VALUE</tt></nobr>,
 * the {@link ApertureProcessor#process(Map dest, Map src) process} method of the
 * {@link #tile(ApertureProcessor) tiled} processor throws <tt>IndexOutOfBoundsException</tt> and does nothing.
 * Of course, these are very improbable cases.</p>
 *
 * <h4>Creating instances of this class</h4>
 *
 * <p>To create instances of this class, you should use one of the following methods:</p>
 *
 * <ul>
 *     <li>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[])},</li>
 *     <li>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[])},</li>
 *     <li>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], int)},</li>
 *     <li>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)}.</li>
 * </ul>
 *
 *
 * <h4>Multithread compatibility</h4>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.
 * The same is true for the tiled processors, created by {@link #tile(ApertureProcessor)} method.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public final class TiledApertureProcessorFactory {

    /**
     * <p>Additional information about the current processed tile, available for tiled aperture processors
     * via their context.</p>
     *
     * <p>This object is returned by {@link ArrayContext#customData() customData()} method of the current context
     * {@link ArrayProcessor#context()} of the <i>one-tile aperture processor</i> &mdash;
     * the argument of <nobr>{@link TiledApertureProcessorFactory#tile(ApertureProcessor)}</nobr> method &mdash;
     * if this one-tile processor implements {@link ArrayProcessorWithContextSwitching} interface and
     * is called from the <i>tiled processor</i> (the result of
     * {@link TiledApertureProcessorFactory#tile(ApertureProcessor) tile} method)
     * for processing a tile. See comments to {@link TiledApertureProcessorFactory},
     * the section "Contexts for the one-tile processor".</p>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static final class TileInformation {
        private final IRectangularArea tile;
        private final IRectangularArea extendedTile;

        private TileInformation(IRectangularArea tile, IRectangularArea extendedTile) {
            assert tile != null && extendedTile != null;
            this.tile = tile;
            this.extendedTile = extendedTile;
        }

        /**
         * Returns the position and sizes of the currently processed <i>tile</i> (<b>f</b>,&nbsp;<b>t</b>).
         * See the strict definition of (<b>f</b>,&nbsp;<b>t</b>) area in the specification
         * of <tt>process</tt> method, stage <b>3</b>, in comments to {@link TiledApertureProcessorFactory}.
         *
         * <p>The {@link IRectangularArea#min() min()} point of the result contains the minimal coordinates
         * of the matrix elements, belonging to this tile:
         * <nobr>{@link IRectangularArea#min() min()} =
         * <b>f</b> = (<i>f</i><sub>0</sub>, <i>f</i><sub>1</sub>, ...,
         * <i>f</i><sub><i>n</i>&minus;1</sub>)</nobr>.
         * The {@link IRectangularArea#max() max()} point of the result contains the maximal coordinates
         * of the matrix elements, belonging to this tile:
         * <nobr>{@link IRectangularArea#max() max()} =
         * <b>t&minus;1</b> = (<i>t</i><sub>0</sub>&minus;1, <i>t</i><sub>1</sub>&minus;1, ...,
         * <i>t</i><sub><i>n</i>&minus;1</sub>&minus;1)</nobr>.
         *
         * @return the currently processed tile (<b>f</b>,&nbsp;<b>t</b>).
         */
        public IRectangularArea getTile() {
            return tile;
        }

        /**
         * Returns the position and sizes of the currently processed <i>extended tile</i> (<b>fe</b>,&nbsp;<b>te</b>).
         * See the strict definition of (<b>fe</b>,&nbsp;<b>te</b>) area in the specification
         * of <tt>process</tt> method, stage <b>3</b>, in comments to {@link TiledApertureProcessorFactory}.
         *
         * <p>The {@link IRectangularArea#min() min()} point of the result contains the minimal coordinates
         * of the matrix elements, belonging to this extended tile:
         * <nobr>{@link IRectangularArea#min() min()} =
         * <b>fe</b> = (<i>fe</i><sub>0</sub>, <i>fe</i><sub>1</sub>, ...,
         * <i>fe</i><sub><i>n</i>&minus;1</sub>)</nobr>.
         * The {@link IRectangularArea#max() max()} point of the result contains the maximal coordinates
         * of the matrix elements, belonging to this extended tile:
         * <nobr>{@link IRectangularArea#max() max()} =
         * <b>te&minus;1</b> = (<i>te</i><sub>0</sub>&minus;1, <i>te</i><sub>1</sub>&minus;1, ...,
         * <i>te</i><sub><i>n</i>&minus;1</sub>&minus;1)</nobr>.
         *
         * @return the currently processed extended tile (<b>fe</b>,&nbsp;<b>te</b>).
         */
        public IRectangularArea getExtendedTile() {
            return extendedTile;
        }
    }

    private final ArrayContext context;
    private final ThreadPoolFactory threadPoolFactory;
    private final int numberOfTasks;
    private final int originalNumberOfTasks;
    private final long maxTempJavaMemory;
    private final Matrix.ContinuationMode continuationMode;
    private final long[] processingTileDim;
    private final long[] allocationTileDim;
    private final int dimCount; // == tileDim.length

    private TiledApertureProcessorFactory(ArrayContext context,
        Matrix.ContinuationMode continuationMode, long maxTempJavaMemory,
        long[] processingTileDim, long[] allocationTileDim, int numberOfTasks)
    {
        if (continuationMode == null)
            throw new NullPointerException("Null continuation mode");
        if (processingTileDim == null)
            throw new NullPointerException("Null processing tile dimensions Java array");
        if (continuationMode == Matrix.ContinuationMode.NONE)
            throw new IllegalArgumentException(getClass() + " cannot be used with continuation mode \""
                + continuationMode + "\"");
        if (processingTileDim.length == 0)
            throw new IllegalArgumentException("Empty processing tile dimensions Java array");
        if (allocationTileDim != null && allocationTileDim.length != processingTileDim.length)
            throw new IllegalArgumentException("Different number of allocation and processing tile dimensions");
        if (numberOfTasks < 0)
            throw new IllegalArgumentException("Negative numberOfTasks=" + numberOfTasks);
        this.processingTileDim = processingTileDim.clone();
        this.allocationTileDim = allocationTileDim == null ? null : allocationTileDim.clone();
        this.dimCount = processingTileDim.length;
        for (int k = 0; k < dimCount; k++) {
            if (this.processingTileDim[k] <= 0)
                throw new IllegalArgumentException("Negative or zero processing tile dimension #"
                    + k + ": " + this.processingTileDim[k]);
            if (this.allocationTileDim != null && this.allocationTileDim[k] <= 0)
                throw new IllegalArgumentException("Negative or zero allocation tile dimension #"
                    + k + ": " + this.allocationTileDim[k]);
        }
        if (maxTempJavaMemory < 0)
            throw new IllegalArgumentException("Negative maxTempJavaMemory argument");
        this.context = context;
        this.maxTempJavaMemory = maxTempJavaMemory;
        this.continuationMode = continuationMode;
        this.threadPoolFactory = Arrays.getThreadPoolFactory(context);
        this.originalNumberOfTasks = numberOfTasks;
        this.numberOfTasks = numberOfTasks > 0 ? numberOfTasks :
            Math.max(1, this.threadPoolFactory.recommendedNumberOfTasks());
    }

    /**
     * Creates new instance of the tiler. Equivalent to the following call of the basic instantiation method:<br>
     * <tt>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     * getInstance}(context, continuationMode, maxTempJavaMemory, tileDim, {@link
     * Matrices#defaultTileDimensions(int) Matrices.defaultTileDimensions}(tileDim.length), 0)</tt>.
     *
     * @param context           see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param continuationMode  see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param maxTempJavaMemory see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param tileDim           see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @return                  new tiler.
     * @throws NullPointerException     if <tt>continuationMode</tt> or <tt>tileDim</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</tt>,
     *                                  or if <tt>maxTempJavaMemory&lt;0</tt>,
     *                                  or if <tt>tileDim.length==0</tt>,
     *                                  or if one of elements of <tt>tileDim</tt> Java array is zero or negative.
     */
    public static TiledApertureProcessorFactory getInstance(ArrayContext context,
        Matrix.ContinuationMode continuationMode, long maxTempJavaMemory, long[] tileDim)
    {
        return new TiledApertureProcessorFactory(context, continuationMode, maxTempJavaMemory,
            tileDim, Matrices.defaultTileDimensions(tileDim.length), 0);
    }

    /**
     * Creates new instance of the tiler. Equivalent to the following call of the basic instantiation method:<br>
     * <tt>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     * getInstance}(context, continuationMode, maxTempJavaMemory, tileDim, allocationTileDim, 0)</tt>.
     *
     * @param context           see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param continuationMode  see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param maxTempJavaMemory see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param tileDim           see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param allocationTileDim see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @return                  new tiler.
     * @throws NullPointerException     if <tt>continuationMode</tt> or <tt>tileDim</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</tt>,
     *                                  or if <tt>maxTempJavaMemory&lt;0</tt>,
     *                                  or if <tt>tileDim.length==0</tt>,
     *                                  or if <tt>allocationTileDim!=null</tt> and
     *                                  <tt>allocationTileDim.length!=tileDim.length</tt>,
     *                                  or if one of elements of <tt>tileDim</tt> or (non-null)
     *                                  <tt>allocationTileDim</tt> Java arrays is zero or negative.
     */
    public static TiledApertureProcessorFactory getInstance(ArrayContext context,
        Matrix.ContinuationMode continuationMode, long maxTempJavaMemory, long[] tileDim, long[] allocationTileDim)
    {
        return new TiledApertureProcessorFactory(context, continuationMode, maxTempJavaMemory,
            tileDim, allocationTileDim, 0);
    }

    /**
     * Creates new instance of the tiler. Equivalent to the following call of the basic instantiation method:<br>
     * <tt>{@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     * getInstance}(context, continuationMode, maxTempJavaMemory, tileDim, {@link
     * Matrices#defaultTileDimensions(int) Matrices.defaultTileDimensions}(tileDim.length), numberOfTasks)</tt>.
     *
     * @param context           see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param continuationMode  see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param maxTempJavaMemory see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param tileDim           see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @param numberOfTasks     see the basic {@link
     *                          #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     *                          getInstance} method.
     * @return                  new tiler.
     * @throws NullPointerException     if <tt>continuationMode</tt> or <tt>tileDim</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</tt>,
     *                                  or if <tt>maxTempJavaMemory&lt;0</tt>,
     *                                  or if <tt>tileDim.length==0</tt>,
     *                                  or if <tt>numberOfTasks&lt;0</tt>,
     *                                  or if one of elements of <tt>tileDim</tt> Java array is zero or negative.
     */
    public static TiledApertureProcessorFactory getInstance(ArrayContext context,
        Matrix.ContinuationMode continuationMode, long maxTempJavaMemory, long[] tileDim,
        int numberOfTasks)
    {
        return new TiledApertureProcessorFactory(context, continuationMode, maxTempJavaMemory,
            tileDim, Matrices.defaultTileDimensions(tileDim.length), numberOfTasks);
    }

    /**
     * Creates new instance of the tiler.
     *
     * <p>The passed Java arrays <tt>tileDim</tt> and <tt>allocationTileDim</tt> are cloned by this method:
     * no references to them are maintained by the created object.
     *
     * @param context           the {@link #context() context} that will be used by this tiler;
     *                          may be <tt>null</tt>, then it will be ignored, and
     *                          the {@link #tile(ApertureProcessor) tiled} processor will create all temporary
     *                          matrices in {@link SimpleMemoryModel}.<br>&nbsp;
     * @param continuationMode  continuation mode, used by the {@link #tile(ApertureProcessor) tiled} processor
     *                          (see also the specification of the
     *                          {@link ApertureProcessor#process(Map, Map) process}
     *                          method in the {@link TiledApertureProcessorFactory comments to this class},
     *                          stage <b>4.b</b>).<br>&nbsp;
     * @param maxTempJavaMemory maximal amount of Java memory, in bytes, allowed for allocating by the
     *                          {@link ApertureProcessor#process(Map, Map) process} method
     *                          of the {@link #tile(ApertureProcessor) tiled} processor
     *                          (see ibid., stage <b>4.a</b>). If you are sure that there is enough Java memory
     *                          for allocating all necessary matrices for {@link #numberOfTasks()} tiles
     *                          of all source and resulting matrices (with the given dimensions <tt>tileDim</tt>),
     *                          you may specify here <tt>Long.MAX_VALUE</tt>.<br>&nbsp;
     * @param tileDim           the desired dimensions of tiles, into which the source and resulting matrices
     *                          are split by the {@link #tile(ApertureProcessor) tiled} processor
     *                          (see ibid., stage <b>3</b>). Typical values for most applications are 4096x4096
     *                          or 2048x2048 (in 2-dimensional case).<br>&nbsp;
     * @param allocationTileDim if not <tt>null</tt>, then the resulting matrices <b>M'</b><sub><i>j</i></sub>,
     *                          created by the {@link #tile(ApertureProcessor) tiled} processor
     *                          (see ibid., stage <b>4.d</b>), are automatically tiled by the call
     *                          <tt>newMatrix.{@link Matrix#tile(long...) tile}(allocationTileDim)</tt>.
     *                          If it is <tt>null</tt>, the resulting matrices are not tiled.<br>&nbsp;
     * @param numberOfTasks     the desired number of tiles, which should be processed simultaneously in
     *                          parallel threads to optimize calculations on multiprocessor or multi-core computers;
     *                          may be 0, then it will be detected automatically as
     *                          <tt>{@link Arrays#getThreadPoolFactory(ArrayContext)
     *                          Arrays.getThreadPoolFactory}(context).{@link
     *                          ThreadPoolFactory#recommendedNumberOfTasks() recommendedNumberOfTasks()}</tt>.
     *                          You may specify <tt>numberOfTasks=1</tt> for saving memory, if you know that
     *                          the one-tile processors, which you are going to tile, provide multithreading
     *                          optimization themselves.
     * @return                  new tiler.
     * @throws NullPointerException     if <tt>continuationMode</tt> or <tt>tileDim</tt> argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>continuationMode=={@link
     *                                  net.algart.arrays.Matrix.ContinuationMode#NONE}</tt>,
     *                                  or if <tt>maxTempJavaMemory&lt;0</tt>,
     *                                  or if <tt>tileDim.length==0</tt>,
     *                                  or if <tt>allocationTileDim!=null</tt> and
     *                                  <tt>allocationTileDim.length!=tileDim.length</tt>,
     *                                  or if <tt>numberOfTasks&lt;0</tt>,
     *                                  or if one of elements of <tt>tileDim</tt> or (non-null)
     *                                  <tt>allocationTileDim</tt> Java arrays is zero or negative.
     * @see #context()
     * @see #continuationMode()
     * @see #maxTempJavaMemory()
     * @see #dimCount()
     * @see #tileDim()
     * @see #numberOfTasks()
     */
    public static TiledApertureProcessorFactory getInstance(ArrayContext context,
        Matrix.ContinuationMode continuationMode, long maxTempJavaMemory, long[] tileDim, long[] allocationTileDim,
        int numberOfTasks)
    {
        return new TiledApertureProcessorFactory(context, continuationMode, maxTempJavaMemory,
            tileDim, allocationTileDim, numberOfTasks);
    }

    /**
     * Returns the current context, used by this tiler. Equal to the first argument,
     * passed to an instantiation method <tt>getInstance</tt>.
     *
     * <p>This context (if it is not <tt>null</tt>) is used by the {@link #tile(ApertureProcessor) tiled} processor
     * for determining memory model, which should be used for allocating resulting matrices and, maybe,
     * temporary matrices for every tile (if {@link #maxTempJavaMemory()} is too small to allocate them
     * in {@link SimpleMemoryModel}), for showing execution progress and allowing to stop execution after
     * processing every tile (even if the one-tile processor does not support these features)
     * and for multithreading simultaneous processing several tiles, if <tt>{@link #numberOfTasks()}&gt;1</tt>.
     *
     * <p>See also the {@link TiledApertureProcessorFactory comments to this class}, the section
     * "Contexts for the one-tile processor".
     *
     * @return the current context, used by this tiler; may be <tt>null</tt>.
     */
    public ArrayContext context() {
        return this.context;
    }

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <tt>newContext</tt> for all operations.
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another context.
     */
    public TiledApertureProcessorFactory context(ArrayContext newContext) {
        return new TiledApertureProcessorFactory(newContext, continuationMode,
            maxTempJavaMemory, processingTileDim, allocationTileDim, originalNumberOfTasks);
    }

    /**
     * Returns the number of dimensions of this tiler. Equal to the number of elements of <tt>tileDim</tt>
     * arrays, passed to an instantiation method <tt>getInstance</tt>.
     *
     * <p>The tiled processor, created by {@link #tile(ApertureProcessor)} method of this tiler,
     * can process only matrices with this number of dimensions.
     *
     * @return the number of dimensions of matrices, which can be processed by aperture processors,
     *         tiled by this tiler; always &ge;1.
     */
    public int dimCount() {
        return this.dimCount;
    }

    /**
     * Returns the continuation mode, used by this tiler. Equal to the corresponding argument,
     * passed to an instantiation method <tt>getInstance</tt>.
     *
     * <p>See comments to the basic
     * {@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int) getInstance}
     * method and the {@link TiledApertureProcessorFactory comments to this class} for more details.
     *
     * @return the continuation mode of this tiler; cannot be <tt>null</tt> or
     *         {@link net.algart.arrays.Matrix.ContinuationMode#NONE}.
     */
    public Matrix.ContinuationMode continuationMode() {
        return this.continuationMode;
    }

    /**
     * Returns the maximal amount of Java memory, in bytes, allowed for allocating temporary matrices
     * for storing a tile. Equal to the corresponding argument,
     * passed to an instantiation method <tt>getInstance</tt>.
     *
     * <p>See comments to the basic
     * {@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int) getInstance}
     * method and the {@link TiledApertureProcessorFactory comments to this class} for more details.
     *
     * @return the maximal amount of Java memory, in bytes, allowed for allocating temporary matrices
     *         for storing a tile; always &ge;0.
     */
    public long maxTempJavaMemory() {
        return this.maxTempJavaMemory;
    }

    /**
     * Returns the desired dimensions of every tile. Equal to <tt>tileDim</tt>
     * arrays, passed to an instantiation method <tt>getInstance</tt>.
     *
     * <p>The returned array is a clone of the internal dimension array stored in this object.
     * The returned array is never empty (its length cannot be zero).
     * The elements of the returned array are never zero or negative.
     *
     * <p>See comments to the basic
     * {@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int) getInstance}
     * method and the {@link TiledApertureProcessorFactory comments to this class} for more details.
     *
     * @return the desired dimensions of every tile.
     */
    public long[] tileDim() {
        return this.processingTileDim.clone();
    }

    /**
     * Returns the number of tiles, which should be processed simultaneously in
     * parallel threads to optimize calculations on multiprocessor or multi-core computers.
     * It is equal to:
     * <ul>
     * <li><tt>numberOfTasks</tt> argument if this instance was created by
     * {@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[], int)
     * getInstance} method, having such argument, and if this argument was non-zero or</li>
     * <li><tt>{@link Arrays#getThreadPoolFactory(ArrayContext)
     * Arrays.getThreadPoolFactory}({@link #context()}).{@link ThreadPoolFactory#recommendedNumberOfTasks()
     * recommendedNumberOfTasks()}</tt> if this instance was created by
     * {@link #getInstance(ArrayContext, Matrix.ContinuationMode, long, long[], long[]) getInstance}
     * method without <tt>numberOfTasks</tt> argument or if this argument was zero
     * (<tt>numberOfTasks=0</tt>).</li>
     * </ul>
     *
     * @return the number of threads, that this class uses for multithreading optimization; always &ge;1.
     */
    public int numberOfTasks() {
        return numberOfTasks;
    }

    /**
     * The main method: builds the tiled aperture processor on the base of the given  one-tile processor.
     * See the {@link TiledApertureProcessorFactory comments to this class} for more details.
     *
     * <p>The result of this method always implements {@link ArrayProcessorWithContextSwitching} interface.
     * See the {@link TiledApertureProcessorFactory comments to this class}, the section
     * "Contexts for the one-tile processor".
     *
     * @param oneTileProcessor one-tile aperture processor.
     * @return                 tiled aperture processor.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public <K> ApertureProcessor<K> tile(ApertureProcessor<K> oneTileProcessor) {
        return new BasicTilingProcessor<K>(oneTileProcessor);
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * a short description of this tiler.
     *
     * @return a brief string description of this object.
     */
    @Override
    public String toString() {
        return "universal " + dimCount + "-dimensional "
            + (numberOfTasks == 1 ? "" : numberOfTasks +"-threading ")
            + "processing tiler (tiles " + JArrays.toString(processingTileDim, "x", 1000) + ")";
    }

    private class BasicTilingProcessor<K> extends AbstractArrayProcessorWithContextSwitching
        implements ApertureProcessor<K>, ArrayProcessorWithContextSwitching
    {
        private ApertureProcessor<K> oneTileProcessor;

        private BasicTilingProcessor(ApertureProcessor<K> oneTileProcessor) {
            super(context);
            if (oneTileProcessor == null)
                throw new NullPointerException("Null one-tile processor");
            this.oneTileProcessor = oneTileProcessor;
        }

        public void process(Map<K, Matrix<?>> dest, Map<K, Matrix<?>> src) {
            if (src == null)
                throw new NullPointerException("Null table of source matrices");
            if (dest == null)
                throw new NullPointerException("Null table of destination matrices");
            final Map<K, Matrix<? extends UpdatableArray>> destCopy =
                new LinkedHashMap<K, Matrix<? extends UpdatableArray>>();
            for (Map.Entry<K, Matrix<?>> e : dest.entrySet()) {
                K key = e.getKey();
                Matrix<?> m = e.getValue();
                if (m != null && !(m.array() instanceof UpdatableArray))
                    throw new IllegalArgumentException("The destination matrix with key \"" + key
                        + "\" is not updatable and cannot be used for returning result");
                destCopy.put(key, m == null ? null : m.cast(UpdatableArray.class));
            }
            final Map<K, Matrix<?>> srcCopy = new LinkedHashMap<K, Matrix<?>>();
            for (Map.Entry<K, Matrix<?>> e : src.entrySet()) {
                K key = e.getKey();
                Matrix<?> m = e.getValue();
                if (m == null)
                    throw new NullPointerException("Null source matrix with key \"" + key + "\"");
                srcCopy.put(key, m);
            }
            // - this cloning is useful if some parallel thread is changing these lists right now
            // (but not enough: we need to save new references to some dest elements)
            final long[] dim = getDimensionsAndCheck(destCopy, srcCopy);
            if (dim == null) {
                return; // no arguments and results: nothing to do
            }
            final long matrixSize = Arrays.longMul(dim);
            final long[] tileCounts = new long[dimCount];
            long[] maxTileDim = new long[dimCount]; // maximal tile dimensions; some tiles near the bounds can be less
            long tileCount = 1;
            for (int k = 0; k < dimCount; k++) {
                if (dim[k] == 0) {
                    return; // no elements in the matrices: nothing to do
                }
                tileCounts[k] = (dim[k] - 1) / processingTileDim[k] + 1;
                assert tileCounts[k] <= dim[k]; // because processingTileDim[k] > 0
                maxTileDim[k] = Math.min(dim[k], processingTileDim[k]);
                // so, there is a guarantee that maxTileDim are allowed matrix dimensions
                tileCount *= tileCounts[k]; // overflow impossible, because tileCounts[k] <= dim[k]
            }
            final int nt = (int)Math.min(numberOfTasks, tileCount);
            final IRectangularArea maxAperture = maxDependenceAperture(srcCopy.keySet());
            DependenceApertureBuilder.extendDimensions(dim, maxAperture); // overflow check before any calculations
            long maxExtTileSize = Arrays.longMul(DependenceApertureBuilder.extendDimensions(maxTileDim, maxAperture));
            double estimatedMemory = estimateWorkMemory(maxExtTileSize, destCopy.values(), srcCopy.values(), nt);
            MemoryModel betterModel = estimatedMemory < maxTempJavaMemory ? Arrays.SMM : memoryModel();
            final List<Map<K, UpdatableArray>> srcTileMem = allocateTile(betterModel, maxExtTileSize, srcCopy, nt);
            final List<Map<K, UpdatableArray>> destTileMem = allocateTile(betterModel, maxExtTileSize, destCopy, nt);

            final Matrix<?> enumerator = Matrices.matrix(Arrays.nIntCopies(tileCount, 157), tileCounts);
            // - this trivial virtual matrix is a simplest way to enumerate all tiles
            ArrayContext context = this.context(); // maybe, already not a context of TilingProcessorFactory!
            if (nt > 1) {
                context = context == null ? ArrayContext.DEFAULT_SINGLE_THREAD : context.singleThreadVersion();
            } else if (context == null) {
                context = ArrayContext.DEFAULT;
            }
            Runnable[] tasks = new Runnable[nt];
            Runnable[] postprocessing = new Runnable[nt]; // non-parallel
            long readyElementsCount = 0;
            int taskIndex = 0;
//            System.out.println("Number of tasks/tiles: " + nt + "/" + tileCount + ", " + maxAperture + "; "
//                + src.size() + " arguments and " + dest.size() + " results " + JArrays.toString(dim, "x", 1000));
            for (long tileIndex = 0; tileIndex < tileCount; tileIndex++) {
                long[] tileIndexes = enumerator.coordinates(tileIndex, null);
                final long[] tilePos = new long[dimCount];
                final long[] tileDim = new long[dimCount];
                final long[] tileMax = new long[dimCount];
                final long[] extTilePos = new long[dimCount];
                final long[] extTileDim = new long[dimCount];
                final long[] extTileMax = new long[dimCount];
                long tileSize = 1;
                for (int k = 0; k < dimCount; k++) {
                    tilePos[k] = tileIndexes[k] * processingTileDim[k];
                    assert tilePos[k] < dim[k];
                    tileDim[k] = Math.min(processingTileDim[k], dim[k] - tilePos[k]); // exclusive
                    assert tileDim[k] > 0; // because processingTileDim[k] > 0: checked in the constructor
                    tileMax[k] = tilePos[k] + tileDim[k] - 1;
                    extTileDim[k] = DependenceApertureBuilder.safelyAdd(tileDim[k], maxAperture.width(k));
                    extTilePos[k] = tilePos[k] + maxAperture.min(k);
                    extTileMax[k] = tileMax[k] + maxAperture.max(k);
                    tileSize *= tileDim[k];
                }
                final ArrayContext ac =
                    nt == 1 ?
                        context.part(readyElementsCount, readyElementsCount + tileSize, matrixSize) :
                        context.noProgressVersion();
                final Map<K, Matrix<?>> srcTile = loadSrcTile(ac.part(0.0, 0.05),
                    maxAperture, srcTileMem.get(taskIndex), srcCopy, tilePos, tileDim, extTileDim);
                final Map<K, Matrix<?>> destTile = prepareDestTile(
                    destTileMem.get(taskIndex), destCopy.keySet(), extTileDim);
                final int ti = taskIndex;
                tasks[taskIndex] = new Runnable() {
                    public void run() {
                        ArrayContext tileContext = switchingContextSupported() ?
                            ac.part(0.05, 0.95).multithreadedVersion(ti, nt).customDataVersion(new TileInformation(
                                IRectangularArea.valueOf(IPoint.valueOf(tilePos), IPoint.valueOf(tileMax)),
                                IRectangularArea.valueOf(IPoint.valueOf(extTilePos), IPoint.valueOf(extTileMax)))) :
                            ac;
                        subtaskTileProcessor(tileContext).process(destTile, srcTile);
                        // additional matrices CAN appear in destTile
                    }
                };
                postprocessing[taskIndex] = new Runnable() {
                    public void run() {
                        allocateDestMatricesIfNecessary(dim, destCopy, destTile);
                        // - synchronization is not necessary, because we call postprocessing in a single thread
                        saveDestTile(ac.part(0.95, 1.0), maxAperture, destCopy, destTile, tilePos, tileDim);
                        freeResources(destTile);
                        // maybe, destTile was created by the parent processor in some file, which should be released
                    }
                };
                taskIndex++;
                readyElementsCount += tileSize;
                if (taskIndex == nt || tileIndex == tileCount - 1) {
                    threadPoolFactory.performTasks(tasks, 0, taskIndex);
                    for (int i = 0; i < taskIndex; i++) {
                        postprocessing[i].run();
                    }
                    Class<?> elementType = (!destCopy.isEmpty() ?
                        destCopy.values().iterator().next() :
                        srcCopy.values().iterator().next()).elementType();
                    context.checkInterruptionAndUpdateProgress(elementType, readyElementsCount, matrixSize);
                }
                if (taskIndex == nt) {
                    taskIndex = 0;
                }
            }
            for (Map.Entry<K, Matrix<? extends UpdatableArray>> e : destCopy.entrySet()) {
                // saving newly created dest matrices back into the original dest list
                K key = e.getKey();
                if (dest.get(key) == null) {
                    dest.put(key, e.getValue());
                }
            }
        }

        public IRectangularArea dependenceAperture(K srcMatrixKey) {
            return oneTileProcessor.dependenceAperture(srcMatrixKey);
        }

        @Override
        public String toString() {
            return "aperture-dependent tiled processor of an " + TiledApertureProcessorFactory.this;
        }

        private long[] getDimensionsAndCheck(
            Map<K, Matrix<? extends UpdatableArray>> dest,
            Map<K, Matrix<?>> src)
        {
            long[] result = null;
            for (Map.Entry<K, Matrix<? extends UpdatableArray>> e : dest.entrySet()) {
                K key = e.getKey();
                Matrix<?> m = e.getValue();
                if (m == null) {
                    continue;
                }
                if (m.dimCount() != dimCount)
                    throw new IllegalArgumentException("The destination matrix with key \"" + key
                        + "\" has " + m.dimCount()
                        + " dimensions, but this processing tiler works with " + dimCount + " dimensions");
                if (result == null) {
                    result = m.dimensions();
                } else if (!m.dimEquals(result))
                    throw new SizeMismatchException("The destination matrix with key \"" + key
                        + "\" and the first matrix dimensions mismatch: "
                        + "the destination matrix with key \"" + key + "\" is " + m
                        + ", but the first matrix has dimensions " + JArrays.toString(result, "x", 1000));
            }
            for (Map.Entry<K, Matrix<?>> e : src.entrySet()) {
                K key = e.getKey();
                Matrix<? extends Array> m = e.getValue();
                assert m != null;
                if (m.dimCount() != dimCount)
                    throw new IllegalArgumentException("The source matrix with key \"" + key
                        + "\" has " + m.dimCount()
                        + " dimensions, but this processing tiler works with " + dimCount + " dimensions");
                if (result == null) {
                    result = m.dimensions();
                } else if (!m.dimEquals(result))
                    throw new SizeMismatchException("The source matrix with key \"" + key
                        + "\" and the first matrix dimensions mismatch: "
                        + "the source matrix with key \"" + key + "\" is " + m
                        + ", but the first matrix has dimensions " + JArrays.toString(result, "x", 1000));
            }
            return result;
        }

        private IRectangularArea maxDependenceAperture(Set<K> srcKeys) {
            long[] min = new long[dimCount]; // zero-filled by Java
            long[] max = new long[dimCount]; // zero-filled by Java
            for (K key : srcKeys) {
                IRectangularArea a = oneTileProcessor.dependenceAperture(key);
                for (int k = 0; k < dimCount; k++) {
                    min[k] = Math.min(min[k], a.min(k));
                    max[k] = Math.max(max[k], a.max(k));
                }
            }
            return IRectangularArea.valueOf(IPoint.valueOf(min), IPoint.valueOf(max));
        }

        private double estimateWorkMemory(long extendedTileSize,
            Collection<Matrix<? extends UpdatableArray>> destList,
            Collection<Matrix<?>> srcList,
            int numberOfTasks)
        {
            double result = 0.0;
            for (Matrix<?> m : srcList) {
                result += Math.max(Arrays.sizeOf(m.elementType()), 0.0) * extendedTileSize;
                // Math.max: we shall not try to use optimized memory model for non-primitive element types
            }
            for (Matrix<?> m : destList) {
                if (m != null) {
                    result += Math.max(Arrays.sizeOf(m.elementType()), 0.0) * extendedTileSize;
                }
            }
            return result * numberOfTasks;
        }

        private List<Map<K, UpdatableArray>> allocateTile(
            MemoryModel betterMemoryModel,
            long extendedTileSize,
            Map<K, ? extends Matrix<?>> processorArguments,
            int numberOfTasks)
        {
            List<Map<K, UpdatableArray>> result = new ArrayList<Map<K, UpdatableArray>>();
            for (int taskIndex = 0; taskIndex < numberOfTasks; taskIndex++) {
                Map<K, UpdatableArray> tileMemory = new LinkedHashMap<K, UpdatableArray>();
                for (Map.Entry<K, ? extends Matrix<?>> e : processorArguments.entrySet()) {
                    Matrix<?> m = e.getValue();
                    if (m != null) {
                        K key = e.getKey();
                        MemoryModel mm = Arrays.sizeOf(m.elementType()) < 0.0 ? memoryModel() : betterMemoryModel;
                        tileMemory.put(key, mm.newUnresizableArray(m.elementType(), extendedTileSize));
                    }
                }
                result.add(tileMemory);
            }
            return result;
        }

        private Map<K, Matrix<?>> loadSrcTile(ArrayContext ac,
            IRectangularArea maxAperture,
            Map<K, UpdatableArray> srcTileMem,
            Map<K, Matrix<?>> src,
            long[] tilePos, long[] tileDim, long[] extTileDim)
        {
            long len = Arrays.longMul(extTileDim);
            Map<K, Matrix<?>> srcTile = new LinkedHashMap<K, Matrix<?>>();
            long[] inTilePos = new long[dimCount];
            long[] preciseTileDim = new long[dimCount];
            long[] preciseTilePos = new long[dimCount];
            int i = 0, n = src.size();
            for (Map.Entry<K, Matrix<?>> e : src.entrySet()) {
                K key = e.getKey();
                Matrix<UpdatableArray> tileMatrix = Matrices.matrix(srcTileMem.get(key).subArr(0, len), extTileDim);
                IRectangularArea a = oneTileProcessor.dependenceAperture(key);
                assert a != null : "Null dependenceAperture(" + key + ")";
                for (int k = 0; k < dimCount; k++) {
                    inTilePos[k] = a.min(k) - maxAperture.min(k);
                    preciseTilePos[k] = tilePos[k] + a.min(k);
                    preciseTileDim[k] = DependenceApertureBuilder.safelyAdd(tileDim[k], a.width(k));
                }
                Matrices.copy(ac.part(i, ++i, n),
                    tileMatrix.subMatr(inTilePos, preciseTileDim),
                    e.getValue().subMatr(preciseTilePos, preciseTileDim, continuationMode));
                srcTile.put(key, tileMatrix);
            }
            return srcTile;
        }

        private Map<K, Matrix<?>> prepareDestTile(
            Map<K, UpdatableArray> destTileMem,
            Set<K> destKeys,
            long[] extTileDim)
        {
            long len = Arrays.longMul(extTileDim);
            Map<K, Matrix<?>> destTile = new LinkedHashMap<K, Matrix<?>>();
            for (K key : destKeys) {
                UpdatableArray a = destTileMem.get(key);
                destTile.put(key, a == null ? null : Matrices.matrix(a.subArr(0, len), extTileDim));
            }
            return destTile;
        }

        private void allocateDestMatricesIfNecessary(long[] dim,
            Map<K, Matrix<? extends UpdatableArray>> dest,
            Map<K, Matrix<?>> destTile)
        {
            for (Map.Entry<K, Matrix<?>> e : destTile.entrySet()) {
                K key = e.getKey();
                if (dest.get(key) != null) {
                    continue; // this resulting argument is pre-allocated by the external client
                }
                Matrix<?> destTileMatrix = e.getValue();
                if (destTileMatrix == null)
                    throw new AssertionError("Illegal implementation of one-tile processor "
                        + oneTileProcessor.getClass()
                        + (dest.containsKey(key) ? ": it leaves null result matrix" : ": it creates null result")
                        + " for the key \"" + key + "\"");
                Matrix<UpdatableArray> destMatrix = this.memoryModel().newMatrix(UpdatableArray.class,
                    destTileMatrix.elementType(), dim);
                if (allocationTileDim != null) {
                    destMatrix = destMatrix.tile(allocationTileDim);
                }
                dest.put(key, destMatrix);
            }
            for (Map.Entry<K, Matrix<? extends UpdatableArray>> e : dest.entrySet()) {
                K key = e.getKey();
                if (e.getValue() == null)
                    throw new AssertionError("Illegal implementation of one-tile processor "
                        + oneTileProcessor.getClass()
                        + ": it does not allocate necessary result matrix with the key \"" + key + "\"");
                if (destTile.get(key) == null)
                    throw new AssertionError("Illegal implementation of one-tile processor "
                        + oneTileProcessor.getClass()
                        + ": it removes the matrix with the key \"" + key + "\" from the list of resulting arguments");
            }
            assert dest.size() == destTile.size(); // moreover, they have identical key sets
        }

        private void saveDestTile(
            ArrayContext ac,
            IRectangularArea maxAperture,
            Map<K, Matrix<? extends UpdatableArray>> dest,
            Map<K, Matrix<?>> destTile,
            long[] tilePos, long[] tileDim)
        {
            long[] inTilePos = maxAperture.min().symmetric().coordinates();
            int i = 0, n = dest.size();
            for (Map.Entry<K, Matrix<? extends UpdatableArray>> e : dest.entrySet()) {
                K key = e.getKey();
                Matrix<? extends UpdatableArray> destMatrix = e.getValue();
                assert destMatrix != null : "internal bug: dest matrix with the key \"" + key + "\" is not allocated";
                Matrix<?> destTileMatrix = destTile.get(key);
                Matrices.copy(ac.part(i, ++i, n),
                    destMatrix.subMatr(tilePos, tileDim),
                    destTileMatrix.subMatr(inTilePos, tileDim));
            }
        }

        private void freeResources(Map<?, Matrix<?>> tile) {
            for (Matrix<?> m : tile.values()) {
                m.freeResources();
            }
        }

        @SuppressWarnings("unchecked")
        private ApertureProcessor<K> subtaskTileProcessor(ArrayContext tileContext) {
            assert tileContext != null : "Null tileContext";
            if (!switchingContextSupported()) {
                return oneTileProcessor;
            }
            Object p = ((ArrayProcessorWithContextSwitching) oneTileProcessor).context(tileContext);
            if (!(p instanceof ApertureProcessor<?>))
                throw new AssertionError("Illegal implementation of one-tile processor, "
                    + oneTileProcessor.getClass() + ": it implements " + ApertureProcessor.class
                    + ", but after switching context the result does not implement it");
            return (ApertureProcessor<K>) p;
        }

        private boolean switchingContextSupported() {
            return oneTileProcessor instanceof ArrayProcessorWithContextSwitching;
        }
    }
}
