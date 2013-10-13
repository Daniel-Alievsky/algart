package net.algart.contexts;

/**
 * <p>The context allowing to inform the user about execution of a long-working method
 * via some status line or its analog.</p>
 *
 * <p>When possible, we recommend using {@link ProgressUpdater} instead, because that context
 * is more abstract and does not require an algorithm to provide textual information, as this one.
 * If you decide to process this context in some algorithm,
 * you should keep in mind that the algorithm may be used in different countries,
 * so the passed textual messages should either support localized versions for main languages,
 * or be very brief and simple.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface StatusUpdater extends Context {
    /**
     * Equivalent to {@link #updateStatus(String, boolean) updateStatus(message, true)}.
     *
     * <p>This method can throw <tt>NullPointerException</tt>, if its <tt>message</tt> argument is <tt>null</tt>,
     * but it is not guaranteed.
     *
     * @param message some information message; must not be <tt>null</tt>.
     */
    public void updateStatus(String message);

    /**
     * Shows some information message to the user.
     * The message should be one line and brief, to allow an application to show it inside a little status line.
     *
     * <p>The <tt>force</tt> argument determines whether this information must be
     * shown to the user with a guarantee. If this argument is <tt>false</tt>,
     * this method <i>may</i> do nothing to save processing time when very often calls.
     * Usual implementation of this method contains a time check (<tt>System.currentTimeMillis()</tt>)
     * and, if <tt>!force</tt>, does nothing if the previous call (for the same context)
     * was performed several tens of milliseconds ago.
     * Please avoid too frequent calls of this method with <tt>force=true</tt>:
     * millions of such calls may require long time.
     *
     * <p>This method can throw <tt>NullPointerException</tt>, if its <tt>message</tt> argument is <tt>null</tt>,
     * but it is not guaranteed.
     *
     * @param message some information message; must not be <tt>null</tt>.
     * @param force   whether this information must be shown always (<tt>true</tt>) or may be lost (<tt>false</tt>).
     */
    public void updateStatus(String message, boolean force);
}
