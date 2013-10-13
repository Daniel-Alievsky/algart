package net.algart.arrays;

/**
 * <p>Unchecked exception thrown if the <i>n</i>-dimensional simplex {@link Matrices.Simplex}
 * cannot be constructed because all vertices lies on the same hyperplane.
 * In other words, it is thrown while attempt to build degenerated simplex: such simplexes are not allowed
 * for {@link Matrices.Simplex} class.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class DegeneratedSimplexException extends IllegalArgumentException {
    /**
     * Constructs an instance of this class.
     */
    public DegeneratedSimplexException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public DegeneratedSimplexException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 5347629842796910748L;
}
