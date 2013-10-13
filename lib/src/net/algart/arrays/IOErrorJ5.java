package net.algart.arrays;

import java.lang.reflect.*;
import java.io.IOException;

/**
 * <p>This error is thrown instead of <tt>java.io.IOError</tt> under Java 1.5.
 * (<tt>java.io.IOError</tt> appeared in Java 1.6 only.)</p>
 *
 * <p><b>Warning: this class may be excluded in future versions of this library!</b>
 * In this case, you will need to replace all operators
 * "<tt>IOErrorJ5.getInstance(cause)</tt>" with "<tt>new&nbsp;IOError(cause)</tt>"
 * in your code.
 * If you not need compatibility with Java 1.5, please don't use this class:
 * under 1.6 or later versions, this library throw <tt>java.io.IOError</tt>.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class IOErrorJ5 extends Error {

    private IOErrorJ5(Throwable cause) {
        super(cause);
    }

    /**
     * Returns a new instance of this class under Java 1.5,
     * or <tt>java.io.IOError</tt> under Java 1.6.
     *
     * @param cause the cause of this error, or <tt>null</tt> if the cause is not known.
     * @return      new Error instance: this class or <tt>java.io.IOError</tt> is possible.
     */
    public static Error getInstance(Throwable cause) {
        Error result = null;
        try {
            Class<?> clazz = Class.forName("java.io.IOError"); // Java 1.6
            Constructor<?> constructor = clazz.getConstructor(Throwable.class);
            result = (Error)constructor.newInstance(cause);
        } catch (InvocationTargetException ex) {
        } catch (IllegalArgumentException ex) {
        } catch (IllegalAccessException ex) {
        } catch (InstantiationException ex) {
        } catch (SecurityException ex) {
        } catch (NoSuchMethodException ex) {
        } catch (ClassNotFoundException ex) {
        }
        if (result == null) // some exception: previous Java versions
            result = new IOErrorJ5(cause);
        result.fillInStackTrace();
        StackTraceElement[] stack = result.getStackTrace();
        stack = (StackTraceElement[])JArrays.copyOfRange(stack, 1, stack.length);
        result.setStackTrace(stack); // emulates direct throwing java.io.IOError
        return result;
    }

    static IOException getIOException(Error error) {
        if (error.getClass().getName().equals("java.io.IOError")
            || error instanceof IOErrorJ5) {
            Throwable cause = error.getCause();
            if (cause instanceof IOException)
                return (IOException)cause;
        }
        return null;
    }

    private static final long serialVersionUID = 6083384198859120376L;
}
