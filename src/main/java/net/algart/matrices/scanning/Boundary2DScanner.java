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

import java.util.Objects;

/**
 * <p>2-dimensional object boundaries scanner: the class allowing to trace boundaries of objects,
 * "drawn" at some 2-dimensional bit matrix.</p>
 *
 * <p>More precisely, let's consider some 2-dimensional AlgART bit matrix
 * <tt>{@link Matrix}&lt;?&nbsp;extends&nbsp;{@link BitArray}&gt;</tt>.
 * Below we shall designate this matrix as <b>M</b>.</p>
 *
 * <p>Let's define a <i>pixel</i> with integer coordinates (<i>x</i>,&nbsp;<i>y</i>)
 * as a set of points of the plane with such coordinates (<i>x'</i>,&nbsp;<i>y'</i>) that
 * <nobr><i>x</i>&minus;0.5&le;<i>x'</i>&le;<i>x</i>+0.5,</nobr>
 * <nobr><i>y</i>&minus;0.5&le;<i>y'</i>&le;<i>y</i>+0.5.</nobr>
 * In other words, pixel is the square with the center (<i>x</i>,&nbsp;<i>y</i>) and the side 1.0.</p>
 *
 * <p>Every unit element of the matrix <b>M</b> with {@link Matrix#index(long...) coordinates}
 * (<i>x</i>,&nbsp;<i>y</i>) corresponds to a pixel (<i>x</i>,&nbsp;<i>y</i>) at the plane.
 * Let's designate <b>IM</b> the figure (point set) consisting of all points of all pixels
 * (squares with the side 1.0), corresponding to unit (1) elements of our matrix <b>M</b>.
 * We can consider <b>IM</b> as an image (figure), "drawn" at the matrix <b>M</b>.
 * Every unit element in <b>M</b> represents a little square 1x1 (a pixel) in the image (figure) <b>IM</b>,
 * and the center of the pixel has integer coordinates in ranges
 * <nobr>0..<b>M</b>.{@link Matrix#dimX() dimX()}&minus;1,</nobr>
 * <nobr>0..<b>M</b>.{@link Matrix#dimY() dimY()}&minus;1.</nobr></p>
 *
 * <p>Then, let's consider a <i>connected object</i> at the matrix <b>M</b>, defined in the same terms
 * as in {@link ConnectedObjectScanner} class, and corresponding <i>connected figure</i>
 * in the image <b>IM</b>. As well as in that class, a connected object can have <i>straight-and-diagonal</i>
 * connectivity (8-connected object) or <i>straight</i> connectivity (4-connected object).
 * The first case corresponds to a usual connected area in the image <b>IM</b>,
 * the second case &mdash; to a connected area in the figure, coming out from <b>IM</b> by removing
 * all points with half-integer coordinates.</p>
 *
 * <p>We define the <i>boundary</i> of some connected object as the geometrical boundary of the corresponding
 * connected figure in the image <b>IM</b>. More precisely, the <i>boundary</i> of the connected object
 * is a connected component of the full set of the boundary points
 * (not pixels, but infinitesimal <i>points</i> of the plane) of the corresponding connected figure.
 * So, the connected object can have several boundaries, if there are some "holes" in it.
 * Any boundary is a chain of horizontal or vertical <i>segments</i> with the length 1.0,
 * that separate pixels from each others. The ends of each segment have half-integer coordinates,
 * and the 2nd end of the last segment coincides with the 1st end of the first segment.</p>
 *
 * <p>We define the <i>main boundary</i> of the connected object as its boundary
 * containing whole this object inside it.</p>
 *
 * <p><a name="completion"></a>We define the <i>completion</i> of the connected object as the sets of all points
 * lying at or inside its main boundary. In other words, the completion is the object, where all internal
 * "holes" ("pores") are filled. If the connected object has no "holes", its completion is identical to it.</p>
 *
 * <p>Each segment with length 1.0 in any object boundary is a boundary of some pixel, belonging to the image <b>IM</b>.
 * These pixels can lie 1) inside the boundary, and then it is true for all segments of the boundary,
 * or 2) outside the boundary, and then it is true for all segments of the boundary.</p>
 *
 * <p>In the first case we shall call the boundary as <i>external</i>,
 * and in the second case we shall call it as <i>internal</i>.
 * A connected object always have only one external boundary, namely, its main boundary.
 * But a connected object can have several internal boundaries: these are boundaries of all its "holes".</p>
 *
 * <p>This class represents a <i>boundary scanner</i>: an iterator allowing to trace all segments
 * of one boundary &mdash; in the clockwise order for external boundaries, in the anticlockwise order
 * for internal boundaries (if the <i>x</i> axis is directed <i>rightwards</i> and
 * the <i>y</i> axis is directed <i>downwards</i>). The basic method of this iterator is {@link #next()}.
 * In addition, this class allows to sequentially visit all boundaries or all main boundaries
 * of all connected objects; it is performed by the method {@link #nextBoundary()}.</p>
 *
 * <p>The boundary scanner always has the current <i>position</i>. The position consists of:</p>
 *
 * <ul>
 * <li>{@link #x()}-coordinate of the current pixel;</li>
 * <li>{@link #y()}-coordinate of the current pixel;</li>
 * <li>the current pixel {@link #side()}: index of one of 4 sides of the square 1x1,
 * represented by {@link Boundary2DScanner.Side} enumeration class.</li>
 * </ul>
 *
 * <p>There is the only exception, when the scanner has no any position &mdash;
 * directly after creating new instance. The first call of {@link #nextBoundary() nextBoundary}
 * or {@link #goTo goTo} method sets some position.</p>
 *
 * <p>In other words, the current position specifies some segment with length 1.0. This segment
 * can be an element of some object boundary, but also can be a random pixel side in the image.</p>
 *
 * <p>There are two basic methods of this class, changing the current position. The first method is
 * {@link #nextBoundary()}: it moves the current position to the nearest next object boundary,
 * according to some rules depending on a concrete kind of scanner. After calling this method you may be sure
 * that the current position specifies a segment of some object boundary.
 * The second method is {@link #next()}: it supposes that the current position specifies a segment of a boundary
 * and, if it's true, moves the current position to the next segment of this boundary.
 * So, you can find the next object boundary by {@link #nextBoundary()} method
 * and then scan it by sequential calls of {@link #next()} method.
 * Instead of manual loop of {@link #next()} calls, you can use {@link #scanBoundary(ArrayContext)} method.</p>
 *
 * <p><a name="positions_order"></a>We suppose that all possible positions are <i>sorted</i>
 * in the following "natural" order: the position
 * <i>x</i><sub>1</sub>,&nbsp;<i>y</i><sub>1</sub>,&nbsp;<i>side</i><sub>1</sub> is "less" than the position
 * <i>x</i><sub>2</sub>,&nbsp;<i>y</i><sub>2</sub>,&nbsp;<i>side</i><sub>2</sub>,</p>
 *
 * <ul>
 * <li>if <i>y</i><sub>1</sub>&lt;<i>y</i><sub>2</sub>,</li>
 * <li>or if <i>y</i><sub>1</sub>=<i>y</i><sub>2</sub> and  <i>x</i><sub>1</sub>&lt;<i>x</i><sub>2</sub>,</li>
 * <li>or if <i>y</i><sub>1</sub>=<i>y</i><sub>2</sub>,  <i>x</i><sub>1</sub>=<i>x</i><sub>2</sub>
 * and <nobr><i>side</i><sub>1</sub>.{@link Boundary2DScanner.Side#ordinal()
 * ordinal()}&lt;<i>side</i><sub>2</sub>.{@link Boundary2DScanner.Side#ordinal() ordinal()}</nobr>
 * (i.e., for the same coordinates,
 * {@link Side#X_MINUS X_MINUS}&lt;{@link Side#Y_MINUS Y_MINUS}&lt;{@link Side#X_PLUS
 * X_PLUS}&lt;{@link Side#Y_PLUS Y_PLUS}).</li>
 * </ul>
 *
 * <p>We also suppose that the "undefined" position, when the scanner is newly created and
 * {@link #nextBoundary() nextBoundary} or {@link #goTo goTo} methods
 * were not called yet, is "less" than all other positions.
 * This order is used by {@link #nextBoundary()} method.</p>
 *
 * <p>There are the following ways to create an instance of this class:</p>
 *
 * <ul>
 * <li>{@link #getSingleBoundaryScanner(Matrix, ConnectivityType)},</li>
 * <li>{@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType)},</li>
 * <li>{@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType)},</li>
 * <li>extending {@link Boundary2DWrapper} class,</li>
 * <li>using some ready wrappers like {@link Boundary2DSimpleMeasurer} or {@link Boundary2DProjectionMeasurer}.</li>
 * </ul>
 *
 * <p>The difference between instances, created by first 3 methods, is in the behavior of
 * {@link #nextBoundary()} and {@link #next()}: see comments to these instantiation methods.
 * The {@link Boundary2DWrapper} class and its inheritors just call some parent boundary scanner and,
 * maybe, do some additional work (for example, measure the objects).</p>
 *
 * <p>The instance of this class always works with some concrete matrix and some concrete connectivity type,
 * specified while creating the instance, and
 * you cannot switch an instance of this class to another bit matrix. But this class is lightweight:
 * there is no problem to create new instances for different matrices.</p>
 *
 * <p>You <b>must not</b> use this instance after any modifications in the scanned matrix,
 * performed by an external code.
 * If you modify the matrix, you must create new instance of this class after this.</p>
 *
 * <p>Below is a typical example of using this class:</p>
 *
 * <pre>
 * {@link Boundary2DScanner} scanner = {@link
 * Boundary2DScanner#getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType)
 * Boundary2DScanner.getAllBoundariesScanner}(m, um1, um2, connectivityType);
 * {@link Boundary2DSimpleMeasurer} measurer = {@link Boundary2DSimpleMeasurer#getInstance
 * Boundary2DSimpleMeasurer.getInstance}(scanner,
 * &#32;   EnumSet.of({@link Boundary2DSimpleMeasurer.ObjectParameter#AREA}));
 * while (measurer.{@link #nextBoundary()}) {
 * &#32;   measurer.{@link #checkInterruption checkInterruption}(ac);
 * &#32;   measurer.{@link #updateProgress updateProgress}(ac);
 * &#32;   measurer.{@link #scanBoundary scanBoundary}(ac);
 * &#32;   long area = measurer.{@link Boundary2DSimpleMeasurer#area() area()};
 * &#32;   // some operations with the found area
 * }
 * </pre>
 *
 * <p>Note: this class works <b>much faster</b> (in several times)
 * if the scanned matrix is created by {@link SimpleMemoryModel},
 * especially if its horizontal dimension {@link Matrix#dimX() dimX()} is divisible by 64
 * (<tt>{@link Matrix#dimX() dimX()}%64==0</tt>).
 * So, if the matrix is not created by {@link SimpleMemoryModel} and is not too large,
 * we recommend to create its clone by {@link SimpleMemoryModel},
 * expanded by <i>x</i> to the nearest integer divisible by 64, and use this class for the clone.</p>
 *
 * <p>Note: this class can process only 2-dimensional matrices.
 * An attempt to create an instance of this class for a matrix with other number of dimensions
 * leads to <tt>IllegalArgumentException</tt>.</p>
 *
 * <p>This class does not use multithreading optimization, unlike
 * {@link Arrays#copy(ArrayContext, UpdatableArray, Array)} and similar methods.
 * In other words, all methods of this class are executed in the current thread.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually, if multithreading access is necessary.
 * <b>Warning</b>! Even if you use in several different threads different instances of this class,
 * created via one of the following methods:</p>
 *
 * <ul>
 * <li>{@link #getAllBoundariesScanner(Matrix matrix, Matrix buffer1, Matrix buffer2, ConnectivityType)},</li>
 * <li>{@link #getMainBoundariesScanner(Matrix matrix, Matrix buffer, ConnectivityType)},</li>
 * </ul>
 *
 * <p>then you either must pass different buffer matrices in different threads,
 * or you must synchronize usage of this class and <b>all</b> accesses to these matrices from any threads.</p>
 * In other case, the content of buffer matrices will be unspecified and behavior of the scanning algorithm
 * will be undefined.</p>
 *
 * @author Daniel Alievsky
 */
public abstract class Boundary2DScanner {

    private static final boolean DEBUG_NESTING_LEVEL = false;

    static final int X_MINUS_CODE = 0; // for micro-optimization inside the package: switch/case with int type
    static final int Y_MINUS_CODE = 1;
    static final int X_PLUS_CODE = 2;
    static final int Y_PLUS_CODE = 3;

    /**
     * <p>The pixel side. This class represents one of 4 sides of a pixel (little square 1x1).
     * See definition of the "pixel" term in comments to {@link Boundary2DScanner} class.</p>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public enum Side {
        /**
         * The left side (if the <i>x</i> axis is directed rightwards):
         * the vertical boundary segment of the pixel with less <i>x</i>-coordinate.
         */
        X_MINUS(X_MINUS_CODE, true, 0, -1, -0.5, 0.0) {
            @Override
            AbstractBoundary2DScanner.AbstractMover getMover(AbstractBoundary2DScanner scanner) {
                return scanner.getMoverXM();
            }

            @Override
            void correctState(AbstractBoundary2DScanner scanner) {
                scanner.stepCount++;
                final long x = scanner.x;
                scanner.orientedArea -= x - 1;
                scanner.atMatrixBoundary = x == 0;
            }
        },

        /**
         * The top side (if the <i>y</i> axis is directed downwards):
         * the horizontal boundary segment of the pixel with less <i>y</i>-coordinate.
         */
        Y_MINUS(Y_MINUS_CODE, false, 1, 0, 0.0, -0.5) {
            @Override
            AbstractBoundary2DScanner.AbstractMover getMover(AbstractBoundary2DScanner scanner) {
                return scanner.getMoverYM();
            }

            @Override
            void correctState(AbstractBoundary2DScanner scanner) {
                scanner.stepCount++;
                scanner.atMatrixBoundary = scanner.y == 0;
            }
        },

        /**
         * The right side (if the <i>x</i> axis is directed rightwards):
         * the vertical boundary segment of the pixel with greater <i>x</i>-coordinate.
         */
        X_PLUS(X_PLUS_CODE, true, 0, 1, 0.5, 0.0) {
            @Override
            AbstractBoundary2DScanner.AbstractMover getMover(AbstractBoundary2DScanner scanner) {
                return scanner.getMoverXP();
            }

            @Override
            void correctState(AbstractBoundary2DScanner scanner) {
                scanner.stepCount++;
                final long x = scanner.x;
                scanner.orientedArea += x;
                scanner.atMatrixBoundary = x == scanner.dimX - 1;
            }
        },

        /**
         * The bottom side (if the <i>y</i> axis is directed downwards):
         * the horizontal boundary segment of the pixel with greater <i>y</i>-coordinate.
         */
        Y_PLUS(Y_PLUS_CODE, false, -1, 0, 0.0, 0.5) {
            @Override
            AbstractBoundary2DScanner.AbstractMover getMover(AbstractBoundary2DScanner scanner) {
                return scanner.getMoverYP();
            }

            @Override
            void correctState(AbstractBoundary2DScanner scanner) {
                scanner.stepCount++;
                scanner.atMatrixBoundary = scanner.y == scanner.dimY - 1;
            }
        };

        final int code;
        final boolean vertical;
        final int dxAlong, dyAlong;
        final double centerX, centerY;
        Step diagonal, straight, rotation; // will be filled while the main class initialization

        Side(int code, boolean vertical, int dxAlong, int dyAlong, double centerX, double centerY) {
            this.code = code;
            this.vertical = vertical;
            this.dxAlong = dxAlong;
            this.dyAlong = dyAlong;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        /**
         * Returns <tt>true</tt> for {@link #Y_MINUS} and {@link #Y_PLUS},
         * <tt>false</tt> for {@link #X_MINUS} and {@link #X_PLUS};
         *
         * @return whether it is a horizontal side of the square pixel.
         */
        public boolean isHorizontal() {
            return !vertical;
        }

        /**
         * Returns <tt>true</tt> for {@link #X_MINUS} and {@link #X_PLUS},
         * <tt>false</tt> for {@link #Y_MINUS} and {@link #Y_PLUS};
         *
         * @return whether it is a vertical side of the square pixel.
         */
        public boolean isVertical() {
            return vertical;
        }

        /**
         * Returns <i>x</i>-projection of this side of the pixel;
         * the side is considered as an oriented segment (vector).
         * It is supposed that we are passing around the pixel in the clockwise order,
         * in assumption that if the <i>x</i> axis is directed rightwards and
         * the <i>y</i> axis is directed downwards, &mdash; according to the general rules of
         * tracing boundary segments, specified in comments to {@link Boundary2DScanner} class.
         *
         * <p>This method returns <tt>+1</tt> for {@link #Y_MINUS}, <tt>-1</tt> for {@link #Y_PLUS},
         * <tt>0</tt> for {@link #X_MINUS} and {@link #X_PLUS}.
         *
         * @return <i>x</i>-projection of this side of the pixel.
         */
        public int dxAlong() {
            return dxAlong;
        }

        /**
         * Returns <i>y</i>-projection of this side of the pixel;
         * the side is considered as an oriented segment (vector).
         * It is supposed that we are passing around the pixel in the clockwise order,
         * in assumption that if the <i>x</i> axis is directed rightwards and
         * the <i>y</i> axis is directed downwards, &mdash; according to the general rules of
         * tracing boundary segments, specified in comments to {@link Boundary2DScanner} class.
         *
         * <p>This method returns <tt>-1</tt> for {@link #X_MINUS}, <tt>+1</tt> for {@link #X_PLUS},
         * <tt>0</tt> for {@link #Y_MINUS} and {@link #Y_PLUS}.
         *
         * @return <i>y</i>-projection of this side of the pixel.
         */
        public int dyAlong() {
            return dyAlong;
        }

        /**
         * Returns <i>x</i>-coordinate of the center (middle) of this side of the pixel.
         * (It is supposed that the center of this pixel is at the origin of coordinates.)
         *
         * <p>This method returns <tt>-0.5</tt> for {@link #X_MINUS}, <tt>+0.5</tt> for {@link #X_PLUS},
         * <tt>0.0</tt> for {@link #Y_MINUS} and {@link #Y_PLUS}.
         *
         * @return <i>x</i>-coordinate of the center of this pixel side.
         */
        public double centerX() {
            return centerX;
        }

        /**
         * Returns <i>y</i>-coordinate of the center (middle) of this side of the pixel.
         * (It is supposed that the center of this pixel is at the origin of coordinates.)
         *
         * <p>This method returns <tt>-0.5</tt> for {@link #Y_MINUS}, <tt>+0.5</tt> for {@link #Y_PLUS},
         * <tt>0.0</tt> for {@link #X_MINUS} and {@link #X_PLUS}.
         *
         * @return <i>x</i>-coordinate of the center of this pixel side.
         */
        public double centerY() {
            return centerY;
        }

        abstract AbstractBoundary2DScanner.AbstractMover getMover(AbstractBoundary2DScanner scanner);

        // Note: diagonalStepCount and rotationStepCount are corrected not here, but directly in the scanner
        abstract void correctState(AbstractBoundary2DScanner scanner);
    }

    /**
     * <p>The step of scanning the boundary: moving from one boundary segment to the next boundary segment.
     * See definition of the "boundary" and "boundary segment" terms in comments to {@link Boundary2DScanner}
     * class.</p>
     *
     * <p>In fact, this class is an enumeration: there are only 12 immutable instances of this class
     * (all they are created while initialization). Namely:
     *
     * <ul>
     * <li>4 <i>straight</i> movements: one of {@link #pixelCenterDX()}, {@link #pixelCenterDY()} is <tt>&plusmn;1</tt>, another is <tt>0</tt>,
     * <tt>{@link #newSide()}=={@link #oldSide()}</tt>;</li>
     * <li>4 <i>diagonal</i> movements: <tt>{@link #pixelCenterDX()}==&plusmn;1</tt>, <tt>{@link #pixelCenterDY()}==&plusmn;1</tt>,
     * {@link #oldSide()} and {@link #newSide()} is a pair of adjacent pixel sides;</li>
     * <li>4 kinds of movement around the same pixel (<i>rotations</i>), from one pixel side to next pixel side:
     * here <tt>{@link #pixelCenterDX()}==0</tt>, <tt>{@link #pixelCenterDY()}==0</tt>,
     * {@link #oldSide()} and {@link #newSide()} is a pair of adjacent pixel sides.</li>
     * </ul>
     *
     * <p>So, you can compare instances of this class by == operator, as well as enumeration instances.</p>
     *
     * <p>You can also identify instances of this class with help of a unique code, returned by {@link #code()}
     * method.</p>
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static class Step {
        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = 0,
         * {@link #pixelCenterDY()} = &minus;1,
         * {@link #oldSide()} = {@link Side#X_MINUS Side.X_MINUS},
         * {@link #newSide()} = {@link Side#X_MINUS Side.X_MINUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="left" valign="middle"
         *     style="font-size:120%">&uarr;</td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="left" valign="middle"
         *     style="font-size:120%">&uarr;</td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int Y_MINUS_CODE = 0;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = +1,
         * {@link #pixelCenterDY()} = 0,
         * {@link #oldSide()} = {@link Side#Y_MINUS Side.Y_MINUS},
         * {@link #newSide()} = {@link Side#Y_MINUS Side.Y_MINUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="top"
         *     style="font-size:120%;line-height:10px"><b>&rarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="top"
         *     style="font-size:120%;line-height:10px"><b>&rarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int X_PLUS_CODE = 1;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = 0,
         * {@link #pixelCenterDY()} = +1,
         * {@link #oldSide()} = {@link Side#X_PLUS Side.X_PLUS},
         * {@link #newSide()} = {@link Side#X_PLUS Side.X_PLUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="right" valign="middle"
         *     style="font-size:120%">&darr;</td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="right" valign="middle"
         *     style="font-size:120%">&darr;</td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int Y_PLUS_CODE = 2;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = &minus;1,
         * {@link #pixelCenterDY()} = 0,
         * {@link #oldSide()} = {@link Side#Y_PLUS Side.Y_PLUS},
         * {@link #newSide()} = {@link Side#Y_PLUS Side.Y_PLUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="bottom"
         *     style="font-size:120%;line-height:10px"><b>&larr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="bottom"
         *     style="font-size:120%;line-height:10px"><b>&larr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int X_MINUS_CODE = 3;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = &minus;1,
         * {@link #pixelCenterDY()} = &minus;1,
         * {@link #oldSide()} = {@link Side#X_MINUS Side.X_MINUS},
         * {@link #newSide()} = {@link Side#Y_PLUS Side.Y_PLUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="bottom"
         *     style="font-size:120%;line-height:10px"><b>&larr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="left" valign="middle"
         *     style="font-size:120%"><b>&uarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int X_MINUS_Y_MINUS_CODE = 4;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = +1,
         * {@link #pixelCenterDY()} = &minus;1,
         * {@link #oldSide()} = {@link Side#Y_MINUS Side.Y_MINUS},
         * {@link #newSide()} = {@link Side#X_MINUS Side.X_MINUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="left" valign="middle"
         *     style="font-size:120%"><b>&uarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="top"
         *     style="font-size:120%;line-height:10px"><b>&rarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int X_PLUS_Y_MINUS_CODE = 5;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = +1,
         * {@link #pixelCenterDY()} = +1,
         * {@link #oldSide()} = {@link Side#X_PLUS Side.X_PLUS},
         * {@link #newSide()} = {@link Side#Y_MINUS Side.Y_MINUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="right" valign="middle"
         *     style="font-size:120%"><b>&darr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="top"
         *     style="font-size:120%;line-height:10px"><b>&rarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int X_PLUS_Y_PLUS_CODE = 6;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = &minus;1,
         * {@link #pixelCenterDY()} = +1,
         * {@link #oldSide()} = {@link Side#Y_PLUS Side.Y_PLUS},
         * {@link #newSide()} = {@link Side#X_PLUS Side.X_PLUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="center" valign="bottom"
         *     style="font-size:120%;line-height:10px"><b>&larr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="right" valign="middle"
         *     style="font-size:120%"><b>&darr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int X_MINUS_Y_PLUS_CODE = 7;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = 0,
         * {@link #pixelCenterDY()} = 0,
         * {@link #oldSide()} = {@link Side#X_MINUS Side.X_MINUS},
         * {@link #newSide()} = {@link Side#Y_MINUS Side.Y_MINUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF" align="center" valign="bottom"
         *     style="font-size:120%;line-height:10px"><b>&rarr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="left" valign="middle"
         *     style="font-size:120%"><b>&uarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int ROTATION_X_MINUS_TO_Y_MINUS_CODE = 8;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = 0,
         * {@link #pixelCenterDY()} = 0,
         * {@link #oldSide()} = {@link Side#Y_MINUS Side.Y_MINUS},
         * {@link #newSide()} = {@link Side#X_PLUS Side.X_PLUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF" align="center" valign="bottom"
         *     style="font-size:120%;line-height:10px"><b>&rarr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="right" valign="middle"
         *     style="font-size:120%"><b>&darr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int ROTATION_Y_MINUS_TO_X_PLUS_CODE = 9;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = 0,
         * {@link #pixelCenterDY()} = 0,
         * {@link #oldSide()} = {@link Side#X_PLUS Side.X_PLUS},
         * {@link #newSide()} = {@link Side#Y_PLUS Side.Y_PLUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="right" valign="middle"
         *     style="font-size:120%"><b>&darr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF" align="center" valign="top"
         *     style="font-size:120%;line-height:10px"><b>&larr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int ROTATION_X_PLUS_TO_Y_PLUS_CODE = 10;

        /**
         * Result of {@link #code()} method for the following scanning step:
         * {@link #pixelCenterDX()} = 0,
         * {@link #pixelCenterDY()} = 0,
         * {@link #oldSide()} = {@link Side#Y_PLUS Side.Y_PLUS},
         * {@link #newSide()} = {@link Side#X_MINUS Side.X_MINUS}.
         * It is shown below:
         * <blockquote>
         * <pre><table cellpadding=0 cellspacing=0 border=0><caption></caption><tr>
         * <td bgcolor="#000000"><table cellpadding=0 cellspacing=1 border=0><caption></caption>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#E0E000" align="left" valign="middle"
         *     style="font-size:120%"><b>&uarr;</b></td>
         *     <td width=25 height=25 bgcolor="#E0E0E0"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF" align="center" valign="top"
         *     style="font-size:120%;line-height:10px"><b>&larr;</b></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * <tr>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         *     <td width=25 height=25 bgcolor="#FFFFFF"></td>
         * </tr>
         * </table></td></tr></table>
         * </pre>
         * </blockquote>
         */
        public static final int ROTATION_Y_PLUS_TO_X_MINUS_CODE = 11;

        final int code, pixelCenterDX, pixelCenterDY;
        final boolean samePixel, horizontal, vertical, straight, diagonal;
        final double d;
        final double segmentCenterDX, segmentCenterDY, pixelVertexX, pixelVertexY;
        final int increasedPixelVertexX, increasedPixelVertexY;
        final Side oldSide, newSide;

        /**
         * Equal to <tt>StrictMath.sqrt(2.0)</tt>: the distance between pixel centers for diagonal steps.
         * See {@link #distanceBetweenPixelCenters()}.
         */
        public static final double DIAGONAL_LENGTH = StrictMath.sqrt(2.0);

        /**
         * Equal to <tt>0.5 * StrictMath.sqrt(2.0)</tt>: the distance between segment centers for diagonal steps
         * and movements around the same pixel.
         */
        public static final double HALF_DIAGONAL_LENGTH = 0.5 * StrictMath.sqrt(2.0);

        private Step(int code, int pixelCenterDX, int pixelCenterDY, Side oldSide, Side newSide) {
            assert code <= 12 && Math.abs(pixelCenterDX) <= 1 && Math.abs(pixelCenterDY) <= 1;
            this.code = code;
            this.pixelCenterDX = pixelCenterDX;
            this.pixelCenterDY = pixelCenterDY;
            this.samePixel = pixelCenterDX == 0 && pixelCenterDY == 0;
            this.horizontal = pixelCenterDX == 0 && pixelCenterDY != 0;
            this.vertical = pixelCenterDX != 0 && pixelCenterDY == 0;
            this.straight = this.horizontal || this.vertical;
            this.diagonal = pixelCenterDX != 0 && pixelCenterDY != 0;
            this.d = this.samePixel ? 0.0 : this.straight ? 1.0 : DIAGONAL_LENGTH;
            this.oldSide = oldSide;
            this.newSide = newSide;
            if (this.straight) {
                this.segmentCenterDX = pixelCenterDX;
                this.segmentCenterDY = pixelCenterDY;
                this.pixelVertexX = pixelCenterDX == 0 ? newSide.centerX : pixelCenterDX == 1 ? -0.5 : 0.5;
                this.pixelVertexY = pixelCenterDY == 0 ? newSide.centerY : pixelCenterDY == 1 ? -0.5 : 0.5;
            } else if (this.samePixel) {
                this.segmentCenterDX = newSide.centerX - oldSide.centerX;
                this.segmentCenterDY = newSide.centerY - oldSide.centerY;
                this.pixelVertexX = newSide == Side.X_MINUS || newSide == Side.X_PLUS ?
                        newSide.centerX : oldSide.centerX;
                this.pixelVertexY = newSide == Side.Y_MINUS || newSide == Side.Y_PLUS ?
                        newSide.centerY : oldSide.centerY;
                if ((StrictMath.abs(0.5 * (oldSide.dxAlong() + newSide.dxAlong()) - segmentCenterDX) > 0.001) ||
                        (StrictMath.abs(0.5 * (oldSide.dyAlong() + newSide.dyAlong()) - segmentCenterDY) > 0.001)) {
                    throw new AssertionError("Incorrect class initialization: "
                            + "dx/dyAlong do not match to centerX/Y in " + oldSide + " and " + newSide);
                }
            } else {
                this.segmentCenterDX = 0.5 * pixelCenterDX;
                this.segmentCenterDY = 0.5 * pixelCenterDY;
                this.pixelVertexX = newSide == Side.X_MINUS || newSide == Side.X_PLUS ?
                        newSide.centerX : -oldSide.centerX;
                this.pixelVertexY = newSide == Side.Y_MINUS || newSide == Side.Y_PLUS ?
                        newSide.centerY : -oldSide.centerY;
            }
            if (StrictMath.abs(StrictMath.abs(pixelVertexX) + StrictMath.abs(pixelVertexY) - 1.0) > 0.001) {
                throw new AssertionError("Incorrect class initialization "
                        + "(|pixelVertexX| + |this.pixelVertexY| != 1): " + this);
            }
            this.increasedPixelVertexX = (int) Math.round(this.pixelVertexX + 0.5);
            this.increasedPixelVertexY = (int) Math.round(this.pixelVertexY + 0.5);
            if (increasedPixelVertexX != 0 && increasedPixelVertexX != 1) {
                throw new AssertionError("Incorrect class initialization "
                        + "(increasedPixelVertexX != 0 and != 1): " + this);
            }
            if (increasedPixelVertexY != 0 && increasedPixelVertexY != 1) {
                throw new AssertionError("Incorrect class initialization "
                        + "(increasedPixelVertexY != 0 and != 1): " + this);
            }
        }

        /**
         * Returns integer unique code from 0 to 11, identifying this step.
         * Works very quickly (this method just returns an internal field).
         *
         * <p>This method returns one of the following
         * constants:
         * <ul>
         * <li>{@link #Y_PLUS_CODE},</li>
         * <li>{@link #X_PLUS_CODE},</li>
         * <li>{@link #Y_MINUS_CODE},</li>
         * <li>{@link #X_MINUS_CODE},</li>
         * <li>{@link #X_MINUS_Y_MINUS_CODE},</li>
         * <li>{@link #X_PLUS_Y_MINUS_CODE},</li>
         * <li>{@link #X_PLUS_Y_PLUS_CODE},</li>
         * <li>{@link #X_MINUS_Y_PLUS_CODE},</li>
         * <li>{@link #ROTATION_X_MINUS_TO_Y_MINUS_CODE},</li>
         * <li>{@link #ROTATION_Y_MINUS_TO_X_PLUS_CODE},</li>
         * <li>{@link #ROTATION_X_PLUS_TO_Y_PLUS_CODE},</li>
         * <li>{@link #ROTATION_Y_PLUS_TO_X_MINUS_CODE}.</li>
         * </ul>
         *
         * <p>You can use this value for maximally efficient switching, depending on the step kind,
         * using Java <tt>switch</tt> operator.
         *
         * @return integer unique code of this step (0..11).
         */
        public int code() {
            return code;
        }

        /**
         * Returns change of <i>x</i>-coordinate of the pixel center, performed by this step. Can be &minus;1, 0, 1.
         * In other word, returns the increment of the {@link Boundary2DScanner#x() current position}
         * along <i>x</i>-axis.
         * Works very quickly (this method just returns an internal field).
         *
         * @return change of <i>x</i>-coordinate of the center of the current pixel.
         * @see Boundary2DScanner#x()
         */
        public int pixelCenterDX() {
            return pixelCenterDX;
        }

        /**
         * Returns change of <i>y</i>-coordinate of the pixel center, performed by this step. Can be &minus;1, 0, 1.
         * In other word, returns the increment of the {@link Boundary2DScanner#y() current position}
         * along <i>y</i>-axis.
         * Works very quickly (this method just returns an internal field).
         *
         * @return change of <i>y</i>-coordinate of the center of the current pixel.
         * @see Boundary2DScanner#y()
         */
        public int pixelCenterDY() {
            return pixelCenterDY;
        }

        /**
         * Returns change of <i>x</i>-coordinate of the center of the current boundary segment,
         * performed by this step. Can be 0.0, &plusmn;0.5, &plusmn;1.0.
         * Works very quickly (this method just returns an internal field).
         *
         * @return change of <i>x</i>-coordinate of the center of the current boundary segment.
         * @see Boundary2DScanner.Side#centerX()
         */
        public double segmentCenterDX() {
            return segmentCenterDX;
        }

        /**
         * Returns change of <i>y</i>-coordinate of the center of the current boundary segment,
         * performed by this step. Can be 0.0, &plusmn;0.5, &plusmn;1.0.
         * Works very quickly (this method just returns an internal field).
         *
         * @return change of <i>y</i>-coordinate of the center of the current boundary segment.
         * @see Boundary2DScanner.Side#centerY()
         */
        public double segmentCenterDY() {
            return segmentCenterDY;
        }

        /**
         * Returns <i>x</i>-coordinate of the vertex of the new (current) pixel,
         * which is common for the current and previous segments (pixel sides),
         * on the assumption that the center of the new (current) pixel is at the origin of coordinates.
         * Works very quickly (this method just returns an internal field).
         *
         * @return <i>x</i>-coordinate of the vertex of the new (current) pixel,
         * lying between previous and current segments of the boundary.
         */
        public double pixelVertexX() {
            return pixelVertexX;
        }

        /**
         * Returns <i>y</i>-coordinate of the vertex of the new (current) pixel,
         * which is common for the current and previous segments (pixel sides),
         * on the assumption that the center of the new (current) pixel is at the origin of coordinates.
         * Works very quickly (this method just returns an internal field).
         *
         * @return <i>y</i>-coordinate of the vertex of the new (current) pixel,
         * lying between previous and current segments of the boundary.
         */
        public double pixelVertexY() {
            return pixelVertexY;
        }

        /**
         * Returns <tt>{@link #pixelVertexX()}+0.5</tt>. It is always an integer value 0 or 1.
         * Works very quickly (this method just returns an internal field).
         *
         * <p>In other words, returns <i>x</i>-coordinate of the vertex of the new (current) pixel,
         * which is common for the current and previous segments (pixel sides),
         * on the assumption that the left up corner of the new (current) pixel is at the origin of coordinates.
         *
         * @return 0.5 + <i>x</i>-coordinate of the vertex of the new (current) pixel,
         * lying between previous and current segments of the boundary.
         */
        public int increasedPixelVertexX() {
            return increasedPixelVertexX;
        }

        /**
         * Returns <tt>{@link #pixelVertexY()}+0.5</tt>. It is always an integer value 0 or 1.
         * Works very quickly (this method just returns an internal field).
         *
         * <p>In other words, returns <i>y</i>-coordinate of the vertex of the new (current) pixel,
         * which is common for the current and previous segments (pixel sides),
         * on the assumption that the left up corner of the new (current) pixel is at the origin of coordinates.
         *
         * @return 0.5 + <i>y</i>-coordinate of the vertex of the new (current) pixel,
         * lying between previous and current segments of the boundary.
         */
        public int increasedPixelVertexY() {
            return increasedPixelVertexY;
        }

        /**
         * Returns <tt>true</tt> if <tt>{@link #pixelCenterDX()}==0 &amp;&amp; {@link #pixelCenterDY()}==0</tt>
         * (<i>rotation</i> kind of movement).
         * Works very quickly (this method just returns an internal field).
         *
         * @return <tt>true</tt> if <tt>{@link #pixelCenterDX()}==0 &amp;&amp; {@link #pixelCenterDY()}==0</tt>.
         */
        public boolean isRotation() {
            return samePixel;
        }

        /**
         * Returns <tt>true</tt> if {@link #pixelCenterDX()} is &plusmn;1 and {@link #pixelCenterDY()} is 0.
         * Works very quickly (this method just returns an internal field).
         *
         * @return <tt>true</tt> if <tt>{@link #pixelCenterDX()}!=0 &amp;&amp; {@link #pixelCenterDY()}==0</tt>.
         */
        public boolean isHorizontal() {
            return horizontal;
        }

        /**
         * Returns <tt>true</tt> if {@link #pixelCenterDY()} is &plusmn;1 and {@link #pixelCenterDX()} is 0.
         * Works very quickly (this method just returns an internal field).
         *
         * @return <tt>true</tt> if <tt>{@link #pixelCenterDX()}==0 &amp;&amp; {@link #pixelCenterDY()}!=0</tt>.
         */
        public boolean isVertical() {
            return vertical;
        }

        /**
         * Returns <tt>true</tt> if one of {@link #pixelCenterDX()} and {@link #pixelCenterDY()} values
         * is &plusmn;1, but another is 0
         * (<i>straight</i> kind of movement).
         * Works very quickly (this method just returns an internal field).
         *
         * @return <tt>true</tt> if <tt>{@link #isHorizontal()} || {@link #isVertical()}</tt>.
         */
        public boolean isStraight() {
            return straight;
        }

        /**
         * Returns <tt>true</tt> if <tt>{@link #pixelCenterDX()}!=0 &amp;&amp; {@link #pixelCenterDY()}!=0</tt>
         * (<i>diagonal</i> kind of movement).
         * Works very quickly (this method just returns an internal field).
         *
         * @return <tt>true</tt> if <tt>{@link #pixelCenterDX()}!=0 &amp;&amp; {@link #pixelCenterDY()}!=0</tt>.
         */
        public boolean isDiagonal() {
            return diagonal;
        }

        /**
         * Returns the distance between the centers of the previous and current pixel;
         * equal to <tt>StrictMath.hypot({@link #pixelCenterDX()}, {@link #pixelCenterDY()})</tt>.
         * In other words, returns 0 if {@link #isRotation()}, 1 for straight steps and
         * {@link #DIAGONAL_LENGTH}=&radic;2 for diagonal steps.
         * Works very quickly (this method just returns an internal field).
         *
         * @return the distance between the centers of the previous and current pixe.
         */
        public double distanceBetweenPixelCenters() {
            return d;
        }

        /**
         * Returns the distance between the centers of the previous and current boundary segments.
         * In other words, returns 1 for straight steps and
         * {@link #HALF_DIAGONAL_LENGTH}=0.5*&radic;2 for diagonal steps and for movements around the same pixel.
         * Works very quickly (this method just returns an internal field).
         *
         * @return the distance between the centers of the previous and current boundary segments.
         */
        public double distanceBetweenSegmentCenters() {
            return straight ? 1.0 : HALF_DIAGONAL_LENGTH;
        }

        /**
         * Returns the previous pixel side.
         *
         * @return the previous pixel side.
         */
        public Side oldSide() {
            return oldSide;
        }

        /**
         * Returns the new (current) pixel side.
         *
         * @return the new (current) pixel side.
         */
        public Side newSide() {
            return newSide;
        }

        /**
         * Returns a brief string description of this object.
         *
         * <p>The result of this method may depend on implementation.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return "boundary scanning step from " + oldSide + " to " + newSide
                    + " by " + pixelCenterDX + "," + pixelCenterDY
                    + " (pixel vertex " + pixelVertexX + "," + pixelVertexY + ")";
        }
    }

    static final Side[] ALL_SIDES = Side.values();

    static {
        Side.X_MINUS.diagonal = new Step(Step.X_MINUS_Y_MINUS_CODE, -1, -1, Side.X_MINUS, Side.Y_PLUS);
        Side.X_MINUS.straight = new Step(Step.Y_MINUS_CODE, 0, -1, Side.X_MINUS, Side.X_MINUS);
        Side.X_MINUS.rotation = new Step(Step.ROTATION_X_MINUS_TO_Y_MINUS_CODE, 0, 0, Side.X_MINUS, Side.Y_MINUS);
        Side.Y_MINUS.diagonal = new Step(Step.X_PLUS_Y_MINUS_CODE, 1, -1, Side.Y_MINUS, Side.X_MINUS);
        Side.Y_MINUS.straight = new Step(Step.X_PLUS_CODE, 1, 0, Side.Y_MINUS, Side.Y_MINUS);
        Side.Y_MINUS.rotation = new Step(Step.ROTATION_Y_MINUS_TO_X_PLUS_CODE, 0, 0, Side.Y_MINUS, Side.X_PLUS);
        Side.X_PLUS.diagonal = new Step(Step.X_PLUS_Y_PLUS_CODE, 1, 1, Side.X_PLUS, Side.Y_MINUS);
        Side.X_PLUS.straight = new Step(Step.Y_PLUS_CODE, 0, 1, Side.X_PLUS, Side.X_PLUS);
        Side.X_PLUS.rotation = new Step(Step.ROTATION_X_PLUS_TO_Y_PLUS_CODE, 0, 0, Side.X_PLUS, Side.Y_PLUS);
        Side.Y_PLUS.diagonal = new Step(Step.X_MINUS_Y_PLUS_CODE, -1, 1, Side.Y_PLUS, Side.X_PLUS);
        Side.Y_PLUS.straight = new Step(Step.X_MINUS_CODE, -1, 0, Side.Y_PLUS, Side.Y_PLUS);
        Side.Y_PLUS.rotation = new Step(Step.ROTATION_Y_PLUS_TO_X_MINUS_CODE, 0, 0, Side.Y_PLUS, Side.X_MINUS);
        // checking constants
        if (ALL_SIDES.length != 4)
            throw new AssertionError("The number of sides must be 4");
        for (Side side : Side.values()) {
            if (side.diagonal.oldSide != side || side.straight.oldSide != side || side.rotation.oldSide != side)
                throw new AssertionError("Incorrect class initialization");
            if (side.straight.newSide != side)
                throw new AssertionError("Incorrect class initialization");
            if (!side.rotation.samePixel)
                throw new AssertionError("Incorrect class initialization");
        }
        int xSum = 0;
        int ySum = 0;
        for (Side side : Side.values()) {
            xSum += side.straight.pixelCenterDX;
            ySum += side.straight.pixelCenterDY;
        }
        if (xSum != 0 || ySum != 0)
            throw new AssertionError("Incorrect class initialization");
        xSum = 0;
        ySum = 0;
        for (Side side : Side.values()) {
            xSum += side.diagonal.pixelCenterDX;
            ySum += side.diagonal.pixelCenterDY;
        }
        if (xSum != 0 || ySum != 0)
            throw new AssertionError("Incorrect class initialization");
    }

    final Matrix<? extends BitArray> matrix;
    final BitArray array;
    final long arrayLength;
    final long dimX, dimY;

    Boundary2DScanner(Matrix<? extends BitArray> matrix) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        if (matrix.dimCount() != 2) {
            throw new IllegalArgumentException(Boundary2DScanner.class
                    + " can be used for 2-dimensional matrices only");
        }
        this.matrix = matrix;
        this.array = matrix.array();
        this.arrayLength = this.array.length();
        this.dimX = matrix.dimX();
        this.dimY = matrix.dimY();
    }

    /**
     * Creates an instance of the simplest kind of this class,
     * allowing to trace all segments of a <i>single</i> boundary (internal or external).
     *
     * <p>In the created instance:</p>
     *
     * <ul>
     * <li>{@link #nextBoundary()} method finds (after the current position)
     * the nearest vertical segment, belonging to some object boundary,
     * sets the current position to the found one and does nothing else.
     * <br>&nbsp;</li>
     *
     * <li>{@link #next()} method switches to the next segment in the current object boundary and does nothing else.
     * </li>
     * </ul>
     *
     * <p>This instance does not save anywhere the fact of tracing the boundary.
     * So, it is not convenient for scanning all boundaries of some kind in the matrix:
     * {@link #nextBoundary()} method will find the same boundary many times, at least 2 times
     * for every horizontal line intersecting the boundary.
     *
     * @param matrix           the matrix that will be scanned by the created instance.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if one of arguments is {@code null}.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is not 2.
     */
    public static Boundary2DScanner getSingleBoundaryScanner(
            Matrix<? extends BitArray> matrix,
            ConnectivityType connectivityType) {
        Objects.requireNonNull(matrix, "Null matrix argument");
        Objects.requireNonNull(connectivityType, "Null connectivityType argument");
        switch (connectivityType) {
            case STRAIGHT_ONLY:
                if (!isDirectBitArray(matrix.array())) {
                    return new SingleBoundary2D4Scanner(matrix);
                } else {
                    return new DirectSingleBoundary2D4Scanner(matrix);
                }
            case STRAIGHT_AND_DIAGONAL:
                if (!isDirectBitArray(matrix.array())) {
                    return new SingleBoundary2D8Scanner(matrix);
                } else {
                    return new DirectSingleBoundary2D8Scanner(matrix);
                }
        }
        throw new AssertionError("Unsupported connectivity type: " + connectivityType);
    }

    /**
     * Creates an instance of this class, allowing to sequentially trace all segments of <i>all</i> boundaries
     * at the matrix (internal and external).
     *
     * <p>The scanner, created by this method, works with two additional matrices <tt>buffer1</tt>
     * and <tt>buffer2</tt>, that are used for marking already visited boundary segments.
     * These matrices can have any fixed-point element type (but usually it is <tt>boolean</tt>)
     * and must have the same dimensions as the main matrix.
     * These matrices should be zero-initialized before using the created instance (in other case,
     * some boundaries are possible to be skipped).
     * One of these matrices is always <i>current</i>.
     * In the <i>state&nbsp;1</i>, the current buffer matrix is <tt>buffer1</tt>;
     * in the <i>state&nbsp;2</i>, the current buffer matrix is <tt>buffer2</tt>.
     * The <i>state&nbsp;1</i> is default: it is chosen after creating the scanner.
     *
     * <p>While scanning boundaries, inside the {@link #next()} method, this scanner writes "brackets" in the current
     * buffer matrix. It means that:
     *
     * <ul>
     * <li>when the {@link #side() current pixel side} is
     * {@link Side#X_MINUS X_MINUS}, the element with coordinates {@link #x()},{@link #y()}
     * in the current buffer matrix is set to 1 ("opening bracket"),</li>
     * <li>when the {@link #side() current pixel side} is
     * {@link Side#X_PLUS X_PLUS}, the element with coordinates {@link #x()}+1,{@link #y()}
     * in the current buffer matrix is set to 1 ("closing bracket"), or nothing occurs if
     * <tt>{@link #x()}+1&gt;=matrix.{@link Matrix#dimX() dimX()}</tt>,</li>
     * <li>nothing occurs if the {@link #side() current pixel side} is
     * {@link Side#Y_MINUS Y_MINUS} or {@link Side#Y_PLUS Y_PLUS}.</li>
     * </ul>
     *
     * <p>This behavior is the same as in main boundaries scanner created by
     * {@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType) getMainBoundariesScanner} method.
     *
     * <p>The {@link #nextBoundary()} method in this scanner finds (after the current position)
     * the nearest vertical segment, belonging to some object boundary,
     * which was not visited yet by {@link #next()} method,
     * and sets the current position to the found one.
     * "Not visited" means that no "brackets" are set for that position neither in <tt>buffer1</tt>
     * nor in <tt>buffer2</tt> matrix.
     * <small>(There is the only exception from this simple rule:
     * this method never stops at the right side of a last pixel in the horizontal line.
     * If the last element in the horizontal line is 1, the corresponding boundary &mdash;
     * its right side &mdash; is always skipped, and
     * {@link #nextBoundary()} method searches for the next unit element in next lines.
     * The only case when it can be important is calling {@link #nextBoundary()} after
     * direct positioning by {@link #goTo} method.)</small>
     *
     * <p>If the newly found position corresponds to a left pixel side
     * ({@link #side()} is {@link Side#X_MINUS Side.X_MINUS}),
     * this method changes the current state to <i>state&nbsp;1</i>.
     * It means an external boundary, if the scanning the matrix was started outside any boundaries,
     * in particular, if {@link #goTo goTo} method was never called.
     *
     * <p>If the newly found position corresponds to a right pixel side
     * ({@link #side()} is {@link Side#X_PLUS Side.X_PLUS}),
     * this method changes the current state to <i>state&nbsp;2</i>
     * It means an internal boundary, if the scanning the matrix was started outside any boundaries,
     * in particular, if {@link #goTo goTo} method was never called.
     *
     * <p>While searching the next non-visited boundary, {@link #nextBoundary()} method counts "brackets"
     * in 1st and 2nd buffer matrices and corrects the current {@link #nestingLevel() nesting level}.
     *
     * <p>It is possible to specify the same matrix as both <tt>buffer1</tt> and <tt>buffer2</tt> arguments.
     * In this case, all will work normally excepting the {@link #nestingLevel() nesting level},
     * which will be calculated incorrectly.
     *
     * <p>This instance is convenient for scanning all boundaries in the matrix.
     * To do this, it's possible to use the following loop:
     *
     * <pre>
     * {@link Boundary2DScanner} scanner = {@link
     * Boundary2DScanner#getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType)
     * getAllBoundariesScanner}(m, um1, um2, connectivityType);
     * while (scanner.{@link #nextBoundary()}) {
     * &#32;   scanner.{@link #scanBoundary scanBoundary}(ac); // or some more useful actions
     * }
     * </pre>
     *
     * @param matrix           the matrix that will be scanned by the created instance.
     * @param buffer1          the 1st buffer matrix for writing "brackets" (usually indicates external boundaries).
     * @param buffer2          the 2nd buffer matrix for writing "brackets" (usually indicates internal boundaries).
     *                         To save memory, you may pass here the same matrix as <tt>buffer1</tt>
     *                         and <tt>buffer2</tt> arguments, but in this case the {@link #nestingLevel()} method
     *                         will work incorrectly.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if one of arguments is {@code null}.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is not 2.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     */
    public static Boundary2DScanner getAllBoundariesScanner(
            Matrix<? extends BitArray> matrix,
            Matrix<? extends UpdatablePFixedArray> buffer1,
            Matrix<? extends UpdatablePFixedArray> buffer2,
            ConnectivityType connectivityType) {
        Objects.requireNonNull(connectivityType, "Null connectivityType argument");
        switch (connectivityType) {
            case STRAIGHT_ONLY:
                if (!isDirectBitArray(matrix.array())) {
                    return new AllBoundaries2D4Scanner(matrix, buffer1, buffer2);
                } else {
                    return new DirectAllBoundaries2D4Scanner(matrix, buffer1, buffer2);
                }
            case STRAIGHT_AND_DIAGONAL:
                if (!isDirectBitArray(matrix.array())) {
                    return new AllBoundaries2D8Scanner(matrix, buffer1, buffer2);
                } else {
                    return new DirectAllBoundaries2D8Scanner(matrix, buffer1, buffer2);
                }
        }
        throw new AssertionError("Unsupported connectivity type: " + connectivityType);
    }

    /**
     * Creates an instance of this class, allowing to trace all segments of <i>main</i> boundaries
     * at the matrix and to build <a href="Boundary2DScanner.html#completion">completions</a> of all objects.
     *
     * <p>The scanner, created by this method, works with the additional matrix <tt>buffer</tt>,
     * where <a href="Boundary2DScanner.html#completion">completions</a>
     * of all objects are stored as a result of the scanning.
     * This matrix can have any fixed-point element type (but usually it is <tt>boolean</tt>)
     * and must have the same dimensions as the main matrix.
     * This matrix should be zero-initialized before using the created instance (in other case,
     * some boundaries are possible to be skipped).
     *
     * <p>While scanning boundaries, inside the {@link #next()} method, this scanner writes "brackets" in the
     * buffer matrix. It means that:
     *
     * <ul>
     * <li>when the {@link #side() current pixel side} is
     * {@link Side#X_MINUS X_MINUS}, the element with coordinates {@link #x()},{@link #y()}
     * in the buffer matrix is set to 1 ("opening bracket"),</li>
     * <li>when the {@link #side() current pixel side} is
     * {@link Side#X_PLUS X_PLUS}, the element with coordinates {@link #x()}+1,{@link #y()}
     * in the buffer matrix is set to 1 ("closing bracket"), or nothing occurs if
     * <tt>{@link #x()}+1&gt;=matrix.{@link Matrix#dimX() dimX()}</tt>,</li>
     * <li>nothing occurs if the {@link #side() current pixel side} is
     * {@link Side#Y_MINUS Y_MINUS} or {@link Side#Y_PLUS Y_PLUS}.</li>
     * </ul>
     *
     * <p>This behavior is the same as in all boundaries scanner created by
     * {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType) getAllBoundariesScanner} method.
     *
     * <p>The {@link #nextBoundary()} method in this scanner is more complicated.
     * If the element of the buffer matrix at the current position is zero,
     * it just finds the nearest vertical segment,
     * belonging to some object boundary,
     * after the current position (alike in the simplest scanner returned by
     * {@link #getSingleBoundaryScanner(Matrix, ConnectivityType) getSingleBoundaryScanner} method).
     * In other case we suppose that we are at the "open bracket" (the beginning of a series of unit elements),
     * and {@link #nextBoundary()} method does the following:
     *
     * <ol>
     * <li>finds the <i>next</i> unit element <tt>#p</tt> in the <i>buffer</i> matrix in the same horizontal line
     * ("close bracket", written while previous scanning the boundary of the current object);</li>
     * <li>fills all elements in the buffer matrix from the current position (inclusive) until
     * the found <tt>#p</tt> position at this line (exclusive) by 1;</li>
     * <li>clears the element <tt>#p</tt> in the buffer matrix to 0;</li>
     * <li>and finds the nearest vertical segment in the main matrix, belonging to some object boundary,
     * after all elements filled at step&nbsp;2.
     * </ol>
     *
     * <p>If the next unit element was not found in the current line at step&nbsp;1,
     * all buffer elements until the end of the horizontal line are filled by 1 &mdash;
     * the <tt>p</tt> index is supposed to be {@link #matrix()}.{@link Matrix#dimX() dimX()}
     * &mdash; and the step 3 is skipped.
     *
     * <p>In fact, {@link #nextBoundary()} method skips all interior of previously scanned boundaries and fills
     * this interior by 1 in the buffer matrix. As a result, the buffer matrix will contain
     * <a href="Boundary2DScanner.html#completion">completions</a>
     * of all objects after finishing scanning the matrix.
     *
     * <p>This instance is convenient for scanning main boundaries in the matrix and, as a side effect,
     * for calculating completions of all objects.
     * To do this, you may call {@link #fillHoles(Matrix, Matrix, ConnectivityType)} method
     * or use the equivalent loop:
     *
     * <pre>
     * {@link Boundary2DScanner} scanner = {@link
     * Boundary2DScanner#getMainBoundariesScanner(Matrix, Matrix, ConnectivityType)
     * getMainBoundariesScanner}(m, um, connectivityType);
     * while (scanner.{@link #nextBoundary()}) {
     * &#32;   scanner.{@link #scanBoundary scanBoundary}(); // or some more useful actions
     * }
     * // now um contains the completions of all objects drawn in m
     * // (if um was initially zero-filled)
     * </pre>
     *
     * @param matrix           the matrix that will be scanned by the created instance.
     * @param buffer           the buffer matrix for writing "brackets" and filling holes.
     * @param connectivityType the connectivity kind used by the created instance.
     * @return new instance of this class.
     * @throws NullPointerException     if one of arguments is {@code null}.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is not 2.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     */
    public static Boundary2DScanner getMainBoundariesScanner(
            Matrix<? extends BitArray> matrix,
            Matrix<? extends UpdatablePFixedArray> buffer,
            ConnectivityType connectivityType) {
        Objects.requireNonNull(connectivityType, "Null connectivityType argument");
        switch (connectivityType) {
            case STRAIGHT_ONLY:
                if (!isDirectBitArray(matrix.array())) {
                    return new MainBoundaries2D4Scanner(matrix, buffer);
                } else {
                    return new DirectMainBoundaries2D4Scanner(matrix, buffer);
                }
            case STRAIGHT_AND_DIAGONAL:
                if (!isDirectBitArray(matrix.array())) {
                    return new MainBoundaries2D8Scanner(matrix, buffer);
                } else {
                    return new DirectMainBoundaries2D8Scanner(matrix, buffer);
                }
        }
        throw new AssertionError("Unsupported connectivity type: " + connectivityType);
    }

    /**
     * Makes <a href="Boundary2DScanner.html#completion">completion</a> of the source binary matrix and returns it
     * in the newly created matrix. Equivalent to the following code:</p>
     * <pre>
     *      Matrix<UpdatableBitArray> result = memoryModel.{@link MemoryModel#newBitMatrix(long...)
     *      newBitMatrix}(source.dimensions());
     *      {@link #fillHoles(Matrix, Matrix, ConnectivityType) fillHoles}(result, source, connectivityType);
     * </pre>
     *
     * @param memoryModel      the memory model, used for creating the result matrix.
     * @param source           the source bit matrix.
     * @param connectivityType the connectivity kind used while building completion.
     * @throws NullPointerException     if one of argument is {@code null}.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is not 2.
     */
    public static Matrix<UpdatableBitArray> fillHoles(
            MemoryModel memoryModel,
            Matrix<? extends BitArray> source,
            ConnectivityType connectivityType) {
        Objects.requireNonNull(memoryModel, "Null memoryModel");
        Objects.requireNonNull(source, "Null source bit matrix");
        Objects.requireNonNull(connectivityType, "Null connectivityType argument");
        Matrix<UpdatableBitArray> result = memoryModel.newBitMatrix(source.dimensions());
        final Boundary2DScanner scanner = Boundary2DScanner.getMainBoundariesScanner(source, result, connectivityType);
        while (scanner.nextBoundary()) {
            scanner.scanBoundary();
        }
        return result;
    }

    /**
     * Makes <a href="Boundary2DScanner.html#completion">completion</a> of the source binary matrix and returns it
     * in the result matrix. Compared to the source matrix, all the "holes" ("pores") in the resulting matrix
     * are filled.</p>
     *
     * <p>This method is equivalent to the following code:</p>
     * <pre>
     * {@link Boundary2DScanner} scanner = {@link
     * Boundary2DScanner#getMainBoundariesScanner(Matrix, Matrix, ConnectivityType)
     * getMainBoundariesScanner}(source, result, connectivityType);
     * {@link Matrices#clear Matrices.clear}(result); // initial zero-filling
     * while (scanner.{@link #nextBoundary()}) {
     * &#32;   scanner.{@link #scanBoundary scanBoundary}(); // or some more useful actions
     * }
     * </pre>
     *
     * @param result           the completion: the bit matrix with filled holes.
     * @param source           the source bit matrix.
     * @param connectivityType the connectivity kind used while building completion.
     * @throws NullPointerException     if one of argument is {@code null}.
     * @throws IllegalArgumentException if <tt>matrix.{@link Matrix#dimCount() dimCount()}</tt> is not 2.
     * @throws SizeMismatchException    if the passed matrices have different dimensions.
     */
    public static void fillHoles(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            ConnectivityType connectivityType) {
        Objects.requireNonNull(result, "Null result bit matrix");
        Objects.requireNonNull(source, "Null source bit matrix");
        Objects.requireNonNull(connectivityType, "Null connectivityType argument");
        final Boundary2DScanner scanner = Boundary2DScanner.getMainBoundariesScanner(source, result, connectivityType);
        Matrices.clear(result);
        while (scanner.nextBoundary()) {
            scanner.scanBoundary();
        }
    }

    /*Repeat() SingleBoundaryScanner ==> AllBoundariesScanner,,MainBoundariesScanner;;
               a single boundary ==> an all boundaries,,a main boundaries */

    /**
     * Returns <tt>true</tt> if and only if this scanner is a single boundary scanner. More precisely,
     * it is <tt>true</tt> if and only if:
     * <ul>
     *     <li>this instance was created by {@link #getSingleBoundaryScanner} method</li>
     *     <li>or it is {@link Boundary2DWrapper} and this method of its {@link Boundary2DWrapper#parent()
     *     parent scanner} returns <tt>true</tt>.</li>
     * </ul>
     *
     * @return whether this scanner is a a single boundary scanner.
     */
    public abstract boolean isSingleBoundaryScanner();
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Returns <tt>true</tt> if and only if this scanner is an all boundaries scanner. More precisely,
     * it is <tt>true</tt> if and only if:
     * <ul>
     *     <li>this instance was created by {@link #getAllBoundariesScanner} method</li>
     *     <li>or it is {@link Boundary2DWrapper} and this method of its {@link Boundary2DWrapper#parent()
     *     parent scanner} returns <tt>true</tt>.</li>
     * </ul>
     *
     * @return whether this scanner is a an all boundaries scanner.
     */
    public abstract boolean isAllBoundariesScanner();


    /**
     * Returns <tt>true</tt> if and only if this scanner is a main boundaries scanner. More precisely,
     * it is <tt>true</tt> if and only if:
     * <ul>
     *     <li>this instance was created by {@link #getMainBoundariesScanner} method</li>
     *     <li>or it is {@link Boundary2DWrapper} and this method of its {@link Boundary2DWrapper#parent()
     *     parent scanner} returns <tt>true</tt>.</li>
     * </ul>
     *
     * @return whether this scanner is a a main boundaries scanner.
     */
    public abstract boolean isMainBoundariesScanner();
    /*Repeat.AutoGeneratedEnd*/

    /**
     * Returns the reference to the currently scanned matrix.
     * If this instance was created by
     * {@link #getSingleBoundaryScanner(Matrix, ConnectivityType) getSingleBoundaryScanner},
     * {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType) getAllBoundariesScanner} or
     * {@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType) getMainBoundariesScanner},
     * the first argument of those methods is returned.
     * If this instance is {@link Boundary2DWrapper}, the result of {@link #matrix()} method
     * of the parent scanner is returned.
     *
     * @return the reference to the currently scanned matrix.
     */
    public final Matrix<? extends BitArray> matrix() {
        return matrix;
    }

    /**
     * Returns <tt>{@link #matrix() matrix()}.{@link Matrix#dimX() dimX()}</tt>.
     *
     * @return x-dimension of the currently scanner matrix.
     */
    public final long dimX() {
        return dimX;
    }

    /**
     * Returns <tt>{@link #matrix() matrix()}.{@link Matrix#dimY() dimY()}</tt>.
     *
     * @return y-dimension of the currently scanner matrix.
     */
    public final long dimY() {
        return dimY;
    }

    /**
     * Returns the connectivity kind, used by this object.
     * It is specified while creating this instance.
     *
     * @return the connectivity kind, used by this object.
     */
    public abstract ConnectivityType connectivityType();

    /**
     * Returns <tt>true</tt> if and only if this instance was positioned to some coordinates in the matrix.
     * More precisely, returns <tt>false</tt> if this instance was newly created and none from
     * {@link #nextBoundary()}, {@link #goTo goTo}, {@link #goToSamePosition goToSamePosition}
     * methods were called yet, or <tt>true</tt> in all other cases.
     * If this instance is {@link Boundary2DWrapper}, the result of this method for the parent scanner is returned.
     * If this object is not positioned, most of methods, processing pixels in the current position,
     * throw <tt>IllegalStateException</tt>.
     *
     * @return <tt>true</tt> if and only if this instance was already positioned by
     * {@link #nextBoundary() nextBoundary} or
     * {@link #goTo goTo} method.
     */
    public abstract boolean isInitialized();

    /**
     * Returns <tt>true</tt> if and only if this scanner is already positioned
     * ({@link #isInitialized()} returns <tt>true</tt>) and, in addition, {@link #next()} or
     * {@link #scanBoundary(ArrayContext)} methods were called at least once.
     *
     * <p>This information can be useful before calling {@link #lastStep()} method
     * (for example, for debugging goals): that method throws <tt>IllegalStateException</tt>
     * if and only if this method returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if and only {@link #next()} or {@link #scanBoundary(ArrayContext)} methods
     * were successfully called after creating this instance.
     */
    public abstract boolean isMovedAlongBoundary();

    /**
     * Returns the current <i>x</i>-coordinate (or throws <tt>IllegalStateException</tt> if the scanner
     * was not {@link #isInitialized() positioned yet}).
     *
     * @return the current <i>x</i>-coordinate.
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public abstract long x();

    /**
     * Returns the current <i>y</i>-coordinate (or throws <tt>IllegalStateException</tt> if this scanner
     * was not {@link #isInitialized() positioned yet}).
     *
     * @return the current <i>y</i>-coordinate.
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public abstract long y();

    /**
     * Returns the current pixel side (or throws <tt>IllegalStateException</tt> if this scanner
     * was not {@link #isInitialized() positioned yet}).
     *
     * @return the current pixel side.
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public abstract Side side();

    /**
     * Returns <tt>true</tt> if and only if the {@link #side() current pixel side} lies at the boundary
     * of the {@link #matrix() scanned matrix}.
     * In other words, returns <tt>true</tt> if:
     * <ul>
     *     <li><tt>{@link #side() side()}=={@link Side#X_MINUS Side.X_MINUS}</tt> and
     *     <tt>{@link #x() x()}==0</tt>,</li>
     *     <li>or <tt>{@link #side() side()}=={@link Side#Y_MINUS Side.Y_MINUS}</tt> and
     *     <tt>{@link #y() y()}==0</tt>,</li>
     *     <li>or <tt>{@link #side() side()}=={@link Side#X_PLUS Side.X_PLUS}</tt> and
     *     <tt>{@link #x() x()}=={@link #dimX() dimX()}-1</tt>,</li>
     *     <li>or <tt>{@link #side() side()}=={@link Side#Y_PLUS Side.Y_PLUS}</tt> and
     *     <tt>{@link #y() y()}=={@link #dimY() dimY()}-1</tt>.</li>
     * </ul>
     * <p>Note: if this scanner was not {@link #isInitialized() positioned yet}, this method
     * does not throw an exception and simply returns <tt>false</tt>.
     *
     * @return whether the current segment of the boundary is a part of the boundary of the whole scanned matrix.
     */
    public abstract boolean atMatrixBoundary();

    /**
     * Returns the current <i>nesting level</i> of object boundaries:
     * the number of boundaries (external or internal), inside which the current pixel side
     * &mdash; the segment with the length 1.0, described by {@link #x()}, {@link #y()}, {@link #side()}
     * &mdash; is located.
     * (Here we suppose, that if the current pixel side lies <i>at</i> some boundary,
     * then it lies <i>inside</i> this boundary.)
     *
     * <p>Just after creating an instance of this class the nesting level is 0.
     * After the first call of {@link #nextBoundary()} it becomes 1.
     * After finding the first internal boundary (if it exists) by {@link #nextBoundary()}
     * the nesting level becomes 2.
     * After each intersection of a boundary while searching for the next boundary
     * the nesting level is increased by 1 or decreased by 1.
     * So, odd values of the nesting level correspond to external boundaries
     * and even values correspond to internal boundaries, excepting the case of a newly created instance
     * (the only case when it is 0).
     *
     * <p>Please <i>note:</i> the nesting level is supported only
     * <ol>
     * <li>if this scanner was created via
     * {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType)} method;</li>
     * <li>if the <tt>buffer1</tt> and <tt>buffer2</tt> argument of that method are different,
     * independently allocated matrices;</li>
     * <li>and if {@link #goTo} method was never called;</li>
     * <li>note: if this instance is {@link Boundary2DWrapper}, the result of this method for the parent scanner
     * is returned.</li>
     * </ol>
     *
     * <p>If this scanner was created via
     * {@link #getSingleBoundaryScanner(Matrix, ConnectivityType)} or
     * {@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType)},
     * the returned nesting level is always 0.
     * In all other cases, the result of this method is not specified.
     *
     * @return the current <i>nesting level</i> of object boundaries.
     */
    public abstract long nestingLevel();

    /**
     * Returns the index of the current pixel in the {@link Matrix#array() underlying array} of the currently
     * scanned matrix. This method is almost equivalent to
     * <nobr><tt>{@link #y()} * {@link #matrix()}.{@link Matrix#dimX() dimX()} + {@link #x()}</tt></nobr>,
     * with the only difference that it works even if this scanner was not {@link #isInitialized() positioned yet}:
     * in the last case it returns 0.
     *
     * @return the index of the current pixel in the underlying array of the scanned matrix.
     */
    public abstract long currentIndexInArray();

    /**
     * Sets the current position in the matrix to the specified coordinates and pixel side.
     *
     * <p>Usually this method is not necessary for scanners, created by
     * {@link #getAllBoundariesScanner getAllBoundariesScanner} and
     * {@link #getMainBoundariesScanner getMainBoundariesScanner} methods:
     * it is enough to use {@link #nextBoundary()} and {@link #next()} (or {@link #scanBoundary(ArrayContext)})
     * methods to visit all object boundaries at the matrix.
     * But this method may be helpful if you need to scan a single boundary (for example,
     * that was found by another scanner).
     *
     * @param x    new current <i>x</i>-coordinate.
     * @param y    new current <i>y</i>-coordinate.
     * @param side new current pixel side.
     * @throws NullPointerException      if <tt>side</tt> argument is {@code null}.
     * @throws IndexOutOfBoundsException if <tt>x&lt;0</tt>, <tt>y&lt;0</tt>,
     *                                   <tt>x&gt;={@link #matrix()}.{@link Matrix#dimX() dimX()}</tt>
     *                                   or <tt>y&gt;={@link #matrix()}.{@link Matrix#dimY() dimY()}</tt>.
     */
    public abstract void goTo(long x, long y, Side side);

    /**
     * Sets the current position in the matrix to the same as in the specified scanner.
     * Equivalent to the following call:
     * <tt>{@link #goTo goTo}(scanner.{@link #x() x()}, scanner.{@link #y() y()}, scanner.{@link #side() side()})</tt>.
     *
     * @param scanner some other scanner.
     * @throws NullPointerException      if <tt>scanner</tt> argument is {@code null}.
     * @throws IllegalStateException     if the specified <tt>scanner</tt> was not {@link #isInitialized() positioned yet}.
     * @throws IndexOutOfBoundsException in the same situations as {@link #goTo goTo} method
     *                                   (impossible if the currently scanned matrices of this and passed scanners
     *                                   have identical dimensions).
     */
    public final void goToSamePosition(Boundary2DScanner scanner) {
        goTo(scanner.x(), scanner.y(), scanner.side());
    }

    /**
     * Resets the counters, returned by {@link #stepCount()} and {@link #orientedArea()} method,
     * and all other counters, that are possibly increased by inheritors of this class.
     * This method is automatically called at the end of {@link #nextBoundary()} and
     * {@link #goTo} methods.
     */
    public abstract void resetCounters();

    /**
     * Returns the value of the current element of the currently scanned matrix.
     * This method is equivalent to
     * <nobr><tt>{@link #matrix()}.{@link Matrix#array() array()}.{@link BitArray#getBit(long)
     * getBit}({@link #currentIndexInArray()})</tt></nobr>,
     * but works little faster.
     * This method works even if this scanner was not {@link #isInitialized() positioned yet};
     * in this case, it returns the value of (0,0) matrix element
     * (i.e. <nobr><tt>{@link #matrix()}.{@link Matrix#array() array()}.{@link BitArray#getBit(long)
     * getBit(0)}</tt></nobr>).
     *
     * @return the value of the current element of the currently scanned matrix.
     */
    public abstract boolean get();

    /**
     * Finds the next vertical segment, belonging to some object boundary,
     * after the current position, and sets the current position to the found one.
     *
     * <p>More precisely, it finds some "next" position <i>after</i> the current position,
     * in the <a href="Boundary2DScanner.html#positions_order">natural order</a>,
     * where the side is {@link Side#X_MINUS X_MINUS} or {@link Side#X_PLUS X_PLUS}
     * and one from two matrix elements on the left and on the right from the specified segment (pixel side) is 1,
     * but another from these two elements is 0 or lies outside the matrix.
     * If this scanner was not {@link #isInitialized() positioned yet}, this method finds the first
     * such position.
     *
     * <p>The precise sense of the "next" term above depends on the kind of the boundary scanner.
     * <ul>
     * <li>If this scanner is created by {@link #getSingleBoundaryScanner(Matrix, ConnectivityType)}
     * method, it is just the nearest possible position
     * (in the <a href="Boundary2DScanner.html#positions_order">natural order</a>).</li>
     * <li>If this scanner is created by {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType)}
     * method, it is the nearest boundary segment that was not visited yet by {@link #next()} method.</li>
     * <li>If this scanner is created by {@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType)}
     * method, it is the nearest boundary segment that was not visited yet by {@link #next()} method
     * and that does not lie inside some already scanned boundary.</li>
     * </ul>
     *
     * <p>In addition to searching for the next position, this method may do something else:
     * see comments to methods {@link #getSingleBoundaryScanner(Matrix, ConnectivityType) getSingleBoundaryScanner},
     * {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType) getAllBoundariesScanner},
     * {@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType) getMainBoundariesScanner}.
     *
     * <p>This method returns <tt>true</tt> if it can find the necessary "next" position, or <tt>false</tt>
     * if there is no required position, i.e. if the matrix scanning is finished. In the second case,
     * the current position is not changed.
     *
     * <p>Note that if this scanner was not {@link #isInitialized() positioned yet}, it becomes positioned
     * if this method returns <tt>true</tt>, but stays not positioned if it returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if this method has successfully found new boundary.
     */
    public abstract boolean nextBoundary();

    /**
     * Move the current position to the next segment of the currently scanned object boundary.
     * External boundaries are scanned in clockwise order, internal boundaries in anticlockwise order
     * (if we suppose that the <i>x</i> axis is directed rightwards and the <i>y</i> axis is directed downwards).
     *
     * <p>If the current position does not correspond to an object boundary, the position will be changed
     * to some unknown position near the current one (precise behavior is not specified).
     *
     * <p>In addition to switching to the next position, this method can do something else:
     * see comments to methods
     * {@link #getSingleBoundaryScanner(Matrix, ConnectivityType) getSingleBoundaryScanner},
     * {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType) getAllBoundariesScanner},
     * {@link #getMainBoundariesScanner(Matrix, Matrix, ConnectivityType) getMainBoundariesScanner},
     * and comments to classes {@link Boundary2DSimpleMeasurer}, {@link Boundary2DProjectionMeasurer}.
     *
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public abstract void next();

    /**
     * Returns information about the movement of the current position, performed by the last call of
     * {@link #next()} method.
     *
     * <p>If that method was never called (in particular, as a part of {@link #scanBoundary(ArrayContext)}),
     * this method throws <tt>IllegalStateException</tt>. You can check this situation with help of
     * {@link #isMovedAlongBoundary()} method.
     *
     * @return the step of scanning boundary, performed by the call of {@link #next()} method.
     * @throws IllegalStateException if {@link #next()} (or {@link #scanBoundary(ArrayContext)}) method was never
     *                               called for this instance.
     * @see #isMovedAlongBoundary()
     */
    public abstract Step lastStep();

    /**
     * Returns <tt>true</tt> if the last call of {@link #next()} method has changed {@link #x()} or {@link #y()}
     * coordinate. Returns <tt>false</tt> if the last call of {@link #next()} method has changed only the
     * {@link #side() current pixel side}.
     *
     * <p>Equivalent to
     * <tt>!{@link #lastStep()}.{@link Step#isRotation() isSamePixel()}</tt>,
     * but works little faster.
     *
     * @return whether the last call of {@link #next()} method has changed current pixel coordinates.
     * @throws IllegalStateException if {@link #next()} (or {@link #scanBoundary(ArrayContext)}) method was never
     *                               called for this instance.
     */
    public abstract boolean coordinatesChanged();

    /**
     * Returns <tt>true</tt> if and only if the current position ({@link #x()}, {@link #y()}, {@link #side()})
     * is identical to the position, set by last call of {@link #nextBoundary()} or
     * {@link #goTo} method.
     * Usually it means that the current boundary has been successfully scanned.
     *
     * @return <tt>true</tt> if the current boundary scanning is finished.
     */
    public abstract boolean boundaryFinished();

    /**
     * Returns the total number of calls of {@link #next()} method since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()} method.
     *
     * <p>The result of this method is based on internal counters,
     * incremented by 1 in {@link #next()} method and cleared to 0 while object creation
     * and while every call of {@link #nextBoundary()}, {@link #goTo} or {@link #resetCounters()} methods.
     *
     * <p>Note that we always have: {@link #stepCount()} = {@link #straightStepCount()}
     * + {@link #diagonalStepCount()} + {@link #rotationStepCount()}.
     *
     * @return number of calls of {@link #next()} method since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()}.
     */
    public abstract long stepCount();

    /**
     * Returns the number of calls of {@link #next()} method since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()} method, corresponding to {@link Step#isStraight() straight steps}.
     *
     * <p>The result of this method is based on internal counters,
     * incremented by 1 in {@link #next()} method and cleared to 0 while object creation
     * and while every call of {@link #nextBoundary()}, {@link #goTo} or {@link #resetCounters()} methods.
     *
     * @return number of straight steps since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()}.
     */
    public final long straightStepCount() {
        return stepCount() - (diagonalStepCount() + rotationStepCount());
    }

    /**
     * Returns the number of calls of {@link #next()} method since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()} method, corresponding to {@link Step#isDiagonal() diagonal steps}.
     *
     * <p>The result of this method is based on internal counters,
     * incremented by 1 in {@link #next()} method and cleared to 0 while object creation
     * and while every call of {@link #nextBoundary()}, {@link #goTo} or {@link #resetCounters()} methods.
     *
     * @return number of straight steps since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()}.
     */
    public abstract long diagonalStepCount();

    /**
     * Returns the number of calls of {@link #next()} method since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()} method, corresponding to {@link Step#isRotation() rotation steps}.
     *
     * <p>The result of this method is based on internal counters,
     * incremented by 1 in {@link #next()} method and cleared to 0 while object creation
     * and while every call of {@link #nextBoundary()}, {@link #goTo} or {@link #resetCounters()} methods.
     *
     * @return number of straight steps since the last call of {@link #nextBoundary()},
     * {@link #goTo} or {@link #resetCounters()}.
     */
    public abstract long rotationStepCount();

    /**
     * Returns the <i>oriented area</i> inside the boundary, traversed by {@link #next()} method since the last
     * call of {@link #nextBoundary()}, {@link #goTo} or {@link #resetCounters()} method.
     *
     * <p>The <i>oriented area</i> is the current value of an internal counter, which is reset to 0
     * while object creation and while every call of {@link #nextBoundary()}, {@link #goTo}
     * or {@link #resetCounters()} methods,
     * and which is incremented while each {@link #next()} method in the following manner:
     * <pre>
     * switch ({@link #side() side()} {
     *     case X_MINUS:
     *         orientedArea -= {@link #x() x()} - 1;
     *         break;
     *     case X_PLUS:
     *         orientedArea += {@link #x() x()};
     *         break;
     * }
     * </pre>
     * <p>In other words, the absolute value of <i>oriented area</i> is the number of pixels inside the traversed
     * boundary, and its sign is positive when it is an object (<i>external</i> boundary) or negative when
     * it is a pore inside an object (<i>internal</i> boundary). It is the total number of pixels of the
     * <a href="Boundary2DScanner.html#completion">completion</a> of the current measured object
     * (with minus sign if it is an internal boundary, i.e. when it is the number of pixels in the "hole").
     *
     * @return the <i>oriented area</i> inside the boundary, traversed by {@link #next()} method since the last
     * call of {@link #nextBoundary()}, {@link #goTo} or {@link #resetCounters()} method
     */
    public abstract long orientedArea();

    /**
     * Returns the oriented area inside the contour line, following along the scanned boundary,
     * estimated according the specified contour line type.
     * "Oriented" means that the result is equal to the area of the figure inside this contour,
     * if the scanned boundary is an <i>external</i> one, or the same value with minus sign
     * if it is an <i>internal</i> one.
     *
     * <p>In particular, if <tt>contourLineType=={@link ContourLineType#STRICT_BOUNDARY}</tt>,
     * this method just returns the result of {@link #orientedArea()}.
     * In the case <tt>contourLineType=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>,
     * the measured area can be 0.0 &mdash; for example, for 1-pixel objects (isolated pixels) or for
     * "thin" 1-pixel "lines".
     *
     * @return the oriented area inside the scanned contour.
     */
    public double area(ContourLineType contourLineType) {
        final long orientedArea = orientedArea();
        switch (contourLineType) {
            case STRICT_BOUNDARY: {
                return orientedArea;
            }
            case PIXEL_CENTERS_POLYLINE: {
                final long straightStepCount = straightStepCount();
                return orientedArea - 0.5 * straightStepCount - 0.25 * (stepCount() - straightStepCount);
            }
            case SEGMENT_CENTERS_POLYLINE: {
                return orientedArea > 0 ? orientedArea - 0.5 : orientedArea + 0.5;
            }
            default:
                throw new AssertionError("Unsupported contourLineType=" + contourLineType);
        }
    }

    /**
     * Returns the total length of the contour, following along the scanned boundary:
     * perimeter of the measured object, "drawn" at the bit matrix, estimated according
     * the specified contour line type.
     *
     * <p>If <tt>contourLineType=={@link ContourLineType#STRICT_BOUNDARY}</tt>,
     * this method just returns the result of {@link #stepCount()}.
     *
     * <p>In the case <tt>contourLineType=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>,
     * this method returns
     * <pre>
     * {@link #straightStepCount()} + {@link Step#DIAGONAL_LENGTH
     *     Step.DIAGONAL_LENGTH} * {@link #diagonalStepCount()}.
     * </pre>
     *
     * <p>In the case <tt>contourLineType=={@link ContourLineType#SEGMENT_CENTERS_POLYLINE}</tt>,
     * this method returns
     * <pre>
     * {@link #straightStepCount()} + {@link Step#HALF_DIAGONAL_LENGTH
     *     Step.HALF_DIAGONAL_LENGTH} * ({@link #diagonalStepCount()}+{@link #rotationStepCount()}).
     * </pre>Usually it is the best approximation for the real perimeter of the object among all 3 variants.
     *
     * @return the length of the contour line, following along the scanned boundary.
     */
    public double perimeter(ContourLineType contourLineType) {
        final long stepCount = stepCount();
        switch (contourLineType) {
            case STRICT_BOUNDARY: {
                return stepCount;
            }
            case PIXEL_CENTERS_POLYLINE: {
                final long diagonalStepCount = diagonalStepCount();
                return stepCount - diagonalStepCount - rotationStepCount() + Step.DIAGONAL_LENGTH * diagonalStepCount;
            }
            case SEGMENT_CENTERS_POLYLINE: {
                final long nonStraightStepCount = diagonalStepCount() + rotationStepCount();
                return stepCount - nonStraightStepCount + Step.HALF_DIAGONAL_LENGTH * nonStraightStepCount;
            }
            default:
                throw new AssertionError("Unsupported contourLineType=" + contourLineType);
        }
    }


    /**
     * Returns <tt>true</tt> if and only if
     * <nobr><tt>{@link #side() side()} == {@link Side#X_PLUS Side.X_PLUS}</tt></nobr>.
     * Usually it means that the current position corresponds to an internal boundary:
     * see comments to {@link #getAllBoundariesScanner(Matrix, Matrix, Matrix, ConnectivityType)} method.
     *
     * @return <nobr><tt>{@link #side() side()} == {@link Side#X_PLUS Side.X_PLUS}</tt></nobr>.
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public final boolean isInternalBoundary() {
        return side() == Side.X_PLUS;
    }

    /**
     * Equivalent of {@link #scanBoundary(ArrayContext) scanBoundary(null)}.
     *
     * @return the length of scanned boundary (the number of visited pixel sides,
     * not the number of visited pixels!)
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public final long scanBoundary() {
        return scanBoundary(null);
    }

    /**
     * Scans the current boundary. This method performs the following simple loop:
     *
     * <pre>
     * do {
     * &#32;   next();
     * } while (!boundaryFinished());
     * </pre>
     *
     * <p>and returns <tt>{@link #stepCount()}</tt> (the number of performed iterations,
     * i.e. the length of the scanned boundary).
     * In addition, this method calls <tt>context.{@link ArrayContext#checkInterruption() checkInterruption()}</tt>
     * method from time to time (if <tt>context!=null</tt>) to allow interruption of scanning very long boundaries.
     * No other methods of the <tt>context</tt> are called.
     *
     * <p>Note: the number of boundary segments, returned of this method, can theoretically be
     * incorrect if the length of the boundary is greater than <tt>Long.MAX_VALUE</tt>.
     * It is a very exotic case, that can be practically realized only on a virtual matrix, containing
     * almost 2<sup>63</sup> bits, with special structure. In this case, this method will work during
     * more than 10<sup>10</sup> seconds (&gt; 300 years) on a very quick computer that can perform one
     * iteration per 1&nbsp;ns.</p>
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     * @return the length of scanned boundary (the number of visited pixel sides,
     * not the number of visited pixels!)
     * @throws IllegalStateException if this scanner was not {@link #isInitialized() positioned yet}.
     */
    public final long scanBoundary(ArrayContext context) {
        do {
            next();
//            System.out.println("   " + stepCount() + ": " + this);
            if (context != null && (stepCount() & 0xFFFF) == 0) {
                context.checkInterruption();
            }
        } while (!boundaryFinished());
        return stepCount();
    }

    /**
     * Calls <tt>context.{@link ArrayContext#updateProgress updateProgress(event)}</tt>
     * with an event, created by the following operator:
     * <nobr><tt>new ArrayContext.Event(boolean.class, {@link #currentIndexInArray()
     * currentIndexInArray()}, {@link #matrix() matrix()}.{@link Matrix#size() size()})</tt></nobr>,
     * or does nothing if <tt>context==null</tt>.
     *
     * <p>The method can be useful while sequentially scanning the matrix via a usual loop of
     * {@link #nextBoundary()} and {@link #scanBoundary(ArrayContext)} calls.
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     */
    public final void updateProgress(ArrayContext context) {
        if (context != null)
            context.updateProgress(new ArrayContext.Event(boolean.class, currentIndexInArray(), matrix.size()));
    }

    /**
     * Calls <tt>context.{@link ArrayContext#checkInterruption() checkInterruption()}</tt> or
     * does nothing if <tt>context==null</tt>.
     *
     * <p>The method can be useful while sequentially scanning the matrix via a usual loop of
     * {@link #nextBoundary()} and {@link #scanBoundary(ArrayContext)} calls.
     *
     * @param context the context of execution; can be {@code null}, then it will be ignored.
     */
    public final void checkInterruption(ArrayContext context) {
        if (context != null) {
            context.checkInterruption();
        }
    }


    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "2D scanner (" + (!isInitialized() ? "not initialized yet" :
                "x = " + x() + ", y = " + y() + ", side = " + side()
                        + (!isMovedAlongBoundary() ? "" : ", last step: " + lastStep())
                        + (nestingLevel() == 0 ? "" : ", nesting level = " + nestingLevel()))
                + (connectivityType() == ConnectivityType.STRAIGHT_ONLY ? "; 4" : "; 8") + "-connectivity)";
    }


    static abstract class AbstractBoundary2DScanner extends Boundary2DScanner {
        long x = 0, y = 0;
        private long startX = 0, startY = 0;
        long nestingLevel = 0;
        final AbstractAccessor accessor;
        final AbstractMover[] movers;
        AbstractMover mover = null; // indicator, that the object is not initialized
        AbstractMover startMover = null;
        ShiftInfo lastShiftInfo = null;
        boolean atMatrixBoundary = false;
        long stepCount = 0;
        long diagonalStepCount = 0;
        long rotationStepCount = 0;
        long orientedArea = 0;

        private AbstractBoundary2DScanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
            this.accessor = getAccessor(matrix);
            this.movers = new AbstractMover[ALL_SIDES.length];
            for (int k = 0; k < ALL_SIDES.length; k++) {
                this.movers[k] = ALL_SIDES[k].getMover(this);
            }
            for (int k = 0; k < ALL_SIDES.length; k++) {
                Side side = ALL_SIDES[k];
                this.movers[k].currentSide = side;
                this.movers[k].diagonal = new ShiftInfo(side.diagonal, movers[side.diagonal.newSide.ordinal()]);
                this.movers[k].straight = new ShiftInfo(side.straight, movers[side.straight.newSide.ordinal()]);
                this.movers[k].rotation = new ShiftInfo(side.rotation, movers[side.rotation.newSide.ordinal()]);
            }
        }

        @Override
        public final boolean isInitialized() {
            return mover != null;
        }

        @Override
        public boolean isMovedAlongBoundary() {
            return lastShiftInfo != null;
        }

        @Override
        public final long x() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            return x;
        }

        @Override
        public final long y() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            return y;
        }

        @Override
        public final Side side() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            return mover.currentSide;
        }

        @Override
        public boolean atMatrixBoundary() {
            return atMatrixBoundary;
        }

        @Override
        public long nestingLevel() {
            return isAllBoundariesScanner() ? nestingLevel : 0;
        }

        @Override
        public boolean nextBoundary() {
            return nextSingleBoundary(this);
        }

        @Override
        public void goTo(long x, long y, Side side) {
            if (x < 0 || x >= dimX) {
                throw new IndexOutOfBoundsException("Index x (" + x
                        + (x < 0 ? ") < 0" : ") >= dim(0) (" + dimX + ")"));
            }
            if (y < 0 || y >= dimY) {
                throw new IndexOutOfBoundsException("Index y (" + y
                        + (y < 0 ? ") < 0" : ") >= dim(1) (" + dimY + ")"));
            }
            Objects.requireNonNull(side, "Null side argument");
            this.x = this.startX = x;
            this.y = this.startY = y;
            this.mover = this.startMover = movers[side.ordinal()];
            resetCounters();
        }

        @Override
        public void resetCounters() {
            this.stepCount = 0;
            this.diagonalStepCount = 0;
            this.rotationStepCount = 0;
            this.orientedArea = 0;
        }

        @Override
        public final Step lastStep() {
            if (lastShiftInfo == null)
                throw new IllegalStateException("The boundary scanner did not performed any steps yet");
            return lastShiftInfo.shift;
        }

        @Override
        public final boolean coordinatesChanged() {
            if (lastShiftInfo == null)
                throw new IllegalStateException("The boundary scanner did not performed any steps yet");
            return lastShiftInfo.coordinatesChanged;
        }

        @Override
        public boolean boundaryFinished() {
            return x == startX && y == startY && mover == startMover;
        }

        @Override
        public long stepCount() {
            return stepCount;
        }

        @Override
        public long diagonalStepCount() {
            return diagonalStepCount;
        }

        @Override
        public long rotationStepCount() {
            return rotationStepCount;
        }

        @Override
        public long orientedArea() {
            return orientedArea;
        }

        abstract AbstractAccessor getAccessor(Matrix<? extends PFixedArray> matrix);

        abstract AbstractMover getMoverXM();

        abstract AbstractMover getMoverYM();

        abstract AbstractMover getMoverXP();

        abstract AbstractMover getMoverYP();

        static final class ShiftInfo {
            final Step shift;
            final boolean coordinatesChanged;
            final AbstractMover newMover;

            ShiftInfo(Step shift, AbstractMover newMover) {
                this.shift = shift;
                this.coordinatesChanged = !shift.samePixel;
                this.newMover = newMover;
            }
        }

        abstract static class AbstractAccessor {
            final PFixedArray array;
            final UpdatablePFixedArray updatableArray;

            AbstractAccessor(PFixedArray array) {
                this.array = array;
                this.updatableArray = array instanceof UpdatablePFixedArray ? (UpdatablePFixedArray) array : null;
            }

            abstract boolean get();

            abstract void set();

            abstract boolean getNext();

            abstract void setNext();
        }

        abstract class AbstractMover {
            final Matrix<? extends BitArray> matrix;
            final BitArray array;
            final long maxX, maxY;
            ShiftInfo diagonal, straight, rotation;
            Side currentSide;

            AbstractMover() {
                this.matrix = AbstractBoundary2DScanner.this.matrix;
                this.array = matrix.array();
                this.maxX = matrix.dimX() - 1;
                this.maxY = matrix.dimY() - 1;
            }

            abstract boolean atMatrixBound();

            abstract boolean straight();

            abstract boolean rightAfterStraight();

            abstract void straightBackLeft();

            abstract boolean diag();

            abstract boolean leftAfterDiag();

            abstract void straightBack();

            abstract boolean getHorizontalBracket(AbstractAccessor accessor);

            abstract void setHorizontalBracket(AbstractAccessor accessor);

            final void next4() {
                if (!atMatrixBound()) {
                    // For example, we at the right side (MoverXP)...
                    if (!straight()) {
                        // attempt to move upward (y++)
                        //     , .   ("," is the checked zero pixel)
                        //     X .
                        straightBack();
                        // return back: we need to rotate and go to the top side
                    } else if (rightAfterStraight()) {
                        // attempt to move rightward (x++) after previous upward (y++)
                        //     X x   ("x" is the checked unit pixel)
                        //     X .
                        // o'k, it is a diagonal step (x++ and y++)
                        lastShiftInfo = mover.diagonal;
                        mover = lastShiftInfo.newMover;
                        diagonalStepCount++;
                        return;
                    } else {
                        // return leftward (back) (x-- after previous x++)
                        //     X .
                        //     X .
                        straightBackLeft();
                        // o'k, it is the simplest straight step (y++)
                        lastShiftInfo = mover.straight;
                        return;
                    }
                }
                lastShiftInfo = mover.rotation;
                mover = lastShiftInfo.newMover;
                rotationStepCount++;
            }

            // This method is overridden in some subclasses for maximal performance
            void next8() {
                if (!atMatrixBound()) {
                    // For example, we at the right side (MoverXP)...
                    if (diag()) {
                        // attempt to move upward and rightward (y++, x++)
                        //     X x   ("x" is the checked unit pixel)
                        //     X .
                        // o'k, it is a diagonal step
                        lastShiftInfo = mover.diagonal;
                        mover = lastShiftInfo.newMover;
                        diagonalStepCount++;
                        return;
                    }
                    if (leftAfterDiag()) {
                        // attempt to move upward (y++ only): x-- after previous x++
                        //     x .   ("x" is the checked unit pixel)
                        //     X .
                        // o'k, it is the simplest straight step (y++)
                        lastShiftInfo = mover.straight;
                        return;
                    }
                    //     . .
                    //     X .
                    // return back: we need to rotate and go to the top side
                    straightBack();
                }
                lastShiftInfo = mover.rotation;
                mover = lastShiftInfo.newMover;
                rotationStepCount++;
            }
        }

        abstract class AbstractMoverXM extends AbstractMover {
            final boolean atMatrixBound() {
                return y == 0;
            }

            final boolean getHorizontalBracket(AbstractAccessor accessor) {
                return accessor.get();
            }

            final void setHorizontalBracket(AbstractAccessor accessor) {
                accessor.set();
            }
        }

        abstract class AbstractMoverYM extends AbstractMover {
            final boolean atMatrixBound() {
                return x == maxX;
            }

            final boolean getHorizontalBracket(AbstractAccessor accessor) {
                return false;
            }

            final void setHorizontalBracket(AbstractAccessor accessor) {
            }
        }

        abstract class AbstractMoverXP extends AbstractMover {
            final boolean atMatrixBound() {
                return y == maxY;
            }

            final boolean getHorizontalBracket(AbstractAccessor accessor) {
                return x < maxX && accessor.getNext();
            }

            final void setHorizontalBracket(AbstractAccessor accessor) {
                if (x < maxX)
                    accessor.setNext();
            }
        }

        abstract class AbstractMoverYP extends AbstractMover {
            final boolean atMatrixBound() {
                return x == 0;
            }

            final boolean getHorizontalBracket(AbstractAccessor accessor) {
                return false;
            }

            final void setHorizontalBracket(AbstractAccessor accessor) {
            }
        }
    }

    static abstract class SingleBoundary2DScanner extends AbstractBoundary2DScanner {
        private long index = 0;
        private long startIndex = 0;

        SingleBoundary2DScanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return true;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return false;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return false;
        }

        @Override
        public long currentIndexInArray() {
            return index;
        }

        @Override
        public void goTo(long x, long y, Side side) {
            super.goTo(x, y, side);
            this.index = this.startIndex = y * dimX + x;
        }

        @Override
        public boolean get() {
            return array.getInt(index) != 0;
        }

        @Override
        public boolean boundaryFinished() {
            return index == startIndex && mover == startMover;
        }

        @Override
        final AbstractAccessor getAccessor(Matrix<? extends PFixedArray> matrix) {
            return new Accessor(matrix);
        }

        //[[Repeat() XM ==> YM,,XP,,YP]]
        @Override
        final AbstractMover getMoverXM() {
            return new MoverXM();
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        @Override
        final AbstractMover getMoverYM() {
            return new MoverYM();
        }

        @Override
        final AbstractMover getMoverXP() {
            return new MoverXP();
        }

        @Override
        final AbstractMover getMoverYP() {
            return new MoverYP();
        }

        //[[Repeat.AutoGeneratedEnd]]

        class Accessor extends AbstractAccessor {
            Accessor(Matrix<? extends PFixedArray> matrix) {
                super(matrix.array());
            }

            @Override
            boolean get() {
                return array.getLong(index) != 0;
            }

            @Override
            void set() {
                updatableArray.setLong(index, DEBUG_NESTING_LEVEL && nestingLevel > 0 ? nestingLevel : 1);
            }

            @Override
            boolean getNext() {
                return array.getLong(index + 1) != 0;
            }

            @Override
            void setNext() {
                updatableArray.setLong(index + 1, DEBUG_NESTING_LEVEL && nestingLevel > 0 ? nestingLevel : 1);
            }
        }

        class MoverXM extends AbstractMoverXM {
            boolean straight() {
                y--;
                index -= dimX;
                return array.getBit(index);
            }

            boolean rightAfterStraight() {
                x--;
                index--;
                return x >= 0 && array.getBit(index);
            }

            void straightBackLeft() {
                x++;
                index++;
            }

            boolean diag() {
                y--;
                x--;
                index -= dimX + 1;
                return x >= 0 && array.getBit(index);
            }

            boolean leftAfterDiag() {
                x++;
                index++;
                return array.getBit(index);
            }

            void straightBack() {
                y++;
                index += dimX;
            }
        }

        class MoverYM extends AbstractMoverYM {
            boolean straight() {
                x++;
                index++;
                return array.getBit(index);
            }

            boolean rightAfterStraight() {
                y--;
                index -= dimX;
                return y >= 0 && array.getBit(index);
            }

            void straightBackLeft() {
                y++;
                index += dimX;
            }

            boolean diag() {
                y--;
                x++;
                index -= maxX;
                return y >= 0 && array.getBit(index);
            }

            boolean leftAfterDiag() {
                y++;
                index += dimX;
                return array.getBit(index);
            }

            void straightBack() {
                x--;
                index--;
            }
        }

        class MoverXP extends AbstractMoverXP {
            boolean straight() {
                y++;
                index += dimX;
                return array.getBit(index);
            }

            boolean rightAfterStraight() {
                x++;
                index++;
                return x < dimX && array.getBit(index);
            }

            void straightBackLeft() {
                x--;
                index--;
            }

            boolean diag() {
                y++;
                x++;
                index += dimX + 1;
                return x < dimX && array.getBit(index);
            }

            boolean leftAfterDiag() {
                x--;
                index--;
                return array.getBit(index);
            }

            void straightBack() {
                y--;
                index -= dimX;
            }
        }

        class MoverYP extends AbstractMoverYP {
            boolean straight() {
                x--;
                index--;
                return array.getBit(index);
            }

            boolean rightAfterStraight() {
                y++;
                index += dimX;
                return y < dimY && array.getBit(index);
            }

            void straightBackLeft() {
                y--;
                index -= dimX;
            }

            boolean diag() {
                y++;
                x--;
                index += maxX;
                return y < dimY && array.getBit(index);
            }

            boolean leftAfterDiag() {
                y--;
                index -= dimX;
                return array.getBit(index);
            }

            void straightBack() {
                x++;
                index++;
            }
        }
    }

    static class SingleBoundary2D4Scanner extends SingleBoundary2DScanner {
        SingleBoundary2D4Scanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
        }

        @Override
        public ConnectivityType connectivityType() {
            return ConnectivityType.STRAIGHT_ONLY;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next4();
            mover.currentSide.correctState(this);
        }
    }

    static class SingleBoundary2D8Scanner extends SingleBoundary2DScanner {
        SingleBoundary2D8Scanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
        }

        @Override
        public ConnectivityType connectivityType() {
            return ConnectivityType.STRAIGHT_AND_DIAGONAL;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next8();
            mover.currentSide.correctState(this);
        }
    }

    static abstract class DirectSingleBoundary2DScanner extends AbstractBoundary2DScanner {
        private final long[] ja;
        private final long jaOfs;
        private long jaMask;
        private int jaDisp;
        private long index; // warning! used only if dimX % 64 != 0

        DirectSingleBoundary2DScanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
            if (!SimpleMemoryModel.isSimpleArray(array))
                throw new AssertionError("Illegal usage of " + getClass() + ": not simple array");
            DataBitBuffer buf = array.buffer(DataBuffer.AccessMode.READ, 16);
            if (!buf.isDirect())
                throw new AssertionError("Illegal usage of " + getClass() + ": not direct buffer");
            buf.map(0);
            this.ja = buf.data();
            this.jaOfs = buf.from();
            this.jaDisp = (int) (jaOfs >>> 6);
            this.jaMask = 1L << (jaOfs & 63);
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return true;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return false;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return false;
        }

        @Override
        public long currentIndexInArray() {
            return y * dimX + x;
        }

        @Override
        public void goTo(long x, long y, Side side) {
            super.goTo(x, y, side);
            this.index = y * dimX + x;
            this.jaDisp = (int) ((jaOfs + index) >>> 6);
            this.jaMask = 1L << ((jaOfs + index) & 63);
        }

        @Override
        public boolean get() {
            return (ja[jaDisp] & jaMask) != 0;
        }

        public String toString() {
            return "direct " + super.toString();
        }

        @Override
        final AbstractAccessor getAccessor(Matrix<? extends PFixedArray> matrix) {
            if (this.ja != null) {
                PFixedArray array = matrix.array();
                if (array instanceof BitArray && SimpleMemoryModel.isSimpleArray(array)) {
                    DataBitBuffer buf = ((BitArray) array).buffer(DataBuffer.AccessMode.READ, 16);
                    if (buf.isDirect()) {
                        buf.map(0);
                        long[] mja = buf.data();
                        long mjaOfs = buf.from();
                        if (mja != null && mjaOfs == this.jaOfs) {
                            return new DirectAccessor((BitArray) array);
                        }
                    }
                }
            }
            return new Accessor(matrix);
        }

        //[[Repeat() XM ==> YM,,XP,,YP]]
        @Override
        final AbstractMover getMoverXM() {
            return (dimX & 63) == 0 ? new Direct64MoverXM() : new DirectMoverXM();
        }

        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        @Override
        final AbstractMover getMoverYM() {
            return (dimX & 63) == 0 ? new Direct64MoverYM() : new DirectMoverYM();
        }

        @Override
        final AbstractMover getMoverXP() {
            return (dimX & 63) == 0 ? new Direct64MoverXP() : new DirectMoverXP();
        }

        @Override
        final AbstractMover getMoverYP() {
            return (dimX & 63) == 0 ? new Direct64MoverYP() : new DirectMoverYP();
        }

        //[[Repeat.AutoGeneratedEnd]]


        class Accessor extends AbstractAccessor {
            Accessor(Matrix<? extends PFixedArray> matrix) {
                super(matrix.array());
            }

            @Override
            boolean get() {
                return array.getInt(y * dimX + x) != 0;
            }

            @Override
            void set() {
                updatableArray.setLong(y * dimX + x, DEBUG_NESTING_LEVEL && nestingLevel > 0 ? nestingLevel : 1);
            }

            @Override
            boolean getNext() {
                return array.getLong(y * dimX + x + 1) != 0;
            }

            @Override
            void setNext() {
                updatableArray.setLong(y * dimX + x + 1, DEBUG_NESTING_LEVEL && nestingLevel > 0 ? nestingLevel : 1);
            }
        }

        class DirectAccessor extends AbstractAccessor {
            final long[] ja;

            DirectAccessor(BitArray array) {
                super(array);
                if (!SimpleMemoryModel.isSimpleArray(array))
                    throw new AssertionError("Illegal usage of " + getClass() + ": not simple array");
                DataBitBuffer buf = array.buffer(DataBuffer.AccessMode.READ, 16);
                if (!buf.isDirect())
                    throw new AssertionError("Illegal usage of " + getClass() + ": not direct buffer");
                buf.map(0);
                this.ja = buf.data();
                if (buf.from() != DirectSingleBoundary2DScanner.this.jaOfs)
                    throw new AssertionError("Illegal usage of " + getClass() + ": different offsets");
            }

            @Override
            boolean get() {
                return (ja[jaDisp] & jaMask) != 0;
            }

            @Override
            void set() {
                ja[jaDisp] |= jaMask;
            }

            @Override
            boolean getNext() {
                int disp = jaDisp;
                long mask = jaMask << 1;
                if (mask == 0) {
                    mask = 1L;
                    disp++;
                }
                return (ja[disp] & mask) != 0;
            }

            @Override
            void setNext() {
                int disp = jaDisp;
                long mask = jaMask << 1;
                if (mask == 0) {
                    mask = 1L;
                    disp++;
                }
                ja[disp] |= mask;
            }
        }

        class DirectMoverXM extends AbstractMoverXM {
            boolean straight() {
                y--;
                index -= dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return x >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
            }

            boolean diag() {
                y--;
                x--;
                index -= dimX + 1;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return x >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                y++;
                index += dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
            }
        }

        class DirectMoverYM extends AbstractMoverYM {
            boolean straight() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                y--;
                index -= dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return y >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                y++;
                index += dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
            }

            boolean diag() {
                y--;
                x++;
                index -= maxX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return y >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                y++;
                index += dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
            }
        }

        class DirectMoverXP extends AbstractMoverXP {
            boolean straight() {
                y++;
                index += dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return x < dimX && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
            }

            boolean diag() {
                y++;
                x++;
                index += dimX + 1;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return x < dimX && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                y--;
                index -= dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
            }
        }

        class DirectMoverYP extends AbstractMoverYP {
            boolean straight() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                y++;
                index += dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return y < dimY && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                y--;
                index -= dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
            }

            boolean diag() {
                y++;
                x--;
                index += maxX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return y < dimY && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                y--;
                index -= dimX;
                jaDisp = (int) ((jaOfs + index) >>> 6);
                jaMask = 1L << ((jaOfs + index) & 63);
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
            }
        }

        class Direct64MoverXM extends AbstractMoverXM {
            final int jaStep = (int) (dimX >>> 6);

            boolean straight() {
                y--;
                jaDisp -= jaStep;
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return x >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
            }

            boolean diag() {
                y--;
                x--;
                jaDisp -= jaStep;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return x >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                x++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                y++;
                jaDisp += jaStep;
            }

            void next8() {
                if (y != 0) {
                    y--;
                    x--;
                    jaDisp -= jaStep;
                    if ((jaMask >>>= 1) == 0) {
                        jaMask = Long.MIN_VALUE;
                        jaDisp--;
                    }
                    if (x >= 0 && (ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.diagonal;
                        mover = lastShiftInfo.newMover;
                        diagonalStepCount++;
                        return;
                    }
                    x++;
                    if ((jaMask <<= 1) == 0) {
                        jaMask = 1L;
                        jaDisp++;
                    }
                    if ((ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.straight;
                        return;
                    }
                    y++;
                    jaDisp += jaStep;
                }
                lastShiftInfo = mover.rotation;
                mover = lastShiftInfo.newMover;
                rotationStepCount++;
            }
        }

        class Direct64MoverYM extends AbstractMoverYM {
            final int jaStep = (int) (dimX >>> 6);

            boolean straight() {
                x++;
                index++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                y--;
                jaDisp -= jaStep;
                return y >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                y++;
                jaDisp += jaStep;
            }

            boolean diag() {
                y--;
                x++;
                jaDisp -= jaStep;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return y >= 0 && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                y++;
                jaDisp += jaStep;
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                x--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
            }

            void next8() {
                if (x != maxX) {
                    y--;
                    x++;
                    jaDisp -= jaStep;
                    if ((jaMask <<= 1) == 0) {
                        jaMask = 1L;
                        jaDisp++;
                    }
                    if (y >= 0 && (ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.diagonal;
                        mover = lastShiftInfo.newMover;
                        diagonalStepCount++;
                        return;
                    }
                    y++;
                    jaDisp += jaStep;
                    if ((ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.straight;
                        return;
                    }
                    x--;
                    if ((jaMask >>>= 1) == 0) {
                        jaMask = Long.MIN_VALUE;
                        jaDisp--;
                    }
                }
                lastShiftInfo = mover.rotation;
                mover = lastShiftInfo.newMover;
                rotationStepCount++;
            }
        }

        class Direct64MoverXP extends AbstractMoverXP {
            final int jaStep = (int) (dimX >>> 6);

            boolean straight() {
                y++;
                jaDisp += jaStep;
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                x++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return x < dimX && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
            }

            boolean diag() {
                y++;
                x++;
                jaDisp += jaStep;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
                return x < dimX && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                x--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                y--;
                jaDisp -= jaStep;
            }

            void next8() {
                if (y != maxY) {
                    y++;
                    x++;
                    jaDisp += jaStep;
                    if ((jaMask <<= 1) == 0) {
                        jaMask = 1L;
                        jaDisp++;
                    }
                    if (x < dimX && (ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.diagonal;
                        mover = lastShiftInfo.newMover;
                        diagonalStepCount++;
                        return;
                    }
                    x--;
                    if ((jaMask >>>= 1) == 0) {
                        jaMask = Long.MIN_VALUE;
                        jaDisp--;
                    }
                    if ((ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.straight;
                        return;
                    }
                    y--;
                    jaDisp -= jaStep;
                }
                lastShiftInfo = mover.rotation;
                mover = lastShiftInfo.newMover;
                rotationStepCount++;
            }
        }

        class Direct64MoverYP extends AbstractMoverYP {
            final int jaStep = (int) (dimX >>> 6);

            boolean straight() {
                x--;
                index--;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return (ja[jaDisp] & jaMask) != 0;
            }

            boolean rightAfterStraight() {
                y++;
                jaDisp += jaStep;
                return y < dimY && (ja[jaDisp] & jaMask) != 0;
            }

            void straightBackLeft() {
                y--;
                jaDisp -= jaStep;
            }

            boolean diag() {
                y++;
                x--;
                jaDisp += jaStep;
                if ((jaMask >>>= 1) == 0) {
                    jaMask = Long.MIN_VALUE;
                    jaDisp--;
                }
                return y < dimY && (ja[jaDisp] & jaMask) != 0;
            }

            boolean leftAfterDiag() {
                y--;
                jaDisp -= jaStep;
                return (ja[jaDisp] & jaMask) != 0;
            }

            void straightBack() {
                x++;
                if ((jaMask <<= 1) == 0) {
                    jaMask = 1L;
                    jaDisp++;
                }
            }

            void next8() {
                if (x != 0) {
                    y++;
                    x--;
                    jaDisp += jaStep;
                    if ((jaMask >>>= 1) == 0) {
                        jaMask = Long.MIN_VALUE;
                        jaDisp--;
                    }
                    if (y < dimY && (ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.diagonal;
                        mover = lastShiftInfo.newMover;
                        diagonalStepCount++;
                        return;
                    }
                    y--;
                    jaDisp -= jaStep;
                    if ((ja[jaDisp] & jaMask) != 0) {
                        lastShiftInfo = mover.straight;
                        return;
                    }
                    x++;
                    if ((jaMask <<= 1) == 0) {
                        jaMask = 1L;
                        jaDisp++;
                    }
                }
                lastShiftInfo = mover.rotation;
                mover = lastShiftInfo.newMover;
                rotationStepCount++;
            }
        }
    }

    static class DirectSingleBoundary2D4Scanner extends DirectSingleBoundary2DScanner {
        DirectSingleBoundary2D4Scanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
        }

        @Override
        public ConnectivityType connectivityType() {
            return ConnectivityType.STRAIGHT_ONLY;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next4();
            mover.currentSide.correctState(this);
        }
    }

    static class DirectSingleBoundary2D8Scanner extends DirectSingleBoundary2DScanner {
        DirectSingleBoundary2D8Scanner(Matrix<? extends BitArray> matrix) {
            super(matrix);
        }

        @Override
        public ConnectivityType connectivityType() {
            return ConnectivityType.STRAIGHT_AND_DIAGONAL;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next8();
            mover.currentSide.correctState(this);
        }
    }

    //[[Repeat() SingleBoundary2D4Scanner ==> SingleBoundary2D8Scanner,,
    //                                        DirectSingleBoundary2D4Scanner,,DirectSingleBoundary2D8Scanner;;
    //           AllBoundaries2D4Scanner  ==> AllBoundaries2D8Scanner,,
    //                                        DirectAllBoundaries2D4Scanner,,DirectAllBoundaries2D8Scanner;;
    //           next4                    ==> next8,,next4,,next8 ]]
    static class AllBoundaries2D4Scanner extends SingleBoundary2D4Scanner {
        private final AbstractAccessor bufferAccessor1, bufferAccessor2;
        private AbstractAccessor bufferAccessor = null;

        private AllBoundaries2D4Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer1,
                Matrix<? extends UpdatablePFixedArray> buffer2) {
            super(matrix);
            Objects.requireNonNull(buffer1, "Null buffer1 argument");
            Objects.requireNonNull(buffer2, "Null buffer2 argument");
            if (!buffer1.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer1 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer1);
            }
            if (!buffer2.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer2 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer2);
            }
            this.bufferAccessor1 = getAccessor(buffer1);
            this.bufferAccessor2 = getAccessor(buffer2);
            this.bufferAccessor = this.bufferAccessor1;
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return true;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return false;
        }

        @Override
        public boolean nextBoundary() {
            AbstractAccessor result = nextAnyBoundary(this, bufferAccessor1, bufferAccessor2);
            if (result == null) {
                return false;
            }
            this.bufferAccessor = result;
            return true;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next4();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "all boundaries " + super.toString();
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class AllBoundaries2D8Scanner extends SingleBoundary2D8Scanner {
        private final AbstractAccessor bufferAccessor1, bufferAccessor2;
        private AbstractAccessor bufferAccessor = null;

        private AllBoundaries2D8Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer1,
                Matrix<? extends UpdatablePFixedArray> buffer2) {
            super(matrix);
            Objects.requireNonNull(buffer1, "Null buffer1 argument");
            Objects.requireNonNull(buffer2, "Null buffer2 argument");
            if (!buffer1.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer1 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer1);
            }
            if (!buffer2.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer2 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer2);
            }
            this.bufferAccessor1 = getAccessor(buffer1);
            this.bufferAccessor2 = getAccessor(buffer2);
            this.bufferAccessor = this.bufferAccessor1;
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return true;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return false;
        }

        @Override
        public boolean nextBoundary() {
            AbstractAccessor result = nextAnyBoundary(this, bufferAccessor1, bufferAccessor2);
            if (result == null) {
                return false;
            }
            this.bufferAccessor = result;
            return true;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next8();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "all boundaries " + super.toString();
        }
    }

    static class DirectAllBoundaries2D4Scanner extends DirectSingleBoundary2D4Scanner {
        private final AbstractAccessor bufferAccessor1, bufferAccessor2;
        private AbstractAccessor bufferAccessor = null;

        private DirectAllBoundaries2D4Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer1,
                Matrix<? extends UpdatablePFixedArray> buffer2) {
            super(matrix);
            Objects.requireNonNull(buffer1, "Null buffer1 argument");
            Objects.requireNonNull(buffer2, "Null buffer2 argument");
            if (!buffer1.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer1 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer1);
            }
            if (!buffer2.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer2 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer2);
            }
            this.bufferAccessor1 = getAccessor(buffer1);
            this.bufferAccessor2 = getAccessor(buffer2);
            this.bufferAccessor = this.bufferAccessor1;
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return true;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return false;
        }

        @Override
        public boolean nextBoundary() {
            AbstractAccessor result = nextAnyBoundary(this, bufferAccessor1, bufferAccessor2);
            if (result == null) {
                return false;
            }
            this.bufferAccessor = result;
            return true;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next4();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "all boundaries " + super.toString();
        }
    }

    static class DirectAllBoundaries2D8Scanner extends DirectSingleBoundary2D8Scanner {
        private final AbstractAccessor bufferAccessor1, bufferAccessor2;
        private AbstractAccessor bufferAccessor = null;

        private DirectAllBoundaries2D8Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer1,
                Matrix<? extends UpdatablePFixedArray> buffer2) {
            super(matrix);
            Objects.requireNonNull(buffer1, "Null buffer1 argument");
            Objects.requireNonNull(buffer2, "Null buffer2 argument");
            if (!buffer1.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer1 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer1);
            }
            if (!buffer2.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer2 dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer2);
            }
            this.bufferAccessor1 = getAccessor(buffer1);
            this.bufferAccessor2 = getAccessor(buffer2);
            this.bufferAccessor = this.bufferAccessor1;
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return true;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return false;
        }

        @Override
        public boolean nextBoundary() {
            AbstractAccessor result = nextAnyBoundary(this, bufferAccessor1, bufferAccessor2);
            if (result == null) {
                return false;
            }
            this.bufferAccessor = result;
            return true;
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next8();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "all boundaries " + super.toString();
        }
    }

    //[[Repeat.AutoGeneratedEnd]]

    //[[Repeat() SingleBoundary2D4Scanner ==> SingleBoundary2D8Scanner,,
    //                                        DirectSingleBoundary2D4Scanner,,DirectSingleBoundary2D8Scanner;;
    //           MainBoundaries2D4Scanner ==> MainBoundaries2D8Scanner,,
    //                                        DirectMainBoundaries2D4Scanner,,DirectMainBoundaries2D8Scanner;;
    //           next4                    ==> next8,,next4,,next8 ]]
    static class MainBoundaries2D4Scanner extends SingleBoundary2D4Scanner {
        private final AbstractAccessor bufferAccessor;
        private final UpdatablePFixedArray bufferArray;

        private MainBoundaries2D4Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer) {
            super(matrix);
            Objects.requireNonNull(buffer, "Null buffer argument");
            if (!buffer.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer);
            }
            this.bufferAccessor = getAccessor(buffer);
            this.bufferArray = buffer.array();
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return false;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return true;
        }

        @Override
        public boolean nextBoundary() {
            return nextMainBoundary(this, bufferArray);
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next4();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "main boundaries " + super.toString();
        }
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    static class MainBoundaries2D8Scanner extends SingleBoundary2D8Scanner {
        private final AbstractAccessor bufferAccessor;
        private final UpdatablePFixedArray bufferArray;

        private MainBoundaries2D8Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer) {
            super(matrix);
            Objects.requireNonNull(buffer, "Null buffer argument");
            if (!buffer.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer);
            }
            this.bufferAccessor = getAccessor(buffer);
            this.bufferArray = buffer.array();
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return false;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return true;
        }

        @Override
        public boolean nextBoundary() {
            return nextMainBoundary(this, bufferArray);
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next8();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "main boundaries " + super.toString();
        }
    }

    static class DirectMainBoundaries2D4Scanner extends DirectSingleBoundary2D4Scanner {
        private final AbstractAccessor bufferAccessor;
        private final UpdatablePFixedArray bufferArray;

        private DirectMainBoundaries2D4Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer) {
            super(matrix);
            Objects.requireNonNull(buffer, "Null buffer argument");
            if (!buffer.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer);
            }
            this.bufferAccessor = getAccessor(buffer);
            this.bufferArray = buffer.array();
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return false;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return true;
        }

        @Override
        public boolean nextBoundary() {
            return nextMainBoundary(this, bufferArray);
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next4();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "main boundaries " + super.toString();
        }
    }

    static class DirectMainBoundaries2D8Scanner extends DirectSingleBoundary2D8Scanner {
        private final AbstractAccessor bufferAccessor;
        private final UpdatablePFixedArray bufferArray;

        private DirectMainBoundaries2D8Scanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer) {
            super(matrix);
            Objects.requireNonNull(buffer, "Null buffer argument");
            if (!buffer.dimEquals(matrix)) {
                throw new SizeMismatchException("matrix and buffer dimensions mismatch: matrix is "
                        + matrix + ", buffer is " + buffer);
            }
            this.bufferAccessor = getAccessor(buffer);
            this.bufferArray = buffer.array();
        }

        @Override
        public boolean isSingleBoundaryScanner() {
            return false;
        }

        @Override
        public boolean isAllBoundariesScanner() {
            return false;
        }

        @Override
        public boolean isMainBoundariesScanner() {
            return true;
        }

        @Override
        public boolean nextBoundary() {
            return nextMainBoundary(this, bufferArray);
        }

        @Override
        public void next() {
            if (mover == null)
                throw new IllegalStateException("The boundary scanner is not positioned yet");
            mover.next8();
            mover.setHorizontalBracket(bufferAccessor);
            mover.currentSide.correctState(this);
        }

        public String toString() {
            return "main boundaries " + super.toString();
        }
    }

    //[[Repeat.AutoGeneratedEnd]]

    static boolean nextSingleBoundary(AbstractBoundary2DScanner scanner) {
        if (scanner.arrayLength == 0) {
            return false;
        }
        final long index = scanner.currentIndexInArray(); // y * dimX + x
        // searching for the next inter-pixel position
        if (!scanner.get()) {
            // searching for nearest 1 after 0 (current)
            return goToNextUnitBit(scanner, index + 1);
            // note: here we may skip some fully empty (zero) lines
        } else {
            if (scanner.mover == null) {
                // special case: start scanning from a unit bit at (0,0)
                assert scanner.x == 0 && scanner.y == 0;
                scanner.goTo(0, 0, Side.X_MINUS);
                return true;
            }
            // searching for nearest 0 after 1 (current)
            final long x = scanner.x();
            long i = scanner.array.indexOf(index + 1, index + scanner.dimX - x, false);
            if (i == -1) {
                // all bits sinse index+1 until line end (dimX) are zero
                i = scanner.dimX - x - 1;
            } else {
                i -= index + 1;
            }
            // now x+i+1 and index+i+1 correspond to nearest 0 in this line or dimX if this line has no more 0
            if (i == 0) {
                // bit #(index+1) is 0 or index+1 corresponds to dimX,
                // so the index corresponds to the last unit bit in a series
                if (scanner.side() != Side.X_PLUS) {
                    scanner.goTo(x, scanner.y(), Side.X_PLUS);
                    return true;
                } else {
                    return goToNextUnitBit(scanner, index + 1);
                }
            }
            scanner.goTo(x + i, scanner.y(), Side.X_PLUS); // (x+1)+i is the index of the next 0 or, maybe, dimX
            return true;
        }
    }

    static AbstractBoundary2DScanner.AbstractAccessor nextAnyBoundary(
            AbstractBoundary2DScanner scanner,
            AbstractBoundary2DScanner.AbstractAccessor bufferAccessor1,
            AbstractBoundary2DScanner.AbstractAccessor bufferAccessor2) {
        boundariesLoop:
        for (; ; ) {
            if (!nextSingleBoundary(scanner)) {
                return null;
            }
            boolean bracket1 = scanner.mover.getHorizontalBracket(bufferAccessor1); // usually external boundary
            boolean bracket2 = scanner.mover.getHorizontalBracket(bufferAccessor2); // usually internal boundary
            if (scanner.x == scanner.dimX - 1 && scanner.mover.currentSide == Side.X_PLUS) {
                assert !bracket1 && !bracket2; // special case: right bracket never can be set here (no space for it)
                scanner.nestingLevel = 0; // of course, no nesting possible here
                continue; // we should not try to scan from here: we don't know whether we already were here
            }
            if (bracket1 || bracket2) { // already scanned external / internal boundary
                switch (scanner.mover.currentSide) {
                    case X_MINUS: {
                        if (bracket1) {
                            scanner.nestingLevel++;
                        } else {
                            scanner.nestingLevel--;
                        }
                        continue boundariesLoop;
                    }
                    case X_PLUS: {
                        if (bracket1) {
                            scanner.nestingLevel--;
                        } else {
                            scanner.nestingLevel++;
                        }
                        continue boundariesLoop;
                    }
                    default: {
                        throw new AssertionError("getHorizontalBracket must be false in "
                                + scanner.mover.currentSide);
                    }
                }
            } else { // first time at this boundary
                scanner.nestingLevel++;
                switch (scanner.mover.currentSide) {
                    case X_MINUS: {
                        // external boundary
                        return bufferAccessor1;
                    }
                    case X_PLUS: {
                        // internal boundary
                        return bufferAccessor2;
                    }
                }
            }
        }
    }

    static boolean nextMainBoundary(AbstractBoundary2DScanner scanner, UpdatablePFixedArray bufferArray) {
        if (scanner.arrayLength == 0) {
            return false;
        }
        long index = scanner.currentIndexInArray(); // y * dimX + x
        boolean atLeftBoundary = bufferArray.getInt(index) != 0;
        if (!atLeftBoundary) {
            return nextSingleBoundary(scanner);
        }
        long x = scanner.x;
        do {
            // not the index corresponds to a left bracket (boundary)
            long i = bufferArray.indexOf(index + 1, index + scanner.dimX - x, 1);
            if (i == -1) {
                i = scanner.dimX - x - 1;
            } else {
                bufferArray.setInt(i, 0);
                i -= index + 1;
            }
            // now x+i+1 and index+i+1 correspond to the next bracket in this line or dimX if it has no more brackets
            bufferArray.fill(index + 1, i, 1);
            index += i;

            if (index >= scanner.arrayLength) {
                return false;
            }
            // now index+1 correspond to the next bracket in this line (or dimX if this line has no more brackets)
            index = scanner.array.indexOf(index + 1, scanner.arrayLength, true);
            if (index == -1) {
                return false;
            }
            x = index % scanner.dimX;
            atLeftBoundary = bufferArray.getInt(index) != 0;
        } while (atLeftBoundary);
        scanner.goTo(x, index / scanner.dimX, Side.X_MINUS);
        return true;
    }

    private static boolean goToNextUnitBit(Boundary2DScanner scanner, long index) {
        // index may be equal to arrayLength!
        // it is possible to search bit 1 through all raw underlying array
        long i = scanner.array.indexOf(index, scanner.arrayLength, true);
        if (i == -1) {
            return false;
        } else {
            scanner.goTo(i % scanner.dimX, i / scanner.dimX, Side.X_MINUS);
            return true;
        }
    }

    private static boolean isDirectBitArray(BitArray array) {
        if (SimpleMemoryModel.isSimpleArray(array)) {
            // We MUST check SimpleMemoryModel: here we can be sure, that DataBitBuffer
            // returns true reference to the built-in packed Java array.
            // Possible alternative is BitArray.jaBits (since AlgART 1.4.10),
            // but this method works better for subarray with zero-offset
            // (here we do not require that the length must be correct).
            DataBitBuffer buf = array.buffer(DataBuffer.AccessMode.READ, 16);
            return buf.isDirect(); // possibly not for immutable or copy-on-next-write arrays
        } else {
            return false;
        }
    }
}
