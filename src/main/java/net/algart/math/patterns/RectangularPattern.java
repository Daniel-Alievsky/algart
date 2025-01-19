/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.patterns;

import net.algart.math.IPoint;
import net.algart.math.Point;

/**
 * <p>Interface, used by {@link Pattern} implementations to indicate that
 * they are <i>rectangular patterns</i>, i&#46;e&#46;
 * consist of all points of some uniform grid inside some hyperparallelepiped.
 * (For 2D patterns it means a rectangle, for 1D pattern it means an interval.)
 * This interface is a subinterface of {@link UniformGridPattern}: all rectangular patterns
 * are also uniform-grid.
 * See also the section "Rectangular patterns" in the comments to {@link Pattern} interface.</p>
 *
 * <p>More precisely, a pattern, implementing this interface, is a non-empty set of all points
 * <b>x</b><sup>(<i>k</i>)</sup> = (<i>x</i><sub>0</sub><sup>(<i>k</i>)</sup>,
 * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup>, ...,
 * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup>)
 * (<i>n</i>={@link #dimCount()}), such that:
 *
 * <blockquote>
 * <i>x</i><sub>0</sub><sup>(<i>k</i>)</sup> =
 * <i>o</i><sub>0</sub> + <i>i</i><sub>0</sub><sup>(<i>k</i>)</sup><i>d</i><sub>0</sub><br>
 * <i>x</i><sub>1</sub><sup>(<i>k</i>)</sup> =
 * <i>o</i><sub>1</sub> + <i>i</i><sub>1</sub><sup>(<i>k</i>)</sup><i>d</i><sub>1</sub><br>
 * . . .<br>
 * <i>x</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup> =
 * <i>o</i><sub><i>n</i>&minus;1</sub>
 * + <i>i</i><sub><i>n</i>&minus;1</sub><sup>(<i>k</i>)</sup><i>d</i><sub><i>n</i>&minus;1</sub>,<br>
 * <i>min</i><sub><i>j</i></sub> &le; <i>i</i><sub><i>j</i></sub><sup>(<i>k</i>)</sup> &le;
 * <i>max</i><sub><i>j</i></sub>
 * for all <i>j</i>=0,1,...,<i>n</i>&minus;1,
 * </blockquote>
 *
 * <p>where <i>o<sub>j</sub></i> are coordinates of the {@link #originOfGrid() origin of the grid},
 * <i>d<sub>j</sub></i> are {@link #stepsOfGrid() steps of the grid},
 * and the integer numbers <i>min</i><sub><i>j</i></sub> / <i>max</i><sub><i>j</i></sub> are
 * coordinates of two integer points, specified while creating the pattern.
 * Moreover, these parameters (<i>o<sub>j</sub></i>, <i>d<sub>j</sub></i>,
 * <i>min</i><sub><i>j</i></sub> / <i>max</i><sub><i>j</i></sub>)
 * <b>are stored inside the object and can be quickly read at any time
 * by {@link #gridIndexMin()}, {@link #gridIndexMax()} and similar methods</b>
 * &mdash; this condition is a requirement for all implementations of this interface.</p>
 *
 * <p>Note: not only patterns, implementing this interface, can be such sets of points.
 * In particular, you can construct an analogous set by {@link Patterns#newPattern(net.algart.math.Point...)}
 * method, and it will not implement this interface. If a {@link UniformGridPattern uniform-grid pattern}
 * does not implement this interface, you can still try to detect, whether it is really a rectangular parallelepiped,
 * by {@link #isActuallyRectangular()} method.</p>
 *
 * <p>If a pattern implements this interface, then there is a guarantee that the following
 * methods work very quickly (<i>O</i>(1) or <i>O</i>({@link #dimCount() dimCount()}) operations):</p>
 *
 * <ul>
 * <li>{@link #pointCount()},</li>
 * <li>{@link #largePointCount()},</li>
 * <li>{@link #isSurelySinglePoint()},</li>
 * <li>{@link #isSurelyOriginPoint()},</li>
 * <li>{@link #coordRange(int)},</li>
 * <li>{@link #coordArea()},</li>
 * <li>{@link #coordMin()},</li>
 * <li>{@link #coordMax()},</li>
 * <li>{@link #roundedCoordRange(int)},</li>
 * <li>{@link #roundedCoordArea()},</li>
 * <li>{@link #round()},</li>
 * <li>{@link #gridIndexRange(int)},</li>
 * <li>{@link #gridIndexArea()},</li>
 * <li>{@link #gridIndexMin()},</li>
 * <li>{@link #gridIndexMax()},</li>
 * <li>{@link #isActuallyRectangular()} (this method just returns <code>true</code> immediately),</li>
 * <li>{@link #lowerSurface(int)},</li>
 * <li>{@link #upperSurface(int)},</li>
 * <li>{@link #surface()},</li>
 * <li>{@link #minBound(int)},</li>
 * <li>{@link #maxBound(int)},</li>
 * <li>{@link #carcass()}.</li>
 * </ul>
 *
 * <p>Also in such patterns there is a guarantee, that all these methods work successfully,
 * i.e. without risk to throw {@link TooManyPointsInPatternError} or <code>OutOfMemoryError</code>.</p>
 *
 * <p>If a pattern implements this interface, it never implements {@link DirectPointSetPattern} interface.</p>
 *
 * <p>There is a guarantee, that the following methods (and, in this package, only they)
 * create patterns, implementing this interface:</p>
 *
 * <ul>
 * <li>{@link Patterns#newRectangularUniformGridPattern(net.algart.math.Point, double[], net.algart.math.IRange...)},
 * </li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IRange...)},</li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IPoint, net.algart.math.IPoint)}.</li>
 * </ul>
 *
 * @author Daniel Alievsky
 * @see #isActuallyRectangular()
 * @see QuickPointCountPattern
 */
public interface RectangularPattern extends UniformGridPattern, QuickPointCountPattern {
    RectangularPattern shift(Point shift);

    RectangularPattern symmetric();

    RectangularPattern multiply(double multiplier);

    RectangularPattern scale(double... multipliers);

    RectangularPattern projectionAlongAxis(int coordIndex);

    RectangularPattern minBound(int coordIndex);

    RectangularPattern maxBound(int coordIndex);

    RectangularPattern gridIndexPattern();

    RectangularPattern shiftGridIndexes(IPoint shift);

    RectangularPattern lowerSurface(int coordIndex);

    RectangularPattern upperSurface(int coordIndex);
}
