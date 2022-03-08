/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.geom;

/**
 * <p>2D rotation by some angle: an immutable object representing some rotation angle &Delta;&phi;.
 * Positive &Delta;&phi; corresponds to anticlockwise rotation.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.0
 * @since JDK 1.6
 */
public final class Rotation2D implements Cloneable {

    private static final double LARGE_ANGLE = 4 * Math.PI;
    private static final double PI2 = 2 * Math.PI;

    double dfi = 0.0, crot = 1.0, srot = 0.0;

    static final Rotation2D DEFAULT_INSTANCE = new Rotation2D();

    private Rotation2D() {}

    private Rotation2D(double dfi) {
        this.dfi = dfi;
        // double tg = Math.tan(0.5 * dfi);
        // double temp = 1.0 / (1.0 + tg * tg);
        // this.crot = (1 - tg * tg) * temp; // cos(dfi)
        // this.srot = 2.0 * tg * temp; // sin(dfi)
        // - this algorithm works almost the same time under PIV, Java 1.5
        this.crot = Math.cos(dfi);
        this.srot = Math.sin(dfi);
    }

    /**
     * Returns a rotation by &Delta;&phi; = 0.
     */
    public static Rotation2D getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Creates a rotation by &Delta;&phi; = <tt>dfi</tt> argument (in radians).
     */
    public static Rotation2D getInstance(double dfi) {
        return new Rotation2D(dfi);
    }

    /**
     * Returns the current rotation angle (in radians).
     */
    public double getRotationAngle() {
        return dfi;
    }

    /**
     * Returns cosine of the current rotation angle.
     */
    public double getCosine() {
        return crot;
    }

    /**
     * Returns sine of the current rotation angle.
     */
    public double getSine() {
        return srot;
    }

    /**
     * Returns some string representation of this object.
     */
    public String toString() {
        return "Rotation by " + Math.toDegrees(dfi) + " degree";
    }

    /**
     * Returns the hash code for this rotation.
     */
    public int hashCode() {
        return Float.floatToIntBits((float)dfi);
    }

    /**
     * Indicates whether some other rotation is equal to this one, i&#46;e&#46; if the angle returned by
     * {@link #getRotationAngle()} is the same for both rotations.
     * <p>Note: if the difference between rotations is <tt>2k&pi;</tt>,
     * the rotations <i>are not considered</i> to be equal.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Rotation2D))
            return false;
        return ((Rotation2D)obj).dfi == dfi;
    }

    /**
     * Returns this instance (possible solution as this class is immutable).
     */
    public Object clone() {
        return this;
    }

    /**
     * Returns the "normalized" equivalent of the passed angle &phi;: the such angle &phi;<sub>0</sub>,
     * that 0&lt;=&phi;<sub>0</sub>&lt;2&pi; and &phi;=&phi;<sub>0</sub>+2k&pi; for some integer k=...,-2,-1,0,1,2,...
     */
    public static double normalizeAngle(double fi) {
        if (fi > PI2) {
            if (fi > LARGE_ANGLE)
                fi = fi % PI2;
            else
                do
                    fi -= PI2;
                while (fi > PI2);
        } else if (fi < 0.0) {
            if (fi < -LARGE_ANGLE) {
                fi = fi % PI2;
                if (fi < 0.0)
                    fi += PI2;
            } else
                do
                    fi += PI2;
                while (fi < 0.0);
        }
        return fi;
    }

    /**
     * Returns the "normalized" equivalent of the passed angle &phi;: the such angle &phi;<sub>0</sub>,
     * that 0&lt;=&phi;<sub>0</sub>&lt;&pi; and &phi;=&phi;<sub>0</sub>+k&pi; for some integer k=...,-2,-1,0,1,2,....
     */
    public static double normalizeAngleLessPI(double fi) {
        if (fi > Math.PI) {
            if (fi > LARGE_ANGLE)
                fi = fi % Math.PI;
            else
                do
                    fi -= Math.PI;
                while (fi > Math.PI);
        } else if (fi < 0.0) {
            if (fi < -LARGE_ANGLE) {
                fi = fi % Math.PI;
                if (fi < 0.0)
                    fi += Math.PI;
            } else
                do
                    fi += Math.PI;
                while (fi < 0.0);
        }
        return fi;
    }
}
