package net.algart.math.geom;

/**
 * <p>2D segment</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.0
 * @since JDK 1.0
 */
public class Segment2D extends UnsafeSegment2D {

    Segment2D() { // can be inherited in this package only
    }

    /**
     * Creates some "default" segment. The parameters of this segment (center, orientation, length)
     * are not specified. So, you must set all segment parameters after creating such instance.
     */
    public static Segment2D getInstance() {
        return new Segment2D();
    }

    /**
     * Creates new segment, identical to <tt>instance</tt>.
     */
    public static Segment2D getInstance(UnsafeSegment2D instance) {
        Segment2D result = new Segment2D();
        result.copyFrom(instance);
        return result;
    }

    /**
     * Creates new segment; slowly, because requires sine and cosine calculation.
     */
    public static Segment2D getInstance(double centerX, double centerY, double length, double fi) {
        Segment2D result = new Segment2D();
        result.setAll(centerX, centerY, length, fi);
        return result;
    }

    /**
     * Creates new segment.
     */
    public static Segment2D getInstance(double centerX, double centerY, double length, Direction2D normal) {
        Segment2D result = new Segment2D();
        result.setAll(centerX, centerY, length, normal);
        return result;
    }

    /**
     * Equivalent to setFi(fi) (nx and ny are ignored, unlike an implementation in UnsafeSegment2D).
     * This method works slowly, because requires sine and cosine calculation.
     */
    public void setFi(double fi, double nx, double ny) {
        setFi(fi);
    }

    /**
     * Equivalent to setAll(centerX, centerY, length, fi)
     * (nx and ny are ignored, unlike an implementation in UnsafeSegment2D).
     * This method works slowly, because requires sine and cosine calculation.
     */
    public void setAll(double centerX, double centerY, double length, double fi, double nx, double ny) {
        setAll(centerX, centerY, length, fi);
    }

    public Direction2D getNormalClone() {
        return (Direction2D)normal.clone();
    }

    public void copyNormalTo(Direction2D normal) {
        normal.copyFrom(this.normal);
    }

    public void copyFrom(UnsafeSegment2D other) {
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.length = other.length;
        if (other instanceof Segment2D)
            this.normal.copyFrom(other.normal);
        else
            this.normal.setAngle(other.normal.getAngle());
    }

    /**
     * Should does nothing (unlike an implementation in {@link UnsafeSegment2D}).
     * However, it will generate InternalError if there are some bugs in this package.
     */
    public void checkInvariants() {
        try {
            super.checkInvariants();
        } catch (IllegalStateException e) {
            throw new InternalError(e.toString());
        }
    }
}
