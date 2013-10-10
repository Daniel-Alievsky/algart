package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PFixed ;;
  Float(Array|Stack) ==> Bit$1 ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>boolean</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableBitArray extends BitStack, UpdatableBitArray, MutablePFixedArray {
    public MutableBitArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableBitArray setData(long arrayPos, Object srcArray);

    public MutableBitArray copy(Array src);

    public MutableBitArray swap(UpdatableArray another);

    public MutableBitArray length(long newLength);

    public MutableBitArray ensureCapacity(long minCapacity);

    public MutableBitArray trim();

    public MutableBitArray append(Array appendedArray);

    public MutableBitArray asCopyOnNextWrite();

    public MutableBitArray shallowClone();
    /*Repeat.IncludeEnd*/
}
