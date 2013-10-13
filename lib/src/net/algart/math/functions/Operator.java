package net.algart.math.functions;

/**
 * <p>Abstract operator: a transformation from one {@link Func mathematical function} to another.
 *
 * <p>Implementations of this interface are usually <b>immutable</b> and
 * always <b>thread-safe</b>: {@link #apply apply} method of this interface may be freely used
 * while simultaneous accessing the same instance from several threads.
 * All implementations of this interface from this package are <b>immutable</b>.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface Operator {
    /**
     * Identity operator, transforming any function to itself.
     * The {@link #apply(Func)} method of this operator returns the reference to its argument.
     */
    public static final Operator IDENTITY = new IdentityOperator();

    /**
     * Returns the result of applying this operator to the given function.
     *
     * @param f some function.
     * @return  new transformed function.
     */
    public Func apply(Func f);
}
