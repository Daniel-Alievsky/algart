package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  PFloating ==> PInteger ;;
  Float(?!ing) ==> Byte ;;
  float ==> byte
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>byte</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableByteArray extends ByteStack, UpdatableByteArray, MutablePIntegerArray {
    public MutableByteArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableByteArray setData(long arrayPos, Object srcArray);

    public MutableByteArray copy(Array src);

    public MutableByteArray swap(UpdatableArray another);

    public MutableByteArray length(long newLength);

    public MutableByteArray ensureCapacity(long minCapacity);

    public MutableByteArray trim();

    public MutableByteArray append(Array appendedArray);

    public MutableByteArray asCopyOnNextWrite();

    public MutableByteArray shallowClone();
    /*Repeat.IncludeEnd*/
}
