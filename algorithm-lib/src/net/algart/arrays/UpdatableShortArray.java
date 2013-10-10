package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, UpdatableFloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Short ;;
  float ==> short
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>AlgART array of <tt>short</tt> values, read/write access, no resizing.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatableShortArray extends ShortArray, UpdatablePIntegerArray {
    /**
     * Sets the element #<tt>index</tt> to specified <tt>value</tt>.
     *
     * @param index index of element to replace.
     * @param value element to be stored at the specified position.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    public void setShort(long index, short value);

    /**
     * Fills all elements of this array by the specified value. Equivalent to
     * <tt>{@link #fill(long, long, short) fill}(0, thisArray.length(), value)</tt>.
     *
     * @param value the value to be stored in all elements of the array.
     * @return      a reference to this array.
     * @see #fill(long, long, short)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableShortArray fill(short value);

    /**
     * Fills <tt>count</tt> elements of this array, starting from <tt>position</tt> index,
     * by the specified value. Equivalent to the following loop:<pre>
     * for (long k = 0; k &lt; count; k++) {
     * &#32;   {@link #setShort(long, short) setShort}(position + k, value);
     * }</pre>
     * but works much faster and checks indexes
     * (and throws possible <tt>IndexOutOfBoundsException</tt>) in the very beginning.
     *
     * @param position start index (inclusive) to be filled.
     * @param count    number of filled elements.
     * @param value    the value to be stored in the elements of the array.
     * @return         a reference to this array.
     * @throws IndexOutOfBoundsException for illegal <tt>position</tt> and <tt>count</tt>
     *                                   (<tt>position &lt; 0 || count &lt; 0 || position + count &gt; length()</tt>).
     * @see #fill(short)
     * @see Arrays#zeroFill(UpdatableArray)
     */
    public UpdatableShortArray fill(long position, long count, short value);

    public UpdatableShortArray subArray(long fromIndex, long toIndex);

    public UpdatableShortArray subArr(long position, long count);

    public UpdatableShortArray asUnresizable();
    /*Repeat.IncludeEnd*/
}
