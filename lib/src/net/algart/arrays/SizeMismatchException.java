package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by some methods, processing several AlgART arrays or matrices,
 * when the passed arrays / matrices have different lengths / dimensions.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class SizeMismatchException extends IllegalArgumentException {
    /**
     * Constructs an instance of this class.
     */
    public SizeMismatchException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public SizeMismatchException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -7197201420951510150L;
}
