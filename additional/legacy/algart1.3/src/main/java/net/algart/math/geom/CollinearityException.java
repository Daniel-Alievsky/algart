package net.algart.math.geometry;

/**
 * Thrown when two collinear (or amost collinear) vectors or segments are detected,
 * but it is not permitted for the given situation.
 */
public final class CollinearityException extends RuntimeException {
    private static final long serialVersionUID = -181255282331453741L;

    /**
     * Constructs an instance of this class.
     */
    public CollinearityException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public CollinearityException(String message) {
        super(message);
    }
}
