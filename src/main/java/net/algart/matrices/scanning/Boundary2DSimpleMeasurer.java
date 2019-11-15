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

package net.algart.matrices.scanning;

import java.util.EnumSet;

/**
 * <p>Wrapper of a boundary scanner, that measures some simple parameters of every scanned boundary.</p>
 *
 * <p>This class overrides only {@link #next()} and {@link #resetCounters()} methods
 * of the abstract {@link Boundary2DWrapper} class.
 * Namely, {@link #next()} method, after calling {@link #parent parent}.{@link #next()},
 * corrects several internal fields, describing some parameters of the current measured object (more precisely,
 * parameters of its <a href="Boundary2DScanner.html#completion">completions</a>).
 * These fields are reset to initial values by {@link #resetCounters()} method.
 * After finishing scanning some boundary, for example, after
 * {@link #scanBoundary(net.algart.arrays.ArrayContext) scanBoundary} call,
 * you may use the following access methods to get measured parameters:</p>
 *
 * <ul>
 * <li>{@link #area()} (the area of the current object),</li>
 * <li>{@link #perimeter()} (the length of the contour of the object),</li>
 * <li>{@link #minX()}, {@link #maxX()}, {@link #minY()}, {@link #maxY()},
 * {@link #minXPlusY()}, {@link #minXPlusY()}, {@link #minXMinusY()}, {@link #maxXMinusY()}
 * (parameters of the minimal octagon, containing the measured object or "hole", with sides parallel
 * to <i>x</i>-axis, <i>y</i>-axis and diagonals of the quadrants);</li>
 * <li>{@link #centroidX()}, {@link #centroidY()} (coordinates of the centroid of the
 * current object or "hole").</li>
 * </ul>
 *
 * <p>More precisely, this class measures the figure, bounded by some <i>contour line</i>,
 * following along the scanned boundary. The precise type of this line is specified by an argument
 * of {@link ContourLineType} class, passed to the instantiation method:
 *
 * <blockquote>{@link
 * Boundary2DSimpleMeasurer#getInstance(Boundary2DScanner, ContourLineType, java.util.EnumSet)}</blockquote>
 *
 * <p>In the simplest case ({@link ContourLineType#STRICT_BOUNDARY}), the contour line is just
 * the scanned boundary.</p>
 *
 * <p>Please also note that <i>not all parameters</i>, available via {@link #area()}, {@link #perimeter()} and other
 * methods, are measured by this class. The set of parameters that should be really measured
 * is specified via <tt>EnumSet</tt> while creating the instance in the instantiation method.
 * You should not specify extra parameters, that are not needful: extra calculations can slow down
 * the scanning.</p>
 *
 * <p>All calculations are performed by this class in <tt>strictfp</tt> mode, so the results
 * are absolutely identical on all platforms.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public strictfp class Boundary2DSimpleMeasurer extends Boundary2DWrapper {
    private static final boolean DEBUG_MODE = false; // leads to 2-way calculation of non-trivial areas and centroids

    /**
     * The class describing what parameters of the connected object boundary should be measured by
     * {@link Boundary2DSimpleMeasurer} class.
     *
     * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
     * there are no ways to modify settings of the created instance.</p>
     */
    public static enum ObjectParameter {
        /**
         * Instructs {@link Boundary2DSimpleMeasurer} to measure the oriented area
         * inside the contour, following along the scanned boundary.
         * See definition of the "boundary" term in comments to {@link Boundary2DScanner} class
         * and the list of possible contour types in {@link ContourLineType} class.
         *
         * @see Boundary2DSimpleMeasurer#area()
         */
        AREA,

        /**
         * Instructs {@link Boundary2DSimpleMeasurer} to measure the perimeter of the object:
         * the length of the contour, following along the scanned boundary.
         * See definition of the "boundary" term in comments to {@link Boundary2DScanner} class
         * and the list of possible contour types in {@link ContourLineType} class.
         *
         * @see Boundary2DSimpleMeasurer#perimeter()
         */
        PERIMETER,

        /**
         * Instructs {@link Boundary2DSimpleMeasurer} to measure
         * the maximums and minimums of <i>x</i>, <i>y</i>, <i>x+y</i>, <nobr><i>x&minus;y</i></nobr> values
         * while scanning the boundary, where <i>x</i> and <i>y</i> are coordinates of all
         * points at the contour, following along the scanned boundary.
         * See definition of the "boundary" term in comments to {@link Boundary2DScanner} class
         * and the list of possible contour types in {@link ContourLineType} class.
         *
         * @see Boundary2DSimpleMeasurer#minX()
         * @see Boundary2DSimpleMeasurer#maxX()
         * @see Boundary2DSimpleMeasurer#minY()
         * @see Boundary2DSimpleMeasurer#maxY()
         * @see Boundary2DSimpleMeasurer#minXPlusY()
         * @see Boundary2DSimpleMeasurer#maxXPlusY()
         * @see Boundary2DSimpleMeasurer#minXMinusY()
         * @see Boundary2DSimpleMeasurer#maxXMinusY()
         */
        COORD_RANGES,

        /**
         * Instructs {@link Boundary2DSimpleMeasurer} to find the centroid (center of mass) of
         * the figure, lying inside the contour, following along the scanned boundary.
         * See definition of the "boundary" term in comments to {@link Boundary2DScanner} class
         * and the list of possible contour types in {@link ContourLineType} class.
         *
         * <p>Note that the centroid may be undefined for some styles of contours, if the area
         * of the figure inside the contour is zero. For example, it is possible for "thin" 1-pixel "lines"
         * in a case of {@link ContourLineType#PIXEL_CENTERS_POLYLINE}.
         * In this case, {@link #centroidX()} and {@link #centroidY()} methods return <tt>Double.NaN</tt>.
         *
         * @see Boundary2DSimpleMeasurer#centroidX()
         * @see Boundary2DSimpleMeasurer#centroidY()
         */
        CENTROID
    }


    private final ContourLineType contourLineType;
    private final boolean contourStrictBoundary;
    private final boolean contourPixelCenters;
    private final boolean contourSegmentCenters;
    private final EnumSet<ObjectParameter> measuredParameters;
    private final boolean measureArea;
    private final boolean measurePerimeter;
    private final boolean measureCoordRanges;
    private final boolean measureCentroid;
    long straightStepCount = 0;
    long iArea = 0;
    double area = 0.0;
    double perimeter = 0.0;
    private long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE;
    private long minXPlusY = Long.MAX_VALUE, minXMinusY = Long.MAX_VALUE;
    private long maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE;
    private long maxXPlusY = Long.MIN_VALUE, maxXMinusY = Long.MIN_VALUE;
    double integralXSqr = 0.0, integralYSqr = 0.0;
    private double integralXSqrForCheck = 0.0, integralYSqrForCheck = 0.0;

    Boundary2DSimpleMeasurer(Boundary2DScanner parent,
        ContourLineType contourLineType, EnumSet<ObjectParameter> measuredParameters)
    {
        super(parent);
        if (contourLineType == null)
            throw new NullPointerException("Null contourLineType");
        switch (contourLineType) {
            case STRICT_BOUNDARY:
                this.contourStrictBoundary = true;
                this.contourPixelCenters = false;
                this.contourSegmentCenters = false;
                break;
            case PIXEL_CENTERS_POLYLINE:
                this.contourStrictBoundary = false;
                this.contourPixelCenters = true;
                this.contourSegmentCenters = false;
                break;
            case SEGMENT_CENTERS_POLYLINE:
                this.contourStrictBoundary = false;
                this.contourPixelCenters = false;
                this.contourSegmentCenters = true;
                break;
            default:
                throw new AssertionError("Unsupported contourLineType=" + contourLineType);
        }
        this.contourLineType = contourLineType;
        this.measuredParameters = measuredParameters.clone();
        this.measurePerimeter = measuredParameters.contains(ObjectParameter.PERIMETER);
        this.measureCoordRanges = measuredParameters.contains(ObjectParameter.COORD_RANGES);
        this.measureCentroid = measuredParameters.contains(ObjectParameter.CENTROID);
        this.measureArea = measuredParameters.contains(ObjectParameter.AREA) || this.measureCentroid;

//        this.intPrecision = this.contourStrictBoundary && arrayLength <= Integer.MAX_VALUE;
        // So, dimX and dimY < 2^31, and every x*(x+1) and y*(y+1) <= (2^31-1)*2^31 < Long.MAX_VALUE.
        // Temporary overflows are possible while calculation isumX2, isumY2,
        // but the end result will be in range 0..Long.MAX_VALUE, because it is
        // a coordinate of the centroid, multiplied by area <= arrayLength.
//        - does not help
    }

    /**
     * Creates an instance of this class,
     * that is based on the specified parent scanner and,
     * while scanning any boundary by the parent scanner,
     * measures the specified set of object parameters.
     *
     * @param parent             the parent scanner.
     * @param contourLineType    the style of the contour line: the created object will measure the object,
     *                           lying inside this line.
     * @param measuredParameters the set of parameters that should be measured: all parameters,
     *                           which are not inside this set, are not modified by {@link #next()} method
     *                           and stay to be equal to their initial value.
     * @return                   new instance of this class.
     * @throws NullPointerException if one of the arguments is <tt>null</tt>.
     * @see #contourLineType()
     * @see #measuredParameters()
     */
    public static Boundary2DSimpleMeasurer getInstance(Boundary2DScanner parent,
        ContourLineType contourLineType, EnumSet<ObjectParameter> measuredParameters)
    {
        if (parent == null)
            throw new NullPointerException("Null parent argument");
        if (measuredParameters == null)
            throw new NullPointerException("Null measuredParameters argument");
        if (contourLineType == null)
            throw new NullPointerException("Null contourLineType argument");
        if (DEBUG_MODE) {
            return new Boundary2DSimpleMeasurer(parent, contourLineType, measuredParameters);
        }
        if (measuredParameters.equals(EnumSet.of(ObjectParameter.AREA))) {
            switch (contourLineType) {
                case STRICT_BOUNDARY:
                case SEGMENT_CENTERS_POLYLINE: // the same algorithm
                    return new SimplestStrictBoundaryAreaMeasurer(parent, contourLineType);
                case PIXEL_CENTERS_POLYLINE:
                    return new SimplestPixelCentersAreaMeasurer(parent);
            }
        }
        if (measuredParameters.equals(EnumSet.of(ObjectParameter.PERIMETER))) {
            switch (contourLineType) {
                case STRICT_BOUNDARY:
                    return new SimplestStrictBoundaryPerimeterMeasurer(parent);
                case PIXEL_CENTERS_POLYLINE:
                    return new SimplestPixelCentersPerimeterMeasurer(parent);
                case SEGMENT_CENTERS_POLYLINE:
                    return new SimplestSegmentCentersPerimeterMeasurer(parent);
            }
        }
        return new Boundary2DSimpleMeasurer(parent, contourLineType, measuredParameters);
    }

    /**
     * Returns the contour line style, used for measuring:
     * this class measures the object, lying inside this line.
     *
     * <p>The returned reference is identical to the corresponding argument of
     * {@link #getInstance(Boundary2DScanner, ContourLineType, EnumSet)} method,
     * used for creating this instance.
     *
     * @return the contour line style, used for measuring by this instance.
     */
    public ContourLineType contourLineType() {
        return contourLineType;
    }

    /**
     * Returns the set of parameters, measured by this instance.
     * All parameters, which are not inside this set, are not modified by {@link #next()} method
     * and stay to be equal to their initial value.
     *
     * <p>The returned set is a clone of the corresponding argument of
     * {@link #getInstance(Boundary2DScanner, ContourLineType, EnumSet)} method,
     * used for creating this instance.
     *
     * @return the set of parameters, measured by this instance.
     */
    public final EnumSet<ObjectParameter> measuredParameters() {
        return this.measuredParameters.clone();
    }

    @Override
    public void resetCounters() {
        super.resetCounters();
        straightStepCount = 0;
        iArea = 0;
        area = 0.0;
        perimeter = 0.0;
        minX = minY = minXPlusY = minXMinusY = Long.MAX_VALUE;
        maxX = maxY = maxXPlusY = maxXMinusY = Long.MIN_VALUE;
        integralXSqr = integralYSqr = 0.0;
        integralXSqrForCheck = integralYSqrForCheck = 0.0;
//        isumXSqr = isumYSqr = 0;
    }

    @Override
    public void next() {
        parent.next();
        long x = parent.x();
        long y = parent.y();
        Step step = parent.lastStep();
        if (measureArea) {
            //[[Repeat.SectionStart STRICT_BOUNDARY_area]]
            switch (parent.side().code) {
                case X_MINUS_CODE:
                    iArea -= x - 1;
                    break;
                case X_PLUS_CODE:
                    iArea += x;
                    break;
            }
            //[[Repeat.SectionEnd STRICT_BOUNDARY_area]]
            switch (contourLineType.code) {
                case ContourLineType.PIXEL_CENTERS_POLYLINE_CODE:
                    //[[Repeat.SectionStart PIXEL_CENTERS_area]]
                    if (step.isStraight()) {
                        straightStepCount++;
                    }
                    if (DEBUG_MODE) { // self-testing
                        switch (step.code) {
                            case Step.Y_MINUS_CODE:
                                area -= x;
                                break;
                            case Step.Y_PLUS_CODE:
                                area += x;
                                break;
                            case Step.X_MINUS_Y_MINUS_CODE:
                                area -= x + 0.5;
                                break;
                            case Step.X_PLUS_Y_MINUS_CODE:
                                area -= x - 0.5;
                                break;
                            case Step.X_MINUS_Y_PLUS_CODE:
                                area += x + 0.5;
                                break;
                            case Step.X_PLUS_Y_PLUS_CODE:
                                area += x - 0.5;
                                break;
                        }
                    }
                    //[[Repeat.SectionEnd PIXEL_CENTERS_area]]
                    break;
                case ContourLineType.SEGMENT_CENTERS_POLYLINE_CODE:
                    //[[Repeat.SectionStart SEGMENT_CENTERS_area]]
                    if (DEBUG_MODE) { // self-testing
                        switch (step.code) {
                            case Step.Y_MINUS_CODE:
                                area -= x - 0.5;
                                break;
                            case Step.Y_PLUS_CODE:
                                area += x + 0.5;
                                break;
                            case Step.X_MINUS_Y_MINUS_CODE:
                                area -= 0.5 * (x + 0.25);
                                break;
                            case Step.X_PLUS_Y_MINUS_CODE:
                                area -= 0.5 * (x - 0.75);
                                break;
                            case Step.X_MINUS_Y_PLUS_CODE:
                                area += 0.5 * (x + 0.75);
                                break;
                            case Step.X_PLUS_Y_PLUS_CODE:
                                area += 0.5 * (x - 0.25);
                                break;
                            case Step.ROTATION_X_MINUS_TO_Y_MINUS_CODE:
                            case Step.ROTATION_Y_PLUS_TO_X_MINUS_CODE:
                                area -= 0.5 * (x - 0.25);
                                break;
                            case Step.ROTATION_Y_MINUS_TO_X_PLUS_CODE:
                            case Step.ROTATION_X_PLUS_TO_Y_PLUS_CODE:
                                area += 0.5 * (x + 0.25);
                                break;
                        }
                    }
                    //[[Repeat.SectionEnd SEGMENT_CENTERS_area]]
                    break;
            }
        }

        if (measurePerimeter) {
            switch (contourLineType.code) {
                case ContourLineType.PIXEL_CENTERS_POLYLINE_CODE:
                    perimeter += step.distanceBetweenPixelCenters();
                    break;
                case ContourLineType.SEGMENT_CENTERS_POLYLINE_CODE:
                    perimeter += step.distanceBetweenSegmentCenters();
                    break;
            }
        }
        if (measureCoordRanges) {
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (x + y < minXPlusY) {
                minXPlusY = x + y;
            }
            if (x + y > maxXPlusY) {
                maxXPlusY = x + y;
            }
            // Overflow is impossible while x+y calculation.
            // Proof.
            // If dimX >= 2 and dimY >= 2, then dimX <= Long.MAX_VALUE/dimY <= Long.MAX_VALUE/2 = 2^62-1,
            // analogously dimY <= 2^62-1 and x+y <= (dimX-1) + (dimY-1)  <= 2^63-4.
            // But if dimY = 1, then y < dimY = 0 always, and if dimX = 1, then x < dimX = 0 always:
            // overflow is also impossible.
            if (x - y < minXMinusY) {
                minXMinusY = x - y;
            }
            if (x - y > maxXMinusY) {
                maxXMinusY = x - y;
            }
        }
        if (measureCentroid) {
            switch (contourLineType.code) {
                case ContourLineType.STRICT_BOUNDARY_CODE:
//                    if (intPrecision) {
//                        switch (parent.side().code) {
//                            case X_MINUS_CODE:
//                                isumXSqr -= x * (x - 1);
//                                break;
//                            case X_PLUS_CODE:
//                                isumXSqr += x * (x + 1);
//                                break;
//                            case Y_MINUS_CODE:
//                                isumYSqr -= y * (y - 1);
//                                break;
//                            case Y_PLUS_CODE:
//                                isumYSqr += y * (y + 1);
//                                break;
//                        }
//                    } else {... - does not help in JRE-32
                    //[[Repeat.SectionStart STRICT_BOUNDARY_centroid]]
                    switch (parent.side().code) {
                        case X_MINUS_CODE:
                            integralXSqr -= (x - 0.5) * (x - 0.5);
                            break;
                        case X_PLUS_CODE:
                            integralXSqr += (x + 0.5) * (x + 0.5);
                            break;
                        case Y_MINUS_CODE:
                            integralYSqr -= (y - 0.5) * (y - 0.5);
                            break;
                        case Y_PLUS_CODE:
                            integralYSqr += (y + 0.5) * (y + 0.5);
                            break;
                    }
                    //[[Repeat.SectionEnd STRICT_BOUNDARY_centroid]]
                    break;
                case ContourLineType.PIXEL_CENTERS_POLYLINE_CODE:
                    //[[Repeat.SectionStart PIXEL_CENTERS_centroid]]
                    switch (step.code) {
                        case Step.Y_MINUS_CODE:
                            integralXSqr -= 3.0 * (double) x * (double) x;
                            break;
                        case Step.X_PLUS_CODE:
                            integralYSqr -= 3.0 * (double) y * (double) y;
                            break;
                        case Step.Y_PLUS_CODE:
                            integralXSqr += 3.0 * (double) x * (double) x;
                            break;
                        case Step.X_MINUS_CODE:
                            integralYSqr += 3.0 * (double) y * (double) y;
                            break;
                        case Step.X_MINUS_Y_MINUS_CODE:
                            // 3* 0..1-integral (x0+y)^2*dy = (x0+1)^3-x0^3 = 1 + 3*x0*(1 + x0)
                            integralXSqr -= 1.0 + 3.0 * x * (1.0 + x);
                            // 3* 0..1-integral (y0+x)^2*dx = (y0+1)^3-y0^3 = 1 + 3*y0*(1 + y0)
                            integralYSqr += 1.0 + 3.0 * y * (1.0 + y);
                            break;
                        case Step.X_PLUS_Y_MINUS_CODE:
                            // 3* 0..1-integral (x0-1+y)^2*dy = x0^3-(x0-1)^3 = 1 - 3*x0*(1 - x0)
                            integralXSqr -= 1.0 - 3.0 * x * (1.0 - x);
                            // 3* 0..1-integral (y0+x)^2*dx = (y0+1)^3-y0^3 = 1 + 3*y0*(1 + y0)
                            integralYSqr -= 1.0 + 3.0 * y * (1.0 + y);
                            break;
                        case Step.X_MINUS_Y_PLUS_CODE:
                            // 3* 0..1-integral (x0+y)^2*dy = (x0+1)^3-x0^3 = 1 + 3*x0*(1 + x0)
                            integralXSqr += 1.0 + 3.0 * x * (1.0 + x);
                            // 3* 0..1-integral (y0-1+x)^2*dx = y0^3-(y0-1)^3 = 1 - 3*y0*(1 - y0)
                            integralYSqr += 1.0 - 3.0 * y * (1.0 - y);
                            break;
                        case Step.X_PLUS_Y_PLUS_CODE:
                            // 3* 0..1-integral (x0-1+y)^2*dy = x0^3-(x0-1)^3 = 1 - 3*x0*(1 - x0)
                            integralXSqr += 1.0 - 3.0 * x * (1.0 - x);
                            // 3* 0..1-integral (y0-1+x)^2*dx = y0^3-(y0-1)^3 = 1 - 3*y0*(1 - y0)
                            integralYSqr -= 1.0 - 3.0 * y * (1.0 - y);
                            break;
                    }
                    //[[Repeat.SectionEnd PIXEL_CENTERS_centroid]]
                    if (DEBUG_MODE) {
                        // We are scanning the boundary in clockwise order, when the x-axis
                        // is directed rightwards and the y-axis is directed downwards.
                        // For traditional orientation (y-axis upwards), it is anticlockwise order.
                        // So, we need to calculate, along the boundary, the integrals
                        // -0.5*y(x)^2*dx and +0.5*x(y)^2*dy.
                        double newX = (double) x;
                        double newY = (double) y;
                        double oldX = newX - step.pixelCenterDX;
                        double oldY = newY - step.pixelCenterDY;
                        if (step.pixelCenterDY != 0) {
                            double deltaY = newY - oldY;
                            double c = (newX - oldX) / deltaY; // oldX + c * deltaY = newX
                            // 3* 0..deltaY integral (oldX + c * t)^2 dt = (newX^3 - oldX^3)/c
                            integralXSqrForCheck += step.pixelCenterDX == 0 ?
                                3.0 * oldX * oldX * deltaY :
                                (newX * newX * newX - oldX * oldX * oldX) / c;
                        }
                        if (step.pixelCenterDX != 0) {
                            double deltaX = newX - oldX;
                            double c = (newY - oldY) / deltaX; // oldY + c * deltaX = newY
                            // 3* 0..deltaX integral (oldY + c * t)^2 dt = (newY^3 - oldY^3)/c
                            integralYSqrForCheck -= step.pixelCenterDY == 0 ?
                                3.0 * oldY * oldY * deltaX :
                                (newY * newY * newY - oldY * oldY * oldY) / c;
                        }
                    }
                    break;
                case ContourLineType.SEGMENT_CENTERS_POLYLINE_CODE:
                    //[[Repeat.SectionStart SEGMENT_CENTERS_centroid]]
                    switch (step.code) {
                        case Step.Y_MINUS_CODE:
                            integralXSqr -= 3.0 * (x - 0.5) * (x - 0.5);
                            break;
                        case Step.X_PLUS_CODE:
                            integralYSqr -= 3.0 * (y - 0.5) * (y - 0.5);
                            break;
                        case Step.Y_PLUS_CODE:
                            integralXSqr += 3.0 * (x + 0.5) * (x + 0.5);
                            break;
                        case Step.X_MINUS_CODE:
                            integralYSqr += 3.0 * (y + 0.5) * (y + 0.5);
                            break;
                        case Step.X_MINUS_Y_MINUS_CODE:
                            // 3* 0..0.5-integral (x0+y)^2*dy = (x0+0.5)^3-x0^3 = 0.125 + 3*0.5*x0*(0.5 + x0)
                            integralXSqr -= 0.125 + 1.5 * x * (0.5 + x);
                            // 3* 0..0.5-integral (y0+0.5+x)^2*dx = (y0+1)^3-(y0+0.5)^3 = 0.875 + 3*0.5*y0*(1.5 + y0)
                            integralYSqr += 0.875 + 1.5 * y * (1.5 + y);
                            break;
                        case Step.X_PLUS_Y_MINUS_CODE:
                            // 3* 0..0.5-integral (x0-1+y)^2*dy = (x0-0.5)^3-(x0-1)^3 = 0.875 - 3*0.5*x0*(1.5 - x0)
                            integralXSqr -= 0.875 - 1.5 * x * (1.5 - x);
                            // 3* 0..0.5-integral (y0+x)^2*dx = (y0+0.5)^3-y0^3 = 0.125 + 3*0.5*y0*(0.5 + y0)
                            integralYSqr -= 0.125 + 1.5 * y * (0.5 + y);
                            break;
                        case Step.X_MINUS_Y_PLUS_CODE:
                            // 3* 0..0.5-integral (x0+0.5+y)^2*dy = (x0+1)^3-(x0+0.5)^3 = 0.875 + 3*0.5*x0*(1.5 + x0)
                            integralXSqr += 0.875 + 1.5 * x * (1.5 + x);
                            // 3* 0..0.5-integral (y0-0.5+x)^2*dx = y0^3-(y0-0.5)^3 = 0.125 - 3*0.5*y0*(0.5 - y0)
                            integralYSqr += 0.125 - 1.5 * y * (0.5 - y);
                            break;
                        case Step.X_PLUS_Y_PLUS_CODE:
                            // 3* 0..0.5-integral (x0-0.5+y)^2*dy = x0^3-(x0-0.5)^3 = 0.125 - 3*0.5*x0*(0.5 - x0)
                            integralXSqr += 0.125 - 1.5 * x * (0.5 - x);
                            // 3* 0..0.5-integral (y0-1+x)^2*dx = (y0-0.5)^3-(y0-1)^3 = 0.875 - 3*0.5*y0*(1.5 - y0)
                            integralYSqr -= 0.875 - 1.5 * y * (1.5 - y);
                            break;
                        case Step.ROTATION_X_MINUS_TO_Y_MINUS_CODE:
                            // 3* 0..0.5-integral (x0-0.5+y)^2*dy = x0^3-(x0-0.5)^3 = 0.125 - 3*0.5*x0*(0.5 - x0)
                            integralXSqr -= 0.125 - 1.5 * x * (0.5 - x);
                            // 3* 0..0.5-integral (y0-0.5+x)^2*dx = y0^3-(y0-0.5)^3 = 0.125 - 3*0.5*y0*(0.5 - y0)
                            integralYSqr -= 0.125 - 1.5 * y * (0.5 - y);
                            break;
                        case Step.ROTATION_Y_MINUS_TO_X_PLUS_CODE:
                            // 3* 0..0.5-integral (x0+y)^2*dy = (x0+0.5)^3-x0^3 = 0.125 + 3*0.5*x0*(0.5 + x0)
                            integralXSqr += 0.125 + 1.5 * x * (0.5 + x);
                            // 3* 0..0.5-integral (y0-0.5+x)^2*dx = y0^3-(y0-0.5)^3 = 0.125 - 3*0.5*y0*(0.5 - y0)
                            integralYSqr -= 0.125 - 1.5 * y * (0.5 - y);
                            break;
                        case Step.ROTATION_X_PLUS_TO_Y_PLUS_CODE:
                            // 3* 0..0.5-integral (x0+y)^2*dy = (x0+0.5)^3-x0^3 = 0.125 + 3*0.5*x0*(0.5 + x0)
                            integralXSqr += 0.125 + 1.5 * x * (0.5 + x);
                            // 3* 0..0.5-integral (y0+x)^2*dx = (y0+0.5)^3-y0^3 = 0.125 + 3*0.5*y0*(0.5 + y0)
                            integralYSqr += 0.125 + 1.5 * y * (0.5 + y);
                            break;
                        case Step.ROTATION_Y_PLUS_TO_X_MINUS_CODE:
                            // 3* 0..0.5-integral (x0-0.5+y)^2*dy = x0^3-(x0-0.5)^3 = 0.125 - 3*0.5*x0*(0.5 - x0)
                            integralXSqr -= 0.125 - 1.5 * x * (0.5 - x);
                            // 3* 0..0.5-integral (y0+x)^2*dx = (y0+0.5)^3-y0^3 = 0.125 + 3*0.5*y0*(0.5 + y0)
                            integralYSqr += 0.125 + 1.5 * y * (0.5 + y);
                            break;
                    }
                    //[[Repeat.SectionEnd SEGMENT_CENTERS_centroid]]
                    if (DEBUG_MODE) {
                        // We are scanning the boundary in clockwise order, when the x-axis
                        // is directed rightwards and the y-axis is directed downwards.
                        // For traditional orientation (y-axis upwards), it is anticlockwise order.
                        // So, we need to calculate, along the boundary, the integrals
                        // -0.5*y(x)^2*dx and +0.5*x(y)^2*dy.
                        double newX = contourLineType.x(parent);
                        double newY = contourLineType.y(parent);
                        double oldX = newX - step.segmentCenterDX;
                        double oldY = newY - step.segmentCenterDY;
                        if (step.segmentCenterDY != 0.0) {
                            double deltaY = newY - oldY;
                            double c = (newX - oldX) / deltaY; // oldX + c * deltaY = newX
                            // 3* 0..deltaY integral (oldX + c * t)^2 dt = (newX^3 - oldX^3)/c
                            integralXSqrForCheck += step.segmentCenterDX == 0.0 ?
                                3.0 * oldX * oldX * deltaY :
                                (newX * newX * newX - oldX * oldX * oldX) / c;
                        }
                        if (step.segmentCenterDX != 0) {
                            double deltaX = newX - oldX;
                            double c = (newY - oldY) / deltaX; // oldY + c * deltaX = newY
                            // 3* 0..deltaX integral (oldY + c * t)^2 dt = (newY^3 - oldY^3)/c
                            integralYSqrForCheck -= step.segmentCenterDY == 0.0 ?
                                3.0 * oldY * oldY * deltaX :
                                (newY * newY * newY - oldY * oldY * oldY) / c;
                        }
                    }
                    break;
            }
            if (DEBUG_MODE) {
                if (!contourStrictBoundary) {
                    if (((Math.abs(integralXSqr) < 1000 ?
                        Math.abs(integralXSqrForCheck - integralXSqr) > 0.001 :
                        Math.abs(integralXSqrForCheck - integralXSqr) / Math.abs(integralXSqr) > 0.00001)) ||
                        ((Math.abs(integralYSqr) < 1000 ?
                        Math.abs(integralYSqrForCheck - integralYSqr) > 0.001 :
                        Math.abs(integralYSqrForCheck - integralYSqr) / Math.abs(integralYSqr) > 0.00001)))
                        throw new AssertionError("Incorrect algorithm of centroid calculation: ("
                            + integralXSqr + ", " + integralYSqr + ") instead of ("
                            + integralXSqrForCheck + ", " + integralYSqrForCheck + ") in " + this);
                }
            }
        }
    }

    /**
     * Returns the number of pixels inside the contour, following along the scanned boundary,
     * with minus sign if the scanned boundary is an internal one.
     *
     * <p>It is the same value as {@link #area()} in a case
     * <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt>,
     * but it is available for any contour line type.
     * (For comparison, the result of {@link #perimeter()} call for {@link ContourLineType#STRICT_BOUNDARY}
     * can be retrieved for any contour line type by {@link #stepCount()} method.)
     *
     * @return the oriented area inside the scanned contour, corresponding always to
     *         {@link ContourLineType#STRICT_BOUNDARY STRICT_BOUNDARY} model, independently on the current
     *         {@link #contourLineType() contourLineType()}.
     */
    public long pixelCount() {
        return iArea;
    }

    /**
     * Returns the oriented area inside the contour, following along the scanned boundary.
     * "Oriented" means that the result is equal to the area of the figure inside this contour,
     * if the scanned boundary is an external one, or the same value with minus sign if it is an internal one.
     * (See the definition of <i>external</i> and <i>internal</i> boundaries in the comments to
     * {@link Boundary2DScanner} class.)
     *
     * <p>In particular, if <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt>,
     * the result is equal to the total number of pixels of the
     * <a href="Boundary2DScanner.html#completion">completion</a> of the current measured object
     * (with minus sign if it is an internal boundary, i.e. when it is the number of pixels in the "hole").
     * In the case <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>,
     * the measured area can be 0.0 &mdash; for example, for 1-pixel objects (isolated pixels) or for
     * "thin" 1-pixel "lines".
     *
     * @return the oriented area inside the scanned contour.
     * @see #pixelCount()
     */
    public double area() {
        double result;
        switch (contourLineType) {
            case STRICT_BOUNDARY:
                result = iArea;
                break;
            case PIXEL_CENTERS_POLYLINE:
                result = iArea - 0.5 * straightStepCount - 0.25 * (stepCount() - straightStepCount);
                break;
            case SEGMENT_CENTERS_POLYLINE:
                result = iArea > 0 ? iArea - 0.5 : iArea + 0.5;
                break;
            default:
                throw new AssertionError("Unsupported contourLineType=" + contourLineType);
        }
        if (DEBUG_MODE) {
            if (!contourStrictBoundary &&
                (Math.abs(result) < 1000 ?
                    Math.abs(area - result) > 0.001 :
                    Math.abs(area - result) / Math.abs(result) > 0.00001))
                throw new AssertionError("Incorrect algorithm of area calculation: " + result
                    + " instead of " + area + " in " + this);
        }
        return result;
    }

    /**
     * Returns the total length of the contour, following along the scanned boundary:
     * an estimated perimeter of the measured object, "drawn" at the bit matrix.
     *
     * <p>In particular, if <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt>,
     * the result is just the number of {@link #next()} calls for this boundary and is equal
     * to the result of {@link #stepCount() stepCount()} method.
     * In the case <tt>{@link #contourLineType()}=={@link ContourLineType#SEGMENT_CENTERS_POLYLINE}</tt>,
     * the measured length is a good approximation for the real perimeter of the object.
     *
     * @return the length of the contour line, following along the scanned boundary.
     */
    public double perimeter() {
        return contourStrictBoundary ? stepCount() : perimeter;
    }

    /**
     * Returns the minimal <i>x</i>-coordinate of all points at the contour, following along the scanned boundary.
     *
     * <p>If <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>, the result
     * is equal to the minimal value of the result of {@link #x() x()} method
     * since the last {@link #resetCounters()} call.
     * If <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt> or
     * {@link #contourLineType()}=={@link ContourLineType#SEGMENT_CENTERS_POLYLINE}, the result
     * is less by 0.5.
     *
     * @return the minimal <i>x</i>-coordinate of all points at the contour, following along the scanned boundary.
     */
    public double minX() {
        return contourPixelCenters ? minX : minX - 0.5;
    }

    /**
     * Returns the maximal <i>x</i>-coordinate of all points at the contour, following along the scanned boundary.
     *
     * <p>If <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>, the result
     * is equal to the maximal value of the result of {@link #x() x()} method
     * since the last {@link #resetCounters()} call.
     * If <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt> or
     * {@link #contourLineType()}=={@link ContourLineType#SEGMENT_CENTERS_POLYLINE}, the result
     * is greater by 0.5.
     *
     * @return the maximal <i>x</i>-coordinate of all points at the contour, following along the scanned boundary.
     */
    public double maxX() {
        return contourPixelCenters ? maxX : maxX + 0.5;
    }

    /**
     * Returns the minimal <i>y</i>-coordinate of all points at the contour, following along the scanned boundary.
     *
     * <p>If <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>, the result
     * is equal to the minimal value of the result of {@link #y() y()} method
     * since the last {@link #resetCounters()} call.
     * If <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt> or
     * {@link #contourLineType()}=={@link ContourLineType#SEGMENT_CENTERS_POLYLINE}, the result
     * is less by 0.5.
     *
     * @return the minimal <i>x</i>-coordinate of all points at the contour, following along the scanned boundary.
     */
    public double minY() {
        return contourPixelCenters ? minY : minY - 0.5;
    }

    /**
     * Returns the maximal <i>y</i>-coordinate of all points at the contour, following along the scanned boundary.
     *
     * <p>If <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>, the result
     * is equal to the maximal value of the result of {@link #y() y()} method
     * since the last{@link #resetCounters()} call.
     * If <tt>{@link #contourLineType()}=={@link ContourLineType#STRICT_BOUNDARY}</tt> or
     * {@link #contourLineType()}=={@link ContourLineType#SEGMENT_CENTERS_POLYLINE}, the result
     * is greater by 0.5.
     *
     * @return the maximal <i>y</i>-coordinate of all points at the contour, following along the scanned boundary.
     */
    public double maxY() {
        return contourPixelCenters ? maxY : maxY + 0.5;
    }

    /**
     * Returns the minimal value of the sum <i>x+y</i> of coordinates of all points at the contour,
     * following along the scanned boundary.
     *
     * @return the minimal value of the sum <i>x+y</i> of coordinates of all points at the contour,
     *         following along the scanned boundary.
     */
    public double minXPlusY() {
        return contourPixelCenters ? minXPlusY : contourSegmentCenters ? minXPlusY - 0.5 : minXPlusY - 1.0;
    }

    /**
     * Returns the maximal value of the sum <i>x+y</i> of coordinates of all points at the contour,
     * following along the scanned boundary.
     *
     * @return the maximal value of the sum <i>x+y</i> of coordinates of all points at the contour,
     *         following along the scanned boundary.
     */
    public double maxXPlusY() {
        return contourPixelCenters ? maxXPlusY : contourSegmentCenters ? maxXPlusY + 0.5 : maxXPlusY + 1.0;
    }

    /**
     * Returns the minimal value of the difference <i>x&minus;y</i> of coordinates of all points at the contour,
     * following along the scanned boundary.
     *
     * @return the minimal value of the difference <i>x&minus;y</i> of coordinates of all points at the contour,
     *         following along the scanned boundary.
     */
    public double minXMinusY() {
        return contourPixelCenters ? minXMinusY : contourSegmentCenters ? minXMinusY - 0.5 : minXMinusY - 1.0;
    }

    /**
     * Returns the maximal value of the difference <i>x&minus;y</i> of coordinates of all points at the contour,
     * following along the scanned boundary.
     *
     * @return the maximal value of the difference <i>x&minus;y</i> of coordinates of all points at the contour,
     *         following along the scanned boundary.
     */
    public double maxXMinusY() {
        return contourPixelCenters ? maxXMinusY : contourSegmentCenters ? maxXMinusY + 0.5 : maxXMinusY + 1.0;
    }

    /**
     * Returns the <i>x</i>-coordinate of the centroid (center of mass) of the figure,
     * lying inside the contour, following along the scanned boundary.
     *
     * <p>Note that the centroid may be undefined for some styles of contours, if the area
     * of the figure inside the contour, returned by {@link #area()} method, is zero.
     * For example, it is possible for "thin" 1-pixel "lines"
     * in the case <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>.
     * In this case, this method returns <tt>Double.NaN</tt>.
     *
     * <p>Also note: if it is an internal boundary, it is the centroid of a "hole".
     *
     * @return the <i>x</i>-coordinate of the centroid of the figure the contour,
     *         following along the scanned boundary.
     */
    public double centroidX() {
        return integralXSqr / ((contourStrictBoundary ? 2.0 : 6.0) * area());
    }

    /**
     * Returns the <i>y</i>-coordinate of the centroid (center of mass) of the figure,
     * lying inside the contour, following along the scanned boundary.
     *
     * <p>Note that the centroid may be undefined for some styles of contours, if the area
     * of the figure inside the contour, returned by {@link #area()} method, is zero.
     * For example, it is possible for "thin" 1-pixel "lines"
     * in the case <tt>{@link #contourLineType()}=={@link ContourLineType#PIXEL_CENTERS_POLYLINE}</tt>.
     * In this case, this method returns <tt>Double.NaN</tt>.
     *
     * <p>Also note: if it is an internal boundary, it is the centroid of a "hole".
     *
     * @return the <i>y</i>-coordinate of the centroid of the figure the contour,
     *         following along the scanned boundary.
     */
    public double centroidY() {
        return integralYSqr / ((contourStrictBoundary ? 2.0 : 6.0) * area());
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "simple measurer (" + contourLineType + " mode) " + parent;
    }

    static class SimplestStrictBoundaryAreaMeasurer extends Boundary2DSimpleMeasurer {
        SimplestStrictBoundaryAreaMeasurer(Boundary2DScanner parent, ContourLineType contourLineType) {
            super(parent, contourLineType, EnumSet.of(ObjectParameter.AREA));
        }

        @Override
        public void next() {
            parent.next();
            long x = parent.x();
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, STRICT_BOUNDARY_area)  !! Auto-generated: NOT EDIT !! ]]
            switch (parent.side().code) {
                case X_MINUS_CODE:
                    iArea -= x - 1;
                    break;
                case X_PLUS_CODE:
                    iArea += x;
                    break;
            }
            //[[Repeat.IncludeEnd]]
        }
    }

    static class SimplestPixelCentersAreaMeasurer extends Boundary2DSimpleMeasurer {
        SimplestPixelCentersAreaMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.PIXEL_CENTERS_POLYLINE, EnumSet.of(ObjectParameter.AREA));
        }

        @Override
        public void next() {
            parent.next();
            long x = parent.x();
            Step step = parent.lastStep();
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, STRICT_BOUNDARY_area)  !! Auto-generated: NOT EDIT !! ]]
            switch (parent.side().code) {
                case X_MINUS_CODE:
                    iArea -= x - 1;
                    break;
                case X_PLUS_CODE:
                    iArea += x;
                    break;
            }
            //[[Repeat.IncludeEnd]]
            if (step.isStraight()) {
                straightStepCount++;
            }
        }
    }

    static class SimplestStrictBoundaryPerimeterMeasurer extends Boundary2DSimpleMeasurer {
        SimplestStrictBoundaryPerimeterMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.STRICT_BOUNDARY, EnumSet.of(ObjectParameter.PERIMETER));
        }

        @Override
        public void next() {
            parent.next();
        }
    }

    static class SimplestPixelCentersPerimeterMeasurer extends Boundary2DSimpleMeasurer {
        SimplestPixelCentersPerimeterMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.PIXEL_CENTERS_POLYLINE, EnumSet.of(ObjectParameter.PERIMETER));
        }

        @Override
        public void next() {
            parent.next();
            perimeter += parent.lastStep().distanceBetweenPixelCenters();
        }
    }

    static class SimplestSegmentCentersPerimeterMeasurer extends Boundary2DSimpleMeasurer {
        SimplestSegmentCentersPerimeterMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.SEGMENT_CENTERS_POLYLINE, EnumSet.of(ObjectParameter.PERIMETER));
        }

        @Override
        public void next() {
            parent.next();
            perimeter += parent.lastStep().distanceBetweenSegmentCenters();
        }
    }

    /* (Does not have measurable effect)
    static class SimplestStrictBoundaryCentroidMeasurer extends Boundary2DSimpleMeasurer {
        SimplestStrictBoundaryCentroidMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.STRICT_BOUNDARY, EnumSet.of(ObjectParameter.CENTROID));
        }

        @Override
        public void next() {
            parent.next();
            long x = parent.x();
            long y = parent.y();
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, STRICT_BOUNDARY_area)  !! Auto-generated: NOT EDIT !! ]]
            switch (parent.side().code) {
                case X_MINUS_CODE:
                    iArea -= x - 1;
                    break;
                case X_PLUS_CODE:
                    iArea += x;
                    break;
            }
            //[[Repeat.IncludeEnd]]
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, STRICT_BOUNDARY_centroid)  !! Auto-generated: NOT EDIT !! ]]
                    switch (parent.side().code) {
                        case X_MINUS_CODE:
                            integralXSqr -= (x - 0.5) * (x - 0.5);
                            break;
                        case X_PLUS_CODE:
                            integralXSqr += (x + 0.5) * (x + 0.5);
                            break;
                        case Y_MINUS_CODE:
                            integralYSqr -= (y - 0.5) * (y - 0.5);
                            break;
                        case Y_PLUS_CODE:
                            integralYSqr += (y + 0.5) * (y + 0.5);
                            break;
                    }
                    //[[Repeat.IncludeEnd]]
        }
    }

    static class SimplestPixelCentersCentroidMeasurer extends Boundary2DSimpleMeasurer {
        SimplestPixelCentersCentroidMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.PIXEL_CENTERS_POLYLINE, EnumSet.of(ObjectParameter.CENTROID));
        }

        @Override
        public void next() {
            parent.next();
            long x = parent.x();
            long y = parent.y();
            Step step = parent.lastStep();
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, STRICT_BOUNDARY_area)  !! Auto-generated: NOT EDIT !! ]]
            switch (parent.side().code) {
                case X_MINUS_CODE:
                    iArea -= x - 1;
                    break;
                case X_PLUS_CODE:
                    iArea += x;
                    break;
            }
            //[[Repeat.IncludeEnd]]
            if (step.isStraight()) {
                straightStepCount++;
            }
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, PIXEL_CENTERS_centroid) !! Auto-generated: NOT EDIT !! ]]
                    switch (step.code) {
                        case Step.Y_MINUS_CODE:
                            integralXSqr -= 3.0 * (double) x * (double) x;
                            break;
                        case Step.X_PLUS_CODE:
                            integralYSqr -= 3.0 * (double) y * (double) y;
                            break;
                        case Step.Y_PLUS_CODE:
                            integralXSqr += 3.0 * (double) x * (double) x;
                            break;
                        case Step.X_MINUS_CODE:
                            integralYSqr += 3.0 * (double) y * (double) y;
                            break;
                        case Step.X_MINUS_Y_MINUS_CODE:
                            // 3* 0..1-integral (x0+y)^2*dy = (x0+1)^3-x0^3 = 1 + 3*x0*(1 + x0)
                            integralXSqr -= 1.0 + 3.0 * x * (1.0 + x);
                            // 3* 0..1-integral (y0+x)^2*dx = (y0+1)^3-y0^3 = 1 + 3*y0*(1 + y0)
                            integralYSqr += 1.0 + 3.0 * y * (1.0 + y);
                            break;
                        case Step.X_PLUS_Y_MINUS_CODE:
                            // 3* 0..1-integral (x0-1+y)^2*dy = x0^3-(x0-1)^3 = 1 - 3*x0*(1 - x0)
                            integralXSqr -= 1.0 - 3.0 * x * (1.0 - x);
                            // 3* 0..1-integral (y0+x)^2*dx = (y0+1)^3-y0^3 = 1 + 3*y0*(1 + y0)
                            integralYSqr -= 1.0 + 3.0 * y * (1.0 + y);
                            break;
                        case Step.X_MINUS_Y_PLUS_CODE:
                            // 3* 0..1-integral (x0+y)^2*dy = (x0+1)^3-x0^3 = 1 + 3*x0*(1 + x0)
                            integralXSqr += 1.0 + 3.0 * x * (1.0 + x);
                            // 3* 0..1-integral (y0-1+x)^2*dx = y0^3-(y0-1)^3 = 1 - 3*y0*(1 - y0)
                            integralYSqr += 1.0 - 3.0 * y * (1.0 - y);
                            break;
                        case Step.X_PLUS_Y_PLUS_CODE:
                            // 3* 0..1-integral (x0-1+y)^2*dy = x0^3-(x0-1)^3 = 1 - 3*x0*(1 - x0)
                            integralXSqr += 1.0 - 3.0 * x * (1.0 - x);
                            // 3* 0..1-integral (y0-1+x)^2*dx = y0^3-(y0-1)^3 = 1 - 3*y0*(1 - y0)
                            integralYSqr -= 1.0 - 3.0 * y * (1.0 - y);
                            break;
                    }
                    //[[Repeat.IncludeEnd]]
        }
    }

    static class SimplestSegmentCentersCentroidMeasurer extends Boundary2DSimpleMeasurer {
        SimplestSegmentCentersCentroidMeasurer(Boundary2DScanner parent) {
            super(parent, ContourLineType.SEGMENT_CENTERS_POLYLINE, EnumSet.of(ObjectParameter.CENTROID));
        }

        @Override
        public void next() {
            parent.next();
            long x = parent.x();
            long y = parent.y();
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, STRICT_BOUNDARY_area)  !! Auto-generated: NOT EDIT !! ]]
            switch (parent.side().code) {
                case X_MINUS_CODE:
                    iArea -= x - 1;
                    break;
                case X_PLUS_CODE:
                    iArea += x;
                    break;
            }
            //[[Repeat.IncludeEnd]]
            //[[Repeat(INCLUDE_FROM_FILE, THIS_FILE, SEGMENT_CENTERS_centroid)
            //    step\.code ==> parent.lastStep().code !! Auto-generated: NOT EDIT !! ]]
                    switch (parent.lastStep().code) {
                        case Step.Y_MINUS_CODE:
                            integralXSqr -= 3.0 * (x - 0.5) * (x - 0.5);
                            break;
                        case Step.X_PLUS_CODE:
                            integralYSqr -= 3.0 * (y - 0.5) * (y - 0.5);
                            break;
                        case Step.Y_PLUS_CODE:
                            integralXSqr += 3.0 * (x + 0.5) * (x + 0.5);
                            break;
                        case Step.X_MINUS_CODE:
                            integralYSqr += 3.0 * (y + 0.5) * (y + 0.5);
                            break;
                        case Step.X_MINUS_Y_MINUS_CODE:
                            // 3* 0..0.5-integral (x0+y)^2*dy = (x0+0.5)^3-x0^3 = 0.125 + 3*0.5*x0*(0.5 + x0)
                            integralXSqr -= 0.125 + 1.5 * x * (0.5 + x);
                            // 3* 0..0.5-integral (y0+0.5+x)^2*dx = (y0+1)^3-(y0+0.5)^3 = 0.875 + 3*0.5*y0*(1.5 + y0)
                            integralYSqr += 0.875 + 1.5 * y * (1.5 + y);
                            break;
                        case Step.X_PLUS_Y_MINUS_CODE:
                            // 3* 0..0.5-integral (x0-1+y)^2*dy = (x0-0.5)^3-(x0-1)^3 = 0.875 - 3*0.5*x0*(1.5 - x0)
                            integralXSqr -= 0.875 - 1.5 * x * (1.5 - x);
                            // 3* 0..0.5-integral (y0+x)^2*dx = (y0+0.5)^3-y0^3 = 0.125 + 3*0.5*y0*(0.5 + y0)
                            integralYSqr -= 0.125 + 1.5 * y * (0.5 + y);
                            break;
                        case Step.X_MINUS_Y_PLUS_CODE:
                            // 3* 0..0.5-integral (x0+0.5+y)^2*dy = (x0+1)^3-(x0+0.5)^3 = 0.875 + 3*0.5*x0*(1.5 + x0)
                            integralXSqr += 0.875 + 1.5 * x * (1.5 + x);
                            // 3* 0..0.5-integral (y0-0.5+x)^2*dx = y0^3-(y0-0.5)^3 = 0.125 - 3*0.5*y0*(0.5 - y0)
                            integralYSqr += 0.125 - 1.5 * y * (0.5 - y);
                            break;
                        case Step.X_PLUS_Y_PLUS_CODE:
                            // 3* 0..0.5-integral (x0-0.5+y)^2*dy = x0^3-(x0-0.5)^3 = 0.125 - 3*0.5*x0*(0.5 - x0)
                            integralXSqr += 0.125 - 1.5 * x * (0.5 - x);
                            // 3* 0..0.5-integral (y0-1+x)^2*dx = (y0-0.5)^3-(y0-1)^3 = 0.875 - 3*0.5*y0*(1.5 - y0)
                            integralYSqr -= 0.875 - 1.5 * y * (1.5 - y);
                            break;
                        case Step.ROTATION_X_MINUS_TO_Y_MINUS_CODE:
                            // 3* 0..0.5-integral (x0-0.5+y)^2*dy = x0^3-(x0-0.5)^3 = 0.125 - 3*0.5*x0*(0.5 - x0)
                            integralXSqr -= 0.125 - 1.5 * x * (0.5 - x);
                            // 3* 0..0.5-integral (y0-0.5+x)^2*dx = y0^3-(y0-0.5)^3 = 0.125 - 3*0.5*y0*(0.5 - y0)
                            integralYSqr -= 0.125 - 1.5 * y * (0.5 - y);
                            break;
                        case Step.ROTATION_Y_MINUS_TO_X_PLUS_CODE:
                            // 3* 0..0.5-integral (x0+y)^2*dy = (x0+0.5)^3-x0^3 = 0.125 + 3*0.5*x0*(0.5 + x0)
                            integralXSqr += 0.125 + 1.5 * x * (0.5 + x);
                            // 3* 0..0.5-integral (y0-0.5+x)^2*dx = y0^3-(y0-0.5)^3 = 0.125 - 3*0.5*y0*(0.5 - y0)
                            integralYSqr -= 0.125 - 1.5 * y * (0.5 - y);
                            break;
                        case Step.ROTATION_X_PLUS_TO_Y_PLUS_CODE:
                            // 3* 0..0.5-integral (x0+y)^2*dy = (x0+0.5)^3-x0^3 = 0.125 + 3*0.5*x0*(0.5 + x0)
                            integralXSqr += 0.125 + 1.5 * x * (0.5 + x);
                            // 3* 0..0.5-integral (y0+x)^2*dx = (y0+0.5)^3-y0^3 = 0.125 + 3*0.5*y0*(0.5 + y0)
                            integralYSqr += 0.125 + 1.5 * y * (0.5 + y);
                            break;
                        case Step.ROTATION_Y_PLUS_TO_X_MINUS_CODE:
                            // 3* 0..0.5-integral (x0-0.5+y)^2*dy = x0^3-(x0-0.5)^3 = 0.125 - 3*0.5*x0*(0.5 - x0)
                            integralXSqr -= 0.125 - 1.5 * x * (0.5 - x);
                            // 3* 0..0.5-integral (y0+x)^2*dx = (y0+0.5)^3-y0^3 = 0.125 + 3*0.5*y0*(0.5 + y0)
                            integralYSqr += 0.125 + 1.5 * y * (0.5 + y);
                            break;
                    }
                    //[[Repeat.IncludeEnd]]
        }
    }
    */
}
