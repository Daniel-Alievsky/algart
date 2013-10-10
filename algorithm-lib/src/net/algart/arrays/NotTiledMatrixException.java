package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by some methods of {@link Matrix} interface,
 * if the matrix is not {@link Matrix#isTiled() tiled}.
 * For example, this exception is thrown for non-tiled matrices by
 * {@link Matrix#tileParent()} and {@link Matrix#tileDimensions()} methods.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class NotTiledMatrixException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public NotTiledMatrixException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public NotTiledMatrixException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 7997963759596154265L;
}
