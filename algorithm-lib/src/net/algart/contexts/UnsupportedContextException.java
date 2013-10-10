package net.algart.contexts;

/**
 * <p>Unchecked exception thrown by {@link Context#as(Class)} method
 * when it cannot serve the request.
 * Usually it means that the context does not implement the required interface
 * and does not contain inside any other context that could be returned.
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class UnsupportedContextException extends RuntimeException {
    /**
     * Constructs an instance of this class.
     */
    public UnsupportedContextException() {
    }

    /**
     * Constructs an instance of this class with the specified detail message.
     *
     * @param message the detail message.
     */
    public UnsupportedContextException(String message) {
        super(message);
    }

    /**
     * Constructs an instance of this class with the message informing that the given context class is not supported.
     *
     * @param contextClass some unsupported context class.
     */
    public UnsupportedContextException(Class<?> contextClass) {
        super("Unsupported context class: " + contextClass.getName());
    }

    private static final long serialVersionUID = -869003795584365428L;
}
