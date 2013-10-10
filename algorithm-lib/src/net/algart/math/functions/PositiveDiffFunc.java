package net.algart.math.functions;

final strictfp class PositiveDiffFunc implements Func {
    PositiveDiffFunc() {
    }

    public double get(double ...x) {
        return x[0] - x[1] > 0.0 ? x[0] - x[1] : 0;
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0, double x1) {
        return x0 - x1 > 0.0 ? x0 - x1 : 0;
    }

    public double get(double x0, double x1, double x2) {
        return x0 - x1 > 0.0 ? x0 - x1 : 0;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return x0 - x1 > 0.0 ? x0 - x1 : 0;
    }


    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "positive difference function f(x,y)=max(x-y,0)";
    }
}
