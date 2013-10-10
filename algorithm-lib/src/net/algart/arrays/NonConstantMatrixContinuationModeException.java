package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by {@link Matrix.ContinuationMode#continuationConstant()} method,
 * if the continuation mode is not a {@link Matrix.ContinuationMode#isConstant() constant continuation}.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class NonConstantMatrixContinuationModeException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public NonConstantMatrixContinuationModeException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public NonConstantMatrixContinuationModeException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 2487762843006655047L;
}
