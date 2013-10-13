package net.algart.matrices.stitching;

import net.algart.math.functions.Func;

public class FirstExceptingNaN implements Func {
    private final double defaultValue;

    private FirstExceptingNaN(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static FirstExceptingNaN getInstance(double defaultValue) {
        return new FirstExceptingNaN(defaultValue);
    }

    public double get(double... x) {
        for (double v : x) {
            if (v == v) { // not NaN
                return v;
            }
        }
        return defaultValue;
    }

    public double get() {
        return defaultValue;
    }

    public double get(double x0) {
        return x0 == x0 ? x0 : defaultValue;
    }

    public double get(double x0, double x1) {
        if (x0 == x0) {
            return x0;
        }
        if (x1 == x1) {
            return x1;
        }
        return defaultValue;
    }

    public double get(double x0, double x1, double x2) {
        if (x0 == x0) {
            return x0;
        }
        if (x1 == x1) {
            return x1;
        }
        if (x2 == x2) {
            return x2;
        }
        return defaultValue;
    }

    public double get(double x0, double x1, double x2, double x3) {
        if (x0 == x0) {
            return x0;
        }
        if (x1 == x1) {
            return x1;
        }
        if (x2 == x2) {
            return x2;
        }
        if (x3 == x3) {
            return x3;
        }
        return defaultValue;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "first not-NaN argument";
    }
}
