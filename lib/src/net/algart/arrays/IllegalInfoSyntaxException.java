package net.algart.arrays;

/**
 * <p>Checked exception thrown if the format of <tt>byte[]</tt> or <tt>String</tt>
 * serialized form of the {@link MatrixInfo} is invalid.
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class IllegalInfoSyntaxException extends Exception {
    /**
     * Constructs an instance of this class.
     */
    public IllegalInfoSyntaxException() {
    }
    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public IllegalInfoSyntaxException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -7581665320134679678L;
}
