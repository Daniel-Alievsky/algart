package net.algart.math.functions;

class IdentityFunc implements Func {
    static class Updatable extends IdentityFunc implements Func.Updatable {
        Updatable() {
        }

        public void set(double[] x, double newResult) {
            x[0] = newResult;
        }

        /**
         * Returns a brief string description of this object.
         *
         * @return a brief string description of this object.
         */
        public String toString() {
            return "updatable " + super.toString();
        }
    }

    IdentityFunc() {
    }

    public double get(double ...x) {
        return x[0];
    }

    public double get() {
        throw new IndexOutOfBoundsException("At least 1 argument required");
    }

    public double get(double x0) {
        return x0;
    }

    public double get(double x0, double x1) {
        return x0;
    }

    public double get(double x0, double x1, double x2) {
        return x0;
    }

    public double get(double x0, double x1, double x2, double x3) {
        return x0;
    }

    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "identity function f(x)=x";
    }
}
