/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.stitching;

/**
 * <p>Abstract mathematical function, depending on coordinates in <i>n</i>-dimensional space
 * and several values. This function defines the algorithm of stitching several {@link Frame frames}:
 * it returns the value in the resulting stitched matrix at the given element coordinates.</p>
 *
 * <p>The typical examples of such functions are</p>
 *
 * <ul>
 * <li>{@link AverageExceptingNaN},</li>
 * <li>{@link MaxExceptingNaN},</li>
 * <li>{@link MinExceptingNaN},</li>
 * <li>{@link FirstExceptingNaN},</li>
 * <li>{@link LastExceptingNaN}.</li>
 * </ul>
 *
 * <p>Implementations of this interface are usually <b>immutable</b> and
 * always <b>thread-safe</b>: <tt>get</tt> methods of this interface may be freely used
 * while simultaneous accessing the same instance from several threads.
 * All implementations of this interface from this package are <b>immutable</b>.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface StitchingFunc {

    /**
     * Returns the result of this function for the given coordinates and for the values (at these coordinates)
     * of the matrices being stitched.
     *
     * <p>The <tt>coordinates</tt> argument contains the
     * {@link net.algart.arrays.Matrix#coordinates(long, long[]) coordinates}
     * of some element in the target (having been stitched) matrix. Usually all coordinates are integer,
     * though this method can be called in more general case of real coordinates of the point.
     *
     * <p>The <tt>values</tt> argument contains the values of elements of all beging stitched matrices,
     * which will correspond to these coordinates after stitching. If coordinates are not integer,
     * these values will be interpolated by some method.
     *
     * <p>Some elements of <tt>values</tt> array can contain the special value <tt>Double.NaN</tt>:
     * it means that the corresponding frame does not contain the point with the specified coordinates.
     * It particular, it is possible that all passed <tt>values</tt> are <tt>Double.NaN</tt>;
     * in this situation, as well as in a case <tt>values.length==0</tt>, this method should return
     * some special value ("filler" outside all frames).
     *
     * <p>This method must not change the elements of the passed Java arrays!
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param values      the values at this position of all matrices being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates, double[] values);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0}, values)</tt>.
     * Provides better performance because it does not require the first Java array creation.
     *
     * @param x0     the coordinate of some point in <i>1</i>-dimensional space.
     * @param values the values at this position of all matrices being stitched.
     * @return       the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0, double[] values);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1}, values)</tt>.
     * Provides better performance because it does not require the first Java array creation.
     *
     * @param x0     the 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1     the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param values the values at this position of all matrices being stitched.
     * @return       the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1, double[] values);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2}, values)</tt>.
     * Provides better performance because it does not require the first Java array creation.
     *
     * @param x0     the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1     the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2     the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param values the values at this position of all matrices being stitched.
     * @return       the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2, double[] values);

    /*Repeat() (new\s+double\[\]\s*\{v0) ==> $1,v1,,$1,v1,v2,,$1,v1,v2,v3,,$1,v1,v2,v3,v4,,
                                             $1,v1,v2,v3,v4,v5,,$1,v1,v2,v3,v4,v5,v6,,$1,v1,v2,v3,v4,v5,v6,v7;;
               (\,\s*double\s+)v0        ==> $1v0$1v1,,$1v0$1v1$1v2,,$1v0$1v1$1v2$1v3,,$1v0$1v1$1v2$1v3$1v4,,
                                             $1v0$1v1$1v2$1v3$1v4$1v5,,$1v0$1v1$1v2$1v3$1v4$1v5$1v6,,
                                             $1v0$1v1$1v2$1v3$1v4$1v5$1v6$1v7;;
               (\*\s*\@param\s*)v0(.*?(?:\r(?!\n)|\n|\r\n)\s*) ==>
               $1v0$2$1v1$2,,$1v0$2$1v1$2$1v2$2,,$1v0$2$1v1$2$1v2$2$1v3$2,,$1v0$2$1v1$2$1v2$2$1v3$2$1v4$2,,
               $1v0$2$1v1$2$1v2$2$1v3$2$1v4$2$1v5$2,,$1v0$2$1v1$2$1v2$2$1v3$2$1v4$2$1v5$2$1v6$2,,
               $1v0$2$1v1$2$1v2$2$1v3$2$1v4$2$1v5$2$1v6$2$1v7$2
     */

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0);
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1);


    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1,v2})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @param v2          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1,
        double v2);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1,v2})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1,
        double v2);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1,v2})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1,
        double v2);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1,v2})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1,
        double v2);


    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1,v2,v3})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @param v2          the value at this position of the matrix being stitched.
     * @param v3          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1,
        double v2,
        double v3);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1,v2,v3})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1,
        double v2,
        double v3);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1,v2,v3})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1,
        double v2,
        double v3);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1,v2,v3})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1,
        double v2,
        double v3);


    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1,v2,v3,v4})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @param v2          the value at this position of the matrix being stitched.
     * @param v3          the value at this position of the matrix being stitched.
     * @param v4          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1,v2,v3,v4})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1,v2,v3,v4})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1,v2,v3,v4})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4);


    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1,v2,v3,v4,v5})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @param v2          the value at this position of the matrix being stitched.
     * @param v3          the value at this position of the matrix being stitched.
     * @param v4          the value at this position of the matrix being stitched.
     * @param v5          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1,v2,v3,v4,v5})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1,v2,v3,v4,v5})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1,v2,v3,v4,v5})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5);


    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1,v2,v3,v4,v5,v6})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @param v2          the value at this position of the matrix being stitched.
     * @param v3          the value at this position of the matrix being stitched.
     * @param v4          the value at this position of the matrix being stitched.
     * @param v5          the value at this position of the matrix being stitched.
     * @param v6          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1,v2,v3,v4,v5,v6})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @param v6 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1,v2,v3,v4,v5,v6})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @param v6 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1,v2,v3,v4,v5,v6})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @param v6 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6);


    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(coordinates,
     * new double[] {v0,v1,v2,v3,v4,v5,v6,v7})</tt>.
     * Provides better performance because it does not require the second Java array creation.
     *
     * @param coordinates the coordinates of some point in <i>n</i>-dimensional space.
     * @param v0          the value at this position of the matrix being stitched.
     * @param v1          the value at this position of the matrix being stitched.
     * @param v2          the value at this position of the matrix being stitched.
     * @param v3          the value at this position of the matrix being stitched.
     * @param v4          the value at this position of the matrix being stitched.
     * @param v5          the value at this position of the matrix being stitched.
     * @param v6          the value at this position of the matrix being stitched.
     * @param v7          the value at this position of the matrix being stitched.
     * @return            the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get(double[] coordinates,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6,
        double v7);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0},
     * new double[] {v0,v1,v2,v3,v4,v5,v6,v7})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the coordinate of some point in <i>1</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @param v6 the value at this position of the matrix being stitched.
     * @param v7 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get1D(double x0,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6,
        double v7);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1},
     * new double[] {v0,v1,v2,v3,v4,v5,v6,v7})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 1st coordinate of some point in <i>2</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>2</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @param v6 the value at this position of the matrix being stitched.
     * @param v7 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get2D(double x0, double x1,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6,
        double v7);

    /**
     * Equivalent to <tt>{@link #get(double[], double[]) get}(new double[] {x0,x1,x2},
     * new double[] {v0,v1,v2,v3,v4,v5,v6,v7})</tt>.
     * Provides better performance because it does not require Java array creation.
     *
     * @param x0 the 1st coordinate of some point in <i>3</i>-dimensional space.
     * @param x1 the 2nd coordinate of some point in <i>3</i>-dimensional space.
     * @param x2 the 3rd coordinate of some point in <i>3</i>-dimensional space.
     * @param v0 the value at this position of the matrix being stitched.
     * @param v1 the value at this position of the matrix being stitched.
     * @param v2 the value at this position of the matrix being stitched.
     * @param v3 the value at this position of the matrix being stitched.
     * @param v4 the value at this position of the matrix being stitched.
     * @param v5 the value at this position of the matrix being stitched.
     * @param v6 the value at this position of the matrix being stitched.
     * @param v7 the value at this position of the matrix being stitched.
     * @return   the value that will be saved in the resulting stitched matrix at these coordinates.
     */
    public double get3D(double x0, double x1, double x2,
        double v0,
        double v1,
        double v2,
        double v3,
        double v4,
        double v5,
        double v6,
        double v7);
    /*Repeat.AutoGeneratedEnd*/
}
