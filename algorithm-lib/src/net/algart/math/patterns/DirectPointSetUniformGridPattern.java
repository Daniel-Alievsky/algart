package net.algart.math.patterns;

import net.algart.math.IPoint;
import net.algart.math.Point;

/**
 * <p>Interface, used by {@link Pattern} implementations to indicate that
 * they are simultaneously {@link DirectPointSetPattern} and {@link UniformGridPattern}.
 * In other words, a pattern implements this interface if and only if it is simultaneously
 * <i>direct point-set</i> and <i>uniform-grid</i>: see the corresponding sections in
 * the comments to {@link Pattern} interface.</p>
 *
 * <p>If a pattern implements this interface, it never implements {@link RectangularPattern} interface.</p>
 *
 * <p>There is a guarantee, that the following methods create patterns, implementing this interface:</p>
 *
 * <ul>
 * <li>{@link Patterns#newUniformGridPattern(net.algart.math.Point, double[], java.util.Collection)},</li>
 * <li>{@link Patterns#newIntegerPattern(net.algart.math.IPoint...)},</li>
 * <li>{@link Patterns#newIntegerPattern(java.util.Collection)},</li>
 * <li>{@link Patterns#newSphereIntegerPattern(net.algart.math.Point, double)},</li>
 * <li>{@link Patterns#newEllipsoidIntegerPattern(net.algart.math.Point, double...)},</li>
 * <li>{@link Patterns#newSpaceSegment Patterns.newSpaceSegment(UniformGridPattern, Func, Func, double, double)}.</li>
 * </ul>
 *
 * <p>The following methods can return an object, implementing this interface, and also an object,
 * not implementing this interface &mdash; it depends on their arguments:</p>
 *
 * <ul>
 * <li>{@link Patterns#newPattern(net.algart.math.Point...)},</li>
 * <li>{@link Patterns#newPattern(java.util.Collection)},</li>
 * </ul>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 *
 * @see QuickPointCountPattern
 */
public interface DirectPointSetUniformGridPattern extends DirectPointSetPattern, UniformGridPattern {
    public DirectPointSetUniformGridPattern shift(Point shift);

    public DirectPointSetUniformGridPattern symmetric();

    public DirectPointSetUniformGridPattern multiply(double multiplier);

    public DirectPointSetUniformGridPattern scale(double... multipliers);

    public DirectPointSetUniformGridPattern projectionAlongAxis(int coordIndex);

    public DirectPointSetUniformGridPattern minBound(int coordIndex);

    public DirectPointSetUniformGridPattern maxBound(int coordIndex);

    public DirectPointSetUniformGridPattern gridIndexPattern();

    public DirectPointSetUniformGridPattern shiftGridIndexes(IPoint shift);

    public DirectPointSetUniformGridPattern lowerSurface(int coordIndex);

    public DirectPointSetUniformGridPattern upperSurface(int coordIndex);
}
