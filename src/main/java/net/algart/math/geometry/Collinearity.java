package net.algart.math.geometry;

import net.algart.math.MutableInt128;

public class Collinearity {
    public static boolean alsoCodirectional(long x1, long y1, long z1, long x2, long y2, long z2) {
        return (x1 < 0) == (x2 < 0) && (y1 < 0) == (y2 < 0) && (z1 < 0) == (z2 < 0);
    }

    public static boolean collinear(int x1, int y1, int z1, int x2, int y2, int z2) {
        return (long) y1 * (long) z2 == (long) z1 * (long) y2
                && (long) z1 * (long) x2 == (long) x1 * (long) z2
                && (long) x1 * (long) y2 == (long) y1 * (long) x2;
    }

    public static boolean collinear(long x1, long y1, long z1, long x2, long y2, long z2) {
        return collinear(x1, y1, z1, x2, y2, z2, new MutableInt128(), new MutableInt128());
    }

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
}
