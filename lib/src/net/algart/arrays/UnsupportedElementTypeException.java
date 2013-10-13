package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by methods, creating new AlgART arrays
 * (as {@link MemoryModel#newEmptyArray(Class)}), if the specified element type
 * is not supported by the {@link MemoryModel memory model}.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class UnsupportedElementTypeException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public UnsupportedElementTypeException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public UnsupportedElementTypeException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 2414151893941477245L;
}
