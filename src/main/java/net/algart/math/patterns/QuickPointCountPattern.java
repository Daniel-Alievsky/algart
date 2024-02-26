/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Interface, used by {@link Pattern} implementations to indicate that
 * they support quick access to the number of points in pattern.</p>
 *
 * <p>More precisely, if a pattern implements this interface, then there is a guarantee that the following
 * methods work very quickly (<i>O</i>(1) operations) and without any exceptions:</p>
 *
 * <ul>
 * <li>{@link #pointCount()},</li>
 * <li>{@link #largePointCount()},</li>
 * <li>{@link #isSurelySinglePoint()},</li>
 * <li>{@link #isSurelyOriginPoint()}.</li>
 * </ul>
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
 * <li>{@link Patterns#newSpaceSegment Patterns.newSpaceSegment(UniformGridPattern, Func, Func, double, double)},</li>
 * <li>{@link Patterns#newRectangularUniformGridPattern(net.algart.math.Point, double[], net.algart.math.IRange...)},
 * </li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IRange...)},</li>
 * <li>{@link Patterns#newRectangularIntegerPattern(net.algart.math.IPoint, net.algart.math.IPoint)}.</li>
 * </ul>
 *
 * @author Daniel Alievsky
 *
 * @see DirectPointSetPattern
 */
public interface QuickPointCountPattern extends Pattern {
    /**
     * Returns <tt>true</tt> if and only if the number of points in this pattern is greater
     * than <tt>Long.MAX_VALUE</tt>. In this case, {@link #pointCount()} returns <tt>Long.MAX_VALUE</tt>,
     * but you can get the approximate number of points by {@link #largePointCount()} method.
     *
     * <p>There is a guarantee that this method works very quickly (<i>O</i>(1) operations).
     * This method never throws any exceptions.
     *
     * @return <tt>true</tt> if the number of points in this pattern is greater than <tt>Long.MAX_VALUE</tt>.
     */
    public boolean isPointCountVeryLarge();
}
