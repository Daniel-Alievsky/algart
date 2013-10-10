package net.algart.matrices.stitching;

import net.algart.math.functions.Func;

public strictfp class MinExceptingNaN implements Func {
    private final double defaultValue;

    private MinExceptingNaN(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static MinExceptingNaN getInstance(double defaultValue) {
        return new MinExceptingNaN(defaultValue);
    }

    public double get(double... x) {
        boolean valid = false;
        double min = Double.POSITIVE_INFINITY;
        for (double v : x) {
            if (v == v) { // not NaN
                if (v < min) {
                    min = v;
                }
                valid = true;
            }
        }
        return valid ? min : defaultValue;
    }

    public double get() {
        return defaultValue;
    }

    public double get(double x0) {
        return x0 == x0 ? x0 : defaultValue;
    }

    public double get(double x0, double x1) {
        return x0 == x0 ?
            (x1 == x1 ? (x0 < x1 ? x0 : x1) : x0) :
            (x1 == x1 ? x1 : defaultValue);
    }

    public double get(double x0, double x1, double x2) {
        boolean valid = false;
        if (x0 == x0) {
            valid = true;
        } else {
            x0 = Double.POSITIVE_INFINITY;
        }
        if (x1 == x1) {
            valid = true;
        } else {
            x1 = Double.POSITIVE_INFINITY;
        }
        if (x2 == x2) {
            valid = true;
        } else {
            x2 = Double.POSITIVE_INFINITY;
        }
        if (!valid) {
            return defaultValue;
        }
        double x = x0 <= x1 ? x0 : x1;
        return x <= x2 ? x : x2;
    }

    public double get(double x0, double x1, double x2, double x3) {
        boolean valid = false;
        if (x0 == x0) {
            valid = true;
        } else {
            x0 = Double.POSITIVE_INFINITY;
        }
        if (x1 == x1) {
            valid = true;
        } else {
            x1 = Double.POSITIVE_INFINITY;
        }
        if (x2 == x2) {
            valid = true;
        } else {
            x2 = Double.POSITIVE_INFINITY;
        }
        if (x3 == x3) {
            valid = true;
        } else {
            x3 = Double.POSITIVE_INFINITY;
        }
        if (!valid) {
            return defaultValue;
        }
        double x = x0 <= x1 ? x0 : x1;
        double y = x2 <= x3 ? x2 : x3;
        return x <= y ? x : y;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "minimum function skipping NaN arguments";
    }
}
