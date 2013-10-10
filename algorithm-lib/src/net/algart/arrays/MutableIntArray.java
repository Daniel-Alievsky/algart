package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Int ;;
  float ==> int
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>int</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableIntArray extends IntStack, UpdatableIntArray, MutablePIntegerArray {
    public MutableIntArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableIntArray setData(long arrayPos, Object srcArray);

    public MutableIntArray copy(Array src);

    public MutableIntArray swap(UpdatableArray another);

    public MutableIntArray length(long newLength);

    public MutableIntArray ensureCapacity(long minCapacity);

    public MutableIntArray trim();

    public MutableIntArray append(Array appendedArray);

    public MutableIntArray asCopyOnNextWrite();

    public MutableIntArray shallowClone();
    /*Repeat.IncludeEnd*/
}
