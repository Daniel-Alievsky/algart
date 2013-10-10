package net.algart.arrays;

/**
 * <p>Resizable AlgART array of any fixed-point numeric primitive elements (byte, short, int or long).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray}
 * subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutablePIntegerArray
    extends UpdatablePIntegerArray, MutablePFixedArray, MutablePNumberArray
{
    public MutablePIntegerArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutablePIntegerArray setData(long arrayPos, Object srcArray);

    public MutablePIntegerArray copy(Array src);

    public MutablePIntegerArray swap(UpdatableArray another);

    public MutablePIntegerArray length(long newLength);

    public MutablePIntegerArray ensureCapacity(long minCapacity);

    public MutablePIntegerArray trim();

    public MutablePIntegerArray append(Array appendedArray);

    public MutablePIntegerArray asCopyOnNextWrite();

    public MutablePIntegerArray shallowClone();
}
