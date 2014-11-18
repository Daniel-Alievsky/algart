/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Contour line style for 2-dimensional object boundaries, traced by {@link Boundary2DScanner}.
 * Used by some classes that measure parameters of 2-dimensional objects on the base of their boundaries,
 * like the length of the perimeter, the area, the projections, etc.
 * For example, it is used while creating {@link Boundary2DSimpleMeasurer} and
 * {@link Boundary2DProjectionMeasurer}.</p>
 *
 * <p>Please note: this enum <b>may be extended in future versions</b> with new contour styles
 * (for example, rounded or curve).
 * So, if your application chooses behaviour depending on a switch of this type,
 * please always provide a special branch for a case of new future contour types, unknown yet.
 * (It can be <tt>default</tt> case for Java <tt>switch</tt> operator.)
 * It is a good idea to throw <tt>UnsupportedOperationException</tt> in such a branch.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public enum ContourLineType {
    /**
     * Style, for which it is considered that the contour is identical to the boundary.
     * Such contour consists of 1-pixel segments, separating object pixels (unit matrix elements)
     * from the free space (zero matrix elements).
     * In this case, for example, if an object consists of only 1 pixel (a square with the side 1.0),
     * the contour line consists of 4 segments of the length 1.0 &mdash; sides of this square,
     * and the length of contour is 4.0.
     */
    STRICT_BOUNDARY(0) {
        public double x(Boundary2DScanner scanner) {
            return scanner.x() + scanner.lastStep().pixelVertexX;
        }

        public double y(Boundary2DScanner scanner) {
            return scanner.y() + scanner.lastStep().pixelVertexY;
        }
    },

    /**
     * Style, for which it is considered that the contour is a polyline, connecting centers of all pixels,
     * visited by {@link Boundary2DScanner#scanBoundary(net.algart.arrays.ArrayContext)
     * scanBoundary} method (boundary pixels of an object).
     * Such contour consists of segments with the length 1.0 and &radic;2,
     * lying inside an object at the little distance from its boundary (not farther than 0.5).
     * In this case, for example, if an object consists of only 1 pixel (a square with the side 1.0),
     * the contour line is empty (degenerated to a point).
     */
    PIXEL_CENTERS_POLYLINE(1) {
        public double x(Boundary2DScanner scanner) {
            return scanner.x();
        }

        public double y(Boundary2DScanner scanner) {
            return scanner.y();
        }
    },

    /**
     * Style, for which it is considered that the contour is a polyline, connecting centers of all
     * boundary segments, visited by {@link Boundary2DScanner#scanBoundary(net.algart.arrays.ArrayContext)
     * scanBoundary} method.
     * Such contour consists of segments with the length 1.0 and 0.5*&radic;2,
     * lying almost at the boundary of an object (sometimes little inside, sometimes little outside).
     * In this case, for example, if an object consists of only 1 pixel (a square with the side 1.0),
     * the contour line consists of 4 segments of the length 0.5*&radic;2, connecting centers of
     * the sides of this square, and the length of contour is 2*&radic;2.
     */
    SEGMENT_CENTERS_POLYLINE(2) {
        public double x(Boundary2DScanner scanner) {
            return scanner.x() + scanner.side().centerX;
        }

        public double y(Boundary2DScanner scanner) {
            return scanner.y() + scanner.side().centerY;
        }
    };

    static final int STRICT_BOUNDARY_CODE = 0;
    static final int PIXEL_CENTERS_POLYLINE_CODE = 1;
    static final int SEGMENT_CENTERS_POLYLINE_CODE = 2;

    final int code;
    private ContourLineType(int code) {
        this.code = code;
    }

    /**
     * Returns <i>x</i>-coordinate of some point at the contour line, corresponding to the current position
     * at the boundary in the given scanner.
     *
     * <p>More precisely, this method is equivalent to:
     * <blockquote>
     * <table cellpadding=0 cellspacing=0 border=0>
     * <tr>
     * <td><tt>scanner.{@link Boundary2DScanner#x() x()}+scanner.{@link Boundary2DScanner#lastStep()
     * lastStep()}.{@link Boundary2DScanner.Step#pixelVertexX() pixelVertexX()}</tt></td>
     * <td>&nbsp;&nbsp;&nbsp; for {@link #STRICT_BOUNDARY};</td>
     * </tr>
     * <tr>
     * <td><tt>scanner.{@link Boundary2DScanner#x() x()}</tt></td>
     * <td>&nbsp;&nbsp;&nbsp; for {@link #PIXEL_CENTERS_POLYLINE};</td>
     * </tr>
     * <tr>
     * <td><tt>scanner.{@link Boundary2DScanner#x() x()}+scanner.{@link Boundary2DScanner#side()
     * side()}.{@link Boundary2DScanner.Side#centerX() centerX()}</tt></td>
     * <td>&nbsp;&nbsp;&nbsp; for {@link #SEGMENT_CENTERS_POLYLINE};</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param scanner some boundary scanner.
     * @return        <i>x</i>-coordinate of the point at the contour line, corresponding to the current position
     *                at the boundary in the given scanner.
     * @throws NullPointerException  if the argument is <tt>null</tt>.
     * @throws IllegalStateException if the scanner was not {@link Boundary2DScanner#isInitialized() positioned yet},
     *                               or, maybe, if {@link Boundary2DScanner#next()}
     *                               (or {@link Boundary2DScanner#scanBoundary}) method was never called for it.
     */
    public abstract double x(Boundary2DScanner scanner);

    /**
     * Returns <i>y</i>-coordinate of some point at the contour line, corresponding to the current position
     * at the boundary in the given scanner.
     *
     * <p>More precisely, this method is equivalent to:
     * <blockquote>
     * <table cellpadding=0 cellspacing=0 border=0>
     * <tr>
     * <td><tt>scanner.{@link Boundary2DScanner#y() y()}+scanner.{@link Boundary2DScanner#lastStep()
     * lastStep()}.{@link Boundary2DScanner.Step#pixelVertexY() pixelVertexY()}</tt></td>
     * <td>&nbsp;&nbsp;&nbsp; for {@link #STRICT_BOUNDARY};</td>
     * </tr>
     * <tr>
     * <td><tt>scanner.{@link Boundary2DScanner#y() y()}</tt></td>
     * <td>&nbsp;&nbsp;&nbsp; for {@link #PIXEL_CENTERS_POLYLINE};</td>
     * </tr>
     * <tr>
     * <td><tt>scanner.{@link Boundary2DScanner#y() y()}+scanner.{@link Boundary2DScanner#side()
     * side()}.{@link Boundary2DScanner.Side#centerY() centerY()}</tt></td>
     * <td>&nbsp;&nbsp;&nbsp; for {@link #SEGMENT_CENTERS_POLYLINE};</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param scanner some boundary scanner.
     * @return        <i>y</i>-coordinate of the point at the contour line, corresponding to the current position
     *                at the boundary in the given scanner.
     * @throws NullPointerException  if the argument is <tt>null</tt>.
     * @throws IllegalStateException if the scanner was not {@link Boundary2DScanner#isInitialized() positioned yet},
     *                               or, maybe, if {@link Boundary2DScanner#next()}
     *                               (or {@link Boundary2DScanner#scanBoundary}) method was never called for it.
     */
    public abstract double y(Boundary2DScanner scanner);
}
