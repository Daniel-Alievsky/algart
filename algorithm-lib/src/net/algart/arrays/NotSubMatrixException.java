package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by some methods of {@link Matrix} interface,
 * if the matrix is not a {@link Matrix#isSubMatrix() submatrix} of another matrix.
 * For example, this exception is thrown for matrices, which are not submatrices of another ones, by
 * {@link Matrix#subMatrixParent()}, {@link Matrix#subMatrixFrom()},
 * {@link Matrix#subMatrixTo()} or {@link Matrix#subMatrixContinuationMode()} methods.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class NotSubMatrixException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public NotSubMatrixException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public NotSubMatrixException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -4789384790151600143L;
}
