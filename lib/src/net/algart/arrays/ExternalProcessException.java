package net.algart.arrays;

import java.io.IOException;

/**
 * <p>Unchecked exception thrown by {@link ExternalProcessor#execute(ProcessBuilder)} method,
 * if the called external process is finished with non-zero OS exit code.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class ExternalProcessException extends IOException {
    private final int externalProcessExitCode;
    private final String externalProcessErrorMessage;

    /**
     * Constructs an instance of this class with the specified exit code, error message of the called program
     * and detail message of this exception.
     *
     * @param externalProcessExitCode     the exit code of the external process.
     * @param externalProcessErrorMessage the summary error message: the sequence of lines (separated by
     *                                    <tt>String.format("%n")</tt>), printed to the error stream
     *                                    of the called external process.
     * @param message                     the detail message for this exception
     *                                    (an argument of <tt>RuntimeException</tt> constructor).
     */
    public ExternalProcessException(int externalProcessExitCode, String externalProcessErrorMessage, String message) {
        super(message);
        this.externalProcessExitCode = externalProcessExitCode;
        this.externalProcessErrorMessage = externalProcessErrorMessage;
    }

    /**
     * Returns the exit code of the external process, the execution of which led to throwing this exception.
     * It is the (non-zero) value, returned by <tt>java.lang.Process.waitFor()</tt> method
     * at the end of execution of {@link ExternalProcessor#execute(ProcessBuilder)} method.
     *
     * <p>It is the first argument of the constructor.
     *
     * @return the exit code of the external process
     */
    public int getExternalProcessExitCode() {
        return externalProcessExitCode;
    }

    /**
     * Returns the summary error message of the external process, the execution of which led to throwing
     * this exception.
     * It is the sequence of lines (separated by <tt>String.format("%n")</tt>), printed to the error stream
     * of the called external process.
     *
     * <p>It is the second argument of the constructor.
     *
     * @return the summary error message of the external process.
     */
    public String getExternalProcessErrorMessage() {
        return externalProcessErrorMessage;
    }

    private static final long serialVersionUID = 4491691670648169355L;
}
