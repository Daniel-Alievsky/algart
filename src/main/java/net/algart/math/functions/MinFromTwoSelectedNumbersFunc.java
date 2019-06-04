/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 * <p>Minimum from 2 arguments, selected by 1st argument:
 * <i>f</i>(<i>x</i><sub>0</sub>, <i>x</i><sub>1</sub>, ..., <i>x</i><sub><i>n</i>-1</sub>) =
 * min(<i>x</i><sub><i>i</i>+1</sub>, <i>x</i><sub><i>j</i>+1</sub>)</nobr>,
 * <nobr><i>i</i>=<tt>(int)x[0]</tt> (<i>x</i><sub>0</sub> cast to integer type),
 * <nobr><i>j</i>=(<i>i</i>&minus;1+<tt>indexShift</tt>)%(<i>n</i>&minus;1)+1</nobr>,
 * where <tt>indexShift</tt> is an integer constant, passed to {@link #getInstance} method.</p>
 *
 * <p>More precisely, the {@link #get} method of this object performs the following actions:</p>
 *
 * <pre>
 * &#32;   int k1 = (int)x[0] + 1;
 * &#32;   // it is supposed that always 1&lt;=(int)x[0]&lt;=x.length
 * &#32;   int k2 = k1 + indexShift;
 * &#32;   if (k2 &lt;= x.length) {
 * &#32;       // it is supposed that indexShift&lt;x.length-1
 * &#32;       k2 -= x.length - 1;
 * &#32;   }
 * &#32;   return x[k1] &lt; x[k2] ? x[k1] : x[k2];
 * </pre>
 *
 * <p>If <tt>k1</tt> or <tt>k2</tt> index, calculated in such a way, is out of range <tt>0..x.length-1</tt>,
 * this method throws <tt>IndexOutOfBoundsException</tt>.</p>
 *
 * <p>This function can be useful for algorithms of non-maximum suppression, usually in combination with
 * {@link Func#SELECT_FROM_8_DIRECTIONS_2D}, for example, in algorithms
 * like <a href="http://en.wikipedia.org/wiki/Canny_edge_detector">Canny edge detector</a>.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class MinFromTwoSelectedNumbersFunc extends AbstractFunc implements Func {
    private final int indexShift;

    private MinFromTwoSelectedNumbersFunc(int indexShift) {
        if (indexShift < 0)
            throw new IllegalArgumentException("Negative index shift " + indexShift);
        this.indexShift = indexShift;
    }

    /**
     * Returns an instance of this class for the given index shift.</p>
     *
     * @param indexShift the index shift (distance between compared numbers); must be non-negative.
     * @return           an instance of this class
     * @throws IllegalArgumentException if <tt>indexShift&lt;0</tt>.
     */
    public static MinFromTwoSelectedNumbersFunc getInstance(int indexShift) {
        return new MinFromTwoSelectedNumbersFunc(indexShift);
    }

    public double get(double ...x) {
        int k1 = (int)x[0] + 1; // it is supposed that k1 < x.length
        int k2 = k1 + indexShift;
        if (k2 >= x.length) {
            k2 -= x.length - 1;
        }
        return x[k1] < x[k2] ? x[k1] : x[k2];
    }


    /**
     * Returns a brief string description of this object.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "min from 2 selected numbers f(x0,x1,...)=min(x[(int)x0+1],x[(int)x0+1+sh]), sh=" + indexShift;
    }
}
