package net.algart.math.geometry;

import net.algart.math.MutableInt128;

/**
 * <p>Functions for checking collinearity of vectors.</p>
 *
 * @author Daniel Alievsky
 */
public class Collinearity {
    /**
     * Returns <tt>true</tt> if and only if two collinear 3D vectors (x1,y1,z1) and (x2,y2,z2) are collinear.
     *
     * <p>This method is the equivalent of {@link #collinear(long, long, long, long, long, long)} method,
     * optimized for the case of 32-bit <tt>int</tt> arguments.</p>
     *
     * @param x1 x-component of the first vector.
     * @param y1 y-component of the first vector.
     * @param z1 z-component of the first vector.
     * @param x2 x-component of the second vector.
     * @param y2 y-component of the second vector.
     * @param z2 z-component of the second vector.
     * @return whether two vectors are collinear.
     */

    public static boolean collinear(int x1, int y1, int z1, int x2, int y2, int z2) {
        return (long) y1 * (long) z2 == (long) z1 * (long) y2
                && (long) z1 * (long) x2 == (long) x1 * (long) z2
                && (long) x1 * (long) y2 == (long) y1 * (long) x2;
    }

    /**
     * Returns <tt>true</tt> if and only if two collinear 3D vectors (x1,y1,z1) and (x2,y2,z2) are collinear.
     *
     * <p>Note that this method allocates two temporary {@link MutableInt128} objects.
     * If you need to call it more than once, you can optimize this by allocating these two objects
     * in your code and reusing them many times with help of the method
     * {@link #collinear(long, long, long, long, long, long, MutableInt128, MutableInt128)}.</p>
     *
     * @param x1 x-component of the first vector.
     * @param y1 y-component of the first vector.
     * @param z1 z-component of the first vector.
     * @param x2 x-component of the second vector.
     * @param y2 y-component of the second vector.
     * @param z2 z-component of the second vector.
     * @return whether two vectors are collinear.
     */
    public static boolean collinear(long x1, long y1, long z1, long x2, long y2, long z2) {
        return collinear(x1, y1, z1, x2, y2, z2, new MutableInt128(), new MutableInt128());
    }

    /**
     * Returns <tt>true</tt> if and only if two collinear 3D vectors (x1,y1,z1) and (x2,y2,z2) are collinear.
     *
     * <p>Unlike {@link #collinear(long, long, long, long, long, long)},
     * this method requires to pass two non-null objects {@link MutableInt128}:
     * it will use them as working memory.
     * Thus, this method does not allocate new objects and is faster.</p>
     *
     * @param x1 x-component of the first vector.
     * @param y1 y-component of the first vector.
     * @param z1 z-component of the first vector.
     * @param x2 x-component of the second vector.
     * @param y2 y-component of the second vector.
     * @param z2 z-component of the second vector.
     * @param temp1 some non-null temporary instance of {@link MutableInt128}.
     * @param temp2 some other non-null temporary instance of {@link MutableInt128}.
     * @return whether two vectors are collinear.
     * @throws NullPointerException if <tt>temp1</tt> or <tt>temp2</tt> is <tt>null</tt>.
     */
    public static boolean collinear(
            long x1, long y1, long z1, long x2, long y2, long z2,
            MutableInt128 temp1, MutableInt128 temp2) {
        temp1.setToLongLongProduct(y1, z2);
        temp2.setToLongLongProduct(z1, y2);
        if (!temp1.equals(temp2)) {
            return false;
        }
        temp1.setToLongLongProduct(z1, x2);
        temp2.setToLongLongProduct(x1, z2);
        if (!temp1.equals(temp2)) {
            return false;
        }
        temp1.setToLongLongProduct(x1, y2);
        temp2.setToLongLongProduct(y1, x2);
        return temp1.equals(temp2);
    }

    /**
     * Returns <tt>true</tt> if two collinear 3D vectors (x1,y1,z1) and (x2,y2,z2) are also co-directional.
     * Note that the result has no sense for non-collinear vectors: you should check this fact separately,
     * for example, using  {@link #collinear(long, long, long, long, long, long)} method.
     *
     * @param x1 x-component of the first vector.
     * @param y1 y-component of the first vector.
     * @param z1 z-component of the first vector.
     * @param x2 x-component of the second vector, collinear to the first.
     * @param y2 y-component of the second vector, collinear to the first.
     * @param z2 z-component of the second vector, collinear to the first.
     * @return whether two vectors are co-directional.
     */
    public static boolean alsoCodirectional(long x1, long y1, long z1, long x2, long y2, long z2) {
        return (x1 < 0) == (x2 < 0) && (y1 < 0) == (y2 < 0) && (z1 < 0) == (z2 < 0);
    }
}
