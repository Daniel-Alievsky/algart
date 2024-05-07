package net.algart.math.geometry;

import java.util.Objects;
import java.util.Random;

/**
 * <p>Directional infinite straight line in 3D space.
 * It is defined as some <i>start point</i> <b>o</b>=(x0,y0,z0), belonging to this straight,
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

    private StraightLine3D() {
    }

    /**
     * Creates some new straight, passing through the origin (0,0,0).
     * The direction of this straight is (1,0,0) (<i>x</i>-axis), but you should not use this fact:
     * in future versions this may change.
     *
     * @return new straight.
     */
    public static StraightLine3D getInstance() {
        return new StraightLine3D();
    }

    /**
     * Creates new straight, passing through the origin (0,0,0), with the given direction.
     *
     * @param dx x-component of the direction <b>d</b>.
     * @param dy y-component of the direction <b>d</b>.
     * @param dz z-component of the direction <b>d</b>.
     * @return new straight.
     */
    public static StraightLine3D getInstanceFromOrigin(
            double dx, double dy, double dz) {
        return new StraightLine3D().setDirection(dx, dy, dz);
    }

    /**
     * Creates new straight, passing through the given start point <tt>(x0,y0,z0)</tt>, with the given direction.
     *
     * @param x0 x-coordinate of the start point <b>o</b>.
     * @param y0 y-coordinate of the start point <b>o</b>.
     * @param z0 z-coordinate of the start point <b>o</b>.
     * @param dx x-component of the direction <b>d</b>.
     * @param dy y-component of the direction <b>d</b>.
     * @param dz z-component of the direction <b>d</b>.
     * @return new straight.
     */
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

    public double x0() {
        return x0;
    }

    public StraightLine3D x0(double x0) {
        this.x0 = x0;
        return this;
    }

    public double y0() {
        return y0;
    }

    public StraightLine3D y0(double y0) {
        this.y0 = y0;
        return this;
    }

    public double z0() {
        return z0;
    }

    public StraightLine3D z0(double z0) {
        this.z0 = z0;
        return this;
    }

    public StraightLine3D setStart(double x0, double y0, double z0) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        return this;
    }

    public double dx() {
        return dx;
    }

    public double dy() {
        return dy;
    }

    public double dz() {
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
     * Returns projection of the given vector <b>a</b> to this straight: <tt>ax * dx + ay * dy + az * dz</tt>.
     *
     * <p>Equivalent to
     * <tt>{@link Orthonormal3DBasis#scalarProduct Orthonormal3DBasis.scalarProduct}(ax, ay, ax, dx, dy, dz).</p>
     *
     * @param ax x-component of the vector.
     * @param ay y-component of the vector.
     * @param az z-component of the vector.
     * @return projection of the given vector to the direction of this straight.
     */
    public double vectorProjection(double ax, double ay, double az) {
        return ax * dx + ay * dy + az * dz;
    }

    /**
     * Returns <tt>(x&minus;x0)*dx + (y&minus;y0)*dy + (z&minus;z0)*dz</tt>.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return <i>t</i> value for the projection of the given point to this straight.
     */
    public double pointProjection(double x, double y, double z) {
        return (x - x0) * dx + (y - y0) * dy + (z - z0) * dz;
    }

    /**
     * Returns (x&minus;x0)<sup>2</sup> + (y&minus;y0)<sup>2</sup> + (z&minus;z0)<sup>2</sup>.
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return square of the distance to the start point <b>o</b>.
     */
    public double distanceToStartPointSquare(double x, double y, double z) {
        return Orthonormal3DBasis.lengthSquare(x - x0, y - y0, z - z0);
    }

    /**
     * Returns the distance from the given point <tt>(x,y,z)</tt> to the start point <tt>(x0,y0,z0)</tt>.
     * <p>Equivalent to
     * <tt>Math.sqrt({@link #distanceToStartPointSquare distanceToStartPointSquare}(x, y, z))</tt>.</p>
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return square of the distance to the start point <b>o</b>.
     */
    public double distanceToStartPoint(double x, double y, double z) {
        return Math.sqrt(distanceToStartPointSquare(x, y, z));
    }

    /**
     * Returns the square of the distance from the given point <tt>(x,y,z)</tt> to this straight.
     *
     * <p>Equivalent to
     * {@link StraightLine3D#distanceToStraightSquare(double, double, double, double, double, double)
     * StraightLine3D.distanceToStraightSquare}({@link #dx()}, {@link #dy()}, {@link #dz()}, x - {@link
     * #x0()}, y - {@link #y0()}, z - {@link #z0()}).</p>
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return square of the distance between the point and this straight.
     */
    public double distanceToStraightSquare(double x, double y, double z) {
        return distanceToStraightSquare(dx, dy, dz, x - x0, y - y0, z - z0);
    }

    /**
     * Returns the distance from the given point <tt>(x,y,z)</tt> to this straight.
     * <p>Equivalent to
     * <tt>Math.sqrt({@link #distanceToStraightSquare(double, double, double)
     * distanceToStraightSquare}(x, y, z))</tt>.</p>
     *
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return distance between the point and this straight.
     */
    public double distanceToStraight(double x, double y, double z) {
        return Math.sqrt(distanceToStraightSquare(x, y, z));
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
        if (!(o instanceof StraightLine3D that)) {
            return false;
        }
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
        result = 37 * result + Double.hashCode(x0);
        result = 37 * result + Double.hashCode(y0);
        result = 37 * result + Double.hashCode(z0);
        result = 37 * result + Double.hashCode(dx);
        result = 37 * result + Double.hashCode(dy);
        result = 37 * result + Double.hashCode(dz);
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

    /**
     * Returns the square of the distance from the given point <tt>(x,y,z)</tt> to the straight
     * <tt>{@link #getInstanceFromOrigin(double, double, double) getInstanceFromOrigin}(dx, dy, dz)</tt>,
     * where <tt><b>d</b>=(dx,dy,dz)</tt> is some unit vector.
     *
     * <p>This method works faster than creating a new instance of this class and calling its
     * {@link #distanceToStraightSquare(double, double, double)} method.
     * But this method requires that the vector <tt>(dx, dy, dz)</tt> to be unit:
     * <tt>dx&nbsp;*&nbsp;dx&nbsp;+&nbsp;dy&nbsp;*&nbsp;dy+&nbsp;dz&nbsp;*&nbsp;dz&nbsp;=&nbsp;1.0</tt>,
     * otherwise its result will be incorrect.
     *
     * @param dx x-component of the unit direction vector <b>d</b>.
     * @param dy y-component of the unit direction vector <b>d</b>.
     * @param dz z-component of the unit direction vector <b>d</b>.
     * @param x  x-coordinate of the point.
     * @param y  y-coordinate of the point.
     * @param z  z-coordinate of the point.
     */
    public static double distanceToStraightSquare(double dx, double dy, double dz, double x, double y, double z) {
        final double t = x * dx + y * dy + z * dz;
        return Orthonormal3DBasis.lengthSquare(x - t * dx, y - t * dy, z - t * dz);
        // - more stable solution than x^2+y^2+z^2-t^2
    }

    /**
     * Returns the distance from the given point <tt>(x,y,z)</tt> to the straight
     * <tt>{@link #getInstanceFromOrigin(double, double, double) getInstanceFromOrigin}(dx, dy, dz)</tt>,
     * where <tt><b>d</b>=(dx,dy,dz)</tt> is some unit vector.
     *
     * <p>Equivalent to
     * <tt>Math.sqrt({@link #distanceToStraightSquare(double, double, double, double, double, double)
     * distanceToStraightSquare}(dx, dy, dz, x, y, z))</tt>.</p>
     *
     * @param dx x-component of the unit direction vector <b>d</b>.
     * @param dy y-component of the unit direction vector <b>d</b>.
     * @param dz z-component of the unit direction vector <b>d</b>.
     * @param x  x-coordinate of the point.
     * @param y  y-coordinate of the point.
     * @param z  z-coordinate of the point.
     */
    public static double distanceToStraight(double dx, double dy, double dz, double x, double y, double z) {
        return Math.sqrt(distanceToStraightSquare(dx, dy, dz, x, y, z));
    }


    // Called via reflection in the test for debugging needs; must be private
    private void setDirectionComponents(double dx, double dy, double dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}
