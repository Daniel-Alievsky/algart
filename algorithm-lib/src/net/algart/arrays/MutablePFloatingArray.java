package net.algart.arrays;

/**
 * <p>Resizable AlgART array of any floating-point primitive elements (float or double).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableFloatArray}, {@link MutableDoubleArray} subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutablePFloatingArray extends UpdatablePFloatingArray, MutablePNumberArray {
    public MutablePFloatingArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutablePFloatingArray setData(long arrayPos, Object srcArray);

    public MutablePFloatingArray copy(Array src);

    public MutablePFloatingArray swap(UpdatableArray another);

    public MutablePFloatingArray length(long newLength);

    public MutablePFloatingArray ensureCapacity(long minCapacity);

    public MutablePFloatingArray trim();

    public MutablePFloatingArray append(Array appendedArray);

    public MutablePFloatingArray asCopyOnNextWrite();

    public MutablePFloatingArray shallowClone();
}
