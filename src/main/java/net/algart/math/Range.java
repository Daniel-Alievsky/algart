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

package net.algart.math;

import java.util.Objects;

/**
 * <p>Numeric inclusive real range:
 * a set of <code>double</code> numbers <code>{@link #min() min()}&lt;=<i>x</i>&lt;={@link #max() max()}</code>.
 * An advanced analog of
 * <a href="http://commons.apache.org/lang/api-2.4/org/apache/commons/lang/math/DoubleRange.html"
 * id="dummy">org.apache.commons.lang.math.DoubleRange</a>.
 *
 * <p>The minimum number ({@link #min()}) is never greater than the maximum number ({@link #max()}),
 * and both numbers are never <code>Double.NaN</code>.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see IRange
 */
public final class Range {
    final double min;
    final double max;

    Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns an instance of this class describing the range
     * <code>min&lt;=<i>x</i>&lt;=max</code>.
     * The <code>min</code> value must not be greater than <code>max</code>,
     * and both arguments must not be <code>Double.NaN</code>.
     *
     * @param min the minimum number in the range, inclusive.
     * @param max the maximum number in the range, inclusive.
     * @return    the new range.
     * @throws IllegalArgumentException if <code>min &gt; max</code> or one of the arguments is <code>Double.NaN</code>.
     */
    public static Range valueOf(double min, double max) {
        if (Double.isNaN(min)) {
            throw new IllegalArgumentException("min is NaN");
        }
        if (Double.isNaN(max)) {
            throw new IllegalArgumentException("max is NaN");
        }
        if (min > max) {
            throw new IllegalArgumentException("min > max (min = " + min + ", max = " + max + ")");
        }
        return new Range(min, max);
    }

    /**
     * Returns an instance of this class describing the same range as the given integer range.
     * The <code>long</code> boundaries of the passed integer range are converted
     * to <code>double</code> boundaries {@link #min()} and {@link #max()} of the returned range by standard
     * Java typecast <code>(double)longValue</code>.
     *
     * @param iRange the integer range.
     * @return       the equivalent real range.
     * @throws NullPointerException if the passed integer range is {@code null}.
     */
    public static Range valueOf(IRange iRange) {
        Objects.requireNonNull(iRange, "Null iRange argument");
        return new Range(iRange.min, iRange.max);
    }

    /**
     * Returns the minimum number in the range, inclusive.
     *
     * @return the minimum number in the range.
     */
    public double min() {
        return this.min;
    }

    /**
     * Returns the maximum number in the range, inclusive.
     *
     * @return the maximum number in the range.
     */
    public double max() {
        return this.max;
    }

    /**
     * Returns <code>{@link #max()}-{@link #min()}</code>.
     *
     * @return <code>{@link #max()}-{@link #min()}</code>.
     */
    public double size() {
        return max - min;
    }

    /**
     * Returns <code>value&lt;{@link #min()}?{@link #min()}:value&gt;{@link #max()}?{@link #max()}:value</code>.
     * In other words, returns the passed number if it is in this range or the nearest range bound in other cases.
     *
     * @param value some number.
     * @return      the passed number if it is in this range or the nearest range bound in other cases.
     */
    public double cut(double value) {
        return value < min ? min : value > max ? max : value;
    }

    /**
     * Returns <code>true</code> if and only if <code>{@link #min()}&lt;=value&lt;={@link #max()}</code>
     *
     * @param value the checked value.
     * @return      <code>true</code> if the value is in this range.
     */
    public boolean contains(double value) {
        return min <= value && value <= max;
    }

    /**
     * Returns <code>true</code> if and only if <code>{@link #min()}&lt;=range.{@link #min()}</code>
     * and <code>range.{@link #max()}&lt;={@link #max()}</code>.
     *
     * @param range the checked range.
     * @return      <code>true</code> if the checked range is a subset of this range.
     */
    public boolean contains(Range range) {
        return min <= range.min && range.max <= max;
    }

    /**
     * Returns <code>true</code> if and only if <code>{@link #min()}&lt;=range.{@link #max()}</code>
     * and <code>range.{@link #min()}&lt;={@link #max()}</code>.
     *
     * @param range the checked range.
     * @return      <code>true</code> if the checked range overlaps with this range.
     */
    public boolean intersects(Range range) {
        return min <= range.max && range.min <= max;
    }

    /**
     * Returns an instance of this class describing the range
     * <code>StrictMath.min(this.{@link #min() min()},value) &lt;= x
     * &lt;= StrictMath.max(this.{@link #max() max()},value)</code>,
     * excepting the case when the passed value is <code>NaN</code> &mdash;
     * in the last situation, returns this instance without changes.
     * In other words, expands the current range to include the given value.
     *
     * @param value some value that should belong to the new range.
     * @return      the expanded range (or this range if <code>Double.isNaN(value)</code>).
     */
    public Range expand(double value) {
        if (Double.isNaN(value)) {
            return this;
        }
        if (value == 0.0) {
            double min = StrictMath.min(value, this.min);
            double max = StrictMath.max(value, this.max);
            return new Range(min, max);
        } else {
            double min = value < this.min ? value : this.min;
            double max = value > this.max ? value : this.max;
            return new Range(min, max);
        }
    }

    /**
     * Equivalent to <code>{@link IRange#valueOf(Range) IRange.valueOf}(thisInstance)</code>,
     * with the only difference that <code>IllegalStateException</code> is thrown instead of
     * <code>IllegalArgumentException</code> for unallowed range.
     *
     * @return the integer range with same (cast) bounds.
     * @throws IllegalStateException in the same situations when
     *                               {@link IRange#valueOf(Range)} method throws <code>IllegalArgumentException</code>.
     */
    public IRange toIntegerRange() {
        return IRange.valueOf((long) min, (long) max, true);
    }

    /**
     * Equivalent to <code>{@link IRange#roundOf(Range) IRange.roundOf}(thisInstance)</code>,
     * with the only difference that <code>IllegalStateException</code> is thrown instead of
     * <code>IllegalArgumentException</code> for unallowed range.
     *
     * @return the integer range with same (rounded) bounds.
     * @throws IllegalStateException in the same situations when
     *                               {@link IRange#roundOf(Range)} method throws <code>IllegalArgumentException</code>.
     */
    public IRange toRoundedRange() {
        return IRange.valueOf(StrictMath.round(min), StrictMath.round(max), true);
    }

    /**
     * Returns a brief string description of this object.
     *
     * <p>The result of this method may depend on implementation and usually contains
     * the minimum and maximum numbers in this range.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return min + ".." + max;
    }

    /**
     * Returns the hash code of this range.
     *
     * @return the hash code of this range.
     */
    public int hashCode() {
        long lMin = Double.doubleToLongBits(min);
        long lMax = Double.doubleToLongBits(max);
        int iMin = (int)lMin * 37 + (int)(lMin >>> 32);
        int iMax = (int)lMax * 37 + (int)(lMax >>> 32);
        return iMin * 37 + iMax;
    }

    /**
     * Indicates whether some other range is equal to this instance.
     * Returns <code>true</code> if and only if <code>obj instanceof Range</code>,
     * <code>Double.doubleToLongBits(((Range)obj).min())==Double.doubleToLongBits(thisInstance.min())</code>
     * and <code>Double.doubleToLongBits(((Range)obj).max())==Double.doubleToLongBits(thisInstance.max())</code>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <code>true</code> if the specified object is a range equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof Range
            && Double.doubleToLongBits(((Range)obj).min) == Double.doubleToLongBits(this.min)
            && Double.doubleToLongBits(((Range)obj).max) == Double.doubleToLongBits(this.max);
    }
}
