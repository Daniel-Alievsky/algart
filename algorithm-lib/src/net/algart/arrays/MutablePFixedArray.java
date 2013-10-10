package net.algart.arrays;

/**
 * <p>Resizable AlgART array of any fixed-point numeric, character or bit primitive elements
 * (byte, short, int, long, char or boolean).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableBitArray}, {@link MutableCharArray},
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
public interface MutablePFixedArray
    extends UpdatablePFixedArray, MutablePArray
{
    public MutablePFixedArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutablePFixedArray setData(long arrayPos, Object srcArray);

    public MutablePFixedArray copy(Array src);

    public MutablePFixedArray swap(UpdatableArray another);

    public MutablePFixedArray length(long newLength);

    public MutablePFixedArray ensureCapacity(long minCapacity);

    public MutablePFixedArray trim();

    public MutablePFixedArray append(Array appendedArray);

    public MutablePFixedArray asCopyOnNextWrite();

    public MutablePFixedArray shallowClone();
}
