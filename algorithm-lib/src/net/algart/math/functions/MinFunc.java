package net.algart.math.functions;

final class MinFunc implements Func {
    MinFunc() {
    }

    public double get(double ...x) {
        if (x.length == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double result = x[0];
        for (int k = 1; k < x.length; k++)
            if (x[k] < result)
                result = x[k];
        return result;
    }

    public double get() {
        return Double.POSITIVE_INFINITY;
    }

    public double get(double x0) {
        return x0;
    }

    public double get(double x0, double x1) {
        return x0 <= x1 ? x0 : x1;
    }

    public double get(double x0, double x1, double x2) {
        double x = x0 <= x1 ? x0 : x1;
        return x <= x2 ? x : x2;
    }

    public double get(double x0, double x1, double x2, double x3) {
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
        return "minimum function f(x0,x1,...)=min(x0,x1,...)";
    }
}
