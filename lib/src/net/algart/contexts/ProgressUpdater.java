package net.algart.contexts;

/**
 * <p>The context allowing to inform the user about the percents of some long-working method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ProgressUpdater extends Context {
    /**
     * Informs the user that <tt>readyPart*100</tt> percents of calculations are done.
     *
     * <p>The <tt>force</tt> argument determines whether this information must be
     * shown to the user with a guarantee. If this argument is <tt>false</tt>,
     * this method <i>may</i> do nothing to save processing time if it's called very often.
     * Usual implementation of this method contains a time check (<tt>System.currentTimeMillis()</tt>)
     * and, if <tt>!force</tt>, does nothing if the previous call (for the same context)
     * was performed several tens of milliseconds ago.
     * Please avoid too frequent calls of this method with <tt>force=true</tt>:
     * millions of such calls may require long time.
     *
     * <p>For example, if the main work of some method is a long loop, this method
     * can be called in the following manner:
     * <pre>
     * &#32;   for (int k = 0; k < n; k++) {
     * &#32;       . . . // some long calculations
     * &#32;       pu.{@link #updateProgress updateProgress}((k + 1.0) / n, k == n - 1);
     * &#32;       // when k==n-1, we always show the last progress state (100%)
     * &#32;   }
     * &#32;
     * </pre>
     *
     * @param readyPart the part of calculations that is already done (from 0.0 to 1.0).
     * @param force     whether this information must be shown always (<tt>true</tt>) or may be lost (<tt>false</tt>).
     */
    public void updateProgress(double readyPart, boolean force);
}
