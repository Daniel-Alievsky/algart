package net.algart.math.functions;

final class IdentityOperator implements Operator {
    public Func apply(Func f) {
        return f;
    }

    public String toString() {
        return "identity operator";
    }
}
