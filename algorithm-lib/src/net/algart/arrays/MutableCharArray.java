package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PFixed ;;
  Float(?!ing) ==> Char ;;
  float ==> char
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>char</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableCharArray extends CharStack, UpdatableCharArray, MutablePFixedArray {
    public MutableCharArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableCharArray setData(long arrayPos, Object srcArray);

    public MutableCharArray copy(Array src);

    public MutableCharArray swap(UpdatableArray another);

    public MutableCharArray length(long newLength);

    public MutableCharArray ensureCapacity(long minCapacity);

    public MutableCharArray trim();

    public MutableCharArray append(Array appendedArray);

    public MutableCharArray asCopyOnNextWrite();

    public MutableCharArray shallowClone();
    /*Repeat.IncludeEnd*/

    /**
     * Appends all characters of <tt>value</tt> to the end of this array and returns this array.
     * @param value a string to be appended to this array.
     * @return      a reference to this object.
     * @throws TooLargeArrayException if the resulting array length is too large for this type of arrays.
     */
    public MutableCharArray append(String value);
}
