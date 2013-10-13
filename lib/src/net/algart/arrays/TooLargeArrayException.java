package net.algart.arrays;

/**
 * <p>Unchecked exception thrown if the current or desired array length is extremely large.
 * It is thrown by methods, creating new arrays or resizing existing arrays,
 * if the desired array length is too large (regardless of the amount of memory,
 * for example, greater than <tt>Long.MAX_VALUE</tt>),
 * or by methods trying to convert the array into a Java array (as {@link Arrays#toJavaArray(Array)}),
 * if the array is too large for storing its content in a form of Java array
 * (Java arrays can contain, as a maximum, <tt>Integer.MAX_VALUE</tt> (2<sup>31</sup>-1) elements).</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class TooLargeArrayException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public TooLargeArrayException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public TooLargeArrayException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 6967772412276478354L;
}
