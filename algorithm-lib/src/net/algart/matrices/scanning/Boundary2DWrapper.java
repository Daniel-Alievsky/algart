package net.algart.matrices.scanning;

/**
 * <p>Abstract wrapper of a boundary scanner:
 * special variant of {@link Boundary2DScanner} class, that redirects all methods to some
 * <i>parent</i> scanner and, maybe, performs some additional actions.</p>
 *
 * <p>All methods of this object and its inheritors call the same methods of the parent scanner
 * and return their results. In addition, the {@link #nextBoundary()}
 * and {@link #goTo} methods of this class
 * call {@link #reset()} method after calling the method of the parent scanner.
 * An inheritor may add some other actions into any methods (usually into {@link #next()} method).</p>
 *
 * <p>Extending this class is a convenient way to process scanned boundaries without complication
 * of the basic scanning loop
 * (usually based on {@link #nextBoundary()} and {@link #scanBoundary(net.algart.arrays.ArrayContext)} calls).
 * The simplest example is {@link Boundary2DSimpleMeasurer} class, that adds measuring operation
 * to calling the parent {@link #next()} method.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public abstract class Boundary2DWrapper extends Boundary2DScanner {
    /**
     * The parent scanner: the methods of this object call the corresponding methods of the parent one.
     *
     * @see #parent()
     */
    protected final Boundary2DScanner parent;

    /**
     * Creates new instance of this class.
     *
     * @param parent the parent scanner, that will be stored in {@link #parent} field.
     * @throws NullPointerException if <tt>parent</tt> argument is <tt>null</tt>.
     */
    protected Boundary2DWrapper(Boundary2DScanner parent) {
        super(parent.matrix());
        this.parent = parent;
    }

    /**
     * Returns the {@link #parent} scanner.
     *
     * @return the parent scanner.
     */
    public final Boundary2DScanner parent() {
        return this.parent;
    }

    @Override
    public boolean isSingleBoundaryScanner() {
        return parent.isSingleBoundaryScanner();
    }

    @Override
    public boolean isAllBoundariesScanner() {
        return parent.isAllBoundariesScanner();
    }

    @Override
    public boolean isMainBoundariesScanner() {
        return parent.isMainBoundariesScanner();
    }

    @Override
    public ConnectivityType connectivityType() {
        return parent.connectivityType();
    }

    @Override
    public boolean isInitialized() {
        return parent.isInitialized();
    }

    @Override
    public boolean isMovedAlongBoundary() {
        return parent.isMovedAlongBoundary();
    }

    @Override
    public long x() {
        return parent.x();
    }

    @Override
    public long y() {
        return parent.y();
    }

    @Override
    public Side side() {
        return parent.side();
    }

    @Override
    public long nestingLevel() {
        return parent.nestingLevel();
    }

    @Override
    public long currentIndexInArray() {
        return parent.currentIndexInArray();
    }

    @Override
    public void goTo(long x, long y, Side side) {
        parent.goTo(x, y, side);
        reset();
    }

    @Override
    public boolean get() {
        return parent.get();
    }

    @Override
    public boolean nextBoundary() {
        boolean result = parent.nextBoundary();
        reset();
        return result;
    }

    @Override
    public void next() {
        parent.next();
    }

    @Override
    public Step lastStep() {
        return parent.lastStep();
    }

    @Override
    public boolean coordinatesChanged() {
        return parent.coordinatesChanged();
    }

    @Override
    public boolean boundaryFinished() {
        return parent.boundaryFinished();
    }

    @Override
    public long stepCount() {
        return parent.stepCount();
    }

    /**
     * This method is automatically called by {@link #nextBoundary()} and
     * {@link #goTo} methods
     * <i>after</i> calling the same methods of the parent scanner.
     * The default implementation does nothing.
     * This method can be overridden, for example, to initialize analysis of the next boundary
     * in the inheritor.
     */
    public void reset() {
    }
}