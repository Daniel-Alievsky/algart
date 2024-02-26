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

import java.math.BigInteger;

/**
 * <p>Integer signed value with 128-bit precision: &minus;2<sup>128</sup>&lt;<i>x</i>&lt;2<sup>128</sup>.</p>
 *
 * <p>Note: this class cannot represent positive or negative values &plusmn;2<sup>128</sup>.
 * (In comparison, <tt>long</tt> 64-bit primitive type can represent &minus;2<sup>63</sup>.)</p>
 *
 * <p>Unlike <tt>BigInteger</tt>, this class is <b>mutable</b>, that allows to reuse one instance for 128-bit
 * calculation without memory allocation. This class is not thread-safe, but <b>is thread-compatible</b>
 * and can be synchronized manually if multithread access is necessary.
 *
 * @author Daniel Alievsky
 */
public final class MutableInt128 implements Cloneable, Comparable<MutableInt128> {
    private static final double EXP_2_63 = StrictMath.scalb(1.0, 63);
    private static final double EXP_2_64 = StrictMath.scalb(1.0, 64);
    private static final double EXP_2_127 = StrictMath.scalb(1.0, 127);
    private static final double EXP_2_128 = StrictMath.scalb(1.0, 128);

    private long high64Bits = 0;
    private long low64Bits = 0;
    // Absolute value = high64Bits * 2^64 + low64Bits (both long are unsigned).
    private boolean negative = false;

    /**
     * Creates new instance of this class, containing integer value 0.
     */
    public MutableInt128() {
    }

    private MutableInt128(long high64Bits, long low64Bits, boolean negative) {
        this.high64Bits = high64Bits;
        this.low64Bits = low64Bits;
        this.negative = negative;
    }

    /**
     * Creates new instance of this class with given bits of stored integer value.
     *
     * @param high64Bits high 64 bits of the absolute value of the created integer value (unsigned value).
     * @param low64Bits  low 64 bits of the absolute value of the created integer value (unsigned value).
     * @param negative   the sign of the created integer value.
     * @throws IllegalArgumentException if <tt>high64Bits&le;0</tt>.
     */
    static MutableInt128 valueOfBits(long high64Bits, long low64Bits, boolean negative) {
        return new MutableInt128(high64Bits, low64Bits, negative);
    }

    /**
     * Creates new instance of this class, equal to the passed <tt>long</tt> value.
     *
     * <p>This method is equivalent to <tt>new MutableInt128().setToLong(value)</tt>.</p>
     *
     * @param value some <tt>long</tt> value.
     * @return newly created object, containing this value.
     */
    public static MutableInt128 valueOf(long value) {
        return new MutableInt128().setToLong(value);
    }

    /**
     * Creates new instance of this class, equal to the passed <tt>long</tt> value, interpreted
     * as unsigned 64-bit value. The result is always non-negative.
     *
     * <p>This method is equivalent to <tt>new MutableInt128().setToUnsignedLong(value)</tt>.</p>
     *
     * @param value some <tt>long</tt> value.
     * @return newly created object, containing this value, interpreted as unsigned 64-bit integer.
     */
    public static MutableInt128 valueOfUnsigned(long value) {
        return new MutableInt128().setToUnsignedLong(value);
    }

    /**
     * Creates new instance of this class, equal to the truncated 128-bit long approximation
     * of the passed <tt>double</tt> value.
     * This conversion is similar to the <i>narrowing primitive conversion</i> <tt>(long)value</tt>.
     *
     * <p>This method is equivalent to <tt>new MutableInt128().setToDouble(value)</tt>.</p>
     *
     * @param value some <tt>long</tt> value.
     * @return newly created object, containing the passed value, truncated to 128-bit integer.
     */
    public static MutableInt128 valueOfDouble(double value) {
        return new MutableInt128().setToDouble(value);
    }

    /**
     * Creates new instance of this class, containing 0.
     *
     * <p>Note: this is just another form of calling the {@link #MutableInt128() constructor}.
     *
     * @return newly created object, containing 0 value.
     */
    public static MutableInt128 newZero() {
        return new MutableInt128();
    }

    /**
     * Creates new instance of this class, containing +1.
     *
     * @return newly created object, containing +1 value.
     */
    public static MutableInt128 newOne() {
        return new MutableInt128().one();
    }

    /**
     * Creates new instance of this class, containing the minimal possible value (&minus;2<sup>128</sup>).
     *
     * @return newly created object, containing &minus;2<sup>128</sup> value.
     */
    public static MutableInt128 newMinValue() {
        return new MutableInt128().minValue();
    }

    /**
     * Creates new instance of this class, containing the maxnimal possible value (+2<sup>128</sup>).
     *
     * @return newly created object, containing +2<sup>128</sup> value.
     */
    public static MutableInt128 newMaxValue() {
        return new MutableInt128().maxValue();
    }

    /**
     * Returns <tt>true</tt> if and only if the value of this number is zero.
     *
     * @return if this value == 0.
     * @see #zero()
     */
    public boolean isZero() {
        return low64Bits == 0 && high64Bits == 0;
    }

    /**
     * Returns <tt>true</tt> if and only if this number is positive. Note that {@link #isZero() zero number}
     * is not positive: this method returns <tt>false</tt> for it.
     *
     * @return if this value &gt; 0.
     */
    public boolean isPositive() {
        return !negative && !isZero();
    }

    /**
     * Returns <tt>true</tt> if and only if this number is negative. Note that {@link #isZero() zero number}
     * is non-negative: this method returns <tt>false</tt> for it.
     *
     * @return if this value &lt; 0.
     */
    public boolean isNegative() {
        return negative && !isZero();
    }

    /**
     * Returns the signum function of this number.
     *
     * @return -1, 0 or 1 as the number is negative, zero or positive.
     */
    public int signum() {
        return isZero() ? 0 : negative ? -1 : 1;
    }

    /**
     * Returns low 64 bits of the absolute value of this number. You can use this method together with
     * {@link #shiftRight(int)} to retrieve any bits of the absolute value.
     *
     * @return low 64 bits of the absolute value (unsigned <tt>long</tt> value).
     */
    public long low64Bits() {
        return low64Bits;
    }

    /**
     * Returns high 64 bits of the absolute value of this number.
     *
     * <p>The result of this method is equal to the following expression:
     * <pre>
     *     thisNumber.{@link #shiftRight(int) shiftRight}(64).{@link #low64Bits()}</pre>
     * <p>but this method does not change the state of this object.
     *
     * @return high 64 bits of the absolute value (unsigned <tt>long</tt> value).
     */
    public long high64Bits() {
        return high64Bits;
    }

    /**
     * Returns <tt>true</tt> if and only if this number can be <b>exactly</b> represented by <tt>double</tt>
     * primitive type, i.e. if the number of significant bits is &le;53 (maximal precision of <tt>double</tt> type).
     * In particular, this method returns <tt>true</tt> if this number is in
     * range&minus;2<sup>53</sup>&le;<i>x</i>&le;2<sup>53</sup>.
     *
     * <p>In other words, this method returns <tt>true</tt> if and only if the method {@link #toDouble()}
     * is performed absolutely exactly, without precision loss.
     *
     * @return <tt>true</tt> if this number can be <b>exactly</b> represented by <tt>double</tt> type.
     */
    public boolean isExactlyConvertibleToDouble() {
        return numberOfLeadingZeros() + numberOfTrailingZeros() >= 75; // = 128 - 53;
    }

    /**
     * Converts this number into <tt>double</tt> value.
     * This conversion is similar to the <i>narrowing primitive conversion</i>,
     * like described in comments to <tt>doubleValue()</tt> method
     * of the standard <tt>BigInteger</tt>.
     *
     * <p>Note: the returned value is en exact representation of this number,
     * if the method {@link #isExactlyConvertibleToDouble()} returns true.
     * In this case, the reverse
     *
     * @return <tt>double</tt>, maximally close to this integer number.
     */
    public double toDouble() {
        if (high64Bits == 0 && low64Bits >= 0) {
            return negative ? -(double) low64Bits : (double) low64Bits;
        }
        final int leadingZeros = Long.numberOfLeadingZeros(high64Bits);
        // Note: if it is 64, it means that we really have 64 leading zeros (because low64Bit < 0)

        // IEEE double: SEEEEEEEEEEEmmmmmm....m (11 bits the exponent, 52 bits mantissa)
        long mantissa;
        final boolean increment;
        // leadingZeros=11 for 2^52 (bit 1 and 52 bits 0); in this case, we just need to clear this bit
        if (leadingZeros > 11) {
            final int leftShift = leadingZeros - 11; // 1 <= leftShift <= 64-11=53
            final int lowBitsRightShift = 64 - leftShift; // 64-53 = 11 <= lowBitsRightShift <= 63
            mantissa = ((high64Bits << leftShift) | (low64Bits >>> lowBitsRightShift)) & 0xFFFFFFFFFFFFFL;
            // 0xFFFFFFFFFFFFFL = (1L << 52) - 1: clearing the highest bit #52
            final long middle = 1L << (lowBitsRightShift - 1);
            final long otherBits = low64Bits & ((middle << 1) - 1L);
            if (otherBits == middle) {
                // exactly between two nearest neighbours: choose correction 0 or 1 to more even mantissa
                increment = (mantissa & 1L) != 0;
            } else {
                increment = (otherBits & middle) != 0; // don't use "<=" here! these longs are unsigned!
            }
        } else {
            // all mantissa is in highBits
            final int rightShift = 11 - leadingZeros; // 0 <= rightShift <= 11
            mantissa = (high64Bits >>> rightShift) & 0xFFFFFFFFFFFFFL;
            if (rightShift == 0) {
                increment = low64Bits == Long.MIN_VALUE ?
                        (mantissa & 1L) != 0 :
                        low64Bits < 0;
            } else {
                final long middle = 1L << (rightShift - 1);
                final long otherHighBits = high64Bits & ((middle << 1) - 1L);
                if (otherHighBits == middle && low64Bits == 0) {
                    // exactly between two nearest neighbours: choose correction 0 or 1 to more even mantissa
                    increment = (mantissa & 1L) != 0;
                } else {
                    increment = otherHighBits >= middle;
                }
            }
        }
        long exponent = 1150 - leadingZeros; // 1023 + (127 - leadingZeros)
        // - long type is important here for the following "<< 52"
        if (increment) {
            mantissa++;
            if (mantissa == 0x10000000000000L) { // 2^52
                mantissa = 0;
                exponent++;
            }
        }
        final long ieeeDouble = mantissa | (exponent << 52) | (negative ? 0x8000000000000000L : 0);
        return Double.longBitsToDouble(ieeeDouble);
    }

    /**
     * Returns <tt>true</tt> if and only if this number can be represented by <tt>long</tt> primitive type,
     * i.e. it is in range&minus;2<sup>63</sup>&le;<i>x</i>&le;2<sup>63</sup>&minus;1.
     * <p>In other words, returns <tt>true</tt> if and only if the method {@link #toLongExact()} does not throw
     * an exception.
     *
     * @return <tt>true</tt> if this number can be represented as <tt>long</tt>.
     */
    public boolean isConvertibleToLong() {
        return high64Bits == 0 && (low64Bits >= 0 || (low64Bits == Long.MIN_VALUE && negative));
    }

    /**
     * Returns this number as <tt>long</tt> value, if it is possible (this number is in range
     * &minus;2<sup>63</sup>&le;<i>x</i>&le;2<sup>63</sup>&minus;1). If it is impossible, throws
     * <tt>ArithmeticException</tt>.
     *
     * <p>You can check, whether this number can be represented as <tt>long</tt>, with help of
     * {@link #isConvertibleToLong()} method.
     *
     * @return this number, represented by <tt>long</tt> type.
     * @throws ArithmeticException in a case of arithmetic overflow.
     */
    public long toLongExact() {
        if (!isConvertibleToLong()) {
            throw new ArithmeticException("This number is out of long range");
        }
        return negative ? -low64Bits : low64Bits;
        // - for Long.MIN_VALUE we correctly return -Long.MIN_VALUE = Long.MIN_VALUE
    }

    /**
     * Converts this number into <tt>BigInteger</tt>.
     *
     * @return <tt>BigInteger</tt> value, containing the same integer value.
     */
    public BigInteger toBigInteger() {
        BigInteger result = BigInteger.valueOf(high64Bits & Long.MAX_VALUE);
        result = result.shiftLeft(64).or(BigInteger.valueOf(low64Bits & Long.MAX_VALUE));
        if (low64Bits < 0) {
            result = result.setBit(63);
        }
        if (high64Bits < 0) {
            result = result.setBit(127);
        }
        if (isNegative()) {
            result = result.negate();
        }
        return result;
    }

    /**
     * Sets this number to be identical to the passed number.
     *
     * @param other other number.
     * @return a reference to this object.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public MutableInt128 setTo(MutableInt128 other) {
        if (other == null) {
            throw new NullPointerException("Null other");
        }
        this.low64Bits = other.low64Bits;
        this.high64Bits = other.high64Bits;
        this.negative = other.negative;
        return this;
    }

    /**
     * Sets this number to be equal to the passed <tt>long</tt> value.
     *
     * @param value some <tt>long</tt> value.
     * @return a reference to this object.
     */
    public MutableInt128 setToLong(long value) {
        this.negative = value < 0;
        this.low64Bits = Math.abs(value);
        // - for Long.MIN_VALUE we correctly sets Long.MIN_VALUE
        this.high64Bits = 0;
        return this;
    }

    /**
     * Sets this number to be equal to the passed <tt>long</tt> value, interpreted as unsigned 64-bit value.
     *
     * @param value some <tt>long</tt> value (interpreted as unsigned 64-bit integer).
     * @return a reference to this object.
     */
    public MutableInt128 setToUnsignedLong(long value) {
        this.negative = false;
        this.low64Bits = value;
        this.high64Bits = 0;
        return this;
    }

    /**
     * Sets this number to be truncated 128-bit long approximation of the passed <tt>double</tt> value.
     * This conversion is similar to the <i>narrowing primitive conversion</i> <tt>(long)value</tt>.
     *
     * @param value some <tt>double</tt> value.
     * @return a reference to this object.
     */
    public MutableInt128 setToDouble(double value) {
        negative = value < 0;
        if (negative) {
            value = -value;
        }
        double valueDivided2Exp64 = StrictMath.floor(StrictMath.scalb(value, -64));
        if (valueDivided2Exp64 >= EXP_2_64) {
            high64Bits = 0xFFFFFFFFFFFFFFFFL;
            low64Bits = 0xFFFFFFFFFFFFFFFFL;
            return this;
        }
        high64Bits = valueDivided2Exp64 >= EXP_2_63 ?
                (long) (valueDivided2Exp64 - EXP_2_63) | 0x8000000000000000L :
                (long) valueDivided2Exp64;
        assert value < EXP_2_128;
        value -= StrictMath.scalb(valueDivided2Exp64, 64);
        low64Bits = value >= EXP_2_63 ?
                (long) (value - EXP_2_63) | 0x8000000000000000L :
                (long) value;
        return this;
    }

    /**
     * Sets this number to zero value (0).
     *
     * @return a reference to this object.
     * @see #isZero()
     * @see #newZero()
     */
    public MutableInt128 zero() {
        this.low64Bits = 0;
        this.high64Bits = 0;
        this.negative = false;
        return this;
    }

    /**
     * Sets this number to one value (+1).
     *
     * @return a reference to this object.
     */
    public MutableInt128 one() {
        this.low64Bits = 1;
        this.high64Bits = 0;
        this.negative = false;
        return this;
    }

    /**
     * Sets this number to minimal possible value (&minus;2<sup>128</sup>).
     *
     * @return a reference to this object.
     */
    public MutableInt128 minValue() {
        this.low64Bits = 0xFFFFFFFFFFFFFFFFL;
        this.high64Bits = 0xFFFFFFFFFFFFFFFFL;
        this.negative = true;
        return this;
    }

    /**
     * Sets this number to maximal possible value (+2<sup>128</sup>).
     *
     * @return a reference to this object.
     */
    public MutableInt128 maxValue() {
        this.low64Bits = 0xFFFFFFFFFFFFFFFFL;
        this.high64Bits = 0xFFFFFFFFFFFFFFFFL;
        this.negative = false;
        return this;
    }

    /**
     * Returns the bit #<tt>index</tt> of the absolute value of this number. For example,
     * if <tt>index==0</tt>, this method returns <tt>true</tt> when this number is odd.
     * If <tt>index</tt> is too large (&ge;128), this method returns <tt>false</tt>.
     *
     * @param index index of the bit of this number.
     * @return value of this bit.
     * @throws IllegalArgumentException if <tt>index</tt> argument is negative.
     */
    public boolean getBit(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative bit index");
        }
        if (index >= 128) {
            return false;
        }
        if (index >= 64) {
            return ((high64Bits >>> (index - 64)) & 1) != 0;
        } else {
            return ((low64Bits >>> index) & 1) != 0;
        }
    }

    /**
     * Sets the bit #<tt>index</tt> of the absolute value of this number to the given value.
     * If <tt>index</tt> is too large (&ge;128), this method does nothing.
     *
     * @param index index of the bit of this number.
     * @param value new value of the bit (<tt>true</tt> is 1, <tt>false</tt> is 0).
     * @return a reference to this object.
     * @throws IllegalArgumentException if <tt>index</tt> argument is negative.
     */
    public MutableInt128 setBit(int index, boolean value) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative bit index");
        }
        if (index >= 128) {
            return this;
        }
        if (value) {
            if (index >= 64) {
                high64Bits |= 1L << (index - 64);
            } else {
                low64Bits |= 1L << index;
            }
        } else {
            if (index >= 64) {
                high64Bits &= ~(1L << (index - 64));
            } else {
                low64Bits &= ~(1L << index);
            }
        }
        return this;
    }

    /**
     * Shifts all bits of the absolute value of this number rightwards by the specified number of bits.
     * As the result, this number <i>x</i> is changed to
     * <pre>    <i>x</i>&lt;0 ? &minus;(&minus;<i>x</i>)/2<sup>shift</sup> : <i>x</i>/2<sup>shift</sup>.</pre>
     *
     * <p>If the argument <tt>shift</tt> is too large (&ge;128), the method sets this number to zero.
     *
     * <p>Shifting by 0 bits (<tt>shift=0</tt>) or shifting zero value with any <tt>shift</tt>
     * does not change the number.</p>
     *
     * @param shift number of bits to shift.
     * @return a reference to this object.
     * @throws IllegalArgumentException if <tt>shift</tt> argument is negative.
     */
    public MutableInt128 shiftRight(int shift) {
        if (shift < 0) {
            throw new IllegalArgumentException("Negative shift");
        }
        if (shift == 0) {
            return this;
        }
        if (shift >= 128) {
            high64Bits = 0;
            low64Bits = 0;
            return this;
        }
        if (shift >= 64) {
            low64Bits = high64Bits >>> (shift - 64);
            high64Bits = 0;
            return this;
        }
        low64Bits = (low64Bits >>> shift) | (high64Bits << (64 - shift));
        high64Bits >>>= shift;
        return this;
    }

    /**
     * Shifts all bits of the absolute value of this number leftwards by the specified number of bits.
     * As the result, this number <i>x</i> is changed to
     * <pre>    <i>x</i>&lt;0 ? &minus;(&minus;<i>x</i>)*2<sup>shift</sup> : <i>x</i>*2<sup>shift</sup>.</pre>
     *
     * <p>If the resulting absolute value cannot be represented by 128-bit number, in particular,
     * if current number is non-zero and shift&ge;128, the method throws <tt>ArithmeticException</tt>.
     *
     * <p>Shifting by 0 bits (<tt>shift=0</tt>) or shifting zero value with any <tt>shift</tt>
     * does not change the number.
     *
     * @param shift number of bits to shift.
     * @return a reference to this object.
     * @throws IllegalArgumentException if <tt>shift</tt> argument is negative.
     * @throws ArithmeticException      in a case of arithmetic overflow.
     */
    public MutableInt128 shiftLeft(int shift) {
        if (shift < 0) {
            throw new IllegalArgumentException("Negative shift");
        }
        if (shift == 0 || (low64Bits == 0 && high64Bits == 0)) {
            return this;
        }
        if (shift >= 128) {
            throw new ArithmeticException("128-bit overflow while left shift <<" + shift);
        }
        if (shift >= 64) {
            if (high64Bits != 0) {
                throw new ArithmeticException("128-bit overflow while left shift <<" + shift);
            }
            long newHigh64Bits = low64Bits << (shift - 64);
            if (newHigh64Bits >>> (shift - 64) != low64Bits) {
                throw new ArithmeticException("128-bit overflow while left shift <<" + shift);
            }
            high64Bits = newHigh64Bits;
            low64Bits = 0;
            return this;
        }
        long newHigh64Bits = (high64Bits << shift) | (low64Bits >>> (64 - shift));
        if (newHigh64Bits >>> shift != high64Bits) {
            throw new ArithmeticException("128-bit overflow while left shift <<" + shift);
        }
        high64Bits = newHigh64Bits;
        low64Bits <<= shift;
        return this;
    }

    /**
     * Divides the absolute value of this number by 2<sup>shift</sup> and rounds the result to the closest
     * 128-bit integer value. If two integer numbers are equally close to the result of division,
     * the result is the 128-bit integer value, which is even.
     * In other words, this number <i>x</i> is changed to
     * <pre>    [<i>x</i>/2.0<sup>shift</sup>],</pre>
     * where <tt>[<i>w</i>]</tt> means the mathematical integer, closest to <tt><i>w</i></tt>,
     * like in <tt>Math.rint()</tt> method.
     *
     * <p>If the argument <tt>shift</tt> is too large (&ge;128), the method sets this number to zero.
     *
     * <p>Shifting by 0 bits (<tt>shift=0</tt>) or shifting zero value with any <tt>shift</tt>
     * does not change the number.</p>
     *
     * @param shift number of bits to shift.
     * @return a reference to this object.
     * @throws IllegalArgumentException if <tt>shift</tt> argument is negative.
     */
    public MutableInt128 shiftRightRounding(int shift) {
        if (shift < 0) {
            throw new IllegalArgumentException("Negative shift");
        }
        if (shift == 0) {
            return this;
        }
        if (shift >= 128) {
            high64Bits = 0;
            low64Bits = 0;
            return this;
        }
        if (shift > 64) {
            shift -= 64; // now 1<=shift<=63
            final long middle = 1L << (shift - 1);
            final long otherBits = high64Bits & ((middle << 1) - 1L);
            // 0 <= middle<<1 <= 2^63, so, otherBits is non-negative
            long newLow64Bits = high64Bits >>> shift;
            if (otherBits == middle && low64Bits == 0 ? (newLow64Bits & 1) == 1 : otherBits >= middle) {
                // otherBits>=middle is correct as unsigned, because both numbers are non-negative
                newLow64Bits++;
                // overflow impossible: low64Bits < 2^63 (unsigned)
            }
            high64Bits = 0;
            low64Bits = newLow64Bits;
            return this;
        }
        if (shift == 64) {
            long newLow64Bits = high64Bits;
            high64Bits = 0;
            if (low64Bits == 0x8000000000000000L ? (newLow64Bits & 1) == 1 : low64Bits < 0) {
                // "low64Bits < 0" means ">= 1L << 63"
                newLow64Bits++;
                if (newLow64Bits == 0) {
                    high64Bits++;
                }
            }
            low64Bits = newLow64Bits;
            return this;
        }
        // now 1<=shift<=63
        final long middle = 1L << (shift - 1);
        final long otherBits = low64Bits & ((middle << 1) - 1L);
        // 0 <= middle<<1 <= 2^63, so, otherBits is non-negative
        long newLow64Bits = (low64Bits >>> shift) | (high64Bits << (64 - shift));
        long newHigh64Bits = high64Bits >>> shift;
        if (otherBits == middle ? (newLow64Bits & 1) == 1 : otherBits >= middle) {
            // otherBits>=middle is correct as unsigned, because both numbers are non-negative
            newLow64Bits++;
            if (newLow64Bits == 0) {
                newHigh64Bits++;
            }
        }
        high64Bits = newHigh64Bits;
        low64Bits = newLow64Bits;
        return this;
    }

    /**
     * Replaces all bits of the absolute value of this number with bitwise AND of this and other number:
     * <tt>|this| = |this| &amp; |other|</tt>.
     *
     * <p>The sign of this number stays unchanged &mdash; excepting the case, when this number was non-zero,
     * but the result is zero (i.e. not positive and not negative).
     *
     * @param other value to be AND'ed with this number.
     * @return a reference to this object.
     */
    public MutableInt128 and(MutableInt128 other) {
        this.high64Bits &= other.high64Bits;
        this.low64Bits &= other.low64Bits;
        return this;
    }

    /**
     * Replaces all bits of the absolute value of this number with bitwise OR of this and other number:
     * <tt>|this| = |this| | |other|</tt>.
     *
     * <p>The sign of this number stays unchanged &mdash; excepting the case, when this number was zero,
     * but the result is nonzero (i.e. positive or negative).
     *
     * @param other value to be OR'ed with this number.
     * @return a reference to this object.
     */
    public MutableInt128 or(MutableInt128 other) {
        this.high64Bits |= other.high64Bits;
        this.low64Bits |= other.low64Bits;
        return this;
    }

    /**
     * Replaces all bits of the absolute value of this number with bitwise XOR of this and other number:
     * <tt>|this| = |this| ^ |other|</tt>.
     *
     * <p>The sign of this number stays unchanged &mdash; excepting the case, when this number or the result
     * is zero (i.e. not positive and not negative).
     *
     * @param other value to be XOR'ed with this number.
     * @return a reference to this object.
     */
    public MutableInt128 xor(MutableInt128 other) {
        this.high64Bits ^= other.high64Bits;
        this.low64Bits ^= other.low64Bits;
        return this;
    }

    /**
     * Inverts all bits of the absolute value of this number:
     * <tt>|this| = ~|this|</tt>.
     *
     * <p>The sign of this number stays unchanged &mdash; excepting the case, when this number or the result
     * is zero (i.e. not positive and not negative).
     *
     * @return a reference to this object.
     */
    public MutableInt128 not() {
        this.high64Bits = ~this.high64Bits;
        this.low64Bits = ~this.low64Bits;
        return this;
    }

    /**
     * Returns the number of zero bits preceding the highest-order
     * ("leftmost") one-bit in the 128-bit absolute value of this number.
     * Returns 128 if this number is zero.
     *
     * <p>It is a 128-bit analogue of the standard <tt>Long.numberOfLeadingZeros</tt> method,
     * but working with the absolute value of this number (instead of two's complement binary representation).
     *
     * @return the number of leading zero bits in the absolute value of this number, or 128 for zero number.
     */
    public int numberOfLeadingZeros() {
        return high64Bits != 0 ? Long.numberOfLeadingZeros(high64Bits) : 64 + Long.numberOfLeadingZeros(low64Bits);
    }

    /**
     * Returns the number of zero bits following the lowest-order ("rightmost")
     * one-bit in the 128-bit absolute value of this number.
     * Returns 128 if this number is zero.
     *
     * <p>It is a 128-bit analogue of the standard <tt>Long.numberOfTrailingZeros</tt> method,
     * but working with the absolute value of this number (instead of two's complement binary representation).
     *
     * @return the number of trailing zero bits in the absolute value of this number, or 128 for zero number.
     */
    public int numberOfTrailingZeros() {
        return low64Bits != 0 ? Long.numberOfTrailingZeros(low64Bits) : 64 + Long.numberOfTrailingZeros(high64Bits);
    }

    /**
     * Returns the number of one-bits in the 128-bit absolute value of this number.
     *
     * <p>It is a 128-bit analogue of the standard <tt>Long.bitCount</tt> method,
     * but working with the absolute value of this number (instead of two's complement binary representation).
     *
     * @return the number of one-bits in the binary representation of the absolute value of this number.
     */
    public int bitCount() {
        return Long.bitCount(high64Bits) + Long.bitCount(low64Bits);
    }

    /**
     * Changes the sign of this integer number and stores the result in this object.
     * Note: this method does not anything for {@link #isZero() zero number}.
     *
     * @return a reference to this object.
     */
    public MutableInt128 negate() {
        if (!isZero()) {
            negative = !negative;
        }
        return this;
    }

    /**
     * Replaces this number with its absolute value.
     *
     * @return a reference to this object.
     */
    public MutableInt128 abs() {
        negative = false;
        return this;
    }

    /**
     * Adds the given <tt>other</tt> number to this one and stores the sum in this object.
     *
     * @return a reference to this object.
     * @throws ArithmeticException  in a case of arithmetic overflow.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public MutableInt128 add(MutableInt128 other) throws ArithmeticException {
        add(other.high64Bits, other.low64Bits, other.negative);
        return this;
    }

    /**
     * Subtracts the given <tt>other</tt> number from this one and stores the difference in this object.
     *
     * @return a reference to this object.
     * @throws ArithmeticException  in a case of arithmetic overflow.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public MutableInt128 subtract(MutableInt128 other) throws ArithmeticException {
        add(other.high64Bits, other.low64Bits, !other.negative);
        return this;
    }

    /**
     * Adds the given integer value to this number.
     *
     * @param value any <tt>long</tt> value to add.
     * @return a reference to this object.
     * @throws ArithmeticException in a case of arithmetic overflow.
     */
    public MutableInt128 addLong(long value) {
        add(0, Math.abs(value), value < 0);
        // - correct even for value==Long.MIN_VALUE
        return this;
    }

    /**
     * Sets this object to be equal to the exact product <tt>firstMultiplier</tt>*<tt>secondMultiplier</tt>.
     *
     * @param firstMultiplier  first multiplicand.
     * @param secondMultiplier second multiplicand.
     * @return a reference to this object.
     */
    public MutableInt128 setToLongLongProduct(long firstMultiplier, long secondMultiplier) {
        this.negative = (firstMultiplier < 0) != (secondMultiplier < 0);
        final long a = firstMultiplier < 0 ? -firstMultiplier : firstMultiplier;
        final long b = secondMultiplier < 0 ? -secondMultiplier : secondMultiplier;
        // Now 0 <= a <= 2^63, 0 <= b <= 2^63 (unsigned); Long.MIN_VALUE is interpreted as a correct 64-bit unsigned
        final long a0 = a & 0xFFFFFFFFL;
        final long a1 = a >>> 32;
        final long b0 = b & 0xFFFFFFFFL;
        final long b1 = b >>> 32;

        //             a1   a0
        //         x
        //             b1   b0
        //         =
        //           a1b0 a0b0   a * b0
        // +
        //      a1b1 a0b1        a * b1 << 32
        //
        long w0 = a0 * b0;
        long t = (w0 >>> 32) + a1 * b0; // <= 2^32-1 + 2^31*(2^32-1) = 2^63 + 2^31 - 1 (unsigned)
        long w1 = t & 0xFFFFFFFFL; // important: it is not (long)(int)t for negative t!
        long w2 = t >>> 32;
        // w2:w1:w0 (low 32 bits of every w2,w1,w0) = a * b0
        w1 += a0 * b1; // also <= 2^63 + a little
        this.low64Bits = (w0 & 0xFFFFFFFFL) + (w1 << 32);
        this.high64Bits = a1 * b1 + w2 + (w1 >>> 32);
        // - high64Bits: see Hacker's Delight 8.2
        assert this.high64Bits >= 0; // - because long arguments are signed and cannot be >2^63 by absolute value
        return this;
    }

    /**
     * Sets this object to be equal to the exact product <tt>firstMultiplier</tt>*<tt>secondMultiplier</tt>.
     *
     * <p>Note: this method works faster than {@link #setToLongLongProduct(long, long)}.
     *
     * @param firstMultiplier  first multiplicand.
     * @param secondMultiplier second multiplicand.
     * @return a reference to this object.
     */
    public MutableInt128 setToLongIntProduct(long firstMultiplier, int secondMultiplier) {
        this.negative = (firstMultiplier < 0) != (secondMultiplier < 0);
        final long a = firstMultiplier < 0 ? -firstMultiplier : firstMultiplier;
        final long b = (secondMultiplier < 0 ? -secondMultiplier : secondMultiplier) & 0xFFFFFFFFL;
        // 0xFFFFFFFFL: in the case Integer.MIN_VALUE we must use the correct positive long 0x80000000L
        final long a0 = a & 0xFFFFFFFFL;
        final long a1 = a >>> 32;

        long w0 = a0 * b;
        long t = (w0 >>> 32) + a1 * b;
        this.low64Bits = (w0 & 0xFFFFFFFFL) + (t << 32);
        this.high64Bits = t >>> 32;
        assert this.high64Bits >= 0; // - because long argument is signed and cannot be >2^63 by absolute value
        return this;
    }

    /**
     * Sets this object to be equal to the exact unsigned product <tt>firstMultiplier</tt>*<tt>secondMultiplier</tt>,
     * where the arguments are interpreted as unsigned 64-bit integers. The result is always non-negative.
     *
     * <p>Note: this method works little faster than {@link #setToLongLongProduct(long, long)}.
     *
     * @param firstMultiplier  first multiplicand (interpreted as unsigned 64-bit integer number).
     * @param secondMultiplier second multiplicand (interpreted as unsigned 64-bit integer number).
     * @return a reference to this object.
     */
    public MutableInt128 setToUnsignedLongLongProduct(long firstMultiplier, long secondMultiplier) {
        this.negative = false;
        // Now 0 <= a < 2^64, 0 <= b < 2^64 (unsigned)
        final long a0 = firstMultiplier & 0xFFFFFFFFL;
        final long a1 = firstMultiplier >>> 32;
        final long b0 = secondMultiplier & 0xFFFFFFFFL;
        final long b1 = secondMultiplier >>> 32;

        //             a1   a0
        //         x
        //             b1   b0
        //         =
        //           a1b0 a0b0   a * b0
        // +
        //      a1b1 a0b1        a * b1 << 32
        //
        long w0 = a0 * b0;
        long t = (w0 >>> 32) + a1 * b0; // <= 2^32-1 + (2^32-1)^2 = 2^64 - 2^32 (unsigned)
        long w1 = t & 0xFFFFFFFFL; // important: it is not (long)(int)t for negative t!
        long w2 = t >>> 32;
        // w2:w1:w0 (low 32 bits of every w2,w1,w0) = a * b0
        w1 += a0 * b1; // <= 2^64 - 1
        this.low64Bits = (w0 & 0xFFFFFFFFL) + (w1 << 32);
        this.high64Bits = a1 * b1 + w2 + (w1 >>> 32);
        // - high64Bits: see Hacker's Delight 8.2
        return this;
    }

    /**
     * Sets this object to be equal to the exact square of the given value: <tt>value</tt>*<tt>value</tt>.
     * Equivalent to
     * <pre>    {@link #setToLongLongProduct(long, long) setToLongLongProduct(value, value)}
     * </pre>
     *
     * @param value some long signed value.
     * @return a reference to this object.
     */
    public MutableInt128 setToLongSqr(long value) {
        this.negative = false;
        final long a = value < 0 ? -value : value;
        // Now 0 <= a <= 2^63 (unsigned); Long.MIN_VALUE is interpreted as a correct 64-bit unsigned
        final long a0 = a & 0xFFFFFFFFL;
        final long a1 = a >>> 32;

        //             a1   a0
        //         x
        //             a1   a0
        //         =
        //           a1a0 a0^2   a * a0
        // +
        //      a1^2 a1a0        a * a1 << 32
        //
        long w0 = a0 * a0;
        long t = (w0 >>> 32) + ((a1 * a0) << 1); // <= 2^32-1 + 2^31*(2^32-1)*2 = 2^63 - 1 (unsigned)
        long w1 = t & 0xFFFFFFFFL; // important: it is not (long)(int)t for negative t!
        long w2 = t >>> 32;
        // w2:w1:w0 (low 32 bits of every w2,w1,w0) = a * a0 + (a1 * a0) << 32
        this.low64Bits = (w0 & 0xFFFFFFFFL) + (w1 << 32);
        this.high64Bits = a1 * a1 + w2;
        assert this.high64Bits >= 0; // - because long argument is signed and cannot be >2^63 by absolute value
        return this;
    }

    /**
     * Calculate exact product <tt>firstMultiplier</tt>*<tt>secondMultiplier</tt> and adds it to this number.
     * Equivalent to
     * <pre>    {@link #add(MutableInt128) add}(new {@link #MutableInt128()
     * MutableLongLong}().{@link #setToLongLongProduct(long, long)
     * setToLongLongProduct}(firstMultiplier,&nbsp;secondMultiplier))</pre>
     *
     * @param firstMultiplier  first multiplicand.
     * @param secondMultiplier second multiplicand.
     * @return a reference to this object.
     * @throws ArithmeticException in a case of arithmetic overflow while adding.
     */
    public MutableInt128 addLongLongProduct(long firstMultiplier, long secondMultiplier) {
        final boolean productNegative = (firstMultiplier < 0) != (secondMultiplier < 0);
        final long a = firstMultiplier < 0 ? -firstMultiplier : firstMultiplier;
        final long b = secondMultiplier < 0 ? -secondMultiplier : secondMultiplier;
        // Now 0 <= a <= 2^63, 0 <= b <= 2^63 (unsigned); Long.MIN_VALUE is interpreted as a correct 64-bit unsigned
        final long a0 = a & 0xFFFFFFFFL;
        final long a1 = a >>> 32;
        final long b0 = b & 0xFFFFFFFFL;
        final long b1 = b >>> 32;

        //             a1   a0
        //         x
        //             b1   b0
        //         =
        //           a1b0 a0b0   a * b0
        // +
        //      a1b1 a0b1        a * b1 << 32
        //
        long w0 = a0 * b0;
        long t = (w0 >>> 32) + a1 * b0; // <= 2^32-1 + 2^31*(2^32-1) = 2^63 + 2^31 - 1 (unsigned)
        long w1 = t & 0xFFFFFFFFL; // important: it is not (long)(int)t for negative t!
        long w2 = t >>> 32;
        // w2:w1:w0 (low 32 bits of every w2,w1,w0) = a * b0
        w1 += a0 * b1; // also <= 2^63 + a little
        final long productLow64Bits = (w0 & 0xFFFFFFFFL) + (w1 << 32); // here w1 << 32 == (w1 & 0xFFFFFFFF) << 32
        final long productHigh64Bits = a1 * b1 + w2 + (w1 >>> 32);
        // - Hackers Delight 8.2
        assert productHigh64Bits >= 0; // - because long arguments are signed and cannot be >2^63 by absolute value
        add(productHigh64Bits, productLow64Bits, productNegative);
        return this;
    }

    /**
     * Calculate exact product <tt>firstMultiplier</tt>*<tt>secondMultiplier</tt> and adds it to this number.
     * Equivalent to
     * <pre>    {@link #add(MutableInt128) add}(new {@link #MutableInt128()
     * MutableLongLong}().{@link #setToLongIntProduct(long, int)
     * setToLongIntProduct}(firstMultiplier,&nbsp;secondMultiplier))</pre>
     *
     * <p>Note: this method works faster than {@link #addLongLongProduct(long, long)}.
     *
     * @param firstMultiplier  first multiplicand.
     * @param secondMultiplier second multiplicand.
     * @return a reference to this object.
     * @throws ArithmeticException in a case of arithmetic overflow while adding.
     */
    public MutableInt128 addLongIntProduct(long firstMultiplier, int secondMultiplier) {
        final boolean productNegative = (firstMultiplier < 0) != (secondMultiplier < 0);
        final long a = firstMultiplier < 0 ? -firstMultiplier : firstMultiplier;
        final long b = (secondMultiplier < 0 ? -secondMultiplier : secondMultiplier) & 0xFFFFFFFFL;
        // 0xFFFFFFFFL: in the case Integer.MIN_VALUE we must use the correct positive long 0x80000000L
        final long a0 = a & 0xFFFFFFFFL;
        final long a1 = a >>> 32;

        long w0 = a0 * b;
        long t = (w0 >>> 32) + a1 * b;
        final long productLow64Bits = (w0 & 0xFFFFFFFFL) + (t << 32);
        final long productHigh64Bits = t >>> 32;
        assert productHigh64Bits >= 0; // - because long argument is signed and cannot be >2^63 by absolute value
        add(productHigh64Bits, productLow64Bits, productNegative);
        return this;
    }

    /**
     * Calculate exact unsigned product <tt>firstMultiplier</tt>*<tt>secondMultiplier</tt>,
     * where the arguments are interpreted as unsigned (non-negative) 64-bit integers,
     * and adds it to this number.
     * Equivalent to
     * <pre>    {@link #add(MutableInt128) add}(new {@link #MutableInt128()
     * MutableLongLong}().{@link #setToUnsignedLongLongProduct(long, long)
     * setToUnsignedLongLongProduct}(firstMultiplier,&nbsp;secondMultiplier))</pre>
     *
     * <p>Note: this method works little faster than {@link #addLongLongProduct(long, long)}.
     *
     * @param firstMultiplier  first multiplicand (interpreted as unsigned 64-bit integer number).
     * @param secondMultiplier second multiplicand (interpreted as unsigned 64-bit integer number).
     * @return a reference to this object.
     * @throws ArithmeticException in a case of arithmetic overflow while adding.
     */
    public MutableInt128 addUnsignedLongLongProduct(long firstMultiplier, long secondMultiplier) {
        // Now 0 <= a < 2^64, 0 <= b < 2^64 (unsigned)
        final long a0 = firstMultiplier & 0xFFFFFFFFL;
        final long a1 = firstMultiplier >>> 32;
        final long b0 = secondMultiplier & 0xFFFFFFFFL;
        final long b1 = secondMultiplier >>> 32;

        //             a1   a0
        //         x
        //             b1   b0
        //         =
        //           a1b0 a0b0   a * b0
        // +
        //      a1b1 a0b1        a * b1 << 32
        //
        long w0 = a0 * b0;
        long t = (w0 >>> 32) + a1 * b0; // <= 2^32-1 + (2^32-1)^2 = 2^64 - 2^32 (unsigned)
        long w1 = t & 0xFFFFFFFFL; // important: it is not (long)(int)t for negative t!
        long w2 = t >>> 32;
        // w2:w1:w0 (low 32 bits of every w2,w1,w0) = a * b0
        w1 += a0 * b1; // <= 2^64 - 1
        final long productLow64Bits = (w0 & 0xFFFFFFFFL) + (w1 << 32);
        final long productHigh64Bits = a1 * b1 + w2 + (w1 >>> 32);
        // - Hackers Delight 8.2
        add(productHigh64Bits, productLow64Bits, false);
        return this;
    }

    /**
     * Calculate exact exact square of the given value: <tt>value</tt>*<tt>value</tt>
     * and adds it to this number.
     * Equivalent to
     * <pre>    {@link #add(MutableInt128) add}(new {@link #MutableInt128()
     * MutableLongLong}().{@link #setToLongSqr(long)
     * setToLongSqr}(value))</pre>
     *
     * @param value some long signed value.
     * @return a reference to this object.
     * @throws ArithmeticException in a case of arithmetic overflow while adding.
     */
    public MutableInt128 addLongSqr(long value) {
        final long a = value < 0 ? -value : value;
        // Now 0 <= a <= 2^63 (unsigned); Long.MIN_VALUE is interpreted as a correct 64-bit unsigned
        final long a0 = a & 0xFFFFFFFFL;
        final long a1 = a >>> 32;

        //             a1   a0
        //         x
        //             a1   a0
        //         =
        //           a1a0 a0^2   a * a0
        // +
        //      a1^2 a1a0        a * a1 << 32
        //
        long w0 = a0 * a0;
        long t = (w0 >>> 32) + ((a1 * a0) << 1); // <= 2^32-1 + 2^31*(2^32-1)*2 = 2^63 - 1 (unsigned)
        long w1 = t & 0xFFFFFFFFL; // important: it is not (long)(int)t for negative t!
        long w2 = t >>> 32;
        // w2:w1:w0 (low 32 bits of every w2,w1,w0) = a * a0 + (a1 * a0) << 32
        final long productLow64Bits = (w0 & 0xFFFFFFFFL) + (w1 << 32);
        final long productHigh64Bits = a1 * a1 + w2;
        assert productHigh64Bits >= 0; // - because long argument is signed and cannot be >2^63 by absolute value
        add(productHigh64Bits, productLow64Bits, false);
        return this;
    }

    /**
     * Returns an exact copy of this integer number.
     *
     * @return a reference to the clone.
     */
    @Override
    public MutableInt128 clone() {
        try {
            return (MutableInt128) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * Compares this number with the specified one.
     *
     * @param o other number to which this number is to be compared.
     * @return -1, 0 or 1 as this number is numerically less than, equal
     * to, or greater than {@code o}.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    @Override
    public int compareTo(MutableInt128 o) {
        if (o.isZero()) {
            return signum();
        }
        if (negative != o.negative) {
            // correct also if this==0 and this.negative
            return negative ? -1 : 1;
        }
        int result = lessUnsigned(high64Bits, o.high64Bits) ? -1 : high64Bits != o.high64Bits ? 1
                : lessUnsigned(low64Bits, o.low64Bits) ? -1 : low64Bits != o.low64Bits ? 1 : 0;
        return negative ? -result : result;
    }

    /**
     * Returns the decimal String representation of this number.
     *
     * @return decimal String representation of this number.
     */
    @Override
    public String toString() {
        return toBigInteger().toString();
    }

    /**
     * Returns the String representation of this number with the given radix.
     * If the radix is outside the range from {@link
     * Character#MIN_RADIX} to {@link Character#MAX_RADIX} inclusive,
     * it will default to 10 (as is the case for
     * {@code Integer.toString}).
     *
     * @param radix radix of the String representation.
     * @return decimal String representation of this number.
     */
    public String toString(int radix) {
        return toBigInteger().toString(radix);
    }

    /**
     * Indicates whether some other object is an instance of this class, equal to this number.
     * Note that two objects, containing {@link #isZero() zero number},
     * are always equal, regardless on possible calling {@link #negate()} method.
     *
     * @param o the object to be compared for equality with this instance.
     * @return <tt>true</tt> if and only if the specified object is an instance of {@link MutableInt128},
     * containing the same mathematical integer number as this object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MutableInt128 that = (MutableInt128) o;
        return low64Bits == that.low64Bits &&
                high64Bits == that.high64Bits &&
                (negative == that.negative || (isZero() && that.isZero()));
    }

    /**
     * Returns the hash code of this number.
     *
     * @return the hash code of this number.
     */
    @Override
    public int hashCode() {
        if (isZero()) {
            return 0x2DFE7324;
        }
        int result = hashCode(low64Bits);
        result = 37 * result + hashCode(high64Bits);
        result = 37 * result + (negative ? 157 : 932);
        // - here "negative" means really negative: the case of zero is checked above
        return result;
    }

    /**
     * Converts the passed value, interpreted as unsigned 64-bit value, into <tt>double</tt>, and returns it.
     * The result of this method is always non-negative.
     * This conversion is similar to the <i>narrowing primitive conversion</i>,
     * like described in comments to <tt>doubleValue()</tt> method
     * of the standard <tt>BigInteger</tt>.
     *
     * @param value some <tt>long</tt> value (interpreted as unsigned 64-bit integer).
     * @return <tt>double</tt>, maximally close to this unsigned 64-bit integer number.
     */
    public static double unsignedToDouble(long value) {
        if (value >= 0) {
            return (double) value;
        }
        // - About the following code see toDouble() method for a fixed case leadingZeros=64
        // (the high bit of value is 1).
        // We could also use something like "2.0 * (double) (value >>> 1)",
        // but such result needs correction in a rare situation "exactly between two nearest neighbours":
        // it this case, the lowest bit #0 can become significant.
        // The following code provide performance, comparable with built-in (double) cast.
        long mantissa = (value >>> 11) & 0xFFFFFFFFFFFFFL;
        // 0xFFFFFFFFFFFFFL = (1L << 52) - 1: clearing the highest bit #52
        final long otherBits = value & 2047L;
        final boolean increment = otherBits == 1024L ? (mantissa & 1L) != 0 : (otherBits & 1024L) != 0;
        // otherBits == 1024L means "exactly between two nearest neighbours"
        long exponent = 1086; // 1023 + 63
        // - long type is important here for the following "<< 52"
        if (increment) {
            mantissa++;
            if (mantissa == 0x10000000000000L) { // 2^52
                mantissa = 0;
                exponent++;
            }
        }
        final long ieeeDouble = mantissa | (exponent << 52);
        return Double.longBitsToDouble(ieeeDouble);
    }

    /**
     * Divides the given value by 2<sup>shift</sup> and rounds the result to the closest
     * <tt>long</tt> value. If two integer numbers are equally close to the result of division,
     * the result is the <tt>long</tt> integer value, which is even.
     * In other words, this method returns
     * <pre>    [value/2.0<sup>shift</sup>],</pre>
     * where <tt>[<i>w</i>]</tt> means the mathematical integer, closest to <tt><i>w</i></tt>,
     * like in <tt>Math.rint()</tt> method.
     *
     * <p>If the argument <tt>shift</tt> is too large (&ge;64), the method returns 0.
     *
     * <p>Shifting by 0 bits (<tt>shift=0</tt>) returns unchanged <tt>value</tt>.</p>
     *
     * @param value some (signed) <tt>long</tt> value.
     * @param shift number of bits to shift.
     * @return the <tt>long</tt> integer, closest to <tt>value/2.0<sup>shift</sup></tt>.
     * @throws IllegalArgumentException if <tt>shift</tt> argument is negative.
     */
    public static long shiftRightRounding(long value, int shift) {
        if (shift < 0) {
            throw new IllegalArgumentException("Negative shift");
        }
        if (shift == 0) {
            return value;
        }
        if (shift >= 64) {
            return 0;
        }
        final long middle = 1L << (shift - 1);
        final long otherBits = value & ((middle << 1) - 1);
        long result = value >> shift;
        if (otherBits == middle ? (result & 1) == 1 : otherBits >= middle) {
            // otherBits>=middle is correct, because both numbers are non-negative
            result++;
        }
        return result;
    }

    /**
     * Divides the given value, interpreted as unsigned 64-bit value, by 2<sup>shift</sup>
     * and rounds the result to the closest unsigned 64-bit <tt>long</tt> value.
     * If two integer numbers are equally close to the result of division,
     * the result is the unsigned 64-bit integer value, which is even.
     * It is an unsigned analogue of {@link #shiftRightRounding(long, int)} method.
     *
     * <p>If the argument <tt>shift</tt> is too large (&ge;64), the method returns 0.
     *
     * <p>Shifting by 0 bits (<tt>shift=0</tt>) returns unchanged <tt>value</tt>.</p>
     *
     * @param value some (unsigned) 64-bit integer value.
     * @param shift number of bits to shift.
     * @return the 64-bit unsigned integer, closest to <tt>value/2.0<sup>shift</sup></tt>.
     * @throws IllegalArgumentException if <tt>shift</tt> argument is negative.
     */
    public static long unsignedShiftRightRounding(long value, int shift) {
        if (shift < 0) {
            throw new IllegalArgumentException("Negative shift");
        }
        if (shift == 0) {
            return value;
        }
        if (shift >= 64) {
            return 0;
        }
        final long middle = 1L << (shift - 1);
        final long otherBits = value & ((middle << 1) - 1);
        long result = value >>> shift;
        if (otherBits == middle ? (result & 1) == 1 : otherBits >= middle) {
            // otherBits>=middle is correct, because both numbers are non-negative
            result++;
        }
        return result;
    }

    // Alternative version of toDouble, used for debugging needs
    double toDoubleByAdding() {
        double resultAbs = (low64Bits & Long.MAX_VALUE)
                + (low64Bits < 0 ? EXP_2_63 : 0.0)
                + StrictMath.scalb((double) (high64Bits & Long.MAX_VALUE), 64)
                + (high64Bits < 0 ? EXP_2_127 : 0.0);
        return negative ? -resultAbs : resultAbs;
    }

    private void add(long otherHigh64Bits, long otherLow64Bits, boolean otherNegative) throws ArithmeticException {
        long newHigh64Bits = high64Bits;
        long newLow64Bits = low64Bits;
        if (negative == otherNegative) {
            newLow64Bits += otherLow64Bits;
            // r = a + b (low 64 bits of the sum; sum is 65-bit)
            // a < 2^63  (0),  b < 2^63  (0):  overflow = false (and a+b < 2^64)
            // a < 2^63  (0),  b >= 2^63 (1):  overflow = a+b < 2^63 = ~(a+b >= 2^63) (really 2^63 <= a+b < 2^64+2^63)
            // a >= 2^63 (1),  b < 2^63  (0):  the same
            // a >= 2^63 (1),  b >= 2^63 (1):  overflow = true

            // final long carryBit = newLow64Bits >= 0 ?
            //         ((low64Bits | otherLow64Bits) >>> 63) & 1L :
            //         ((low64Bits & otherLow64Bits) >>> 63) & 1L;
            // - equivalent, almost the same speed
            final long carryBit =
                    (((low64Bits & otherLow64Bits) | ((low64Bits | otherLow64Bits) & ~newLow64Bits)) >>> 63) & 1L;


            // See Hacker's Delight, "Unsigned Add/Subtract": (x & y) | ((x | y) & ~(x + y [+c]))
            // Note: it differs from version for signed numbers (see Math.addExact)
            newHigh64Bits += (otherHigh64Bits + carryBit);
            // r = a + b + 0/1 (low 64 bits of the sum; sum is 65-bit even if carryBit=1)
            // a < 2^63  (0),  b < 2^63  (0):  overflow = false (and a+b[+1] < 2^64)
            // a < 2^63  (0),  b >= 2^63 (1):  overflow = a+b[+1] < 2^63 = ~(a+b >= 2^63)
            // a >= 2^63 (1),  b < 2^63  (0):  the same
            // a >= 2^63 (1),  b >= 2^63 (1):  overflow = true

            // if (newHigh64Bits >= 0 ?
            //         (high64Bits | otherHigh64Bits) < 0 :
            //         (high64Bits & otherHigh64Bits) < 0) {
            // - equivalent, almost the same speed
            if (((high64Bits & otherHigh64Bits) | ((high64Bits | otherHigh64Bits) & ~newHigh64Bits)) < 0) {
                // Overflow
                throw new ArithmeticException("128-bit overflow: absolute value ||a|+|b||>2^128");
            }
        } else {
            newLow64Bits -= otherLow64Bits;
            // r = a - b (low 64 bits of 65-bit difference)
            // a < 2^63  (0),  b < 2^63  (0):  overflow = a<b = a-b >= 2^63
            // a < 2^63  (0),  b >= 2^63 (1):  overflow = true  (high bit of a-b can be any)
            // a >= 2^63 (1),  b < 2^63  (0):  overflow = false (high bit of a-b can be any)
            // a >= 2^63 (1),  b >= 2^63 (1):  overflow = a<b = a-b >= 2^63

            // final long carryBit = newLow64Bits >= 0 ?
            //         ((~low64Bits & otherLow64Bits) >>> 63) & 1L :
            //         ((~low64Bits | otherLow64Bits) >>> 63) & 1L;
            // - equivalent, almost the same speed
            final long carryBit =
                    (((~low64Bits & otherLow64Bits) | ((~low64Bits | otherLow64Bits) & newLow64Bits)) >>> 63) & 1L;
            // See Hacker's Delight, "Unsigned Add/Subtract": (~x & y) | ((~x | y) & (x - y [-c]))
            newHigh64Bits -= (otherHigh64Bits + carryBit);
            // r = a - b - 0/1 (low 64 bits of the difference; difference is 65-bit even if carryBit=1:
            // in the worst case it is -2^64)
            // a < 2^63  (0),  b < 2^63  (0):  overflow = a-b[-1] >= 2^63
            // a < 2^63  (0),  b >= 2^63 (1):  overflow = true  (high bit of a-b[-1] can be any)
            // a >= 2^63 (1),  b < 2^63  (0):  overflow = false (high bit of a-b[-1] can be any)
            // a >= 2^63 (1),  b >= 2^63 (1):  overflow = a-b[-1] >= 2^63

            // if (newHigh64Bits >= 0 ?
            //         (~high64Bits & otherHigh64Bits) < 0 :
            //         (~high64Bits | otherHigh64Bits) < 0) {
            // - equivalent, almost the same speed
            if (((~high64Bits & otherHigh64Bits) | ((~high64Bits | otherHigh64Bits) & newHigh64Bits)) < 0) {
                // Overflow
                // Let H = unsigned newHigh64Bits (H is 64 bits!), L = newLow64Bits
                // Now W=1:H:L is a correct 129-bit two’s complement unsigned representation of the signed result.
                // We need to find -W = not W + 1
                newHigh64Bits = ~newHigh64Bits;
                newLow64Bits = ~newLow64Bits;
                newLow64Bits++;
                if (newLow64Bits == 0) {
                    // very simple commands, maybe no sense to optimize by avoiding branch
                    newHigh64Bits++;
                }
                negative = !negative;
            }
        }
        high64Bits = newHigh64Bits;
        low64Bits = newLow64Bits;
    }

    private static int hashCode(long value) {
        return (int) (value ^ (value >>> 32));
    }

    private static boolean lessUnsigned(long a, long b) {
        return a - b >= 0 ? (~a & b) < 0 : (~a | b) < 0;
        // - see "add" private method above
    }
}
