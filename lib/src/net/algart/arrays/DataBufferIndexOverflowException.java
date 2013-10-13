package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by {@link DataBuffer} methods {@link DataBuffer#from() from()},
 * {@link DataBuffer#to() to()} and {@link DataBuffer#cnt() cnt()}, if the values, they should be returned
 * by these methods, are greater than <tt>Integer.MAX_VALUE</tt>.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class DataBufferIndexOverflowException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public DataBufferIndexOverflowException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public DataBufferIndexOverflowException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 244582200268913586L;
}
