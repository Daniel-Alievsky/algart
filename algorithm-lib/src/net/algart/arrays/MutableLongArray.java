package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Long ;;
  float ==> long
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>long</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableLongArray extends LongStack, UpdatableLongArray, MutablePIntegerArray {
    public MutableLongArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableLongArray setData(long arrayPos, Object srcArray);

    public MutableLongArray copy(Array src);

    public MutableLongArray swap(UpdatableArray another);

    public MutableLongArray length(long newLength);

    public MutableLongArray ensureCapacity(long minCapacity);

    public MutableLongArray trim();

    public MutableLongArray append(Array appendedArray);

    public MutableLongArray asCopyOnNextWrite();

    public MutableLongArray shallowClone();
    /*Repeat.IncludeEnd*/
}
