package net.algart.arrays;

/**
 * <p>Resizable AlgART array of primitive elements (boolean, char, byte, short, int, long, float or double).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableBitArray}, {@link MutableCharArray},
 * {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray},
 * {@link MutableFloatArray}, {@link MutableDoubleArray}
 * subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutablePArray extends UpdatablePArray, MutableArray {
    public MutablePArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutablePArray setData(long arrayPos, Object srcArray);

    public MutablePArray copy(Array src);

    public MutablePArray swap(UpdatableArray another);

    public MutablePArray length(long newLength);

    public MutablePArray ensureCapacity(long minCapacity);

    public MutablePArray trim();

    public MutablePArray append(Array appendedArray);

    public MutablePArray asCopyOnNextWrite();

    public MutablePArray shallowClone();
}
