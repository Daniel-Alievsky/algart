/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.matrices.scanning;

/**
 * <p>Abstract wrapper of a boundary scanner:
 * special variant of {@link Boundary2DScanner} class, that redirects all methods to some
 * <i>parent</i> scanner and, maybe, performs some additional actions.</p>
 *
 * <p>All methods of this object and its inheritors call the same methods of the parent scanner
 * and return their results. In addition, the {@link #nextBoundary()}
 * and {@link #goTo} methods of this class
 * call {@link #resetCounters()} method after calling the method of the parent scanner.
 * An inheritor may add some other actions into any methods (usually into {@link #next()} method).</p>
 *
 * <p>Extending this class is a convenient way to process scanned boundaries without complication
 * of the basic scanning loop
 * (usually based on {@link #nextBoundary()} and {@link #scanBoundary(net.algart.arrays.ArrayContext)} calls).
 * The simplest example is {@link Boundary2DSimpleMeasurer} class, that adds measuring operation
 * to calling the parent {@link #next()} method.</p>
 *
 * @author Daniel Alievsky
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
    public boolean atMatrixBoundary() {
        return parent.atMatrixBoundary();
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
        resetCounters();
    }

    @Override
    public boolean get() {
        return parent.get();
    }

    @Override
    public boolean nextBoundary() {
        final boolean result = parent.nextBoundary();
        resetCounters();
        // - Important: we MUST call here this.resetCounters(), because the parent object does not know
        // anything about this one (it is composition, not subclassing!).
        // So, parent.resetCounters() will be called twice, from parent.nextBoundary() and from
        // this method resetCounters().
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

    @Override
    public long diagonalStepCount() {
        return parent.diagonalStepCount();
    }

    @Override
    public long rotationStepCount() {
        return parent.rotationStepCount();
    }

    @Override
    public long orientedArea() {
        return parent.orientedArea();
    }

    public void resetCounters() {
        parent.resetCounters();
    }
}