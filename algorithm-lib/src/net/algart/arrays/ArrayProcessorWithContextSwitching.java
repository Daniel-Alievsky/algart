package net.algart.arrays;

/**
 * <p>Array processor allowing to switch the current context.</p>
 *
 * <p>It is an extended version of {@link ArrayProcessor} interface.
 * Many algorithms should implement this interface instead of {@link ArrayProcessor}.
 * The reason is often necessity to extract a subtask of the full algorithmic task,
 * with a context corresponding to this subtask
 * ({@link ArrayContext#part(double, double)} method).
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayProcessorWithContextSwitching extends ArrayProcessor {

    /**
     * Switches the context: returns an instance, identical to this one excepting
     * that it uses the specified <tt>newContext</tt> for all operations.
     * The returned instance is usually a clone of this one, but there is no guarantees
     * that it is a deep clone.
     * Usually, the returned instance is used only for performing a
     * {@link ArrayContext#part(double, double) subtask} of the full task.
     *
     * @param newContext another context, used by the returned instance; may be <tt>null</tt>.
     * @return           new instance with another context.
     */
    public ArrayProcessorWithContextSwitching context(ArrayContext newContext);
}
