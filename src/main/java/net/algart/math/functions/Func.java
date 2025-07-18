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

package net.algart.math.functions;

import net.algart.math.IPoint;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.DoublePredicate;

/**
 * <p>Abstract mathematical function
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>),
 * or <i>f</i>(<b>x</b>), where <b>x</b> is a point of the <i>n</i>-dimensional space.</p>
 *
 * <p>Implementations of this interface are usually <b>immutable</b> and
 * always <b>thread-safe</b>: <code>get</code> methods of this interface may be freely used
 * while simultaneous accessing to the same instance from several threads.
 * All implementations of this interface from this package are <b>immutable</b>.</p>
 *
 * @author Daniel Alievsky
 */
public interface Func {

    /**
     * Identity function, just returning its first argument:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
     * <i>x</i><sub>0</sub>.
     * The {@link #get} method of this object requires at least 1 argument
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func IDENTITY = new IdentityFunc();

    /**
     * Updatable version of {@link #IDENTITY} function.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func.Updatable UPDATABLE_IDENTITY = new IdentityFunc.Updatable();

    /**
     * Maximum from several numbers:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
     * max(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>).
     * The {@link #get} method of this object may process any number of arguments.
     * If the number of arguments is 0, it returns <code>Double.NEGATIVE_INFINITY</code>.
     *
     * <p>Unlike standard <code>Math.max</code> method, this function supposes that
     * <code>max(x,y) = x&gt;y ? x : y</code>
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func MAX = new MaxFunc();

    /**
     * Minimum from several numbers:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
     * min(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>).
     * The {@link #get} method of this object may process any number of arguments.
     * If the number of arguments is 0, it returns <code>Double.POSITIVE_INFINITY</code>.
     *
     * <p>Unlike standard <code>Math.min</code> method, this function supposes that
     * <code>min(x,y) = x&lt;y ? x : y</code>
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func MIN = new MinFunc();

    /**
     * Absolute value function:
     * <i>f</i>(<i>x</i><sub>0</sub>) =
     * |<i>x</i><sub>0</sub>|.
     * More precisely, the result of this function is
     * <code>StrictMath.abs(x[0])</code>.
     * The {@link #get} method of this object requires at least 1 argument
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func ABS = new AbsFunc();

    /**
     * Absolute value of the difference of 2 numbers:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) =
     * |<i>x</i><sub>0</sub>-<i>x</i><sub>1</sub>|.
     * The {@link #get} method of this object requires at least 2 arguments
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0 or 1.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func ABS_DIFF = new AbsDiffFunc();

    /**
     * Positive difference of 2 numbers:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) =
     * max(<i>x</i><sub>0</sub>-<i>x</i><sub>1</sub>,0).
     * The {@link #get} method of this object requires at least 2 arguments
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0 or 1.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func POSITIVE_DIFF = new PositiveDiffFunc();

    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * <i>x</i><sub>0</sub> + <i>x</i><sub>1</sub>:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(0.0, 1.0, 1.0)}.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc X_PLUS_Y = LinearFunc.getInstance(0.0, 1.0, 1.0);

    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * <i>x</i><sub>0</sub> - <i>x</i><sub>1</sub>:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(0.0, 1.0, -1.0)}.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc X_MINUS_Y = LinearFunc.getInstance(0.0, 1.0, -1.0);

    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * <i>x</i><sub>1</sub> - <i>x</i><sub>0</sub>:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(0.0, -1.0, 1.0)}.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc Y_MINUS_X = LinearFunc.getInstance(0.0, -1.0, 1.0);

    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * (<i>x</i><sub>0</sub> + <i>x</i><sub>1</sub>)/2:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(0.0, 0.5, 0.5)}.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc HALF_X_PLUS_Y = LinearFunc.getInstance(0.0, 0.5, 0.5);

    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * (<i>x</i><sub>0</sub> - <i>x</i><sub>1</sub>)/2:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(0.0, 0.5, -0.5)}.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc HALF_X_MINUS_Y = LinearFunc.getInstance(0.0, 0.5, -0.5);

    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * (<i>x</i><sub>1</sub> - <i>x</i><sub>0</sub>)/2:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(0.0, -0.5, 0.5)}.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc HALF_Y_MINUS_X = LinearFunc.getInstance(0.0, -0.5, 0.5);
    /**
     * An instance of {@link LinearFunc} class, describing the linear function
     * 1.0 - <i>x</i><sub>0</sub>:
     * {@link LinearFunc#getInstance(double, double...) LinearFunc.getInstance(1.0, -1.0)}.
     * This instance describes logical NOT operation in  a case when the arguments are bits (0 and 1 values).
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    LinearFunc REVERSE = LinearFunc.getInstance(1.0, -1.0);

    /**
     * Select function:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
     * <i>x</i><sub><i>i</i>+1</sub>, where <i>i</i> is <i>x</i><sub>0</sub> cast to integer type:
     * <code><i>i</i>=(int)x[0]</code>.
     * The {@link #get} method of this object requires at least 2 arguments
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0 or 1.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func SELECT = new SelectFunc();

    /**
     * Select function:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>) =
     * <i>x</i><sub>0</sub> &gt; <i>x</i><sub>1</sub> ? <i>x</i><sub>2</sub> : <i>x</i><sub>3</sub>.
     * The {@link #get} method of this object requires at least 4 arguments
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0, 1, 2 or 3.
     *
     * <p>Note: call of this function is almost equivalent to calling {@link #SELECT_IF_GREATER_OR_EQUAL} function
     * with another order of the arguments:
     * <i>f</i>(<i>x</i><sub>1</sub>, <i>x</i><sub>0</sub>, <i>x</i><sub>3</sub>, <i>x</i><sub>2</sub>),
     * that is
     * <i>x</i><sub>1</sub> &gt;= <i>x</i><sub>0</sub> ? <i>x</i><sub>3</sub> : <i>x</i><sub>2</sub>.
     * The only difference is connected with processing <code>Double.NaN</code> values of <i>x</i><sub>0</sub> and
     * <i>x</i><sub>1</sub>: this function will choose <i>x</i><sub>3</sub>, but the corresponding call of
     * {@link #SELECT_IF_GREATER_OR_EQUAL} will choose <i>x</i><sub>2</sub>, because in Java any comparison with
     * <code>Double.NaN</code> returns <code>false</code>.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func SELECT_IF_GREATER = new SelectIfGreaterFunc();

    /**
     * Select function:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, <i>x</i><sub>2</sub>, <i>x</i><sub>3</sub>) =
     * <i>x</i><sub>0</sub> &gt;= <i>x</i><sub>1</sub> ? <i>x</i><sub>2</sub> : <i>x</i><sub>3</sub>.
     * The {@link #get} method of this object requires at least 4 arguments
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0, 1, 2 or 3.
     *
     * <p>Note: call of this function is almost equivalent to calling {@link #SELECT_IF_GREATER} function
     * with another order of the arguments:
     * <i>f</i>(<i>x</i><sub>1</sub>, <i>x</i><sub>0</sub>, <i>x</i><sub>3</sub>, <i>x</i><sub>2</sub>),
     * that is
     * <i>x</i><sub>1</sub> &gt; <i>x</i><sub>0</sub> ? <i>x</i><sub>3</sub> : <i>x</i><sub>2</sub>.
     * The only difference is connected with processing <code>Double.NaN</code> values of <i>x</i><sub>0</sub> and
     * <i>x</i><sub>1</sub>: this function will choose <i>x</i><sub>3</sub>, but the corresponding call of
     * {@link #SELECT_IF_GREATER} will choose <i>x</i><sub>2</sub>, because in Java any comparison with
     * <code>Double.NaN</code> returns <code>false</code>.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func SELECT_IF_GREATER_OR_EQUAL = new SelectIfGreaterOrEqualFunc();

    /**
     * Selecting from 8 "integer" directions on 2D plane:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>) = integer code from 1 to 8.
     * The result of this function is the index of one of 8 sectors, where the 2-dimensional vector
     * (<i>x</i><sub>0</sub>,<i>x</i><sub>1</sub>) lies.
     * Namely, let &phi;=Math.atan2(<i>x</i><sub>1</sub>,<i>x</i><sub>0</sub>), more precisely,
     * an equivalent angle in 0&deg;..360&deg; range.
     * The result of this function is:
     * <ul>
     * <li>0, if 337.5&deg; &lt; &phi; &le; 22.5&deg; and also in the special case
     * <i>x</i><sub>0</sub>=<i>x</i><sub>1</sub>=0.0;</li>
     * <li>1, if 22.5&deg; &lt; &phi; &le; 67.5&deg;;</li>
     * <li>2, if 67.5&deg; &lt; &phi; &le; 112.5&deg;;</li>
     * <li>3, if 112.5&deg; &lt; &phi; &le; 157.5&deg;;</li>
     * <li>4, if 157.5&deg; &lt; &phi; &le; 202.5&deg;;</li>
     * <li>5, if 202.5&deg; &lt; &phi; &le; 247.5&deg;;</li>
     * <li>6, if 247.5&deg; &lt; &phi; &le; 292.5&deg;;</li>
     * <li>7, if 292.5&deg; &lt; &phi; &le; 337.5&deg;.</li>
     * </ul>
     *
     * <p>(A strange formula "337.5&deg; &lt; &phi; &le; 22.5&deg;" just means that the direction lies
     * between 337.5&deg;=-22.5&deg; and +22.5&deg; &mdash; in other words, "almost rightward" along X axis.)
     *
     * <p>This function is useful while processing 2-dimensional matrices, for example, in algorithms
     * like <a href="http://en.wikipedia.org/wiki/Canny_edge_detector">Canny edge detector</a>.
     *
     * <p>The {@link #get} method of this object requires at least 2 arguments
     * and throws <code>IndexOutOfBoundsException</code> if the number of arguments is 0 or 1.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify its settings.
     */
    Func SELECT_FROM_8_DIRECTIONS_2D = new SelectFrom8Directions2DFunc();

    /**
     * Vectors, corresponding to 8 directions recognized by {@link #SELECT_FROM_8_DIRECTIONS_2D} function.
     * More precisely, if is an unmodifiable list consisting of the following elements:
     * <ol start="0">
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(1,0)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(1,1)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(0,1)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(-1,1)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(-1,0)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(-1,-1)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(0,-1)},</li>
     * <li>{@link IPoint#valueOf(long...) IPoint.valueOf(1,-1)}.</li>
     * </ol>
     *
     * <p>So, if the direction returned by {@link #SELECT_FROM_8_DIRECTIONS_2D} is <i>n</i>, then the corresponding
     * "rounding" direction is described by
     * <code>{@link #SHIFTS_ALONG_8_DIRECTIONS_2D}.get(</code><i>n</i><code>)</code>.
     *
     * <p>This instance is <b>immutable</b> and <b>thread-safe</b>: there are no ways to modify it.
     */
    List<IPoint> SHIFTS_ALONG_8_DIRECTIONS_2D = Collections.unmodifiableList(
            java.util.Arrays.asList(
                    IPoint.valueOf(1, 0),
                    IPoint.valueOf(1, 1),
                    IPoint.valueOf(0, 1),
                    IPoint.valueOf(-1, 1),
                    IPoint.valueOf(-1, 0),
                    IPoint.valueOf(-1, -1),
                    IPoint.valueOf(0, -1),
                    IPoint.valueOf(1, -1)));

    /**
     * <p>"Updatable" mathematical function: an extension of {@link Func} interface
     * allowing assigning values to the function result, that leads to
     * corresponding correction of arguments.
     * Usually can be implemented for one-argument functions,
     * where the {@link #set set(x[], newResult)} method assigns
     * to <code>x[0]</code> the result of the inverse function, called for <code>newResult</code>.
     * </p>
     *
     * <p>Implementations of this interface are usually <b>immutable</b> and
     * always <b>thread-safe</b>: <code>get</code> and <code>set</code> methods of this interface may be freely used
     * while simultaneous accessing the same instance from several threads.</p>
     */
    interface Updatable extends Func {
        /**
         * Correct some of <code>x</code> arguments so that
         * {@link #get get(x)} will be, as possible, equal to <code>newResult</code>.
         * For example, if this is one-argument function <i>f</i>(<i>x</i>),
         * and its inverse function is <i>g</i>(<i>y</i>) (<i>g</i>(<i>f</i>(<i>x</i>))=<i>x</i>),
         * then this method should assign <code>x[0]=<i>g</i>(newResult)</code>.
         *
         * <p>This method does not guarantee the precise equality
         * <code>{@link #get get(x)}==newResult</code>. (Usually, it is impossible due to
         * limited precision of floating-point calculations.) But this method should try
         * to provide this equality (after its call) with, as possible, maximal possible precision.
         *
         * @param x         the function arguments.
         * @param newResult the desired function result.
         * @throws IndexOutOfBoundsException may be thrown if the length of <code>x</code> array is less
         *                                   than the required number of this function arguments.
         */
        void set(double[] x, double newResult);
    }

    /**
     * Returns the result of this function for the given arguments:
     * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><code>x.length</code>-1</sub>).
     *
     * <p>This method must not change the values of <code>x</code> elements!
     *
     * @param x the function arguments.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown if the number of passed arguments is less
     *                                   than the required number of this function arguments.
     */
    double get(double... x);

    /**
     * Equivalent to <code>{@link #get(double...) get}(new double[0])</code>.
     * Provides better performance because it does not require Java array creation.
     *
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 1 argument.
     */
    double get();

    /**
     * Equivalent to <code>{@link #get(double...) get}(new double[] {x0})</code>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 2 arguments.
     */
    double get(double x0);

    /**
     * Equivalent to <code>{@link #get(double...) get}(new double[] {x0, x1})</code>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 3 arguments.
     */
    double get(double x0, double x1);

    /**
     * Equivalent to <code>{@link #get(double...) get}(new double[] {x0, x1, x2})</code>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 4 arguments.
     */
    double get(double x0, double x1, double x2);

    /**
     * Equivalent to <code>{@link #get(double...) get}(new double[] {x0, x1, x2, x3})</code>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the first function argument.
     * @param x1 the second function argument.
     * @param x2 the third function argument.
     * @param x3 the fourth function argument.
     * @return the function result.
     * @throws IndexOutOfBoundsException may be thrown this function requires at least 5 arguments.
     */
    double get(double x0, double x1, double x2, double x3);

    /**
     * Returns the function {@link Func1} that is based on the given predicate and returns
     * 1.0 for all values for which this predicate returns <code>true</code>,
     * and 0.0 for all values for which this predicate returns <code>false</code>.
     * Equivalent to
     * <pre>
     *     (Func1) x0 -> predicate.test(x0) ? 1.0 : 0.0;
     * </pre>
     *
     * @param predicate some predicate for double values.
     * @return equivalent AlgART function.
     * @throws NullPointerException if the argument is {@code null}.
     */
    static Func1 of(DoublePredicate predicate) {
        Objects.requireNonNull(predicate, "Null predicate");
        return x0 -> predicate.test(x0) ? 1.0 : 0.0;
    }
}
