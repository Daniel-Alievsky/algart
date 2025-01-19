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

package net.algart.math;

import java.util.Objects;

/**
 * <p>Numeric inclusive integer range:
 * a set of <code>long</code> numbers <code>{@link #min() min()}&lt;=<i>x</i>&lt;={@link #max() max()}</code>.
 * An advanced analog of
 * <a href="http://commons.apache.org/lang/api-2.4/org/apache/commons/lang/math/LongRange.html"
 * id="dummy">org.apache.commons.lang.math.LongRange</a>.
 *
 * <p>The minimum number ({@link #min()}) is never greater than the maximum number ({@link #max()}),
 * both minimal and maximum numbers {@link #min()} and ({@link #max()}) are always in range
 * <code>-Long.MAX_VALUE+1..Long.MAX_VALUE-1</code>,
 * and their difference is always <i>less</i> than <code>Long.MAX_VALUE</code>.
 * In other words, "<code>{@link #max()}-{@link #min()}+1</code>" expression,
 * returned by {@link #size()} method, and also
 * "<code>{@link #min()}-1</code>", "<code>{@link #min()}-2</code>" and
 * "<code>{@link #max()}+1</code>" expressions
 * are always calculated without overflow.
 *
 * <p>Please draw attention to the important effect of the requirement above.
 * <b>If <i>a</i>..<i>b</i> is an allowed range</b> (<i>a</i>={@link #min()}, <i>b</i>={@link #max()}),
 * <b>then 0..<i>b</i>&minus;<i>a</i> and <i>a</i>&minus;<i>b</i>..0 are also allowed ranges</b>.
 * Really, they have the same difference
 * <code>{@link #max()}-{@link #min()}</code>=<i>b</i>&minus;<i>a</i>=<i>diff</i>,
 * and so far as this difference <i>diff</i>&lt;<code>Long.MAX_VALUE</code>, both new bounds
 * <i>b</i>&minus;<i>a</i>=<i>diff</i> and <i>a</i>&minus;<i>b</i>=&minus;<i>diff</i> are also
 * inside the required range <code>-Long.MAX_VALUE+1..Long.MAX_VALUE-1</code>.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @see Range
 */
public final class IRange {
    final long min;
    final long max;

    IRange(long min, long max) {
        this.min = min;
        this.max = max;
        assert isAllowedRange(min, max);
    }

    /**
     * Returns an instance of this class describing the range
     * <code>min&lt;=<i>x</i>&lt;=max</code>.
     * The <code>min</code> value must not be greater than <code>max</code>,
     * both values must be in range <code>-Long.MAX_VALUE+1..Long.MAX_VALUE-1</code>,
     * and the difference <code>max-min</code> must be <i>less</i> than <code>Long.MAX_VALUE</code>.
     *
     * @param min the minimum number in the range, inclusive.
     * @param max the maximum number in the range, inclusive.
     * @return    the new range.
     * @throws IllegalArgumentException if <code>min &gt; max</code>, or if <code>max-min &gt;= Long.MAX_VALUE</code>
     *                                  (more precisely, if the Java expression <code>max-min+1</code> is nonpositive
     *                                  due to integer overflow),
     *                                  or if <code>min&lt;=-Long.MAX_VALUE</code>,
     *                                  or if <code>max==Long.MAX_VALUE</code>.
     */
    public static IRange valueOf(long min, long max) {
        return valueOf(min, max, false);
    }

    /**
     * Returns an instance of this class describing the same range as the given real range,
     * with bounds, truncated to integers by Java typecast <code>(long)doubleValue</code>.
     * Equivalent to
     * <pre>{@link #valueOf(long, long) valueOf}((long)range.{@link Range#min()
     * min()}, (long)range.{@link Range#max() max())}</pre>
     *
     * @param range the real range.
     * @return      the integer range with same (cast) bounds.
     * @throws NullPointerException     if the passed range is {@code null}.
     * @throws IllegalArgumentException if the desired range does not match requirements of
     *                                  {@link #valueOf(long, long)} method.
     */
    public static IRange valueOf(Range range) {
        Objects.requireNonNull(range, "Null range argument");
        return valueOf((long)range.min, (long)range.max);
    }

    /**
     * Returns an instance of this class describing the same range as the given real range,
     * with bounds, rounded to the nearest integers.
     * Equivalent to
     * <pre>{@link #valueOf(long, long) valueOf}(StrictMath.round(range.{@link Range#min()
     * min()}), StrictMath.round(range.{@link Range#max() max()}))</pre>
     *
     * @param range the real range.
     * @return      the integer range with same (rounded) bounds.
     * @throws NullPointerException     if the passed range is {@code null}.
     * @throws IllegalArgumentException if the desired range does not match requirements of
     *                                  {@link #valueOf(long, long)} method.
     */
    public static IRange roundOf(Range range) {
        Objects.requireNonNull(range, "Null range argument");
        return valueOf(StrictMath.round(range.min), StrictMath.round(range.max));
    }

    /**
     * Returns <code>true</code> if and only if the arguments <code>min</code> and <code>max</code> are allowed
     * {@link #min()}/{@link #max()} bounds for some instance of this class. In other words,
     * this method returns <code>false</code> in the same situations, when {@link #valueOf(long min, long max)}
     * method, called with the same arguments, throws <code>IllegalArgumentException</code>.
     *
     * <p>Equivalent to the following check:
     * <pre>
     *     min &lt;= max &amp;&amp; min &gt; -Long.MAX_VALUE &amp;&amp;
     *         max != Long.MAX_VALUE &amp;&amp; max - min + 1L &gt; 0L
     * </pre>
     *
     * @param min the minimum number in some range, inclusive.
     * @param max the maximum number in some range, inclusive.
     * @return    whether these bounds are allowed minimum and maximum for some instance of this class.
     */
    public static boolean isAllowedRange(long min, long max) {
        return min <= max && min > -Long.MAX_VALUE && max != Long.MAX_VALUE && max - min + 1L > 0L;
    }

    /**
     * Returns the minimum number in the range, inclusive.
     *
     * @return the minimum number in the range.
     */
    public long min() {
        return this.min;
    }

    /**
     * Returns the maximum number in the range, inclusive.
     *
     * @return the maximum number in the range.
     */
    public long max() {
        return this.max;
    }

    /**
     * Returns <code>{@link #max()}-{@link #min()}+1</code>.
     *
     * @return <code>{@link #max()}-{@link #min()}+1</code>.
     */
    public long size() {
        return max - min + 1;
    }

    /**
     * Returns <code>value&lt;{@link #min()}?{@link #min()}:value&gt;{@link #max()}?{@link #max()}:value</code>.
     * In other words, returns the passed number if it is in this range or the nearest range bound in other cases.
     *
     * @param value some number.
     * @return      the passed number if it is in this range or the nearest range bound in other cases.
     */
    public long cut(long value) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Returns <code>true</code> if and only if <code>{@link #min()}&lt;=value&lt;={@link #max()}</code>.
     *
     * @param value the checked value.
     * @return      <code>true</code> if the value is in this range.
     */
    public boolean contains(long value) {
        return min <= value && value <= max;
    }

    /**
     * Returns <code>true</code> if and only if <code>{@link #min()}&lt;=range.{@link #min()}</code>
     * and <code>range.{@link #max()}&lt;={@link #max()}</code>.
     *
     * @param range the checked range.
     * @return      <code>true</code> if the checked range is a subset of this range.
     */
    public boolean contains(IRange range) {
        return min <= range.min && range.max <= max;
    }

    /**
     * Returns <code>true</code> if and only if <code>{@link #min()}&lt;=range.{@link #max()}</code>
     * and <code>range.{@link #min()}&lt;={@link #max()}</code>.
     *
     * @param range the checked range.
     * @return      <code>true</code> if the checked range overlaps with this range.
     */
    public boolean intersects(IRange range) {
        return min <= range.max && range.min <= max;
    }

    /**
     * Returns an instance of this class describing the range
     * <code>Math.min(this.{@link #min() min()},value) &lt;= x
     * &lt;= Math.max(this.{@link #max() max()},value)</code>.
     * In other words, expands the current range to include the given value.
     *
     * @param value some value that should belong to the new range.
     * @return      the expanded range.
     * @throws IllegalArgumentException if <code>value==Long.MAX_VALUE</code>,
     *                                  <code>value&lt;=-Long.MAX_VALUE</code> or
     *                                  if in the resulting range
     *                                  <code>max-min &gt;= Long.MAX_VALUE</code>.
     */
    public IRange expand(long value) {
        if (value == Long.MAX_VALUE)
            throw new IllegalArgumentException("Cannot expand " + this + " until Long.MAX_VALUE");
        if (value <= -Long.MAX_VALUE)
            throw new IllegalArgumentException("Cannot expand " + this + " until -Long.MAX_VALUE or Long.MIN_VALUE");
        long min = Math.min(value, this.min);
        long max = Math.max(value, this.max);
        if (max - min + 1L <= 0L)
            throw new IllegalArgumentException("Cannot expand " + this + " until " + value
                + ", because in the result max - min >= Long.MAX_VALUE (min = " + min + ", max = " + max + ")");
        return new IRange(min, max);
    }

    /**
     * Equivalent to <code>{@link Range#valueOf(IRange) Range.valueOf}(thisInstance)</code>.
     *
     * @return the equivalent real range.
     */
    public Range toRange() {
        return Range.valueOf(this);
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
        int iMin = (int)min * 37 + (int)(min >>> 32);
        int iMax = (int)max * 37 + (int)(max >>> 32);
        return iMin * 37 + iMax;
    }

    /**
     * Indicates whether some other range is equal to this instance.
     * Returns <code>true</code> if and only if <code>obj instanceof IRange</code>,
     * <code>((IRange)obj).min()==this.min()</code> and <code>((IRange)obj).max==this.max</code>.
     *
     * @param obj the object to be compared for equality with this instance.
     * @return    <code>true</code> if the specified object is a range equal to this one.
     */
    public boolean equals(Object obj) {
        return obj instanceof IRange && ((IRange)obj).min == this.min && ((IRange)obj).max == this.max;
    }

    static IRange valueOf(long min, long max, boolean ise) {
        if (min == max && min >= -MAX_CACHED && min <= -MAX_CACHED)
            return DegenerateIRangeCache.cache[MAX_CACHED + (int)min];
        if (min > max)
            throw new IllegalArgumentException("min > max (min = " + min + ", max = " + max + ")");
        if (max == Long.MAX_VALUE)
            throw invalidBoundsException("max == Long.MAX_VALUE", ise);
        if (min <= -Long.MAX_VALUE)
            throw invalidBoundsException("min == Long.MAX_VALUE or Long.MIN_VALUE+1", ise);
        if (max - min + 1L <= 0L)
            throw invalidBoundsException("max - min >= Long.MAX_VALUE (min = " + min + ", max = " + max + ")", ise);
        return new IRange(min, max);
    }

    static RuntimeException invalidBoundsException(String message, boolean useIllegalStateException) {
        return useIllegalStateException ?
            new IllegalStateException(message) :
            new IllegalArgumentException((message));
    }

    private static final int MAX_CACHED = 1024;
    private static class DegenerateIRangeCache {
        static final IRange[] cache = new IRange[2 * MAX_CACHED + 1];

        static {
            for(int i = -MAX_CACHED; i <= MAX_CACHED; i++)
                cache[MAX_CACHED + i] = new IRange(i, i);
        }
    }
}
