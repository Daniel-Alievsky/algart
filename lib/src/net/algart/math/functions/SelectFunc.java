package net.algart.math.functions;

final class SelectFunc implements Func {
    SelectFunc() {
    }

    public double get(double ...x) {
        return x[(int)x[0] + 1];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0) {
        throw new IndexOutOfBoundsException("At least 2 arguments required");
    }

    public double get(double x0, double x1) {
        int i = (int)x0;
        if (i == 0)
            return x1;
        throw new IndexOutOfBoundsException("Illegal index " + i);
    }

    public double get(double x0, double x1, double x2) {
        int i = (int)x0;
        if (i == 0)
            return x1;
        if (i == 1)
            return x2;
        throw new IndexOutOfBoundsException("Illegal index " + i);
    }

    public double get(double x0, double x1, double x2, double x3) {
        int i = (int)x0;
        if (i == 0)
            return x1;
        if (i == 1)
            return x2;
        if (i == 2)
            return x3;
        throw new IndexOutOfBoundsException("Illegal index " + i);
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "select function f(x0,x1,...)=x[(int)x0+1]";
    }
}
