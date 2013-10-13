package net.algart.math.patterns;

public class TooLargePatternCoordinatesException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public TooLargePatternCoordinatesException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public TooLargePatternCoordinatesException(String message) {
        super(message);
    }

    static final long serialVersionUID = -2153895144415795724L;
}
