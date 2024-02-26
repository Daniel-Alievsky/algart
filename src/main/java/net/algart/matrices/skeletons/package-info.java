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

/**
 <p>Algorithms of <i>skeletonization</i> of binary
 2- or <i>n</i>-dimensional {@link net.algart.arrays.Matrix AlgART&nbsp;matrices}.</p>

 <p>This package contains several skeletonization classes,
 implementing {@link net.algart.arrays.IterativeArrayProcessor} interface:</p>
 <ul>
 <li>{@link net.algart.matrices.skeletons.ErodingSkeleton},</li>
 <li>{@link net.algart.matrices.skeletons.WeakOctupleThinningSkeleton2D},</li>
 <li>{@link net.algart.matrices.skeletons.OctupleThinningSkeleton2D},</li>
 <li>{@link net.algart.matrices.skeletons.Quadruple3x5ThinningSkeleton2D},</li>
 <li>{@link net.algart.matrices.skeletons.StrongQuadruple3x5ThinningSkeleton2D}.</li>
 </ul>
 <p>Every iteration of these iterative processors makes all particles (matrix areas, filled by unit elements)
 little smaller, but saves connectivity of particles
 (excepting {@link net.algart.matrices.skeletons.ErodingSkeleton}, which does not provide saving connectivity).
 After finishing all iterations, the resulting matrix, called <i>skeleton</i>,
 usually contains thin "lines", called skeleton <i>branches</i>, which connect different
 <i>skeleton nodes</i> (points of intersection of these lines).
 The class {@link net.algart.matrices.skeletons.SkeletonScanner} allows to detect and analyse
 the structure of skeleton.</p>

 @author Daniel Alievsky
 @version 1.2
 @since JDK 1.6
 */
package net.algart.matrices.skeletons;
