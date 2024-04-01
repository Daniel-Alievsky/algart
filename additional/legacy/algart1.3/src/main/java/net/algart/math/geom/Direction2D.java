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

package net.algart.math.geometry;

/**
 * <p>2D direction: an angle &phi; + corresponding unit vector (cos &phi;, sin &phi;) </p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 */
public class Direction2D implements Cloneable {
    double fi = 0.0, dx = 1.0, dy = 0.0;

    Direction2D() { // can be inherited in this package only
    }

    Direction2D(double fi) {
        setAngle(fi);
    }

    /**
     * Creates a direction (1, 0), &phi; = 0.
     */
    public static Direction2D getInstance() {
        return new Direction2D();
    }

    /**
     * Creates a direction (cos &phi;, sin &phi;), &phi; = <tt>fi</tt> argument (in radians).
     */
    public static Direction2D getInstance(double fi) {
        return new Direction2D(fi);
    }

    /**
     * Sets the angle: new direction will be (cos &phi;, sin &phi;), &phi; = <tt>fi</tt> argument (in radians).
     */
    public void setAngle(double fi) {
        this.fi = fi;
        this.dx = Math.cos(fi);
        this.dy = Math.sin(fi);
    }

    /**
     * Returns the angle: current direction is (cos &phi;, sin &phi;), &phi; = method result (in radians).
     */
    public double getAngle() {
        return fi;
    }

    /**
     * Returns cos &phi;, current direction is (cos &phi;, sin &phi;).
     */
    public double getUnitVectorX() {
        return dx;
    }

    /**
     * Returns sin &phi;, current direction is (cos &phi;, sin &phi;).
     */
    public double getUnitVectorY() {
        return dy;
    }

    /**
     * Returns the shortes angle (0..&pi;, in radians)
     * between the given directions.
     */
    public double getAngleBetweenDirections(Direction2D other) {
        return Rotation2D.normalizeAngleLessPI(other.fi > fi ? other.fi - fi : fi - other.fi);
    }

    /**
     * Revive direction vector: the direction angle &phi; is changed to an equivalent angle in
     * 0&lt;=&phi;&lt;2&pi; range (if current &phi; is not in this range), and
     * the values cos &phi; and sin &phi; are recalculated.
     * This method should be called sometimes in any long sequence of <tt>rotate</tt>
     * or <tt>rotateBack</tt> methods.
     */
    public void revive() {
        fi = Rotation2D.normalizeAngle(fi);
        // double tg = Math.tan(0.5 * fi);
        // double temp = 1.0 / (1.0 + tg * tg);
        // this.dx = (1 - tg * tg) * temp; // cos(fi)
        // this.dy = 2.0 * tg * temp; // sin(fi)
        // - this algorithm works almost the same time under PIV, Java 1.5
        dx = Math.cos(fi);
        dy = Math.sin(fi);
    }

    /**
     * Rotates current direction: new direction angle will be &phi; + rot.getRotationAngle(),
     * where &phi; is current direction angle.
     */
    public void rotate(Rotation2D rot) {
        fi += rot.dfi;
        double newDx = rot.crot * dx - rot.srot * dy;
        double newDy = rot.srot * dx + rot.crot * dy;
        dx = newDx;
        dy = newDy;
    }

    /**
     * Rotates current direction: new direction angle will be &phi; - rot.getRotationAngle(),
     * where &phi; is current direction angle.
     */
    public void rotateBack(Rotation2D rot) {
        fi -= rot.dfi;
        double newDx = rot.crot * dx + rot.srot * dy;
        double newDy = - rot.srot * dx + rot.crot * dy;
        dx = newDx;
        dy = newDy;
    }

    /**
     * Rotates current direction by &pi;: new direction angle will be &phi; + &pi;,
     * where &phi; is current direction angle.
     */
    public void inverse() {
        fi += Math.PI;
        dx = -dx;
        dy = -dy;
    }

    /**
     * Sets the current direction identical to <tt>other</tt>.
     */
    public void copyFrom(Direction2D other) {
        this.fi = other.fi;
        this.dx = other.dx;
        this.dy = other.dy;
    }

    /**
     * Returns some string representation of this object.
     */
    public String toString() {
        return "Direction " + dx + ", " + dy + " (" + Math.toDegrees(fi) + " degree)";
    }

    /**
     * Returns the hash code for this direction.
     */
    public int hashCode() {
        return Float.floatToIntBits((float)fi);
    }

    /**
     * Indicates whether some other direction is equal to this one, i&#46;e&#46; if the angle returned by
     * {@link #getAngle()} is the same for both directions.
     * <p>Note: if the difference between directions is <tt>2k&pi;</tt>,
     * the directions <i>are not considered</i> to be equal, though their unit vector are identical.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Direction2D))
            return false;
        return ((Direction2D)obj).fi == fi;
    }


    /**
     * Creates an identical copy of this object.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
