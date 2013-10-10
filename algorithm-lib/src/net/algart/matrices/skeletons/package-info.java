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

 <p>AlgART Laboratory 2007-2013</p>

 @author Daniel Alievsky
 @version 1.2
 @since JDK 1.5
 */
package net.algart.matrices.skeletons;
