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

package net.algart.math.geometry;

/**
 * <p>"Unsafe" 2D segment</p>
 *
 * <p>It is a version of {@link Segment2D} segment, which can work incorrectly if arguments
 * of setFi(fi, nx, ny) or setAll(centerX, centerY, length, fi, nx, ny) method are illegal.</p>
 *
 * <p>This class allows to avoid slow calculation of cosine and sine
 * while setting new direction. But getNormalClone() and copyNormalTo()
 * methods work slower than the same methods in {@link Segment2D} class.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class UnsafeSegment2D implements Cloneable {

    private static final double PI_05 = 0.5 * Math.PI;

    private double equalityPrecision = 0.0;
    private double equalityPrecisionInv = Double.POSITIVE_INFINITY;

    double centerX = 0.0, centerY = 0.0;
    double length = 0.0;
    Direction2D normal = new Direction2D();

    UnsafeSegment2D() { // can be inherited in this package only
    }

    /**
     * Creates some "default" segment. The parameters of this segment (center, orientation, length)
     * are not specified. So, you must set all segment parameters after creating such instance.
     */
    public static UnsafeSegment2D getUnsafeInstance() {
        return new UnsafeSegment2D();
    }

    /**
     * Returns the x-coordinate of the segment center.
     */
    public double getCenterX() {
        return centerX;
    }

    /**
     * Sets the x-coordinate of the segment center.
     */
    public void setCenterX(double value) {
        centerX = value;
    }

    /**
     * Returns the y-coordinate of the segment center.
     */
    public double getCenterY() {
        return centerY;
    }

    /**
     * Sets the y-coordinate of the segment center.
     */
    public void setCenterY(double value) {
        centerY = value;
    }

    /**
     * Returns the segment length.
     */
    public double getLength() {
        return length;
    }

    /**
     * Sets the segment length.
     */
    public void setLength(double value) {
        if (length < 0.0)
            throw new IllegalArgumentException("Negative segment length " + length);
        length = value;
    }

    /**
     * Returns the direction (in radians) from the segment begin to the segment end:
     * an angle between 0x and a ray from 1st to 2nd segment end.
     */
    public double getFi() {
        return normal.fi - PI_05;
    }

    /**
     * Sets the segment orientation (an angle between 0x and a ray from 1st to 2nd segment end).
     * This method works slowly, because requires sine and cosine calculation.
     */
    public void setFi(double value) {
        normal.setAngle(value + PI_05);
    }

    /**
     * Returns x-coordinate of the segment begin.
     */
    public double getX1() {
        return centerX - normal.dy * 0.5 * length;
    }

    /**
     * Returns y-coordinate of the segment begin.
     */
    public double getY1() {
        return centerY + normal.dx * 0.5 * length;
    }

    /**
     * Returns x-coordinate of the segment end.
     */
    public double getX2() {
        return centerX + normal.dy * 0.5 * length;
    }

    /**
     * Returns y-coordinate of the segment end.
     */
    public double getY2() {
        return centerY - normal.dx * 0.5 * length;
    }

    /**
     * Sets the coordinates of the segment center.
     */
    public void setCenter(double centerX, double centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    /**
     * Sets all segment information.
     * This method works slowly, because requires sine and cosine calculation.
     */
    public void setAll(double centerX, double centerY, double length, double fi) {
        if (length < 0.0)
            throw new IllegalArgumentException("Negative segment length " + length);
        this.centerX = centerX;
        this.centerY = centerY;
        this.length = length;
        this.normal.setAngle(fi + PI_05);
    }

    /**
     * Sets all segment information (normal.getAngle() = this.getFi() + &pi; / 2).
     */
    public void setAll(double centerX, double centerY, double length, Direction2D normal) {
        if (normal == null)
            throw new NullPointerException("Null normal argument");
        if (length < 0.0)
            throw new IllegalArgumentException("Negative segment length " + length);
        this.centerX = centerX;
        this.centerY = centerY;
        this.length = length;
        this.normal.copyFrom(normal);
    }

    /**
     * Copies the agrument into the normal unit vector inside this object
     * (value.getAngle() = this.getFi() + &pi; / 2)
     */
    public void setNormal(Direction2D value) {
        normal.copyFrom(value);
    }

    /**
     * Sets the segment direction.
     * <p>The following conditions must be true: nx = cos(fi+&pi;/2), ny = sin(fi+&pi;/2).
     * If it is not so, then getNormalUnitVectorX(), getNormalUnitVectorY(),
     * getX1(), getY1(), getX2(), getY2() methods will work incorrectly.
     */
    public void setFi(double fi, double nx, double ny) {
        this.normal.fi = fi + PI_05;
        this.normal.dx = nx;
        this.normal.dy = ny;
    }

    /**
     * Sets all segment information.
     * <p>The following conditions must be true: length &gt;= 0.0, nx = cos(fi+&pi;/2), ny = sin(fi+&pi;/2)
     * If it is not so, then getNormalUnitVectorX(), getNormalUnitVectorY(),
     * getX1(), getY1(), getX2(), getY2() methods will work incorrectly.
     */
    public void setAll(double centerX, double centerY, double length, double fi, double nx, double ny) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.length = length;
        this.normal.fi = fi + PI_05;
        this.normal.dx = nx;
        this.normal.dy = ny;
    }

    /**
     * Returns the normal unit vector, with creating a clone.
     */
    public Direction2D getNormalClone() {
        return new Direction2D(normal.fi);
    }

    /**
     * Copies the normal vector to this segment to the given argument
     * (normal.getAngle() will be equal to this.getFi() + &pi; / 2).
     */
    public void copyNormalTo(Direction2D normal) {
        normal.setAngle(this.normal.fi);
    }

    /**
     * Returns x-projection of the unit vector directed from the segment begin to the segment end,
     * that is which direction is fi, where fi is set by setFi or setAll methods.
     * Equivalent to <tt>getNormalUnitVectorY()</tt>.
     */
    public double getUnitVectorX() {
        return normal.dy;
    }

    /**
     * Returns y-projection of the unit vector directed from the segment begin to the segment end,
     * that is which direction is fi, where fi is set by setFi or setAll methods.
     * Equivalent to <tt>-getNormalUnitVectorX()</tt>.
     */
    public double getUnitVectorY() {
        return -normal.dx;
    }

    /**
     * Returns x-projection of the normal unit vector,
     * which direction is fi+&pi;/2, where fi is set by setFi or setAll methods.
     */
    public double getNormalUnitVectorX() {
        return normal.dx;
    }

    /**
     * Returns y-projection of the normal unit vector,
     * which direction is fi+&pi;/2, where fi is set by setFi or setAll methods.
     */
    public double getNormalUnitVectorY() {
        return normal.dy;
    }

    private static final double LARGE_ANGLE = 8 * Math.PI;

    /**
     * Returns the shortes angle (0..&pi;/2, in radians)
     * between the straights containing this and <tt>other</tt> segments.
     */
    public double getAngleBetweenStraights(UnsafeSegment2D other) {
        double dfi = normal.getAngleBetweenDirections(other.normal);
        return dfi <= PI_05 ? dfi : Math.PI - dfi;
    }

    /**
     * Returns the shortes angle (0..&pi;, in radians)
     * between the directions of this and <tt>other</tt> segments, from 1st end to 2nd.
     */
    public double getAngleBetweenDirections(UnsafeSegment2D other) {
        return normal.getAngleBetweenDirections(other.normal);
    }

    /**
     * Revive direction vector by recalculation of the normal unit vector:
     * see {@link Direction2D#revive()}.
     * This method should usually be called sometimes in any long sequence of <tt>rotate</tt>
     * or <tt>rotateBack</tt> methods.
     */
    public void revive() {
        this.normal.revive();
    }

    /**
     * Rotates the segment around its center: new direction angle will be &phi; + rot.getRotationAngle(),
     * where &phi; is current direction angle.
     */
    public void rotate(Rotation2D rot) {
        normal.rotate(rot);
    }

    /**
     * Rotates the segment around its center: new direction angle will be &phi; - rot.getRotationAngle(),
     * where &phi; is current direction angle.
     */
    public void rotateBack(Rotation2D rot) {
        normal.rotateBack(rot);
    }

    /**
     * Rotates the segment around its center by &pi;: new direction angle will be &phi; + &pi;,
     * where &phi; is current direction angle.
     */
    public void inverse() {
        normal.inverse();
    }

    /**
     * Sets the current segment identical to <tt>other</tt>.
     */
    public void copyFrom(UnsafeSegment2D other) {
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.length = other.length;
        this.normal.copyFrom(other.normal);
    }

    /**
     * Generates IllegalStateException exception if the information stored in this object is invalid, setFi(fi, nx, ny) or setAll(centerX, centerY, length, fi, nx, ny) method
     * was called with illegal arguments.
     */
    public void checkInvariants() {
        double trueNx = Math.cos(normal.fi);
        double trueNy = Math.sin(normal.fi);
        if (Math.abs(trueNx - normal.dx) > 0.01 || Math.abs(trueNy - normal.dy) > 0.01)
            throw new IllegalStateException("Illegal instance of UnsafeSegment2D: " + this);
        if (length < 0.0)
            throw new IllegalStateException("Illegal instance of UnsafeSegment2D (negative segment length): " + this);
    }


    /**
     * Returns the "equality prevision" for this segment: a precision of segments equality
     * considered by <tt>equals</tt> and <tt>hashCode</tt> methods.
     * Default value is 0.0 (precise equality).
     *
     * @see #equals(Object)
     * @see #hashCode()
     * @see #setEqualityPrecision(double)
     */
    public double getEqualityPrecision() {
        return equalityPrecision;
    }

    /**
     * Sets the "equality prevision" for this segment: a precision of segments equality
     * considered by <tt>equals</tt> and <tt>hashCode</tt> methods.
     * Default value is 0.0 (precise equality).
     * The <tt>value</tt> argument must be &gt;=0.0.
     *
     * @see #equals(Object)
     * @see #hashCode()
     */
    public void setEqualityPrecision(double value) {
        if (value < 0.0)
            throw new IllegalArgumentException("Equality prevision cannot be negative");
        equalityPrecision = value;
        equalityPrecisionInv = 1.0 / value;
    }

    /**
     * Returns some string representation of this object.
     */
    public String toString() {
        return "(" + getX1() + "," + getY1() + ")-(" + getX2() + "," + getY2() + ")"
            + ", length=" + getLength()
            + ", fi=" + Math.toDegrees(getFi()) + " degree, normal=[" + normal + "]"
            ;
    }

    /**
     * Returns the hash code for this segment.
     * If "equality precision", set by {@link #setEqualityPrecision(double)} method, is non-zero,
     * this method uses it to correspond to behavior of {@link #equals(Object)}} method.
     */
    public int hashCode() {
        if (equalityPrecision == 0.0) {
            return ((Float.floatToIntBits((float)centerX)
                * 37 + Float.floatToIntBits((float)centerY))
                * 37 + Float.floatToIntBits((float)length))
                * 37 + Float.floatToIntBits((float)normal.fi);
        } else {
            return (((int)(centerX * equalityPrecisionInv)
                * 37 + (int)(centerY * equalityPrecisionInv))
                * 37 + (int)(length * equalityPrecisionInv))
                * 37 + (int)(normal.fi * length * equalityPrecisionInv);
        }
    }

    /**
     * Indicates whether some other segment is equal to this one, with a precision set by the last
     * {@link #setEqualityPrecision(double)} call (or with absolute precision if that method was never called).
     *
     * <p>This method <i>requires</i> that the "equality precision", which can be set by
     * {@link #setEqualityPrecision(double)} method (and is 0.0 by default),
     * is the same for both segments. In other case, this method throws IllegalArgumentException.
     *
     * <p>This method returns <tt>true</tt> if:<ol>
     *
     * <li>The <tt>obj</tt> argument is a segment (an instance of {@link UnsafeSegment2D} or {@link Segment2D}).
     *
     * <li>The values <tt>centerX</tt>, <tt>centerY</tt>, <tt>length</tt> and <tt>fi*length</tt>
     * (see the corresponding methods {@link #getCenterX()}, {@link #getCenterY()}, {@link #getLength()},
     * {@link #getFi()}) are equal for both segments. But some difference is still possible.
     * Namely, let <tt>&epsilon;</tt> is "equality precision" set by {@link #setEqualityPrecision(double)} method
     * (identical for both segments).
     * Two values <tt>A</tt> and <tt>B</tt> are considered "equal" here,
     * if <tt>A==B</tt> or if &epsilon;&gt;0.0 and (int)(A/&epsilon;)==(int)(B/&epsilon;).
     * Of course, this algorithm works good only if all segment coordinatates,
     * divided by equality precision &epsilon;, are not too large (&gt;&gt;2^31) or too small (&lt;&lt;1.0).
     * </ol>
     *
     * <p>Note: if the difference between directions is <tt>k&pi;</tt> or even <tt>2k&pi;</tt>,
     * the segments <i>are not considered</i> to be equal, though their ends are identical.
     * @throws IllegalArgumentException if the passed argument is a segment,
     * but this and passed segments have different equality precision
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof UnsafeSegment2D))
            return false;
        UnsafeSegment2D o = (UnsafeSegment2D)obj;
        if (equalityPrecision != o.equalityPrecision)
            throw new IllegalArgumentException("Not-comparable segments!");
        if (equalityPrecision == 0.0)
            return centerX == o.centerX && centerY == o.centerY && length == o.length && normal.fi == o.normal.fi;
        else
            return (int)(centerX * equalityPrecisionInv) == (int)(o.centerX * equalityPrecisionInv)
                && (int)(centerY * equalityPrecisionInv) == (int)(o.centerY * equalityPrecisionInv)
                && (int)(length * equalityPrecisionInv) == (int)(o.length * equalityPrecisionInv)
                && (int)(normal.fi * length * equalityPrecisionInv) == (int)(o.normal.fi * o.length * equalityPrecisionInv);
    }


    /**
     * Creates an identical copy of this object.
     */
    public Object clone() {
        try {
            UnsafeSegment2D result = (UnsafeSegment2D)super.clone();
            result.normal = new Direction2D();
            result.copyFrom(this);
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
