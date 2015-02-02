/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Numeric inclusive real range:
 * a set of <tt>double</tt> numbers <tt>{@link #min() min()}&lt;=<i>x</i>&lt;={@link #max() max()}</tt>.
 * An advanced analog of
 * <a href="http://commons.apache.org/lang/api-2.4/org/apache/commons/lang/math/DoubleRange.html"
 * id="dummy">org.apache.commons.lang.math.DoubleRange</a>.
 *
 * <p>The minimum number ({@link #min()}) is never greater than the maximum number ({@link #max()}),
 * and both numbers are never <tt>Double.NaN</tt>.
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
     * <tt>min&lt;=<i>x</i>&lt;=max</tt>.
     * The <tt>min</tt> value must not be greater than <tt>max</tt>,
     * and both arguments must not be <tt>Double.NaN</tt>.
     *
     * @param min the minimum number in the range, inclusive.
     * @param max the maximum number in the range, inclusive.
     * @return    the new range.
     * @throws IllegalArgumentException if <tt>min &gt; max</tt> or one of the arguments is <tt>Double.NaN</tt>.
     */
    public static Range valueOf(double min, double max) {
        if (Double.isNaN(min))
            throw new IllegalArgumentException("min is NaN");
        if (Double.isNaN(max))
            throw new IllegalArgumentException("max is NaN");
        if (min > max)
            throw new IllegalArgumentException("min > max (min = " + min + ", max = " + max + ")");
        return new Range(min, max);
    }

    /**
     * Returns an instance of this class describing the same range as the given integer range.
     * The <tt>long</tt> boundaries of the passed integer range are converted
     * to <tt>double</tt> boundaries {@link #min()} and {@link #max()} of the returned range by standard
     * Java typecast <tt>(double)longValue</tt>.
     *
     * @param iRange the integer range.
     * @return       the equivalent real range.
     * @throws NullPointerException if the passed integer range is <tt>null</tt>.
     */
    public static Range valueOf(IRange iRange) {
        if (iRange == null)
            throw new NullPointerException("Null iRange argument");
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
     * Returns <tt>{@link #max()}-{@link #min()}</tt>.
     *
     * @return <tt>{@link #max()}-{@link #min()}</tt>.
     */
    public double size() {
        return max - min;
    }

    /**
     * Returns <tt>value&lt;{@link #min()}?{@link #min()}:value&gt;{@link #max()}?{@link #max()}:value</tt>.
     * In other words, returns the passed number if it is in this range or the nearest range bound in other cases.
     *
     * @param value some number.
     * @return      the passed number if it is in this range or the nearest range bound in other cases.
     */
    public double cut(double value) {
        return value < min ? min : value > max ? max : value;
    }

    /**
     * Returns <tt>true</tt> if and only if <tt>{@link #min()}&lt;=value&lt;={@link #max()}</tt>
     *
     * @param value the checked value.
     * @return      <tt>true</tt> if the value is in this range.
     */
    public boolean contains(double value) {
        return min <= value && value <= max;
    }

    /**
     * Returns <tt>true</tt> if and only if <tt>{@link #min()}&lt;=range.{@link #min()}</tt>
     * and <tt>range.{@link #max()}&lt;={@link #max()}</tt>.
     *
     * @param range the checked range.
     * @return      <tt>true</tt> if the checked range is a subset of this range.
     */
    public boolean contains(Range range) {
        return min <= range.min && range.max <= max;
    }

    /**
     * Returns <tt>true</tt> if and only if <tt>{@link #min()}&lt;=range.{@link #max()}</tt>
     * and <tt>range.{@link #min()}&lt;={@link #max()}</tt>.
     *
     * @param range the checked range.
     * @return      <tt>true</tt> if the checked range overlaps with this range.
     */
    public boolean intersects(Range range) {
        return min <= range.max && range.min <= max;
    }

    /**
     * Returns an instance of this class describing the range
     * <nobr><tt>StrictMath.min(this.{@link #min() min()},value) &lt;= x
     * &lt;= StrictMath.max(this.{@link #max() max()},value)</tt></nobr>,
     * excepting the case when the passed value is <tt>NaN</tt> &mdash;
     * in the last situation, returns this instance without changes.
     * In other words, expands the current range to include the given value.
     *
     * @param value some value that should belong to the new range.
     * @return      the expanded range (or this range if <tt>Double.isNaN(value)</tt>).
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
     * Equivalent to <tt>{@link IRange#valueOf(Range) IRange.valueOf}(thisInstance)</tt>,
     * with the only difference that <tt>IllegalStateException</tt> is thrown instead of
     * <tt>IllegalArgumentException</tt> for unallowed range.
     *
     * @return the integer range with same (cast) bounds.
     * @throws IllegalStateException in the same situations when
     *                               {@link IRange#valueOf(Range)} method throws <tt>IllegalArgumentException</tt>.
     */
    public IRange toIntegerRange() {
        return IRange.valueOf((long) min, (long) max, true);
    }

    /**
     * Equivalent to <tt>{@link IRange#roundOf(Range) IRange.roundOf}(thisInstance)</tt>,
     * with the only difference that <tt>IllegalStateException</tt> is thrown instead of
     * <tt>IllegalArgumentException</tt> for unallowed range.
     *
     * @return the integer range with same (rounded) bounds.
     * @throws IllegalStateException in the same situations when
     *                               {@link IRange#roundOf(Range)} method throws <tt>IllegalArgumentException</tt>.
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
     * Returns <tt>true</tt> if and only if <tt>obj instanceof Range</tt>,
     * <tt>Double.doubleToLongBits(((Range)obj).min())==Double.doubleToLongBits(thisInstance.min())</tt>
     * and <tt>Double.doubleToLongBits(((Range)obj).max())==Double.doubleToLongBits(thisInstance.max())</tt>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <tt>true</tt> if the specified object is a range equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof Range
            && Double.doubleToLongBits(((Range)obj).min) == Double.doubleToLongBits(this.min)
            && Double.doubleToLongBits(((Range)obj).max) == Double.doubleToLongBits(this.max);
    }
}
