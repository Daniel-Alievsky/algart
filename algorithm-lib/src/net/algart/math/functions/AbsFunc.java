package net.algart.math.functions;

final class AbsFunc implements Func {
    AbsFunc() {
    }

    public double get(double ...x) {
        return StrictMath.abs(x[0]);
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public double get(double x0) {
        return StrictMath.abs(x0);
    }

    public double get(double x0, double x1) {
        return StrictMath.abs(x0);
    }

    public double get(double x0, double x1, double x2) {
        return StrictMath.abs(x0);
    }

    public double get(double x0, double x1, double x2, double x3) {
        return StrictMath.abs(x0);
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "absolute value function f(x)=|x|";
    }
}
