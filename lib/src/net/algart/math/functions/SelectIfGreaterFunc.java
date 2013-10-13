package net.algart.math.functions;

final class SelectIfGreaterFunc implements Func {
    SelectIfGreaterFunc() {
    }

    public double get(double ...x) {
        return x[0] > x[1] ? x[2] : x[3];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 4 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 4 arguments required");
    }

    public double get(double x0, double x1) {
        throw new IndexOutOfBoundsException("At least 4 arguments required");
    }

    public double get(double x0, double x1, double x2) {
        throw new IndexOutOfBoundsException("At least 4 arguments required");
    }

    public double get(double x0, double x1, double x2, double x3) {
        return x0 > x1 ? x2 : x3;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "select function f(x,y,u,w)=x>y?u:w";
    }
}
