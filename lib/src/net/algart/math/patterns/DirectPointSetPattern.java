package net.algart.math.patterns;

/**
 * <p>Interface, used by {@link Pattern} implementations to indicate that
 * they are <i>direct point-set patterns</i>, i&#46;e&#46; are internally represented as actual sets of points
 * like <tt>Set&lt;{@link net.algart.math.Point}&gt;</tt>.
 * See also the section "Direct point-set patterns" in the comments to {@link Pattern} interface.</p>
 *
 * <p>If a pattern implements this interface, then there is a guarantee that the following
 * methods work quickly and successfully (without any exceptions):</p>
 *
 * <ul>
 * <li>{@link #pointCount()},</li>
 * <li>{@link #largePointCount()},</li>
 * <li>{@link #isSurelySinglePoint()},</li>
 * <li>{@link #isSurelyOriginPoint()},</li>
 * <li>{@link #points()},</li>
 * <li>{@link #roundedPoints()},</li>
 * <li>{@link #coordRange(int)},</li>
 * <li>{@link #coordArea()},</li>
 * <li>{@link #coordMin()},</li>
 * <li>{@link #coordMax()},</li>
 * <li>{@link #roundedCoordRange(int)},</li>
 * <li>{@link #roundedCoordArea()},</li>
 * <li>{@link #round()},</li>
 * <li>{@link UniformGridPattern#gridIndexRange(int)} (if it is {@link DirectPointSetUniformGridPattern}),</li>
 * <li>{@link UniformGridPattern#gridIndexArea()} (if it is {@link DirectPointSetUniformGridPattern}),</li>
 * <li>{@link UniformGridPattern#gridIndexMin()} (if it is {@link DirectPointSetUniformGridPattern}),</li>
 * <li>{@link UniformGridPattern#gridIndexMax()} (if it is {@link DirectPointSetUniformGridPattern}).</li>
 * </ul>
 *
 * <p>Here "quickly" means <i>O</i>(1) operations for {@link #pointCount()}, {@link #largePointCount()},
 * {@link #isSurelyOriginPoint()} methods and <i>O</i>(<i>N</i>) or less operations
 * (<i>N</i>={@link #pointCount() pointCount()}) for other methods.</p>
 *
 * <p>Also there is a guarantee in such patterns, that
 * {@link #isPointCountVeryLarge()} returns <tt>false</tt> and, so, the number of points can be retrieved
 * by {@link #pointCount()} method.</p>
 *
 * <p>If a pattern implements this interface, it never implements {@link RectangularPattern} interface.</p>
 *
 * <p>There is a guarantee, that the following methods (and, in this package, only they)
 * create patterns, implementing this interface:</p>
 *
 * <ul>
 * <li>{@link SimplePattern} constructor,</li>
 * <li>{@link Patterns#newPattern(net.algart.math.Point...)},</li>
 * <li>{@link Patterns#newPattern(java.util.Collection)},</li>
 * <li>{@link Patterns#newUniformGridPattern(net.algart.math.Point, double[], java.util.Collection)},</li>
 * <li>{@link Patterns#newIntegerPattern(net.algart.math.IPoint...)},</li>
 * <li>{@link Patterns#newIntegerPattern(java.util.Collection)},</li>
 * <li>{@link Patterns#newSphereIntegerPattern(net.algart.math.Point, double)},</li>
 * <li>{@link Patterns#newEllipsoidIntegerPattern(net.algart.math.Point, double...)},</li>
 * <li>{@link Patterns#newSurface(Pattern, net.algart.math.functions.Func)},</li>
 * <li>{@link Patterns#newSpaceSegment Patterns.newSpaceSegment(UniformGridPattern, Func, Func, double, double)}.</li>
 * </ul>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 *
 * @see QuickPointCountPattern
 * @see DirectPointSetUniformGridPattern
 */
public interface DirectPointSetPattern extends QuickPointCountPattern {
}
