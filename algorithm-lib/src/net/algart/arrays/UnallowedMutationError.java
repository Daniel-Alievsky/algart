package net.algart.arrays;

/**
 * <p>Unchecked error thrown if the elements of {@link Array#asTrustedImmutable() trusted immutable}
 * AlgART arrays have been changed.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class UnallowedMutationError extends Error {
    /**
     * Constructs an instance of this class.
     */
    public UnallowedMutationError() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public UnallowedMutationError(String message) {
        super(message);
    }

    private static final long serialVersionUID = -8608272027574159505L;
}
