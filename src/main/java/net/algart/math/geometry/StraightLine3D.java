package net.algart.math.geometry;

import java.util.Objects;
import java.util.Random;

/**
 * <p>Directional infinite straight line in 3D space.
 * It is defined as some <i>point</i> <b>o</b>=(x0,y0,z0), belonging to this straight,
 * and some <i>direction</i>, represented as a unit vector <b>d</b>=(dx,dy,dz).
 * The line consists of all point <b>p</b>=<b>o</b>+<i>t</i><b>d</b>, &minus;&infin;&lt;<i>t</i>&lt;+&infin;.</p>
 *
 * <p>This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually if multithreading access is necessary.</p>
 */
public final class StraightLine3D implements Cloneable {
    private double x0 = 0.0;
    private double y0 = 0.0;
    private double z0 = 0.0;
    private double dx = 1.0;
    private double dy = 0.0;
    private double dz = 0.0;

    public StraightLine3D() {
    }

    public static StraightLine3D getInstance(
            double x0, double y0, double z0,
            double dx, double dy, double dz) {
        return new StraightLine3D().setStart(x0, y0, z0).setDirection(dx, dy, dz);
    }

    public static StraightLine3D getInstanceAlongI(double x0, double y0, double z0, Orthonormal3DBasis basis) {
        return new StraightLine3D().setStart(x0, y0, z0).setDirectionAlongI(basis);
    }

    public static StraightLine3D getInstanceAlongJ(double x0, double y0, double z0, Orthonormal3DBasis basis) {
        return new StraightLine3D().setStart(x0, y0, z0).setDirectionAlongJ(basis);
    }

    public static StraightLine3D getInstanceAlongK(double x0, double y0, double z0, Orthonormal3DBasis basis) {
        return new StraightLine3D().setStart(x0, y0, z0).setDirectionAlongK(basis);
    }

    public double getX0() {
        return x0;
    }

    public StraightLine3D setX0(double x0) {
        this.x0 = x0;
        return this;
    }

    public double getY0() {
        return y0;
    }

    public StraightLine3D setY0(double y0) {
        this.y0 = y0;
        return this;
    }

    public double getZ0() {
        return z0;
    }

    public StraightLine3D setZ0(double z0) {
        this.z0 = z0;
        return this;
    }

    public StraightLine3D setStart(double x0, double y0, double z0) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        return this;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public double getDz() {
        return dz;
    }

    public StraightLine3D setDirection(double dx, double dy, double dz) {
        final double length = Orthonormal3DBasis.length(dx, dy, dz);
        if (length < Orthonormal3DBasis.MIN_ALLOWED_LENGTH) {
            throw new IllegalArgumentException("Zero or too short direction vector ("
                    + dx + ", " + dy + ", " + dz + ")"
                    + " (vectors with length <" + Orthonormal3DBasis.MIN_ALLOWED_LENGTH + " are not allowed)");
        }
        final double mult = 1.0 / length;
        setDirectionComponents(dx * mult, dy * mult, dz * mult);
        return this;
    }

    public StraightLine3D shiftAlong(double shift) {
        x0 += shift * dx;
        y0 += shift * dy;
        z0 += shift * dz;
        return this;
    }

    public StraightLine3D setDirectionAlongI(Orthonormal3DBasis basis) {
        Objects.requireNonNull(basis, "Null basis");
        return setDirection(basis.ix(), basis.iy(), basis.iz());
    }

    public StraightLine3D setDirectionAlongJ(Orthonormal3DBasis basis) {
        Objects.requireNonNull(basis, "Null basis");
        return setDirection(basis.jx(), basis.jy(), basis.jz());
    }

    public StraightLine3D setDirectionAlongK(Orthonormal3DBasis basis) {
        Objects.requireNonNull(basis, "Null basis");
        return setDirection(basis.kx(), basis.ky(), basis.kz());
    }

    public StraightLine3D setDirectionAlongX() {
        return setDirection(1.0, 0.0, 0.0);
    }

    public StraightLine3D setDirectionAlongY() {
        return setDirection(0.0, 1.0, 0.0);
    }

    public StraightLine3D setDirectionAlongZ() {
        return setDirection(0.0, 0.0, 1.0);
    }

    public StraightLine3D setRandomDirection(Random random) {
        for (; ; ) {
            final double dx = 2 * random.nextDouble() - 1.0;
            final double dy = 2 * random.nextDouble() - 1.0;
            final double dz = 2 * random.nextDouble() - 1.0;
            final double distanceSqr = dx * dx + dy * dy + dz * dz;
            if (distanceSqr >= 0.01 && distanceSqr < 1.0) {
                // Note: the second check is necessary to provide uniform distribution in a sphere (not in a cube).
                return setDirection(dx, dy, dz);
            }
        }
    }

    /**
     * Sets this straight to be identical to the passed straight.
     *
     * @param other other straight.
     * @return a reference to this object.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public StraightLine3D setTo(StraightLine3D other) {
        Objects.requireNonNull(other, "Null other");
        this.x0 = other.x0;
        this.y0 = other.y0;
        this.z0 = other.z0;
        this.dx = other.dx;
        this.dy = other.dy;
        this.dz = other.dz;
        return this;
    }

    public double x(double t) {
        return x0 + t * dx;
    }

    public double y(double t) {
        return y0 + t * dy;
    }

    public double z(double t) {
        return z0 + t * dz;
    }

    /**
     * Returns x*dx + y*dy + z*dz.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return projection of the given vector to the direction of this straight.
     */
    public double vectorProjection(double x, double y, double z) {
        return x * dx + y * dy + z * dz;
    }

    /**
     * Returns (x&minus;x0)*dx + (y&minus;y0)*dy + (z&minus;z0)*dz.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return <i>t</i> value for the projection of the given point to this straight.
     */
    public double pointProjection(double x, double y, double z) {
        return (x - x0) * dx + (y - y0) * dy + (z - z0) * dz;
    }

    public double distanceToStraightSquare(double x, double y, double z) {
        x -= x0;
        y -= y0;
        z -= z0;
        final double t = x * dx + y * dy + z * dz;
        return Orthonormal3DBasis.lengthSquare(x - t * dx, y - t * dy, z - t * dz);
        // - more stable solution than x^2+y^2+z^2-t^2
    }

    public double distanceToStraight(double x, double y, double z) {
        return Math.sqrt(distanceToStraightSquare(x, y, z));
    }

    /**
     * Returns (x&minus;x0)<sup>2</sup> + (y&minus;y0)<sup>2</sup> + (z&minus;z0)<sup>2</sup>.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return square of the distance to the start point <b>o</b>.
     */
    public double distanceToStartSquare(double x, double y, double z) {
        return Orthonormal3DBasis.lengthSquare(x - x0, y - y0, z - z0);
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "straight (" + x0 + "," + y0 + "," + z0 + ")+t*(" + dx + "," + dy + "," + dz + ")";
    }

    /**
     * Indicates whether some other object is an instance of this class, containing the same straight line
     * (the same <b>o</b> and <b>d</b>).
     * The corresponding coordinates are compared as in <tt>Double.equals</tt> method,
     * i.e. they are converted to <tt>long</tt> values by <tt>Double.doubleToLongBits</tt> method
     * and the results are compared.
     *
     * @param o the object to be compared for equality with this instance.
     * @return <tt>true</tt> if and only if the specified object is an instance of {@link StraightLine3D},
     * representing the same straight line as this object.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StraightLine3D)) {
            return false;
        }
        final StraightLine3D that = (StraightLine3D) o;
        return (Double.doubleToLongBits(that.x0) == Double.doubleToLongBits(x0)
                && Double.doubleToLongBits(that.y0) == Double.doubleToLongBits(y0)
                && Double.doubleToLongBits(that.z0) == Double.doubleToLongBits(z0)
                && Double.doubleToLongBits(that.dx) == Double.doubleToLongBits(dx)
                && Double.doubleToLongBits(that.dy) == Double.doubleToLongBits(dy)
                && Double.doubleToLongBits(that.dz) == Double.doubleToLongBits(dz));
    }

    /**
     * Returns the hash code of this object.
     *
     * @return the hash code of this object.
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 37 * result + hashCode(x0);
        result = 37 * result + hashCode(y0);
        result = 37 * result + hashCode(z0);
        result = 37 * result + hashCode(dx);
        result = 37 * result + hashCode(dy);
        result = 37 * result + hashCode(dz);
        return result;
    }

    /**
     * Returns an exact copy of this object.
     *
     * @return a reference to the clone.
     */
    @Override
    public StraightLine3D clone() {
        try {
            return (StraightLine3D) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private static int hashCode(double value) {
        long l = Double.doubleToLongBits(value);
        return (int) (l ^ (l >>> 32));
    }

    // Called via reflection in the test for debugging needs; must be private
    private void setDirectionComponents(double dx, double dy, double dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}
