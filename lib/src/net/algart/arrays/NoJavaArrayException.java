package net.algart.arrays;

/**
 * <p>Unchecked exception thrown by {@link DirectAccessible#javaArray()} method,
 * if the object cannot be viewed as a Java array.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class NoJavaArrayException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public NoJavaArrayException() {
    }

    private static final long serialVersionUID = -3038714244503245322L;
}
