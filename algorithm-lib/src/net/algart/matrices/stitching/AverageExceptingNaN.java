package net.algart.matrices.stitching;

import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;

public strictfp class AverageExceptingNaN implements Func {
    private final double defaultValue;

    private AverageExceptingNaN(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static AverageExceptingNaN getInstance(double defaultValue) {
        return new AverageExceptingNaN(defaultValue);
    }

    public double get(double... x) {
        int n = 0;
        double sum = 0.0;
        for (double v : x) {
            if (v == v) { // not NaN
                sum += v;
                n++;
            }
        }
        return n == 0 ? defaultValue : sum / n;
    }

    public double get() {
        return defaultValue;
    }

    public double get(double x0) {
        return x0 == x0 ? x0 : defaultValue;
    }

    public double get(double x0, double x1) {
        return x0 == x0 ?
            (x1 == x1 ? 0.5 * (x0 + x1) : x0) :
            (x1 == x1 ? x1 : defaultValue);
    }

    public double get(double x0, double x1, double x2) {
        int n = x0 == x0 ? 1 : 0;
        double sum = x0 == x0 ? x0 : 0.0;
        if (x1 == x1) {
            sum += x1;
            n++;
        }
        if (x2 == x2) {
            sum += x2;
            n++;
        }
        return n == 0 ? defaultValue : sum / n;
    }

    public double get(double x0, double x1, double x2, double x3) {
        int n = x0 == x0 ? 1 : 0;
        double sum = x0 == x0 ? x0 : 0.0;
        if (x1 == x1) {
            sum += x1;
            n++;
        }
        if (x2 == x2) {
            sum += x2;
            n++;
        }
        if (x3 == x3) {
            sum += x3;
            n++;
        }
        return n == 0 ? defaultValue : sum / n;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "average function skipping NaN arguments";
    }
}
