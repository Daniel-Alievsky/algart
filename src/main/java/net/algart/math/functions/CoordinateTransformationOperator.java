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

/**
 * <p>Coordinate transformation operator in <i>n</i>-dimensional Euclidean space:
 * <i>g</i>(<b>x</b>) = <i>O</i>&nbsp;<i>f</i>(<b>x</b>) = <i>f</i>(<b>map</b>(<b>x</b>)),
 * where <b>x</b> is a point of the <i>n</i>-dimensional space,
 * <b>map</b> is some mapping of this space, <i>f</i> is the source mathematical function
 * and <i>g</i> is the result of applying the operator to <i>f</i>.
 * The mapping is fully defined by the basic method of this interface,
 * {@link #map(double[] destPoint, double[] srcPoint)},
 * that transforms the original point to the new point.</p>
 *
 * <p>Implementations of this interface are usually <b>immutable</b> and
 * always <b>thread-safe</b>: {@link #map map} method of this interface may be freely used
 * while simultaneous accessing the same instance from several threads.
 * All implementations of this interface from this package are <b>immutable</b>.</p>
 *
 * @author Daniel Alievsky
 */
public interface CoordinateTransformationOperator extends Operator {

    /**
     * Transforms the coordinates <code>srcPoint</code> of the original point in <i>n</i>-dimensional space
     * to the coordinates <code>destPoint</code> of the destination point.
     * Usually <code>destPoint.length</code> must be equal
     * to <code>srcPoint.length</code> (the number of dimensions),
     * but this requirement is not strict.
     *
     * <p>This method must not modify <code>srcPoint</code> array.
     *
     * <p><i>Warning:</i> this method will probably not work correctly if
     * <code>destPoint</code> and <code>srcPoint</code>
     * is the same Java array!
     *
     * @param destPoint the coordinates of the destinated point <b>y</b>, filled by this method.
     * @param srcPoint  the coordinates of the source point <b>x</b>.
     * @throws NullPointerException     if one of the arguments is {@code null}.
     * @throws IllegalArgumentException if <code>destPoint.length!=srcPoint.length</code>
     *                                  (may be not thrown by some implementations,
     *                                  or may be thrown in other situations).
     */
    void map(double[] destPoint, double[] srcPoint);

    /**
     * In this interface, this method is equivalent to
     * {@link CoordinateTransformedFunc#getInstance(Func, CoordinateTransformationOperator)
     * CoordinateTransformedFunc.getInstance(f, this)}.
     *
     * @param f the parent function, the arguments of which will be mapped by this operator.
     * @return new transformed function.
     */
    Func apply(Func f);

    /**
     * Returns the hash code of this object. The result depends on all parameters, specifying
     * coordinate transformation, performed by this operator.
     *
     * @return the hash code of this operator.
     */
    int hashCode();

    /**
     * Indicates whether some other object is also a {@link CoordinateTransformationOperator
     * coordinate transformation operator}, performing the same coordinate transformation as this one.
     *
     * <p>There is high probability, but no guarantee that this method returns <code>true</code> if the passed operator
     * specifies a transformation, identical to this one.
     * There is a guarantee that this method returns <code>false</code>
     * if the passed operator specifies a transformation, different from this one.
     *
     * @param obj the object to be compared for equality with this operator.
     * @return <code>true</code> if the specified object is a coordinate transformation operator equal to this one.
     */
    boolean equals(Object obj);
}
