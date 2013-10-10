package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Short ;;
  float ==> short
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>short</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableShortArray extends ShortStack, UpdatableShortArray, MutablePIntegerArray {
    public MutableShortArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableShortArray setData(long arrayPos, Object srcArray);

    public MutableShortArray copy(Array src);

    public MutableShortArray swap(UpdatableArray another);

    public MutableShortArray length(long newLength);

    public MutableShortArray ensureCapacity(long minCapacity);

    public MutableShortArray trim();

    public MutableShortArray append(Array appendedArray);

    public MutableShortArray asCopyOnNextWrite();

    public MutableShortArray shallowClone();
    /*Repeat.IncludeEnd*/
}
