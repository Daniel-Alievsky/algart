package net.algart.contexts;

/**
 * <p>Unchecked analog of the standard <tt>InterruptedException</tt>.
 * Thrown by {@link InterruptionContext#checkInterruption()} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class InterruptionException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public InterruptionException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public InterruptionException(String message) {
        super(message);
    }

    /**
     * Constructs an instance of this class with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    public InterruptionException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 8166277608312778448L;
}
