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

package net.algart.matrices.spectra;

/**
 * <p>Array of samples for transforming by some spectral algorithm like FFT.</p>
 *
 * <p>A <i>sample</i> can be an element of any linear space, real or complex. Usually samples are
 * either usual complex numbers (represented by pairs of <code>float</code> or <code>double</code> values)
 * or vectors of such numbers. The first case is used for 1-dimensional transformations,
 * the second case is used for multidimensional transformations, when we need parallel processing (adding,
 * subtracting, multiplying by scalar) whole lines of a matrix.</p>
 *
 * <p>This interface is an abstraction representing an array of samples, that should be transformed
 * by FFT or similar algorithms. The array is updatable (elements can be overwritten),
 * but unresizable (its length cannot be changed).
 * This interface allows to perform the basic necessary operations over
 * samples: copying, swapping, adding, subtracting, multiplying by a scalar.
 * The set of supported operations is chosen according needs of fast transformation algorithms.
 * In addition, this interface allows to allocate work memory for necessary number
 * of temporary samples in a form of new sample array of the same type.</p>
 *
 * <p>Different implementations of this interface can store samples of different <i>kinds</i>.
 * For example, the complex numbers with <code>double</code> precision is one possible kind of samples,
 * and the vectors of complex numbers with some fixed length is another kind
 * (vectors with different lengths belong to different kinds).
 * Moreover, usual complex numbers, stored by different technologies (by different implementations
 * of this interface), can belong to different kinds.
 * All operations, specified by this class between samples &mdash; copying, adding, etc.
 * &mdash; can be performed only between samples of the same kind.</p>
 *
 * <p>There is a guarantee that all samples in this array belong to the same kind and are fully
 * compatible between each other: for example, can be swapped, added or subtracted.
 * In particular, if the samples are vectors of complex numbers, then the lengths of all these vectors are equal.
 * There is also a guarantee that the samples in the array, created by {@link #newCompatibleSamplesArray(long)}
 * method, also belong to the same kind.</p>
 *
 * <p>All indexes, passed to methods of this class, must be in range <code>0..length-1</code>,
 * where <code>length</code> is the {@link #length() length} of the corresponding sample array.
 * (In a case of {@link #multiplyRangeByRealScalar multiplyRangeByRealScalar} method,
 * its arguments must comply the conditions <code>0&lt;=fromIndex&lt;=toIndex&lt;=length</code>.)
 * If this requirement is not satisfied, the results are unspecified.
 * ("Unspecified" means that any elements of sample arrays can be read or changed,
 * or that <code>IndexOutOfBoundsException</code> can be thrown.)
 * The reason of this behavior is that this interface is designed for maximal performance,
 * and its methods do not always check the passed indexes.</p>
 *
 * <p>All calculations, performed by methods of this interface and its implementations,
 * are performed over floating-point numbers, corresponding to <code>float</code> or <code>double</code> Java types.
 * It is theoretically possible to work with sample arrays, represented by fixed-point numbers,
 * alike Java <code>int</code>, <code>long</code>, <code>short</code> and other types (for example,
 * if it is {@link RealScalarSampleArray}, built on the base of
 * {@link net.algart.arrays.UpdatableByteArray UpdatableByteArray}). In this situation,
 * calculations can be performed with some form of rounding, possible overflows lead to unspecified results
 * and algorithms can work slower.</p>
 *
 * <p>The sample arrays are not thread-safe, but <b>are thread-compatible</b>
 * and can be synchronized manually if multithreading access is necessary.</p>
 *
 * @author Daniel Alievsky
 */
public interface SampleArray {

    /**
     * The number of elements if new sample arrays, that can be allocated
     * by {@link #newCompatibleSamplesArray} method in any case, if there is enough memory.
     * You may freely use that method for this or less lengths of created arrays,
     * as well as for creating arrays not longer than this one.
     * The value of this limit is {@value}, that is enough for most practical needs in temporary values.
     */
    int GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH = 64;

    /**
     * Returns <code>true</code> if the samples in this array are complex, <code>false</code> if they are real.
     * The method {@link #multiplyByScalar(long, SampleArray, long, double, double)} works correctly
     * if and only if this method returns <code>true</code>.
     *
     * <p>Some methods of this package, for example, Fourier transformations
     * throw {@link UnsupportedOperationException} if this method returns <code>false</code>.
     *
     * @return <code>true</code> if the samples in this array are complex.
     */
    boolean isComplex();

    /**
     * Returns the length: number of elements in this array.
     * The result is never negative.
     *
     * @return the length: number of elements in this array.
     */
    long length();

    /**
     * Creates a new array of samples of the same kind as this one.
     * For example, if the samples are vectors of N complex numbers with <code>float</code> precision,
     * then the elements of the returned arrays will also be vectors of N complex numbers
     * with <code>float</code> precision.
     *
     * <p>The typical usage of this method is allocating temporary elements for storing one or several
     * samples inside FFT algorithm.
     *
     * <p>Usually this method allocates new array in a usual Java heap (i.e.
     * {@link net.algart.arrays.SimpleMemoryModel SimpleMemoryModel} when it is based on AlgART arrays).
     * But some implementations can use another storages, like
     * {@link net.algart.arrays.LargeMemoryModel LargeMemoryModel}
     * or another custom {@link net.algart.arrays.MemoryModel memory models}, specified while creating
     * the sample array. This package provides two implementations, illustrating this:
     * the results of {@link RealVectorSampleArray#asSampleArray RealVectorSampleArray.asSampleArray}
     * and {@link ComplexVectorSampleArray#asSampleArray ComplexVectorSampleArray.asSampleArray} method,
     * but only in a case, when the length of each real/complex vector (a sample) is large
     * (for relatively little samples, {@link net.algart.arrays.SimpleMemoryModel SimpleMemoryModel} is used always).
     *
     * <p>If the required length is too long and there is not enough memory, <code>OutOfMemoryError</code> or similar
     * errors will be thrown. However, for very large <code>length</code> values, it is also possible
     * that there is enough memory, but this method throws an exception &mdash; because the technology
     * of storing elements does not support such large lengths. For example, if it is an array of real
     * vectors with length 1000000, and all vectors are stored inside a single Java array <code>float[]</code>,
     * then <code>length=10000</code> will lead to an exception, even if there is necessary amount of Java memory
     * (~40&nbsp;GB) &mdash; because Java cannot allocate an array longer than
     * <code>Integer.MAX_VALUE</code>. But there is the following guarantee:
     * this method always works correctly, if there is enough memory and
     *
     * <pre>
     * length &lt;= max(thisArray.{@link #length()
     * length()}, {@link #GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH
     * GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH}).</pre>
     *
     * <p>So, your can freely use this method for allocating new sample arrays, not greater
     * than this instance, or for allocating a short arrays for temporary values, if their lengths
     * does not exceed {@link #GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH GUARANTEED_COMPATIBLE_SAMPLES_ARRAY_LENGTH}
     * limit.
     *
     * @param length the length of new sample array.
     * @return       new sample array, compatible with this one.
     * @throws IllegalArgumentException if the argument is negative.
     */
    SampleArray newCompatibleSamplesArray(long length);

    /**
     * Copies the sample #<code>srcIndex</code> from <code>src</code> array into position
     * #<code>destIndex</code> in this array.
     *
     * @param destIndex index of sample in this array to replace.
     * @param src       the source sample array (maybe, a reference to this array).
     * @param srcIndex  index of sample in <code>src</code> array to be copied.
     * @throws IllegalArgumentException if elements of <code>src</code> array do not belong to the same kind
     *                                  as elements of this array, for example, they are vectors of
     *                                  complex numbers with another length. Instead of this exception,
     *                                  some other exceptions are also possible in this case, for example,
     *                                  <code>ClassCastException</code> or
     *                                  {@link net.algart.arrays.SizeMismatchException}.
     */
    void copy(long destIndex, SampleArray src, long srcIndex);

    /**
     * Swaps samples at positions #<code>firstIndex</code> and #<code>secondIndex</code> inside this array.
     * #<code>destIndex</code> in this array.
     *
     * @param firstIndex  first index of sample to exchange.
     * @param secondIndex second index of sample to exchange.
     */
    void swap(long firstIndex, long secondIndex);

    /**
     * Adds the sample #<code>srcIndex2</code> of <code>src</code> array to the sample #<code>srcIndex1</code>
     * of <code>src</code> array and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex1</code> or <code>srcIndex2</code>,
     * and <code>src</code> array can be a reference to this array: these situations are processed correctly.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param src       some other array (or, maybe, a reference to this array).
     * @param srcIndex1 the index of the first summand in <code>src</code> array.
     * @param srcIndex2 the index of the second summand in <code>src</code> array.
     * @throws IllegalArgumentException if elements of <code>src</code> array do not belong to the same kind
     *                                  as elements of this array, for example, they are vectors of
     *                                  complex numbers with another length. Instead of this exception,
     *                                  some other exceptions are also possible in this case, for example,
     *                                  <code>ClassCastException</code> or
     *                                  {@link net.algart.arrays.SizeMismatchException}.
     */
    void add(long destIndex, SampleArray src, long srcIndex1, long srcIndex2);

    /**
     * Subtracts the sample #<code>srcIndex2</code> of <code>src</code> array from the sample #<code>srcIndex1</code>
     * of <code>src</code> array and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex1</code> or <code>srcIndex2</code>,
     * and <code>src</code> array can be a reference to this array: these situations are processed correctly.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param src       some other array (or, maybe, a reference to this array).
     * @param srcIndex1 the index of the minuend in <code>src</code> array.
     * @param srcIndex2 the index of the subtrahend in <code>src</code> array.
     * @throws IllegalArgumentException if elements of <code>src</code> array do not belong to the same kind
     *                                  as elements of this array, for example, they are vectors of
     *                                  complex numbers with another length. Instead of this exception,
     *                                  some other exceptions are also possible in this case, for example,
     *                                  <code>ClassCastException</code> or
     *                                  {@link net.algart.arrays.SizeMismatchException}.
     */
    void sub(long destIndex, SampleArray src, long srcIndex1, long srcIndex2);

    /**
     * Adds the sample #<code>srcIndex2</code> of <code>src2</code> array to the sample #<code>srcIndex1</code>
     * of this array and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex1</code> or <code>srcIndex2</code>,
     * and <code>src2</code> array can be a reference to this array: these situations are processed correctly.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param srcIndex1 the index of the first summand in this array.
     * @param src2      some other array (or, maybe, a reference to this array).
     * @param srcIndex2 the index of the second summand in <code>src2</code> array.
     * @throws IllegalArgumentException if elements of <code>src2</code> array do not belong to the same kind
     *                                  as elements of this array, for example, they are vectors of
     *                                  complex numbers with another length. Instead of this exception,
     *                                  some other exceptions are also possible in this case, for example,
     *                                  <code>ClassCastException</code> or
     *                                  {@link net.algart.arrays.SizeMismatchException}.
     */
    void add(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2);

    /**
     * Subtracts the sample #<code>srcIndex2</code> of <code>src2</code> array from the sample #<code>srcIndex1</code>
     * of this array and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex1</code> or <code>srcIndex2</code>,
     * and <code>src2</code> array can be a reference to this array: these situations are processed correctly.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param srcIndex1 the index of the minuend in this array.
     * @param src2      some other array (or, maybe, a reference to this array).
     * @param srcIndex2 the index of the subtrahend in <code>src2</code> array.
     * @throws IllegalArgumentException if elements of <code>src2</code> array do not belong to the same kind
     *                                  as elements of this array, for example, they are vectors of
     *                                  complex numbers with another length. Instead of this exception,
     *                                  some other exceptions are also possible in this case, for example,
     *                                  <code>ClassCastException</code> or
     *                                  {@link net.algart.arrays.SizeMismatchException}.
     */
    void sub(long destIndex, long srcIndex1, SampleArray src2, long srcIndex2);

    /**
     * Adds the sample #<code>srcIndex2</code> of this array to the sample #<code>srcIndex1</code>
     * of this array and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex1</code> or <code>srcIndex2</code>:
     * these situations are processed correctly.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param srcIndex1 the index of the first summand in this array.
     * @param srcIndex2 the index of the second summand in this array.
     */
    void add(long destIndex, long srcIndex1, long srcIndex2);

    /**
     * Subtracts the sample #<code>srcIndex2</code> of this array from the sample #<code>srcIndex1</code>
     * of this array and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex1</code> or <code>srcIndex2</code>:
     * these situations are processed correctly.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param srcIndex1 the index of the minuend in this array.
     * @param srcIndex2 the index of the subtrahend in this array.
     */
    void sub(long destIndex, long srcIndex1, long srcIndex2);

    /**
     * Multiplies the sample #<code>srcIndex</code> of <code>src</code> array by the complex scalar
     * <code>aRe+aIm</code>*<i>i</i> (<i>i</i> is the imaginary unit)
     * and stores the result into position #<code>destIndex</code> of this array.
     * The <code>destIndex</code> can be the same as <code>srcIndex</code>
     * and <code>src</code> array can be a reference to this array: this situations is processed correctly.
     *
     * <p>If this sample array consists of real samples (for example, real numbers or vectors of real numbers),
     * then the imaginary part <code>aIm</code> is ignored.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param src       some other array (or, maybe, a reference to this array).
     * @param srcIndex  the index of the sample in <code>src</code> array.
     * @param aRe       the real part of the complex scalar.
     * @param aIm       the imaginary part of the complex scalar.
     * @throws IllegalArgumentException if elements of <code>src</code> array do not belong to the same kind
     *                                  as elements of this array, for example, they are vectors of
     *                                  complex numbers with another length. Instead of this exception,
     *                                  some other exceptions are also possible in this case, for example,
     *                                  <code>ClassCastException</code> or
     *                                  {@link net.algart.arrays.SizeMismatchException}.
     * @see #isComplex()
     */
    void multiplyByScalar(long destIndex, SampleArray src, long srcIndex, double aRe, double aIm);

    /**
     * Multiplies the sample #<code>destIndex</code> of this array by the real scalar <code>a</code>
     * and stores the result into the same position #<code>destIndex</code> of this array.
     * Equivalent to <code>{@link #multiplyByScalar
     * multiplyByScalar}(destIndex,thisInstance,destIndex,a,0.0)</code>.
     *
     * @param index index of sample in this array.
     * @param a     the real scalar.
     */
    void multiplyByRealScalar(long index, double a);

    /**
     * Multiplies the sample #<code>srcIndex1</code> of this array by the real scalar <code>a1</code>,
     * multiplies the sample #<code>srcIndex2</code> of this array by the real scalar <code>a2</code>
     * and stores the sum of this two products result into position #<code>destIndex</code> of this array.
     *
     * @param destIndex index of sample in this array to store the result.
     * @param srcIndex1 the index of the first sample in this array.
     * @param a1        the multiplier for the first sample.
     * @param srcIndex2 the index of the second sample in this array.
     * @param a2        the multiplier for the second sample.
     */
    void combineWithRealMultipliers(long destIndex, long srcIndex1, double a1, long srcIndex2, double a2);

    /**
     * Multiplies the samples #<code>fromIndex..toIndex-1</code> of this array by the real scalar <code>a</code>
     * and stores the result into the same positions of this array.
     * Equivalent to the loop of {@link #multiplyByRealScalar(long, double) multiplyByRealScalar(index, a)}
     * for <code>index=fromIndex,fromIndex+1,...,toIndex-1</code>.
     *
     * @param fromIndex low index (inclusive) of elements to be multiplied.
     * @param toIndex   high index (exclusive) of elements to be multiplied.
     * @param a         the real scalar.
     */
    void multiplyRangeByRealScalar(long fromIndex, long toIndex, double a);

    /**
     * Returns a string representation of this sample array as contatenated string representations of samples,
     * separating by the given <code>separator</code>.
     *
     * <p>If the samples are numbers (real or complex) or vectors of numbers,
     * this method may use the <code>format</code> argument
     * to clarify the format of numbers according the rules of <code>String.format</code> method.
     * In other cases, this argument may be ignored.
     *
     * <p>If the necessary string length exceeds <code>maxStringLength</code> characters,
     * this method break concatenation after the element, which leads to exceeding this limit,
     * and adds "..." instead of all further elements. So, the length of returning
     * string will never be essentially larger than <code>maxStringLength</code> characters.
     *
     * <p>If the passed array is empty, returns the empty string (<code>""</code>).
     *
     * @param format          format string for numeric samples.
     * @param separator       the string used for separating elements.
     * @param maxStringLength the maximal allowed length of returned string (longer results are trunctated
     *                        with adding "..." at the end).
     * @return                the string representations of all samples joined into one string.
     * @throws NullPointerException     if <code>format</code> or <code>separator</code> argument is {@code null}
     * @throws IllegalArgumentException if <code>maxStringLength</code> &lt;= 0.
     */
    String toString(String format, String separator, int maxStringLength);
}
